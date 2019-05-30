# Migration Notes 5.0.0

This document contains migration notes about the migration from Imixs-Workflow version 4.x to version 5.x.

With version 5.0.0 the Eclipse Microprofile 2.0 is supported. 


## Migrate imixs.properties

With version 5.0 the MP config API is introduced. The WorkflowConfigSource provides the imixs.properties file so there is **no** need to rename or move the imixs.properties file to /META-INF/microprofile-config.properties

Of course it is possible to set property values also into the file /META-INF/microprofile-config.properties

Imixs property values can be set in:

* System.getProperties()
* System.getenv()
* All META-INF/microprofile-config.properties on the classpath
* The imixs.properties on the classpath 		