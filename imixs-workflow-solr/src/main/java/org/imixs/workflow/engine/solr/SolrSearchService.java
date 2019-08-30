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

package org.imixs.workflow.engine.solr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.index.DefaultOperator;
import org.imixs.workflow.engine.index.SchemaService;
import org.imixs.workflow.engine.index.SearchService;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.QueryException;

/**
 * This session ejb provides a service to search the solr index.
 * <p>
 * 
 * @version 1.0
 * @author rsoika
 */
@DeclareRoles({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RolesAllowed({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
public class SolrSearchService implements SearchService {

	public static final int DEFAULT_MAX_SEARCH_RESULT = 9999; // limiting the
																// total
	// number of hits
	public static final int DEFAULT_PAGE_SIZE = 100; // default docs in one page

	@Inject
	private SchemaService schemaService;

	@Inject
	private DocumentService documentService;

	private static Logger logger = Logger.getLogger(SolrSearchService.class.getName());

	

	/**
	 * Returns a collection of documents matching the provided search term. The
	 * provided search team will we extended with a users roles to test the read
	 * access level of each workitem matching the search term.
	 * 
	 * @param sSearchTerm
	 * @return collection of search result
	 * @throws QueryException
	 */
//	@Override
//	public List<ItemCollection> search(String sSearchTerm) throws QueryException {
//		// no sort order
//		return search(sSearchTerm, DEFAULT_MAX_SEARCH_RESULT, 0, null, DefaultOperator.AND);
//	}

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
//	@Override
//	public List<ItemCollection> search(String sSearchTerm, int pageSize, int pageIndex) throws QueryException {
//		// no sort order
//		return search(sSearchTerm, pageSize, pageIndex, null, null);
//	}

	/**
	 * Returns a collection of documents matching the provided search term. The term
	 * will be extended with the current users roles to test the read access level
	 * of each workitem matching the search term.
	 * <p>
	 * The method returns the full loaded documents. If you only want to search for
	 * document stubs use instead the method
	 * <p>
	 * <code>search(String searchTerm, int pageSize, int pageIndex, Sort sortOrder,
			Operator defaultOperator, boolean loadStubs)</code>
	 * <p>
	 * 
	 */
//	@Override
//	public List<ItemCollection> search(String sSearchTerm, int pageSize, int pageIndex,
//			org.imixs.workflow.engine.index.SortOrder sortOrder, DefaultOperator defaultOperator)
//			throws QueryException {
//		return search(sSearchTerm, pageSize, pageIndex, sortOrder, defaultOperator, false);
//	}

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

		logger.finest("......solr search: pageNumber=" + pageIndex + " pageSize=" + pageSize);

		ArrayList<ItemCollection> workitems = new ArrayList<ItemCollection>();

		searchTerm = schemaService.getExtendedSearchTerm(searchTerm);
		// test if searchtem is provided
		if (searchTerm == null || "".equals(searchTerm)) {
			return workitems;
		}
		
			logger.fine("...search result computed in " + (System.currentTimeMillis() - ltime) + " ms - loadStubs="
					+ loadStubs);
		

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

		logger.warning("...TBD");
		return 0;
	}

	
}
