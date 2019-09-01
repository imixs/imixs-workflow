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

import java.util.List;
import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.index.UpdateService;
import org.imixs.workflow.exceptions.IndexException;
import org.imixs.workflow.services.rest.RestAPIException;

/**
 * The SolrUpdateService provides methods to write Imixs Workitems into a Solr
 * search index. With the method <code>addWorkitem()</code> a ItemCollection can
 * be added to a Solr search index. The service init method reads the property
 * file 'imixs.properties' from the current classpath to determine the
 * configuration.
 * 
 * <ul>
 * <li>The property "solr.core" defines the Solr core for the lucene index
 * </ul>
 * 
 *
 * @version 1.0
 * @author rsoika
 */
@Stateless
public class SolrUpdateService implements UpdateService {

	@Inject
	SolrIndexService solrIndexService;

	private static Logger logger = Logger.getLogger(SolrUpdateService.class.getName());
	

	/**
	 * This method adds a collection of documents to the Lucene index. The documents
	 * are added immediately to the index. Calling this method within a running
	 * transaction leads to a uncommitted reads in the index. For transaction
	 * control, it is recommended to use instead the the method updateDocumetns()
	 * which takes care of uncommitted reads.
	 * <p>
	 * This method is used by the JobHandlerRebuildIndex only.
	 * 
	 * @param documents
	 *            of ItemCollections to be indexed
	 * @throws RestAPIException
	 * @throws IndexException
	 */
	@Override
	public void updateIndex(List<ItemCollection> documents) {
		try {
			solrIndexService.indexDocuments(documents);
		} catch (RestAPIException e) {
			logger.severe("Failed to update document collection: " + e.getMessage());
			throw new IndexException(IndexException.INVALID_INDEX, "Unable to update solr search index", e);
		}
	}

	@Override
	public void updateIndex() {
		long ltime = System.currentTimeMillis();
		// flush eventlog (see issue #411)
		int flushCount = 0;
		while (solrIndexService.flushEventLog(2048) == false) {
			// repeat flush....
			flushCount = +2048;
			logger.info("...flush event log: " + flushCount + " entries updated in "
					+ (System.currentTimeMillis() - ltime) + "ms ...");
		}
	}

}
