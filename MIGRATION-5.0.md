# Migration Notes 



## Migration 5.1.13

In version 5.1.13 the EventLog JPA entity was extended with a new column 'timeout'. For that the table schema need to be updated.

**Solution 1)**

In persistence.xml set the eclipse property 'eclipselink.ddl-generation' to 'create-or-extend-tables'

		....
		<properties>
		   .....
			<!-- optional extend schema... --> 
			<property name="eclipselink.ddl-generation" value="create-or-extend-tables" />
			.....
		</properties>
		....


**Solution 2)**

As an alternative you can also drop the table 

	$ docker exec -it f637718e09be bash
	...
	root@f637718e09be:/# psql workflow-db -Upostgres
	workflow-db=# drop table eventlog;
	DROP TABLE
	
After a redeployment the table will be recreated.



## Migration 5.0.0

This document contains migration notes about the migration from Imixs-Workflow version 4.x to version 5.x.

With version 5.0.0 the Eclipse Microprofile 2.0 is supported. 


### Migrate imixs.properties

With version 5.0 the MP config API is introduced. The ImixsConfigSource provides the imixs.properties file so there is **no** need to rename or move the imixs.properties file to /META-INF/microprofile-config.properties

Of course it is possible to set property values also into the file /META-INF/microprofile-config.properties

Imixs property values can be set in:

* System.getProperties()
* System.getenv()
* All META-INF/microprofile-config.properties on the classpath
* The imixs.properties on the classpath 		