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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.KnnFloatVectorField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.KnnFloatVectorQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermInSetQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.BytesRef;
import org.imixs.workflow.engine.index.DefaultOperator;
import org.imixs.workflow.engine.index.SchemaService;
import org.imixs.workflow.exceptions.IndexException;
import org.imixs.workflow.exceptions.QueryException;
import org.imixs.workflow.index.core.LuceneIndexManager;
import org.imixs.workflow.index.core.LuceneSearchService;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

/**
 * The LuceneVectorIndexService provides methods to write and remove vectors in
 * the vector index. The service is independent from any embedding model or LLM
 * - it only stores the given vectors.
 * <p>
 * A business document can have multiple sets of vectors, one per category. A
 * category represents a business view of the document (e.g. 'risk' for a risk
 * assessment, 'contract' for a contract document). Indexing a category replaces
 * only the vectors of this category.
 * <p>
 * Each call is committed immediately. Creating embeddings takes much longer
 * than a commit, so a commit per document has no relevant impact. This also
 * allows a caller to remove a processed event log entry right after the call.
 * Because indexing a category is an atomic replace, processing an event twice
 * is harmless.
 * <p>
 * The method {@link #search} joins the vector index with the full-text index
 * by the $uniqueid. The full-text index provides the business filter and the
 * read access restriction, the vector index provides the similarity ranking.
 *
 * @see LuceneVectorIndexManager
 * @version 1.1
 * @author rsoika
 */
@Stateless
public class LuceneVectorIndexService {

    // internal key of a document category (uniqueid + category) - used for an
    // atomic replace of all vectors of one category
    static final String FIELD_KEY = "$vectorkey";

    // if the full-text filter matches at most this number of documents, the
    // matching ids are used as a pre-filter for the knn search (exact result).
    // Otherwise the knn result is post-filtered by the full-text index.
    static final int PRE_FILTER_THRESHOLD = 10000;

    // number of chunks fetched per requested document - a document can have
    // multiple chunks, so more chunks than documents must be fetched
    static final int CHUNK_OVERSAMPLING = 5;

    // additional factor for the post-filter strategy - some candidates are
    // removed by the full-text filter
    static final int POST_FILTER_OVERSAMPLING = 4;

    private static final Logger logger = Logger.getLogger(LuceneVectorIndexService.class.getName());

    @Inject
    private LuceneVectorIndexManager vectorIndexManager;

    @Inject
    private LuceneIndexManager luceneIndexManager;

    @Inject
    private SchemaService schemaService;

    /**
     * Replaces all vectors of the given document and category. If the list of
     * vectors is empty, the vectors of this category are removed.
     * <p>
     * The vectors are normalized to unit length before indexing. The given
     * arrays are not modified.
     *
     * @param uniqueId - the $uniqueid of the business document (required)
     * @param category - the category of the vectors (required)
     * @param vectors  - list of vectors, one per chunk. Each vector must match
     *                 the configured dimension.
     * @throws IllegalArgumentException if a parameter is invalid
     * @throws IndexException           if the index can not be updated
     */
    public void indexVectors(String uniqueId, String category, List<float[]> vectors) {
        validateRequired(uniqueId, "uniqueId");
        validateRequired(category, "category");
        if (vectors == null) {
            throw new IllegalArgumentException("vectors must not be null");
        }
        long l = System.currentTimeMillis();
        String key = buildKey(uniqueId, category);

        // build one lucene document per chunk
        List<Document> documents = new ArrayList<>();
        for (float[] vector : vectors) {
            Document doc = new Document();
            doc.add(new StringField(LuceneVectorIndexManager.FIELD_UNIQUEID, uniqueId, Store.YES));
            doc.add(new StringField(LuceneVectorIndexManager.FIELD_CATEGORY, category, Store.NO));
            doc.add(new StringField(FIELD_KEY, key, Store.NO));
            doc.add(new KnnFloatVectorField(LuceneVectorIndexManager.FIELD_EMBEDDING, normalize(vector),
                    LuceneVectorIndexManager.SIMILARITY));
            documents.add(doc);
        }

        try {
            IndexWriter writer = vectorIndexManager.getIndexWriter();
            // atomic replace of all vectors of this document and category
            writer.updateDocuments(new Term(FIELD_KEY, key), documents);
            vectorIndexManager.commit();
        } catch (IOException e) {
            logger.log(Level.WARNING, "├── ⚠️ unable to update vector index: {0}", e.getMessage());
            throw new IndexException(IndexException.INVALID_INDEX, "Unable to update vector index", e);
        }

        logger.log(Level.FINE, "├── ➕ {0} vector(s) indexed for ''{1}'' (category ''{2}'') ⇢ {3}ms",
                new Object[] { documents.size(), uniqueId, category, System.currentTimeMillis() - l });
    }

