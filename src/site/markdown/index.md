# How Imixs-Workflow Works

**Imixs-Workflow** is an Open Source Workflow engine optimized for a human-centric business process management. 

 * Imixs-Workflow controls your business processes and distributes tasks within an organization. 
 
 * Imixs-Workflow ensures that all tasks are processed in accordance to your Compliance Guidelines and Business Rules.

 * Imixs-Workflow stores your business data and protects it from unauthorized access. 
 
In this way Imixs-Workflow improves your business processes and supports human skills and activities in a task oriented and event driven way. 


## BPMN 2.0

In Imixs-Workflow you can design and execute your business process with the help of the **BPMN 2.0** modeling standard. BPMN 2.0 makes it easy to change your business logic without changing one single line of code.

<img src="./images/bpmn-example01.png"  />

You will learn to design your own business process in the section "[How to Model with Imixs-BPMN](.//modelling/howto.html)". To install Imixs-BPMN see the [installation guide](./modelling/install.html).

## How to Integrate 

The Imixs-Workflow engine can be integrated in different ways: 

 * Imixs-Workflow can be embedded into a **Jakarta EE** Application and extended by the  **Imixs-Plugin API**.
 
 * Or you can run Imixs-Workflow as a **Microservice** and interact with the engine through the **Imixs-Rest API**.


### Imixs-Workflow in the Embedded Mode

Imixs-Workflow is build on the Jakarta EE standard. Within this standard business applications can be developed on a highly scalable and transactional framework. Take a look into the [Quickstart Guide](quickstart.html) to learn how the Imixs-Workflow engine works in your own business application.
With the [Imixs-Workflow Plugin API](engine/plugins/index.html) you can extend the behavior of Imixs-Workflow. 
 
On Github you will find an [JSF Example application](https://github.com/imixs/imixs-jsf-example) which can be used as a starting point.
See also the [Deployment Guide](./deployment/deployment_guide.html) for information how to deploy the Imixs-Workflow engine on an application server.  
   

### Imixs-Workflow as Part of a Microservice Architecture 

Imixs-Workflow provides a [Rest Service API](restapi/index.html) and can be run as a service in a microservice architecture. In this architectural style the workflow engine can be bound to any existing business application, independent from the technology behind. Business logic can be changed without changing a single line of code.

You can start with the [Imixs-Microservice Project](https://github.com/imixs/imixs-microservice) which is available on Github. You can use the code as a template for your own microservice. 


<center><img src="./images/docker_small_h-trans.png"  /></center>

The Imixs-Workflow project also supports the use of [Docker](https://www.docker.com/). With this technology you can start an Imixs-Workflow instance with one single command in a container. Learn more about how to containerize Imixs-Workflow in the section [Docker](docker.html). See also the [Imixs-Microservice project](https://github.com/imixs/imixs-microservice) on Github which provides full Docker support.


## What's Next...

Get started now and read more about:

 * [The Quickstart Guide](quickstart.html)
 * [How to Model with Imixs-BPMN](./modelling/howto.html)
 * [How to Manage your Business Data](./quickstart/workitem.html)
 * [Why You Should Use Imixs-Workflow](./quickstart/why.html)
 * [What Means Human Centric Workflow?](./quickstart/human.html)
 * [Imixs-BPMN - The Modeler User Guide](./modelling/index.html)
 * [The Imixs-Workflow Plugin API](./engine/plugins/index.html)
 * [The Imixs-Workflow Rest API](./restapi/index.html)


## Need Help?

If you have any questions about the Imixs-Workflow project or how you can best integrate Imixs-Workflow in your own project, 
[join the Imixs Community](https://www.imixs.org/sub_community.html).
