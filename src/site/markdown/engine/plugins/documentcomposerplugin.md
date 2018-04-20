# The DocumentComposerPlugin

The DocumentComposerPlugin provides functionality to create html output stored in an item of the current process instance.

    org.imixs.workflow.engine.plugins.DocumentComposerPlugin
 
The Plugin uses XSL transformation based on a XSL templage proivded by an BPMN DataObject assigned to the target task.

<img src="../../images/modelling/example_12.png"/> 
 
The configuration is provided by an BPMN Event result:

	<item name="document-composer" data-object="[TEMPLATE-NAME]">[OUTPUT-ITEM]</item>

 
 The following example generates a new item 'html_invoice' with the outcome of the XSL transformation based on a XLS template in the dataobject 'invoice'
 

	<item name="document-composer" data-object="invoice">html_invoice</item>
 