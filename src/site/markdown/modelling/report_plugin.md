# Workflow Reports
The Imixs-Report plug-In is an Eclipse Plug-In to define business reports for the Imixs-Workflow engine. A report aggregates and transforms a collection of WorkItems into a custom output format (e.g. XML, JSON, HTML, PDF, ...). Once a report was created with the Imixs-Report Plug-In, it can be uploaded to a Imixs-Workflow instance via the [REST Service API](../restapi/reportservice.html).

## Installation
The Imixs-Report plug-In is installed the same way as any other Eclipse plug-in. To install the Imixs-Report plug-in select from the Eclipse Workbench main menu *Help -> Install New Software*. Enter the following update site URL:

    http://www.imixs.org/org.imixs.eclipse.updatesite


## Creating a Report Definition 
Reports are stored in a file with the extension ".imixs-report" and can be added to any existing Eclipse project. To create a new report definition add a new file with the extension ".imixs-report" into a project of the Eclipse Workspace. 

Opening a report shows the Imixs-Report Editor. 

<img src="../images/modelling/report-01.png"  width="700"/>

The editor provides Input-Fields to define a Lucene search statement, defining the result set of the report and an optional Attribute List. 

The "XSL" section of the Imixs-Report Editor can be used to transform the result set of a report in any kind of output format using the "Extended Stylesheet Language".  

### Publish a Report
Once a report was created with the Eclipse Report Plugin it can be uploaded unisng the Imixs Rest API.

    curl --user admin:admin_password --request POST -H "Content-Type: application/xml" -Tmyreport.imixs-repport http://localhost:8080/workflow/report
    
Each report is identified uniquely by its report name which is equals to the file resource name.  If a report of the same name already exists in a Imixs-Workflow instance the report will be updated.


## Executing a Report
To execute a report the report can be triggered via the Imixs-Rest API given the report name:

    http://localhost:8080/api/report/REPOTNAME.html

When a report is executed, a result set of WorkItems based on the search statement and attribute definition will be generated. The result set of a report can be requested in different formats (e.g  HTML, XML, JSON). 

Find details about the Imixs-Report Rest Service API in the [section REST API](../restapi/reportservice.html).

To remove an existing report use the [Imixs-Admin Client](../administration.html). 

## The Report Query Statement
Each report defines at least a [Lucene Query](../engine/queries.html). The query selects the result set returned by a report. 

	 (type:"workitem") AND ($workflowgroup:"Ticket")

This example query will select all running workitems in the workflow group 'Ticket'. 

### Providing Parameters
The query statement of a report can contain dynamic parameters. These parameters can be provided through the [Imixs Report REST API](../restapi/reportservice.html). See the following example of a query defining a parameter named '1':
  
	 (type:"workitem") AND ($taskid:{1})

To provide the execution with a parameter, the parameter value is appended into the query string of the Rest API call:
 
    http://Host/WorkflowApp/report/reportfile.imixs-report?pagesize=100&1=5130
 
In this example the URL contains the parameter "1=5130" which will be inserted into the query statement during the report execution.


### Dynamic Date Values
Another feature provided by the Imixs-Report API are *dynamic date values* added into a search statement. A *dynamic date value* is an abstract description of date. The date is compute during execution time of the report. A dynamic date value is embraced by the 'date' tag:

    <DATE />
 
 The date tag supports the following optional attributes:
 
 

