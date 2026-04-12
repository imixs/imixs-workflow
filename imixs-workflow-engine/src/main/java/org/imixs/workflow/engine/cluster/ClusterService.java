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

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.workflow.engine.EventLogService;
import org.imixs.workflow.engine.cluster.exceptions.ClusterException;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.InvalidKeyspaceException;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.servererrors.InvalidQueryException;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.inject.Inject;

/**
 * The ClusterService provides methods to persist the content of a Imixs
 * Document into a Cassandra keystore.
 * <p>
 * The service saves the content in XML format. The size of an XML
 * representation of a Imixs document is only slightly different in size from
 * the serialized map object. This is the reason why we do not store the
 * document map in a serialized object format.
 * <p>
 * The ClusterService creates a Core-KeySpace automatically which is used for
 * the internal management.
 * 
 * @author rsoika
 * 
 */
@DeclareRoles({ "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
@Singleton
@Startup
public class ClusterService {

    public static final String KEYSPACE_REGEX = "^[a-z_]*[^-]$";
    public static final int SEARCH_LIMIT_MAX = 100;

    // mandatory environment settings
    public static final String STORAGE_MODE_CLUSTER = "CLUSTER";
    public static final String STORAGE_MODE_LEGACY = "LEGACY";

    public static final String SNAPSHOTID = "$snapshotid";
    public static final String TYPE_PRAFIX = "snapshot-";

    public static final String ENV_WORKFLOW_CLUSTER_MODE = "WORKFLOW_CLUSTER_MODE";
    public static final String ENV_WORKFLOW_CLUSTER_CONTACTPOINTS = "WORKFLOW_CLUSTER_CONTACTPOINTS";
    public static final String ENV_WORKFLOW_CLUSTER_KEYSPACE = "WORKFLOW_CLUSTER_KEYSPACE";

    public static final int DIMENSIONS = 768;

    // optional environment settings
    public static final String ENV_WORKFLOW_CLUSTER_AUTH_USER = "WORKFLOW_CLUSTER_AUTH_USER";
    public static final String ENV_WORKFLOW_CLUSTER_AUTH_PASSWORD = "WORKFLOW_CLUSTER_AUTH_PASSWORD";
    public static final String ENV_WORKFLOW_CLUSTER_SSL = "WORKFLOW_CLUSTER_SSL";
    public static final String ENV_WORKFLOW_CLUSTER_SSL_TRUSTSTOREPATH = "WORKFLOW_CLUSTER_SSL_TRUSTSTOREPATH";
    public static final String ENV_WORKFLOW_CLUSTER_SSL_TRUSTSTOREPASSWORD = "WORKFLOW_CLUSTER_SSL_TRUSTSTOREPASSWORD";
    public static final String ENV_WORKFLOW_CLUSTER_SSL_KEYSTOREPATH = "WORKFLOW_CLUSTER_SSL_KEYSTOREPATH";
    public static final String ENV_WORKFLOW_CLUSTER_SSL_KEYSTOREPASSWORD = "WORKFLOW_CLUSTER_SSL_KEYSTOREPASSWORD";

    public static final String ENV_WORKFLOW_CLUSTER_REPLICATION_FACTOR = "WORKFLOW_CLUSTER_REPLICATION_FACTOR";
    public static final String ENV_WORKFLOW_CLUSTER_REPLICATION_CLASS = "WORKFLOW_CLUSTER_REPLICATION_CLASS";

    // table schemas
    public static final String TABLE_SCHEMA_SNAPSHOTS = "CREATE TABLE IF NOT EXISTS snapshots (snapshot text, data blob, PRIMARY KEY (snapshot))";
    public static final String TABLE_SCHEMA_SNAPSHOTS_BY_UNIQUEID = "CREATE TABLE IF NOT EXISTS snapshots_by_uniqueid (uniqueid text,snapshot text, PRIMARY KEY(uniqueid, snapshot));";
    public static final String TABLE_SCHEMA_SNAPSHOTS_BY_MODIFIED = "CREATE TABLE IF NOT EXISTS snapshots_by_modified (modified date,snapshot text,PRIMARY KEY(modified, snapshot));";
    public static final String TABLE_SCHEMA_DOCUMENTS = "CREATE TABLE IF NOT EXISTS documents (md5 text, sort_id int, data_id text, PRIMARY KEY (md5,sort_id))";
    public static final String TABLE_SCHEMA_SNAPSHOTS_BY_DOCUMENT = "CREATE TABLE IF NOT EXISTS snapshots_by_document (md5 text,snapshot text, PRIMARY KEY(md5, snapshot));";
    public static final String TABLE_SCHEMA_DOCUMENTS_DATA = "CREATE TABLE IF NOT EXISTS documents_data (data_id text, data blob, PRIMARY KEY (data_id))";

    // table columns
    public static final String COLUMN_SNAPSHOT = "snapshot";
    public static final String COLUMN_MODIFIED = "modified";
    public static final String COLUMN_UNIQUEID = "uniqueid";
    public static final String COLUMN_DATA = "data";
    public static final String COLUMN_DATA_ID = "data_id";
    public static final String COLUMN_SORT_ID = "sort_id";
    public static final String COLUMN_MD5 = "md5";

    // cqlsh upset statements
    public static final String STATEMENT_UPSET_SNAPSHOTS = "insert into snapshots (snapshot, data) values (?, ?)";
    public static final String STATEMENT_UPSET_SNAPSHOTS_BY_UNIQUEID = "insert into snapshots_by_uniqueid (uniqueid, snapshot) values (?, ?)";
    public static final String STATEMENT_UPSET_SNAPSHOTS_BY_MODIFIED = "insert into snapshots_by_modified (modified, snapshot) values (?, ?)";
    public static final String STATEMENT_UPSET_DOCUMENTS = "insert into documents (md5, sort_id, data_id) values (?, ?, ?)";
    public static final String STATEMENT_UPSET_DOCUMENTS_DATA = "insert into documents_data (data_id, data) values (?, ?)";

    // Prepared Statement constants
    public static final String PS_SELECT_SNAPSHOT = "SELECT * FROM snapshots WHERE snapshot=?";
    public static final String PS_SELECT_SNAPSHOT_ID = "SELECT snapshot FROM snapshots WHERE snapshot=?";
    public static final String PS_SELECT_MD5 = "SELECT md5 FROM documents WHERE md5=?";
    public static final String PS_SELECT_DOCUMENTS = "SELECT * FROM documents WHERE md5=?";
    public static final String PS_SELECT_DOCUMENTS_DATA = "SELECT * FROM documents_data WHERE data_id=?";
    public static final String PS_SELECT_SNAPSHOTS_BY_DOCUMENT = "SELECT * FROM snapshots_by_document WHERE md5=?";
    public static final String PS_SELECT_SNAPSHOTS_BY_UNIQUEID = "SELECT * FROM snapshots_by_uniqueid WHERE uniqueid=?";
    public static final String PS_SELECT_SNAPSHOTS_BY_MODIFIED = "SELECT * FROM snapshots_by_modified WHERE modified=?";
    public static final String PS_DELETE_SNAPSHOTS = "DELETE FROM snapshots WHERE snapshot=?";
    public static final String PS_DELETE_SNAPSHOTS_BY_UNIQUEID = "DELETE FROM snapshots_by_uniqueid WHERE uniqueid=? AND snapshot=?";
    public static final String PS_DELETE_SNAPSHOTS_BY_MODIFIED = "DELETE FROM snapshots_by_modified WHERE modified=? AND snapshot=?";
    public static final String PS_DELETE_SNAPSHOTS_BY_DOCUMENT = "DELETE FROM snapshots_by_document WHERE md5=? AND snapshot=?";
    public static final String PS_DELETE_DOCUMENTS_DATA = "DELETE FROM documents_data WHERE data_id=?";
    public static final String PS_DELETE_DOCUMENTS = "DELETE FROM documents WHERE md5=? AND sort_id=?";
    public static final String PS_UPSERT_SNAPSHOTS_BY_DOCUMENT = "INSERT INTO snapshots_by_document (md5, snapshot) VALUES (?, ?)";

    // Cached PreparedStatement fields
    private PreparedStatement psSelectSnapshot;
    private PreparedStatement psSelectSnapshotID;
    private PreparedStatement psSelectMD5;
    private PreparedStatement psSelectDocuments;
    private PreparedStatement psSelectDocumentsData;
    private PreparedStatement psSelectSnapshotsByDocument;
    private PreparedStatement psSelectSnapshotsByUniqueID;
    private PreparedStatement psSelectSnapshotsByModified;
    private PreparedStatement psDeleteSnapshots;
    private PreparedStatement psDeleteSnapshotsByUniqueID;
    private PreparedStatement psDeleteSnapshotsByModified;
    private PreparedStatement psDeleteSnapshotsByDocument;
    private PreparedStatement psDeleteDocumentsData;
    private PreparedStatement psDeleteDocuments;
    private PreparedStatement psUpsertSnapshotsByDocument;

    public static final String EVENTLOG_TOPIC_PERSIST = "cluster.persist";
    public static final String EVENTLOG_TOPIC_REMOVE = "cluster.remove";

    private static Logger logger = Logger.getLogger(ClusterService.class.getName());

    @Inject
    @ConfigProperty(name = ENV_WORKFLOW_CLUSTER_MODE, defaultValue = "false")
    boolean clusterMode;

    @Inject
    @ConfigProperty(name = ENV_WORKFLOW_CLUSTER_REPLICATION_FACTOR, defaultValue = "1")
    String repFactor;

    @Inject
    @ConfigProperty(name = ENV_WORKFLOW_CLUSTER_REPLICATION_CLASS, defaultValue = "SimpleStrategy")
    String repClass;

    @Inject
    @ConfigProperty(name = ENV_WORKFLOW_CLUSTER_CONTACTPOINTS)
    Optional<String> contactPoint;

    @Inject
    @ConfigProperty(name = ENV_WORKFLOW_CLUSTER_KEYSPACE)
    Optional<String> keySpace;

    @Inject
    @ConfigProperty(name = ENV_WORKFLOW_CLUSTER_AUTH_USER)
    Optional<String> userid;

    @Inject
    @ConfigProperty(name = ENV_WORKFLOW_CLUSTER_AUTH_PASSWORD)
    Optional<String> password;

    @Inject
    @ConfigProperty(name = ENV_WORKFLOW_CLUSTER_SSL, defaultValue = "false")
    boolean bUseSSL;

    @Inject
    @ConfigProperty(name = ENV_WORKFLOW_CLUSTER_SSL_TRUSTSTOREPATH)
    Optional<String> truststorePath;

    @Inject
    @ConfigProperty(name = ENV_WORKFLOW_CLUSTER_SSL_TRUSTSTOREPASSWORD)
    Optional<String> truststorePwd;

    @Inject
    @ConfigProperty(name = ENV_WORKFLOW_CLUSTER_SSL_KEYSTOREPATH)
    Optional<String> keystorePath;

    @Inject
    @ConfigProperty(name = ENV_WORKFLOW_CLUSTER_SSL_KEYSTOREPASSWORD)
    Optional<String> keystorePwd;

    private CqlSession session;

    @Resource
    private TimerService initTimerService;

    @Resource
    ManagedScheduledExecutorService syncScheduler;
    // deadlock timeout interval in ms
    long deadLockInterval = 60000;

    @Inject
    EventLogService eventLogService;

    @Inject
    ClusterOperator clusterOperator;

    @PostConstruct
    public void init() {
        if (isEnabled()) {
            scheduleClusterCheck();
        }
    }

    /**
     * This method indicates wether Imixs-Workflow runs in
     * the storage mode "CLUSTER"
     * This mode is indicated by the environment variables
     * 
     * WORKFLOW_CLUSTER_MODE=cluster
     * 
     * and the "WORKFLOW_CLUSTER_KEYSPACE" is set to valid keyspace.
     * 
     * In this mode all data is stored in a apache cassandra cluster.Otherwise the
     * engine runs in the so called 'LEGACY'.
     * 
     * @return
     */
    public boolean isEnabled() {
        return clusterMode;
    }

    /**
     * Helper method to establish a new cluster session. If not possible the method
     * schedules a retry every 10 seconds.
     */
    private void scheduleClusterCheck() {
        try {
            logger.log(Level.INFO, "├── connecting Cassandra cluster...");
            getSession();
            logger.log(Level.INFO, "├── ✅ successfully connected to cassandra!");

            // Registering a non-persistent Timer Service. 500ms initialDelay, 10ms period.
            this.syncScheduler.scheduleAtFixedRate(this::sync, 500, 10,
                    TimeUnit.MILLISECONDS);

            logger.log(Level.INFO, "├── ✅ Started sync service");

        } catch (ClusterException e) {
            logger.log(Level.WARNING, "├── " + e.getMessage());
            logger.log(Level.WARNING, "├── ⚠️ cluster not ready yet. retrying in 10 seconds...");
            // retry in 10 sec
            initTimerService.createSingleActionTimer(10000, new TimerConfig());
        }
    }

    /**
     * The sync method is called to update the cluster status after a document was
     * persisted by the DocumentService
     */
    public void sync() {
        // logger.info("├── sync cluster status..");
        eventLogService.releaseDeadLocks(deadLockInterval,
                EVENTLOG_TOPIC_PERSIST,
                EVENTLOG_TOPIC_REMOVE);
        clusterOperator.processEventLog();

    }

    /**
     * called by timer. Tries to connect cassandra clauster
     */
    @Timeout
    public void retryClusterCheck(Timer timer) {
        scheduleClusterCheck(); // retry Check
    }

    @PreDestroy
    private void tearDown() {
        // close session and cluster object
        if (session != null) {
            session.close();
        }
    }

    /**
     * Returns the Cassandra Session object. If the session is not yet created, the
     * method tries to init the session the first time. If initialization failed the
     * method throws a ClusterException
     * <p>
     * Note: the initialization is only successful if the cassandra cluster is fully
     * started.
     * <p>
     * The method also verifies the keyspace and creates a table schema if not yet
     * available.
     * 
     * @return
     */
    public CqlSession getSession() throws ClusterException {
        if (session == null) {
            try {
                logger.info("├── initializing cluster and keyspace...");
                session = initSessionWithKeyspace();
                logger.info("│   ├── ✅ cluster status = OK");
                createSchema();
                prepareStatements();
            } catch (Exception e) {
                try {
                    // Reset session so the next retry will try again completely
                    if (session != null) {
                        session.close();
                    }
                } finally {
                    // Ensure session is always reset to null on any failure
                    session = null;
                }
                throw new ClusterException(ClusterException.CLUSTER_ERROR,
                        "Failed to init cassandra session: " + e.getMessage(), e);
            }
        }
        return session;
    }

    /**
     * This method opens the configured keyspace. If the keyspace does no yet exist
     * it creates a new keyspace with the table schema
     * 
     * @return
     * @throws ClusterException
     */
    private CqlSession initSessionWithKeyspace() throws ClusterException {
        // validate keyspace name
        String keySpacename = keySpace.orElseThrow(
                () -> new ClusterException(ClusterException.INVALID_KEYSPACE, "WORKFLOW_CLUSTER_KEYSPACE not set!"));

        if (!isValidKeyspaceName(keySpacename)) {
            throw new ClusterException(ClusterException.INVALID_KEYSPACE,
                    "keyspace '" + keySpacename + "' name invalid.");
        }

        try {
            // try to connect keyspace ...
            session = createSession(keySpacename);
            logger.info("│   ├── ✅ keyspace '" + keySpacename + "' status = OK");
            return session;
        } catch (InvalidQueryException | InvalidKeyspaceException e) {
            logger.warning("│   ├── ⚠️ Keyspace does not yet exist, creating new keyspace...");
            createKeySpace(keySpacename); // Fully self-contained, uses own temp session
            session = createSession(keySpacename);
            return session;
        }
    }

    /**
     * This method creates a Cassandra Cluster object. The cluster is defined by
     * ContactPoints provided in the environment variable
     * 'WORKFLOW_CLUSTER_CONTACTPOINTS' or in the imixs.property
     * 'archive.cluster.contactpoints'
     * 
     * @return Cassandra Cluster instacne
     */
    private CqlSession createSession(String keyspace) throws ClusterException {
        CqlSessionBuilder builder = CqlSession.builder();
        // Boolean used to check if at least one host could be resolved
        boolean found = false;

        if (!contactPoint.isPresent() || contactPoint.get().isEmpty()) {
            throw new ClusterException(ClusterException.MISSING_CONTACTPOINT,
                    "missing cluster contact points - verify configuration!");
        }

        logger.info("│   ├── cluster connecting: " + contactPoint.get());
        String[] hosts = contactPoint.get().split(",");
        for (String host : hosts) {
            try {
                logger.info("│   ├── adding host: " + host + ":9042");
                builder.addContactPoint(new InetSocketAddress(host, 9042));
                // One host could be resolved
                found = true;
            } catch (IllegalArgumentException e) {
                // This host could not be resolved so we log a message and keep going
                logger.warning("...the host '" + host + "' is unknown so it will be ignored");
            }
        }
        if (!found) {
            // No host could be resolved so we throw an exception
            throw new IllegalStateException("All provided hosts are unknown - check cluster status and configuration!");
        }
        // set optional credentials...
        if (userid.isPresent() && !userid.get().isEmpty()) {
            builder = builder.withAuthCredentials(userid.get(), password.get());
        }

        if (keyspace != null) {
            builder.withKeyspace(keyspace);
        }
        builder.withLocalDatacenter("datacenter1");
        return builder.build();
    }

    /**
     * Test if the keyspace name is valid.
     * 
     * @param keySpace
     * @return
     */
    public boolean isValidKeyspaceName(String keySpace) {
        if (keySpace == null || keySpace.isEmpty()) {
            return false;
        }
        return keySpace.matches(KEYSPACE_REGEX);
    }

    /**
     * Returns the prepared statement for loading a full snapshot by ID.
     * 
     * @throws ClusterException if the session is not yet available
     */
    public PreparedStatement getPsSelectSnapshot() throws ClusterException {
        getSession(); // ensures session and statements are initialized
        return psSelectSnapshot;
    }

    public PreparedStatement getPsSelectSnapshotID() throws ClusterException {
        getSession();
        return psSelectSnapshotID;
    }

    public PreparedStatement getPsSelectMD5() throws ClusterException {
        getSession();
        return psSelectMD5;
    }

    public PreparedStatement getPsSelectDocuments() throws ClusterException {
        getSession();
        return psSelectDocuments;
    }

    public PreparedStatement getPsSelectDocumentsData() throws ClusterException {
        getSession();
        return psSelectDocumentsData;
    }

    public PreparedStatement getPsSelectSnapshotsByDocument() throws ClusterException {
        getSession();
        return psSelectSnapshotsByDocument;
    }

    public PreparedStatement getPsSelectSnapshotsByUniqueID() throws ClusterException {
        getSession();
        return psSelectSnapshotsByUniqueID;
    }

    public PreparedStatement getPsSelectSnapshotsByModified() throws ClusterException {
        getSession();
        return psSelectSnapshotsByModified;
    }

    public PreparedStatement getPsDeleteSnapshots() throws ClusterException {
        getSession();
        return psDeleteSnapshots;
    }

    public PreparedStatement getPsDeleteSnapshotsByUniqueID() throws ClusterException {
        getSession();
        return psDeleteSnapshotsByUniqueID;
    }

    public PreparedStatement getPsDeleteSnapshotsByModified() throws ClusterException {
        getSession();
        return psDeleteSnapshotsByModified;
    }

    public PreparedStatement getPsDeleteSnapshotsByDocument() throws ClusterException {
        getSession();
        return psDeleteSnapshotsByDocument;
    }

    public PreparedStatement getPsDeleteDocumentsData() throws ClusterException {
        getSession();
        return psDeleteDocumentsData;
    }

    public PreparedStatement getPsDeleteDocuments() throws ClusterException {
        getSession();
        return psDeleteDocuments;
    }

    public PreparedStatement getPsUpsertSnapshotsByDocument() throws ClusterException {
        getSession();
        return psUpsertSnapshotsByDocument;
    }

    /**
     * Creates a new Cassandra keyspace using a temporary session without a keyspace
     * context.
     * <p>
     * A temporary session is required here because creating a keyspace cannot be
     * performed on a session that is already bound to a specific keyspace. The
     * temporary session is created without a keyspace, used solely to execute the
     * CREATE KEYSPACE statement, and then immediately closed via try-with-resources
     * to ensure proper resource cleanup even in case of an error.
     * <p>
     * This approach keeps the method fully self-contained and avoids exposing the
     * temporary session to the caller, preventing any risk of the main session
     * field being set to an unbound session on failure.
     *
     * @param keySpace the name of the keyspace to create
     * @throws ClusterException if the keyspace creation fails
     */
    private void createKeySpace(String keySpace) throws ClusterException {
        logger.info("│   ├── creating new keyspace '" + keySpace + "'...");
        String statement = "CREATE KEYSPACE IF NOT EXISTS " + keySpace + " WITH replication = {'class': '" + repClass
                + "', 'replication_factor': " + repFactor + "};";

        // Use a temporary session without keyspace to create the keyspace
        try (CqlSession tempSession = createSession(null)) {
            tempSession.execute(statement);
        } catch (Exception e) {
            throw new ClusterException(ClusterException.CLUSTER_ERROR,
                    "Failed to create keyspace '" + keySpace + "': " + e.getMessage(), e);
        }
        logger.info("│   ├── ✅ keyspace '" + keySpace + "' created.");
    }

    /**
     * This method creates the keySpace schema.
     * 
     * CREATE TABLE embeddings.document_vectors ( business_document_id text,
     * chunk_id text, chunk_text text, content_vector VECTOR <FLOAT, 768>, PRIMARY
     * KEY (business_document_id, chunk_id) ); );
     * 
     * @param cluster
     */
    private void createSchema() throws ClusterException {

        logger.info("│   ├── 🔃 verify schema...");

        logger.fine(TABLE_SCHEMA_SNAPSHOTS);
        getSession().execute(TABLE_SCHEMA_SNAPSHOTS);

        logger.fine(TABLE_SCHEMA_SNAPSHOTS_BY_UNIQUEID);
        getSession().execute(TABLE_SCHEMA_SNAPSHOTS_BY_UNIQUEID);

        logger.fine(TABLE_SCHEMA_SNAPSHOTS_BY_MODIFIED);
        getSession().execute(TABLE_SCHEMA_SNAPSHOTS_BY_MODIFIED);

        logger.fine(TABLE_SCHEMA_DOCUMENTS);
        session.execute(TABLE_SCHEMA_DOCUMENTS);

        logger.fine(TABLE_SCHEMA_SNAPSHOTS_BY_DOCUMENT);
        getSession().execute(TABLE_SCHEMA_SNAPSHOTS_BY_DOCUMENT);

        logger.fine(TABLE_SCHEMA_DOCUMENTS_DATA);
        getSession().execute(TABLE_SCHEMA_DOCUMENTS_DATA);

        logger.info("│   ├── ✅ database status = OK");
    }

    /**
     * Prepares all CQL statements after the session has been established.
     * Called internally after a successful session init.
     * 
     * @throws ClusterException if preparation fails
     */
    private void prepareStatements() throws ClusterException {
        logger.info("│   ├── 🔃 preparing CQL statements...");

        // SELECT
        psSelectSnapshot = session.prepare(PS_SELECT_SNAPSHOT);
        psSelectSnapshotID = session.prepare(PS_SELECT_SNAPSHOT_ID);
        psSelectMD5 = session.prepare(PS_SELECT_MD5);
        psSelectDocuments = session.prepare(PS_SELECT_DOCUMENTS);
        psSelectDocumentsData = session.prepare(PS_SELECT_DOCUMENTS_DATA);
        psSelectSnapshotsByDocument = session.prepare(PS_SELECT_SNAPSHOTS_BY_DOCUMENT);
        psSelectSnapshotsByUniqueID = session.prepare(PS_SELECT_SNAPSHOTS_BY_UNIQUEID);
        psSelectSnapshotsByModified = session.prepare(PS_SELECT_SNAPSHOTS_BY_MODIFIED);
        psUpsertSnapshotsByDocument = session.prepare(PS_UPSERT_SNAPSHOTS_BY_DOCUMENT);

        // DELETE
        psDeleteSnapshots = session.prepare(PS_DELETE_SNAPSHOTS);
        psDeleteSnapshotsByUniqueID = session.prepare(PS_DELETE_SNAPSHOTS_BY_UNIQUEID);
        psDeleteSnapshotsByModified = session.prepare(PS_DELETE_SNAPSHOTS_BY_MODIFIED);
        psDeleteSnapshotsByDocument = session.prepare(PS_DELETE_SNAPSHOTS_BY_DOCUMENT);
        psDeleteDocumentsData = session.prepare(PS_DELETE_DOCUMENTS_DATA);
        psDeleteDocuments = session.prepare(PS_DELETE_DOCUMENTS);

        logger.info("│   ├── ✅ CQL status = OK");
    }
}
