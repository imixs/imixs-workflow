# The ApproverPlugin

The ApproverPlugin can be used to manage an approval process, involving multiple users.

    org.imixs.workflow.engine.plugins.ApproverPlugin

The list of approvers must be defined by an item stored in the current process instance. The approval process can be started from an event with the following workflow result:
 
Example:
 
    <item name='approvedby'>ReviewTeam</item>
 
This result definition will start a new approval process based on the source item 'ReviewTeam'. The source item is holding the users involved in the approver procedure. The plugin creates the following additional items to monitor the approver process:
 
 * [SOURCEITEMNAME]$Approvers   -  contains all users who still have to participate in the approval process
 * [SOURCEITEMNAME]$ApprovedBy  -  contains all users who have already completed the approval process

Within a BPMN model conditions can added to complete an approval process after all users have approved:


<img src="../../images/approverplugin.png"/> 


See the following condition example to test if all users of the ReviewTeam have approved:

	(workitem.reviewteam$approvers.length===0)

You can use these rules to validate also a minimum number of approvers needed for the approver process. For example according to an four-eye principle:

	(workitem.reviewteam$approvers.length>=2)

In this case a minimum of 2 users need to approve the task. 


## Refresh the Approver List

If the source item is updated during the approving process, the plugin will automatically add new users if these new Users are not yet listed in the 'approvedBy' item

You can disable the auto update with the attribute refresh='false'.


    <item name="approvedby" refresh="false">ReviewTeam</item>



## Reset the Approval Process


If the attribute 'reset' is set to true, the list [SOURCEITEMNAME]$Approvers
will be reseted and the item [SOURCEITEMNAME]$ApprovedBy will be cleared. This can be usefull in situations where the approving process need to be restarted. 


    <item name="approvedby" reset="true">ReviewTeam</item>

    
    