# The ModelService
The Model Service provides a service layer to manage workflow models. The model service persists BPMN models in the internal model repository. Each model is identified by a unique version assigned to a model.  This service component is used by the Imixs Workflow Engine internally. The service is based on the core interface [Model](../core/model.html).

To create and manage workflow models the Eclipse tool  [Imixs-BPMN Modeller](../modelling/index.html) can be used.
 
##Methods 
The ModelService implementations extends Interface '_org.imixs.workflow.Model_' and provides the following methods:


|Method              		 | Description 				 |
|----------------------------|---------------------------|
|getProcessEntity(processID,Version)| returns a process entity |
|getActivityEntity(processID,activityID,Version)| returns a activity entity assigned to a process entity| 
|getProcessEntityList(Version)| returns all process entities for a specific model version | 
|getActivityEntityList(processID,Version)| returns all activity entities assigned to a  process entity |
|saveProcessEntity(entity)	| persists an activity entity 				|
|saveActivityEntity(entity)	| persists an activity entity 				|
|saveEnvironmentEntity(entity)|persists an environament entity  containing model environment properties				|
|removeModel(version)		| removes a persisted process model version|
|removeModelGroup(group,version)		| removes a persisted process model group by inside a model|
|getLatestVersion()		| returns the latest model version available in the model repository|
|getLatestVersionByWorkitem()		| returns the latest model version available in the model repository matching the model assigned to a existing process instance|
|getAllModelProfiles()		| returns all profile environment entities |
|getAllModelVersions()		| returns a list of all model versions stored in the model repository|
|getPublicActivities(processID,Version)| returns all public activity entities assigned to a  process entity |
|getAllWorkflowGroups(Version)| returns all workflow groups assigned to a model version |
|getAllStartProcessEntities(Version)| returns all initial process entities for a model version |
|getAllProcessEntitiesByGroup(Group,Version)| returns all process entities for a workflow group in a specific model version |
|importBPMNModel(Stream)| imports a BPMN model file |
|importModel(Stream)| imports a XML model file |





 

  
  