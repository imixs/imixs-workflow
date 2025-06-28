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

package org.imixs.workflow.engine.solr;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.index.UpdateService;
import org.imixs.workflow.exceptions.IndexException;
import org.imixs.workflow.services.rest.RestAPIException;

import jakarta.ejb.Stateless;

/**
 * The SolrUpdateService process the index event log entries written by the
 * Imixs DocumentService. The service updates the solr index by flushing the
 * Index EventLog cache.
 * <p>
 * 
 * @version 1.1
 * @author rsoika
 */
@DeclareRoles({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
        "org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
        "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RolesAllowed({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
        "org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
        "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
public class SolrUpdateService implements UpdateService {

    public static final String SOLR_AUTOFLUSH_DISABLED = "solr.autoflush.disabled";
    public static final String SOLR_AUTOFLUSH_INTERVAL = "solr.autoflush.interval";
    public static final String SOLR_AUTOFLUSH_INITIALDELAY = "solr.autoflush.initialdelay";

    @Inject
    SolrIndexService solrIndexService;

    private static final Logger logger = Logger.getLogger(SolrUpdateService.class.getName());

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
     * @throws RestAPIException
     * @throws IndexException
     */
    @Override
    public void updateIndex(List<ItemCollection> documents) {
        try {
            solrIndexService.indexDocuments(documents);
        } catch (RestAPIException e) {
            logger.log(Level.SEVERE, "Failed to update document collection: {0}", e.getMessage());
            throw new IndexException(IndexException.INVALID_INDEX, "Unable to update solr search index", e);
        }
    }

    @Override
    public void updateIndex() {
        boolean debug = logger.isLoggable(Level.FINE);
        long ltime = System.currentTimeMillis();
        // flush eventlog (see issue #411)
        int flushCount = 0;
        while (solrIndexService.flushEventLog(1024) == false) {
            // repeat flush....
            flushCount = +1048;
            if (debug) {
                logger.log(Level.FINE, "...flush event log: {0} entries updated in {1}ms ...",
                        new Object[]{flushCount, System.currentTimeMillis() - ltime});
            }
        }
        if (debug) {
            logger.log(Level.FINE, "...flush solr index completed in {0}ms ...",
                    System.currentTimeMillis() - ltime);
        }
    }

}
