/*  
 *  Imixs-Workflow 
 *  
 *  Copyright (C) 2001-2020 Imixs Software Solutions GmbH,  
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
 *      https://www.imixs.org
 *      https://github.com/imixs/imixs-workflow
 *  
 *  Contributors:  
 *      Imixs Software Solutions GmbH - Project Management
 *      Ralph Soika - Software Developer
 */

package org.imixs.workflow.engine.solr;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;

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
     * @param documents of ItemCollections to be indexed
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
        boolean debug = logger.isLoggable(Level.FINE);
        long ltime = System.currentTimeMillis();
        // flush eventlog (see issue #411)
        int flushCount = 0;
        while (solrIndexService.flushEventLog(1024) == false) {
            // repeat flush....
            flushCount = +1048;
            if (debug) {
                logger.fine("...flush event log: " + flushCount + " entries updated in "
                        + (System.currentTimeMillis() - ltime) + "ms ...");
            }
        }
        if (debug) {
            logger.fine("...flush solr index completed in " + (System.currentTimeMillis() - ltime) + "ms ...");
        }
    }

}
