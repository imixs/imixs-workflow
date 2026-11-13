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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.index.UpdateService;
import org.imixs.workflow.engine.jpa.Document;
import org.imixs.workflow.exceptions.InvalidAccessException;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * The JobHandlerRebuildIndex rebuilds the search index for all documents
 * stored in the database.
 * <p>
 * The search index can not be used to select the documents because the index
 * itself is the subject of this job. Therefore the documents are read directly
 * by JPA in the order of their creation date, starting at 1970/01/01.
 * <p>
 * The following job attributes are supported:
 * <ul>
 * <li>numBlockSize - number of documents per timer run (default 1000)</li>
 * </ul>
 * The interval between two runs is controlled by the AdminPService with the
 * attribute 'numInterval'. The throughput of the job is therefore
 * numBlockSize / numInterval. Note that each run is executed in one
 * transaction, so a very large block size can exceed the transaction timeout.
 * <p>
 * <strong>Paging:</strong> After each run the creation timestamp of the last
 * indexed document is stored in the item '_syncpoint'. The next run continues
 * with all documents created after this timestamp. Documents sharing the same
 * creation timestamp are always read as one group, so a block can slightly
 * exceed the block size.
 * <p>
 * <strong>Error handling:</strong> On any problem the job stops with an
 * AdminPException. The syncpoint of the last successfully indexed group is
 * kept, so a restart of the job continues at this position.
 *
 * @see AdminPService
 * @author rsoika
 */
