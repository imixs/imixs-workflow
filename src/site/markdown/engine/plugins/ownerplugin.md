# Owner Plugin 

The OwnerPlugin is responsible for the ownership associated with a workitem.
The plug-in applies a ownership list of userIDs and roles to the current process instance stored in the item '$owner'.


_Plugin Class Name:_

    org.imixs.workflow.engine.plugins.OwnerPlugin
    
The Ownership settings can be configured for a BPMN Event or Task element using the Imixs-BPMN modeling tool:


<img src="../../images/modelling/bpmn_screen_21.png"/>  


This plug-in class is mandatory in case of the tasklist-method 'getWorkListByOwner'. The result of this list depends on the $owner item computed by this plug-in. 
