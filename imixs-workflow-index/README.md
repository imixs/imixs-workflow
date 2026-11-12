# Imixs-Workflow Index

The module `imixs-workflow-index` provides the search index for Imixs-Workflow based on [Apache Lucene 10](https://lucene.apache.org/). It replaces the former module `imixs-workflow-index-lucene` (Lucene 7) and is the central index solution of Imixs-Workflow.

The index runs embedded in the application server. No external search server is required.

## Configuration

| Property                | Default                                              | Description                                                              |
| ----------------------- | ---------------------------------------------------- | ------------------------------------------------------------------------ |
| `lucence.indexDir`      | `imixs-workflow-index`                               | Location of the index. The taxonomy index is stored in `<indexDir>_tax`. |
| `lucence.analyzerClass` | `org.apache.lucene.analysis.classic.ClassicAnalyzer` | Analyzer used for indexing.                                              |

The property names are kept for backward compatibility. The Lucene 7 class name `org.apache.lucene.analysis.standard.ClassicAnalyzer` is still accepted and mapped to the new package automatically (a warning is logged).

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

### Single node

The index is stored locally. Only one application instance can write to an index directory. Running multiple application instances requires a separate index per instance.

## JVM notes

Lucene 10 uses modern JVM features. The following warnings may appear on startup:

- `Use --enable-native-access=ALL-UNNAMED to avoid a warning` — Lucene uses the Foreign Function API for memory mapping. The warning is cosmetic and can be removed by adding the option to the JVM parameters.
- `Java runtime is not using Hotspot VM; Java vector incubator API can't be enabled` — Lucene can not use SIMD acceleration on non-HotSpot JVMs (e.g. OpenJ9). This has no relevant impact on full text search, but will be important for vector search. On a HotSpot JVM, add `--add-modules jdk.incubator.vector` to enable it.

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
- **The index must be rebuilt.** Lucene 10 can not read indices created by Lucene 7. The default index directory changed to `imixs-workflow-index-v10`, so a new index is created and rebuilt automatically on first startup. The old index is not touched.
- **Package name changed** from `org.imixs.workflow.engine.lucene` to `org.imixs.workflow.index`. Projects overriding classes like the `LuceneItemAdapter` need to update their imports.
- **Outdated indices are not deleted automatically.** If the index directory is configured explicitly and still contains a Lucene 7 index, the startup fails with a clear warning. An administrator has to decide how to handle the old index (backup, move or delete). After removing the index directory **and** the taxonomy directory (`<indexDir>_tax`), the index is rebuilt on the next startup.
