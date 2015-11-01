#ResultPlugin
The Result Plugi-In can be used to provide additional property values during a process step.

    org.imixs.workflow.plugins.ResultPlugin

The Plug-In evaluates the result message provided by the Activity property
 'txtActivityResult'. The value will be parsed for the xml tag 'item'. 
 
    <item name="fieldname">value</item> 

The provided value will be assigned to the named property. See the following examples:
 
	<item name="txtName">Some Title</item> 
	<item name="numAccount" type="integer">500</item> 
	<item name="type">workitemarchive</item> 

This example will set the property 'txtName' to the value 'Some Title' and change the value 
 of the field 'numAccount' to 500. The value will be of type 'integer'. The last will change the type of the workitem to 'workitemarchive'.
 
<strong>Note:</strong> It is not possible to update any workflow processing properties beginning with an  '$' character in the item name. 

 The new value can also be evaluated by the tag 'itemValue' to assign a value form any existing field. See the following example which assignes the value of 'namCreator' to the item 'responsible':
 
    <item name="responsible"><itemvalue>namCreator</itemvalue></item> 

 If the result message can not be evaluated, it will be stored into the attribute 
 "txtworkflowresultmessage". 
 
 