# Database Schema

The Imixs-Workflow engine persists all information about the model and the running workflow instances (_workitems_) using the Java Persistence API (JPA). Therefore the Imixs-Workflow engine is database vendor independent and can be run on any SQL database (e.g. MySQL, PostgreSQL, Oracle, MS SQL, ...). See the [Deployment Guide](./deployment_guide.html) for further details how to deploy the Imixs-Workflow engine into a application sever.
  
## The JPA Classes and Tables
The database schema used by the Imixs-Workflow engine is quite simple and constis of only one table named '_DOCUMENT_'. During the deployment, JPA maps the jpa class _org.imixs.workflow.engine.jpa.Document_ automatically to the database and creates the corresponding data table.


	CREATE TABLE `DOCUMENT` (
	  `ID` varchar(255) NOT NULL,
	  `CREATED` datetime DEFAULT NULL,
	  `DATA` longblob,
	  `MODIFIED` datetime DEFAULT NULL,
	  `TYPE` varchar(255) DEFAULT NULL,
	  `VERSION` int(11) DEFAULT NULL,
	  PRIMARY KEY (`ID`)
	) ENGINE=InnoDB DEFAULT CHARSET=latin1;



## Performance
In large databases with many documents it is recommended to provide additional indexes to the following columns:

 * type
 * created
 * modified
 * version
 
 
### MySQL 

The following statement adds the necessary indexes for a MySQL Database: 
 
	ALTER TABLE `DOCUMENT` ADD INDEX `index1`(`CREATED`,`MODIFIED`,`TYPE`,`VERSION`);

### PostgreSQL

The following statement adds the necessary indexes for a PostgreSQL Database: 

	CREATE INDEX index_document1 ON document USING btree(created, modified, type , version);
         
