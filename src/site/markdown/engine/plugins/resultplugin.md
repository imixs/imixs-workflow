#The ResultPlugin

The Imixs-Workflow Result Plug-In is used to provide model based processing information. These information is provided by additional property values assigned to an Imixs BPMN Event. The plug-in evaluates these processing information which can be used by different modules. 

_Plugin Class Name:_

    org.imixs.workflow.plugins.ResultPlugin

The Result Plug-in should run in the first place, so that processing information can be provided to following plug-ins.
With the [Imixs-BPMN modeler](../../modelling/activities.html) the Workflow Result can be edited in the 'Workflow' section of an event. 


<img src="../../images/modelling/bpmn_screen_20.png"/>


##The Item Tag

The processing information to be evaluated is stored in the extension property 'txtActivityResult' of an Imixs BPMN Event. The values are stored in the following XML format: 
 
    <item name="[NAME]">[VALUE]</item> 

See the following examples:
 
	<item name="txtName">Some Title</item> 
	<item name="numAccount" type="integer">500</item> 

This example will update two properties of the current workitem. The property 'txtName' is set to the value 'Some Title'. The property 'numAccount' will be set to the integer value 500. 
 
<strong>Note:</strong> It is not possible to update any internal [workflow data items](../../quickstart/workitem.html) beginning with an  '$' character. 

### The Item Value 

The Item value can also be evaluated by the tag 'itemValue' to assign a value form any existing item. See the following example which computes the value of the property 'responsible' to the value of the existing item 'namCreator':
 
    <item name="responsible"><itemvalue>namCreator</itemvalue></item> 

With the optional attribute 'type' the item value type can be specified. The following types are supported:

* boolean - results in type Boolean
* integer - results in type Integer


### Item Attributes
A item definition can also contain optional attributes : 

    <item name="[NAME]" [OPTION]="[OPTION-VALUE]">[VALUE]</item> 

See the following example:
 
	<item name="comment" ignore="true">some data</item> 

The Result Plug-in provides the static method evaluateWorkflowResult() returning a ItemCollection with all item names and there attributes. In the example the ItemCollection 'result' will contain a item with the name 'comment' storing the value 'some data' and also a item value with the name 'comment.ignore' storing the value 'true'	
	
	
	ItemCollection result = ResultPlugin.evaluateWorkflowResult(activityEntity, workitem);
	Assert.assertNotNull(result);
	Assert.assertTrue(result.hasItem("comment"));
	Assert.assertEquals("some data", result.getItemValueString("comment"));
	Assert.assertEquals("true", result.getItemValueString("comment.ignore"));

