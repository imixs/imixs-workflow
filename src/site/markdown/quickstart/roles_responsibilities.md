# Modeling Roles & Responsibilities 

Modeling a business process with Imixs-Workflow does not only give you way to describe the tasks within your business process, but also a fine grained way to model roles and responsibilities. In difference to static business applications, where you define technical roles for different tasks and functions, in Imixs-Workflow this can be solved by the model. 

However the Imixs-Workflow engine defines a set of [technical security roles](../engine/acl.html) a user must be assigned, to participant in a business process. But beyond these security roles, no technical roles are required in an Imixs-Workflow application. This is because Imixs-Workflow can dynamically assign roles and responsibilities based on the model definition.

## Technical Roles vs. Organizational Groups

Lets assume your application defines a publishing service for a bog. Each blog article need to be reviewed before release. 

<img src="../images/modelling/roles_00.png" />

    
At first glance, it seems logical to define the role 'Reviewer' assigned to all users involved in the review task. As a result, the technical role 'Reviewer' is tied to your application. However, in a human-focused workflow engine, the definition of technical roles should be avoided. This is because the workflow engine itself assigns responsibilities to a task depending on the processing life-cycle. So instead of defining technical roles, your application should only group users in organizational units. 

In difference to a technical role, an organizational group defines a team or a department within your organization, responsible for different tasks. 
The advantage in defining  organizational groups instead of technical roles is, that you can reuse these groups for different tasks within one workflow or even reuse the group for different workflows. 

Lets assume that the review task is done by the 'Marketing' team. If you have defined a group 'marketing' you can map the group to your model and assign it to the review task. As a result the marketing team becomes responsible for the review without the need defining the technical role.


<img src="../images/modelling/roles_01.png" />

In the ACL of the task 'Review' you can set the owner and write access to the corresponding marketing group. In this way the Imixs-Workflow engine assigns the responsibility to the marketing team in a dynamic way.
 
<img src="../images/modelling/roles_04.png" width="700px" />



## How define a Organizational Group

Defining organizational groups can be done in different ways depending on your application design. You can either define your groups by the model, a custom configuration within your application, or you can map a technical role to each organization unit - like in an LDAP directory. 


### Defining Groups by the Model

Defining a organizational group by the model can be done in the section 'workflow' of the [process properties](../modelling/main_editor.html#The_Imixs_Workflow_Properties):

<img src="../images/modelling/roles_02.png" width="700px" />


In this case, the corresponding UserIDs are placed in curly brackets.

	Marketing | {john, tom}

The userIds will be automatically assigned to the write and owner properties of a process instance when processing the task 'Review'.

#### Pros

Defining organizational groups within the model, there is no extra configuration or security context needed. It's a fast and easy way to model responsibilities. 

#### Cons

Each time the organization change, the model must be updated to map the new group members. 


### Defining Custom Groups

Instead of defining groups in the model, you can define your organizational groups by a custom configuration within your application. With this approach you map the group members to a custom attribute of a process instance:

<img src="../images/modelling/roles_03.png" width="700px" />

In this case Imixs-Workflow expects the group members of the group 'Marketing' in the property 'group.marketing'. Your application is now responsible to resolve the corresponding group at runtime and transfer the members of the group into the custom attribute.
This can be done by a custom Plug-In:


	public class GroupPlugin implements Plugin {
	    public ItemCollection run(ItemCollection workitem, ItemCollection event) throws Exception {
	      	List<String> marketingGroup;
	      	// read group members from configuration
			....
			workitem.setItemValue("group.marketing",marketingGroup);
			return workitem;
	    }
	}

#### Pros

The advantage of this approach is that you can define your groups independent from the model or an external security context. UserIDs are directly mapped into an attribute of the process instance. This allows additional functionality like to display the responsible users of a task within your application. 

#### Cons

The custom group management must be implemented by your application. 


### Mapping Technical Roles

Another way to define organizational groups is to map existing groups into your application. This can be done either by defining existing security roles or by resolving groups at runtime. 

By defining an external String Resource with the name 'ACCESS\_ROLES' assigned to the _DocumentService_ EJB, you can provide a list of application specific Security-Roles. The Imixs-Workflow Engine will test these roles by calling _ctx.isCallerInRole(testRole)_ for each user. In this way you can map a security role to a organizational group. Find more details [here](https://www.imixs.org/doc/deployment/security.html#How_to_Define_Individual_Access_Role).

Another way to map existing groups is implementing the [CDI UserGroupEvent](https://www.imixs.org/doc/engine/documentservice.html#Inject_Custom_User_Groups).
In this case your application returns a List of external user groups the current user is assigned to. The Imixs-Workflow engine will fire this event each time the ACL of a process instance need to be recalculated. See the following example:

	public void onUserGroupEvent(@Observes UserGroupEvent userGroupEvent) {
	    List<String> customGroups = new ArrayList<String>();
	    if (ctx.isCallerInRole("marketing")) {
	        customGroups.add("group.marketing");
	    }
	    userGroupEvent.setGroups(customGroups);
	}

This approach allows you also to map existing groups - e.g. from a LDAP directory - into your application. 

In case of a technical role mapping, the group names can be added directly to the ACL definition of the model. A actor mapping is not needed here:

<img src="../images/modelling/roles_05.png" width="700px" />

However the role mapping can also be combined with a Group-Plug-In as shown for the section "Defining Custom Groups". Similar to the _UserGroupEvent_ a custom plug-in can resolve existing external group members and assign the list to a custom attribute of your process instance. 

#### Pros

The advantage of a technical role mapping is that you can reuse existing group definitions from an external system, such as an LDAP directory.


#### Cons

The only disadvantage here is that your application depends on the external group definition which can not be changed without side effects if the groups have different functionalities. 
