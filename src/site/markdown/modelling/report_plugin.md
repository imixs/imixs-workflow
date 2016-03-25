#Workflow Reports
The Imixs-Report plug-In allows the definition of business reports. A report aggregates and transforms a collection of WorkItems into a custom output format (e.g. XML, HTML, PDF, ...). A report created with the Imixs-Report Plug-In can be accessed and synchronized with a running Imixs-Workflow instance via the [REST Service interface](../restapi/reportservice.html).

##Installation
The Imixs-Report plug-In is installed the same way as any other Eclipse plug-in. To install the Imixs-Report plug-in select from the Eclipse Workbench main menu *Help -> Install New Software*. Enter the following update site URL:

    http://www.imixs.org/org.imixs.eclipse.updatesite


##Creating a Report Definition 
Reports are stored in a File with the extension ".ixr" and can be added to any existing Eclipse project. Each report consists of a JPQL Statement and an optional attribute list and xsl definition. The JPQL Statement must be valid for the workflow instance the report should be run in. 

<img src="../images/modelling/report-01.png"  width="700"/>

The Attribute list is optional and only used in case of a HTML output format.
To synchronize a report the Report Rest Service endpoint can be added into the report definition:
   
    http://localhost:8080/workflow/rest-service/report/

Each report is identified by its report name equals to the file resource name.  If a report of the same name still exists in the Imixs-Workflow instance the report will be replaced.

To generate a report during runtime the report can be called by the Imixs REST Service:

    http://HOST/WEBMODULE/RestService/report/REPOTNAME.html

See details on the Imixs [REST Service documentation](../restapi/reportservice.html).

To remove an existing report use the [Imixs-Admin Client](../administration.html). 

### The Attribute Definition
In case a report is used to be exported into a HTML table the section 'attributes' can be used to define a subset of properties to be rendered into the result table. 

The content of an attribute can be formated by adding a 'format' tag behind the property name. 

    $created<format locale="de" label="Date">yy-dd-mm</format> 

This example formats the creation date of a workitem into ISO Date format. The optional attribute 'locale' can be used to format dates in a country specific way. See the [Java Doc](http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html) for details.  

### The XSL Definition
To transform the result of a report definition into various output formats a XSL template can be added into each report.  

<img src="../images/modelling/report-02.png" width="700"/>

For each Report supporting a XSL Template additional parameters can be defined

 * Content Type 
 * Encoding
 
These parameters a equals to the corresponding  parameters of the Imixs-REST Service interface. 
 