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

import java.util.List;
import java.util.logging.Logger;

import jakarta.inject.Inject;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.index.UpdateService;
import org.imixs.workflow.exceptions.IndexException;

import jakarta.ejb.Singleton;
import java.util.logging.Level;

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

    private static final Logger logger = Logger.getLogger(LuceneUpdateService.class.getName());

    /**
     * This method adds a collection of documents to the Lucene index. The documents
     * are added immediately to the index. Calling this method within a running
     * transaction leads to a uncommitted reads in the index. For transaction
     * control, it is recommended to use instead the the method updateDocumetns()
     * which takes care of uncommitted reads.
     * <p>
     * This method is used by the JobHandlerRebuildIndex only.
     * 
     * @param documents of ItemCollections to be indexed
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
            logger.log(Level.INFO, "...flush event log: {0} entries updated in {1}ms ...",
                    new Object[]{flushCount, System.currentTimeMillis() - ltime});
        }
    }

}
