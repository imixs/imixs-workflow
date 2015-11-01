#How Imixs-Workflow works

Imixs-Workflow provides a framework to develop and manage business applications in a very simple and flexible way. Imixs-Workflow allows to describe and control the workflow of business objects in a business process model. Thus you the implementation of business applications is even faster and becomes more flexible. Imixs-Workflow focuses on human-centric workflows. This means that a business process is  typically controlled by a user (called 'actor'). Imixs-Workflow manages the state of a business object and supports the actor with information to process a business task:
 
  * What is the current Status of a business object 
  * Who is the owner for a business object
  * What happended to the business object during the process
  * Who is allowed to access and modifiy the business object

In that way Imixs-Workflow assists users in starting a new process, finding and processing open tasks and helps users to complete current jobs in the defined way. The Workflow Engine automatically routes open tasks to the next actor and notifies users about new tasks depending on the current process definition. The following illustration demonstrates the typical flow of a task from one user to another controlled by a Workflow Management System:
 
<img src="./images/imixs-overview.png"  />
 
A Workflow Management System - also called WFMS or Workflow System - provides the following general concepts:
 
###The Actors
Each business process define different users to be involved. These users are called Actors. 
The Actor interacts with the Workflow Management System during a business process. 
Actors can either start, update or read a process instance. The Workflow Management System also grants access for actors to each specific process instance. This means that users which are not involved are not allowed to access a running process instance. Thus a Workflow Management System provides also a security layer to confidential business data.  
 
 
###The Workitem
In a Workflow Management System the data which is processed is called a  'Workitem'. Each Workitem can assume different states during a business process. If a Workitem is still processed and yet not finished, the Workitem is called a 'running process instance'. 
 The status of a workitem is defined by the workflow model and is controlled by the workflow engine.
 

###The Workflow Model
The Workflow Model defines which status a process instance can present during its life-cycle. Each time a Workitem  is processed, the Workflow Management System assigns a new status. The transition form one status into another  is defined by the Workflow Activities. 

###The Workflow Engine
The Workflow Engine controls each process instance depending on the model definition. The Workflow Engine  ensures that a workitem is always in a predefined state and can not change the state in a way  which was not defined by the model. The Workflow Engine also routes the workitem to the next actor  and sends notifications if defined by the model. 


###Tasklist and Statuslist
A Workflow Management System provides views of all running process instances. A 'tasklist' contains all open tasks from the view of an individual actor. The statuslist provides the user with the latest status information about running process instances.
 
 
 
 
 
##Getting started...
There are several ways how you can benefit from Imixs-Workflow. The following section gives you a short guideline how to find out the best way  to use Imixs-Workflow in your software development project.
 
###Using the Imixs-Workflow Engine out of the Box
Using Imixs-Workflow out of the Box is a good starting point to run the workflow engine  without modification or Java EE development. You can start with the [Imixs-Workflow Sample Application](https://github.com/imixs/imixs-jsf-example) if you don't want to develop a new project from scratch. 

Before you begin, consider the following:
 
  * Provide a Database where the Workflow Model and the Workitems can be stored
  * Configure a Security Realm to grant access for Actors
  * Define a Workflow Model using the [Imixs-Workflow-Modeler](./modelling/index.html) 
  * Deploy the Workflow Application
  


###Using Imixs-Workflow in a Java EE Project 
Using Imixs-Workflow in a Java EE project is the typical way to extend  your own java project with the functionality of a workflow engine.  In this kind of usage you develop your own Java EE project. Inside your application you integrate the Imixs-Workflow Engine to provide the typical functionality of a Workflow Management System. 

The {{{./deployment/overview.html}Imixs-Workflow deployment section}} describes how to deploy the Imixs-Workflow engine in an enterprise application (EAR) in more detail. To integrate Imixs-Worflow in your JEE Application consider the following steps: 
 
  * Add the Imixs-Workflow jar files into your application. Read more on the [deployment guide](/jee/deployment/overview.html)
  * Add the Imixs-RestService into your application. Read more on [RestService-deployment guide](http://www.imixs.org/xml/deployment_rest_service.html])
  * Deploy your application together with the Imixs-Workflow on you Application Server

  
   
###Contribute to the Imixs-Workflow project
You can also help in developing the Imixs-Workflow project or use the results of the project to  implement new components.  In this case you extend the implementation with additional features or just add a different behavior. So in different of the usage described before, you need more than a running instance of the Workflow  Engine or the Java EE libraries. In this kind of usage you should check out the source  code packages from the {{{./source-repository.html}Source Code repository}} and set up a Java EE Project like  the Imixs Java EE Sample Application.  You can test the different parts of the implementation and extend the existing components. The source code include also some JUnit tests which can help you to test different behaviors of the engine in more detail.

See the [Imixs Workflow community site on GitHub](https://github.com/imixs/imixs-workflow) for further details or to ask questions or report bugs. 

 
 