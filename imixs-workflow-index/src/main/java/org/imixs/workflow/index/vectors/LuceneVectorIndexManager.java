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

package org.imixs.workflow.index.vectors;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.workflow.exceptions.IndexException;
import org.imixs.workflow.index.core.LuceneIndexManager;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;

/**
 * The LuceneVectorIndexManager owns the lifecycle of the optional vector index.
 * It is the counterpart of the {@link LuceneIndexManager} and holds one
 * long-lived IndexWriter for the vector index.
 * <p>
 * The vector index is a pure similarity index. Each Lucene document represents
 * one chunk of a business document and contains only:
 * <ul>
 * <li>{@code $uniqueid} - the id of the business document</li>
 * <li>{@code category} - the business view of the embeddings (e.g. 'risk',
 * 'contract')</li>
 * <li>{@code embedding} - the vector</li>
 * </ul>
 * All business data, metadata and read access information are provided by the
 * full-text index. Both indices are joined by the {@code $uniqueid}.
 * <p>
 * The vector index is disabled by default. It is enabled by the property
 * {@code index.vector.enabled=true}. In this case the property
 * {@code index.vector.dimension} is required. The index is stored in the
 * directory {@code <index.directory>_vector}.
 * <p>
 * There is no rebuild job for the vector index. Embeddings are created by
 * workflow events only. A new vector index is simply created empty.
 * <p>
 * Vectors are normalized to unit length before indexing. This allows to use
 * the fast DOT_PRODUCT similarity, which is equivalent to the cosine similarity
 * for normalized vectors.
 *
 * @version 1.0
 * @author rsoika
 */
