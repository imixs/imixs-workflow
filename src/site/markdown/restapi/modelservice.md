#The Model Service

The resource _/model_ provides methods to read and update workflow models through the Imixs-Rest API.
 
  

## GET a Model
The GET method is used to read model objects provided by the Model Manager:


| URI                                           | Method| Description                                                          | 
|-----------------------------------------------|-------|----------------------------------------------------------------------|
| /model                                        | GET  | a list of model versions provided by the  workflow instance           |
| /model/{version}/tasks                        | GET  | all tasks of a specific model  version                                |
| /model/{version}/tasks/{taskid}               | GET  | a task identified by its taskID                                       |
| /model/{version}/tasks/{taskid}/events        | GET  | all events assigned to a specific task identified by its eventID.     |
| /model/{version}/groups                       | GET  | a collection of all workflow groups                                   |
| /model/{version}/groups/{group}               | GET  | all tasks of a specific workflow group                                |
| /model/{version}/definition                   | GET  | the model definition containing general model information (e.g.$ModelVersion, Actors, Plugins, ...).      |
| /model/{version}/bpmn                         | GET  | BPMN source file                                                      |


## PUT/POST a Model
The methods PUT and POST are used to write and update a model:

| URI                        | Method| Description                                                      | 
|----------------------------|-----|-------------------------------------------------------------|
| /model/bpmn                | GET | creates or update based on a BPMN 2.0 model definition           |
| /model/                    | GET | creates or update a model based on a Imixs EntityCollection      |


## DELETE a Model
The method DELETE is used to delete model objects:

| URI                                           | Method | Description                               | 
|-----------------------------------------------|--------|------------------------------------|
| /model/{version}                              | DELETE | deletes a specified model version         |



   