#How Imixs-Workflow works

Imixs-Workflow provides a framework for the development and management of Human-Centric workflow applications. Imixs-Workflow allows to describe and control a business process in a BPMN 2.0 model. Thus the implementation and maintenance of business applications is even faster and becomes more flexible. Imixs-Workflow focuses on Human-Centric workflows. This means that a business process is typically started by a user (called 'actor'). The Workflow Engine manages the state of a business object and guides the actor through the process providing necessary information for a business task. The main objectives of a Human-Centric Workflow solution are:
 
  * What is the current Status of a business process 
  * Who is the owner for a business process
  * Who need to be informed
  * What happened so far to the business process
  * Who is allowed to access and modify data

In that way Imixs-Workflow assists users in starting a new process, finding and processing open tasks and helps users to complete current jobs in the defined way. The Workflow Engine automatically routes open tasks to the next actor and notifies users about new tasks depending on the current process definition. The following illustration demonstrates the typical flow of a task from one user to another controlled by a Workflow Management System:
 
<img src="./images/imixs-overview.png"  />
 
A Workflow Management System - also called WFMS or Workflow System - provides the following general concepts:
 
###The Actors
Each business process define different users to be involved. These users are called actors. A actor interacts with the Workflow Management System during a business process. Actors can either start, update or read information about a process instance. The Workflow Management System also grants actors access to a process instance. This means that users which are not involved are not allowed to access a running process instance. Thus a Workflow Management System provides also a security layer to secure business data based on a process model.  
 
 
###The Workitem
A running process instance inside a Workflow Management System is called 'Workitem'. Each Workitem can assume different states during a business process and can contain any kind of business data. If a Workitem is still processed and yet not finished, the Workitem is called a 'running process instance'. The status of a workitem is defined by the process model and is controlled by the workflow engine.
 

###The Process Model
The Process Model (or Workflow Model) defines which status a process instance can present during its life-cycle. Each time a Workitem  is processed, the Workflow Management System assigns a new status (Task). The transition form one status into another is defined by the Workflow Activities (Event). 

###The Workflow Engine
The Workflow Engine persists and controls each process instance depending on the model definition. The Workflow Engine ensures that a workitem is always in a state defined by the process model and can not change its status in a way which is not defined by the model. The Workflow Engine also routes the workitem to the next actor and sends notifications if defined by the model.  


###Tasklist and Statuslist
A Workflow Management System provides views of all running process instances. A 'tasklist' contains all open tasks from the view of an individual actor. The statuslist provides the user with the latest status information about running process instances. There are a lot of different views allowing the actor to navigate through the running process instances. 
 
 
 
 
 
## Getting started...
There are several ways how you can benefit from Imixs-Workflow. The following section gives you a short guideline how to find out the best way to use Imixs-Workflow in your own project.
 
### Using the Imixs-Workflow Engine out of the Box
Using Imixs-Workflow out of the Box is a good starting point to run the workflow engine without modification or Java EE development. You can start with the [Imixs-Workflow Sample Application](https://github.com/imixs/imixs-jsf-example) if you don't want to develop a new project from scratch. 

Before you begin, consider the following:
 
  * Provide a database where the workflow model and the workitems can be stored
  * Configure a security realm to grant access for actors
  * Define a workflow model using the [Imixs-Workflow-Modeler](./modelling/index.html) 
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

 
 