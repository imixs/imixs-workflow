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

package org.imixs.workflow.engine.adminp;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RunAs;
import jakarta.inject.Inject;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.plugins.OwnerPlugin;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.PluginException;

import jakarta.ejb.LocalBean;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import java.util.logging.Level;

/**
 * JobHandler to upgrate existing workItems to the latest workflow version.
 * 
 * Missing workflow items will be added.
 * 
 * Version 5.1.10 : new space.|process. Items
 * 
 * 
 * @author rsoika
 *
 */
@DeclareRoles({ "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
@Stateless
@LocalBean
public class JobHandlerUpgradeWorkitems implements JobHandler {

    private static final int DEFAULT_BLOCK_SIZE = 100;

    @Resource
    SessionContext ctx;

    @Inject
    DocumentService documentService;

    private static Logger logger = Logger.getLogger(JobHandlerUpgradeWorkitems.class.getName());

    /**
     * This method runs the RebuildLuceneIndexJob. The AdminP job description
     * contains the start position (numIndex) and the number of documents to read
     * (numBlockSize).
     * <p>
     * The method updates the index for all affected documents which can be filtered
     * by 'type' and '$created'.
     * <p>
     * An existing lucene index must be deleted manually by the administrator.
     * <p>
     * After the run method is finished, the properties numIndex, numUpdates and
     * numProcessed are updated.
     * <p>
     * If the number of documents returned from the DocumentService is less the the
     * BlockSize, the method returns true to indicate that the Timer should be
     * canceled.
     * <p>
     * The method runs in an isolated new transaction because the method flushes the
     * local persistence manager.
     * 
     * @param adminp
     * @return true if no more unprocessed documents exist.
     * @throws AccessDeniedException
     * @throws PluginException
     */
    @Override
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public ItemCollection run(ItemCollection adminp) throws AdminPException {

        long lProfiler = System.currentTimeMillis();
        int iIndex = adminp.getItemValueInteger("numIndex");
        int iBlockSize = adminp.getItemValueInteger("numBlockSize");

        // test if numBlockSize is defined.
        if (iBlockSize <= 0) {
            // no set default block size.
            iBlockSize = DEFAULT_BLOCK_SIZE;
            adminp.replaceItemValue("numBlockSize", iBlockSize);
        }

        int iUpdates = adminp.getItemValueInteger("numUpdates");
        int iProcessed = adminp.getItemValueInteger("numProcessed");

        String query = buildQuery(adminp);
        logger.log(Level.FINEST, "......JQPL query: {0}", query);
        adminp.replaceItemValue("txtQuery", query);

        logger.info("... selecting workitems...");
        List<ItemCollection> workitemList = documentService.getDocumentsByQuery(query, iIndex, iBlockSize);
        int colSize = workitemList.size();
        // Update index
        logger.log(Level.INFO,"Job " + AdminPService.JOB_UPGRADE
                + " ({0}) - verifeing {1} workitems...", new Object[]{adminp.getUniqueID(), colSize});
        int iCount = 0;
        for (ItemCollection workitem : workitemList) {
            // only look into documents with a model version...
            if (workitem.hasItem(WorkflowKernel.MODELVERSION)) {
                if (upgradeWorkitem(workitem)) {
                    // update workitem...
                    logger.log(Level.INFO, "...upgrade ''{0}'' ...", workitem.getUniqueID());
                    documentService.saveByNewTransaction(workitem);
                    iCount++;
                }
            }
        }
        iIndex = iIndex + colSize;
        iUpdates = iUpdates + iCount;
        iProcessed = iProcessed + colSize;

        // adjust start pos and update count
        adminp.replaceItemValue("numUpdates", iUpdates);
        adminp.replaceItemValue("numProcessed", iProcessed);
        adminp.replaceItemValue("numIndex", iIndex);

        long time = (System.currentTimeMillis() - lProfiler) / 1000;
        if (time == 0) {
            time = 1;
        }

        logger.log(Level.INFO,"Job " + AdminPService.JOB_UPGRADE + " ({0}) - {1} documents processed,"
                + " {2} updates in {3} sec.  (in total: {4} processed, {5} updates)",
                new Object[]{adminp.getUniqueID(), colSize, iCount, time, iProcessed, iUpdates});

        // if colSize<numBlockSize we can stop the timer
        if (colSize < iBlockSize) {
            // iscompleted = true
            adminp.replaceItemValue(JobHandler.ISCOMPLETED, true);
        }

        return adminp;
    }

    /**
     * This method upgrades missing fields in a workitem
     * 
     * @param worktem
     * @return
     */
    public boolean upgradeWorkitem(ItemCollection workitem) {
        boolean bUpgrade = false;

        if (workitem.getItemValueBoolean("$immutable")) {
            return false;
        }

        if (!workitem.hasItem("$workflowGroup")) {
            workitem.replaceItemValue("$workflowGroup", workitem.getItemValue("txtworkflowgroup"));
            bUpgrade = true;
        }

        if (!workitem.hasItem("$workflowStatus")) {
            workitem.replaceItemValue("$workflowStatus", workitem.getItemValue("txtworkflowstatus"));
            bUpgrade = true;
        }

        if (!workitem.hasItem("$lastEvent")) {
            workitem.replaceItemValue("$lastEvent", workitem.getItemValue("numlastactivityid"));
            bUpgrade = true;
        }

        if (!workitem.hasItem("$lastEventDate")) {
            workitem.replaceItemValue("$lastEventDate", workitem.getItemValue("timworkflowlastaccess"));
            bUpgrade = true;
        }

        if (!workitem.hasItem("$creator")) {
            workitem.replaceItemValue("$creator", workitem.getItemValue("namcreator"));
            bUpgrade = true;
        }

        if (!workitem.hasItem("$taskid")) {
            workitem.replaceItemValue("$taskid", workitem.getItemValue("$processid"));
            bUpgrade = true;
        }
        

        if (!workitem.hasItem(OwnerPlugin.OWNER)) {
            workitem.replaceItemValue(OwnerPlugin.OWNER, workitem.getItemValue("namowner"));
            bUpgrade = true;
        }
        
        
        
        if (!workitem.hasItem("process.name")) {
            workitem.replaceItemValue("process.name", workitem.getItemValue("txtprocessname"));
            workitem.replaceItemValue("process.ref", workitem.getItemValue("txtprocessRef"));
            workitem.replaceItemValue("space.name", workitem.getItemValue("txtspacename"));
            workitem.replaceItemValue("space.ref", workitem.getItemValue("txtspaceRef"));
            
            workitem.replaceItemValue("space.assist", workitem.getItemValue("namspaceassist"));
            workitem.replaceItemValue("space.team", workitem.getItemValue("namspaceTeam"));
            workitem.replaceItemValue("space.manager", workitem.getItemValue("namspaceManager"));
            
            workitem.replaceItemValue("process.assist", workitem.getItemValue("namprocessassist"));
            workitem.replaceItemValue("process.team", workitem.getItemValue("namprocessTeam"));
            workitem.replaceItemValue("process.manager", workitem.getItemValue("namprocessManager"));

            bUpgrade = true;
        }
        
        
        

        return bUpgrade;
    }

    /**
     * This method builds the query statemetn based on the filter criteria.
     * 
     * @param adminp
     * @return
     */
    private String buildQuery(ItemCollection adminp) {
        Date datFilterFrom = adminp.getItemValueDate("datfrom");
        Date datFilterTo = adminp.getItemValueDate("datto");
        String typeFilter = adminp.getItemValueString("typelist");
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd");

        boolean bAddAnd = false;
        String query = "SELECT document FROM Document AS document ";
        // ignore lucene event log entries
        query += "WHERE document.type NOT IN ('event') ";
        // ignore imixs-archive snapshots and deprecated blob
        query += "AND document.type NOT LIKE 'snapshot%' ";
        query += "AND document.type NOT LIKE 'workitemlob%' ";

        if (typeFilter != null && !typeFilter.isEmpty()) {
            // convert type list into comma separated list
            List<String> typeList = Arrays.asList(typeFilter.split("\\s*,\\s*"));
            String sType = "";
            for (String aValue : typeList) {
                sType += "'" + aValue.trim() + "',";
            }
            sType = sType.substring(0, sType.length() - 1);
            query += " AND document.type IN(" + sType + ")";
            bAddAnd = true;
        } else {
        	// default to workitem
        	  query += " AND document.type IN('workitem')";
        	  bAddAnd = true;
        }

        if (datFilterFrom != null) {
            if (bAddAnd) {
                query += " AND ";
            }
            query += " document.created>='" + isoFormat.format(datFilterFrom) + "' ";
            bAddAnd = true;
        }

        if (datFilterTo != null) {
            if (bAddAnd) {
                query += " AND ";
            }
            query += " document.created<='" + isoFormat.format(datFilterTo) + "' ";
            bAddAnd = true;
        }

        query += " ORDER BY document.created";

        return query;
    }
}
