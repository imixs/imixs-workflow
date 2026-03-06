# Welcome to Imixs-Workflow

<p class="lead">
Whether you are building modern <strong>Cloud-Native Microservices</strong> or integrating into a robust <strong>Jakarta EE environment</strong>, you are always working with the same powerful core engine. This unified approach allows you to execute your BPMN models anywhere—from a standalone service to a ready-made suite like <a href="https://doc.office-workflow.com/">Imixs-Office-Workflow</a>. Start with the architecture that fits your current project and remain flexible to switch or combine environments later without ever losing your process logic.
</p>
Choose your preferred path to get started:

<div class="feature-boxes">
<div class="feature-box">
<h3>Open BPMN Modeler</h3>
<div class="audience">For Business Analysts</div>
<p>
Start designing your business processes visually with <a href="https://www.open-bpmn.org" target="_blank">Open-BPMN</a>. This modern BPMN 2.0 modeler integrates seamlessly into VS-Code or runs directly in your browser. No technical knowledge required.
</p>
<a href="./modelling/index.html">Learn more →</a>
</div>
<div class="feature-box">
<h3>Imixs-Forms</h3>
<div class="audience">For Web Developers</div>
<p>Build workflow-enabled web applications in minutes using <a href="https://github.com/imixs/imixs-forms" target="_blank">Imixs-Forms</a>. 
The JavaScript framework provides ready-to-use components and follows a low-code approach. Perfect for modern web applications.
</p>
<a href="./webforms/index.html">Learn more →</a>
</div>
<div class="feature-box">
<h3>Microservice Architecture</h3>
<div class="audience">For API-First Developers</div>
<p>Integrate Imixs-Workflow into any application using the comprehensive REST API. Language-independent, containerized, and ready for your microservice architecture. Full flexibility with minimal setup.</p>
<a href="../sub_microservice.html">Learn more →</a>
</div>
<div class="feature-box">
<h3>Jakarta EE Integration</h3>
<div class="audience">For Enterprise Developers</div>
<p>Embed Imixs-Workflow directly into your Jakarta EE applications. Take advantage of enterprise-grade features including transaction management, security integration, and scalability options.</p>
<a href="../sub_jee.html">Learn more →</a>
</div>
<div class="feature-box">
<h3>Imixs-Micro</h3>
<div class="audience">For IoT Developers</div>
<p>Run workflows on embedded devices with <a href="https://github.com/imixs/imixs-micro" target="_blank">Imixs-Micro</a>. The lightweight engine is optimized for IoT environments and industrial automation. Perfect for edge computing and distributed systems.
</p>
<a href="https://github.com/imixs/imixs-micro">Learn more →</a>
</div>
<div class="feature-box">
<h3>Imixs-Office-Workflow</h3>
<div class="audience">For Small and Medium Businesses</div>
<p>
Built on top of the Imixs-Workflow core engine, <a href="https://www.office-workflow.com" target="_blank">Imixs-Office-Workflow</a> is a ready-to-use BPM suite with organization management, task management and document features. Get productive immediately — no Java development required.</p>
<a href="https://www.office-workflow.com">Learn more →</a>
</div>
</div>

## Event-Based Process Modeling

Let's look at a simple order process to understand how business processes are modeled using the event-based approach of Imixs-Workflow:

<img src="./images/modelling/order-01.png" />

This BPMN diagram demonstrates how Imixs-Workflow uses an event-based approach to model a business process. While the blue boxes (Tasks) represent different status within the process, the yellow symbols (Events) define how to transition from one status to another.
For example, when a new order is received, it starts in the status "New Order". Through the "Submit" event, the order transitions into the "Prepare for shipment" status. This event-based approach gives you more flexibility in modeling your business logic, as you can:

- Define multiple events for a single task
- Add business rules and conditions to events
- Trigger automatic actions during status transitions
- Model complex approval workflows

The event-based approach of Imixs-Workflow makes it easy to adapt your process to real-world business scenarios where status changes often involve complex decision making and parallel activities.

## What's Next...

Get started now and read more about:

- [How to get Started with Imixs Workflow](./tutorials/tutorial-01.html)
- [How to Model with Imixs-BPMN](./modelling/howto.html)
- [How to Manage your Business Data](./quickstart/workitem.html)
- [Why You Should Use Imixs-Workflow](./quickstart/why.html)
- [What Means Human Centric Workflow?](./quickstart/human.html)
- [Imixs-BPMN - The Modeler User Guide](./modelling/index.html)
- [The Imixs-Workflow Plugin API](./engine/plugins/index.html)
- [The Imixs-Workflow Rest API](./restapi/index.html)

## Need Help?

If you have any questions about the Imixs-Workflow project or how you can best integrate Imixs-Workflow in your own project,
Join our [Community Forum](https://github.com/imixs/imixs-workflow/discussions) for support.
You will also find useful information on our [Imixs Community page](https://www.imixs.org/sub_community.html).
And you can check out our [Java Docs](./apidocs/index.html) for detailed information.
