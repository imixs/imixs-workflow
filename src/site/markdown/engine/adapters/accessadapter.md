# The ParticipantAdapter

The ParticipantAdapter is responsible for the ACL settings and ownership associated with a workitem.
The generic adapter class  applies a access control list (ACL) to the current process instance. See also the [section security](../acl.html) for more details.  

 The adapter updates the WorkItem attributes:

* $ReadAccess 
* $WriteAccess
* $owner
* $participants.

The ACL and Ownership settings can be configured for a BPMN Event or Task element using the Imixs-BPMN modeling tool:


<img src="../../images/modelling/bpmn_screen_21.png"/>  

See also the section [ACL Properties](http://www.imixs.org/doc/modelling/activities.html#ACL_Properties) of the BPMN modeler. 

The following attributes defined in the model element are evaluated by the plugin:

 * keyupdateacl (Boolean): if false the ACL will not be changed
 
 * keyaddreadfields (Vector): a list of items of the current WorkItem to be applied to the read access
 
 * keyaddwritefields (Vector): a list of items of the current WorkItem to be applied to the write access
 
 * namaddreadaccess (Vector): Names & Groups to be applied to the read access
 
 * namaddwriteaccess (Vector): Names & Groups to be applied to the write access

The AccessPlugin  evaluates the ACL settings of the current Event element as also the  ACL settings of the next Task element. 
If the current Event Element provides a ACL setting, the next Task element will be ignored. 



### Dynamic ACLs

The dynamic ACL settings are used to compute the access list based on the item values stored in the current process instance. 
The ACL is computed dynamically based on the properties of the corresponding item values. 

For example, if a process instance holds a Item '\_team' with the values 'bob','frank' and 'anna', and the item name '\_team' is stored in the  
field 'keyaddwritefields' of the BPMN element, than only those users will be granted to a write access. 


### Static ACL 
It is also possible to define the ACL in a static way. The UserIDs and Role names have to match the  login name and role definitions of the workflow application. The values can be stored in the fields 'namaddreadaccess' and 'namaddwriteaccess' of the BPMN element.  
  