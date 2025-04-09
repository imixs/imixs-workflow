# The Imixs Process Manager

With the Imixs Process Manager you can start quickly and develop and test your own Imixs business process.
The Imixs Process Manager provides a UI to start, execute and manage process instances.

<center><img src="./images/process-manager-001.png"  class="screenshot"  /></center>

You can use this [project available on Github](https://github.com/imixs/imixs-process-manager) also as a template for your own project. In this case you can [fork the project on Github](https://github.com/imixs/imixs-process-manager/).

## Run with Docker

The Imixs Process Manager comes with a Docker image which enables you to start within seconds. If you haven't already installed Docker, follow the instructions [here](https://docs.docker.com/get-docker/).

Download the [docker-compose.yml](https://raw.githubusercontent.com/imixs/imixs-process-manager/master/docker-compose.yml) file...

```
version: "3.6"
services:
  imixs-db:
    image: postgres:13.11
    environment:
      POSTGRES_PASSWORD: adminadmin
      POSTGRES_DB: workflow-db
    volumes:
      - dbdata:/var/lib/postgresql/data

  imixs-app:
    image: imixs/imixs-process-manager:latest
    environment:
      TZ: "CET"
      LANG: "en_US.UTF-8"
      JAVA_OPTS: "-Dnashorn.args=--no-deprecation-warning"
      POSTGRES_USER: "postgres"
      POSTGRES_PASSWORD: "adminadmin"
      POSTGRES_CONNECTION: "jdbc:postgresql://imixs-db/workflow-db"
    ports:
      - "8080:8080"
      - "8787:8787"
      - "9990:9990"
volumes:
  dbdata:
```

... and run

```bash
$ docker-compose up
```

You can access it from your web browser at: http://localhost:8080/

From the Web UI can upload your own process model and create customized forms. You can view and search running process instances.

<center><img src="./images/process-manager-002.png"  class="screenshot"  /></center>

## Authentication and Authorization

Imixs-Workflow is a human-centric workflow engine which means that each actor need to authenticate against the service to interact.

The default setup of the _Imixs Process Manager_ provides a set of predefined users which can be used for testing purpose. The test users are stored in a separate user and roles properties files. See the following list of predefined test user accounts:

| User    | Role                   | Password   |
| ------- | ---------------------- | ---------- |
| admin   | IMIXS-WORKFLOW-Manager | adminadmin |
| alex    | IMIXS-WORKFLOW-Manager | password   |
| marty   | IMIXS-WORKFLOW-Author  | password   |
| melman  | IMIXS-WORKFLOW-Author  | password   |
| gloria  | IMIXS-WORKFLOW-Author  | password   |
| skipper | IMIXS-WORKFLOW-Author  | password   |

You can add accounts or change the default account later, by updating the files "_sampleapp-roles.properties_" and "_sampleapp-users.properties_". You can also configure a different custom security realm (e.g. LDAP or Database).

You will find more information about the security concept in the [Imixs-Workflow Deployent guide](https://www.imixs.org/doc/deployment/index.html).

## Process Design

You can define your own business process models using the [Imixs-BPMN modeller tool](https://www.imixs.org/doc/modelling/index.html) and you can upload and execute your models directly within Imixs Process Manager. General information about how to model can be found [here](https://www.imixs.org/doc/modelling/howto.html).

The Imixs Process Manager allows you to define custom forms for your business process without programming. By defining an XML template, you can store forms directly in a BPMN 2.0 model.

<img src="https://raw.githubusercontent.com/imixs/imixs-process-manager/master/src/main/webapp/pages/model-example.png" />

Example:

```xml
<?xml version="1.0"?>
<imixs-form>
    <imixs-form-section label="Order">
    <item name="_orderid" type="text" label="Order ID:" />
    <item name="_orderdate" type="date" label="Order Date:" />
    </imixs-form-section>
</imixs-form>
```

You can create and change your models at runtime without interrupting your workflow instance.

Find out more about the Imixs-Process Manager on [Github](https://github.com/imixs/imixs-process-manager)
