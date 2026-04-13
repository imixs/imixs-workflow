/****************************************************************************
 * Copyright (c) 2026 Imixs Software Solutions GmbH and others.
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
package org.imixs.workflow.engine.cluster;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.FileData;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.EventLogService;
import org.imixs.workflow.engine.cluster.events.ArchiveEvent;
import org.imixs.workflow.engine.cluster.exceptions.ClusterException;
import org.imixs.workflow.engine.cluster.exceptions.DataException;
import org.imixs.workflow.engine.jpa.EventLog;
import org.imixs.workflow.xml.XMLDocument;
import org.imixs.workflow.xml.XMLDocumentAdapter;

import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;

import jakarta.ejb.Stateless;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

/**
 * The DataService is used to store a imixs snapshot into the cluster
 * keyspace.
 * 
 * @author rsoika
 * 
 */
@Stateless
public class DataService {

    public final static String ITEM_MD5_CHECKSUM = "md5checksum";
    public final static String ITEM_SNAPSHOT_HISTORY = ".history"; // optional historical snapshots
    private static final String REGEX_SNAPSHOTID = "([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}-[0-9]{13,15})";
    // private static final String REGEX_OLD_SNAPSHOTID =
    // "([0-9a-f]{8}-.*|[0-9a-f]{11}-.*)";

    private static Logger logger = Logger.getLogger(DataService.class.getName());

    @Inject
    EventLogService eventLogService;

    @Inject
    ClusterService clusterService;

    @Inject
    protected Event<ArchiveEvent> events;

