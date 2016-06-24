#The Workflow Scheduler
The Imixs WorkflowScheduler provides a JEE TimerService, which can schedule  workitems based on scheduled Workflow Activities.  A scheduled Workflow Activity can be defined using the [Imixs-BPM Modeler](../modelling/index.html) in the property tab 'Schedule'. If a workitem is in a stage with scheduled activities, the WorkflowScheduler will process  this workitem automatically in a defined interval. This is useful for automatic  workflow tasks like reminder or escalation tasks.
 
##The WorkflowSchedulerService EJB
The  WorkflowSchedulerService expects the scheduling configuration in a entity with the name 'org.imixs.workflow.scheduler' from  type 'configuration'. The configuration entity is used to start the timer service and confgure the intervall the timer should schedule the workflow activities. The WorkflowSchedulerService implements the following methods:
 
	public ItemCollection loadConfiguration() ;
	
	public ItemCollection saveConfiguration(ItemCollection configItemCollection)
			throws AccessDeniedException;

	public ItemCollection start() throws AccessDeniedException;
	
	public ItemCollection stop() throws AccessDeniedException ;
	
	public boolean isRunning();

The methods "loadConfiguration()" and "saveConfiguration()" can be used to lookup
or update the current workflow scheduler configuration entity.  The methods "start()" and "stop()" can be used to start or stop the timer service. The method "isRunning()" returns true if the TimerService was started. If the workflowScheduler was started the timerService can be identified by the $UniqueID of the scheduler configuration entity.
 
##The WorkflowScheduler configuration
The Imixs WorkflowSchedulerService uses a calendar-based syntax for scheduling based on  the EJB 3.1 Timer Service specification. The syntax takes its roots from the Unix cron utility.
The following attributes can be stored in the txtConfiguration property of the workflowScheulderSercice  configuration:
  
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
 
###Every hour
    hour=*
 
###Every five minutes with the hour

    minute=*/15
    hour=*

###Every hour from Monday to Friday

    hour=*
    dayOfWeek=1-5

###Every 30 minutes with the hours from 8:00 AM to 5:00 PM, from Monday to Friday

    minute=*/30
    hour=8-17
    dayOfWeek=1-5

###Only Sunday on 1:00 AM 

    hour=1
    dayOfWeek=7
 
The scheudler information is stored in the property 'txtConfiguration' as a String List.
The configuration entity for the WorkflowSchedulerService holds the following additional information. 
 
| property   |type      | description                                                  |       
|------------|----------|--------------------------------------------------------------| 
|$uniqueid   | String   | defines the unique ID the for the corresponding TimerService  |
|txtConfiguration| String List   | Holds information about the calendar based scheduling|
|statusmessage|String   | last status message (read only)                               |
|Schedule    | String   | scheduling information (read only)                            |
|nextTimeout | Date     | Timestamp for next timeout                                    |
|timeRemaining | Long   | milisecnds until next timeout                                 |
|datLastRun  | Date     | Timestamp of last sucessfull run (read only)                  |
|numInterval | int      | optional- timer interval if no txtConfiguration is defined    |
|datStart    | Date      | optional- start date for timer  if no txtConfiguration is defined    |
|datStop     | Date      | optional- stop date for timer  if no txtConfiguration is defined    |

<strong>Note:</strong> The properties "statusmessage", "schedule", "nextTimeout" and "timeRemaining" are read only and will be updated computed if the method findConfiguration() was called.
 
  

## Security & Deployment
The ScheduledWorkflowService EJB is embedded into the security concepts of the Imixs  Workflow Engine. As the ScheduledWorkflowService needs full access rights to all workitems 
to perform a scheduled activity the EJB is annotated with the @RunAs declaration:

	@DeclareRoles( { "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
	@Stateless
	@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
	@Remote(org.imixs.workflow.jee.ejb.WorkflowScheduler.class)
	public class WorkflowSchedulerService implements WorkflowSchedulerServiceRemote {
	 ....
	}

This means that the WorkflowSchedulerService EJB need to be deployed with sufficient  security role mappings. For Glassfish it is necessary to declare a principal-name which will be used by the EJB Container calling the @TimeOut method.  You can use the glassfish specific glassfish-application.xml deployment descriptor  in the jee module. To declare a principal-name with manager access use the following  example where the userID "IMIXS-WORKFLOW-Service" is mapped to the Role "org.imixs.ACCESSLEVEL.MANAGER" in the  realm "imixsrealm": 

	<glassfish-application>
	....
	    <security-role-mapping>
	        <role-name>org.imixs.ACCESSLEVEL.MANAGERACCESS</role-name>
	        <group-name>Manager</group-name>
	        <principal-name>IMIXS-WORKFLOW-Service</principal-name>
	    </security-role-mapping>
	    <realm>imixsrealm</realm>
	 ...
	</glassfish-application>
 
So in this case the TimerService will be performed by the user "Manfred".

<strong>Note:</strong> Be careful to declare this role and principal-name with care and a setting which is reasonable in your server environment. For example use a reserved user account like "IMIXS-WORKFLOW-Service" which is not used by persons in your  organization.
          

### galssfish-ejb-jar.xml

Using the galssfish-ejb-jar.xml descriptor it is also necessary to add a principal user to  the ejb declaration which will be associate to the @RunAs annotation. If no principal is defined in galssfish-ejb-jar.xml, the application server uses a principal  from the security-role-mapping. If there is only one principal associated with the role,  that principal will be taken as the default run-as principal value. But if there is more  than one principal associated with the role, you need to explicitly set the run-as  principal in the galssfish-ejb-jar.xml. The following example demonstrates setting the run-as principal in galssfish-ejb-jar.xml:

	 ...
	<ejb>
		<ejb-name>WorkflowSchedulerService</ejb-name>
		<principal>
			<name>IMIXS-WORKFLOW-Service</name>
		</principal>
	</ejb>
	 ...
 
 