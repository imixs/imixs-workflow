# Taxonomy Plugin 

The Imixs Taxonomy plugin can be used to collect taxonomy data at specific stages of a business process.


    org.imixs.workflow.plugins.TaxonomyPlugin
    
The plugin can be used to collect data about the processing time of a certain phase within the life cycle of a process instance. Several individual taxonomy definitions can be configured in the workflow result of a BPMN event:


###Example:

    <taxonomy name="approval">
    	<type>start</type>
    	<anonymized>true</anonymized>
    </taxonomy>
    

defines a start point named 'approval'

    <taxonomy name="approval">
    	<type>stop</type>
    	<anonymized>true</anonymized>
    </taxonomy>
    

defines the end point named 'approval'. 

## Taxonomy Data 

Defining a taxonomy within the model will result in the following workitem properties based on the name of the taxonomy:

  * taxonomy.name : contains a list of all collected taxonomy names (e.g. 'approval')
  * taxonomy.[NAME].start : contains the start time points in a list (latest entry on top!)
  * taxonomy.[NAME].end : contains the end time points (list)
  * taxonomy.[NAME].duration: contains the total time in seconds
  * taxonomy.[NAME].start.by: contains the $owner list at the first start
  * taxonomy.[NAME].end.by: contains the $editor list at the last stop
  
**Note:** [NAME] will be replaced with the name of the taxonomy definition. It is possible  
 to define several measuring points in one process model.
  
The following diagram illustrates two taxonomy definitions in a ticket-workflow:


<img src="../../images/analysisplugin.png"/>  


The first taxonomy definition 'P1' will measure the total processing time for a ticket. The second taxonomy definition 'P2' will measure the duration of the processing time (in seconds) for accepting the ticket.




