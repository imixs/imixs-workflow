#The Access Control List (ACL)
The Imixs-Workflow engine supports a multi-level security model for a fine-grained access control on workitems.
The access control list (ACL) can be assigned individually to each workitem. 

With application specific roles it is possible to precisely control who will have read and write access to certain workitems. For instance, a group of users can be authorized for only reading workitems while other ones are allowed to create and process workitems.

The access is closely coupled to the security concept of Java EE and provides many ways to authorize users to access the data managed by the Imixs-Workflow engine. See also the section [Deployment >> Security](../deployment/security.html). 
 
## Access Levels 
The following section describes the core concept of how to grant or restrict access for a user to interact with the Imixs-Workflow engine. The general access level is defined by 5 different access roles: 

  * org.imixs.ACCESSLEVEL.NOACCESS
  * org.imixs.ACCESSLEVEL.READACCESS
  * org.imixs.ACCESSLEVEL.AUTHORACCESS
  * org.imixs.ACCESSLEVEL.EDITORACCESS
  * org.imixs.ACCESSLEVEL.MANAGERACCESS

Each of these roles allows the user to interact in a different way with workitems controlled by the Imixs-Workflow engine:

### MANAGERACCESS: 
Users who are assigned to this access level, are authorized to read and write all workitems. AccessLevels assigned to a single workitem are ignored.

### EDITORACCESS:
Users who are assigned to this access level, are authorized to create and process all workitems for which they have read privileges or which do not have a restricted read access.

### AUTHORACCESS:
Users assigned to this access level are only authorized to process workitems in which they are registered as authors and for which they have read permissions.

### READACCESS:
Users who are assigned to this access level, are not allowed to create or process workitems. They are only authorized to read workitems for which they have read permissions or which do not have any read restriction.

### NOACCESS:
Users who are assigned to this access level or have none of these roles, are not authorized to read or write workitems independent of the read and write privileges of a workitem.


## The Access Level Matrix

It is important to note that a user must have at least read access to a workitem if he or she wants to process this workitem.
The following section shows the mapping between different roles that a user owns and his effective access to a protected workitem.

Within this matrix the following workitem types are distinguished: 

### Public Workitem:
A public workitem means that the workitem has no read or write restriction
  
### Personal Workitem:
A personal workitem means that the workitem is protected by a read/ or write access that the user is assigned to by its name, role or group.

### Protected Workitem:
A protected workitem means that the workitem is protected by a read/ or write access that the user is not assigned to by its name, role or group.


|AccessLevel  |read<br/>public <br/>workitems    |read <br/>personal<br/>workitems    |read <br /> protected<br/>workitems    |write<br/>public <br/>workitems    |write <br/>personal<br/>workitems    |write <br /> protected<br/>workitems    |       
|--------------|:-------:|:-------:|:-------:|:-------:|:-------:|:-------:|
|NOACCESS      | no      | no      | no      | no      |  no     | no      |
|READACCESS    | yes     | yes     | no      | no      |  no     | no      |
|AUTHORACCESS  | yes     | yes     | no      | no      |  yes    | no      |
|EDITORACCESS  | yes     | yes     | no      | yes     |  yes    | yes     |
|MANAGERACCESS | yes     | yes     | yes     | yes     |  yes    | yes     |


The read and write access for a workitem can be defined by the BPMN model with the [ACL Properties of the Imixs-BPMN modeler](../modelling/process.html#ACL_Properties) and the [Access Plugin](./plugins/accessplugin.html)


