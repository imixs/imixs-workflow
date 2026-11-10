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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.exceptions.QueryException;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.EJBException;
import jakarta.ejb.LocalBean;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;

/**
 * The JobHandlerUpdateWorkitems updates a single item in all workitems selected
 * by a given Lucene search query. Optionally each workitem can be processed by
 * the WorkflowService with a given event.
 * <p>
 * The following job attributes are expected:
 * <ul>
 * <li>query - Lucene search query to select the workitems (required)</li>
 * <li>itemname - name of the item to be updated (required)</li>
 * <li>itemvalue - list of new values. The value types are taken over as
 * given in the job description. An empty list clears the item in replace
 * mode.</li>
 * <li>append - if true the values are appended, otherwise the item values are
 * replaced</li>
 * <li>eventid - optional event. If &gt; 0 the workitem is processed by the
 * WorkflowService, otherwise the workitem is only saved</li>
 * <li>numBlockSize - number of workitems per timer run (default 30)</li>
 * </ul>
 * <p>
 * <strong>Paging:</strong> Each updated workitem is marked with the $uniqueid
 * of this job in the index item '$migration'. The search query is extended
 * with <code>AND NOT $migration:"&lt;jobid&gt;"</code> so already updated
 * workitems drop out of the result set. For this reason the job always reads
 * page 0. Because DocumentService.find() flushes the index before searching,
 * small blocks guarantee that the markers of the previous run are visible.
 * <p>
 * <strong>Error handling:</strong> A workitem which can not be updated would
 * stay in the result set forever. Therefore the job stops completely with an
 * AdminPException on the first problem. Workitems updated in earlier
 * transactions remain committed; the failed workitem is rolled back.
 *
 * @see AdminPService
 */
