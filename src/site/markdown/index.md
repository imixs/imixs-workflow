# How Imixs-Workflow Works

Imixs-Workflow is an open source BPMN workflow engine built on the Jakarta EE architecture. The engine is optimized for human-centric business process management, providing essential features for professional process management:

- Model and control business processes with BPMN 2.0
- Enforce compliance rules and guidelines
- Secure business data management

In this way, Imixs-Workflow improves your business processes and supports human activities in a task-centric and event-driven way.

## BPMN 2.0

Imixs-Workflow allows you to execute **BPMN 2.0** models. This standardized approach allows you to change your business logic without touching a single line of code.

<img src="./images/bpmn-example01.png"  />

Learn how to design your own business processes in the section "[How to Model with Imixs-BPMN](./modelling/howto.html)". To install Imixs-BPMN see the [installation guide](./modelling/install.html).

## How to Get Started

The fastest way to get started is our [Quickstart Guide](quickstart.html), which shows you different approaches to integrate Imixs-Workflow into your project.

### Jakarta EE Integration

Imixs-Workflow is built on the Jakarta EE standard, allowing you to integrate the workflow engine directly into your business application. Simply add the following Maven dependencies:

```xml
<dependency>
    <groupId>org.imixs.workflow</groupId>
    <artifactId>imixs-workflow-engine</artifactId>
    <version>${org.imixs.workflow.version}</version>
</dependency>
<dependency>
    <groupId>org.imixs.workflow</groupId>
    <artifactId>imixs-workflow-index-lucene</artifactId>
    <version>${org.imixs.workflow.version}</version>
</dependency>
<dependency>
    <groupId>org.imixs.workflow</groupId>
    <artifactId>imixs-workflow-jax-rs</artifactId>
    <version>${org.imixs.workflow.version}</version>
</dependency>
```

This embedded mode gives you full access to the workflow engine API by injecting the [workflow engine](./engine/index.html) as a service component.

```java
@Inject
WorkflowService workflowService;
...
ItemCollection workitem=new ItemCollection().model("1.0.0").task(100).event(10);
workitem=workflowService.processWorkItem(workitem);
```

You can extend the functionality through the [Imixs-Workflow Plugin API](engine/plugins/index.html).

Alternatively you can start with the [Imixs Process Manager](https://github.com/imixs/imixs-process-manager) as a Jakarta EE template for your own application. The Process Manager can be deployed within seconds using Docker.

See also the [Deployment Guide](./deployment/deployment_guide.html) for information how to deploy the Imixs-Workflow engine on an application server.

<h3>Microservice Architecture <img src="./images/docker_small_h-trans.png" height="40" style="vertical-align: middle; position: relative; top: -4px" /></h3>

Alternatively, you can run Imixs-Workflow as a standalone microservice in your architecture. This approach allows you to interact with the workflow engine through its [REST API](restapi/index.html), making it completely language-independent. The workflow engine acts as a "black box", managing your business processes while being accessible from any programming language or platform.

Start with the [Imixs-Microservice Project](https://github.com/imixs/imixs-microservice) on GitHub, which provides full Docker support for quick deployment and scaling.

### Web Development with Imixs-Forms

For web developers, [Imixs-Forms](./webforms/index.html) offers an alternative integration approach. This lightweight JavaScript framework allows you to build workflow-enabled web applications on top of the Imixs-Workflow engine. Simply integrate the framework into your web project and start building workflow-driven forms while the robust Java backend handles all workflow operations.

Find out more about Imixs-Forms in our [Quickstart Guide](quickstart.html).

## What's Next...

Get started now and read more about:

- [The Quickstart Guide](quickstart.html)
- [How to Model with Imixs-BPMN](./modelling/howto.html)
- [How to Manage your Business Data](./quickstart/workitem.html)
- [Why You Should Use Imixs-Workflow](./quickstart/why.html)
- [What Means Human Centric Workflow?](./quickstart/human.html)
- [Imixs-BPMN - The Modeler User Guide](./modelling/index.html)
- [The Imixs-Workflow Plugin API](./engine/plugins/index.html)
- [The Imixs-Workflow Rest API](./restapi/index.html)

## Need Help?

If you have any questions about the Imixs-Workflow project or how you can best integrate Imixs-Workflow in your own project,
[join the Imixs Community](https://www.imixs.org/sub_community.html).
