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

package org.imixs.workflow.index.core;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsCollectorManager;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldCollectorManager;
import org.apache.lucene.search.TopScoreDocCollectorManager;
import org.apache.lucene.store.Directory;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.index.Category;
import org.imixs.workflow.engine.index.DefaultOperator;
import org.imixs.workflow.engine.index.SchemaService;
import org.imixs.workflow.engine.index.SearchService;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.QueryException;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

/**
 * This session ejb provides a service to search the lucene index. The EJB uses
 * the IndexSearcher to query the current index. As the index can change across
 * multiple searches we can not share a single IndexSearcher instance. For that
 * reason the EJB is creating a new IndexSearch per-search.
 *
 * The service provides a set of public methods which can be used to query
 * workitems or collections of workitems. A search term can be escaped by
 * calling the method <code>escpeSearchTerm</code>. This method prepends a
 * <code>\</code> for those characters that QueryParser expects to be escaped.
 *
 * @version 3.0
 * @author rsoika
 */
@DeclareRoles({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
        "org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
        "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RolesAllowed({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
        "org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
        "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
public class LuceneSearchService implements SearchService {

    public static final int DEFAULT_MAX_SEARCH_RESULT = 9999; // limiting the total number of hits
    public static final int DEFAULT_PAGE_SIZE = 100; // default docs in one page

    @Inject
    private LuceneIndexService luceneIndexService;

    @Inject
    private LuceneIndexManager luceneIndexManager;

    @Inject
    private DocumentService documentService;

    @Inject
    private SchemaService schemaService;

    private static final Logger logger = Logger.getLogger(LuceneSearchService.class.getName());

    /**
     * Returns a collection of documents matching the provided search term. The term
     * will be extended with the current users roles to test the read access level
     * of each workitem matching the search term.
     * <p>
     * The optional param 'searchOrder' can be set to force lucene to sort the
     * search result by any search order.
     * <p>
     * The optional param 'defaultOperator' can be set to Operator.AND
     * <p>
     * The optional param 'stubs' indicates if the full Imixs Document should be
     * loaded or if only the data fields stored in the lucedn index will be return.
     * The later is the faster method but returns only document stubs.
     *
     * @param searchTerm
     * @param pageSize        - docs per page
     * @param pageIndex       - page number
     * @param sortOrder
     * @param defaultOperator - optional to change the default search operator
     * @param loadStubs       - optional indicates of only the lucene document
     *                        should be returned.
     * @return collection of search result
     *
     * @throws QueryException in case the searchtem is not understandable.
     */
    @Override
    public List<ItemCollection> search(String searchTerm, int pageSize, int pageIndex,
            org.imixs.workflow.engine.index.SortOrder sortOrder, DefaultOperator defaultOperator, boolean loadStubs)
            throws QueryException {

        long ltime = System.currentTimeMillis();

        if (pageSize <= 0) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        if (pageIndex < 0) {
            pageIndex = 0;
        }

        ArrayList<ItemCollection> workitems = new ArrayList<ItemCollection>();

        searchTerm = schemaService.getExtendedSearchTerm(searchTerm);
        // test if searchtem is provided
        if (searchTerm == null || "".equals(searchTerm)) {
            return workitems;
        }

        logger.log(Level.FINEST, "├── 🔍 search: pageIndex={0} pageSize={1}", new Object[] { pageIndex, pageSize });

        IndexSearcher searcher = null;
        try {
            searcher = createIndexSearcher();
            QueryParser parser = createQueryParser(defaultOperator);
            parser.setAllowLeadingWildcard(true);

            int startIndex = pageIndex * pageSize;

            // test it pageindex is above the DEFAULT_MAX_SEARCH_RESULT
            // if the pageindex is above the method will extend the
            // maxSearchResult by 3*pageSize. This behavior is than
            // simmilar to the google search which is also adjusting the
            // search scope after paging.
            int maxSearchResult = DEFAULT_MAX_SEARCH_RESULT;
            if ((startIndex + pageSize) > DEFAULT_MAX_SEARCH_RESULT) {
                // adjust maxSearchResult
                maxSearchResult = startIndex + (3 * pageSize);
                logger.log(Level.WARNING, "│   ├── ⚠️ page {0}x{1} exceeded DEFAULT_MAX_SEARCH_RESULT ({2})"
                        + " ⇢ MAX_SEARCH_RESULT set to {3}",
                        new Object[] { pageSize, pageIndex, DEFAULT_MAX_SEARCH_RESULT, maxSearchResult });
            }

            Query query = parser.parse(searchTerm);

            // Lucene 10: IndexSearcher.search(Query, Collector) is deprecated - we use a
            // CollectorManager instead. The manager returns the top 'maxSearchResult'
            // hits; total hits are counted accurately up to the same threshold.
            long lsearchtime = System.currentTimeMillis();
            TopDocs topDocs;
            if (sortOrder != null) {
                logger.log(Level.FINEST, "│   ├── sorted by sortOrder ''{0}''", sortOrder);
                topDocs = searcher.search(query,
                        new TopFieldCollectorManager(buildLuceneSort(sortOrder), maxSearchResult, maxSearchResult));
            } else {
                logger.finest("│   ├── sorted by score");
                topDocs = searcher.search(query, new TopScoreDocCollectorManager(maxSearchResult, maxSearchResult));
            }

            // Lucene 10: collector.topDocs(start, howMany) is no longer available after
            // the search - we cut out the requested page from the result array.
            ScoreDoc[] allHits = topDocs.scoreDocs;
            int endIndex = Math.min(startIndex + pageSize, allHits.length);

            // Lucene 10: TotalHits is a record - use the accessor value()
            logger.log(Level.FINEST, "│   ├── returned {0} documents in {1}ms - total hits={2}",
                    new Object[] { Math.max(0, endIndex - startIndex), System.currentTimeMillis() - lsearchtime,
                            topDocs.totalHits.value() });

            // Lucene 10: IndexSearcher.doc(int) was removed - stored fields are
            // accessed via a StoredFields instance
            StoredFields storedFields = searcher.storedFields();
            SimpleDateFormat luceneDateformat = new SimpleDateFormat("yyyyMMddHHmmss");
            for (int i = startIndex; i < endIndex; i++) {
                Document luceneDoc = storedFields.document(allHits[i].doc);

                String sID = luceneDoc.get(WorkflowKernel.UNIQUEID);
                ItemCollection imixsDoc = null;
                if (loadStubs) {
                    // return only the fields form the Lucene document
                    imixsDoc = convertLuceneDocument(luceneDoc, luceneDateformat);
                    imixsDoc.replaceItemValue(WorkflowKernel.UNIQUEID, sID);
                } else {
                    // load the full imixs document from the database
                    imixsDoc = documentService.load(sID);
                }

                if (imixsDoc != null) {
                    workitems.add(imixsDoc);
                } else {
                    logger.log(Level.WARNING, "│   ├── ⚠️ index returned unreadable document ''{0}''", sID);
                    documentService.removeDocumentFromIndex(sID);
                    // this situation happens if the search index returned
                    // documents the current user has no read access.
                    // this should normally avoided with the $readaccess
                    // search phrase! So if this happens we need to check
                    // the createDocument method!
                }
            }

            logger.log(Level.FINE, "│   └── ✅ search result computed in {0}ms - loadStubs={1}",
                    new Object[] { System.currentTimeMillis() - ltime, loadStubs });

        } catch (IOException e) {
            logger.log(Level.SEVERE, "├── ❌ index error: {0}", e.getMessage());
            throw new InvalidAccessException(InvalidAccessException.INVALID_INDEX, e.getMessage(), e);
        } catch (ParseException e) {
            logger.log(Level.SEVERE, "├── ❌ search error: {0}", e.getMessage());
            throw new QueryException(QueryException.QUERY_NOT_UNDERSTANDABLE, e.getMessage(), e);
        } finally {
            closeIndexSearcher(searcher);
        }

        return workitems;
    }

    @Override
    public List<Category> getTaxonomy(String... categories) {
        return getTaxonomyByQuery(null, categories);
    }

    @Override
    public List<Category> getTaxonomyByQuery(String searchTerm, String... categories) {
        List<Category> results = new ArrayList<>();
        IndexSearcher searcher = null;
        TaxonomyReader taxoReader = null;
        try {
            searcher = createIndexSearcher();
            taxoReader = createTaxonomyReader();
            FacetsConfig config = luceneIndexService.getFacetsConfig();

            // MatchAllDocsQuery is for "browsing" (counts facets
            // for all non-deleted docs in the index); normally
            // you'd use a "normal" query:
            Query query;
            if (searchTerm == null || searchTerm.isEmpty()) {
                query = new MatchAllDocsQuery();
            } else {
                searchTerm = schemaService.getExtendedSearchTerm(searchTerm);
                QueryParser parser = createQueryParser(DefaultOperator.OR);
                query = parser.parse(searchTerm);
            }
            // Lucene 10: the FacetsCollector is created by a FacetsCollectorManager
            FacetsCollector fc = searcher.search(query, new FacetsCollectorManager());
            Facets facets = new FastTaxonomyFacetCounts(taxoReader, config, fc);

            // count each result
            for (String cat : categories) {
                // Count the dimensions (we use a index field prefix to avoid conflicts with
                // existing indices.
                FacetResult facetResult = facets.getTopChildren(10,
                        cat + LuceneIndexService.TAXONOMY_INDEXFIELD_PRAFIX);
                if (facetResult != null) {
                    Category category = new Category(cat, facetResult.childCount);
                    for (LabelAndValue lav : facetResult.labelValues) {
                        category.setLabel(lav.label, lav.value.intValue());
                    }
                    results.add(category);
                }
            }
        } catch (IOException | QueryException | ParseException e) {
            logger.log(Level.SEVERE, "├── ❌ index error: {0}", e.getMessage());
            throw new InvalidAccessException(InvalidAccessException.INVALID_INDEX, e.getMessage(), e);
        } finally {
            closeIndexSearcher(searcher);
            if (taxoReader != null) {
                try {
                    taxoReader.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "├── ⚠️ unable to close taxonomy reader: {0}", e.getMessage());
                }
            }
        }
        return results;
    }

    /**
     * Returns the total hits for a given search term from the lucene index. The
     * method did not load any data. The provided search term will we extended with
     * a users roles to test the read access level of each workitem matching the
     * search term.
     * <p>
     * Lucene 10: The hit count is computed by IndexSearcher.count(), which counts
     * all matching documents exactly - the same result as the Lucene 7
     * implementation. The param 'maxResult' is no longer needed for counting but
     * kept for API compatibility.
     *
     * @see search(String, int, int, Sort, Operator)
     *
     * @param sSearchTerm
     * @param maxResult   - max search result (not used)
     * @return total hits of search result
     * @throws QueryException in case the searchterm is not understandable.
     */
    @Override
    public int getTotalHits(final String _searchTerm, final int _maxResult, final DefaultOperator defaultOperator)
            throws QueryException {
        int result;

        String sSearchTerm = schemaService.getExtendedSearchTerm(_searchTerm);
        // test if searchtem is provided
        if (sSearchTerm == null || "".equals(sSearchTerm)) {
            return 0;
        }

        IndexSearcher searcher = null;
        try {
            searcher = createIndexSearcher();
            QueryParser parser = createQueryParser(defaultOperator);
            parser.setAllowLeadingWildcard(true);

            Query query = parser.parse(sSearchTerm);
            // Lucene 10: count() is the most efficient way to get an exact hit count
            result = searcher.count(query);

            logger.log(Level.FINEST, "├── 🔢 count result = {0}", result);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "├── ❌ index error: {0}", e.getMessage());
            throw new InvalidAccessException(InvalidAccessException.INVALID_INDEX, e.getMessage(), e);
        } catch (ParseException e) {
            logger.log(Level.SEVERE, "├── ❌ search error: {0}", e.getMessage());
            throw new QueryException(QueryException.QUERY_NOT_UNDERSTANDABLE, e.getMessage(), e);
        } finally {
            closeIndexSearcher(searcher);
        }

        return result;
    }

    /**
     * Returns a IndexSearcher instance based on the last commit point of the
     * index. The index itself is created by the LuceneIndexManager on startup.
     *
     * @return IndexSearcher
     * @throws IOException
     */
    IndexSearcher createIndexSearcher() throws IOException {
        IndexReader reader = null;
        // the directory is provided by the LuceneIndexManager
        Directory indexDir = luceneIndexManager.getIndexDirectory();
        try {
            // if the index dose not yet exits we got a IO Exception (issue #329)
            reader = DirectoryReader.open(indexDir);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "├── ⚠️ index can not be opened: {0}", ioe.getMessage());
            // throw the origin exception....
            throw ioe;
        }
        return new IndexSearcher(reader);
    }

    /**
     * Closes the IndexReader of a given IndexSearcher. The method is null-safe and
     * is called in a finally block to avoid open readers in case of an exception.
     *
     * @param searcher - the IndexSearcher to be closed, can be null
     */
    private void closeIndexSearcher(IndexSearcher searcher) {
        if (searcher != null) {
            try {
                searcher.getIndexReader().close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "├── ⚠️ unable to close index reader: {0}", e.getMessage());
            }
        }
    }

    /**
     * Returns a TaxonomyReader instance.
     *
     * @return TaxonomyReader
     * @throws IOException
     */
    TaxonomyReader createTaxonomyReader() throws IOException {
        // the directory is provided by the LuceneIndexManager
        Directory taxoDir = luceneIndexManager.getTaxonomyDirectory();
        return new DirectoryTaxonomyReader(taxoDir);
    }

    /**
     * Returns in instance of a QueyParser based on a KeywordAnalyzer. The method
     * set the lucene DefaultOperator to 'OR' if not specified otherwise.
     *
     * @see issue #28 - normalizeSearchTerm
     * @param defaultOperator
     * @return QueryParser
     */
    public static QueryParser createQueryParser(DefaultOperator defaultOperator) {
        // use the keywordAnalyzer for searching a search term.
        QueryParser parser = new QueryParser("content", new KeywordAnalyzer());
        if (defaultOperator == DefaultOperator.OR) {
            parser.setDefaultOperator(QueryParser.Operator.OR);
        } else {
            parser.setDefaultOperator(QueryParser.Operator.AND);
        }

        // set setSplitOnWhitespace (issue #438)
        // we do no longer support a config parameter here!
        parser.setSplitOnWhitespace(true);
        return parser;
    }

    /**
     * This method converts a LuceneDocument into a ItemCollection with all stored
     * index fields from the Lucene document.
     *
     * @param luceneDoc
     * @return ItemCollection representing the Lucene Document
     */
    ItemCollection convertLuceneDocument(Document luceneDoc, SimpleDateFormat luceneDateformat) {
        ItemCollection imixsDoc = new ItemCollection();

        List<IndexableField> fields = luceneDoc.getFields();
        for (IndexableField indexableField : fields) {

            Object objectValue = null;
            String stringValue = indexableField.stringValue();
            // check for numbers....
            if (isNumeric(stringValue)) {
                // is date?
                if (stringValue.length() == 14 && !stringValue.contains(".")) {
                    try {
                        objectValue = luceneDateformat.parse(stringValue);
                    } catch (java.text.ParseException e) {
                        // no date!
                    }
                }
                // lets see if it is a number..?
                if (objectValue == null) {
                    try {
                        Number number = NumberFormat.getInstance().parse(stringValue);
                        objectValue = number;
                    } catch (java.text.ParseException e) {
                        // no number - should not happen
                    }
                }
            }
            if (objectValue == null) {
                objectValue = stringValue;
            }
            logger.log(Level.FINEST, "│   │   ├── {0} = {1}", new Object[] { indexableField.name(), objectValue });
            imixsDoc.appendItemValue(indexableField.name(), objectValue);
        }

        // compute $isAuthor flag...
        imixsDoc.replaceItemValue(DocumentService.ISAUTHOR, documentService.isAuthor(imixsDoc));

        return imixsDoc;
    }

    private Sort buildLuceneSort(org.imixs.workflow.engine.index.SortOrder sortOrder) {
        // we do not support multi values here - see
        // LuceneIndexService.addItemValues
        // it would be possible if we use a SortedSetSortField class here
        return new Sort(
                new SortField[] { new SortField(sortOrder.getField(), SortField.Type.STRING, sortOrder.isReverse()) });
    }

    /**
     * Helper method to check for numbers.
     *
     * @see https://stackoverflow.com/questions/1102891/how-to-check-if-a-string-is-numeric-in-java
     * @param str
     * @return
     */
    private static boolean isNumeric(String str) {
        boolean dot = false;
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (char c : str.toCharArray()) {
            if (c == '.' && dot == false) {
                dot = true; // first dot!
                continue;
            }
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

}