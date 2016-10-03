#How to Search Documents
As the Imixs-Workflow engine is bundled with the [Lucene Search Technology](https://lucene.apache.org/) documents can be query by a lucene search term. 
A search string can be specified either as a simple string or as an combination of multiple parentheses expressions.

    the imixs workflow egine
    
expressions may contain parentheses expressions
 
	(imixs-workflow) (engine)
	
expressions can also include wildcards

    (imixs*)
    
This serach term will serach for all documents containing a phrase starting with 'imxis'


##Query Items

The Imixs-Worklfow engine takes also a list of [workflow data items](../quickstart/workitem.html) into the index. These items can be used to serach for documents of a specific type or category:


    (type:"workitem") ($processid:1200)
     
To specify more complex queries a search term can boolean expressions like 'AND' or 'OR'

	(type:"workitem" OR $processid:1200)

This serach term will search for all documents from the type 'workitem' or the $processid 1200.

See the [Lucene Query Syntax](https://lucene.apache.org/core/3_6_0/queryparsersyntax.html) for further information.
 
      