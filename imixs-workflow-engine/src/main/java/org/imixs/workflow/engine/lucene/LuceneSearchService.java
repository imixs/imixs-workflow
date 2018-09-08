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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.standard.ClassicAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParser.Operator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.PropertyService;
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
@LocalBean
public class LuceneSearchService {

	public static final int DEFAULT_MAX_SEARCH_RESULT = 9999; // limiting the
																// total
	// number of hits
	public static final int DEFAULT_PAGE_SIZE = 100; // default docs in one page

	@EJB
	PropertyService propertyService;

	@EJB
	DocumentService documentService;
	
	@EJB
	LuceneUpdateService luceneUpdateService;

	private static Logger logger = Logger.getLogger(LuceneSearchService.class.getName());

	/**
	 * PostContruct event - loads the imixs.properties.
	 */
	@PostConstruct
	void init() {
	}

	/**
	 * Returns a collection of documents matching the provided search term. The
	 * provided search team will we extended with a users roles to test the read
	 * access level of each workitem matching the search term.
	 * 
	 * @param sSearchTerm
	 * @return collection of search result
	 * @throws QueryException
	 */
	public List<ItemCollection> search(String sSearchTerm) throws QueryException {
		// no sort order
		return search(sSearchTerm, DEFAULT_MAX_SEARCH_RESULT, 0, null, null);
	}

	/**
	 * Returns a collection of documents matching the provided search term. The
	 * provided search team will we extended with a users roles to test the read
	 * access level of each workitem matching the search term.
	 * 
	 * @param pageSize
	 *            - docs per page
	 * @param pageIndex
	 *            - page number
	 * 
	 * @return collection of search result
	 * @throws QueryException
	 */
	public List<ItemCollection> search(String sSearchTerm, int pageSize, int pageIndex) throws QueryException {
		// no sort order
		return search(sSearchTerm, pageSize, pageIndex, null, null);
	}

