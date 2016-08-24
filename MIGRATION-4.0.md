# Migration Notes 4.0.0

This document contains release notes about the migration from Imixs-Workflow version 3.x.x to version 4.x.x.

## Packages

The following java packages are deprecated in version 4.0.0 but still available to support the migration:

 * org.imxis.workflow.jee.ejb
 * org.imxis.workflow.jee.jpa
 * org.imxis.workflow.jee.util
 * org.imxis.workflow.plugins.jee
 
The following new packages are introduced with version 4.0.0 :

 * org.imixs.workflow.jpa - contains the persistence JPA classes
 * org.imixs.workflow.ejb - contains the services EJB classes
 * org.imixs.workflow.plugins - contains all plug-in classes
 
## Persistence

With version 4.0.0 we introduced a new persistence layer. There is now only one single JPA entity bean class 'org.imixs.workflow.jpa.Document'. The Document class replaces the deprecated Entity Class with all additional index classes.

The package 'org.imxis.workflow.jee.jpa' is still available in version 4.0.x to support the migration path. We will drop this package with version 4.1.x finally. 
 
 
## EJBs

All Imxis Service EJBs are moved into the package 'org.imixs.workflow.ejb'. The EntityService EJB was replaced with the new DocumentService EJB.  