    /**
     * Removes all vectors of all categories of the given document.
     *
     * @param uniqueId - the $uniqueid of the business document (required)
     * @throws IndexException if the index can not be updated
     */
    public void removeVectors(String uniqueId) {
        validateRequired(uniqueId, "uniqueId");
        try {
            IndexWriter writer = vectorIndexManager.getIndexWriter();
            writer.deleteDocuments(new Term(LuceneVectorIndexManager.FIELD_UNIQUEID, uniqueId));
            vectorIndexManager.commit();
        } catch (IOException e) {
            logger.log(Level.WARNING, "├── ⚠️ unable to remove vectors: {0}", e.getMessage());
            throw new IndexException(IndexException.INVALID_INDEX, "Unable to remove vectors", e);
        }
        logger.log(Level.FINE, "├── ➖ vectors removed for ''{0}''", uniqueId);
    }

    /**
     * Returns the documents most similar to the given query vector. Each document
     * is returned only once (best matching chunk), sorted by similarity.
     * <p>
     * Only documents matching the given search term in the full-text index are
     * returned. The search term is extended by the read access restriction of the
     * current user, exactly as for a regular search. So the result contains only
     * documents the current user is allowed to read.
     * <p>
     * The join between both indices is done by one of two strategies:
     * <ul>
     * <li><b>pre-filter</b> - if the full-text filter matches at most
     * PRE_FILTER_THRESHOLD documents, the knn search is restricted to these
     * documents. The result is exact, even for very selective filters.</li>
     * <li><b>post-filter</b> - otherwise the knn search runs with an increased
     * number of candidates, which are verified against the full-text index. For
     * broad filters only a few candidates are removed.</li>
     * </ul>
     *
     * @param queryVector - the embedding of the query
     * @param categories  - list of categories to search in. null or empty = all
     *                    categories
     * @param searchTerm  - optional full-text search term (Lucene syntax). Empty =
     *                    all documents the user can read
     * @param maxResults  - maximum number of documents
     * @return list of matches, sorted by similarity (descending)
     * @throws QueryException if the search term is not understandable
     */
    public List<VectorMatch> search(float[] queryVector, List<String> categories, String searchTerm,
            int maxResults) throws QueryException {
        long l = System.currentTimeMillis();
        if (maxResults <= 0) {
            return new ArrayList<>();
        }
        float[] target = normalize(queryVector);

        // build the full-text filter incl. read access
        String term = (searchTerm == null || searchTerm.isBlank()) ? "*:*" : searchTerm;
        String extendedTerm = schemaService.getExtendedSearchTerm(term);

        DirectoryReader fullTextReader = null;
        DirectoryReader vectorReader = null;
        try {
            Query filterQuery = LuceneSearchService.createQueryParser(DefaultOperator.AND).parse(extendedTerm);
            fullTextReader = DirectoryReader.open(luceneIndexManager.getIndexDirectory());
            vectorReader = DirectoryReader.open(vectorIndexManager.getDirectory());
            IndexSearcher fullTextSearcher = new IndexSearcher(fullTextReader);
            IndexSearcher vectorSearcher = new IndexSearcher(vectorReader);

            int filterCount = fullTextSearcher.count(filterQuery);
            if (filterCount == 0) {
                return new ArrayList<>();
            }

            List<VectorMatch> result;
            boolean preFilter = filterCount <= PRE_FILTER_THRESHOLD;
            if (preFilter) {
                result = searchPreFiltered(fullTextSearcher, vectorSearcher, filterQuery, filterCount, target,
                        categories, maxResults);
            } else {
                result = searchPostFiltered(fullTextSearcher, vectorSearcher, filterQuery, target, categories,
                        maxResults);
            }

            logger.log(Level.FINE, "├── 🔎 vector search: {0} matches in {1}ms ({2}, filter={3} documents)",
                    new Object[] { result.size(), System.currentTimeMillis() - l,
                            preFilter ? "pre-filter" : "post-filter", filterCount });
            return result;

        } catch (ParseException e) {
            throw new QueryException(QueryException.QUERY_NOT_UNDERSTANDABLE, e.getMessage(), e);
        } catch (IOException e) {
            logger.log(Level.WARNING, "├── ⚠️ vector search failed: {0}", e.getMessage());
            throw new IndexException(IndexException.INVALID_INDEX, "Vector search failed", e);
        } finally {
            closeQuietly(fullTextReader);
            closeQuietly(vectorReader);
        }
    }

    /**
     * Pre-filter strategy: collects the ids of all documents matching the
     * full-text filter and restricts the knn search to these documents.
     */
    private List<VectorMatch> searchPreFiltered(IndexSearcher fullTextSearcher, IndexSearcher vectorSearcher,
            Query filterQuery, int filterCount, float[] target, List<String> categories, int maxResults)
            throws IOException {
        // collect the ids of all matching documents
        TopDocs filterHits = fullTextSearcher.search(filterQuery, filterCount);
        StoredFields storedFields = fullTextSearcher.storedFields();
        List<BytesRef> ids = new ArrayList<>();
        for (ScoreDoc hit : filterHits.scoreDocs) {
            String id = storedFields.document(hit.doc).get(LuceneVectorIndexManager.FIELD_UNIQUEID);
            if (id != null) {
                ids.add(new BytesRef(id));
            }
        }

        BooleanQuery.Builder vectorFilter = new BooleanQuery.Builder();
        vectorFilter.add(new TermInSetQuery(LuceneVectorIndexManager.FIELD_UNIQUEID, ids), Occur.FILTER);
        Query categoryFilter = buildCategoryFilter(categories);
        if (categoryFilter != null) {
            vectorFilter.add(categoryFilter, Occur.FILTER);
        }

        Map<String, Float> candidates = searchVectors(vectorSearcher, target, maxResults * CHUNK_OVERSAMPLING,
                vectorFilter.build());
        return toMatches(candidates, null, maxResults);
    }

