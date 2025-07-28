# Database Schema

The Imixs-Workflow engine persists all information about the model and the running workflow instances (_workitems_) using the Java Persistence API (JPA). Therefore the Imixs-Workflow engine is database vendor independent and can be run on any SQL database (e.g. MySQL, PostgreSQL, Oracle, MS SQL, ...). See the [Deployment Guide](./deployment_guide.html) for further details how to deploy the Imixs-Workflow engine into a application sever.

## The JPA Classes and Tables

The database schema used by the Imixs-Workflow engine is quite simple and consists of only two tablee named `DOCUMENT` and `EVENTLOG`. During the deployment, JPA maps the jpa entity classes automatically to the database and creates the corresponding data table.

If you like to to create the table manually you can do it like this:

```bash
CREATE TABLE document (
    id character varying(255) NOT NULL,
    created timestamp without time zone,
    data bytea,
    modified timestamp without time zone,
    type character varying(255),
    version integer
);

ALTER TABLE document ADD CONSTRAINT document_pkey PRIMARY KEY (id);
```

```bash
CREATE TABLE eventlog (
    id character varying(255) NOT NULL,
    created timestamp without time zone,
    data bytea,
    ref character varying(255),
    timeout timestamp without time zone,
    topic character varying(255),
    version integer
);

ALTER TABLE eventlog ADD CONSTRAINT eventlog_pkey PRIMARY KEY (id);
```

## Performance

In large databases with many documents it is recommended to provide additional indexes to the following columns:

- type
- created
- modified
- version

### MySQL

The following statement adds the necessary indexes for a MySQL Database:

    ALTER TABLE `DOCUMENT` ADD INDEX `index1`(`CREATED`,`MODIFIED`,`TYPE`,`VERSION`);

### PostgreSQL

The following statement adds the necessary indexes for a PostgreSQL Database:

    CREATE INDEX index_document1 ON document USING btree(created, modified, type , version);
    CREATE INDEX idx_eventlog_topic_created ON eventlog (topic, created);

#### Optimizing Vacuum Settings

In case you are handling a lot of documents (file attachments) in your workflow the largest part of you data will be caused by the TOAST data and indexes, not by the `document` table itself. For this reason it is recommended to optimize the vacuum settings for PostgreSQL.
Autovacuum Settings:

The default settings (autovacuum_vacuum_scale_factor = 0.2) is mostly too high for a table with large TOAST data. On the other side the
memory settings (maintenance_work_mem = 65536 kB (64 MB)) will be too low for the size of your database, especially for VACUUM operations.

1. First optimize the vacuum settings for the document table:

```sql
ALTER TABLE document SET (
    autovacuum_vacuum_scale_factor = 0.01,
    autovacuum_vacuum_threshold = 1000,
    autovacuum_vacuum_cost_delay = 0,
    autovacuum_vacuum_cost_limit = 2000
);
```

2. Change global settings

```sql
ALTER SYSTEM SET maintenance_work_mem = '1GB';
ALTER SYSTEM SET autovacuum_max_workers = 6;
ALTER SYSTEM SET autovacuum_naptime = '30s';
SELECT pg_reload_conf();
```

3. Verify teh result

From time to time you can check the result of the settings:

```sql
# SELECT
    relname,
    n_dead_tup,
    n_live_tup,
    last_autovacuum,
    autovacuum_count,
    pg_size_pretty(pg_total_relation_size(relid)) as total_size
FROM pg_stat_user_tables
WHERE relname = 'document';
```

The result may look like this:

```
 relname  | n_dead_tup | n_live_tup |        last_autovacuum        | autovacuum_count | total_size
----------+------------+------------+-------------------------------+------------------+------------
 document |          2 |     629039 | 2025-05-25 07:37:27.292062+00 |                1 | 224 GB
(1 row)
```
