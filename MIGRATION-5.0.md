# Migration Notes 5.0.0

This document contains migration notes about the migration from Imixs-Workflow version 4.x to version 5.x.

With version 5.0.0 the Eclipse Microprofile 2.0 is supported. 


## Migrate imixs.properties

With version 5.0 the MP config API is introduced. This need to rename and move the imixs.properties file to /META-INF/microprofile-config.properties

	$ mv imixs.properties /META-INF/microprofile-config.properties
 		