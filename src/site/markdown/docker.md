# Imixs Docker

The Imixs-Workflow project supports several Docker Images to run Imixs-Workflow in a containerized infrastructure. 
The Imixs Docker Images are hosted on [Docker Hub](https://hub.docker.com/r/imixs/) and can be installed and extended in various ways.


## The Imixs Microservice

The subproject [Imixs-Microservice](https://github.com/imixs/imixs-microservice) provides a WebService Interface which can be used to interact with the Imixs-Workflow-Engine over the [Imixs Rest API](./restapi/index.html). The 'Imixs-Microsoervice' is a Java EE Web Module which extends the Imixs-Workflow Engine providing a Rest Service for Human Centric Workflow Applications. This service can be used as a single Microservice or bundled with a Java EE Business Application. See also the [Deployment Guide](./deployment/index.html) for details.

## How to run a Imixs Docker Container

To use Imixs-Workflow out of the box, you can create a 'docker-compose' file:

	version: '3.3'
	services:
	  db:
	    image: postgres:9.6.1
	    environment:
	      POSTGRES_PASSWORD: adminadmin
	      POSTGRES_DB: workflow
	  app:
	    image: imixs/imixs-microservice
	    environment:
	      WILDFLY_PASS: adminadmin
	      POSTGRES_HOST: "db"
	      POSTGRES_USER: "postgres"
	      POSTGRES_PASSWORD: "adminadmin"
	      POSTGRES_DATABASE: "workflow"
	      POSTGRES_CONNECTION: "jdbc:postgresql://db/workflow"
	    ports:
	      - "8080:8080"

Run start imixs-wokflow with docker-compose run:

	docker-compose up
	    
**Note:** The container is linked to the postgres container providing a database name 'workflow'. See the [docker project home](https://hub.docker.com/r/imixs/imixs-microservice/) for more information. 

## Testing the Imixs-Microservice

Using the command-line tool '[curl](http://curl.haxx.se/)' makes it easy to test the Imixs-Microservice. Here are some examples.

**NOTE**: As Imixs-Workflow is a human-centric Workflow Engine onyl authenticated users (Actors) can interact with the engine. Therefore it is necessary to authenticate against the Imixs-Rest service API. Imixs-Microservice provides a User-Management-Service to register and authenticate users. The default user has the userid 'admin' and the default password 'adminadmin'. This user is used  in the following examples. Run the setup url of Imixs-Micorservice to initalize the admin user acount:

	http://localhost:8080/imixs-microservice/setup

### Deploy a new BPMN model

With the following command a BPMN model created with the [Imixs-BPMN Modeling Tool](./modelling/index.html) can be deployed into the Imixs-Microservce.

    curl --user admin:adminadmin --request POST \
    	-Tticket.bpmn \
    	http://localhost:8080/imixs-microservice/model/bpmn

### Request the Deployed Model Version

The following command returns the model versions deployed into the service: 

    curl --user admin:adminadmin -H \
    	"Accept: application/xml" \
    	http://localhost:8080/imixs-microservice/model/

### Request the Task List

To request the current task list for the user 'admin' run:

    curl --user admin:adminadmin -H \
    	"Accept: application/json" \
    	http://localhost:8080/imixs-microservice/workflow/tasklist/creator/admin


### Create a new Process Instance
The next example shows how to post a new Workitem in JSON Format. The request post a JSON structure for a new workitem with the txtWorkflowGroup 'Ticket', the ProcessID 1000 and ActivityID 10. The result is a new process instance controlled by Imixs-Workflow Engine



	curl --user admin:adminadmin -H "Content-Type: application/json" -H "Accept: application/json"  -d \
	'{"item":[
	        {"name": "$modelversion", "value":["1.0"]},
	        {"name": "$taskid", "value": [1000] }, 
	        {"name": "$eventid", "value": [10] }, 
	        {"name": "_subject", "value": ["some data...","more data..."]} 
	     ]}' \
	    http://localhost:8080/api/workflow/workitem


### Read a Process Instance
After posting a new process instance the Imixs-Workflow engine will retrun a datastructure includign the uniqueid of the created process instance.
The uniqueid is used to request a single Workitem from the Imixs-Microservice. See the following curl example to request a workitem by it's $UniqueID in JSON format:

    curl --user admin:adminadmin -H \
    	"Accept: application/json" \
    	http://localhost:8080/imixs-microservice/workflow/workitem/14b65352f58-259f4f9b

This example returns the content of the Workitem with the UniqueID '14b65352f58-259f4f9b'. You can also restrict the result to a subset of properties when you add the query parameter 'items':


For more details read the section [Imixs-Rest API](./restapi/index.html)


