#The Model Service
The main ressource /model is used to read and update workflow models through the Rest Service Interface.
 

## The /model resources GET
The /model GET resources are used to get business objects provided by the Imixs Model Manager:


*-----------------------------------------------*-------------------------------------------* 
| URI                                           | Description                               | 
*-----------------------------------------------+-------------------------------------------+
| /model                                        | a list of model versions provided by the  |
|                                               | workflow instance                         |
*-----------------------------------------------+-------------------------------------------+
| /model/{version}                              | a collection of processEntities for a spcific model    |
|                                               | version (HTML, XML, JSON)                 |
*-----------------------------------------------+-------------------------------------------+
| /model/{version}/process/{processid}          | a specific processEntity                  |
|                                               |  (HTML, XML, JSON)                        |
*-----------------------------------------------+-------------------------------------------+
| /model/{version}/activities/{processid}       | a collection of workflow activities for   |
|                                               | a specific processID  (HTML, XML, JSON)   |
*-----------------------------------------------+-------------------------------------------+
| /model/{version}/groups                       | a collection of start process entity for  |
|                                               | a specific model (HTML, XML, JSON)        |
*-----------------------------------------------+-------------------------------------------+
| /model/{version}/groups/{group}               | a collection of process entity for        |
|                                               | a specific group (HTML, XML, JSON)        |
*-----------------------------------------------*-------------------------------------------+



##The /model resources DELETE
The /model DELETE resources URIs are used to delete business objects:


*-----------------------------------------------*-------------------------------------------*
| URI                                           | Description                               | 
*-----------------------------------------------+-------------------------------------------+
| /model/{version}                              | deletes a specified model version         |
*-----------------------------------------------+-------------------------------------------+



##The /model resources PUT and POST
The /model PUT and POST resources URIs are used to write a model:

*----------------------------*------------------------------------------------------* 
| URI                        | Description                                          | 
*----------------------------+------------------------------------------------------+
| /model/bpmn                | creates or update based on a BPMN 2.0 model definition |
*----------------------------+------------------------------------------------------+
| /model/                    | creates or update a model based on a Imixs EntityCollection  |
*----------------------------+------------------------------------------------------+
   