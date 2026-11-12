/****************************************************************************
 * Copyright (c) 2022-2025 Imixs Software Solutions GmbH and others.
 * https://www.imixs.com
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * This Source Code may also be made available under the terms of the
 * GNU General Public License, version 2 or later (GPL-2.0-or-later),
 * which is available at https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-or-later
 ****************************************************************************/

package org.imixs.workflow.index;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexFormatTooOldException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.adminp.AdminPService;
import org.imixs.workflow.exceptions.IndexException;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Singleton;
import jakarta.inject.Inject;

/**
 * The LuceneIndexManager owns the lifecycle of the Lucene index resources. It
 * holds one long-lived IndexWriter and one long-lived DirectoryTaxonomyWriter
 * per JVM, together with the underlying Directory instances.
 * <p>
 * Lucene writers are thread-safe and designed to be shared. Opening and closing
 * a writer for each update is expensive (lock acquisition, reading segment
 * infos, commit and fsync on close). Instead, the writers are opened once and
 * changes are persisted by an explicit {@link #commit()}.
 * <p>
 * The manager uses bean managed concurrency. All Lucene resources are
 * thread-safe, only the (re)opening of a writer is synchronized.
 * <p>
 * On startup the manager verifies the index format. An index created by an
 * outdated Lucene version is not deleted automatically - an IndexException is
 * thrown and an administrator has to decide how to handle the old index.
 *
 * @version 1.0
 * @author rsoika
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class LuceneIndexManager {

    // Lucene 9+ moved the ClassicAnalyzer from 'analysis.standard' to
    // 'analysis.classic'
    public static final String DEFAULT_ANALYZER = "org.apache.lucene.analysis.classic.ClassicAnalyzer";
    public static final String DEFAULT_INDEX_DIRECTORY = "imixs-workflow-index-v10";
    public static final String TAXONOMY_DIRECTORY_SUFFIX = "_tax";

    // Lucene 7 class name of the ClassicAnalyzer, still accepted for backward
    // compatibility
    private static final String LEGACY_CLASSIC_ANALYZER = "org.apache.lucene.analysis.standard.ClassicAnalyzer";

    private static final Logger logger = Logger.getLogger(LuceneIndexManager.class.getName());

    @Inject
    @ConfigProperty(name = "lucence.indexDir", defaultValue = DEFAULT_INDEX_DIRECTORY)
    private String luceneIndexDir;

    @Inject
    @ConfigProperty(name = "lucence.analyzerClass", defaultValue = DEFAULT_ANALYZER)
    private String luceneAnalyzerClass;

    @Inject
    private AdminPService adminPService;

    private Directory indexDirectory;
    private Directory taxonomyDirectory;
    private IndexWriter indexWriter;
    private DirectoryTaxonomyWriter taxonomyWriter;

    /**
     * Opens the index and taxonomy directories, verifies the index format and
     * opens the long-lived writers. If no index exists yet, a new empty index is
     * created and a rebuild job is triggered.
     */
    @PostConstruct
    void init() {
        String indexPath = getIndexPath();
        String taxonomyPath = getTaxonomyPath();
        logger.log(Level.INFO, "├── 📂 opening index ''{0}'' (Lucene {1})...",
                new Object[] { indexPath, Version.LATEST });
        try {
            indexDirectory = FSDirectory.open(Paths.get(indexPath));
            taxonomyDirectory = FSDirectory.open(Paths.get(taxonomyPath));

            boolean indexExists = DirectoryReader.indexExists(indexDirectory);
            boolean taxonomyExists = DirectoryReader.indexExists(taxonomyDirectory);

            // verify existing indices before opening a writer
            if (indexExists) {
                verifyIndexFormat(indexDirectory, indexPath);
            }
            if (taxonomyExists) {
                verifyIndexFormat(taxonomyDirectory, taxonomyPath);
            }

            openWriters();

            if (!indexExists || !taxonomyExists) {
                // commit the new empty index so readers can open it immediately
                commit();
                logger.log(Level.INFO, "│   ├── 📂 index or taxonomy directory was empty ⇢ new index created");
                createRebuildJob();
            }
            logger.log(Level.INFO, "│   └── ✅ index ready");
        } catch (IOException e) {
            throw new IndexException(IndexException.INVALID_INDEX, "Unable to open lucene index: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Commits pending changes and closes all writers and directories on shutdown.
     */
    @PreDestroy
    void close() {
        logger.log(Level.INFO, "├── 📂 closing index...");
        try {
            commit();
        } catch (IOException e) {
            logger.log(Level.WARNING, "│   ├── ⚠️ unable to commit index on shutdown: {0}", e.getMessage());
        }
        closeQuietly(taxonomyWriter, "TaxonomyWriter");
        closeQuietly(indexWriter, "IndexWriter");
        closeQuietly(taxonomyDirectory, "taxonomy directory");
        closeQuietly(indexDirectory, "index directory");
        logger.log(Level.INFO, "│   └── ✅ index closed");
    }

    /**
     * Returns the shared IndexWriter. If the writer was closed due to a tragic
     * event (e.g. an IO error), a new writer is opened.
     *
     * @return IndexWriter
     * @throws IOException
     */
    public synchronized IndexWriter getIndexWriter() throws IOException {
        if (indexWriter == null || !indexWriter.isOpen()) {
            logger.log(Level.WARNING, "├── ⚠️ IndexWriter is closed ⇢ reopening...");
            if (indexWriter != null && indexWriter.getTragicException() != null) {
                logger.log(Level.WARNING, "│   └── cause: {0}", indexWriter.getTragicException().getMessage());
            }
            indexWriter = createIndexWriter();
        }
        return indexWriter;
    }

    /**
     * Returns the shared DirectoryTaxonomyWriter.
     *
     * @return DirectoryTaxonomyWriter
     * @throws IOException
     */
    public synchronized DirectoryTaxonomyWriter getTaxonomyWriter() throws IOException {
        if (taxonomyWriter == null) {
            taxonomyWriter = new DirectoryTaxonomyWriter(taxonomyDirectory);
        }
        return taxonomyWriter;
    }

    /**
     * Persists all pending changes. The taxonomy is committed first, so the main
     * index never references facet ordinals which are not yet persisted in the
     * taxonomy index.
     *
     * @throws IOException
     */
    public synchronized void commit() throws IOException {
        long l = System.currentTimeMillis();
        getTaxonomyWriter().commit();
        getIndexWriter().commit();
        logger.log(Level.FINEST, "│   ├── ✓ index committed in {0}ms", System.currentTimeMillis() - l);
    }

    public Directory getIndexDirectory() {
        return indexDirectory;
    }

    public Directory getTaxonomyDirectory() {
        return taxonomyDirectory;
    }

    public String getIndexPath() {
        // issue #599
        return luceneIndexDir.trim();
    }

    public String getTaxonomyPath() {
        String path = getIndexPath();
        if (path.endsWith("/")) {
            path = path.substring(0, path.lastIndexOf("/"));
        }
        return path + TAXONOMY_DIRECTORY_SUFFIX;
    }

    /**
     * Opens the IndexWriter and the TaxonomyWriter.
     */
    private void openWriters() throws IOException {
        indexWriter = createIndexWriter();
        taxonomyWriter = new DirectoryTaxonomyWriter(taxonomyDirectory);
    }

    /**
     * Creates a new IndexWriter with the configured analyzer.
     */
    private IndexWriter createIndexWriter() throws IOException {
        IndexWriterConfig indexWriterConfig;
        try {
            // issue #429
            // Class.newInstance() is deprecated since Java 9
            Analyzer analyzer = (Analyzer) Class.forName(resolveAnalyzerClassName()).getDeclaredConstructor()
                    .newInstance();
            indexWriterConfig = new IndexWriterConfig(analyzer);
        } catch (ReflectiveOperationException e) {
            throw new IndexException(IndexException.INVALID_INDEX,
                    "Unable to create analyzer '" + luceneAnalyzerClass + "'", e);
        }
        indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        logger.finest("│   ├── open IndexWriter");
        return new IndexWriter(indexDirectory, indexWriterConfig);
    }

    /**
     * Creates a rebuild index job. Because this bean is a singleton, the job is
     * created only once per JVM.
     */
    private void createRebuildJob() {
        logger.log(Level.INFO, "│   ├── 🔄 rebuild index job created");
        ItemCollection job = new ItemCollection();
        job.replaceItemValue("job", AdminPService.JOB_REBUILD_INDEX);
        adminPService.createJob(job);
    }

    /**
     * Resolves the configured analyzer class name. Maps the legacy Lucene 7
     * package name of the ClassicAnalyzer to the new Lucene 9+ package.
     *
     * @return analyzer class name
     */
    private String resolveAnalyzerClassName() {
        String className = luceneAnalyzerClass.trim();
        if (LEGACY_CLASSIC_ANALYZER.equals(className)) {
            logger.log(Level.WARNING, "├── ⚠️ deprecated analyzer class ''{0}'' ⇢ using ''{1}''"
                    + " - please update your configuration!", new Object[] { className, DEFAULT_ANALYZER });
            return DEFAULT_ANALYZER;
        }
        return className;
    }

    /**
     * Verifies that an existing index can be read by the current Lucene version.
     * Lucene only supports indices created by the current or the previous major
     * version. An index created by an older version (e.g. Lucene 7) can not be
     * opened and must be rebuilt.
     * <p>
     * The method does not delete an outdated index automatically. Instead it logs
     * a warning and throws an IndexException, so an administrator can decide how
     * to handle the existing index (e.g. backup, move or delete).
     *
     * @param directory - the index directory to be verified
     * @param path      - the path of the directory, used for logging
     * @throws IOException
     */
    private void verifyIndexFormat(Directory directory, String path) throws IOException {
        try {
            // reads only the latest commit point - a cheap operation
            SegmentInfos.readLatestCommit(directory);
        } catch (IndexFormatTooOldException e) {
            logger.log(Level.WARNING, "├── ⚠️ index directory ''{0}'' was created by an outdated Lucene version!",
                    path);
            logger.log(Level.WARNING, "│   ├── the index can not be read by Lucene {0}", Version.LATEST);
            logger.log(Level.WARNING, "│   ├── {0}", e.getMessage());
            logger.log(Level.WARNING, "│   └── 👉 remove or move the index directory ''{0}'' and the taxonomy"
                    + " directory - the index will be rebuilt automatically on next startup", path);
            throw new IndexException(IndexException.INVALID_INDEX,
                    "Index directory '" + path + "' was created by an outdated Lucene version", e);
        }
    }

    private void closeQuietly(AutoCloseable closeable, String name) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                logger.log(Level.WARNING, "│   ├── ⚠️ unable to close {0}: {1}", new Object[] { name, e.getMessage() });
            }
        }
    }
}