@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class LuceneVectorIndexManager {

    public static final String CONFIG_VECTOR_ENABLED = "index.vector.enabled";
    public static final String CONFIG_VECTOR_DIMENSION = "index.vector.dimension";

    public static final String VECTOR_DIRECTORY_SUFFIX = "_vector";

    public static final String FIELD_UNIQUEID = "$uniqueid";
    public static final String FIELD_CATEGORY = "category";
    public static final String FIELD_EMBEDDING = "embedding";

    // vectors are normalized, so DOT_PRODUCT equals the cosine similarity
    public static final VectorSimilarityFunction SIMILARITY = VectorSimilarityFunction.DOT_PRODUCT;

    private static final Logger logger = Logger.getLogger(LuceneVectorIndexManager.class.getName());

    @Inject
    @ConfigProperty(name = CONFIG_VECTOR_ENABLED, defaultValue = "false")
    private boolean vectorEnabled;

    @Inject
    @ConfigProperty(name = CONFIG_VECTOR_DIMENSION)
    private Optional<Integer> vectorDimensionConfig;

    @Inject
    private LuceneIndexManager luceneIndexManager;

    private int dimension;
    private String vectorPath;
    private Directory vectorDirectory;
    private IndexWriter indexWriter;

    /**
     * Verifies the configuration and opens the vector index if enabled. A
     * misconfiguration or an incompatible index aborts the deployment.
     */
    @PostConstruct
    void init() {
        if (!vectorEnabled) {
            logger.log(Level.FINE, "├── vector index disabled");
            return;
        }

        // the dimension is required if the vector index is enabled
        dimension = vectorDimensionConfig.orElse(0);
        if (dimension <= 0) {
            logger.log(Level.SEVERE, "├── ❌ invalid vector index configuration!");
            logger.log(Level.SEVERE, "│   ├── missing or invalid property ''{0}'' (environment variable"
                    + " ''INDEX_VECTOR_DIMENSION'')", CONFIG_VECTOR_DIMENSION);
            logger.log(Level.SEVERE, "│   └── 👉 deployment aborted - please fix the configuration");
            throw new IndexException(IndexException.INVALID_INDEX,
                    "Invalid vector index configuration: missing property '" + CONFIG_VECTOR_DIMENSION + "'");
        }

        // the vector index is stored next to the full-text index
        vectorPath = luceneIndexManager.getIndexPath() + VECTOR_DIRECTORY_SUFFIX;
        logger.log(Level.INFO, "├── 📂 opening vector index ''{0}'' (dimension={1})...",
                new Object[] { vectorPath, dimension });
        try {
            vectorDirectory = FSDirectory.open(Paths.get(vectorPath));
            if (DirectoryReader.indexExists(vectorDirectory)) {
                LuceneIndexManager.verifyIndexFormat(vectorDirectory, vectorPath);
                verifyDimension();
            }

            indexWriter = createIndexWriter();
            // commit once, so an empty index can be opened by readers immediately
            indexWriter.commit();
            logger.log(Level.INFO, "│   └── ✅ vector index ready");
        } catch (IOException e) {
            throw new IndexException(IndexException.INVALID_INDEX,
                    "Unable to open vector index: " + e.getMessage(), e);
        }
    }

    /**
     * Commits pending changes and closes the writer and directory on shutdown.
     */
    @PreDestroy
    void close() {
        if (!vectorEnabled) {
            return;
        }
        logger.log(Level.INFO, "├── 📂 closing vector index...");
        try {
            commit();
        } catch (IOException e) {
            logger.log(Level.WARNING, "│   ├── ⚠️ unable to commit vector index on shutdown: {0}", e.getMessage());
        }
        closeQuietly(indexWriter, "IndexWriter");
        closeQuietly(vectorDirectory, "vector directory");
        logger.log(Level.INFO, "│   └── ✅ vector index closed");
    }

    /**
     * Returns true if the vector index is enabled.
     */
    public boolean isEnabled() {
        return vectorEnabled;
    }

    /**
     * Returns the configured vector dimension.
     */
    public int getDimension() {
        return dimension;
    }

    /**
     * Returns the shared IndexWriter of the vector index. If the writer was closed
     * due to a tragic event (e.g. an IO error), a new writer is opened.
     *
     * @return IndexWriter
     * @throws IOException
     */
    public synchronized IndexWriter getIndexWriter() throws IOException {
        assertEnabled();
        if (indexWriter == null || !indexWriter.isOpen()) {
            logger.log(Level.WARNING, "├── ⚠️ vector IndexWriter is closed ⇢ reopening...");
            if (indexWriter != null && indexWriter.getTragicException() != null) {
                logger.log(Level.WARNING, "│   └── cause: {0}", indexWriter.getTragicException().getMessage());
            }
            indexWriter = createIndexWriter();
        }
        return indexWriter;
    }

    /**
     * Persists all pending changes of the vector index.
     *
     * @throws IOException
     */
    public synchronized void commit() throws IOException {
        assertEnabled();
        long l = System.currentTimeMillis();
        getIndexWriter().commit();
        logger.log(Level.FINEST, "│   ├── 💾 vector index committed in {0}ms", System.currentTimeMillis() - l);
    }

    public Directory getDirectory() {
        assertEnabled();
        return vectorDirectory;
    }

    /**
     * Verifies that the dimension of an existing vector index matches the
     * configured dimension. A different dimension means that a different
     * embedding model was used - the existing vectors can not be compared with
     * new vectors.
     */
    private void verifyDimension() throws IOException {
        try (DirectoryReader reader = DirectoryReader.open(vectorDirectory)) {
            FieldInfo fieldInfo = FieldInfos.getMergedFieldInfos(reader).fieldInfo(FIELD_EMBEDDING);
            if (fieldInfo != null && fieldInfo.getVectorDimension() != dimension) {
                logger.log(Level.SEVERE, "├── ❌ vector index ''{0}'' has dimension {1}, but {2} is configured!",
                        new Object[] { vectorPath, fieldInfo.getVectorDimension(), dimension });
                logger.log(Level.SEVERE, "│   └── 👉 the embedding model was changed - remove the vector index"
                        + " directory and re-index the documents");
                throw new IndexException(IndexException.INVALID_INDEX,
                        "Vector index dimension mismatch: index=" + fieldInfo.getVectorDimension()
                                + " configured=" + dimension);
            }
        }
    }

    private IndexWriter createIndexWriter() throws IOException {
        // no analyzer needed - the vector index contains only keyword and vector fields
        IndexWriterConfig config = new IndexWriterConfig();
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        logger.finest("│   ├── open vector IndexWriter");
        return new IndexWriter(vectorDirectory, config);
    }

    private void assertEnabled() {
        if (!vectorEnabled) {
            throw new IndexException(IndexException.INVALID_INDEX,
                    "Vector index is disabled - set '" + CONFIG_VECTOR_ENABLED + "=true'");
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