    /**
     * This method saves a ItemCollection into the KeySpace.
     * <p>
     * 
     * @param snapshot - ItemCollection object
     * @param session  - cassandra session
     * @throws DataException
     * @throws ClusterException
     */
    public void saveSnapshot(ItemCollection snapshot) throws DataException, ClusterException {

        long l = System.currentTimeMillis();

        // String snapshotID = snapshot.getUniqueID();
        String snapshotID = snapshot.getItemValueString(ClusterService.SNAPSHOTID);
        logger.info("├── 🔃 Save Snapshot :'" + snapshotID + "'");
        if (!isSnapshotID(snapshotID)) {
            throw new DataException(DataException.INVALID_DOCUMENT_OBJECT,
                    "unexpected '' format: '" + snapshotID + "'");
        }

        if (!snapshot.hasItem("$modified")) {
            throw new DataException(DataException.INVALID_DOCUMENT_OBJECT,
                    "missing item '$modified' for snapshot " + snapshotID);
        }

        logger.log(Level.INFO,
                "│   ├── ⬆️  Snapshot Item count=" + snapshot.getItemList().keySet().size());
        // verify if this snapshot is already stored - if so, we do not overwrite
        // the origin data. See issue #40
        // For example this situation also occurs when restoring a remote snapshot.
        if (existSnapshot(snapshotID)) {
            // skip!
            logger.info("│   ├── ⬆️  Snapshot '" + snapshotID + "' already exits....");
            return;
        }

        // extract 2de78aec-6f14-4345-8acf-dd37ae84875d-1530315900599
        // String originUnqiueID = getUniqueIDBySnapshotID(snapshotID);

        // extract content into the table 'documents'....
        extractDocuments(snapshot);

        clusterService.getSession()
                .execute(SimpleStatement.newInstance(ClusterService.STATEMENT_UPSET_SNAPSHOTS, snapshotID,
                        ByteBuffer.wrap(getRawData(snapshot))));

        clusterService.getSession()
                .execute(SimpleStatement.newInstance(ClusterService.STATEMENT_UPSET_SNAPSHOTS_BY_UNIQUEID,
                        snapshot.getUniqueID(), snapshotID));

        java.time.LocalDate ld = Instant.ofEpochMilli(snapshot.getItemValueDate("$modified").getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        clusterService.getSession()
                .execute(SimpleStatement.newInstance(ClusterService.STATEMENT_UPSET_SNAPSHOTS_BY_MODIFIED, ld,
                        snapshot.getUniqueID()));

        cleanupSnapshotHistory(snapshot);

        // Finally we fire the ArchiveEvent ON_ARCHIVE
        if (events != null) {
            events.fire(new ArchiveEvent(snapshot, ArchiveEvent.ON_ARCHIVE));
        } else {
            logger.warning("Missing CDI support for Event<ArchiveEvent> !");
        }

        logger.log(Level.INFO, "└── ☑️  Save Snapshot successful in " + (System.currentTimeMillis() - l) + "ms");

    }

    /**
     * This method test if a snapshot recored with a given ID already exists.
     * 
     * @param snapshotID
     * @return true if the snapshot exists.
     * @throws ClusterException
     */
    public boolean existSnapshot(String snapshotID) throws ClusterException {
        ResultSet rs = clusterService.getSession()
                .execute(clusterService.getPsSelectSnapshotID().bind(snapshotID));
        return rs.one() != null;
    }

    /**
     * This method loads a snapshot from the cassandra cluster. The snapshot data
     * includes also the associated document data. In case you need only the
     * snapshot data without documents use loadSnapshot(id, false).
     * 
     * @param snapshotID - snapshot id
     * @return snapshot data including documents
     * @throws DataException
     * @throws ClusterException
     */
    public ItemCollection loadSnapshot(String snapshotID) throws DataException, ClusterException {
        return loadSnapshot(snapshotID, true);
    }

    /**
     * This method loads a snapshot form the cassandra cluster. The snapshot data
     * includes also the associated document data.
     * 
     * @param snapshotID - snapshot id
     * @return snapshot data
     * @throws DataException
     * @throws ClusterException
     */
    public ItemCollection loadSnapshot(String snapshotID, boolean mergeDocuments)
            throws DataException, ClusterException {

        long l = System.currentTimeMillis();

        logger.log(Level.INFO, "├── 🔃 Load Snapshot :'" + snapshotID + "'");

        // Check if the snapshot is still pending in the EventLog — load directly from
        // there
        List<EventLog> pending = eventLogService.findEventsByRef(1, snapshotID,
                ClusterService.EVENTLOG_TOPIC_PERSIST,
                ClusterService.EVENTLOG_TOPIC_PERSIST + ".lock");

        if (pending != null && !pending.isEmpty()) {
            logger.log(Level.INFO, "│   ├── ⚡ Snapshot still pending in EventLog — loading directly from EventLog");
            Map<String, List<Object>> workitemData = pending.get(0).getData();
            if (workitemData != null) {
                logger.log(Level.INFO,
                        "└── ☑️ Load Snapshot from EventLog in " + (System.currentTimeMillis() - l) + "ms");
                return new ItemCollection(workitemData);
            }
        }

        // Default: load form Cassandra
        ItemCollection snapshot = new ItemCollection();

        ResultSet rs = clusterService.getSession()
                .execute(clusterService.getPsSelectSnapshot().bind(snapshotID));
        Row row = rs.one();

        if (row != null) {
            // Load ItemCollection object from ByteBuffer
            ByteBuffer data = row.getByteBuffer(ClusterService.COLUMN_DATA);
            if (data != null) {
                byte[] bytes = new byte[data.remaining()];
                data.get(bytes);
                snapshot = getItemCollection(bytes);
                // Optionally merge associated document data
                if (mergeDocuments) {
                    mergeDocumentData(snapshot);
                }

                // finally remove the snapshot prefix

                logger.log(Level.INFO, "│   ├── ⬇️ Snapshot Item count=" + snapshot.getItemList().keySet().size());

            } else {
                logger.warning("no data found for snapshotId '" + snapshotID + "'");
            }
        }
        // Row == null: return empty ItemCollection (already initialized above)
        logger.log(Level.INFO, "└── ☑️ Load Snapshot successful in " + (System.currentTimeMillis() - l) + "ms");
        return snapshot;
    }

    /**
     * This method loads all existing snapshotIDs for a given unqiueID.
     * <p>
     * 
     * @param uniqueID
     * @param maxCount   - max search result
     * @param descending - sort result descending
     * 
     * @return list of snapshots
     * @throws ClusterException
     */
    public List<String> loadSnapshotsByUniqueID(String uniqueID, int maxCount, boolean descending)
            throws ClusterException {
        boolean debug = logger.isLoggable(Level.FINE);
        List<String> result = new ArrayList<>();

        // Build the base query — ORDER BY and LIMIT are dynamic, so no prepared
        // statement possible here
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM snapshots_by_uniqueid WHERE uniqueid='" + uniqueID + "'");

        if (descending) {
            sql.append(" ORDER BY snapshot DESC");
        }
        if (maxCount > 0) {
            sql.append(" LIMIT ").append(maxCount);
        }

        if (debug) {
            logger.finest("......search snapshots for uniqueID: " + sql);
        }

        ResultSet rs = clusterService.getSession().execute(sql.toString());
        for (Row row : rs) {
            result.add(row.getString(ClusterService.COLUMN_SNAPSHOT));
        }

        return result;
    }

    /**
     * This method loads all exsting snapshotIDs for a given date.
     * 
     * @param date
     * @return list of snapshots or an empty list if no snapshots exist for the
     *         given date
     * @throws ClusterException
     */
    public List<String> loadSnapshotsByDate(java.time.LocalDate date) throws ClusterException {
        boolean debug = logger.isLoggable(Level.FINE);
        List<String> result = new ArrayList<>();
        if (debug) {
            logger.finest("......load snapshots by date: " + date);
        }
        ResultSet rs = clusterService.getSession()
                .execute(clusterService.getPsSelectSnapshotsByModified().bind(date));
        for (Row row : rs) {
            result.add(row.getString(ClusterService.COLUMN_SNAPSHOT));
        }
        return result;
    }

    /**
     * This helper method loades the content of a document defned by a FileData
     * object. A document is uniquely identified by its md5 checksum which is part
     * of the FileData custom attributes. The data of the document is stored in
     * split 1md data blocks in the tabe 'documents_data'
     * 
     * @param itemCol
     * @throws DataException
     * @throws ClusterException
     */
    public FileData loadFileData(FileData fileData) throws DataException, ClusterException {
        // read md5 form custom attributes
        ItemCollection customAttributes = new ItemCollection(fileData.getAttributes());
        String md5 = customAttributes.getItemValueString(ITEM_MD5_CHECKSUM);
        // now we have all the bytes...
        byte[] allData = loadFileContent(md5);
        if (allData == null) {
            return null;
        } else {
            return new FileData(fileData.getName(), allData, fileData.getContentType(), fileData.getAttributes());
        }
    }

    /**
     * This helper method loads the content of a document defned by its MD5
     * checksum. The data of the document is stored in chunked 1md data blocks in
     * the table 'documents_data'
     * 
     * @param itemCol
     * @throws DataException
     * @throws ClusterException
     */
    public byte[] loadFileContent(String md5) throws DataException, ClusterException {
        if (md5 == null || md5.isEmpty()) {
            return null;
        }
        boolean debug = logger.isLoggable(Level.FINE);
        if (debug) {
            logger.finest("......search MD5 entry: " + md5);
        }

        ResultSet rs = clusterService.getSession()
                .execute(clusterService.getPsSelectDocuments().bind(md5));

        ByteArrayOutputStream bOutput = new ByteArrayOutputStream(1024 * 1024);
        try {
            for (Row row : rs) {
                int sort_id = row.getInt(ClusterService.COLUMN_SORT_ID);
                String data_id = row.getString(ClusterService.COLUMN_DATA_ID);
                if (debug) {
                    logger.finest("......load 1mb data block: sort_id=" + sort_id + " data_id=" + data_id);
                }
                ResultSet rs_data = clusterService.getSession()
                        .execute(clusterService.getPsSelectDocumentsData().bind(data_id));
                Row row_data = rs_data.one();
                if (row_data != null) {
                    ByteBuffer byteDataBlock = row_data.getByteBuffer(ClusterService.COLUMN_DATA);
                    byte[] bytes = new byte[byteDataBlock.remaining()];
                    byteDataBlock.get(bytes);
                    bOutput.write(bytes);
                } else {
                    logger.warning("Document Data missing: MD5=" + md5
                            + " sort_id=" + sort_id + " data_id=" + data_id);
                }
            }
            return bOutput.toByteArray();
        } catch (IOException e) {
            throw new DataException(DataException.INVALID_DOCUMENT_OBJECT,
                    "failed to load document data: " + e.getMessage(), e);
        }
    }

    /**
     * This method loads the metadata object represended by an ItemCollection. The
     * snapshot id for the metadata object is always "0". This id is reserverd for
     * metadata only.
     * <p>
     * If no metadata object yet exists, the method returns an empty ItemCollection.
     * <p>
     * The method expects a valid session instance which must be closed by the
     * client.
     * 
     * @return metadata object
     * @throws DataException
     * @throws ClusterException
     */
    public ItemCollection loadMetadata() throws DataException, ClusterException {
        return loadSnapshot("0");
    }

    /**
     * This method saves the metadata represented by an ItemCollection. The snapshot
     * id for the metadata object is always "0". This id is reserverd for metadata
     * only.
     * <p>
     * The method expects a valid session instance which must be closed by the
     * client.
     * 
     * @param itemCol - metadata
     * @throws DataException
     * @throws ClusterException
     */
    public void saveMetadata(ItemCollection metadata) throws DataException, ClusterException {
        // upset document....
        clusterService.getSession()
                .execute(SimpleStatement.newInstance(ClusterService.STATEMENT_UPSET_SNAPSHOTS, "0",
                        ByteBuffer.wrap(getRawData(metadata))));
    }

    /**
     * This method deletes a single snapshot instance.
     * <p>
     * The method also deletes the documents and all relations
     * 
     * @param snapshotID - id of the snapshot
     * @throws DataException
     * @throws ClusterException
     */
    public void deleteSnapshot(String snapshotID) throws DataException, ClusterException {
        logger.finest("......delete snapshot and documents for: " + snapshotID);
        String uniqueID = this.getUniqueIDBySnapshotID(snapshotID);
        ItemCollection snapshot = loadSnapshot(snapshotID, false);

        clusterService.getSession()
                .execute(clusterService.getPsDeleteSnapshots().bind(snapshotID));

        clusterService.getSession()
                .execute(clusterService.getPsDeleteSnapshotsByUniqueID().bind(uniqueID, snapshotID));

        long modifiedTime = 0;
        if (snapshot != null) {
            Date modified = snapshot.getItemValueDate("$modified");
            if (modified == null) {
                logger.warning("Snapshot Object '" + snapshotID + "' has no '$modified' item!");
                modifiedTime = this.getSnapshotTime(snapshotID);
            } else {
                modifiedTime = modified.getTime();
            }
        } else {
            logger.warning("Snapshot Object '" + snapshotID + "' not found in archive!");
        }

        LocalDate ld = Instant.ofEpochMilli(modifiedTime)
                .atZone(ZoneId.systemDefault()).toLocalDate();

        clusterService.getSession()
                .execute(clusterService.getPsDeleteSnapshotsByModified().bind(ld, snapshotID));

        deleteDocuments(snapshot);
    }

    /**
     * Converts a ItemCollection into a XMLDocument and returns the byte data.
     * 
     * @param itemCol
     * @return
     * @throws DataException
     */
    public byte[] getRawData(ItemCollection itemCol) throws DataException {
        byte[] data = null;
        // create byte array from XMLDocument...
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            JAXBContext context;
            context = JAXBContext.newInstance(XMLDocument.class);
            Marshaller m = context.createMarshaller();
            XMLDocument xmlDocument = XMLDocumentAdapter.getDocument(itemCol);
            m.marshal(xmlDocument, outputStream);
            data = outputStream.toByteArray();
        } catch (JAXBException e) {
            throw new DataException(DataException.INVALID_DOCUMENT_OBJECT, e.getMessage(), e);
        }

        return data;
    }