| Attribute      | Description                    | Example  |
|----------------|--------------------------------|-----------
| DAY_OF_MONTH   | set day of month               | <date DAY_OF_MONTH="1" /> (first day of month, use 'ACTUAL_MAXIMUM' to get last day of month
| MONTH          | set month                      | <date MONTH="1" /> (January)
| YEAR           | set year                       | <date YEAR="2016" />   
| ADD            | add offset (see Calendar.class)| <date ADD="MONTH,-1" /> subracts one month from the current year
 
See the following example selects all workitems form type 'invoice' from the last 6 months:


    (type:"workitem" OR type:"workitemarchive") 
		AND (txtworkflowgroup:"Invoice")
		AND ($created:[<date DAY_OF_MONTH="1" ADD="MONTH,-6" /> TO <date DAY_OF_MONTH="ACTUAL_MAXIMUM" />])

 


## The Attribute List

Each report definition can include an optional attribute list. The attribute list defines the elements to be included in the result during the report execution. 

<img src="../images/modelling/report-03.png"/>

The attribute list allows you to customize the report in various ways. Note, that WorkItems processed by the same workflow can consist of different item values. To ensure that all entities returned through a report issue the same content, defining an attribute list is mandatory.  
If no attribute list is defined, all item values of a workitem are included in the result set. 

The attribute list defines the following columns:

### Item
The first column defines the name of the item value to be returned by the report. 

#### Embedded Child Items
An item value can contain embedded workitems (called 'ChildItems'). A ChildItem can be selected in the attribute list by separating the ChildItem name from the item name with a '~'. For example:

    _childItems~amount
    
This will select the embedded item value 'amount' from the item with the name '_childItems'. 

### Label
The column label can be used to define an optional label which is used when the result is transformed into a table format (e.g HTML).
  
### Format
Each item value can be formated in different ways. For example a date value can be returned in a custom output format : 

    yy-dd-mm 

This example formats the a date value into ISO Date format. The optional attribute 'locale' can be used to format dates in a country specific way. 

    <format locale="DE">dd. mmm yyyy</format>

Find more details about how to format a date value in the [Java Date Format Patterns](http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html).  

The format definition can also be used to format the output of currency and number values. 

Examples: 

     <format>#,##0.00</format>
     <format locale="DE">#,###,##0.00 €</format>
     <format locale="en_UK">###,###.###</format>

Find more details about how to format a number in the [Java Formating Patterns](https://docs.oracle.com/javase/tutorial/i18n/format/decimalFormat.html). 

### Convert
Beside the option to format values it is also possible to convert the values into a specific data type. This can be useful in case
the object values are not from the same type which can be necessary for XSL transformation. 
To convert the item value the tag 'convert'  can be appended behind the item name: 

     xs:decimal
     xs:int

In case the value is not from the specified type (Double, Integer) the value will be converted into a double or integer object. In case the value can not be converted the value will be reset to 0. 

#### Custom Converters

It is also possible to adapt the item value by defining a [TextAdapter](https://www.imixs.org/doc/engine/adapttext.html). The Text Adapter is a feature of the Imixs-Workflow engine and can be used to convert item values in custom String values. 



This attribute definition will extend the JPQL result with all embedded Child Items stored in the attribute '_childItems' and containing the item 'amount'. 

## The XSL Transformation
To transform the result of a report definition into various output formats a XSL template can be added into each report.  

<img src="../images/modelling/report-02.png" />

The XSL template is a file provided in the Eclipse project workspace. For each Report supporting a XSL Template additional parameters can be defined

 * Content Type 
 * Encoding
 
These parameters are equals to the corresponding  parameters of the Imixs-REST Service interface. 

To execute the transformation of a result set by the defined XSL template the report can be requested with the '.imixs-report' extension from the [Report REST API](../restapi/reportservice.html):

    http://localhost:8080/api/report/REPOTNAME.imixs-report
 
 
## The Report Format

Internally a report is stored in a document with the type 'ReportEntity'. The following attributes are stored in report definition entity.


| Attribute      | Description                    | Example  |
|----------------|--------------------------------|-----------
| txtName        | Report name                    | my report
| txtQuery       | Lucene search term             | (type:"workitem") AND ($processid:1200)
| xsl            | Optional XSL Template          |   |
| xslresource    | Optional XSL File location     |   |
| contenttype    | Optional content type for XSL transformation   |  application/json
| encoding       | Optional encoding for XSL transformation     | ISO-8859-1
| attributes     | attribute list for result set  |  |




     