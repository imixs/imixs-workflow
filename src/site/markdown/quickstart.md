# Quickstart

Starting with **Imixs-Workflow** you first of all create a workflow-model. The model describes your business logic. You can change this  logic during runtime without changing one line of code.
Imixs-Workflow is based on the [BPMN 2.0 modeling standard](http://www.bpmn.org/). BPMN is useful for visualizing both - the business process and the responsibilities. 

You create your workflow model with the Eclipse based modeling tool [Imixs-BPMN](./modelling/). Let's take look at a simple example:

<img src="./images/modelling/order-01.png" />

The blue boxes symbolize **Task** elements, while the yellow symbols describe **Event** elements. The later can change the state within the process.
An event is typically triggered by a process participant within your application. The example model can be download from [Github](https://github.com/imixs/imixs-workflow/tree/master/src/site/resources/bpmn). You will find more examples about modeling in the section [How to Model with Imixs-BPMN](./modelling/howto.html).

Next lets see how you can integrate Imixs-Workflow in your own Java application. 

**Notice:** In case you plan to use Imixs-Workflow as a microservice you can start an Imixs-Workflow instance with one single command in a container. Learn more about how to containerize Imixs-Workflow in the section [Docker](docker.html). See also the [Imixs-Microservice project](https://github.com/imixs/imixs-microservice) on Github which provides full Docker support.

## The Imixs Process Manger

With the [Imixs Process Managager](https://github.com/imixs/imixs-process-manager) you can start within seconds. The Business Process Management Suite can be used for development, testing and productive environments. It provides you with a generic user interface which can be easily adapted. The Imixs Process Manager comes with a Docker image that can be deployed locally or in a containerized environment like Docker Swarm or Kubernetes. 

Download the [docker-compose.yaml](https://raw.githubusercontent.com/imixs/imixs-process-manager/master/docker-compose.yaml) file and run:

	$ docker-compose up

You can access the Process Manger from your Browser

http://localhost:8080

Login with the default user `admin` and the password `adminadmin`. 

<img class="screenshot" src="./images/process-manager-002.png" width="80%" />

Find more about the Imixs Process Manager on [GitHub](https://github.com/imixs/imixs-process-manager).

## How to Integrate Imixs-Workflow into you Business Application

The Imixs-Workflow engine is based on Jakarta EE and so it can be integrated easily into a business application by just adding the maven depdencies:

		<dependency>
			<groupId>org.imixs.workflow</groupId>
			<artifactId>imixs-workflow-engine</artifactId>
			<version>${org.imixs.workflow.version}</version>
		</dependency>
		<dependency>
			<groupId>org.imixs.workflow</groupId>
			<artifactId>imixs-workflow-jax-rs</artifactId>
			<version>${org.imixs.workflow.version}</version>
		</dependency>
		<dependency>
			<groupId>org.imixs.workflow</groupId>
			<artifactId>imixs-workflow-index-lucene</artifactId>
			<version>${org.imixs.workflow.version}</version>
		</dependency>


From you code you can access the Imixs-Workflow engine by injection.

Let's see what this looks like in your Java code:

```java
@Inject
private WorkflowService workflowService;

ItemCollection workitem=new ItemCollection().model("1.0.0").task(1000).event(10);
// assign some business data...
workitem.setItemValue("_customer","M. Melman");
workitem.setItemValue("_ordernumber",20051234);
// process the workitem
workitem = workflowService.processWorkItem(workitem);
```

1. You inject the Workflow Engine with the annotation @Inject. 
2. Next you create a new business object and assign it to your model. 
3. You also can add your own business data. 
4. Finally you 'process' your object. 

From now on the newly created **Process Instance** is under the control of your business model. 
After you have created a new process instance you can use the _UniqueID_ to access the instance later: 
   
```java
String uniqueID=workitem.getUnqiueID();
....
// load the instance
ItemCollection workitem=workflowService.getWorkItem(unqiueID);
....
```

Depending on the design of your workflow model a process instance can be assigned to a team or a single process participant. E.g. the method _getWorkListByOwner_ can be used to select all process instances belonging to
a specified participant:

```java
List<ItemCollection> result=workflowService.getWorkListByOwner("melman", "workitem", 30, 0,null,false);  
```

See the documentation of the [WorkflowService](engine/workflowservice.html) for more details. 


## What's Next...

Continue reading more about:

 * [How to Model with Imixs-BPMN](./modelling/howto.html)
 * [How to Manage your Business Data](./quickstart/workitem.html)
 * [Why You Should Use Imixs-Workflow](./quickstart/why.html)
 * [What Means Human Centric Workflow?](./quickstart/human.html)
 * [Imixs-BPMN - The Modeler User Guide](./modelling/index.html)
 * [The Imixs-Workflow Plugin API](./engine/plugins/index.html)
 * [The Imixs-Workflow Rest API](./restapi/index.html)
 
