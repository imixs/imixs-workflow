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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RunAs;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.index.UpdateService;
import org.imixs.workflow.engine.jpa.Document;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.PluginException;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.logging.Level;

/**
 * JobHandler to rebuild the lucene fulltext index.
 * 
 * The job starts at 1970/01/01 and reads documents in sequence.
 * 
 * @author rsoika
 *
 */
@DeclareRoles({ "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
@Stateless
@LocalBean
public class JobHandlerRebuildIndex implements JobHandler {

    private static final int BLOCK_SIZE = 1000;

    @Inject
    @ConfigProperty(name = "lucene.rebuild.time_out", defaultValue = "" + AdminPService.DEFAULT_INTERVAL) 
    int time_out;

    private static final int READ_AHEAD = 32;
    public final static String ITEM_SYNCPOINT = "syncpoint";
    public final static String ITEM_SYNCDATE = "syncdate";
    public static final String SNAPSHOT_TYPE_PRAFIX = "snapshot-";

    @PersistenceContext(unitName = "org.imixs.workflow.jpa")
    private EntityManager manager;

    @Inject
    UpdateService updateService;

    private static final Logger logger = Logger.getLogger(JobHandlerRebuildIndex.class.getName());

    /**
     * This method runs the RebuildLuceneIndexJob. The job starts at creation date
     * 1970/01/01 and reads single documents in sequence.
     * <p>
     * After the run method is finished, the properties numIndex, numUpdates and
     * numProcessed are updated.
     * <p>
     * The method runs in an isolated new transaction because the method flushes the
     * local persistence manager.
     * 
     * @param adminp
     * @return true when finished
     * @throws AccessDeniedException
     * @throws PluginException
     */

    @Override
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public ItemCollection run(ItemCollection adminp) throws AdminPException {
        long lProfiler = System.currentTimeMillis();
        long syncPoint = adminp.getItemValueLong("_syncpoint");
        int totalCount = adminp.getItemValueInteger("numUpdates");
        int blockCount = 0;
        // The job blocksize will be ignored by the index job. Only the timeout matters

        // test if the job document provide a time_out otherwise we take the defaults.
        int jobTimeOut = adminp.getItemValueInteger("numinterval");
        if (jobTimeOut > 0) {
            // fix deprecated invtervall unit
            if (jobTimeOut <= 5) { // <5 seconds
                logger.log(Level.WARNING, "fix deprecated interval of {0} - set new job interval to {1}sec",
                        new Object[]{jobTimeOut, time_out});
                jobTimeOut = time_out;
            }
            // overwrite default time_out
            time_out = jobTimeOut;
        }
        // update interval
        adminp.setItemValue("numinterval", time_out);
        logger.log(Level.INFO, "...Job " + AdminPService.JOB_REBUILD_INDEX + " ({0}) - lucene.rebuild.time_out={1}",
                new Object[]{adminp.getUniqueID(), time_out});
        try {
            while (true) {
                List<ItemCollection> resultList = new ArrayList<ItemCollection>();
                List<Document> documents = findNextDocumentsBySyncPoint(syncPoint);
                if (documents != null && documents.size() > 0) {
                    for (Document doc : documents) {
                        // update syncpoint
                        syncPoint = doc.getCreated().getTimeInMillis();
                        try {
                            resultList.add(new ItemCollection(doc.getData()));
                        } catch (InvalidAccessException e) {
                            logger.log(Level.WARNING, "...unable to index document ''{0}'' {1}",
                                    new Object[]{doc.getId(), e.getMessage()});
                        }
                        // detach object!
                        manager.detach(doc);

                    }

                    // update the index
                    updateService.updateIndex(resultList);
                    manager.flush();

                    // update count
                    totalCount += resultList.size();
                    blockCount += resultList.size();
                    if (blockCount >= BLOCK_SIZE) {
                        long time = (System.currentTimeMillis() - lProfiler) / 1000;
                        if (time == 0) {
                            time = 1;
                        }
                        logger.log(Level.INFO,"...Job " + AdminPService.JOB_REBUILD_INDEX
                                + " ({0}) - ...{1} documents indexed in {2} sec. ... ",
                                new Object[]{adminp.getUniqueID(), totalCount, time});
                        blockCount = 0;
                    }
                } else {
                    // no more documents
                    manager.flush();
                    break;
                }

                // suspend job?
                long time = (System.currentTimeMillis() - lProfiler) / 1000;
                if (time == 0) {
                    time = 1;
                }
                if (time > time_out) { // suspend after 2 mintues (default 120)....
                    logger.log(Level.INFO,"...Job " + AdminPService.JOB_REBUILD_INDEX
                            + " ({0}) - suspended: {1} documents indexed in {2} sec. ",
                            new Object[]{adminp.getUniqueID(), totalCount, time});

                    adminp.replaceItemValue("_syncpoint", syncPoint);
                    adminp.replaceItemValue(JobHandler.ISCOMPLETED, false);
                    adminp.replaceItemValue("numUpdates", totalCount);
                    adminp.replaceItemValue("numProcessed", totalCount);
                    adminp.replaceItemValue("numLastCount", 0);
                    return adminp;
                }
            }
        } catch (Exception e) {
            // print exception and stop job
            logger.log(Level.SEVERE,"...Job " + AdminPService.JOB_REBUILD_INDEX
                    + " ({0}) - failed - {1} last syncpoint  {2} - {3}  documents reindexed....",
                    new Object[]{adminp.getUniqueID(), e.getMessage(), syncPoint, totalCount});
            e.printStackTrace();
            adminp.replaceItemValue(JobHandler.ISCOMPLETED, false);
            // update syncpoint
            Date syncDate = new Date(syncPoint);
            adminp.replaceItemValue("error", e.getMessage());
            adminp.replaceItemValue(ITEM_SYNCPOINT, syncPoint);
            adminp.replaceItemValue(ITEM_SYNCDATE, syncDate);
            adminp.replaceItemValue("numUpdates", totalCount);
            adminp.replaceItemValue("numProcessed", totalCount);
            adminp.replaceItemValue("numLastCount", 0);
            return adminp;
        }

        // completed
        long time = (System.currentTimeMillis() - lProfiler) / 1000;
        if (time == 0) {
            time = 1;
        }
        logger.log(Level.INFO,"...Job " + AdminPService.JOB_REBUILD_INDEX
                + " ({0}) - Finished: {1} documents indexed in {2} sec. ",
                new Object[]{adminp.getUniqueID(), totalCount, time});

        adminp.replaceItemValue(JobHandler.ISCOMPLETED, true);
        adminp.replaceItemValue("numUpdates", totalCount);
        adminp.replaceItemValue("numProcessed", totalCount);
        adminp.replaceItemValue("numLastCount", 0);
        return adminp;

    }

    /**
     * Loads the next documents by a given symcpoint (timestamp in milis) compared
     * with the created timestamp of a document entity.
     * <p>
     * It is possible that more than one document entities have the same created
     * timestamp. For that reason the method returns all documents with the same
     * timestamp in a collection.
     * 
     * @param lSyncpoint
     * @return a list of documents with the same creation timestamp after the given
     *         syncpoint. Returns null in case no more documents were found.
     */
    @SuppressWarnings("unchecked")
    private List<Document> findNextDocumentsBySyncPoint(long lSyncpoint) {

        Date syncpoint = new Date(lSyncpoint);
        // ISO date time format: '2016-08-25 01:23:46.0',
        DateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String query = "SELECT document FROM Document AS document ";
        query += " WHERE document.created > '" + isoFormat.format(syncpoint) + "'";
        query += " AND NOT document.type LIKE '" + SNAPSHOT_TYPE_PRAFIX + "%' ";
        query += " AND NOT document.type LIKE 'workitemlob%' ";
        query += " AND document.type != 'event' ";
        query += " ORDER BY document.created ASC";
        Query q = manager.createQuery(query);
        q.setFirstResult(0);
        q.setMaxResults(READ_AHEAD);
        List<Document> documentList = q.getResultList();
        if (documentList != null && documentList.size() > 0) {
            Document lastDocument = null;
            Document nextToLastDocument = null;

            // test if we have two documents with the same creation date (in seldom cases
            // possible)
            if (documentList.size() == READ_AHEAD) {
                lastDocument = documentList.get(READ_AHEAD - 1);
                nextToLastDocument = documentList.get(READ_AHEAD - 2);
                // now test if we have more than one document with the same timestamp at the end
                // of the list
                if (lastDocument != null && nextToLastDocument != null
                        && lastDocument.getCreated().equals(nextToLastDocument.getCreated())) {
                    logger.finest("......there are more than one document with the same creation timestamp!");
                    // lets build a new collection with the duplicated creation timestamp
                    syncpoint = new Date(lastDocument.getCreated().getTimeInMillis());
                    query = "SELECT document FROM Document AS document ";
                    query += " WHERE document.created = '" + isoFormat.format(syncpoint) + "'";
                    query += " AND NOT document.type LIKE '" + SNAPSHOT_TYPE_PRAFIX + "%' ";
                    query += " AND NOT document.type LIKE 'workitemlob%' ";
                    query += " AND document.type != 'event' ";
                    query += " ORDER BY document.created ASC";
                    q = manager.createQuery(query);
                    q.setFirstResult(0);
                    q.setMaxResults(READ_AHEAD);
                    documentList.addAll(q.getResultList());
                    return documentList;

                } else {
                    // we found exactly READ_AHEAD documents and the last two ones are not equal
                    // so we drop the last one of the result to avoid overlapping duplicates in the
                    // next block.
                    documentList.remove(lastDocument);
                    manager.detach(lastDocument);
                    return documentList;
                }
            } else {
                // we are at the end of the list
                return documentList;
            }
        }
        return null;
    }

}
