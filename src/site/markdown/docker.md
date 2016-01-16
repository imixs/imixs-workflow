#The Imixs-Workflow Docker Container

The subproject [Imixs-Microservice](https://github.com/imixs/imixs-microservice) provides a WebService Interface which can be used to interact with the Imixs-Workflow-Engine over the [Imixs Rest API](./restapi/index.html). 
The 'Imixs-Microsoervice' is a Java EE Web Module which extends the Imixs-Workflow Engine providing a Rest Service for Human Centric Workflow Applications. This service can be used as a single Microservice or bundled with a Java EE Business Application. See also the [Deployment Guide](./deployment/index.html) for details.

To use Imixs-Workflow out of the box, the Imixs-Microservice is also deployable as a docker image which can be deployed into any [Docker Environment](https://www.docker.com/). 


## How to Build and Install the Docker Container
To build and run the docker container the following steps are necessary:

 1. Checkout Source from GitHub
 2. Build the Docker Image
 3. Start the Docker Image

 

### Checkout the Sources from GitHub

The 'Imixs-Microsoervice' is published on [GitHub](https://github.com/imixs/imixs-microservice). The project includes a Docker file to build the Docker Image. To build the Dokcer image first checkout the source from the git repository

    git clone https://github.com/imixs/imixs-microservice.git

next run the following [Maven](https://maven.apache.org/) command to build the Imixs-Microservice Java EE web module:

    mvn clean install -Pwildfly -DskipTests

### Build the Docker container

After the Imixs-Microservice was build with Maven, the Docker Image can be created form the source directory:

    docker build --tag=imixs-microservice .

Note: Take care about the ending '.' in the commandline!


### Starting the Imixs-Microservice

Finally you can start the docker container within your Docker environment. The Docker container provided with Imixs-Microservice runs with WildFly 9.0.2 and PostgreSQL. To start the container use the Docker run command:

    docker run --rm -ti -p 8080:8080 -p 9990:9990 imixs-microservice

After the server was started, you can run Imixs-Microservice from your web browser:

[http://localhost:8080/imixs-microservice](http://localhost:8080/imixs-microservice)


## Testing the Imixs-Microservice

Using the commandline tool '[curl](http://curl.haxx.se/)' makes it easy to test the Imixs-Microservice. Here are some examples.

*NOTE*: As Imixs-Workflow is a human-centric Workflow Engine onyl authenticated users (Actors) can interact with the engine. Therefore it is necessary to authenticate against the Imixs-Rest service API. Imixs-Microservice provides a User-Management-Service to register and authenticate users. The default user has the userid 'admin' and the default password 'adminadmin'. This user is used  in the following examples:

###Deploy a new BPMN model

With the following command a BPMN model created with the [Imixs-BPMN Modelling Tool](./modelling/index.html) can be deployed into the Imixs-Microservce.

    curl --user admin:adminadmin --request POST -Tsrc/main/resources/ticket.bpmn http://localhost:8080/imixs-microservice/model/bpmn

###Request the Deployed Model Version

The following command returns the model versions deployed into the service: 

    curl --user admin:adminadmin -H "Accept: application/xml" http://localhost:8080/imixs-microservice/model/

### Request the Worklist

To request the current Worklist for the user 'admin' run:

    curl --user admin:adminadmin http://localhost:8080/imixs-microservice/workflow/worklist

to get the same result in JSON format:

    curl --user admin:adminadmin -H "Accept: application/json" http://localhost:8080/imixs-microservice/workflow/worklist

to get a list with all process instances created by the user 'admin' run:

    curl --user admin:adminadmin -H "Accept: application/json" http://localhost:8080/imixs-microservice/workflow/worklistbycreator/admin

### How to create a new Process Instance
The next example shows how to post a new Workitem in JSON Format. The request post a JSON structure for a new workitem with the txtWorkflowGroup 'Ticket', the ProcessID 1000 and ActivityID 10. The result is a new process instance controlled by Imixs-Workflow Engine

    curl --user admin:adminadmin -H "Content-Type: application/json" -d '{"item":[ {"name":"type","value":{"@type":"xs:string","$":"workitem"}}, {"name":"txtworkflowgroup","value":{"@type":"xs:string","$":"Ticket"}}, {"name":"$processid","value":{"@type":"xs:int","$":"1000"}}, {"name":"$activityid","value":{"@type":"xs:int","$":"10"}}, {"name":"txtname","value":{"@type":"xs:string","$":"test-json"}}]}' http://localhost:8080/imixs-microservice/workflow/workitem.json



### Media Types

If you don't specify the HTTP media type, the Imixs-Rest API will return all results as HTML output. You can see this also in your Browser. But Imixs-Workflow also supports the media types JSON and XML for each request type. To request the same URL in JSON you can add a Header parameter like this:

    curl --user admin:mypassword -H "Accept: application/json" http://localhost:8080/imixs-microservice/workflow/worklist

or if you want to get the same response in XML format:

    curl --user admin:mypassword -H "Accept: application/xml" http://localhost:8080/imixs-microservice/workflow/worklist

### Read a Process Instance
If you know the UniqueID of a running process instance, you can request a single Workitem from the Imixs-Microservice. See the following curl example to request a workitem by it's $UniqueID in JSON format:

    curl --user admin:adminadmin -H "Accept: application/json" http://localhost:8080/imixs-microservice/workflow/workitem/14b65352f58-259f4f9b

This example returns the content of the Workitem with the UniqueID '14b65352f58-259f4f9b'. You can also restrict the result to a subset of properties when you add the query parameter 'items':

    curl --user admin:adminadmin -H "Accept: application/json" http://localhost:8080/imixs-microservice/workflow/workitem/14b65352f58-259f4f9b?items=txtname;$processid

For more details read the section [Imixs-Rest API](./restapi/index.html)

## JUnit Tests

The Imixs-Microservice project provide a set of JUnit Tests. These tests can be used as a starting point to see how the RestService API works. Read also the [Section Testing](testing.html) to learn more about the Imixs-Workflow Test Suite. 