@DeclareRoles({ "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
@Stateless
@LocalBean
public class JobHandlerUpdateWorkitems implements JobHandler {

    public static final String ITEM_MIGRATION = "$migration";
    public static final String UPDATE_FAILED = "UPDATE_FAILED";
    private static final int DEFAULT_BLOCK_SIZE = 30;

    @Resource
    SessionContext ctx;

    @Inject
    DocumentService documentService;

    @Inject
    WorkflowService workflowService;

    private static final Logger logger = Logger.getLogger(JobHandlerUpdateWorkitems.class.getName());

    /**
     * Selects the next block of not yet updated workitems and updates each
     * workitem in a separate transaction.
     */
    @Override
    public ItemCollection run(ItemCollection adminp) throws AdminPException {
        long lProfiler = System.currentTimeMillis();

        // read and validate job parameters
        String rawQuery = adminp.getItemValueString("query").trim();
        String itemName = adminp.getItemValueString("itemname").trim();
        // raw value list - keeps the types defined by the client
        List<Object> itemValues = new ArrayList<>(adminp.getItemValue("itemvalue"));
        boolean append = adminp.getItemValueBoolean("append");
        int eventId = adminp.getItemValueInteger("eventid");
        String jobId = adminp.getUniqueID();

        logger.info("├── ⚙ Run update job....");
        logger.info("│   ├── Query: " + rawQuery);
        logger.info("│   ├── ItemName: " + itemName);
        logger.info(() -> "│   ├── itemValues: " + itemValues.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", ")));
        logger.info("│   ├── append: " + append);
        logger.info("│   ├── eventId: " + eventId);
        logger.info("│   ├── jobId: " + jobId);

        if (rawQuery.isEmpty() || itemName.isEmpty()) {
            throw new AdminPException(AdminPException.INVALID_PARAMS,
                    "Invalid job configuration - attributes 'query' or 'itemname' are empty.");
        }

        int blockSize = adminp.getItemValueInteger("numBlockSize");
        if (blockSize <= 0) {
            blockSize = DEFAULT_BLOCK_SIZE;
            adminp.replaceItemValue("numBlockSize", blockSize);
        }
        int updates = adminp.getItemValueInteger("numUpdates");

        String summary = "Update item '" + itemName + "' (append=" + append + ", eventid=" + eventId + ")";
        adminp.replaceItemValue("$WorkflowSummary", summary);

        // wrap the user query in brackets so an OR inside does not break the logic,
        // and exclude all workitems already marked by this job
        String searchQuery = "(" + rawQuery + ") AND NOT " + ITEM_MIGRATION + ":\"" + jobId + "\"";
        adminp.replaceItemValue("txtQuery", searchQuery);

        Collection<ItemCollection> col;
        try {
            long totalCount = documentService.count(searchQuery);
            logger.info("│   ├── ↻ " + totalCount + " workitems remaining");
            logger.info("│   ├── ⚙ Processing next " + blockSize + " workitems...");
            // always page 0 - updated workitems drop out of the result set
            col = documentService.find(searchQuery, blockSize, 0, "$created", false);
        } catch (QueryException e) {
            throw new AdminPException(AdminPException.INVALID_PARAMS, "Invalid query: " + e.getMessage());
        }

        int colSize = col.size();
        // proxy is required so that REQUIRES_NEW of updateWorkitem is honored
        JobHandlerUpdateWorkitems proxy = ctx.getBusinessObject(JobHandlerUpdateWorkitems.class);
        for (ItemCollection entity : col) {
            try {
                proxy.updateWorkitem(entity.getUniqueID(), jobId, itemName, itemValues, append, eventId);
            } catch (EJBException e) {
                // runtime exception inside the new transaction - already rolled back
                throw new AdminPException(UPDATE_FAILED,
                        "Update of workitem '" + entity.getUniqueID() + "' failed: " + e.getMessage());
            }
            updates++;
        }

        adminp.replaceItemValue("numUpdates", updates);
        adminp.replaceItemValue("numProcessed", updates);
        adminp.replaceItemValue("numLastCount", colSize);

        long time = (System.currentTimeMillis() - lProfiler) / 1000;
        logger.log(Level.INFO,
                "├── ✓ Job " + AdminPService.JOB_UPDATE_WORKITEMS
                        + " ({0}) Block finished - {1} workitems updated in {2} sec. (in total: {3})",
                new Object[] { jobId, colSize, time, updates });

        // a block smaller than the block size means no more workitems are left
        if (colSize < blockSize) {
            adminp.replaceItemValue(JobHandler.ISCOMPLETED, true);
        }
        return adminp;
    }

    /**
     * Updates a single workitem in a new transaction. The workitem is reloaded
     * here to work on the current state. On any problem the transaction is rolled
     * back and an AdminPException is thrown which stops the whole job.
     *
     * @param uniqueId   id of the workitem
     * @param jobId      migration key used to mark the workitem
     * @param itemName   item to update
     * @param itemValues new values
     * @param append     append or replace the value
     * @param eventId    event to process, or &lt;= 0 to save only
     * @throws AdminPException if the workitem can not be updated
     */
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void updateWorkitem(String uniqueId, String jobId, String itemName, List<Object> itemValues, boolean append,
            int eventId) throws AdminPException {

        ItemCollection workitem = documentService.load(uniqueId);
        if (workitem == null) {
            throw new AdminPException(UPDATE_FAILED, "Workitem '" + uniqueId + "' could not be loaded.");
        }

        // an immutable workitem can not be marked and would block the job forever
        if (workitem.getItemValueBoolean("$immutable")) {
            throw new AdminPException(UPDATE_FAILED,
                    "Workitem '" + uniqueId + "' is immutable - exclude it in the query.");
        }

        // marker already present means the search index is out of sync with the
        // database - continuing would loop or process the event twice
        if (workitem.getItemValueList(ITEM_MIGRATION, String.class).contains(jobId)) {
            throw new AdminPException(UPDATE_FAILED,
                    "Workitem '" + uniqueId + "' is already marked but still found by the index.");
        }

        try {
            if (append) {
                for (Object value : itemValues) {
                    workitem.appendItemValue(itemName, value);
                }
            } else {
                // copy - each workitem gets its own list instance
                workitem.replaceItemValue(itemName, new ArrayList<>(itemValues));
            }
            // set the marker before processing so it is saved in the same transaction
            workitem.appendItemValue(ITEM_MIGRATION, jobId);

            if (eventId > 0) {
                workitem.event(eventId);
                workflowService.processWorkItem(workitem);
            } else {
                documentService.save(workitem);
            }
            logger.log(Level.FINEST, "......updated: {0}", uniqueId);
        } catch (Exception e) {
            // checked exceptions do not roll back automatically
            ctx.setRollbackOnly();
            throw new AdminPException(UPDATE_FAILED,
                    "Update of workitem '" + uniqueId + "' failed: " + e.getMessage());
        }
    }
}