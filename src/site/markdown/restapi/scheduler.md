# The Workflow Scheduler Service

The resource _/scheduler_ provides methods to control the [Imixs Workflow Scheduler](../engine/scheduler.html) service.
The workflow Scheduler can be monitored with the [Imixs-Admin Client](../administration.html).

<img src="../images/imixs-admin-client-06.png" class="screenshot" />  
 
## GET Jobs
The GET method is used to read all running ore completed jobs:

| URI               | Method | Description                                                                        |
| ----------------- | ------ | ---------------------------------------------------------------------------------- |
| /scheduler/{name} | GET    | returns the scheduler configurration by name (e.g. `org.imixs.workflow.scheduler`) |

A scheduler configuration is defined by the following item definitions:

```xml
<document xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xs="http://www.w3.org/2001/XMLSchema">
    <item name="type">
        <value xsi:type="xs:string">scheduler</value>
    </item>
    <item name="name">
        <value xsi:type="xs:string">org.imixs.workflow.scheduler</value>
    </item>
    <item name="_scheduler_class">
        <value xsi:type="xs:string">org.imixs.workflow.engine.WorkflowScheduler</value>
    </item>
    <item name="_scheduler_definition">
        <value xsi:type="xs:string">hour=*</value>
        <value xsi:type="xs:string">minute=30</value>
    </item>
    <item name="_scheduler_enabled">
        <value xsi:type="xs:boolean">true</value>
    </item>
</document>
```

## POST a new Configuration

The methods POST allow to update/create a scheduler configuration. Depending on the flag `_scheduler_enabled` the scheduler will be started or stopped :

| URI         | Method | Description                                                              |
| ----------- | ------ | ------------------------------------------------------------------------ |
| /scheduler/ | POST   | posts a scheduler configuration. The post data is expected in xml format |

The following curl example shows how to update a scheduler configuration:

    curl --user admin:adminpassword -H "Content-Type: text/xml" -d \
      '<document xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xs="http://www.w3.org/2001/	XMLSchema"> \
       <item name="type"><value xsi:type="xs:string">scheduler</value></item> \
       <item name="name"><value xsi:type="xs:string">org.imixs.workflow.scheduler</value></item> \
       <item name="_scheduler_class"><value xsi:type="xs:string">org.imixs.workflow.engine.WorkflowScheduler</value></item> \
       <item name="_scheduler_definition"><value xsi:type="xs:string">hour=*</value></item> \
       <item name="_scheduler_enabled"><value xsi:type="xs:boolean">true</value></item> \
    </document>' \
    http://localhost:8080/api/adminp/jobs

In case the scheduler definition is not valid the attribute '$error_code' will be returned in the response.
