/*******************************************************************************
 *  Imixs Workflow 
 *  Copyright (C) 2001, 2011 Imixs Software Solutions GmbH,  
 *  http://www.imixs.com
 *  
 *  This program is free software; you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License 
 *  as published by the Free Software Foundation; either version 2 
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 *  General Public License for more details.
 *  
 *  You can receive a copy of the GNU General Public
 *  License at http://www.gnu.org/licenses/gpl.html
 *  
 *  Project: 
 *  	http://www.imixs.org
 *  	http://java.net/projects/imixs-workflow
 *  
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika - Software Developer
 *******************************************************************************/

package org.imixs.workflow.engine.lucene;

import java.io.IOException;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.index.DefaultOperator;
import org.imixs.workflow.engine.index.SchemaService;
import org.imixs.workflow.engine.index.SearchService;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.QueryException;

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
	@ConfigProperty(name = "index.defaultOperator", defaultValue = "AND")
	private String luceneDefaultOperator;

	@Inject
	@ConfigProperty(name = "index.splitOnWhitespace", defaultValue = "true")
	private boolean luceneSplitOnWhitespace;

	@Inject
	private LuceneIndexService luceneIndexService;

	@Inject
	private DocumentService documentService;

	@Inject
	private SchemaService schemaService;

	private static Logger logger = Logger.getLogger(LuceneSearchService.class.getName());

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
	 * @param pageSize
	 *            - docs per page
	 * @param pageIndex
	 *            - page number
	 * @param sortOrder
	 * @param defaultOperator
	 *            - optional to change the default search operator
	 * @param loadStubs
	 *            - optional indicates of only the lucene document should be
	 *            returned.
	 * @return collection of search result
	 * 
	 * @throws QueryException
	 *             in case the searchtem is not understandable.
	 */
	@Override
	public List<ItemCollection> search(String searchTerm, int pageSize, int pageIndex,
			org.imixs.workflow.engine.index.SortOrder sortOrder, DefaultOperator defaultOperator, boolean loadStubs)
			throws QueryException {

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

		logger.finest("......lucene search: pageNumber=" + pageIndex + " pageSize=" + pageSize);

		ArrayList<ItemCollection> workitems = new ArrayList<ItemCollection>();

		searchTerm = schemaService.getExtendedSearchTerm(searchTerm);
		// test if searchtem is provided
		if (searchTerm == null || "".equals(searchTerm)) {
			return workitems;
		}

		try {
			IndexSearcher searcher = createIndexSearcher();
			QueryParser parser = createQueryParser();

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
				logger.warning("PageIndex (" + pageSize + "x" + pageIndex + ") exeeded DEFAULT_MAX_SEARCH_RESULT("
						+ DEFAULT_MAX_SEARCH_RESULT + ") -> new MAX_SEARCH_RESULT is set to " + maxSearchResult);
			}

			Query query = parser.parse(searchTerm);
			if (sortOrder != null) {
				// sorted by sortoder
				logger.finest("......lucene result sorted by sortOrder= '" + sortOrder + "' ");
				// MAX_SEARCH_RESULT is limiting the total number of hits
				collector = TopFieldCollector.create(buildLuceneSort(sortOrder), maxSearchResult, false, false, false,
						false);

			} else {
				// sorted by score
				logger.finest("......lucene result sorted by score ");
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

			logger.finest("...returned " + scoreDosArray.length + " documents in "
					+ (System.currentTimeMillis() - lsearchtime) + " ms - total hits=" + topDocs.totalHits);

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
					logger.finest("......lucene lookup $uniqueid=" + sID);
					imixsDoc = documentService.load(sID);
				}

				if (imixsDoc != null) {
					workitems.add(imixsDoc);
				} else {
					logger.warning("lucene index returned unreadable workitem : " + sID);
					documentService.removeDocumentFromIndex(sID);
					// this situation happens if the search index returned
					// documents the current user has no read access.
					// this should normally avoided with the $readaccess
					// search phrase! So if this happens we need to check
					// the createDocument method!
				}
			}

			searcher.getIndexReader().close();

			logger.fine("...search result computed in " + (System.currentTimeMillis() - ltime) + " ms - loadStubs="
					+ loadStubs);
		} catch (IOException e) {
			// in case of an IOException we just print an error message and
			// return an empty result
			logger.severe("Lucene index error: " + e.getMessage());
			throw new InvalidAccessException(InvalidAccessException.INVALID_INDEX, e.getMessage(), e);
		} catch (ParseException e) {
			logger.severe("Lucene search error: " + e.getMessage());
			throw new QueryException(QueryException.QUERY_NOT_UNDERSTANDABLE, e.getMessage(), e);
		}

		return workitems;
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
	 * @param maxResult
	 *            - max search result
	 * @return total hits of search result
	 * @throws QueryException
	 *             in case the searchterm is not understandable.
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
			QueryParser parser = createQueryParser();

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

			logger.finest("......lucene count result = " + result);
		} catch (IOException e) {
			// in case of an IOException we just print an error message and
			// return an empty result
			logger.severe("Lucene index error: " + e.getMessage());
			throw new InvalidAccessException(InvalidAccessException.INVALID_INDEX, e.getMessage(), e);
		} catch (ParseException e) {
			logger.severe("Lucene search error: " + e.getMessage());
			throw new QueryException(QueryException.QUERY_NOT_UNDERSTANDABLE, e.getMessage(), e);
		}

		return result;
	}

	/**
	 * Creates a Lucene FSDirectory Instance. The method uses the proeprty
	 * LockFactory to set a custom LockFactory.
	 * 
	 * For example: org.apache.lucene.store.SimpleFSLockFactory
	 * 
	 * @return
	 * @throws IOException
	 */

	Directory createIndexDirectory() throws IOException {
		logger.finest("......createIndexDirectory...");
		Directory indexDir;
		indexDir = FSDirectory.open(Paths.get(luceneIndexService.getLuceneIndexDir()));
		return indexDir;
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

		Directory indexDir = createIndexDirectory();

		// if the index dose not yet exits we got a IO Exception (issue #329)
		try {
			reader = DirectoryReader.open(indexDir);
		} catch (IOException ioe) {
			// verify if the index is missing. In this case we try to fix the issue by
			// creating a new index dir...
			if (!DirectoryReader.indexExists(indexDir)) {
				logger.info("...lucene index does not yet exist, trying to initialize the index....");
				luceneIndexService.rebuildIndex(indexDir);
				// now try to reopen once again.
				// If this dose not work we really have a IO problem
				reader = DirectoryReader.open(indexDir);
			} else {
				// throw the origin exception....
				throw ioe;
			}
		}

		IndexSearcher searcher = new IndexSearcher(reader);
		return searcher;
	}

	/**
	 * Returns in instance of a QueyParser based on a KeywordAnalyser. The method
	 * set the lucene DefaultOperator to 'OR' if not specified otherwise in the
	 * imixs.properties.
	 * 
	 * @see issue #28 - normalizeSearchTerm
	 * @param prop
	 * @return
	 */
	QueryParser createQueryParser() {
		// use the keywordAnalyzer for searching a search term.
		QueryParser parser = new QueryParser("content", new KeywordAnalyzer());
		// set default operator to 'AND' if not defined by property setting
		// String defaultOperator = prop.getProperty("lucene.defaultOperator");
		if (luceneDefaultOperator != null && "OR".equals(luceneDefaultOperator.toUpperCase())) {
			logger.finest("......DefaultOperator: OR");
			parser.setDefaultOperator(org.apache.lucene.queryparser.classic.QueryParser.Operator.OR);
		} else {
			logger.finest("......DefaultOperator: AND");
			parser.setDefaultOperator(org.apache.lucene.queryparser.classic.QueryParser.Operator.AND);
		}

		// set setSplitOnWhitespace (issue #438)
		logger.finest("......SplitOnWhitespace: " + luceneSplitOnWhitespace);
		parser.setSplitOnWhitespace(luceneSplitOnWhitespace);

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
			logger.finest(".........append " + indexableField.name() + " = " + objectValue);
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
