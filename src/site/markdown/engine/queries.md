# The Search Index

The Imixs-Workflow engine provides a **Full-Text-Search** based on the [Lucene Search Technology](https://lucene.apache.org/). The search index is managed by the [DocumentService](./documentservice.html), which offers various search methods.
You can search documents by search terms like "cat" or "dog*" and you can also define complex queries to select documents with specific search criteria.


The Search-Index stores also parts of a document, which dramatically speeds up the document access. The following section gives you an overview how a search term can be assembled from the very powerful [Lucene Query Syntax](https://lucene.apache.org/core/7_5_0/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#package.description).

## How to Search Documents

A search term can be specified either as a simple string or as an combination of multiple parentheses expressions.

    the imixs workflow egine
    
expressions may contain parentheses expressions:
 
	(imixs-workflow) (engine)

### Wildcard Expression	

A search term can also include wildcards.

The following example will search for all documents containing a phrase starting with 'imxis'

    (imixs*)

**Note:** A wildcard search term is not case sensitive.  Unlike other types of Lucene queries, Wildcard, Prefix, and Fuzzy queries are not passed through the Analyzer, which is the component that performs operations such as stemming and lowercasing. The reason for skipping the Analyzer is that if you were searching for "dogs*" you would not want "dogs" first stemmed to "dog", since that would then match "dog*", which is not the intended query. These queries should be lower-cased before!


### Fuzzy Searches
Lucene supports fuzzy searches based on the Levenshtein Distance, or Edit Distance algorithm. To do a fuzzy search use the tilde, "~", symbol at the end of a Single word Term. For example to search for a term similar in spelling to "roam" use the fuzzy search:

	roam~

This search will find terms like "foam" and "roams".


### Boolean Operators

Queries can be combined with boolean operators like 'AND' or 'OR':

	(imixs) AND ( (workflow) OR (engine) )


The **NOT** operator excludes documents that contain the term after NOT. The symbol ! can be used in place of the word NOT.

To search for documents that contain "imixs" but not "lucene" use the query:

	"imixs" NOT "lucene"

**Note:** The NOT operator cannot be used with just one term. In this case the search will return no results.


## Query Items

The Imixs-Workflow engine adds a list of [workflow data items](../quickstart/workitem.html#Workflow_data) into the index, which allow the targeted query of documents. These items can be used to search for documents of a specific type or item value. 

The following example searches only documents form the type 'workitem' and with the $taskid of '1200':

    (type:"workitem") ($tasnkid:1200)
     
To specify more complex queries a search term can boolean expressions like 'AND' or 'OR'. 

The next example searches for all documents from the type 'workitem' and the $taskid with a value of 1100 or 1200.

	(type:"workitem") AND ($taskid:1100 OR $taskid:1200)


### Query Value Ranges

Range Queries allow to search for documents whose item values are between the lower and upper bound specified by the Range Query *[xxx TO yyy] *:


	$taskid:[1100 TO 1200]

Range Queries can be inclusive or exclusive of the upper and lower bounds. Sorting is done lexicographically.

### Query Date Ranges

To search for a date range, the date has to be formated inot

	YYYYMMDD
	
The following example searches for documents created between 20020101 and 20030101, inclusive. 

	$created:[20020101 TO 20030101]
	

In java you can format a Date object into the lucene syntax with a Forater object:

	SimpleDateFormat luceneFormat = new SimpleDateFormat("yyyyMMdd");
	String query = "($created:["+luceneFormat.format(fromDate)+ " TO " + luceneFormat.format(toDate) + "])";


See the [Lucene Query Syntax](https://lucene.apache.org/core/7_5_0/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#package.description) for further information.


## How to Setup an Index Schema

In the section [Index Schema](luceneservice.html) you will find detailed information how to setup and customize the search index by defining an Index Schema and a Search Engine.