    /**
     * Converts a byte array into a XMLDocument and returns the ItemCollection
     * object.
     * 
     * @throws DataException
     *
     */
    public ItemCollection getItemCollection(byte[] source) throws DataException {
        ByteArrayInputStream bis = new ByteArrayInputStream(source);
        try {
            JAXBContext context;
            context = JAXBContext.newInstance(XMLDocument.class);
            Unmarshaller m = context.createUnmarshaller();
            Object jaxbObject = m.unmarshal(bis);
            if (jaxbObject == null) {
                throw new RuntimeException("readCollection error - wrong xml file format - unable to read content!");
            }
            XMLDocument xmlDocument = (XMLDocument) jaxbObject;
            return XMLDocumentAdapter.putDocument(xmlDocument);
        } catch (JAXBException e) {
            throw new DataException(DataException.INVALID_DOCUMENT_OBJECT, e.getMessage(), e);
        }
    }

    /**
     * This method returns true if the given id is a valid Snapshot id (UUI +
     * timestamp
     * <p>
     * We also need to support the old snapshto format
     * <code>4832b09a1a-20c38abd-1519421083952</code>
     * 
     * @param uid
     * @return
     */
    public boolean isSnapshotID(String uid) {
        boolean valid = uid.matches(REGEX_SNAPSHOTID);
        return valid;
    }