    /**
     * Post-filter strategy: runs the knn search with an increased number of
     * candidates and verifies the candidates against the full-text filter.
     */
    private List<VectorMatch> searchPostFiltered(IndexSearcher fullTextSearcher, IndexSearcher vectorSearcher,
            Query filterQuery, float[] target, List<String> categories, int maxResults) throws IOException {
        Map<String, Float> candidates = searchVectors(vectorSearcher, target,
                maxResults * CHUNK_OVERSAMPLING * POST_FILTER_OVERSAMPLING, buildCategoryFilter(categories));
        if (candidates.isEmpty()) {
            return new ArrayList<>();
        }

        // verify the candidates against the full-text filter
        List<BytesRef> ids = new ArrayList<>();
        for (String id : candidates.keySet()) {
            ids.add(new BytesRef(id));
        }
        BooleanQuery verifyQuery = new BooleanQuery.Builder()
                .add(filterQuery, Occur.MUST)
                .add(new TermInSetQuery(LuceneVectorIndexManager.FIELD_UNIQUEID, ids), Occur.FILTER)
                .build();
        TopDocs verified = fullTextSearcher.search(verifyQuery, candidates.size());
        StoredFields storedFields = fullTextSearcher.storedFields();
        Set<String> allowed = new HashSet<>();
        for (ScoreDoc hit : verified.scoreDocs) {
            allowed.add(storedFields.document(hit.doc).get(LuceneVectorIndexManager.FIELD_UNIQUEID));
        }
        return toMatches(candidates, allowed, maxResults);
    }

    /**
     * Runs the knn search and returns the best score per document, in descending
     * order. The knn result is sorted by score, so the first chunk found for a
     * document is its best chunk.
     */
    private Map<String, Float> searchVectors(IndexSearcher vectorSearcher, float[] target, int k, Query filter)
            throws IOException {
        Query knnQuery = new KnnFloatVectorQuery(LuceneVectorIndexManager.FIELD_EMBEDDING, target, k, filter);
        TopDocs hits = vectorSearcher.search(knnQuery, k);
        StoredFields storedFields = vectorSearcher.storedFields();
        Map<String, Float> bestScores = new LinkedHashMap<>();
        for (ScoreDoc hit : hits.scoreDocs) {
            String id = storedFields.document(hit.doc).get(LuceneVectorIndexManager.FIELD_UNIQUEID);
            bestScores.putIfAbsent(id, hit.score);
        }
        return bestScores;
    }

    /**
     * Converts the candidates into the result list. Lucene scores DOT_PRODUCT for
     * float vectors as (1 + dot) / 2 - this is converted back into the cosine
     * similarity.
     */
    private List<VectorMatch> toMatches(Map<String, Float> candidates, Set<String> allowed, int maxResults) {
        List<VectorMatch> result = new ArrayList<>();
        for (Map.Entry<String, Float> entry : candidates.entrySet()) {
            if (allowed != null && !allowed.contains(entry.getKey())) {
                continue;
            }
            result.add(new VectorMatch(entry.getKey(), 2 * entry.getValue() - 1));
            if (result.size() >= maxResults) {
                break;
            }
        }
        return result;
    }

    /**
     * Returns a filter for the given categories, or null for all categories.
     */
    private static Query buildCategoryFilter(List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return null;
        }
        List<BytesRef> terms = new ArrayList<>();
        for (String category : categories) {
            if (category != null && !category.isBlank()) {
                terms.add(new BytesRef(category));
            }
        }
        return terms.isEmpty() ? null : new TermInSetQuery(LuceneVectorIndexManager.FIELD_CATEGORY, terms);
    }

    private static void closeQuietly(DirectoryReader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "├── ⚠️ unable to close index reader: {0}", e.getMessage());
            }
        }
    }

    /**
     * Returns a normalized copy of the given vector (unit length). The vector
     * must match the configured dimension and must not be a zero vector.
     * <p>
     * For normalized vectors the dot product equals the cosine similarity.
     */
    float[] normalize(float[] vector) {
        if (vector == null || vector.length != vectorIndexManager.getDimension()) {
            throw new IllegalArgumentException("invalid vector dimension: expected "
                    + vectorIndexManager.getDimension() + " but was " + (vector == null ? "null" : vector.length));
        }
        double sum = 0;
        for (float v : vector) {
            sum += v * v;
        }
        double norm = Math.sqrt(sum);
        if (norm == 0) {
            throw new IllegalArgumentException("invalid vector: zero vector can not be normalized");
        }
        float[] result = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            result[i] = (float) (vector[i] / norm);
        }
        return result;
    }

    private static String buildKey(String uniqueId, String category) {
        return uniqueId + "|" + category;
    }

    private static void validateRequired(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}