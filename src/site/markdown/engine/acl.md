#The Access Control List (ACL)
The Imixs-Workflow Engine supports a multiple-level security model that offers a great space of flexibility while controlling the access to all parts of a workitem  within the Imixs JEE workflow system. The highest security level is achieved by means of  access roles. This is a kind of ACL (access control list) for the workitems managed through the  Imixs workflow system.

You can define this individual roles to precisely control who will have access  to certain workitems and which maximum privileges should be assigned to each user.  For instance, a user can be authorized for only reading workitems while another one is allowed to create and process them.
See also the Section [Deployment Security](../deployment/security.html). 
 
##The Imixs-Workflow Access Roles 

The following section describes the concept how to restrict the access to workitems controlled by the Imixs-Workflow engine. The access privileges can be controlled by the deployment descriptors. See details about the deployment descriptor [here](../deployment/security.html).

The security concept of the Imixs-Workflow Engine defines 5 roles :

  * org.imixs.ACCESSLEVEL.NOACCESS
  * org.imixs.ACCESSLEVEL.READACCESS
  * org.imixs.ACCESSLEVEL.AUTHORACCESS
  * org.imixs.ACCESSLEVEL.EDITORACCESS
  * org.imixs.ACCESSLEVEL.MANAGERACCESS

 Each role allows the user a different privilege to interact with the Imixs-Workflow engine.

  * MANAGERACCESS: 
      Users who are assigned to this AccessLevel, are authorized to read and write all workitems inside the workflow system. The AccessLevels having been set to a single workitem are to be ignored.
  * EDITORACCESS:
      Users who are assigned to this AccessLevel, are authorized to create and process all workitems for which they have read privileges or which do not have a restricted read access assigned.
  * AUTHORACCESS:
      Users who are assigned to this AccessLevel, are only authorized to process workitems in which they are entered as authors and for which they have read privileges or which do not have a restricted read access assigned.
  * READACCESS:
      Users who are assigned to this AccessLevel, are not allowed to create or process workitems. They are only authorized to read workitems for which they have read privileges or which do not have a restricted read access assigned.
  * NOACCESS:
      Users who are assigned to this AccessLevel, are not authorized to read or write workitems independent of the read and write privileges of a workitem.


It is important to take care about that a user must have at least read access to a workitem  if he wants to process a workitem. The following table shows the mapping of different roles and shows what effective access a user is granted to a workitem. 

  * public workitem means that the workitem have no read or write restriction (no or empty read/write access fields)
  * personal workitem means that the workitem is protected by a read/ or write access field and the user is personal recorded into that fields by its name, role or group.
  * protected workitem means the the workitem is protected by a read/ or write access field and the user is not recorded to that fields

 ACL Table:

|AccessLevel  |read<br/>public <br/>workitems    |read <br/>protected<br/>workitems    |read <br /> personal<br/>workitems    |write<br/>public <br/>workitems    |write <br/>protected<br/>workitems    |write <br /> personal<br/>workitems    |       
|--------------|:-------:|:-------:|:-------:|:-------:|:-------:|:-------:|
|NOACCESS      | no      | no      | no      | no      |  no     | no      |
|READACCESS    | yes     | yes     | no      | no      |  no     | no      |
|AUTHORACCESS  | yes     | yes     | no      | no      |  yes    | no      |
|EDITORACCESS  | yes     | yes     | no      | yes     |  yes    | yes     |
|MANAGERACCESS | yes     | yes     | yes     | yes     |  yes    | yes     |


 
<strong>Note:</strong> The following roles are general access roles of the Imixs JEE Workflow System. 

  * NOACCESS means that the user has in general no access to the system.
  * READACCESS means that the user can not create or modify any workitem
  * MANAGERACCESS allows the user to read and write any workitem.

To restrict the access to workitems of the Imixs JEE   workflow system follow these rules:

 1. Assign the users the "ACCESSLEVEL.READER" role to grant read access.
 2. Assign the users the "ACCESSLEVEL.AUTHOR" role to grant write access.
 3. To grant explicit read privileges to a workitem define reader privileges.
 4. To grant explicit write privileges to a workitem define author privileges.
             
## How to Restrict the Read Access    
Read access privileges assigned to a workitem define all users who have read privileges  for a workitem inside the workflow system if they do not have the MANAGERACCESS. Assigning read access privileges to a workitem, users are not able to see it and so the  user can not access a workitem by the getter methods in the workflow service interface.  The read access privilege is independent from the later discussed write access privilege. So you can restrict the access privileges of users who have no manager access privileges  preventing them from reading documents if they are not listed in a reader field. Users who  have author or editor access privileges can read a document if the following conditions are met:

  * They are listed in a reader field of the workitem
  * They are listed in a author field of the workitem without any read restriction
  * The workitem does not contain restrictions in the reader access nor the write access. 

## How to Restrict Write Access   
Write access privileges assigned to a workitem define the write access for all users who  have author privileges (AUTHORACCESS) inside the workflow system. Those users can process  and remove a workitem through the methods provided by the workflow service. Even workitems the user has created can not be modified if the user is not explicit listed in the access  privileges. Users who are not authorized to read a workitem  will never be able to edit a workitem. This even applies if you specify write access privileges for  these users.   Access by users who have at least editor access privileges for the workflow system is not  restricted by an author field. The write access provided for a workitem only affects users  with author access privileges for the hole workflow system. 

Users who have author access privileges can edit a workitem if the following conditions are  met:

  * They are listed in a author and reader field of the workitem 
  * They are listed in a author field of the workitem without any read restriction

 