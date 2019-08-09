# Rule Plugin 
The Imixs RulePlugin is used to evaluate business rules during the processing life cycle of an event. 

_Plugin Class Name:_

    org.imixs.workflow.plugins.RulePlugin

A business rule can be written in any script language supported by the JVM. E.g 'JavaScript or 'Groovy'. 
The [Imixs-BPMN modeler](../../modelling/activities.html) provides an easy way to enter business rules for an event. 

<img src="../../images/modelling/bpmn_screen_24.png"/>

The the section [RuleEngine](../../core/ruleengine.html) for more information about the Imixs Rule Engine. 

## Script Objects

From a script any basic item value contained by the workitem or the event definition can be accessed through local objects provided by the RulePlugin. This offers various ways to develop custom business rules.

A script may also create a result-object to provide custom processing information or update the current workitem. The following objects variables are provided by the RulePlugin:

 * _workitem_ - provides all item values of the process instance
 * _event_ - provides all item values of the BPMN event triggered in the processing life cycle
 * _result_ - an optional result object to be returned by the script, containing updated or new item values. 
 
The following section explains how the script objects can be used by a script.

### The 'workitem' Object

The script object '_workitem_' provides all item values form the current process instance. The item values are stored in a JSON object, holding the item name (key) and an object array of item values (value). See the following example, showing how to get the first item value of an item with the name 'txtname':

	 // test the first value of the workitem attribute 'txtname'
	 if (workitem.txtname && workitem.txtname[0] === 'car') {
	     ....
	 }

<strong>Note: </strong>The item name must always be written in lower case.

A item value is stored in an array. The following example shows how to access all values of the item 'namteam'

	 // test the number of item values stored in the attribute 'namteam'
	 if (workitem.namteam && workitem.namteam.length>0) {
	 	...
	 }
	 // iterate over all item values of the attribute 'namteam'
	 for (int i=0;i<workitem.namteam.length; i++) {
	 	value=workitem.namteam[i];
	 	....
	 }
	 

### The 'event' Object

The script object 'event' provides all item values of the BPMN event object currently processed by the workflow engine. The item values can be accessed the same way as from the 'workitem' object:

	 // test the email flag
	 if (event.keymailenabled[0] == '0') {
	     // email enabled....
	 }

It is possible to change an item value of the event object to influence the behavior of the event.


	// set mail-reply-to item...
	var result={}; 
	event.nammailreplytouser='test@me.com';
	....


All changes made to the event object will be reflected back to the current processing life-cycle, so that new values can be used for further processing.
 
<strong>Note: </strong>The item name must always be written in lower case.
 

### The 'result' Object

A business rule can also provide an optional result object with new or updated item values. The item values stored in the result object will be applied to the current process instance.
See the following example which creates a new single item name 'some_item' with the value 'Hello World'.


	var result={}; 
	result.some_item='Hello World';
 
The result object is expected in an JSON object format.
A property of the result object may also contain multi-values which have to be stored in an array. See the following example:


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
 
 
 
## How to control the process flow 
To control the process flow by the result of an evaluated business rule a script can set the following result properties:
 
|Name      |Type        | Description                                   |
|----------|------------|-----------------------------------------------| 
| followUp | integer    | Defines a followUp event assigned to the next task, which will be called after the current event is completed.      |
| isValid  | boolean    | If false the current processing live-cycle will stop and a PluginException will be thrown.     |
| errorCode| String     | The errorCode of the PluginException if _isValid_ is set to 'false'. The default errorCode is 'VALIDATION_ERROR'    |
| errorMessage | String | A String value or String array which contains error messages provided to the PluginException    |


If a script can not be evaluated by the scriptEngin a PluginExcpetion with the errorCode 'INVALID_SCRIPT' will be thrown.

<strong>Note:</strong> All names of the business rule variable are case sensitive!
 

### result.followUp 
If the script sets the variable 'followUp' an optional workflow event can be triggered. The value of the variable 'followUp' defines the next workflow event which will be processed by the WorkflowManager after the current activity is completed.  This is the recommended way to trigger different activities depending on a business rule. See the following example:
 
	 var result={};
	 if (workitem.budget[0]>10000)
	    result.followUp=20;
	 else
	    result.followUp=30;

This rule evaluates the property 'budget' from the current workItem. If the value is higher than 10.000 EUR the activity with the ID=20 will be processed in the next step.  If the budget is lower than 10.000 EUR the activity with the ID=30 will be processed.

### result.isValid 
The variable 'isValid' can be used in a business rule to validate the data of a workitem. If the script set the variable 'isValid' to 'false' then the plugin throws a PluginExcpetion. The Plugin evaluates the optional variables 'errorCode' and errorMessage. If these variables are set by the Script then the Plugin 
 will update the errorCode and the params[] of the PluginException. If no errorCode is set then the errorCode of the PluginException will default to 'VALIDATION_ERROR'. See the following example:
 
	 var result={};
	 if (workitem.budget[0]>10000) 
	    result.isValid=false;

This example tests if the 'budget' from the current workItem is higher than 10.000 EUR. In this  case the activity is declared as 'invalid' for the current process and a PluginException will be thrown. To provide an application with a specific errorCode and errorMessage the corresponding variables can be set ba the script:
 
    var result={};
	 if (workitem.budget[0]>10000) {
	    result.isValid=false;
	    result.errorCode='MY_ERROR';
	    result.errorMessage='Somehing go wrong!';
	 }
 
If no errorCode is defined the default errorCode 'VALIDATION_ERROR' will be set. It is also possible to provide more then one message in the PluginException:
 
	  if (workitem.budget[0]>10000) {
	     // create a JSON result object
	     var result={
				'isValid':false,
				'errorMessage':[
								'Somehing go wrong!',
								'Somehingelse go wrong!'
							  ]
				}; 
	  }
  
This script defines the result as an JSON object containing an array with two error messages. The error messages will be part of  the param[] property of the PluginException. See the [section exception handling](./exception_handling.html)

## Definition of a Business Rule 

The business rule and the used script language are defined in the following BPMN event properties:

  * _txtBusinessRule_ - defines the business rule to be evaluated by the Rule Plugin.
  * _txtBusinessRuleEngine_ - defines the script language used by the Plugin.
 
The rule plugin supports any Script Engine provided by the JVM. 


## JDK-7 Support 

With JDK-8 the new Script Engine 'Nashorn' was introduced. 
Nashorn treats java.util.Map objects as JSON objects which allows to tread the Map keys as "properties". 
So an item value of the script variables 'workitem' and 'activity' can be accessed as properties by its item name. 

	var name=workitem.txtname[0];

So as long as business rule is evaluated by Nashorn, the script engine will specially link the Map properties provided by the Imixs-Workflow engine and you can access the values by properties.  

In case of using JDK-7 you need to use the getter method to access values in an equal way. See the following example:

	var name=workitem.get('txtname')[0];

<strong>Note:</strong> In case you are running Imixs-Workflow in JDK-7 you need to use the get-method instead of direct access to properties by the item name.

Find further details details about Nashorn [here](https://wiki.openjdk.java.net/display/Nashorn/Nashorn+extensions#Nashornextensions-SpecialtreatmentofobjectsofspecificJavaclasses). 
