# Rule Plugin 
The Imixs RulePlugin is used to evaluate business rules during the processing life cycle of an event. 
The Plugin enables various business validations and automations:

- Data validation (e.g. checking required fields, value ranges)
- Complex calculations (e.g. budget approval limits, discounts)
- Dynamic workflow routing based on business conditions
- Automated data transformations and enrichment
- Integration with external systems through API calls



_Plugin Class Name:_

    org.imixs.workflow.plugins.RulePlugin

A business rule can be written in any script language supported by the JVM. E.g 'JavaScript or 'Groovy'. 
The [Imixs-BPMN modeler](../../modelling/activities.html) provides an easy way to enter business rules for an event. 

<img src="../../images/modelling/bpmn_screen_24.png"/>

The the section [RuleEngine](../../core/ruleengine.html) for more information about the Imixs Rule Engine. 




## Script Objects

From a script any basic item value contained by the workitem or the event definition can be accessed through local objects provided by the RulePlugin. This offers various ways to develop custom business rules.

A script may also create a result-object to provide custom processing information or update the current workitem. The following objects variables are provided by the RulePlugin:

 * `workitem` - provides all item values of the process instance
 * `event` - provides all item values of the BPMN event triggered in the processing life cycle
 * `result` - an optional result object to be returned by the script, containing updated or new item values. 
 
The following section explains how the script objects can be used by a script.

### The 'workitem' Object

The script object `workitem` provides all item values form the current process instance. The item values are stored in a Java interface reflecting the [ImixsItemCollection](../../core/itemcollection.html). 

The following example shows how to validate if the item 'products' has any value:

```javaScript
var result={isValid: true}; 
if (!workitem.getItemValueString('products')=='') {
   result={ 
     isValid: false,
     errorMessage: 'Please enter a product!'
   }
}	 
```

You can also test a value list for specific entries: 


```javaScript
var result={isValid: true}; 
if (!workitem.getItemValue('products').includes('car')) {
   result={ 
     isValid: false,
     errorMessage: 'Please add at least the product car!'
   }
} 
```

### Script Validation 

The variable `isValid` can be used in a business rule to validate the data of a workitem. If the script set the variable `isValid` to `false` then the plugin will throw a `PluginException`. In addition the Plugin evaluates the optional variables `errorCode` and `errorMessage`. If these variables are set by the Script then the Plugin  will update the errorCode and the `params[]` of the PluginException. If no errorCode is set then the errorCode of the PluginException will default to 'VALIDATION_ERROR'. See the following example:
 
```javaScript
var result={};
if (workitem.getItemValueDouble('budget')>10000) {
   result.isValid=false;
}   
```
This example tests if the 'budget' from the current workItem is higher than 10000 . In this  case the activity is declared as 'invalid' for the current process and a PluginException will be thrown. 

To provide an application with a specific errorCode and errorMessage the corresponding variables can be set by the script:
 
```javaScript
var result={};
if (workitem.getItemValueDouble('budget')>10000) {
  result.isValid=false;
  result.errorCode='MY_ERROR';
  result.errorMessage='Somehing go wrong!';
} 
```

If no errorCode is defined the default errorCode `VALIDATION_ERROR` will be set. It is also possible to provide more then one message in the PluginException:

```javaScript 
if (workitem.getItemValueDouble('budget')>10000) {
  var result={
    'isValid':false,
    'errorMessage':[
      'Something go wrong!',
      'Something else go wrong!'
    ]
  }; 
}
```


This script defines the result as an JSON object containing an array with two error messages. The error messages will be part of  the param[] property of the PluginException. See the [section exception handling](./exception_handling.html)


### The 'result' Object

A business rule always expect the `result` object storing the data for the workflow engine to be processed. You can provide an optional result object with new or updated item values. The item values stored in the result object will be applied to the current process instance.

See the following example which creates a new single item name 'some_item' with the value 'Hello World'.

```javaScript
var result={
  isValid=true,
  some_item: 'Hello World'
}; 
```


A property of the result object may also contain multi-values which have to be stored in an array. See the following example:

```javaScript
var result={ 
  isValid: true,
  multi_item: [ 'Hello World', 'Hello Imixs']
};
```


### The 'event' Object

The script object `event` provides all item values of the BPMN event object currently processed by the workflow engine. The item values can be accessed the same way as from the `workitem` object:

```javaScript
// test the email flag
if (event.getItemValueString('keymailenabled' == '0') {
	// email enabled....
}
```

It is possible to change an item value of the event object to influence the behavior of the event.

```javaScript
// set mail-reply-to item...
var result={}; 
event.nammailreplytouser='test@me.com';
....
```

All changes made to the event object will be reflected back to the current processing life-cycle, so that new values can be used for further processing.
 


## Definition of a Business Rule 

The business rule and the used script language are defined in the following BPMN event properties:

  * _txtBusinessRule_ - defines the business rule to be evaluated by the Rule Plugin.
  * _txtBusinessRuleEngine_ - defines the script language used by the Plugin.
 
The rule plugin supports any Script Engine provided by the JVM. 


## GraalVM 

Since version 6.x Imixs-Workflow supports the GraalVM and its ScriptEngine. The GraalVM integration offers several advantages for business rule implementation:

- Support for multiple languages including JavaScript, Python, Ruby, and R
- High performance through JIT compilation
- Polyglot programming - combine different languages within one rule
- Access to GraalVM's extensive ecosystem of libraries
- Native support for JSON processing
- Better debugging capabilities compared to traditional script engines

For JavaScript rules, the GraalVM provides full ECMAScript 2022 support including modern features like optional chaining, nullish coalescing, and array methods.
