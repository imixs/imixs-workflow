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

import java.util.Collection;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.EventLogService;
import org.imixs.workflow.engine.index.UpdateService;
import org.imixs.workflow.exceptions.IndexException;
import org.imixs.workflow.exceptions.PluginException;

/**
 * The SolrUpdateService provides methods to write Imixs Workitems into a
 * Solr search index. With the method <code>addWorkitem()</code> a
 * ItemCollection can be added to a Solr search index. The service init method
 * reads the property file 'imixs.properties' from the current classpath to
 * determine the configuration.
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
	private EventLogService eventLogService;
	
		private static Logger logger = Logger.getLogger(SolrUpdateService.class.getName());

	/**
	 * PostContruct event - The method loads the lucene index properties from the
	 * imixs.properties file from the classpath. If no properties are defined the
	 * method terminates.
	 * 
	 */
	@PostConstruct
	void init() {

		logger.finest("...... ");
		
	}

	/**
	 * This method adds a single document into the to the Lucene index. Before the
	 * document is added to the index, a new eventLog is created. The document will
	 * be indexed after the method flushEventLog is called. This method is called by
	 * the LuceneSearchService finder methods.
	 * <p>
	 * The method supports committed read. This means that a running transaction
	 * will not read an uncommitted document from the Lucene index.
	 * 
	 * 
	 * @param documentContext
	 */
	public void updateDocument(ItemCollection documentContext) {
		logger.info("...TBD");
	}

	/**
	 * This method adds a collection of documents to the Lucene index. For each
	 * document in a given selection a new eventLog is created. The documents will
	 * be indexed after the method flushEventLog is called. This method is called by
	 * the LuceneSearchService finder methods.
	 * <p>
	 * The method supports committed read. This means that a running transaction
	 * will not read uncommitted documents from the Lucene index.
	 * 
	 * @see updateDocumentsUncommitted
	 * @param documents
	 *            to be indexed
	 * @throws IndexException
	 */
	public void updateDocuments(Collection<ItemCollection> documents) {
		logger.info("...TBD");

	}

	
	/**
	 * This method adds a new eventLog for a document to be deleted from the index.
	 * The document will be removed from the index after the method fluschEventLog
	 * is called. This method is called by the LuceneSearchService finder method
	 * only.
	 * 
	 * 
	 * @param uniqueID
	 *            of the workitem to be removed
	 * @throws PluginException
	 */
	public void removeDocument(String uniqueID) {
		logger.info("...TBD");

		
	}
	


	
}
