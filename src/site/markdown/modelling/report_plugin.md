#Workflow Reports
The Imixs-Report plug-In is a Eclipse Plug-In used to define business reports for the Imixs-Workflow system. A report aggregates and transforms a collection of WorkItems into a custom output format (e.g. XML, JSON, HTML, PDF, ...). Once a report was created with the Imixs-Report Plug-In, it can be uploaded to a Imixs-Workflow instance via the [REST Service API](../restapi/reportservice.html).

##Installation
The Imixs-Report plug-In is installed the same way as any other Eclipse plug-in. To install the Imixs-Report plug-in select from the Eclipse Workbench main menu *Help -> Install New Software*. Enter the following update site URL:

    http://www.imixs.org/org.imixs.eclipse.updatesite


##Creating a Report Definition 
Reports are stored in a file with the extension ".ixr" and can be added to any existing Eclipse project. To create a new report definition add a new file with the extension ".ixr" into a project of your Eclipse Workspace. 

Opening a report shows the Imixs-Report Editor. The report editor is split into a "Overview" section and a "XSL" section. 

<img src="../images/modelling/report-01.png"  width="700"/>

The "Overview" section provides Input-Fields to define a JPQL statement defining the result set of the report and an optional Attribute List. 

The "XSL" section of the Imixs-Report Editor can be used to transform the result set of a report in any kind of output format using the "Extended Stylesheet Language".  

Once a report was created it can be uploaded form the Eclipse IDE into a running Imixs-Workflow instance. The Rest Service endpoint URI form the Imixs-Report Service can be entered into the section 'Web Service location': 

    http://localhost:8080/workflow/rest-service/report/
    
Each report is identified uniquely by its report name which is equals to the file resource name.  If a report of the same name already exists in a Imixs-Workflow instance the report will be updated.


##Executing a Report
To execute a report the report can be triggered via the Imixs-Rest API given the report name:

    http://localhost:8080/workflow/rest-service/report/REPOTNAME.html

When a report is executed, a result set of WorkItems based on the JPQL statement defined by the report will be generated.  The result set of a report can be requested in different formats. To request a HTML representation of a report the report name can be extended with the sufix ".html". Optional the result set can be requested as XML (.xml) or JSON (.json) format. 

Find details about the Imixs-Report Rest Service API in the [section REST API](../restapi/reportservice.html).

To remove an existing report use the [Imixs-Admin Client](../administration.html). 

## The JPQL Definition
Each report defines at least a JQPL statement to query a set of WorkItems during execution of the report. 
The JPQL Statement must be valid for the workflow instance the report will be executed. 
You can find details about how to define a JPQL statement to query workitems in the [section Workflow Engine](../engine/queries.html)

###Providing Parameters
The JPQL statement of a report can contain dynamic JPQL parameters. These parameters can be provided through the [Imixs Report REST API](../restapi/reportservice.html). See the following example of JPQL statement defining a parameter named '1':
  
	 SELECT workitem FROM Entity AS workitem
	 JOIN workitem.integerItems AS p
	  WHERE workitem.type = 'workitem' 
	  AND p.itemName = '$processid' 
	  AND p.itemValue = ?1

To provide the Report during execution with the expected parameter ?1 the parameter can be appended into the query string of a Rest API call:
 
    http://Host/WorkflowApp/report/reportfile.ixr&1=5130
 
In this example the URL contains the parameter "?1=5130" which will be inserted into the JPQL statement during the report execution.


###Dynamic Date Values
Another feature provided by the Imixs-Report API are *dynamic date values* added into a JPQL statement. A *dynamic date value* is an abstract description of date. The date is compute during execution time of the report. A dynamic date value is embraced by the 'date' tag:

    <DATE />
 
 The date tag supports the following optional attributes:
 
 

| Attribute      | Description                    | Example  |
|----------------|--------------------------------|-----------
| DAY_OF_MONTH   | set day of month               | <date DAY_OF_MONTH="1" /> (first day of month, use 'ACTUAL_MAXIMUM' to get last day of month
| MONTH          | set month                      | <date MONTH="1" /> (January)
| YEAR           | set year                       | <date YEAR="2016" />   
| ADD            | add offset (see Calendar.class)| <date ADD="MONTH,-1" /> subracts one month from the current year
 
See the following example to set the start and end date of the last month:


     SELECT workitem FROM Entity AS workitem
	  WHERE workitem.created BETWEEN '<date DAY_OF_MONTH="1" ADD="MONTH,-1 />' 
	                            AND  '<date DAY_OF_MONTH="ACTUAL_MAXIMUM" ADD="MONTH,-1 />' 

 


##Definition of an Attribute List

Each report definition can provide an optional list of attributes. The attribute list defines items to be added into the result set during the report execution. 

<img src="../images/modelling/report-03.png"  width="500"/>

The attribute list is a powerful feature to customize the result set of a report. Note, that WorkItems processed by the same workflow can consist of different property values. To make sure that all Workitems returned in the result set of a report provide the same set of properties the definition of a attribute list can be used.  
If no attribute list exists all item values of a workitem will be returned in the result set during the report execution. 

### Format Definition
Each value of an item defined in the attribute list can be formated in different ways. Therefor the tag 'format' can be appended behind the item name: 

    $created<format locale="de" label="Date">yy-dd-mm</format> 

This example formats the creation date of a workitem into ISO Date format. The optional attribute 'locale' can be used to format dates in a country specific way. See details about how to format a date value in the [Java Doc](http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html).  

The format definition can also be used to format the output of currency and number values: 

     _amount<format>#,##0.00</format>

 See the [Class DecimalFormat](http://docs.oracle.com/javase/7/docs/api/java/text/DecimalFormat.html) for more details. 

### Converter Definition
Beside the option to format values it is also possible to convert the values into a specific object type. This can be useful in case
the object values are not from the same type which can be necessary for XSL transformation. 
To convert the item value the tag 'convert'  can be appended behind the item name: 

     _amount<convert>xs:decimal</convert>
     _number<convert>xs:int</convert>

In case the value is not from the specified type (Double, Integer) the value will be converted into a double or integer object. In case the value can not be converted the value will be reset to 0. 

### Embedded Child Items
A Workitem can optional contain embedded child items in a single item value. An embedded ChildItem can be named in the attribute list of a report by seperating the ChildItem name from the item name inside the child with a '~'. For example:

    _childItems~amount

This attribute definition will extend the JPQL result with all embedded Child Items stored in the attribute '_childItems' and containing the item 'amount'. 

## The XSL Transformation
To transform the result of a report definition into various output formats a XSL template can be added into each report.  

<img src="../images/modelling/report-02.png" width="700"/>

For each Report supporting a XSL Template additional parameters can be defined

 * Content Type 
 * Encoding
 
These parameters are equals to the corresponding  parameters of the Imixs-REST Service interface. 

To execute the transformation of a result set by the defined XSL template the report can be requested with the '.ixr' extension from the [Report REST API](../restapi/reportservice.html):

    http://localhost:8080/workflow/rest-service/report/REPOTNAME.ixr
 