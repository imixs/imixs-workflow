# The Index Schema

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

The following list of items are so called 'index-items'. These items are indexed by the _LuceneService_ without using an analyzer. S

	$modelversion, $taskid, $workitemid, $uniqueidref, type, $writeaccess, $modified,$created, $creator, 
	$editor, $lasteditor, $workflowgroup, $workflowstatus, txtname, owner, txtworkitemref, $uniqueidsource, 
	$uniqueidversions, $lasttask,$lastevent, $lasteventdate

The values can be used for searching and sorting. Find more information how to query a result set in the section [Full-Text-Search](queries.html).  
  
### Store Documents

The Imixs Search Index provides a feature to store item values into the search index. This is called a 'Document Stub'. Document Stubs can be fetched much faster from the search index like the full document. The items to be stored in the index can be defined by the property 
	
	index.fields.store
	
To fetch the Document Stub form the serach index use the method findStubs:

	result = documentService.findStubs(query, 100, 0, '$created' , true);

You can later load the full document by the $uniqueid which is part of the document stub.   



## Custom Configuration
A custom configuration for the _Index Schma_ can be provided in the file _imixs.properties_. The following example shows an example:
 
	##############################
	# Imixs-Workflow Index Schema 
	##############################
	# Fields to be added into the searchindex
	index.fields=_searchstring,_subject,_name,_email
	index.fields.analyze=
	index.fields.noanalyze=datDate,email, date, util, numsequencenumber, username

 
### index.fields
The property 'index.fields' defines a comma separated list of fields whose values will be indexed. The content of these fields will be stored into the lucene default search field named 'content'. The text will be analyzed  with the lucene standard analyzer.
You can search by any search phrase

    (imixs workflow engine)
 
 
### index.fields.analyze
The property 'index.fields.analyze' defines a comma separated list of fields whose will be added as keyword  fields into the lucene index. You can search the content by naming the field. 


    ($workflowsummary:approved*) AND (imixs workflow engine)
    

The content of this fields will be analyzed by the  lucene standard analyzer. 
 
### index.fields.noanalyze
The property 'index.fields.noanalyze' defines a comma separated list of fields which will be added as keyword  fields into the lucene index. The content of this fields will not be analyzed. So a exact phrase search is possible here. 

    ($workflowstatus:"approved for payment")

These fields can also be used to sort a search result.
 
### index.fields.store
The property 'index.fields.store' defines a comma separated list of fields to be stored into the search index. This kind of data can be requested by the method findStubs(). This finder method is faster but did not load the whole document. 
  
### index.defaultOperator
The defaultOperator sets the boolean operator of the QueryParser. In default mode (AND\_OPERATOR) terms without any modifiers are considered optional: for example the search phrase

    (capital of France)
    
is equal to 

    (capital) AND (of) AND (France)
    
    
In OR\_OPERATOR mode terms are considered to be in conjunction: the above mentioned query is parsed as _capital OR of OR France_
 
 
## How to Initialize the Lucene Index

The lucene index is automatically written into the Index Directory by the Imixs-Workflow engine.
However, it is also possible to rebuild the lucene index manually. For example, this can be the case after a Database restore. To force a rebuild of the index it is sufficient to delete the Index Directory and restart the application. The Imixs-Workflow engine will recognize the missing index and start an [AdminP Job](adminp.html) called "REBUILD_LUCENE_INDEX" immediately. It can take some minutes depending on the amount of documents until the index is created. 

### Configuration of the JOB\_REBUILD\_INDEX

The REBUILD\_INDEX Job_ can be configured by the following imixs.properties settings:

 * lucene.rebuild.block_size - defines the internal block size of documents be read in one block by the indexer job. The default value is 500. It can be reduced in case the VM heap size is to small.
  
 * lucene.rebuild.time_out - defines the internal time out in seconds after the AdminP job will be suspended form 60 seconds. The defautl time out is set to 120 seconds. It can be reduced in case the VM heap size is to small.

### Initialize the Index with the Imixs-Admin Client 
The [Imixs-Admin Client](../administration.html) provides a web interface to build a new index. 

### Initialize the Index via a Rest API call

It is also possible to trigger the build process for the lucene index via the Rest API. See the following example with a curl command:

	curl --user admin:adminpassword -H "Content-Type: text/xml" -d \
       '<document xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xs="http://www.w3.org/2001/XMLSchema"> \
           <item name="type"><value xsi:type="xs:string">adminp</value></item> \
           <item name="job"><value xsi:type="xs:string">JOB_REBUILD_INDEX</value></item> \
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
 
 
 
 
 
## The Search Engine

The Search Index can be controlled by different search engines and can be extended by custom implementations. The Imixs-Workflow engine provides two search engines:

### Apache Lucene Core

[Apache Lucene Core](https://lucene.apache.org/core/) is the default search engine for Imixs-Workflow. The index is created in the local file system and is very fast. It's the best choice for a workflow instance in a single-server environment. 
To activate the Lucene Core Engine you just have to add the following dependency:

	<dependency>
		<groupId>org.imixs.workflow</groupId>
		<artifactId>imixs-workflow-lucene</artifactId>
		<version>${org.imixs.workflow.version}</version>
	</dependency>
	
The index location in the local file system can be defined by the imixs.property '_lucence.indexDir_'. The property can set in the imixs.properies file together with the index schema configuration:


	# Fields to be added into the searchindex
	index.fields=txtsearchstring,txtSubject,txtname,txtEmail,txtWorkflowAbstract,txtWorkflowSummary
	index.fields.analyze=
	index.fields.noanalyze=datDate,txtWorkflowGroup,txtemail, datdate, datfrom, datto, numsequencenumber, txtUsername
	# Search Index Direcotry 
	lucence.indexDir=${imixs-office.IndexDir}


The default location will be the directory 'imixs-workflow-index'
	

### Apache Solr

[Apache Solr](https://lucene.apache.org/solr/) is a highly reliable, scalable and fault tolerant search engine. 
Solr is providing distributed indexing and replication and can be deployed in a microservice architecture as well in large distributed cloud environments.

You need to setup a Solr server environment to use the Solr Search Engine. To activate the Solr Engine you need to add the following dependency:

	<dependency>
		<groupId>org.imixs.workflow</groupId>
		<artifactId>imixs-workflow-solr</artifactId>
		<version>${org.imixs.workflow.version}</version>
	</dependency>
      
Apache Solr provides a lot of flexibility in configuration and offers additional features like translation or suggestion. In this way you can control the Imixs Search Index in a more fine grained way. 


There is a set of optional properties which can be set to access the solr instance: 

 - _solr.api_ - solr api endpoint (default host=http://solr:8983);
 - _solr.core_ - the solr core (default 'imixs-workflow')
 - _solr.configset_ - an optional solr config set (default set is '_default')
 - _solr.user_ - optional user id to login 
 - _solr.password_ - optional user password to login
 
The optional parameter can be set together with the  the index schema configuration:


	# Fields to be added into the searchindex
	index.fields=txtsearchstring,txtSubject,txtname,txtEmail,txtWorkflowAbstract,txtWorkflowSummary
	index.fields.analyze=
	index.fields.noanalyze=datDate,txtWorkflowGroup,txtemail, datdate, datfrom, datto, numsequencenumber, txtUsername	
	# Solr configuration
	solr.api=http://my-solr-host:8983




### Build Your Custom Search Engine

You can also extend Imixs-Workflow Search engines or you can implement your own custom search engine. Therefor you need to implement the following interfaces and deploy your engine together with the Imixs-Workflow engine:

 - **SearchService** - provides method to search an index
 - **UpdateService** - is responsible to build the search index. 
  