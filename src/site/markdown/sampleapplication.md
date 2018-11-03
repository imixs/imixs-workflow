# The Imixs-JSF-Sample Application
The Imixs-Workflow project provides a Sample Application which demonstrates the behavior and the concepts of the Imixs-Workflow Engine. The source code can be downloaded from [GitHub](https://github.com/imixs/imixs-jsf-example). The following section gives a brief overview how to build the sample application and how to deploy it into an Application Server. This installation guide is also helpful to get a better understanding what is necessary to integrate Imixs-Workflow into your own project.  		
 
<center><img src="./images/imixs-sample-application-01.png" style="width:700px"  /></center>

This introduction refers the basic steps needed to get the Imixs-Sample Application up and running on different platforms. If you need further informations about the deployment of the Imixs-Workflow components, see the [section deployment](./deployment/index.html) for more detailed information.


## How to Build the Imixs-Sample Application 
First checkout the sources from [GitHub](https://github.com/imixs/imixs-jsf-example) and build  the Imixs-Sample Application in your workspace. All Imixs-Workflow projects are build on [Maven](http://maven.apache.org/). This makes it easy to build the application from the sources. After you have checked out the sources you can run the Maven install command to build the war file. 
 
    ~/git/imixs-jsf-example$ mvn clean install
 
You find the war file in the target folder of your working directory. 
 
## How to Setup the Environment 
Before you deploy the Imixs-Sample Application, you first need a Java EE application server like 
 [Glassfish](http://www.glassfish.org/), [WildFly](http://www.wildfly.org) or [Apache TomEE](https://tomee.apache.org/).  You need at least to configure the following resources:
 
 * a Database to store the Workitems and the Workflow Model
 * a JDBC Database connection from your Application Server to the database
 * a JAAS Security Realm to allow users to authenticate against the Workflow Application.

In the following deployment sections you will find more information how to setup a server environment:

  * [WildFly](./deployment/wildfly.html)
  * [GlassFish/Payara](./deployment/glassfish.html) 
  * [Apache TomEE](./deployment/tomee.html)


## How to Setup a Workflow Model
To run the workflow you need to provide a workflow model. The workflow model describes the behavior of your Workflow. Things like the different states, the Read- or Write-Access, the Process History or Email notifications are declared in the Model. The Imixs-Sample Application contains already a valid model file for a "Trouble Ticket Workflow System". A trouble ticket system is based on a business process that manages and maintains lists of issues, as needed by an organization. Trouble Ticket Systems are commonly used in an organization's customer support call center to create, update, and resolve reported customer issues, or even issues reported by that organization's other employees. You can explore and customize this sample model using the [Imixs-BPMN Modeler](./modelling/index.html).

## Run the Imixs-Sample Application 
After you have installed and setup the Imixs-Sample Application start the application with the following url from your browser:

[http://localhost:8080/workflow](http://localhost:8080/workflow)    

 
  
