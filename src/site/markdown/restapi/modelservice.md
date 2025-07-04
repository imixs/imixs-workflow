# The Model Service

The resource _/model_ provides methods to read and update workflow models through the Imixs-Rest API.

## GET a Model

The GET method is used to read model objects provided by the Model Manager:

| URI                                                                | Method | Description                                                                                                                                          |
| ------------------------------------------------------------------ | ------ | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| /model                                                             | GET    | a list of model versions provided by the workflow instance                                                                                           |
| /model/{version}/tasks                                             | GET    | all tasks of a specific model version                                                                                                                |
| /model/{version}/tasks/{taskid}                                    | GET    | a task identified by its taskID                                                                                                                      |
| /model/{version}/tasks/{taskid}/events                             | GET    | all events assigned to a specific task.                                                                                                              |
| /model/{version}/tasks/{taskid}/events/{eventid}                   | GET    | a event assigned to a specific task identified by its eventID.                                                                                       |
| /model/{version}/groups                                            | GET    | a sorted list of all unique process groups. In case the model is a collaboration diagram only group names from private process are returned (Pools)! |
| /model/{version}/groups/{group}                                    | GET    | all tasks of a specific workflow group                                                                                                               |
| /model/{version}/groups/{group}/start                              | GET    | all start tasks of a specific workflow group                                                                                                         |
| /model/{version}/groups/{group}/end                                | GET    | all end tasks of a specific workflow group                                                                                                           |
| /model/{version}/definition                                        | GET    | the model definition containing general model information (e.g.$ModelVersion, Actors, Plugins, ...).                                                 |
| /model/{version}/bpmn                                              | GET    | BPMN source file                                                                                                                                     |
| /model/{version}/tasks/{taskid}/dataobjects/{name}                 | GET    | the content of a dataObject by name assigned to a BPMN task                                                                                          |
| /model/{version}/tasks/{taskid}/events/{eventid}/dataobject/{name} | GET    | the content of a dataObject by name assigned to a BPMN event                                                                                         |
| /model/{version}/tasks/{taskid}/formdefinition                     | GET    | the form definition assigned to a BPMN task                                                                                                          |

**DataObjects**

BPMN DataObjects assigned to a Task or an Event element are stored in the item `dataobjects` and can be requested by the corresponding element. See the following example to request all dataObjects assigned to task 1000 of the model 1.0

http://localhost:8080/api/model/1.0/tasks/1000?format=xml&items=dataobjects

## PUT/POST a Model

The methods PUT and POST are used to write and update a model:

| URI                    | Method | Description                                                                                                               |
| ---------------------- | ------ | ------------------------------------------------------------------------------------------------------------------------- |
| /model/bpmn/{filename} | GET    | creates or update a model based on a BPMN 2.0 model file. The filename defines the internal filename to store the model   |
| /model/bpmn            | GET    | creates or update a model based on a BPMN 2.0 model file. The internal filename is generated from the modelversion +.bpmn |

Using the `curl` command a model file can be uploaded with:

```bash
$ curl --user admin:adminadmin --request POST -Tticket-en-1.0.0.bpmn http://localhost:8080/api/model/bpmn/
```

## DELETE a Model

The method DELETE is used to delete model objects:

| URI              | Method | Description                       |
| ---------------- | ------ | --------------------------------- |
| /model/{version} | DELETE | deletes a specified model version |
