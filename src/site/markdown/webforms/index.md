# Imixs-Forms

Imixs-Forms is a lightweight JavaScript framework that helps you build workflow-enabled web applications in minutes. Define your forms directly in your BPMN model and let Imixs-Forms handle the rest.

<img src="../images/imixs-forms-01.png" />

## Quick Start with Docker

Get started in less than 5 minutes:

### 1. Download the demo application:

```bash
$ git clone https://github.com/imixs/imixs-forms-demo
$ cd imixs-forms-demo
```

### 2. Start the Imixs-Workflow Service with Docker.

If not yet have Docker installed on your machine, see the [official install guide](https://docs.docker.com/engine/install/).:

```bash
$ docker compose up
```

### 3. Open the demo application:

    http://localhost:8080/app/?modelversion=ticket-en-1.0&taskid=1000

## Create Your First Form

To create your own forms, you'll need the Open-BPMN VS Code extension:

1. Install VS Code if you haven't already
2. Open VS Code and install the 'Open-BPMN' extension from the marketplace
3. Open the model file `tiket-en-1.0.0.bpmn' or create a new BPMN file and start modeling your workflow
4. Upload your model using the `curl` command

```bash
   curl --user admin:adminadmin --request POST -Tticket-en-1.0.0.bpmn http://localhost:8080/api/model/bpmn
```

<img src="../images/imixs-forms-02.png" />

### Define Forms in BPMN

Each task in your BPMN model can have its own form definition. Here's how it works:

1. Create a Data-Object in your BPMN Model
2. Connect a task element with an association with your Data Object
3. Open the properties panel of the Data Object by double-click
4. Add a form definition:

```xml
<imixs-form>
  <imixs-form-section label="Customer Data:">
    <item name="name" type="text" label="Name:" />
    <item name="email" type="text" label="Email:" />
  </imixs-form-section>
</imixs-form>
```

## Next Steps

- learn more about [Imixs-Forms Layout](../webforms/forms.html)
- learn more about [Imixs-Forms XML](../webfomrs/xml.html)
- read how the [Imixs Rest API works](../../sub_microservice.html)
- find out more about Imixs-Forms on [Github](https://github.com/imixs/Imixs-forms)
