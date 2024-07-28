# The ResultPlugin

The Imixs-Workflow Result Plugin evaluates optional processing information assigned to an Imixs BPMN Event.
The processing information can be defined by additional item values or custom XML tags. 

_Plugin Class Name:_

    org.imixs.workflow.engine.plugins.ResultPlugin

The Result plugin should run in the first place, so that processing information can be provided to following plugins.
With the [Imixs-BPMN modeler](../../modelling/activities.html) the Workflow Result can be edited in the 'Workflow' section of an BPMN Event node. 


<img src="../../images/modelling/bpmn_screen_20.png"/>


## The Item Tag

By defining a single XML tag  `<item>` a new item value can be set or an existing item value can be changed. 

```xml
<item name="[NAME]">[VALUE]</item> 
```

See the following examples:

```xml
<item name="name">Some Title</item> 
<item name="account" type="integer">500</item> 
```

This example will update two properties of the current workitem. The property 'name' is set to the value 'Some Title'. The property 'account' will be set to the integer value 500. 
 
<strong>Note:</strong> It is not possible to update any internal [workflow data items](../../quickstart/workitem.html) beginning with an  '$' character. 

The processing information to be evaluated is defined in the bpmn-extension property 'txtActivityResult' of an Imixs BPMN Event. 

### The Item Value 

The Item value can also be evaluated by the tag `<itemValue>` to assign a value form any existing item. See the following example which computes the value of the property 'responsible' to the value of the existing item 'team':

```xml
<item name="responsible"><itemvalue>team</itemvalue></item> 
```

### Item Value Type

With the optional attribute 'type' the item value type can be specified. The following types are supported:

* boolean - results in type Boolean
* integer - results in type Integer
* double - results in type Double
* date - results in type java.util.Date
 
Example Boolean: 

```xml
<item name="isManager" type="boolean">true</item>
```	

This will store the boolean value true into the item 'isManager'.  


Example Integer: 

```xml
<item name="count" type="integer">55</item>
```

This will store the integer value 55 into the item 'count'.  


Example Date: 
```xml
<item name="reminder" type="date" format="yyyy-MM-dd" >2018-12-31</item>
```

This will store a date value into the item 'reminder'. The attribute "format" must match the given string value.

The date value can also be taken from an existing itemvalue: 

```xml
<item name="reminder" type="date" ><itemvalue>$modified</itemvalue></item>
```  

In this case formating is not necessary. 




### Clear an Item Value
It is also possible to clear an existing item value:

```xml
<item name="txtName"></item> 
```

This will reset the item 'txtname' with an empty string. It is not possible to set a null value. 


### Custom Attributes
A item definition can also contain optional custom attributes. These attributes can be used for extended plugin computation: 


```xml
<item name="[NAME]" [OPTION]="[OPTION-VALUE]">[VALUE]</item> 
```

The following example will create an additional field _'comment.ignore'_ with the value _'true'_:
 
 ```xml 
<item name="comment" ignore="true">some data</item> 
 ```

## XML Configurations

A processing configuration can also be provided in a custom xml tag. A custom XML tag must provide the atribute `name`, simmilar to the `<item>` tag.  
See the follwoing example XML configuration:

```xml 
<imixs-sepa name="CONFIG">
	<textblock>....</textblock>
	<template>....</template>
</imixs-sepa>
```

This custom configuration with the tag name `imixs-sepa` and the name `CONFIG` defines two config items 'textblock' and 'template'. 

A result definition can have multiple custom XML tags with the same name attribute. The `WorkflowService` provides methods to evaluate these configuration during a procssing lifec-cycle phase. 


## Evaluate the Workflow Result

The `WorkflowService` provides the methods to evaluate the processing information in various ways. The method  `evalWorkflowResult()` returns a `ItemCollection` with all item names and values. In the following example shows how to get the evaluated item values form the current workflow result.
	

```java 
ItemCollection result = getWorkflowService().evalWorkflowResult(event, documentContext, true);
Assert.assertNotNull(result);
Assert.assertTrue(result.hasItem("comment"));
Assert.assertEquals("some data", result.getItemValueString("comment"));
Assert.assertEquals("true", result.getItemValueString("comment.ignore"));
```

### Evaluate XML Configurations

A custom configuration can have any XML tag with the mandatory attribute `name`.

To extract this configuration use the method `evalWorkflowResultXML` from the `WorkflowService`. See the following code example extracting the tags of the xml configuration above:

```java 
List<ItemCollection> sepaConfigList = workflowService.evalWorkflowResultXML(
	event, "imixs-sepa", "CONFIG", sepaExport, false);
if (sepaConfigList == null || sepaConfigList.size() == 0) {
	// no configuration found!
	throw new PluginException(OpenAIAPIAdapter.class.getSimpleName(), ERROR_CONFIG,
			"Missing or invalid imixs-sepa definition in Event!");
}

ItemCollection sepaConf = sepaConfigList.get(0);
String template = sepaConf.getItemValueString("template");
...
```

### Evaluate custom XML Objects

A workflow result can also contain more complex xml structures. In that case you can embed the XML object in a root element with at least the `name` attribute:

```xml 
<my-config name="bookstore">
	<bookstore name="LIBRARY">
		<book category="COOKING">  
			<title lang="en">Everyday Italian</title>  
			<author>Giada De Laurentiis</author>  
			<year>2005</year>  
		</book> 
		<book category="CHILDREN">  
			<title lang="en">Harry Potter</title>  
			<author>J K. Rowling</author>  
			<year>2005</year>   
		</book>  	
	</bookstore>
</my-config>	
```

You can extract the XML content and parse the the structure manually. 

```java 
 ItemCollection configItemCol = evalWorkflowResult(event, "my-config", documentContext, false);
 List<String> xmlDefinitions = configItemCol.getItemValueList("bookstore", String.class);
 // parse xml
 ...
```

**Note:** A xml configuration may occur more then once in a workflow result definition.
