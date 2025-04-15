# How to get Started with Imixs Workflow

In this tutorial you will learn how get started with your own Imixs-Workflow Project.
Imixs-Workflow is an open-source BPMN 2.0 workflow engine based on Java. It allows developers to model, execute, and monitor business processes in Java EE environments. In this tutorial you will learn :

- Deploy in containerized environments (Docker)
- Install the Imixs Open BPMN Modeller
- How to design your own workflows using BPMN 2.0
- write your first Java Workflow Plugin

Before you get started, ensure you have:

- Java JDK 17 or later installed
- Apache Maven 3.6+
- Git installed and a basic understanding about Git.
- Docker installed (for container deployment) and a basic understanding about Docker
- Basic knowledge in Java and JavaScript

## Step 1: Setting Up Your Development Environment

The easiest way to get started is with the Imixs Starter project 'Imixs-Forms'.
Imixs Forms is a SinglePage Application connected to the Imixs-Workflow engine via the Imixs Rest API. This allows you to get familiar with the basis concepts. You can implement Applications in any other programming language using the Rest API. We will also implement a Java Plugin to learn how to implement your own business logic. And of course it is also possible to implement a pure Jakarta EE Application with Imixs-Workflow.

So first get a copy of the imixs-forms project using Git:

```bash
$ git clone --depth=1 https://github.com/imixs/imixs-forms.git my-project
$ cd my-project && rm -rf .git
```

To run your project you just need to start the docker compose stack. If not yet installed see the [official install guide](https://docs.docker.com/engine/install/).

Start the demo with:

```bash
$ docker compose up
```

The docker-compose.yaml file expects a data directory `./models/` providing your BPMN models to be automatically imported during startup. You can add later your own BPMN models here.

```
.
├── docker-compose.yaml
└── models/
    └── ticket-en-1.0.0.bpmn
```

Our Environment now starts a Postgres Database and the Imixs-Workflow Microservice. We will use the microservice to interact with the workflow engine. You will get an overview about the Imixs-Microservice if you open the following URL in your Web Browser:

http://localhost:8080/

## Step 2: Test your Imixs-Forms App

The Imixs-Forms project provides you with a single-page application based on HTML and JavaScript. You can test the application from your Browser with the following URL:

http://localhost:8080/app/?modelversion=ticket-en-1.0&taskid=1000

<img src="../images/imixs-tutorial-01.png" class="screenshot" />

This shows you a Web Form with some input elements and a submit button. The submit button will already trigger the Workflow Engine and creates a new so called 'process instance'.

That's it - you Imixs Workflow project is up and running. Next let's see how to model a BPMN model.

## Step 3: Create your own Model

Before we can start modelling you need to install Visual Studio Code. If you have not yet installed Visual Studio Code just follow the [official install guide](https://code.visualstudio.com/).

Next go to the ‘Extensions’ section and search for ‘Open-BPMN’. Install the extension. Now you are ready to open your BPMN models. Just open your project with `File > Open Folder...` and you can access the BPMN Models:

<img src="../images/imixs-tutorial-02.png" class="screenshot" />

Now let's see how the Imixs BPMN Model works. If we take a closer look at the `ticket-en-1.0.0.bpmn` model you can see a BPMN Start Element (gree cycle) and a BPMN Task element (blue box) named 'New Ticket'.

<img src="../images/imixs-tutorial-03.png" class="screenshot" />

The start element defines where the workflow get started. The Task element defines the initial state of a new Process instance. And the BPMN Data Element named "Form" defines the Input Fields in your Web Form.
If you double click on the Data Element you will see the form definition:

```xml
<?xml version="1.0"?>
<imixs-form>
  <imixs-form-section label="Address Data:"> <!-- each section has 12 columns -->
    <item name="zip" type="text"  label="ZIP:" span="2" />
    <item name="city" type="text"  label="City:" span="6" />
    <item name="country" type="text"  label="Country:" span="4" />
  </imixs-form-section>
  <imixs-form-section label="Order Details">
    <item name="budget" type="currency"  label="Budget:" span="6" />
    <item name="details" type="html" label="Description" />
  </imixs-form-section>
</imixs-form>
```

Lets see how we can add a new input field.

Add the following line below the item "budget"

```xml
...
<item name="costcenter" type="text"  label="Cost Center:" span="4" />
...
```

Save the Model and restart your Docker server (stop it with 'ctrl + c')

Now you will see a new Input Field named "Cost Center" beside the Budget field.

<img src="../images/imixs-tutorial-04.png" class="screenshot" />

The Imixs-Workflow Engine will automatically store the field values in a new process instance and we can use this information for further processing steps.

Now lets see what happens when we click on the "Submit". In the BPMN event you can see the Submit button is defined as an BPMN Cath Event. And the event is connected by a sequence flow with the Task named "Open"

<img src="../images/imixs-tutorial-05.png" class="screenshot" />

This means, when you click the "Submit" button the corresponding event in your model will be triggered and the Workflow Engine will change the state of our Process Instance from "New Ticket" into "Open". You can see this effect in the headline of our web form. And also you will notice that new Buttons are shown up:

<img src="../images/imixs-tutorial-06.png" class="screenshot" />

The buttons you see are the corresponding BPMN Events in our model connected with the Task "Open". Each of the events defines a new transition from one status into another or just stay in the same status (as in the 'save' event)

<img src="../images/imixs-tutorial-07.png" class="screenshot" />

This is called a "Event Driven Workflow". Each BPMN Event defines what happens if the user triggers the event. Each event can change the status in our process flow and also triggere different backend services as we will see later.
