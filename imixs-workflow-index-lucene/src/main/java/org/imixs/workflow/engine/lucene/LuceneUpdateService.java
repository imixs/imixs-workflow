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

import java.util.List;
import java.util.logging.Logger;

import javax.ejb.Singleton;
import javax.inject.Inject;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.index.UpdateService;
import org.imixs.workflow.exceptions.IndexException;

/**
 * The LuceneUpdateService provides methods to write Imixs Workitems into a
 * Lucene search index. With the method <code>addWorkitem()</code> a
 * ItemCollection can be added to a lucene search index. The service init method
 * reads the property file 'imixs.properties' from the current classpath to
 * determine the configuration.
 * 
 * <ul>
 * <li>The property "IndexDir" defines the location of the lucene index
 * <li>The property "FulltextFieldList" lists all fields which should be
 * searchable after a workitem was updated
 * <li>The property "IndexFieldList" lists all fields which should be indexed as
 * keywords by the lucene search engine
 * </ul>
 * 
 * The singleton pattern is used to avoid conflicts within multi-thread
 * scenarios. The service is used by the LucenPlugin to update the lucene index
 * during a workflow processing step.
 * 
 * 
 * @see http://stackoverflow.com/questions/34880347/why-did-lucene-indexwriter-
 *      did-not-update-the-index-when-called-from-a-web-modul
 * @see LucenePlugin
 * @version 1.2
 * @author rsoika
 */
@Singleton
public class LuceneUpdateService implements UpdateService {

	@Inject
	private LuceneIndexService luceneIndexService;

	private static Logger logger = Logger.getLogger(LuceneUpdateService.class.getName());

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
	 * @throws IndexException
	 */
	@Override
	public void updateIndex(List<ItemCollection> documents) {
		luceneIndexService.indexDocuments(documents);

	}

	/**
	 * This method flush the event log.
	 */
	@Override
	public void updateIndex() {
		long ltime = System.currentTimeMillis();
		// flush eventlog (see issue #411)
		int flushCount = 0;
		while (luceneIndexService.flushEventLog(2048) == false) {
			// repeat flush....
			flushCount = +2048;
			logger.info("...flush event log: " + flushCount + " entries updated in "
					+ (System.currentTimeMillis() - ltime) + "ms ...");
		}
	}

}
