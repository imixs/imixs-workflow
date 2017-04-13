#The LuceneService 
This _LuceneService_ is used by the Imixs-Workflow engine to provide a [Lucene Search Index](https://lucene.apache.org/). The Imixs-Workflow engine is currently supporting Lucene version 6.2.1. For details how to search for documents see the [DocumentService section](./documentservice.html).

## The Index default configuration
The lucene index is written by default into the folder 

	/imixs-workflow-index/
	
inside the runtime environment of the application server.

If no further configuration exists, the content of the document items  "txtworkflowsummary" and "txtworkflowabstract" will be analyzed and added into the serach index. 

In addition the following list of items in a document are indexed by the _LuceneService_ without using an analyser. 

	"$modelversion", "$processid","$workitemid", "$uniqueidref", "type", "$writeaccess", "$modified", "$created", "namcreator",	"txtworkflowgroup", "txtname", "namowner", "txtworkitemref");

These items can be used in a search term. 

    (type:"workitem") and ($modelversion:"1.0.0")

##Custom Configuration
The custom configuration of the _LuceneService_ can be provided in the file _imixs.properties_. The following example shows custom configuration for the _LuceneService_:
 
	##############################
	# Imixs Lucene Service 
	##############################
	
	# Search Index Direcotry 
	lucence.indexDir=${imixs-office.IndexDir}
	# Fields to be added into the searchindex
	lucence.fulltextFieldList=txtsearchstring,txtSubject,txtname,txtEmail,txtWorkflowAbstract,txtWorkflowSummary
	lucence.indexFieldListAnalyze=
	lucence.indexFieldListNoAnalyze=type,$UniqueIDRef,$created,$modified,$ModelVersion,namCreator,$ProcessID,datDate,txtWorkflowGroup,txtemail, datdate, datfrom, datto, numsequencenumber, txtUsername,


###IndexDir
 
This is the directory on the servers file system the lucene index will be created. Make sure that 
the server has sufficient write access for this location. Using Glassfish Server the example above will  create a directory named 'my-index' into the location GLASSFISH_INSTALL/domains/domain1/config/
 
###FulltextFieldList
The property 'lucene.fulltextFieldList' defines a comma separated list of fields which will be indexed by the LucenePlugin. The content of these fields will be stored into the lucene field name 'content'. The values will be analyzed  with the lucene standard analyzer.
 
###IndexFieldListAnalyze
The property 'lucene.indexFieldListAnalyze' defines a comma separated list of fields which will be added as keyword  fields into the lucene index. The content of this fields will be analyzed by the  lucene standard analyzer. 
 
###IndexFieldListNoAnalyze
The property 'lucene.indexFieldListNoAnalyze' defines a comma separated list of fields which will be added as keyword  fields into the lucene index. The content of this fields will not be analyzed. So a exact phrase search is possible here.
 
 
## Keyword Search

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
 