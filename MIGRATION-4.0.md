# Migration Notes 4.0.0

This document contains release notes about the migration from Imixs-Workflow version 3.x.x to version 4.x.x.

## Packages

The following java packages are deprecated in version 4.0.0 but still available to support the migration:

 * org.imxis.workflow.jee.ejb
 * org.imxis.workflow.jee.jpa
 * org.imxis.workflow.jee.util
 * org.imxis.workflow.plugins.jee
 
The following new packages are introduced with version 4.0.0 :

 * org.imixs.workflow.engine - contains the services EJB classes
 * org.imixs.workflow.engine.jpa - contains the persistence JPA classes
 * org.imixs.workflow.engine.plugins - contains all plug-in classes
 
## Persistence

With version 4.0.0 we introduced a new persistence layer. There is now only one single JPA entity bean class 'org.imixs.workflow.jpa.Document'. The Document class replaces the deprecated Entity Class with all additional index classes.

The package 'org.imxis.workflow.jee.jpa' is still available in version 4.0.x to support the migration path. We will drop this package with version 4.1.x finally. 
 
 
## EJBs

All Imxis Service EJBs are moved into the package 'org.imixs.workflow.ejb'. The EntityService EJB was replaced with the new DocumentService EJB.  

## Migrate JPQL 

JPQL Statements can be replaced with Lucene Search terms.

		String sQuery = "SELECT config FROM Entity AS config " + " JOIN config.textItems AS t2"
				+ " WHERE config.type = '" + TYPE_CONFIGURATION + "'" + " AND t2.itemName = 'txtname'" + " AND t2.itemValue = '"
				+ NAME + "'" + " ORDER BY t2.itemValue asc";
		Collection<ItemCollection> col = entityService.findAllEntities(sQuery, 0, 1);

...to be replaced with....
		
		String searchTerm="(type:\"" + TYPE_CONFIGURATION  +"\" AND txtname:\"" + NAME + "\")";
		Collection<ItemCollection> col = documentService.find(sQuery, 0, 1);

## Rest API

The old rest api to post workitems in JSON/XML format is supported by a backport using the resource /v3/

     /v3/workflow/
     /v3/entity/
     /v3/model/

**NOTE:** This api is deprecated and will be droped with future releases!
		
# Migration Guide

 1. Migrate Workflow Models - (new package org.imixs.workflow.engine)

 2. Undeploy Application (with Imixs-Workflow 3.x)

 2. Backup Database (optional)
 
 3. Shutdown server, remove Fulltextindex from Filesystem, and remove old artifacts
 
 4. Extend FieldListNoAnalyse (txtemail, datdate, datfrom, datto, numsequencenumber, txtUsername)

 5. Restart Server and deploy new Application (with Imixs-Workflow 4.x) 
  
 6. Start Migration Job from Imixs-Admin Interface
 
 7. Upload new Models
 
 
## Migrate Models

In Imixs-Workflow 4.0.0 the plug-in package has changed from 'org.imixs.workflow.plugins....'  to 'org.imixs.workflow.engine.plugins...'.
The plugin 'org.imixs.workflow.lucene.LucenePlugin' can be removed form the model. 

You need to change the plugin classes in your model files and upload the new models through the rest API

	curl --user admin:adminadmin --request POST -Tmymodel.bpmn http://localhost:8080/workflow/rest-service/model/bpmn
 		