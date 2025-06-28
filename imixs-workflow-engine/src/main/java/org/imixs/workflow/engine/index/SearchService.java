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

package org.imixs.workflow.engine.index;

import java.util.List;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.QueryException;

import jakarta.ejb.Stateless;

/**
 * This SearchService defines methods to search workitems or collections of
 * workitems.
 * 
 * @version 1.0
 * @author rsoika
 */
//@Stateless
public interface SearchService {

	public static final int DEFAULT_MAX_SEARCH_RESULT = 9999; // limiting the
																// total
	// number of hits
	public static final int DEFAULT_PAGE_SIZE = 100; // default docs in one page

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
	 * @param sortOrder       - optional to sort the result
	 * @param defaultOperator - optional to change the default search operator
	 * @param loadStubs       - optional indicates of only the lucene document
	 *                        should be returned.
	 * @return collection of search result
	 * 
	 * @throws QueryException in case the searchtem is not understandable.
	 */
	public List<ItemCollection> search(String searchTerm, int pageSize, int pageIndex, SortOrder sortOrder,
			DefaultOperator defaultOperator, boolean loadStubs) throws QueryException;

	/**
	 * Returns the total hits for a given search term from the lucene index. The
	 * method did not load any data. The provided search term will be extended with
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
	public int getTotalHits(final String _searchTerm, final int _maxResult, final DefaultOperator defaultOperator)
			throws QueryException;

	/**
	 * Returns the total hits for a given set of categories from the lucene taxonomy
	 * index. The method did not load any data.
	 * 
	 * 
	 * @param categories - a list of categories.
	 * @return total hits of search result
	 * @throws QueryException in case the searchterm is not understandable.
	 */
	public List<Category> getTaxonomy(String... categories);
	
	/**
     * Returns the total hits for a given set of categories from the lucene taxonomy
     * index based on a search query. The method did not load any data.
     * 
     * @param searchTerm - a lucene search term
     * @param categories - a list of categories.
     * @return total hits of search result
     * @throws QueryException in case the searchterm is not understandable.
     */
	public List<Category> getTaxonomyByQuery(String searchTerm, String... categories);
}
