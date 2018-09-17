# How Imixs-Workflow works

**Imixs-Workflow** allows you to describe and execute your business process in a model driven way. The open source workflow engine is based on **BPMN 2.0** and designed for a human-centric business process management. In this way your application supports human skills and activities in a task oriented and event driven way. The Imixs-Workflow engine can be embedded into a Java Enterprise Application, or can be run as a microservice.


## BPMN 2.0

Imixs-Workflow is based on the [BPMN 2.0 modeling standard](http://www.bpmn.org/). You create a workflow model with the
Eclipse based modeling tool [Imixs-BPMN](./modelling/). 

With BPMN 2.0 a business process can be described from different perspectives.
In Imixs-Workflow the process status is described with the BPMN element '_Task_'. 
The status change between the Task elements is defined by BPMN element '_Event_'.

<img src="./images/bpmn-example01.png"  />

This type of modeling is also known as event-driven modeling. You can find more examples in the section "[How to Model](.//modelling/howto.html)". To install Imixs-BPMN see the [installation guide](./modelling/install.html).


## Imixs Microservice

Imixs-Workflow provides a Rest Service API and can be run as a service in a microservice architecture. In this architectural style the workflow engine can be bound to any existing business application, independent from the technology behind. Business logic can be changed without changing a single line of code.

The [Imixs-Microservice project](https://github.com/imixs/imixs-microservice) provides a Docker Containerj. You can start a Imixs-Workflow instance with one single command. 


## Human Centric Workflow

Imixs-Workflow is supporting human skills, activities and relieves collaboration in a task-oriented manner.
Each process instance can be assigned to different actors. 
The main objective is to support the human actors and provide them with relevant information about the business process. The workflow engine ensures that the business process is aligned to predetermined business rules:
 
  * Who is the owner of a business process
  * Who is allowed to access and modify the data
  * Who need to be informed
  
In that way Imixs-Workflow assists users in starting a new process, finding and processing open tasks and to complete current jobs. The Workflow Engine automatically routes open tasks to the next actor and notifies users about open tasks depending on the current process definition. 

Each business process can involve different users to interact with the Workflow Management System.
These users are called the *actors*. An Actor can either start, update or read a process instance and also the embedded business data
Imixs Workflow allows you to assign any kind of business data with a running process instance.
You can use Imixs workflow to control access to a process instance in a fine-grained way using an ACL. This includes the read and write access for users and roles. The ACL can be defined via the BPMN model for each Task or Event separately. 

<img src="./images/bpmn-example02.png" width="500px" />
 
## The Architecture
The architecture of the Imixs-Workflow engine provides a simple concept to develop a business application.
 
### The Process Model

The BPMN 2.0 based process model defines tasks and events to describe the life-cycle of a business process. 
The model driven approach helps to understand what happens in a business process. The Imixs-Workflow engine can start and execute a process instance assigned to a process model.


### The Workflow Engine
The Imixs-Workflow engine persists and controls the process instance into a database. The Workflow Engine ensures that a workitem is in sync with the process model. The Imixs-Workflow engine supports a Plugin-API to implement different function blocks of a workflow management system. 
This includes, for example: 

* Access Controll
* Routing 
* E-Mail Notification
* Versioning
* Business Rules
* Reporting 
* Archiving

### The Workitem
A running process instance is called a '*Workitem*'. Each workitem is assigned to a Task in the BPMN model and can contain several business information.
If a Workitem is still in process, the Workitem is called a '*running process instance*'. After a process instance is finished the workitem is '*closed*'. 
Imixs Workflow provides an [XML schema](core/xml/index.html) to translate all workflow and business information of a process instance into an open and compatible data format. As a result, business data can be safely archived with Imixs-Workflow for long periods of time.


### Tasklists
A Workflow Management System provides various views of all running process instances. A 'tasklist' contains all open tasks from the view of an individual actor. Imixs-Workflow provides a lot of different views to navigate through the running process instances. 
  
 
## Getting started...
There are several ways how you can benefit from Imixs-Workflow. The following section gives you a short guideline how to find out the best way to use Imixs-Workflow in your own project.
 
### Using the Imixs-Workflow Engine out of the Box
Using Imixs-Workflow out of the Box is a good starting point to run the workflow engine without modification or Java EE development. 
You can start with the [Imixs-Microservice Project](https://github.com/imixs/imixs-microservice) which can be deployed in a Java EE Application server or by simply starting a [Docker Container](https://hub.docker.com/r/imixs/imixs-microservice/). 

If you want to develop a business application or embed the Imixs-Workflow engine into an existing project, you can also start with the [Imixs-Workflow Sample Application](sampleapplication.html) which gives a good starting point to develop a new project from scratch. 

Before you start developing with Imixs-Workflow, consider the following:
 
  * Provide a database where the workflow data can be stored
  * Configure a [security realm](./deployment/security.html) for granting access to different actors
  * Design a workflow model using the [Imixs-Workflow-Modeler](./modelling/index.html) 
  * Deploy the workflow application on an application server
  


### Using Imixs-Workflow in a Java EE Project 
Using Imixs-Workflow in a Java EE project is the typical way to extend your own java project with the functionality of a workflow engine.  In this kind of usage you develop your own workflow application. Inside your application you integrate the Imixs-Workflow Engine to provide the typical functionality of a Workflow Management System. 

To integrate Imixs-Workflow engine in an JEE application consider the following steps: 
 
  * Add the Imixs-Workflow jar files to your application. 
  * Add the Imixs-RestService into your application. 
  * Deploy your application together with the Imixs-Workflow on a application server

Read the [Deployment Guide](./deployment/deployment_guide.html) about how to deploy the Imixs-Workflow engine into an enterprise application (EAR).  
   
### Contribute to the Imixs-Workflow project
You can also help in developing the Imixs-Workflow project or use the results of the project to implement new components. In this case you extend the implementation with additional features or just add a different behavior. So in different of the usage described before, you need more than a running instance of the Workflow  Engine or the Java EE libraries. In this kind of usage you should check out the source code packages from [GitHub](https://github.com/imixs/imixs-workflow) and set up a Java EE Project. The source code include also some JUnit tests which can help to test different behaviors of the engine.

See the [Imixs Workflow community site on GitHub](https://github.com/imixs/imixs-workflow) for source code or ask questions in the issue tracking tool. 

 
 