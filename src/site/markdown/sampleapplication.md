# The Web Sample Application

To demonstrate the concepts of the Imixs-Workflow Engine, the project provides a Web Sample Application based on JSF. The source code can be downloaded from [GitHub](https://github.com/imixs/imixs-jsf-example). 

The following section gives you an overview how to build the sample application and how to deploy the application into an Application Server.
 
<center><img src="./images/imixs-sample-application-01.png"  class="screenshot"  /></center>

We assume, that your are familiar with the concepts of Java Enterprise, JSF and Maven. This tutorial also refers the general steps needed to get the Imixs-Workflow engine up and running on different platforms. You will find more detailed information about deployment in the [deployment guide](./deployment/index.html).


## How to Build the Imixs-Sample Application 
First checkout the sources from [GitHub](https://github.com/imixs/imixs-jsf-example) and build  the Imixs-Sample Application in your workspace. All Imixs-Workflow projects are build on [Maven](maven.html). This makes it easy to build the application from the sources. You can build the application with the maven command: 
 
    $ mvn clean install
 
You will find the war file in the target folder of your working directory. 
 
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



## Run the Imixs-Sample Application 

After you have successfully deployed your application you can open it from from your web browser:

[http://localhost:8080/workflow](http://localhost:8080/workflow)    

 
  
## The Workflow Model
Imixs-Workflow is based on BPMN. The workflow model describes the behavior of your Workflow. Things like the different states, the Read- or Write-Access, the Process History or Email notifications are declared in the Model. The Imixs-Sample Application contains already a valid model file for a "Trouble Ticket Workflow System" under _/src/workflow/ticket.bpmn_ 

<img src="./images/model-ticket.png"  class="screenshot"/>

Trouble Ticket Systems are commonly used in an organization's customer support. So this will be a good example to start with. You can explore and customize this sample model using the [Imixs-BPMN Modeler](./modelling/index.html).

Uploading a BPMN model into your application is done by using the [Imixs-Rest API](restapi/index.html). 
With the following Linux [curl command line tool](https://manpages.debian.org/stretch/curl/curl.1.en.html) you can upload the model from your workspace:

	$ curl --user admin:adminpassword --request POST -Tsrc/workflow/ticket.bpmn http://localhost:8080/api/model/bpmn

You can also use any other Rest Client Tools to manage your models. E.g: [Insomia](https://insomnia.rest/) or [Postman](https://www.getpostman.com/).

To verify all models available in your application use the URL:


	http://localhost:8080/workflow/api/model


## What's Next...

The Imixs-JSF-Sample application can be used as a starting point. You can add your own forms and views if you are familiar with the concepts of Java Enterprise, JSF and Maven. 

Get started now and read more about:

 * [The Quickstart Guide](quickstart.html)
 * [How to Model with Imixs-BPMN](./modelling/howto.html)
 * [How to Manage your Business Data](./quickstart/workitem.html)
 * [Why You Should Use Imixs-Workflow](./quickstart/why.html)
 * [What Means Human Centric Workflow?](./quickstart/human.html)
 * [Imixs-BPMN - The Modeler User Guide](./modelling/index.html)
 * [The Imixs-Workflow Plugin API](./engine/plugins/index.html)
 * [The Imixs-Workflow Rest API](./restapi/index.html)
 
 

