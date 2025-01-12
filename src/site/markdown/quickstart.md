# Quickstart Guide

Welcome to Imixs-Workflow! Whether you're a business analyst, developer, or system architect, Imixs-Workflow offers the right entry point for your needs.

All of the following approaches can be combined as your needs grow. You might start with process modeling using just the BPMN Modeler and later integrate these models into your web application. Or begin with the Jakarta EE workflow engine and gradually customize it by developing your own plugins and BPMN adapter classes.

Choose your preferred path to get started:

<div class="feature-boxes">
<div class="feature-box">
<h3>Open BPMN Modeler</h3>
<div class="audience">For Business Analysts</div>
<p>
Start designing your business processes visually with <a href="https://www.open-bpmn.org" target="_blank">Open-BPMN</a>. This modern BPMN 2.0 modeler integrates seamlessly into VS-Code or runs directly in your browser. No technical knowledge required.
</p>
<a href="./modelling/">Learn more →</a>
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
Get productive immediately with <a href="https://www.office-workflow.com" target="_blank">Imixs-Office-Workflow</a>. This modern workflow suite comes with ready-to-use processes and a low-code customization approach. Start managing your business processes today.</p>
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

## Next Steps

- Visit our [GitHub repository](https://github.com/imixs/imixs-workflow) for examples
- Join our [community forum](https://github.com/imixs/imixs-workflow/discussions) for support
- Check out our [complete documentation](./docs/) for detailed information
