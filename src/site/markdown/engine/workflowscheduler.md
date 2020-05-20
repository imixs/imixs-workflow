# The Workflow Scheduler

The Imixs-Workflow Scheduler is responsible for the automated processing of *scheduled events*. A *scheduled event* is a BPMN event assigned to a BPMN task. *Scheduled events* can be used e.g. for a auto reminding or an escalation task.

The Imixs WorkflowScheduler implements a TimerService to process workitems based on *scheduled events*.  A *scheduled event* can be configured, using the [Imixs-BPM Modeler](../modelling/index.html), on the property tab 'Timer'. If a workitem is in a status having scheduled events, the Imixs-Workflow Scheduler will process this workitem automatically based on the event definition.  
 
<center><img src="../images/modelling/bpmn_screen_35.png" style="max-width: 750px;" /></center>

## The Scheduled Event

A *scheduled event* can be configured in the property tab 'Timer'. 

  
|            | Description                                                                  |
|------------|------------------------------------------------------------------------------| 
|enabled     | choose 'yes' to enable a scheduled event (default 'no')                      |
|selection   | optional definition to select workitems based on a query or selector class   |
|delay       | delay in minutes, hours, days or working days                                | 
|from        | base item to calculate the schedule                                          | 

**Note:** A scheduled event must define a base datetime item to calculate the schedule.
You can choose a standard datetime item like '$created', '$modified' or '$lastprocessdate' or you can define any datetime item managed by your application (e.g. a due-date or an invoice-date) . 


 
 
## The Timer Configuration

The *WorkflowSchedulerService* expects the scheduling configuration document. The scheduling configuration defines the timer interval to run the workflow scheduler. 

A timer configuration can be created with the following item definitions:

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

The Scheduler configuration object can also be created by the *WorkflowSchedulerController* bean:

	@Inject 
	WorkflowSchedulerController workflowSchedulerController;
    
    public void setup() {
        ItemCollection config=workflowSchedulerController.getConfiguration();
        config.setItemValue("_scheduler_definition", "hour=*");
        config.appendItemValue("_scheduler_definition", "minute=/5");
        workflowSchedulerController.setConfiguration(config);
        workflowSchedulerController.saveConfiguration();
        workflowSchedulerController.startScheduler();
    }

With the method call startScheduler() the workflow scheduler will be started. 



### Scheduling

The *Imixs WorkflowSchedulerService* uses a calendar-based syntax for scheduling based on  the EJB 3.1 Timer Service specification. The syntax takes its roots from the Unix cron utility.
The following attributes can be stored as a value list in the item *'_scheduler_definition'*:
  
|Attriubte   |Description          | Possible Values                             |Default Value |       
|------------|---------------------|---------------------------------------------|--------------| 
|second      | One or more seconds  within a minute  | [0,59]                    |0             |
|minute      | One or more minutes  within a hour | [0,59]                       |0             |
|hour        | One or more hours  within a day  | [0,23]                         |0             |
|dayOfMonth  | One or more days <br />within a month  | [1,31] or "1st","2nd","3rd",.... <br />or "Sun","Mon","Tue","Wed","Thu","Fri","Sat" <br />or "Last" (the last day in month) <br />or -x (means x day(s) before the last day of <br /> or the month                                        |*  |
|month       | One or more month  within a year  | [1,12] or "Jan","Feb","Mar","Apr","May",<br /> "Jun","Jul","Aug","Sep","Oct","Nov","Dec"  | * |
|dayOfWeek   | One or more days within a week    | [1,7] or "Sun",Mon","Thu","Wed","Thu","Fri","Sat"  "0" and "7" refer to Sunday!           |*  |
|year        | A paticular year    | 4 digit calendar year                                                                                   |*   |
|timezone    | A specific time zone| see time zoneinof (or tz) database          |*             |
|start       | Start date          | yyyy/mm/dd                                  |              |
|end         | End date            | yyyy/mm/dd                                  |              |

Each attribute of an expression supports values in different formats. For example you can define a list of days or a range of years. The the following table with suported value formats: 

|Format      |Description             | Example                             |       
|------------|------------------------|-------------------------------------| 
|Single Value|only one possible value | year="2009"  or month="May"         |
|Wildcard    |all possible values     | year="*"  or dayOfWeek="*"           |
|List        |two or more values      | year="2014,2015"  or dayOfWeek="Sat,Sun"  |
|Range       |range of values         | second="1-10" or dayOfWeek="Mon-Fri"  |
|Increments  |startignpoint and increment| minute="*/15" or second="30/10"  |

So you can configure the scheduler is several ways. Here a some typical exampls for possible configuration:
 
### Every hour
    hour=*
 
### Every five minutes with the hour

    minute=*/15
    hour=*

### Every hour from Monday to Friday

    hour=*
    dayOfWeek=1-5

### Every 30 minutes with the hours from 8:00 AM to 5:00 PM, from Monday to Friday

    minute=*/30
    hour=8-17
    dayOfWeek=1-5

### Only Sunday on 1:00 AM 

    hour=1
    dayOfWeek=7
 
The configuration entity will be updated by the WorkflowSchedulerService in each iteration and provides the following additional information. 
 
| property               |type           | description                                                   |       
|------------------------|---------------|---------------------------------------------------------------| 
|$uniqueid               | String        | defines the unique ID the for the corresponding TimerService  |
|_scheduler_definition   | String List   | Holds information about the calendar based scheduling         |
|_scheduler_enabled      | Boolean       | indicates if the scheduler is enabled                         |
|_scheduler_logmessage   | String List   | message log (read only)                                       |
|_scheduler_errormessage | String        | Error message details (read only)                             |
|_scheduler_status       | String        | current status (read only) |
|_scheduler_class        | String        | read only must be set to "org.imixs.workflow.engine.WorkflowScheduler"  |
|nextTimeout             | Date          | Timestamp for next timeout  (read only)                       |
|timeRemaining           | Long          | milliseconds until next timeout  (read only)                  |




## Selector

The workitems processed by the scheduler are selected by a default selector based on the $TaskID and the $workflowgroup. 

	($taskid:"[TASKID]" AND $workflowgroup:"[MY-WORKLFOWGROUP]")
	
The selector can be overwritten by a BPMN event. For example the workflow group can be replaced by the modelversion to select only a subset of process instances:

	($taskid:"[TASKID]" AND $modelversion:"1.0.0")


### Custom QuerySelector

An application can implement a *QuerySelector* CDI bean to provide a custom selection of scheduled workitems. The full qualified class name of a QuerySelector can be used instead of a search query. 

	myapp.selectors.MyCustomSelector
  
If a   *QuerySelector* is defined, the WorkflowScheduler tries to inject the selector CDI Bean and calls the find method to select the workitems to be scheduled. 

### Ignored Workitems

The _WorkflowSchedulerService_ processes all kinds of workitems which are assigned to a valid workflow model definition with scheduled events. 
A workitem is ignored by the _WorkflowSchedulerService_  only in case the workitem type ends with the sufix 'deleted' 

	type=workitemdeleted

or the workitem is marked as immutable 

	$immutable=true

See the [DocumentService](./documentservice.html) for details. 
  
 