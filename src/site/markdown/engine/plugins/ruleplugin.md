#Rule Plugin 
The Imixs RulePlugin is used to evaluate business rules during the processing life cycle of an event. 

_Plugin Class Name:_

    org.imixs.workflow.plugins.RulePlugin

A business rule can be written in any script language supported by the JVM. E.g 'JavaScript or 'Groovy'. 
The [Imixs-BPMN modeler](../../modelling/activities.html) provides an easy way to enter business rules for an event. 

<img src="../images/modelling/bpmn_screen_24.png"/>

From the script language any basic item value contained by the processed workitem or the event definition can be accessed by local variables. This offers many possibilities to develop various business rules.
A script can update properties of the event and create a result object to provide custom processing information and update the current workitem.

To access a single item value from a script, the RulePlugin provide the following objects variables 

 * workitem - provides all item values of the process instance
 * activity - provides item values of the event triggered in the processing life cycle
 * result - a result object returned by the script containing updated or new item values. 
 
The following section explains how the objects can be access from a script.

## Variable 'workitem'

The variable 'workitem' provides the item values form the process instance to be processed by the workflow engine. The item values are stored in a JSON object, holding the item name (key) and an object array of item values (value). See the following example showing how to get the item value of an item with the name 'txtname':

	 // test the first value of the workitem attribute 'txtname'
	 if (workitem.txtname == 'car') {
	     ....
	 }

<strong>Note: </strong>The item name must always be written in lower case.

A item can hold a single value or a value list which is stored in an array. The following example shows how to access the value list of the item 'namteam'

	 // test the number of item values stored in the attribute 'namteam'
	 if (workitem.namteam && workitem.namteam.length>0) {
	 	...
	 }
	 // iterate over all item values of the attribute 'namteam'
	 for (int i=0;i<workitem.namteam.length; i++) {
	 	value=workitem.namteam[i];
	 	....
	 }

## Variable 'activity'

The variable 'activity' provides the item values of the event object currently processed by the workflow engine. The item values can be accessed the same way as from the 'workitem' object:

	 // test the email flag
	 if (activity.keymailenabled == '0') {
	     // email enabled....
	 }

It is possible to change an item value of the activity object to influence the behavior of the event.


	// set keymailenabled to 1
	activity.keymailenabled='0';
	....


All changes made to the activity object will be reflected back to the current workflow event which can be used for further processing.
 
<strong>Note: </strong>The item name must always be written in lower case.
 

## Variable 'result'

A business rule may also provide a result object with new or updated item values to be applied to the current process instance.
See the following example which creats a new single item name 'some_item' with the value 'Hello World'.


	var result={}; 
	result.some_item='Hello World';
 
The result variable have to be defined as an JSON object.
A property of the result object may also contain multi values which have to stored in an array. See the following example:


	var result={}; 
	result.some_item=[]; 
	result.some_item[0]='Hello World'; 
	result.some_item[1]='Hello Imixs';"

The result object can also be constructed with a JSON string:


	var result={
				'single_item':'Hello World', 
				'multi_item':[
								'Hello World',
								'Hello Imixs'
							  ]
				}; 
 
 
 
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

##Definition of a Business Rule 

The business rule and the used script language are defined in the following event properties:

  * txtBusinessRule - defines the business rule to be evaluated by the Rule Plugin.
  * txtBusinessRuleEngine - defines the script language used by the Plugin.
 
The rule plugin supports any Script Engine provided by the JVM. 