    /**
     * Returns the from a
     * 
     * @param snapshotID
     * @return
     */
    public String getUniqueIDBySnapshotID(String snapshotID) {
        if (snapshotID != null && snapshotID.contains("-")) {
            return snapshotID.substring(0, snapshotID.lastIndexOf("-"));
        }
        return null;
    }

    /**
     * Returns the snapshot time n millis of a
     * 
     * @param snapshotID
     * @return long - snapshot time
     */
    public long getSnapshotTime(String snapshotID) {
        if (snapshotID != null && snapshotID.contains("-")) {
            String sTime = snapshotID.substring(snapshotID.lastIndexOf("-") + 1);
            return Long.parseLong(sTime);
        }
        return 0;
    }

    /**
     * count total value size...
     * 
     * @param xmldoc
     * @return
     */
    public long calculateSize(XMLDocument xmldoc) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(baos);
            oos.writeObject(xmldoc);
            oos.close();
            return baos.size();
        } catch (IOException e) {
            logger.warning("...unable to calculate document size!");
        } finally {
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e) {
                    logger.warning("failed to close stream");
                    e.printStackTrace();
                }
            }
        }

        return 0;

    }

    /**
     * returns the date tiem from a date in iso format
     * 
     * @return
     */
    public String getSyncPointISO(long point) {
        SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        Date date = new Date(point);
        return dt.format(date);
    }

    /**
     * This method deletes older snapshots exceeding the optional .history.
     * If no .history is defined or is 0 than no historical snapshots will
     * be deleted.
     * 
     * @param snapshot
     * @throws DataException
     * @throws ClusterException
     */
    private void cleanupSnapshotHistory(ItemCollection snapshot) throws DataException, ClusterException {
        boolean debug = logger.isLoggable(Level.FINE);
        int snapshotHistory = snapshot.getItemValueInteger(ITEM_SNAPSHOT_HISTORY);
        if (snapshotHistory <= 0) {
            return;
        }

        if (debug) {
            logger.finest(".......history=" + snapshotHistory);
        }
        String uniqueid = this.getUniqueIDBySnapshotID(snapshot.getUniqueID());

        // Step 1: Find the oldest snapshot that is still within the allowed history
        // window
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM snapshots_by_uniqueid WHERE uniqueid='" + uniqueid + "'");
        sql.append(" ORDER BY snapshot DESC");
        sql.append(" LIMIT ").append(snapshotHistory + 1);

        if (debug) {
            logger.finest("......search snapshots for uniqueid: " + sql);
        }
        ResultSet rs = clusterService.getSession().execute(sql.toString());

        int snapshotcount = 0;
        String oldestSnapshotID = null;
        for (Row row : rs) {
            oldestSnapshotID = row.getString(ClusterService.COLUMN_SNAPSHOT);
            snapshotcount++;
        }

        // If the total number of snapshots is within the allowed history, nothing to do
        if (snapshotcount < snapshotHistory) {
            return;
        }

        // Step 2: Find all snapshots older than the oldest allowed snapshot
        sql = new StringBuilder(
                "SELECT * FROM snapshots_by_uniqueid WHERE uniqueid='" + uniqueid + "'");
        sql.append(" AND snapshot<='" + oldestSnapshotID + "'");
        sql.append(" ORDER BY snapshot ASC");
        sql.append(" LIMIT 100");

        if (debug) {
            logger.finest("......search outdated snapshots: " + sql);
        }
        rs = clusterService.getSession().execute(sql.toString());

        // Step 3: Delete all outdated snapshots
        int deletions = 0;
        for (Row row : rs) {
            String id = row.getString(ClusterService.COLUMN_SNAPSHOT);
            deleteSnapshot(id);
            deletions++;
        }

        if (deletions >= 2) {
            logger.info("...deleted " + deletions + " deprecated snapshots from history (" + uniqueid + ")");
        }
    }

    /**
     * This helper method extracts the content of attached documents and stores the
     * content into the documents table space. A document is uniquely identified by
     * its md5 checksum.
     * 
     * @param itemCol
     * @throws DataException
     * @throws ClusterException
     */
    private void extractDocuments(ItemCollection itemCol) throws DataException, ClusterException {
        boolean debug = logger.isLoggable(Level.FINE);
        byte[] empty = {};
        List<FileData> files = itemCol.getFileData();
        for (FileData fileData : files) {
            try {
                if (debug) {
                    logger.finest("... extract fileData objects: " + files.size() + " fileData objects found....");
                }
                if (fileData.getContent() != null && fileData.getContent().length > 0) {
                    String md5 = fileData.generateMD5();

                    // Test if md5 is already stored
                    if (debug) {
                        logger.finest("......search MD5 entry: " + md5);
                    }
                    ResultSet rs = clusterService.getSession()
                            .execute(clusterService.getPsSelectMD5().bind(md5));
                    Row row = rs.one();
                    if (row == null) {
                        storeDocument(md5, fileData.getContent());
                    } else {
                        if (debug) {
                            logger.finest("......update fileData not necessary because object: " + md5
                                    + " is already stored!");
                        }
                    }

                    // Update snapshots_by_document reference
                    // CORRECT
                    clusterService.getSession()
                            .execute(clusterService.getPsUpsertSnapshotsByDocument().bind(md5, itemCol.getUniqueID()));

                    // Remove file content from itemCol
                    if (debug) {
                        logger.finest("drop content for file '" + fileData.getName() + "'");
                    }
                    itemCol.addFileData(new FileData(fileData.getName(), empty, fileData.getContentType(),
                            fileData.getAttributes()));
                }
            } catch (NoSuchAlgorithmException e) {
                throw new DataException(DataException.MD5_ERROR, "can not compute md5 of document - " + e.getMessage());
            }
        }
    }

    /**
     * This helper method deletes the content of attached documents A document is
     * uniquely identified by its md5 checksum.
     * 
     * @param itemCol
     * @throws DataException
     * @throws ClusterException
     */
    private void deleteDocuments(ItemCollection itemCol) throws DataException, ClusterException {
        if (itemCol == null) {
            return;
        }
        boolean debug = logger.isLoggable(Level.FINE);
        List<FileData> files = itemCol.getFileData();
        for (FileData fileData : files) {
            try {
                if (debug) {
                    logger.finest(
                            "......delete fileData ref and objects: " + files.size() + " fileData objects found....");
                }
                if (fileData.getContent() != null && fileData.getContent().length > 0) {
                    String md5 = fileData.generateMD5();

                    // Delete reference in snapshots_by_document
                    clusterService.getSession()
                            .execute(clusterService.getPsDeleteSnapshotsByDocument().bind(md5, itemCol.getUniqueID()));

                    // Check if other snapshots still reference this md5
                    ResultSet rs = clusterService.getSession()
                            .execute(clusterService.getPsSelectSnapshotsByDocument().bind(md5));
                    if (rs.one() != null) {
                        // Other snapshots still reference this document — skip deletion
                        return;
                    }

                    // Collect all data block references for this md5
                    ResultSet rs_sub = clusterService.getSession()
                            .execute(clusterService.getPsSelectDocuments().bind(md5));
                    List<String> dataIDs = new ArrayList<>();
                    List<Integer> sortIDs = new ArrayList<>();
                    for (Row row : rs_sub) {
                        int sort_id = row.getInt(ClusterService.COLUMN_SORT_ID);
                        String data_id = row.getString(ClusterService.COLUMN_DATA_ID);
                        logger.info("......delete 1mb data block: sort_id=" + sort_id + " data_id=" + data_id);
                        dataIDs.add(data_id);
                        sortIDs.add(sort_id);
                    }

                    // Delete all data blocks
                    for (String data_id : dataIDs) {
                        clusterService.getSession()
                                .execute(clusterService.getPsDeleteDocumentsData().bind(data_id));
                    }

                    // Delete all document index entries
                    for (int sort_id : sortIDs) {
                        clusterService.getSession()
                                .execute(clusterService.getPsDeleteDocuments().bind(md5, sort_id));
                    }
                }
            } catch (NoSuchAlgorithmException e) {
                throw new DataException(DataException.MD5_ERROR, "can not compute md5 of document - " + e.getMessage());
            }
        }
    }

    /**
     * This method stores a single document identified by the MD5 checksum.
     * <p>
     * The method splits the data into 1mb blocks stored in the table
     * 'documents_data'
     * 
     * 
     * @param md5
     * @param data
     * @param session
     * @throws ClusterException
     */
    private void storeDocument(String md5, byte[] data) throws ClusterException {
        boolean debug = logger.isLoggable(Level.FINE);
        // split the data into 1md blocks....
        DocumentSplitter documentSplitter = new DocumentSplitter(data);
        Iterator<byte[]> it = documentSplitter.iterator();
        int sort_id = 0;
        while (it.hasNext()) {
            String data_id = WorkflowKernel.generateUniqueID();
            if (debug) {
                logger.finest("......write new 1mb data block: sort_id=" + sort_id + " data_id=" + data_id);
            }
            byte[] chunk = it.next();
            // write 1MB chunk into cassandra....
            clusterService.getSession()
                    .execute(SimpleStatement.newInstance(ClusterService.STATEMENT_UPSET_DOCUMENTS_DATA, data_id,
                            ByteBuffer.wrap(chunk)));
            // write sort_id....
            clusterService.getSession()
                    .execute(SimpleStatement.newInstance(ClusterService.STATEMENT_UPSET_DOCUMENTS, md5, sort_id,
                            data_id));
            // increase sort_id
            sort_id++;
        }
        if (debug) {
            logger.finest("......stored filedata object: " + md5);
        }
    }

    /**
     * This helper method merges the content of attached documents into a
     * itemCollection. A document is uniquely identified by its md5 checksum. The
     * data of the document is split into 1md data blocks in the tabe
     * 'documents_data'
     * 
     * @param itemCol
     * @throws DataException
     * @throws ClusterException
     */
    private void mergeDocumentData(ItemCollection itemCol) throws DataException, ClusterException {
        boolean debug = logger.isLoggable(Level.FINE);
        List<FileData> files = itemCol.getFileData();
        for (FileData fileData : files) {
            // first verify if content is already stored.
            if (debug) {
                logger.finest("... merge fileData objects: " + files.size() + " fileData objects defined....");
            }
            // add document if not exists
            if (fileData.getContent() == null || fileData.getContent().length == 0) {
                fileData = loadFileData(fileData);
                itemCol.addFileData(fileData);
            }

        }
    }

}
