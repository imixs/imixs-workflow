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

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.inject.Inject;

/**
 * The LuceneUpdateService provides methods to write Imixs Workitems into a
 * Lucene search index. The service delegates all index operations to the
 * LuceneIndexService.
 * <p>
 * The singleton pattern is used to avoid conflicts within multi-thread
 * scenarios. All flush calls (triggered by a search or by the timer) are
 * serialized by the container.
 * <p>
 * The event log is flushed in two ways:
 * <ul>
 * <li>{@link #updateIndex()} - called before each search. Flushes the complete
 * event log to guarantee read-your-writes.</li>
 * <li>{@link #scheduledFlush()} - called every 3 seconds. Flushes at most one
 * junk per tick, so the singleton lock is released quickly and searches are not
 * blocked during long running update jobs.</li>
 * </ul>
 *
 * @see LuceneIndexService
 * @version 2.0
 * @author rsoika
 */
@Singleton
@DeclareRoles({ "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
public class LuceneUpdateService implements UpdateService {

    // maximum number of event log entries processed in one transaction
    public static final int EVENTLOG_JUNK_SIZE = 1024;

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
     * This method flushes the complete event log. If the event log contains more
     * entries than the junk size, the flush is repeated in a new transaction (see
     * issue #411 and #439). The method returns only after the event log is empty,
     * so a following search reflects all changes (read-your-writes).
     */
    @Override
    public void updateIndex() {
        long ltime = System.currentTimeMillis();
        int flushCount = 0;
        while (luceneIndexService.flushEventLog(EVENTLOG_JUNK_SIZE) == false) {
            // junk size reached - repeat flush....
            flushCount += EVENTLOG_JUNK_SIZE;
            logger.log(Level.INFO, "├── 🔃 flush index event log: {0} entries updated in {1}ms...",
                    new Object[] { flushCount, System.currentTimeMillis() - ltime });
        }
    }

    /**
     * Flushes the index event log periodically every 3 seconds. This keeps the
     * event log backlog small during long running update jobs, so a search
     * triggered flush is fast.
     * <p>
     * In contrast to {@link #updateIndex()} the method processes at most one junk
     * per tick. A large backlog is processed over several ticks, and the singleton
     * lock is released between the ticks, so searches are not blocked.
     * <p>
     * If the event log is empty, the flush is a cheap query only.
     */
    @Schedule(second = "*/3", minute = "*", hour = "*", persistent = false)
    public void scheduledFlush() {
        long ltime = System.currentTimeMillis();
        if (luceneIndexService.flushEventLog(EVENTLOG_JUNK_SIZE) == false) {
            logger.log(Level.FINE, "├── 🔃 scheduled flush: {0} entries updated in {1}ms"
                    + " ⇢ backlog will be continued with next tick",
                    new Object[] { EVENTLOG_JUNK_SIZE, System.currentTimeMillis() - ltime });
        }
    }

}