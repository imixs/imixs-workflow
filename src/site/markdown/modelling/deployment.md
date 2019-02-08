#How to deploy a BPMN Model
After you have created a model with the Imixs-BPMN modeler you can deploy the model. The Imixs-Workflow Rest API provides a convenient way to upload a BPMN model or to verify the deployment
 status of BPMN models.  Make sure that your Imixs-Workflow instance is up and running and the Imxis-Workflow Rest Service is deployed on your server. (See the [deployment section](../deployment/index.html) how to deploy the Imixs-Workflow engine into a server.) 
 
The Imixs-Workflow Model Rest Service is available under the following resource URL:
 
    http://localhost:8080/api/model/
 
where '/workflow/' is the context root of your workflow application and '/api/' is the 
 context of the Imixs-Workflow Rest Services. See the [section model rest service](../restapi/modelservice.html) for general information about the Imixs-Workflow Rest API.
 
 
## Deploy a Model
To deploy a BPMN model you can use the [command-line tool curl](https://en.wikipedia.org/wiki/CURL) to upload a Imixs BPMN 2.0 model into a Imixs-Workflow instance. 
See the following example:
 
	curl --user admin:adminpassword --request POST -Tticket.bpmn http://localhost:8080/api/model/bpmn
 
This example deploys the bpmn file 'ticket.bpmn' into the Imixs-Workflow Instance 'http://localhost:8080/workflow/'. The result of the deployment can be verified in the server log:

	09:20:47,611 INFO  [org.imixs.workflow.bpmn.BPMNParser] (default task-3) BPMN Model parsed in 12ms
  
You can also verify the current status of deployed models by calling the model Rest Service in your Web Browser:
 
	http://localhost:8080/api/model/

<img src="../images/modelling/bpmn_screen_28.png"/> 
 

## Delete a model 
To delete a specific model version the Imxis-Rest API provides a DELETE command

	curl --user admin:adminpassword --request DELETE http://localhost:8080/workflow/rest-service/model/[VERSION]
  
 
## How to Deploy a Model from a Java Application
You can also use the Imixs ModelService to deploy a BPMN model programtically. In this case you call the Model Service EJB form your application to deploy a model. The following example demonstrates  the API call:
 
	import org.imixs.workflow.bpmn.BPMNModel;
	import org.imixs.workflow.bpmn.BPMNParser;
	import org.imixs.workflow.jee.ejb.ModelService;
	....
	
	@EJB
	protected ModelService modelService;
	
	.....
	// open a inputStream of the model file
	InputStream inputStream = getClass().getResourceAsStream(
				"/bpmn/ticket.bpmn");
	// parse and import the model....
	BPMNModel model = BPMNParser.parseModel(file.getData(), "UTF-8");
	modelService.importBPMNModel(model);
	... 
 
You can find more information about the Imixs-Workflow services in the [section Imixs-Workflow Engine](../engine/index.html).
 