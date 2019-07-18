# The ResultPlugin

The Imixs-Workflow Result Plugin is used to provide model based processing information. These information is provided by additional property values assigned to an Imixs BPMN Event. The plugin evaluates these processing information which can be used by different modules. 

_Plugin Class Name:_

    org.imixs.workflow.engine.plugins.ResultPlugin

The Result plugin should run in the first place, so that processing information can be provided to following plugins.
With the [Imixs-BPMN modeler](../../modelling/activities.html) the Workflow Result can be edited in the 'Workflow' section of an event. 


<img src="../../images/modelling/bpmn_screen_20.png"/>


## The Item Tag

The ResultPlugin evaluates the tag _'item'_. This tag can be used to set or change a item value of the current workitem. The  tag _'item'_ has the following XML format: 
 
    <item name="[NAME]">[VALUE]</item> 

See the following examples:
 
	<item name="txtName">Some Title</item> 
	<item name="numAccount" type="integer">500</item> 

This example will update two properties of the current workitem. The property 'txtName' is set to the value 'Some Title'. The property 'numAccount' will be set to the integer value 500. 
 
<strong>Note:</strong> It is not possible to update any internal [workflow data items](../../quickstart/workitem.html) beginning with an  '$' character. 

The processing information to be evaluated is stored in the bpmn-extension property 'txtActivityResult' of an Imixs BPMN Event. 

### The Item Value 

The Item value can also be evaluated by the tag 'itemValue' to assign a value form any existing item. See the following example which computes the value of the property 'responsible' to the value of the existing item 'namCreator':
 
    <item name="responsible"><itemvalue>namCreator</itemvalue></item> 


### Item Value Type
With the optional attribute 'type' the item value type can be specified. The following types are supported:

* boolean - results in type Boolean
* integer - results in type Integer
* date - results in type java.util.Date
 
Example Boolean: 
 
	<item name="isManager" type="boolean">true</item>
	
This will store the boolean value true into the item 'isManager'.  



Example Integer: 
 
	<item name="count" type="integer">55</item>
	
This will store the integer value 55 into the item 'count'.  


Example Date: 
 
	<item name="reminder" type="date" format="yyyy-MM-dd" >2018-12-31</item>
	
This will store a date value into the item 'reminder'. The attribute "format" must match the given string value.

The date value can also be taken from an existing itemvalue: 

	<item name="reminder" type="date" ><itemvalue>$modified</itemvalue></item>
  
In this case formating is not necessary. 




### Clear an Item Value
It is also possible to clear an existing item value:

	<item name="txtName"></item> 

This will reset the item 'txtname' with an empty string. It is not possible to set a null value. 


### Custom Attributes
A item definition can also contain optional custom attributes. These attributes can be used for extended plugin computation: 

    <item name="[NAME]" [OPTION]="[OPTION-VALUE]">[VALUE]</item> 


The following example will create an additional field _'comment.ignore'_ with the value _'true'_:
 
	<item name="comment" ignore="true">some data</item> 


### Eval Workflow Result

The WorkflowService Result plugin provides the method evalWorkflowResult() returning a ItemCollection with all item names and there attributes. In the following example shows how to get the evaluated item values form the current workflow result.
	
	
	ItemCollection result = getWorkflowService().evalWorkflowResult(event, documentContext, true);
	Assert.assertNotNull(result);
	Assert.assertTrue(result.hasItem("comment"));
	Assert.assertEquals("some data", result.getItemValueString("comment"));
	Assert.assertEquals("true", result.getItemValueString("comment.ignore"));