	/**
	 * Returns a collection of documents matching matching the provided search term.
	 * The provided search term will we extended with a users roles to test the read
	 * access level of each workitem matching the search term.
	 * <p>
	 * The optional param 'searchOrder' can be set to force lucene to sort the
	 * search result by any search order.
	 * <p>
	 * The optional param 'defaultOperator' can be set to Operator.AND
	 * 
	 * @param sSearchTerm
	 * @param pageSize
	 *            - docs per page
	 * @param pageIndex
	 *            - page number
	 * @param sortOrder
	 *            - optional to sort the result
	 * @param defaultOperator
	 *            - optional to change the default search operator
	 * 
	 * @return collection of search result
	 * @throws QueryException
	 *             in case the searchtem is not understandable.
	 */
	public List<ItemCollection> search(String sSearchTerm, int pageSize, int pageIndex, Sort sortOrder,
			Operator defaultOperator) throws QueryException {

		long ltime = System.currentTimeMillis();
		
		// flush eventlog (see issue #411)
		luceneUpdateService.flushEventLog();

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

		sSearchTerm = getExtendedSearchTerm(sSearchTerm);
		// test if searchtem is provided
		if (sSearchTerm == null || "".equals(sSearchTerm)) {
			return workitems;
		}

		Properties prop = propertyService.getProperties();
		if (prop.isEmpty()) {
			logger.warning("imixs.properties not found!");
			return workitems;
		}

		try {
			IndexSearcher searcher = createIndexSearcher(prop);
			QueryParser parser = createQueryParser(prop);

			parser.setAllowLeadingWildcard(true);

			// set default operator?
			if (defaultOperator != null)
				parser.setDefaultOperator(defaultOperator);

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

			Query query = parser.parse(sSearchTerm);
			if (sortOrder != null) {
				// sorted by sortoder
				logger.finest("......lucene result sorted by sortOrder= '" + sortOrder + "' ");
				// MAX_SEARCH_RESULT is limiting the total number of hits
				collector = TopFieldCollector.create(sortOrder, maxSearchResult, false, false, false);

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

			logger.fine("...returned " + scoreDosArray.length + " documents in "
					+ (System.currentTimeMillis() - lsearchtime) + " ms - total hits=" + topDocs.totalHits);

			for (ScoreDoc scoredoc : scoreDosArray) {
				// Retrieve the matched document and show relevant details
				Document doc = searcher.doc(scoredoc.doc);

				String sID = doc.get("$uniqueid");
				logger.finest("......lucene lookup $uniqueid=" + sID);
				ItemCollection itemCol = documentService.load(sID);
				if (itemCol != null) {
					workitems.add(itemCol);
				} else {
					logger.warning("lucene index returned unreadable workitem : " + sID);
					luceneUpdateService.removeDocument(sID);
					// this situation happens if the search index returned
					// documents the current user has no read access.
					// this should normally avoided with the $readaccess
					// search phrase! So if this happens we need to check
					// the createDocument method!
				}
			}

			searcher.getIndexReader().close();

			logger.fine("...search result computed in " + (System.currentTimeMillis() - ltime) + " ms");
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
	public int getTotalHits(String sSearchTerm, int maxResult, Operator defaultOperator) throws QueryException {
		int result;
		if (maxResult <= 0) {
			maxResult = DEFAULT_MAX_SEARCH_RESULT;
		}

		sSearchTerm = getExtendedSearchTerm(sSearchTerm);
		// test if searchtem is provided
		if (sSearchTerm == null || "".equals(sSearchTerm)) {
			return 0;
		}

		Properties prop = propertyService.getProperties();
		if (prop.isEmpty()) {
			logger.warning("imixs.properties not found!");
			return 0;
		}

		try {
			IndexSearcher searcher = createIndexSearcher(prop);
			QueryParser parser = createQueryParser(prop);

			parser.setAllowLeadingWildcard(true);

			// set default operator?
			if (defaultOperator != null) {
				parser.setDefaultOperator(defaultOperator);
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
	 * Returns the extended search term for a given query. The search term will we
	 * extended with a users roles to test the read access level of each workitem
	 * matching the search term.
	 * 
	 * The optional param 'defaultOperator' can be set to Operator.AND
	 * 
	 * @param sSearchTerm
	 * @param defaultOperator
	 *            - optional to change the default search operator
	 * 
	 * @return extended search term
	 * @throws QueryException
	 *             in case the searchtem is not understandable.
	 */
	String getExtendedSearchTerm(String sSearchTerm) throws QueryException {
		// test if searchtem is provided
		if (sSearchTerm == null || "".equals(sSearchTerm)) {
			logger.warning("No search term provided!");
			return "";
		}
		// extend the Search Term if user is not ACCESSLEVEL_MANAGERACCESS
		if (!documentService.isUserInRole(DocumentService.ACCESSLEVEL_MANAGERACCESS)) {
			// get user names list
			List<String> userNameList = documentService.getUserNameList();
			// create search term (always add ANONYMOUS)
			String sAccessTerm = "($readaccess:" + LuceneUpdateService.ANONYMOUS;
			for (String aRole : userNameList) {
				if (!"".equals(aRole))
					sAccessTerm += " OR $readaccess:\"" + aRole + "\"";
			}
			sAccessTerm += ") AND ";
			sSearchTerm = sAccessTerm + sSearchTerm;
		}
		logger.finest("......lucene final searchTerm=" + sSearchTerm);

		return sSearchTerm;
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
	Directory createIndexDirectory(Properties prop) throws IOException {
		logger.finest("......createIndexDirectory...");
		// read configuration
		String sIndexDir = prop.getProperty("lucence.indexDir", LuceneUpdateService.DEFAULT_INDEX_DIRECTORY);
		Directory indexDir;
		indexDir = FSDirectory.open(Paths.get(sIndexDir));
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
	IndexSearcher createIndexSearcher(Properties prop) throws IOException {
		IndexReader reader = null;
		logger.finest("......createIndexSearcher...");

		Directory indexDir = createIndexDirectory(prop);

		// if the index dose not yet exits we got a IO Exception (issue #329)
		try {
			reader = DirectoryReader.open(indexDir);
		} catch (IOException ioe) {
			// verify if the index is missing. In this case we try to fix the issue by
			// creating a new index dir...
			if (!DirectoryReader.indexExists(indexDir)) {
				logger.finest("......index does not yet exist, trying to initialize the index....");
				// create a IndexWriter Instance
				IndexWriterConfig indexWriterConfig;
				indexWriterConfig = new IndexWriterConfig(new ClassicAnalyzer());
				indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
				IndexWriter indexWriter = new IndexWriter(indexDir, indexWriterConfig);
				indexWriter.close();
				// now try to reopen once again.
				// If this dose not work we really have a IO problem
				reader = DirectoryReader.open(indexDir);
				logger.finest("......index successfull created.");
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
	QueryParser createQueryParser(Properties prop) {
		// use the keywordAnalyzer for searching a search term.
		QueryParser parser = new QueryParser("content", new KeywordAnalyzer());
		// set default operator to 'AND' if not defined by property setting
		String defaultOperator = prop.getProperty("lucene.defaultOperator");
		if (defaultOperator != null && "OR".equals(defaultOperator.toUpperCase())) {
			logger.finest("......DefaultOperator: OR");
			parser.setDefaultOperator(Operator.OR);
		} else {
			logger.finest("......DefaultOperator: AND");
			parser.setDefaultOperator(Operator.AND);
		}

		return parser;
	}

	/**
	 * This helper method escapes wildcard tokens found in a lucene search term. The
	 * method can be used by clients to prepare a search phrase.
	 * 
	 * The method rewrites the lucene <code>QueryParser.escape</code> method and did
	 * not! escape '*' char.
	 * 
	 * Clients should use the method normalizeSearchTerm() instead of
	 * escapeSearchTerm() to prepare a user input for a lucene search.
	 * 
	 * 
	 * @see normalizeSearchTerm
	 * @param searchTerm
	 * @param ignoreBracket
	 *            - if true brackes will not be escaped.
	 * @return escaped search term
	 */
	public static String escapeSearchTerm(String searchTerm, boolean ignoreBracket) {
		if (searchTerm == null || searchTerm.isEmpty()) {
			return searchTerm;
		}

		// this is the code from the QueryParser.escape() method without the '*'
		// char!
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < searchTerm.length(); i++) {
			char c = searchTerm.charAt(i);
			// These characters are part of the query syntax and must be escaped
			// (ignore brackets!)
			if (c == '\\' || c == '+' || c == '-' || c == '!' || c == ':' || c == '^' || c == '[' || c == ']'
					|| c == '\"' || c == '{' || c == '}' || c == '~' || c == '?' || c == '|' || c == '&' || c == '/') {
				sb.append('\\');
			}

			// escape bracket?
			if (!ignoreBracket && (c == '(' || c == ')')) {
				sb.append('\\');
			}

			sb.append(c);
		}
		return sb.toString();

	}

	public static String escapeSearchTerm(String searchTerm) {
		return escapeSearchTerm(searchTerm, false);
	}

	/**
	 * This method normalizes a search term using the Lucene ClassicTokenzier. The
	 * method can be used by clients to prepare a search phrase.
	 * 
	 * The method also escapes the result search term.
	 * 
	 * e.g. 'europe/berlin' will be normalized to 'europe berlin' e.g. 'r555/333'
	 * will be unmodified 'r555/333'
	 * 
	 * @param searchTerm
	 * @return normalzed search term
	 * @throws QueryException
	 */
	public static String normalizeSearchTerm(String searchTerm) throws QueryException {
		if (searchTerm == null) {
			return "";
		}
		if (searchTerm.trim().isEmpty()) {
			return "";
		}

		ClassicAnalyzer analyzer = new ClassicAnalyzer();
		QueryParser parser = new QueryParser("content", analyzer);
		// issue #331
		parser.setAllowLeadingWildcard(true);
		try {
			Query result = parser.parse(escapeSearchTerm(searchTerm, false));
			searchTerm = result.toString("content");
		} catch (ParseException e) {
			logger.warning("Unable to normalze serchTerm '" + searchTerm + "'  -> " + e.getMessage());
			throw new QueryException(QueryException.QUERY_NOT_UNDERSTANDABLE, e.getMessage(), e);
		}
		return escapeSearchTerm(searchTerm, true);

	}
}
