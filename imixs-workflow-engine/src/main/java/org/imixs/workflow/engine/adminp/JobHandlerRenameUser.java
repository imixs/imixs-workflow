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
import java.util.Collection;
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
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.engine.plugins.OwnerPlugin;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.QueryException;

import jakarta.ejb.LocalBean;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import java.util.logging.Level;

/**
 * The JobHandlerRenameUser updates the name fields of workitems. A name can be
 * replaced or added. The following job attributes are expected:
 * 
 * <ul>
 * <li>namFrom - source userID</li>
 * <li>namTo - target userID</li>
 * <li>keyReplace - if true the source UserID will be replaced with the target
 * UserID, otherwise the target userID will be added</li>
 * </ul>
 * 
 * The jobHandler only processes workitems from the type
 * <ul>
 * <li>workitem</li>
 * <li>childworkitem</li>
 * <li>workitemlob</li>
 * </ul>
 * 
 * The following workitem attributes will be updated:
 * <ul>
 * <li>$writeaccess</li>
 * <li>$readaccess</li>
 * <li>$creator</li>
 * <li>owner</li>
 * <li>namcreator (deprecated)</li>
 * </ul>
 * 
 * The attributes $creator can not be replaced. Only an additional userID is
 * placed here.
 * 
 * @see AdminPService AdminPService for details
 * @version 1.0
 * 
 */
