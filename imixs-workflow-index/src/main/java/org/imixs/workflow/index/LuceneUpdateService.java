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

package org.imixs.workflow.index;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.index.UpdateService;

import jakarta.ejb.Singleton;
import jakarta.inject.Inject;

/**
 * The LuceneUpdateService provides methods to write Imixs Workitems into a
 * Lucene search index. The service delegates all index operations to the
 * LuceneIndexService.
 * <p>
 * The singleton pattern is used to avoid conflicts within multi-thread
 * scenarios. Index updates are serialized by the container.
 * 
 * @see LuceneIndexService
 * @version 2.0
 * @author rsoika
 */
@Singleton
public class LuceneUpdateService implements UpdateService {

    private static final Logger logger = Logger.getLogger(LuceneUpdateService.class.getName());

    @Inject
    private LuceneIndexService luceneIndexService;

    /**
     * This method adds a collection of documents to the Lucene index. The documents
     * are added immediately to the index. Calling this method within a running
     * transaction leads to a uncommitted reads in the index. For transaction
     * control, it is recommended to use instead the the method updateIndex()
     * which flushes the event log.
     * <p>
     * This method is used by the JobHandlerRebuildIndex only.
     * 
     * @param documents of ItemCollections to be indexed
     */
    @Override
    public void updateIndex(List<ItemCollection> documents) {
        luceneIndexService.indexDocuments(documents);
    }

    /**
     * This method flushes the event log. If the event log contains more entries
     * than the junk size, the flush is repeated in a new transaction (see issue
     * #411 and #439).
     */
    @Override
    public void updateIndex() {
        long ltime = System.currentTimeMillis();
        int flushCount = 0;
        while (luceneIndexService.flushEventLog(2048) == false) {
            // junk size reached - repeat flush....
            flushCount += 2048;
            logger.log(Level.INFO, "├── 🔃 flush index event log: {0} entries updated in {1}ms...",
                    new Object[] { flushCount, System.currentTimeMillis() - ltime });
        }
    }

}