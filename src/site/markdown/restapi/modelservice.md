#The Model Service
The main resource /model is used to read and update workflow models through the Rest Service Interface.
 

## The /model resources GET
The /model GET resources are used to get model objects provided by the Model Manager:


| URI                                           | Description                                                           | 
|-----------------------------------------------|-----------------------------------------------------------------------|
| /model                                        | a list of model versions provided by the  workflow instance           |
| /model/{version}/tasks                        | all tasks of a specific model  version                                |
| /model/{version}/tasks/{taskid}               | a task identified by its taskID                                       |
| /model/{version}/tasks/{taskid}/events        | all events assigned to a specific task identified by its eventID.     |
| /model/{version}/groups                       | a collection of all workflow groups                                   |
| /model/{version}/groups/{group}               | all tasks of a specific workflow group                                |
| /model/{version}/bpmn                         | BPMN source file                                                      |



##The /model resources DELETE
The /model DELETE resources URIs are used to delete model objects:

| URI                                           | Description                               | 
|-----------------------------------------------|-------------------------------------------|
| /model/{version}                              | deletes a specified model version         |



##The /model resources PUT and POST
The /model PUT and POST resources URIs are used to write a model:

| URI                        | Description                                                      | 
|----------------------------|------------------------------------------------------------------|
| /model/bpmn                | creates or update based on a BPMN 2.0 model definition           |
| /model/                    | creates or update a model based on a Imixs EntityCollection      |
   