@DeclareRoles({ "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
@Stateless
@LocalBean
public class JobHandlerRenameUser implements JobHandler {

    @Resource
    SessionContext ctx;

    @Inject
    DocumentService documentService;

    private static final int DEFAULT_COUNT = 100;
    private static final Logger logger = Logger.getLogger(JobHandlerRenameUser.class.getName());

    /**
     * This method creates a new AdminP Job to rename userId in workitems.
     * 
     * @throws QueryException
     * @throws AccessDeniedException
     */
    @Override
    public ItemCollection run(ItemCollection adminp) throws AdminPException {
        long lProfiler = System.currentTimeMillis();
        int iIndex = adminp.getItemValueInteger("numIndex");
        int iBlockSize = adminp.getItemValueInteger("numBlockSize");
        if (iBlockSize <= 0) {
            iBlockSize = DEFAULT_COUNT;
            adminp.replaceItemValue("numBlockSize", iBlockSize);
        }
        Date datFilterFrom = adminp.getItemValueDate("datfrom");
        Date datFilterTo = adminp.getItemValueDate("datto");

        int iUpdates = adminp.getItemValueInteger("numUpdates");
        int iProcessed = adminp.getItemValueInteger("numProcessed");
        String fromUserID = adminp.getItemValueString("namFrom").trim();
        String toUserID = adminp.getItemValueString("namTo").trim();
        boolean replace = adminp.getItemValueBoolean("keyReplace");

        if (fromUserID.isEmpty() || toUserID.isEmpty()) {
            throw new AdminPException(AdminPException.INVALID_PARAMS,
                    "Invalid job configuration - attributes 'namFrom' or 'namTo' are empty.");
        }

        // update $WorkflowSummary
        String summary = "Rename: " + fromUserID + " -> " + toUserID + " (replace=" + replace + ")";
        logger.info(summary);

        adminp.replaceItemValue("$WorkflowSummary", summary);

        // build search query

        String typeFilter = adminp.getItemValueString("typelist");
        if (typeFilter.isEmpty()) {
            // set default type
            typeFilter = "workitem";
        }

        String sQuery = "(";
        // convert type list into comma separated list
        List<String> typeList = Arrays.asList(typeFilter.split("\\s*,\\s*"));
        for (String aValue : typeList) {
            sQuery += "type:\"" + aValue.trim() + "\" OR ";
        }
        sQuery = sQuery.substring(0, sQuery.length() - 4);
        sQuery += ")";
        // !! We do ignore the creator!! - see issue #350
        sQuery += " AND ($writeaccess:\"" + fromUserID + "\" OR $readaccess:\"" + fromUserID + "\" OR $owner:\""
                + fromUserID + "\" OR namowner:\"" + fromUserID + "\")";

        if (datFilterFrom != null && datFilterTo != null) {
            SimpleDateFormat luceneFormat = new SimpleDateFormat("yyyyMMdd");
            sQuery += " AND ($created:[" + luceneFormat.format(datFilterFrom) + " TO "
                    + luceneFormat.format(datFilterTo) + "])";
        }

        adminp.replaceItemValue("txtQuery", sQuery);

        Collection<ItemCollection> col;
        try {
            // ASC sorting is important here!
            col = documentService.find(sQuery, iBlockSize, iIndex, "$created", false);
        } catch (QueryException e) {
            throw new InvalidAccessException(InvalidAccessException.INVALID_ID, e.getMessage(), e);
        }
        int colSize = col.size();
        // check all selected documents
        for (ItemCollection entity : col) {
            iProcessed++;
            // call from new instance because of transaction new...
            // see: http://blog.imixs.org/?p=155
            // see: https://www.java.net/node/705304
            boolean result = ctx.getBusinessObject(JobHandlerRenameUser.class).updateWorkitemUserIds(entity, fromUserID,
                    toUserID, replace);
            if (result == true) {
                // inc counter
                iUpdates++;
            }
        }

        // adjust start pos and update count
        adminp.replaceItemValue("numUpdates", iUpdates);
        adminp.replaceItemValue("numProcessed", iProcessed);
        adminp.replaceItemValue("numLastCount", col.size());
        iIndex++;
        adminp.replaceItemValue("numIndex", iIndex);

        long time = (System.currentTimeMillis() - lProfiler) / 1000;
        if (time == 0) {
            time = 1;
        }

        logger.log(Level.INFO, "Job " + AdminPService.JOB_RENAME_USER + " ({0}) - {1} documents processed,"
                + " {2} updates in {3} sec.  (in total: {4} processed, {5} updates)",
                new Object[]{adminp.getUniqueID(), colSize, iUpdates, time, iProcessed, iUpdates});

        // if colSize<numBlockSize we can stop the timer
        if (colSize < iBlockSize) {
            // iscompleted = true
            adminp.replaceItemValue(JobHandler.ISCOMPLETED, true);
        }
        return adminp;
    }

    /**
     * Updates read,write and owner of a entity and returns true if an update was
     * necessary
     * 
     * @param entity
     * @param from
     * @param to
     * @param replace
     * @return true if the entiy was modified.
     * @throws AccessDeniedException
     */
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public boolean updateWorkitemUserIds(ItemCollection entity, String from, String to, boolean replace)
            throws AccessDeniedException {

        boolean bUpdate = false;
        if (entity == null)
            return false;

        if (entity.getItemValueBoolean("$immutable")) {
            return false;
        }

        if (entity.getItemValueBoolean("private")) {
            return false;
        }

        // Verify Fields

        if (updateList(entity.getItemValue(WorkflowService.READACCESS), from, to, replace))
            bUpdate = true;

        if (updateList(entity.getItemValue(WorkflowService.WRITEACCESS), from, to, replace))
            bUpdate = true;

        if (updateList(entity.getItemValue(OwnerPlugin.OWNER), from, to, replace))
            bUpdate = true;

        // support deprecated field
        if (updateList(entity.getItemValue("namOwner"), from, to, replace))
            bUpdate = true;

        // !! We do not replace the creator!! - see issue #350
        /*
         * if (updateList(entity.getItemValue("$Creator"), from, to, false)) bUpdate =
         * true;
         */

        if (bUpdate) {
            // create log entry....
            String summary = "Rename: " + from + " -> " + to + " (replace=" + replace + ")";
            entity.appendItemValue("txtAdminpLog", new Date(System.currentTimeMillis()) + " " + summary);
            documentService.save(entity);
            logger.log(Level.FINEST, "......updated: {0}", entity.getItemValueString(WorkflowKernel.UNIQUEID));
        }
        return bUpdate;
    }

    /**
     * Update the values of a single list.
     * 
     * @param list
     * @param from
     * @param to
     * @param replace
     * @return true if the list was modified.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private boolean updateList(List list, String from, String to, boolean replace) {

        boolean update = false;

        if (list == null || list.isEmpty())
            return false;

        if (list.contains(from)) {

            if (to != null && !"".equals(to) && !list.contains(to)) {
                list.add(to);
                update = true;
            }

            if (replace) {
                while (list.contains(from)) {
                    list.remove(from);
                    update = true;
                }
            }

        }

        return update;
    }

}
