#Rule Plugin 
The Imixs Rule Plugin (org.imixs.workflow.plugins.RulePlugin) can be used to evaluate a business rule during the processing of an activity. A business rule can be configured which the help of the [Imixs-BPMN modeler](../../modelling/index.html). Business rules can be written in any script language supported by the JVM.
 For example 'JavaScript or 'Groovy'. A Script can access all basic item values from the current WorkItem and the ActivityEntity. A script may not update values of the WorkItem but can modify the values of the Activity Entity. To access the item values the RulePlugin provide the map objects 'workitem' and 'activity'. The item values of a property are provided in an object array. See the following example:
 
	 // test the first value of the workitem attribute 'txtname'
	 var isValid = ('Anna'==workitem.get('txtname')[0]);

 
<strong>Note:</strong> It is not possible to manipulate the values from a workitem by script. But you can 
 modify values of the activity map object. These changes will be reflected back to the current ActivityEntity which can be used for further processing.
 
The next example shows how to change a single value of the ActivityEntity which  can be used for further processing:


	// set keymailenabled to 0
	activity.put('keymailenabled',['0']);
 
<strong>Note:</strong>All property names of the object WorkItem and activity are lower cased!
 
 
##How to control the process flow 
To control the process flow by the result of an evaluated business rule a script can set the following variables:
 
|Name      |Type       | Description                                   |
|----------|-----------|-----------------------------------------------| 
| followUp | integer   | Defines a followUp activity which will be called after the current activity.      |
| isValid  | boolean   | If false the current processing will stop  and a PluginException is thrown     |
| errorCode| String    | Set the errorCode of the PluginException if  isValid=false. The default errorCode is 'VALIDATION_ERROR'    |
|errorMessage | String | A String value or String array which contains error messages provided in the parameter property of the PluginException    |


If a script can not be evaluated by the scriptEngin a PluginExcpetion with the errorCode 'INVALID_SCRIPT' will be thrown.

<strong>Note:</strong> All names of the business rule variable are case sensitive!
 

###followUp 
If the script sets the variable 'followUp' an optional workflow event can be triggered. The value of the variable 'followUp' defines the next workflow event which will be processed by the WorkflowManager after the current activity is completed.  This is the recommended way to trigger different activities depending on a business rule. See the following example:
 
	 var followUp=null;
	 if (budget[0]>10000)
	    followUp=20;
	 else
	    followUp=30;

This rule evaluates the property 'budget' from the current workItem. If the value is higher than 10.000 EUR the activity with the ID=20 will be processed in the next step.  If the budget is lower than 10.000 EUR the activity with the ID=30 will be processed.

###nextTask
Another way to influence the flow of the process is to set the variable 'nextTask'. This variable can be used to change the outgoing flow of a workflow event, which is normally defined by the workflow model. 
 
	 var nextTask=null;
	 if (budget[0]>10000)
	    nextTask=2100;
	 else
	    nextTask=3100; 

###isValid 
The variable 'isValid' can be used in a business rule to validate the data of a workitem. If the script set the variable 'isValid' to 'false' then the plugin throws a PluginExcpetion. The Plugin evaluates the optional variables 'errorCode' and errorMessage. If these variables are set by the Script then the Plugin 
 will update the errorCode and the params[] of the PluginException. If no errorCode is set then the errorCode of the PluginException will default to 'VALIDATION_ERROR'. See the following example:
 
	 var isValid=true;
	 if (budget[0]>10000)
	    isValid=false;

This example tests if the 'budget' from the current workItem is higher than 10.000 EUR. In this  case the activity is declared as 'invalid' for the current process and a PluginException will be thrown. To provide an application with a specific errorCode and errorMessage the corresponding variables can be set ba the script:
 
	 if (budget[0]>10000) {
	    var errorCode='MY_ERROR';
	    var errorMessage='Somehing go wrong!';
	 }
 
If no errorCode is defined the default errorCode 'VALIDATION_ERROR' will be set. It is also possible to provide more then one message in the PluginException:
 
	  if (budget[0]>10000) {
	 	 var errorMessage = new Array();
	  	 errorMessage[0]='Somehing go wrong!';
		 errorMessage[1]='Somehingelse go wrong!';
	  }
  
This script defines an array with two error messages. The error messages will be part of  the param[] property of the PluginException. See the [section exception handling](./exception_handling.html)

##How to access the ActivityEntity 

The Script Language and the script are defined in reserved properties of the current ActivityEntity:

  * txtBusinessRule - defines the business rule to be evaluated by the Rule Plugin.
  * txtBusinessRuleEngine - defines the script language used by the Plugin.
 
The rule plugin uses the Script Engine provided by the JDK. So business rule can be written in any script language supported by the runtime environment.  

