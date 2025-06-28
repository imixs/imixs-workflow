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

package org.imixs.workflow.engine.lucene;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopScoreDocCollector;
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

import jakarta.ejb.Stateless;

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
 * @see http://stackoverflow.com/questions/34880347/why-did-lucene-indexwriter-
 *      did-not-update-the-index-when-called-from-a-web-modul
 * @version 2.0
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

    public static final int DEFAULT_MAX_SEARCH_RESULT = 9999; // limiting the
                                                              // total
    // number of hits
    public static final int DEFAULT_PAGE_SIZE = 100; // default docs in one page

    @Inject
    private LuceneIndexService luceneIndexService;

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

        boolean debug = logger.isLoggable(Level.FINE);
        long ltime = System.currentTimeMillis();

        // flush eventlog (see issue #411)
        // flush();

        // see issue #382
        /*
         * if (sSearchTerm.toLowerCase().contains("$processid")) { logger.
         * warning("The field $processid is deprecated. Please use $taskid instead. " +
         * "searching a workitem with an deprecated $processid is still supported."); }
         */

        if (pageSize <= 0) {
            pageSize = DEFAULT_PAGE_SIZE;
        }

        if (pageIndex < 0) {
            pageIndex = 0;
        }

        if (debug) {
            logger.log(Level.FINEST, "......lucene search: pageNumber={0} pageSize={1}", new Object[]{pageIndex, pageSize});
        }
        ArrayList<ItemCollection> workitems = new ArrayList<ItemCollection>();

        searchTerm = schemaService.getExtendedSearchTerm(searchTerm);
        // test if searchtem is provided
        if (searchTerm == null || "".equals(searchTerm)) {
            return workitems;
        }

        try {
            IndexSearcher searcher = createIndexSearcher();
            QueryParser parser = createQueryParser(defaultOperator);

            parser.setAllowLeadingWildcard(true);

            // set default operator?
            if (defaultOperator == DefaultOperator.OR) {
                parser.setDefaultOperator(org.apache.lucene.queryparser.classic.QueryParser.Operator.OR);
            } else {
                parser.setDefaultOperator(org.apache.lucene.queryparser.classic.QueryParser.Operator.AND);
            }

            long lsearchtime = System.currentTimeMillis();
            TopDocs topDocs = null;
            TopDocsCollector<?> collector = null;
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
                logger.log(Level.WARNING, "PageIndex ({0}x{1}) exeeded DEFAULT_MAX_SEARCH_RESULT({2}) ->"
                        + " new MAX_SEARCH_RESULT is set to {3}",
                        new Object[]{pageSize, pageIndex, DEFAULT_MAX_SEARCH_RESULT, maxSearchResult});
            }

            Query query = parser.parse(searchTerm);
            if (sortOrder != null) {
                // sorted by sortoder
                if (debug) {
                    logger.log(Level.FINEST, "......lucene result sorted by sortOrder= ''{0}'' ", sortOrder);
                }
                // MAX_SEARCH_RESULT is limiting the total number of hits
                collector = TopFieldCollector.create(buildLuceneSort(sortOrder), maxSearchResult, false, false, false,
                        false);

            } else {
                // sorted by score
                if (debug) {
                    logger.finest("......lucene result sorted by score ");
                }
                // MAX_SEARCH_RESULT is limiting the total number of hits
                collector = TopScoreDocCollector.create(maxSearchResult);
            }

            // - ignore time limiting for now
            // Counter clock = Counter.newCounter(true);
            // TimeLimitingCollector timeLimitingCollector = new
            // TimeLimitingCollector(collector, clock, 10);

            // start search....
            searcher.search(query, collector);

            // get one page
            topDocs = collector.topDocs(startIndex, pageSize);
            // Get an array of references to matched documents
            ScoreDoc[] scoreDosArray = topDocs.scoreDocs;

            if (debug) {
                logger.log(Level.FINEST, "...returned {0} documents in {1} ms - total hits={2}",
                        new Object[]{scoreDosArray.length, System.currentTimeMillis() - lsearchtime, topDocs.totalHits});
            }
            SimpleDateFormat luceneDateformat = new SimpleDateFormat("yyyyMMddHHmmss");
            for (ScoreDoc scoredoc : scoreDosArray) {
                // Retrieve the matched document and show relevant details
                Document luceneDoc = searcher.doc(scoredoc.doc);

                String sID = luceneDoc.get(WorkflowKernel.UNIQUEID);
                ItemCollection imixsDoc = null;
                if (loadStubs) {
                    // return only the fields form the Lucene document
                    imixsDoc = convertLuceneDocument(luceneDoc, luceneDateformat);
                    imixsDoc.replaceItemValue(WorkflowKernel.UNIQUEID, sID);
                } else {
                    // load the full imixs document from the database
                    logger.log(Level.FINEST, "......lucene lookup $uniqueid={0}", sID);
                    imixsDoc = documentService.load(sID);
                }

                if (imixsDoc != null) {
                    workitems.add(imixsDoc);
                } else {
                    logger.log(Level.WARNING, "lucene index returned unreadable workitem : {0}", sID);
                    documentService.removeDocumentFromIndex(sID);
                    // this situation happens if the search index returned
                    // documents the current user has no read access.
                    // this should normally avoided with the $readaccess
                    // search phrase! So if this happens we need to check
                    // the createDocument method!
                }
            }

            searcher.getIndexReader().close();

            if (debug) {
                logger.log(Level.FINE, "...search result computed in {0} ms - loadStubs={1}",
                        new Object[]{System.currentTimeMillis() - ltime, loadStubs});
            }
        } catch (IOException e) {
            // in case of an IOException we just print an error message and
            // return an empty result
            logger.log(Level.SEVERE, "Lucene index error: {0}", e.getMessage());
            throw new InvalidAccessException(InvalidAccessException.INVALID_INDEX, e.getMessage(), e);
        } catch (ParseException e) {
            logger.log(Level.SEVERE, "Lucene search error: {0}", e.getMessage());
            throw new QueryException(QueryException.QUERY_NOT_UNDERSTANDABLE, e.getMessage(), e);
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
        try {
            IndexSearcher searcher = createIndexSearcher();
            TaxonomyReader taxoReader = createTaxonomyReader();
            FacetsConfig config = luceneIndexService.getFacetsConfig();
            FacetsCollector fc = new FacetsCollector();

            // MatchAllDocsQuery is for "browsing" (counts facets
            // for all non-deleted docs in the index); normally
            // you'd use a "normal" query:
            if (searchTerm == null || searchTerm.isEmpty()) {
                searcher.search(new MatchAllDocsQuery(), fc);
            } else {
                searchTerm = schemaService.getExtendedSearchTerm(searchTerm);
                QueryParser parser = createQueryParser(DefaultOperator.OR);
                // parser.setAllowLeadingWildcard(true);
                Query query = parser.parse(searchTerm);
                searcher.search(query, fc);
            }
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
            searcher.getIndexReader().close();
            taxoReader.close();
        } catch (IOException | QueryException | ParseException e) {
            // in case of an IOException we just print an error message and
            // return an empty result
            logger.log(Level.SEVERE, "Lucene index error: {0}", e.getMessage());
            throw new InvalidAccessException(InvalidAccessException.INVALID_INDEX, e.getMessage(), e);
        }
        return results;
    }

    /**
     * Returns the total hits for a given search term from the lucene index. The
     * method did not load any data. The provided search term will we extended with
     * a users roles to test the read access level of each workitem matching the
     * search term.
     * 
     * The optional param 'maxResult' can be set to overwrite the
     * DEFAULT_MAX_SEARCH_RESULT.
     * 
     * @see search(String, int, int, Sort, Operator)
     * 
     * @param sSearchTerm
     * @param maxResult   - max search result
     * @return total hits of search result
     * @throws QueryException in case the searchterm is not understandable.
     */
    @Override
    public int getTotalHits(final String _searchTerm, final int _maxResult, final DefaultOperator defaultOperator)
            throws QueryException {
        int result;
        int maxResult = _maxResult;

        if (maxResult <= 0) {
            maxResult = DEFAULT_MAX_SEARCH_RESULT;
        }

        String sSearchTerm = schemaService.getExtendedSearchTerm(_searchTerm);
        // test if searchtem is provided
        if (sSearchTerm == null || "".equals(sSearchTerm)) {
            return 0;
        }

        try {
            IndexSearcher searcher = createIndexSearcher();
            QueryParser parser = createQueryParser(defaultOperator);

            parser.setAllowLeadingWildcard(true);

            // set default operator?
            if (defaultOperator == DefaultOperator.OR) {
                parser.setDefaultOperator(org.apache.lucene.queryparser.classic.QueryParser.Operator.OR);
            } else {
                parser.setDefaultOperator(org.apache.lucene.queryparser.classic.QueryParser.Operator.AND);
            }

            TopDocsCollector<?> collector = null;

            Query query = parser.parse(sSearchTerm);
            // MAX_SEARCH_RESULT is limiting the total number of hits
            collector = TopScoreDocCollector.create(maxResult);

            // - ignore time limiting for now
            // Counter clock = Counter.newCounter(true);
            // TimeLimitingCollector timeLimitingCollector = new
            // TimeLimitingCollector(collector, clock, 10);

            // start search....
            searcher.search(query, collector);
            result = collector.getTotalHits();

            logger.log(Level.FINEST, "......lucene count result = {0}", result);
        } catch (IOException e) {
            // in case of an IOException we just print an error message and
            // return an empty result
            logger.log(Level.SEVERE, "Lucene index error: {0}", e.getMessage());
            throw new InvalidAccessException(InvalidAccessException.INVALID_INDEX, e.getMessage(), e);
        } catch (ParseException e) {
            logger.log(Level.SEVERE, "Lucene search error: {0}", e.getMessage());
            throw new QueryException(QueryException.QUERY_NOT_UNDERSTANDABLE, e.getMessage(), e);
        }

        return result;
    }

    /**
     * Returns a IndexSearcher instance.
     * <p>
     * In case no index yet exits, the method tries to create a new index. This
     * typically is necessary after first deployment.
     * 
     * @param prop
     * @return
     * @throws IOException
     * @throws Exception
     */
    IndexSearcher createIndexSearcher() throws IOException {
        IndexReader reader = null;
        logger.finest("......createIndexSearcher...");
        Directory indexDir = luceneIndexService.createIndexDirectory();
        try {
            // if the index dose not yet exits we got a IO Exception (issue #329)
            reader = DirectoryReader.open(indexDir);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "lucene index can not be opened: {0}", ioe.getMessage());
            // throw the origin exception....
            throw ioe;
        }
        IndexSearcher searcher = new IndexSearcher(reader);
        return searcher;
    }

    /**
     * Returns a IndexSearcher instance.
     * <p>
     * In case no index yet exits, the method tries to create a new index. This
     * typically is necessary after first deployment.
     * 
     * @param prop
     * @return
     * @throws IOException
     * @throws Exception
     */
    TaxonomyReader createTaxonomyReader() throws IOException {

        logger.finest("......createTaxonomyReader...");
        Directory taxoDir = luceneIndexService.createTaxonomyDirectory();
        TaxonomyReader taxoReader = new DirectoryTaxonomyReader(taxoDir);

        return taxoReader;
    }

    /**
     * Returns in instance of a QueyParser based on a KeywordAnalyzer. The method
     * set the lucene DefaultOperator to 'OR' if not specified otherwise in the
     * imixs.properties.
     * 
     * @see issue #28 - normalizeSearchTerm
     * @param prop
     * @return
     */
    QueryParser createQueryParser(DefaultOperator defaultOperator) {
        // use the keywordAnalyzer for searching a search term.
        QueryParser parser = new QueryParser("content", new KeywordAnalyzer());
        // set default operator to 'AND' if not defined by property setting
        // String defaultOperator = prop.getProperty("lucene.defaultOperator");
        if (defaultOperator == DefaultOperator.OR) {
            logger.finest("......DefaultOperator: OR");
            parser.setDefaultOperator(org.apache.lucene.queryparser.classic.QueryParser.Operator.OR);
        } else {
            logger.finest("......DefaultOperator: AND");
            parser.setDefaultOperator(org.apache.lucene.queryparser.classic.QueryParser.Operator.AND);
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
        // load the full imixs document from the database
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
            logger.log(Level.FINEST, ".........append {0} = {1}", new Object[]{indexableField.name(), objectValue});
            imixsDoc.appendItemValue(indexableField.name(), objectValue);
        }

        // compute $isAuthor flag...
        imixsDoc.replaceItemValue(DocumentService.ISAUTHOR, documentService.isAuthor(imixsDoc));

        return imixsDoc;
    }

    private Sort buildLuceneSort(org.imixs.workflow.engine.index.SortOrder sortOrder) {
        Sort sort = null;
        // we do not support multi values here - see
        // LuceneUpdateService.addItemValues
        // it would be possible if we use a SortedSetSortField class here
        sort = new Sort(
                new SortField[] { new SortField(sortOrder.getField(), SortField.Type.STRING, sortOrder.isReverse()) });
        return sort;
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