@DeclareRoles({ "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
@Stateless
@LocalBean
public class JobHandlerRebuildIndex implements JobHandler {

    // readable copies of the current position - kept for compatibility
    public static final String ITEM_SYNCPOINT = "syncpoint";
    public static final String ITEM_SYNCDATE = "syncdate";
    public static final String SNAPSHOT_TYPE_PRAFIX = "snapshot-";
    public static final String REBUILD_FAILED = "REBUILD_FAILED";

    // internal resume position of the job
    private static final String ITEM_RESUME_SYNCPOINT = "_syncpoint";
    private static final int DEFAULT_BLOCK_SIZE = 1000;
    private static final int READ_AHEAD = 32;

    private static final String SELECT_DOCUMENTS = "SELECT document FROM Document AS document";
    private static final String COUNT_DOCUMENTS = "SELECT COUNT(document) FROM Document AS document";
    private static final String ORDER_BY_CREATED = " ORDER BY document.created ASC";

    @PersistenceContext(unitName = "org.imixs.workflow.jpa")
    private EntityManager manager;

    @Inject
    UpdateService updateService;

    private static final Logger logger = Logger.getLogger(JobHandlerRebuildIndex.class.getName());

    /**
     * Reads the next block of documents after the current syncpoint and updates
     * the search index.
     * <p>
     * The method runs in an isolated new transaction because it flushes the
     * local persistence manager.
     *
     * @param adminp the job description
     * @return the updated job description
     * @throws AdminPException if the index can not be updated
     */
    @Override
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public ItemCollection run(ItemCollection adminp) throws AdminPException {
        long lProfiler = System.currentTimeMillis();
        String jobId = adminp.getUniqueID();
        long syncPoint = adminp.getItemValueLong(ITEM_RESUME_SYNCPOINT);
        int totalCount = adminp.getItemValueInteger("numUpdates");

        int blockSize = adminp.getItemValueInteger("numBlockSize");
        if (blockSize <= 0) {
            blockSize = DEFAULT_BLOCK_SIZE;
            adminp.replaceItemValue("numBlockSize", blockSize);
        }
        adminp.replaceItemValue("$WorkflowSummary", "Rebuild search index");

        logger.info("├── ⚙ Run rebuild index job....");
        logger.info("│   ├── jobId: " + jobId);
        logger.info("│   ├── syncpoint: " + new Date(syncPoint));
        logger.info("│   ├── blockSize: " + blockSize);

        // documents read in this run (including documents that could not be indexed)
        int runCount = 0;
        // documents indexed in this run
        int indexCount = 0;
        boolean completed = false;

        try {
            long remaining = countRemainingDocuments(syncPoint);
            logger.info("│   ├── ↻ " + remaining + " documents remaining");
            logger.info("│   ├── ⚙ Processing next " + blockSize + " documents...");

            while (runCount < blockSize) {
                List<Document> documents = findNextDocumentsBySyncPoint(syncPoint);
                if (documents.isEmpty()) {
                    // no more documents - the job is finished
                    manager.flush();
                    completed = true;
                    break;
                }

                long nextSyncPoint = syncPoint;
                List<ItemCollection> resultList = new ArrayList<>();
                for (Document doc : documents) {
                    nextSyncPoint = doc.getCreated().getTimeInMillis();
                    try {
                        resultList.add(new ItemCollection(doc.getData()));
                    } catch (InvalidAccessException e) {
                        logger.log(Level.WARNING, "│   ├── ⚠️ Unable to index document ''{0}'': {1}",
                                new Object[] { doc.getId(), e.getMessage() });
                    }
                    // detach to keep the persistence context small
                    manager.detach(doc);
                }

                updateService.updateIndex(resultList);
                manager.flush();

                // move the syncpoint only after the group was indexed successfully
                syncPoint = nextSyncPoint;
                runCount += documents.size();
                indexCount += resultList.size();
            }
        } catch (Exception e) {
            // store the position of the last indexed group so a restart continues here
            totalCount += indexCount;
            updateJobStatus(adminp, syncPoint, totalCount, indexCount);
            logger.log(Level.SEVERE, "├── ⚠️ Job " + AdminPService.JOB_REBUILD_INDEX + " (" + jobId
                    + ") failed at syncpoint " + new Date(syncPoint) + " - " + e.getMessage(), e);
            throw new AdminPException(REBUILD_FAILED,
                    "Rebuild index failed at syncpoint " + new Date(syncPoint) + ": " + e.getMessage());
        }

        totalCount += indexCount;
        updateJobStatus(adminp, syncPoint, totalCount, indexCount);
        adminp.replaceItemValue(JobHandler.ISCOMPLETED, completed);

        long time = System.currentTimeMillis() - lProfiler;
        // '#' avoids locale specific grouping separators in the log output
        logger.log(Level.INFO,
                "├── ✓ Job " + AdminPService.JOB_REBUILD_INDEX
                        + " ({0}) Block finished - {1,number,#} documents indexed in {2,number,#} ms (in total: {3,number,#})",
                new Object[] { jobId, indexCount, time, totalCount });
        return adminp;
    }

    /**
     * Stores the current position and the counters in the job description.
     */
    private void updateJobStatus(ItemCollection adminp, long syncPoint, int totalCount, int lastCount) {
        adminp.replaceItemValue(ITEM_RESUME_SYNCPOINT, syncPoint);
        adminp.replaceItemValue(ITEM_SYNCPOINT, syncPoint);
        adminp.replaceItemValue(ITEM_SYNCDATE, new Date(syncPoint));
        adminp.replaceItemValue("numUpdates", totalCount);
        adminp.replaceItemValue("numProcessed", totalCount);
        adminp.replaceItemValue("numLastCount", lastCount);
    }

    /**
     * Counts all documents created after the given syncpoint which are relevant
     * for the search index.
     *
     * @param syncPoint timestamp in milliseconds
     * @return number of remaining documents
     */
    private long countRemainingDocuments(long syncPoint) {
        Query q = manager.createQuery(COUNT_DOCUMENTS + buildWhereClause(">", syncPoint));
        return ((Number) q.getSingleResult()).longValue();
    }

    /**
     * Loads the next documents after a given syncpoint (timestamp in
     * milliseconds) compared with the creation timestamp of a document entity.
     * <p>
     * More than one document can share the same creation timestamp. A group of
     * such documents is never split, because the next call continues after the
     * timestamp of the last returned document. If the read ahead ends inside such
     * a group, the whole group is reloaded without a size limit.
     *
     * @param syncPoint timestamp in milliseconds
     * @return list of documents ordered by creation date, empty if no more
     *         documents exist
     */
    @SuppressWarnings("unchecked")
    private List<Document> findNextDocumentsBySyncPoint(long syncPoint) {
        Query q = manager.createQuery(SELECT_DOCUMENTS + buildWhereClause(">", syncPoint) + ORDER_BY_CREATED);
        q.setFirstResult(0);
        q.setMaxResults(READ_AHEAD);
        List<Document> documentList = new ArrayList<>(q.getResultList());

        if (documentList.size() < READ_AHEAD) {
            // end of the list reached
            return documentList;
        }

        Document lastDocument = documentList.get(READ_AHEAD - 1);
        Document nextToLastDocument = documentList.get(READ_AHEAD - 2);
        long lastTimestamp = lastDocument.getCreated().getTimeInMillis();

        if (lastTimestamp != nextToLastDocument.getCreated().getTimeInMillis()) {
            // drop the last document - it is read again by the next call. This avoids
            // that a group with the same timestamp beyond the read ahead is split.
            documentList.remove(READ_AHEAD - 1);
            manager.detach(lastDocument);
            return documentList;
        }

        // the list ends with a group of documents sharing the same timestamp. The
        // group may continue beyond the read ahead, so it is removed here and
        // reloaded completely to avoid duplicates and gaps.
        logger.finest("......more than one document with the same creation timestamp found!");
        List<Document> result = new ArrayList<>();
        for (Document doc : documentList) {
            if (doc.getCreated().getTimeInMillis() == lastTimestamp) {
                manager.detach(doc);
            } else {
                result.add(doc);
            }
        }
        Query groupQuery = manager
                .createQuery(SELECT_DOCUMENTS + buildWhereClause("=", lastTimestamp) + ORDER_BY_CREATED);
        // no max results - the whole group must be read
        result.addAll(groupQuery.getResultList());
        return result;
    }

    /**
     * Builds the where clause selecting all documents relevant for the search
     * index, compared with the given creation timestamp.
     *
     * @param operator  comparison operator for the creation date ('&gt;' or '=')
     * @param syncPoint timestamp in milliseconds
     * @return JPQL where clause
     */
    private String buildWhereClause(String operator, long syncPoint) {
        // ISO date time format: '2016-08-25 01:23:46.0'
        DateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return " WHERE document.created " + operator + " '" + isoFormat.format(new Date(syncPoint)) + "'"
                + " AND NOT document.type LIKE '" + SNAPSHOT_TYPE_PRAFIX + "%'"
                + " AND NOT document.type LIKE 'workitemlob%'"
                + " AND document.type != 'event'";
    }
}