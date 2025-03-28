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
