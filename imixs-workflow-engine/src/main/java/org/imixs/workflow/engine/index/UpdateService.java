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

package org.imixs.workflow.engine.index;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.ejb.Local;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.IndexException;
import org.imixs.workflow.exceptions.PluginException;

/**
 * The UpdateService provides methods to write Imixs Workitems into a search
 * index. An ItemCollection can be added into the index by calling themethod
 * <code>updateDocument()</code>
 * 
 * @see SchemaService
 * @version 1.0
 * @author rsoika
 */
@Local
public interface UpdateService {

	// default field lists
	public static List<String> DEFAULT_SEARCH_FIELD_LIST = Arrays.asList("$workflowsummary", "$workflowabstract");
	public static List<String> DEFAULT_NOANALYSE_FIELD_LIST = Arrays.asList("$modelversion", "$taskid", "$processid",
			"$workitemid", "$uniqueidref", "type", "$writeaccess", "$modified", "$created", "namcreator", "$creator",
			"$editor", "$lasteditor", "$workflowgroup", "$workflowstatus", "txtworkflowgroup", "name", "txtname",
			"$owner", "namowner", "txtworkitemref", "$uniqueidsource", "$uniqueidversions", "$lasttask", "$lastevent",
			"$lasteventdate");
	public static List<String> DEFAULT_STORE_FIELD_LIST = Arrays.asList("type", "$taskid", "$writeaccess",
			"$workflowsummary", "$workflowabstract", "$workflowgroup", "$workflowstatus", "$modified", "$created",
			"$lasteventdate", "$creator", "$editor", "$lasteditor", "$owner", "namowner");

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
	public void updateDocument(ItemCollection documentContext);

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
	public void updateDocuments(Collection<ItemCollection> documents);

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
	public void removeDocument(String uniqueID);

}
