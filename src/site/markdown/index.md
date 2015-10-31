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
 
 
##How to get started...
To use Imixs-Workflow in your own business application you need to configure and integrate  the Imixs-Workflow Engine. Before you begin, consider the following:
 
  * Provide a Database where the Workflow Model and the Workitems can be stored
  * Configure a Security Realm to grant access for Actors
  * Define a Workflow Model using the [Imixs-Workflow-Modeler](./modelling/index.html) 
  
To integrate Imixs-Worflow in your JEE Application follow the next steps: 
 
  * Add the Imixs-Workflow jar files into your application. Read more on the [deployment guide](/jee/deployment/overview.html)
  * Add the Imixs-RestService into your application. Read more on [RestService-deployment guide](http://www.imixs.org/xml/deployment_rest_service.html])
  * Deploy your application together with the Imixs-Workflow on you Application Server

Before your deploy your application take care about the persistence and the security configuration.  You can find a [Sample Application on GitHub](https://github.com/imixs/imixs-jsf-example) which can be used to start. Also take a look at the web tutorial on the [Imixs Blog](http://blog.imixs.org/building-web-application-imixs-workflow-part/).

