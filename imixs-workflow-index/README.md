# Imixs-Workflow Index

The module `imixs-workflow-index` provides the search index for Imixs-Workflow based on [Apache Lucene 10](https://lucene.apache.org/). It replaces the former module `imixs-workflow-index-lucene` (Lucene 7) and is the central index solution of Imixs-Workflow.

The index runs embedded in the application server. No external search server is required.

## Configuration

| Property                    | Environment variable        | Default                                              | Description                                                                     |
| --------------------------- | --------------------------- | ---------------------------------------------------- | ------------------------------------------------------------------------------- |
| `index.directory`           | `INDEX_DIRECTORY`           | **required**                                         | Location of the index. The taxonomy index is stored in `<index.directory>_tax`. |
| `index.luceneAnalyzerClass` | `INDEX_LUCENEANALYZERCLASS` | `org.apache.lucene.analysis.classic.ClassicAnalyzer` | Analyzer used for indexing.                                                     |

`index.directory` has no default value. If it is missing or empty, the deployment is aborted. Use an **absolute path on a persistent volume**, e.g. `/opt/jboss/lucene/imixs-workflow-index`. A relative path is resolved against the working directory of the server (e.g. `/opt/jboss/wildfly` in the WildFly container), which is usually not persistent — the index would be lost and rebuilt each time the container is recreated. A warning is logged in this case.

The Lucene 7 class name `org.apache.lucene.analysis.standard.ClassicAnalyzer` is still accepted and mapped to the new package automatically (a warning is logged).

## Architecture

### Transactional indexing via the EventLog

Index updates are never written directly within the processing transaction. When a document is saved, the `DocumentService` creates an EventLog entry (`index.add` / `index.remove`) in the same transaction. The index is updated later by flushing the EventLog. This way the index only reflects changes of successfully committed transactions.

When the EventLog is flushed, each processed block is committed to the index and the corresponding EventLog entries are removed in the same transaction. If the index commit fails, the transaction is rolled back and the entries are processed again. Because index updates are idempotent (`updateDocument` by `$uniqueid`), processing an entry twice is harmless.

### Read-your-writes

Before each search the complete EventLog is flushed (`UpdateService.updateIndex()`). A search therefore always reflects all changes committed before — for example, a workitem processed by a user immediately shows up in the worklist with its new status.

### Scheduled flush

In addition, a timer flushes the EventLog every 3 seconds. The timer is not required for correctness, but keeps the EventLog backlog small during long running update jobs, so the search-triggered flush stays fast.

The timer processes **at most one junk per tick**. A large backlog is processed over several ticks and the singleton lock is released between the ticks, so searches are not blocked for a long time.

| Value          | Setting      | Purpose                                                                                                                                         |
| -------------- | ------------ | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| Block size     | 256 entries  | One Lucene commit per block                                                                                                                     |
| Junk size      | 1024 entries | Maximum entries per transaction. Limits the heap usage, because all loaded documents stay in the persistence context until the transaction ends |
| Timer interval | 3 seconds    | Keeps the backlog small                                                                                                                         |

These values are starting points and may be tuned based on experience in production. The processing time per block is logged on level `FINE`.

### One long-lived IndexWriter per JVM

The `LuceneIndexManager` is a singleton owning all Lucene resources: one `IndexWriter`, one `DirectoryTaxonomyWriter` and the underlying directories. The writers are opened once on startup and closed on shutdown.

Lucene writers are thread-safe and designed to be shared. Opening and closing a writer for each update is expensive (lock acquisition, reading segment infos, waiting for merges) and leads to lock conflicts if two writers are opened at the same time. With a single writer per JVM, such conflicts are excluded by design.

Changes are persisted by an explicit commit. The taxonomy index is always committed first, so the main index never references facet ordinals which are not yet persisted.

### Why no Near-Real-Time search

Lucene supports Near-Real-Time (NRT) search, where changes become visible by a refresh without a commit. We decided against it. With NRT, an EventLog entry could no longer be removed in the same transaction as the index update, because the change is not yet persisted. This would require a two-phase processing of the EventLog (mark, commit, remove) and a recovery on startup. The additional complexity is not justified by the gain (saving one fsync per block).

### Fail fast on startup

The `LuceneIndexManager` is started on deployment. A missing configuration, a deprecated configuration property or an outdated index aborts the deployment immediately. A misconfiguration is detected when the application is deployed, not on the first search.

### Single node

The index is stored locally. Only one application instance can write to an index directory. Running multiple application instances requires a separate index per instance.

## Deployment on WildFly

Lucene 10 uses modern JVM features which are not available to a deployment by default. The index works without additional settings, but Lucene can not detect the JVM correctly and runs without SIMD acceleration. For full text search the impact is small, for vector search it is significant.

On startup the following warnings indicate missing settings:

| Warning                                                      | Cause                                                                                                         |
| ------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------- |
| `Lucene cannot optimize algorithms ... not based on Hotspot` | WildFly hides the JDK package `com.sun.management` from deployments. Lucene uses it to detect the HotSpot VM. |
| `Java vector incubator module is not readable`               | The Vector API module is not enabled.                                                                         |
| `Use --enable-native-access=ALL-UNNAMED to avoid a warning`  | Lucene uses the Foreign Function API for memory mapping.                                                      |

### Recommended JVM options

```
-Djboss.modules.system.pkgs=org.jboss.byteman,com.sun.management,jdk.incubator.vector
--add-modules=jdk.incubator.vector
--enable-native-access=ALL-UNNAMED
```

`jboss.modules.system.pkgs` makes the JDK packages visible to all deployments. `com.sun.management` is needed for the HotSpot detection, `jdk.incubator.vector` for the Vector API.

On startup the JVM logs `WARNING: Using incubator modules: jdk.incubator.vector`. This is expected.

### Setting JAVA_OPTS

**Important:** If the environment variable `JAVA_OPTS` is set, WildFly ignores all its default JVM options (see `bin/standalone.conf`). In this case the defaults must be included explicitly, together with the memory and GC settings. A complete example for Kubernetes:

```yaml
- name: JAVA_OPTS
  value: >
    -XX:MaxRAMPercentage=75.0
    -XX:+UseG1GC
    -XX:MaxGCPauseMillis=200
    -XX:InitiatingHeapOccupancyPercent=45
    -Djava.net.preferIPv4Stack=true
    -Djava.awt.headless=true
    -Djboss.modules.system.pkgs=org.jboss.byteman,com.sun.management,jdk.incubator.vector
    --add-modules=jdk.incubator.vector
    --enable-native-access=ALL-UNNAMED
```

The lines `-Djava.net.preferIPv4Stack`, `-Djava.awt.headless` and `-Djboss.modules.system.pkgs` replace the WildFly defaults which are dropped when `JAVA_OPTS` is set.

### Non-HotSpot JVMs

On JVMs not based on HotSpot (e.g. Eclipse OpenJ9) the Vector API can not be used by Lucene. Use a HotSpot based JDK (e.g. Eclipse Temurin, as used by the official WildFly images) for vector search.

## Migration from imixs-workflow-index-lucene

The module is a drop-in replacement. To migrate a project, replace the Maven dependency:

```xml
<dependency>
    <groupId>org.imixs.workflow</groupId>
    <artifactId>imixs-workflow-index</artifactId>
    <version>${org.imixs.workflow.version}</version>
</dependency>
```

Please note:

- **Only one index module per application.** Both modules implement the same `SearchService` and `UpdateService` interfaces. Deploying both leads to ambiguous CDI dependencies.
- **Java 21 is required** (Lucene 10).
- **The configuration properties were renamed.** `lucence.indexDir` is now `index.directory` and `lucence.analyzerClass` is now `index.luceneAnalyzerClass`. If an old property is still in use, the deployment is aborted with a hint which property has to be renamed.
- **The index must be rebuilt.** Lucene 10 can not read indices created by Lucene 7. Point `index.directory` to a new, empty location. A new index is created and rebuilt automatically on first startup.
- **Package name changed** from `org.imixs.workflow.engine.lucene` to `org.imixs.workflow.index`. Projects overriding classes like the `LuceneItemAdapter` need to update their imports.
- **Outdated indices are not deleted automatically.** If `index.directory` still points to a Lucene 7 index, the deployment is aborted with a clear warning. An administrator has to decide how to handle the old index (backup, move or delete). After removing the index directory **and** the taxonomy directory (`<indexDir>_tax`), the index is rebuilt on the next startup.
