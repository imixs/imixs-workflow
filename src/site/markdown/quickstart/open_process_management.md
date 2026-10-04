# What Is Open Process Management?

<p class="lead">
Open Process Management means your business processes stay under your control: built on open source, described in an open standard (BPMN 2.0) and connected through open interfaces, with no vendor lock-in.
</p>

A business process usually outlives the software that runs it. An approval flow or a ticket process may be used for ten years, while the tools around it change several times. If the process is locked into a proprietary format or a closed platform, every change of tools becomes a migration project, and every change of a license model becomes a risk.

Open Process Management is our answer to this. It is not a product and not a certification. It is a set of five principles, and each of them can be checked.

The first three describe the technical foundation. The last two describe how you keep control over your data, and how the project works together with its users.

## 1. Open Source Code

You can read, run and change the software that executes your processes. Imixs-Workflow is licensed under the **Eclipse Public License 2.0 (EPL 2.0)**. There is no enterprise edition and no feature limit, and it is free to use in production.

We build our own products, such as Imixs-Office-Workflow, on the very same engine. There is no better version that we keep for ourselves, and everyone is free to build a business on it as well.

**How to check it:** The source code is public on [GitHub](https://github.com/imixs/imixs-workflow). Clone it and build it.

## 2. Open Standard

Processes are described in **BPMN 2.0**, the international standard for business process models. A model is a plain file with the extension `.bpmn`. You can keep it in Git, compare versions and review changes like source code. [Imixs-BPMN](../modelling/index.html) is an open extension of the BPMN 2.0 standard with all properties that are needed to run human-centric workflows.

**How to check it:** Open a model from the [tutorial](../tutorials/tutorial-01.html) project in a text editor. It is a file you own, not an entry in the database of a proprietary tool.

## 3. Open Interfaces

The engine is accessed through a [REST API](../restapi/index.html) that supports XML and JSON. It runs as a microservice in a container, and as a Jakarta EE component it can be embedded in your own applications. With the [Plugin API](../engine/plugins/index.html) you can add your own business logic. You are free to choose the programming language for your applications.

**How to check it:** The web forms in the tutorial use the same REST API that your own applications would use.

## 4. Open Data

Your process data stays with you. The engine runs on your own infrastructure, on premises or in a cloud of your choice, together with a database that you operate. All process data can be read through the API in XML or JSON.

**How to check it:** The command `docker compose up` in the tutorial starts everything on your own machine, including the database.

## 5. Open Participation

Imixs-Workflow is developed in the open. Anyone can report a bug, start a discussion or contribute code. Companies can build their own solutions and services on top of the engine, as we do ourselves.

**How to check it:** The [issue tracker](https://github.com/imixs/imixs-workflow/issues) and the [discussions](https://github.com/imixs/imixs-workflow/discussions) are public.

## What Open Process Management Is Not

Open does not mean that you have to run everything alone. Imixs Software Solutions GmbH offers support and services, but you are not required to use them. The software works the same way without them.

## Open Process Management in Practice

If you are a developer or architect, start with the [tutorial](../tutorials/tutorial-01.html) and see how a process model becomes a running application in a few minutes.

If you are looking for a complete workflow solution for your organization, [Imixs-Office-Workflow](https://www.office-workflow.de/) is built on the same engine and follows the same principles.

## What's Next...

Continue reading more about:

- [How to Get Started with Imixs Workflow](../tutorials/tutorial-01.html)
- [Why Should I Use Imixs-Workflow?](../quickstart/why.html)
- [Imixs-BPMN - The Modeler User Guide](../modelling/index.html)
- [The Imixs-Workflow REST API](../restapi/index.html)
