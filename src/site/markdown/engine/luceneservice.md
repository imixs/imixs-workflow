# Lucene Configuration 

The [Full-Text-Search](queries.html) in Imixs-Workflow is part of the [DocumentService](./documentservice.html) and based on the [Lucene Search Technology](https://lucene.apache.org/) (version 7.5). 
The behavior of the search index can be configured and adapted in various ways. The following section gives you an overview how to setup the Lucene Full-Text-Search.

## The Search Index
The lucene search index is written by default into the folder 

	/imixs-workflow-index/
	
The location is typical the home or installation directory in the runtime environment of your application server.

In the default configuration the content of the following [workflow items](../quickstart/workitem.html) will be full-text indexed:

 * $workflowsummary
 * $workflowabstract

The content of these items is defined by the [task element](../modelling/process.html#Workflow_Properties) in your workflow model.

The following list of items are so called 'index-items'. THese items are indexed by the _LuceneService_ without using an analyser. 

	$modelversion, $taskid, $workitemid, $uniqueidref, type, $writeaccess, $modified,$created, $creator, 
	$editor, $lasteditor, $workflowgroup, $workflowstatus, txtname, namowner, txtworkitemref, $uniqueidsource, 
	$uniqueidversions, $lasttask,$lastevent, $lasteventdate

_Index-Items_ can be used in a search term. 

    (type:"workitem") and ($modelversion:"1.0.0")
    
You will find more information how to query a result set in the section [Full-Text-Search](queries.html).  
  

## Custom Configuration
A custom configuration for the _Lucene Search Index_ can be provided in the file _imixs.properties_. The following example shows an example:
 
	##############################
	# Imixs Lucene Service 
	##############################
	
	# Search Index Direcotry 
	lucence.indexDir=${imixs-office.IndexDir}
	# Fields to be added into the searchindex
	lucence.fulltextFieldList=txtsearchstring,txtSubject,txtname,txtEmail,txtWorkflowAbstract,txtWorkflowSummary
	lucence.indexFieldListAnalyze=
	lucence.indexFieldListNoAnalyze=datDate,txtWorkflowGroup,txtemail, datdate, datfrom, datto, numsequencenumber, txtUsername
	lucene.defaultOperator=AND
	lucene.splitOnWhitespace=true	


### IndexDir
 
This is the directory on the servers file system the lucene index will be created. Make sure that 
the server has sufficient write access for this location. The root directory is typical the home or installation directory in the runtime environment of your application server.
 
### FulltextFieldList
The property 'lucene.fulltextFieldList' defines a comma separated list of fields which will be indexed by the LucenePlugin. The content of these fields will be stored into the lucene field name 'content'. The values will be analyzed  with the lucene standard analyzer.
 
### IndexFieldListAnalyze
The property 'lucene.indexFieldListAnalyze' defines a comma separated list of fields which will be added as keyword  fields into the lucene index. The content of this fields will be analyzed by the  lucene standard analyzer. 
 
### IndexFieldListNoAnalyze
The property 'lucene.indexFieldListNoAnalyze' defines a comma separated list of fields which will be added as keyword  fields into the lucene index. The content of this fields will not be analyzed. So a exact phrase search is possible here.
 
### defaultOperator
The defaultOperator sets the boolean operator of the QueryParser. In default mode (AND\_OPERATOR) terms without any modifiers are considered optional: for example _capital of France_ is equal to _capital AND of AND France_.
In OR\_OPERATOR mode terms are considered to be in conjunction: the above mentioned query is parsed as _capital OR of OR France_
 
### splitOnWhitespace
Whether query text should be split on whitespace prior to analysis. Default is true. For example _cat dog_ will be treated as _cat AND dog_
 
 
 
## How to Initialize the Lucene Index

The lucene index is automatically written into the Index Directory by the Imixs-Workflow engine.
However, it is also possible to rebuild the lucene index manually. For example, this can be the case after a Database restore. To force a rebuild of the index it is sufficient to delete the Index Directory and restart the application. The Imixs-Workflow engine will recognize the missing index and start an [AdminP Job](adminp.html) called "REBUILD_LUCENE_INDEX" immediately. It can take some minutes depending on the amount of documents until the index is created. 

### Configuration of the REBUILD_LUCENE_INDEX-Job

The _REBUILD_LUCENE_INDEX Job_ can be configured by the following imixs.properties settings:

 * lucene.rebuild.block_size - defines the internal block size of documents be read in one block by the indexer job. The default value is 500. It can be reduced in case the VM heap size is to small.
  
 * lucene.rebuild.time_out - defines the internal time out in seconds after the AdminP job will be suspended form 60 seconds. The defautl time out is set to 120 seconds. It can be reduced in case the VM heap size is to small.

### Initialize the Index with the Imixs-Admin Client 
The [Imixs-Admin Client](../administration.html) provides a web interface to build a new index. 

### Initialize the Index via a Rest API call

It is also possible to trigger the build process for the lucene index via the Rest API. See the following example with a curl command:

	curl --user admin:adminpassword -H "Content-Type: text/xml" -d \
       '<document xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xs="http://www.w3.org/2001/XMLSchema"> \
           <item name="type"><value xsi:type="xs:string">adminp</value></item> \
           <item name="job"><value xsi:type="xs:string">REBUILD_LUCENE_INDEX</value></item> \
           <item name="numblocksize"><value xsi:type="xs:int">1000</value></item> \
           <item name="numindex"><value xsi:type="xs:int">0</value></item> \
           <item name="numinterval"><value xsi:type="xs:int">1</value></item> \
        </document>' \
    http://localhost:8080/api/adminp/jobs

 
This call starts the Job 'REBUILD\_LUCENE\_INDEX' on the workflow application located at

    http://localhost:8080/api
  
The user must have manager-access to be allowed to trigger this build job. 



## The Lucene KeywordAnalyzer 

The Imixs LuceneSearchService uses the Lucene KeywordAnalyzer to parse a given search term. This means that a search phrase is taken as is. For example:

	rs/82550/201618 

will search exactly for the keyword 'rs/82550/201618'. Also the phrase

	europe/berlin

will start a search for the keyword 'europe/berlin'. This sometimes leads to an unexpected search result, because the Imixs LuceneUpdateService is using the 'ClassicAnalyzer' per default to create the luncene index. And this analyzer will splitt 'europe/berlin' into ''europe' and 'berlin'. If you need to seach for an exact keyoword you need to add the corresponding field into the imixs.property value 'lucence.indexFieldListNoAnalyze'. This will create a document field with the exact keyword independent form the content and it will not be split up by the ClassicAnalyzer. But in this case you need to specify the document field also in your query. See the following example:

	(_country:europe\/berlin)

This lucene search query will search for the keyword 'europe/berlin' in the document field '_country'.

Note: All other content of a workitem will typically be stored into the document field 'content' and analyzed by the ClassicAnalyer which will - as explained before - split up the keyword into two separate words.

### Normalze a Search Term.

The Imixs LuceneSearchService provides a static method 'normalizeSearchTerm'. This method can be used to split up a search term in separate phrases in the same way as the term would be split up by the Lucene ClassicAnalyzer. So for example the search term:

	europe/berlin

will be modified by this method into:

	europe berlin

The normalizeSearchTerm method takes care about article numbers supported by the ClassicAnalyzer. So the following search term

	europe/berlin rs/82550/201618

will result in

	europe berlin rs/82550/201618

and will return all workitems containing the keywords 'europe', 'berlin' and 'rs/82550/201618'
 
 
See the section [Query Syntax](queries.html) for more details about the lucene search syntax.  
 
 