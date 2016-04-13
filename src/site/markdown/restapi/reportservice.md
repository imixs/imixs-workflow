#The Report Service
The main resource /report provides a flexible rest service interface to create dynamic reports defined by through the imixs Report Service. The report rest service can be used to create and update report definitions and to process the result of a report providing a set of different parameters.

##The /report resources GET
The /report GET resources are used to get business objects provided by the Imixs Report Manager:


| URI                                           | Description                               | 
|-----------------------------------------------|-------------------------------------------|
| /report/reports                               | a list of reports provided by the  workflow instance          |
| /report/reports/{name}                        | a report description for a specific report|
| /report/{name}.ixr                            | the result of a specific report           |
| /report/{name}.html                           | a html table of the result for a specific report              |
| /report/{name}.xml                            | a xml representation of the result for a specific report      |

A report definition need to provide a set of informations to define the layout and content of a report
 
  * <strong>JPQL Statement</strong> - selection of workitems to be selected in a report
  * <strong>contentType / Encoding</strong> - defines the MIME-TYPE and encoding for a report (e.g. text/html for html output, application/pdf for pdf files)
  * <strong>processing instructions</strong> - xsl template to format the xml output of an workitem collection


The following example shows a simple XSLT file to format a workitem collection into a html output:
 
	<?xml version="1.0" encoding="UTF-8" ?>
	<xsl:stylesheet 
		xmlns="http://www.w3.org/1999/xhtml"
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		version="1.0">
		<xsl:output 
			method="xhtml"
			encoding="UTF-8" 
			indent="yes"
			doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"
			doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
			omit-xml-declaration="no" />
		<xsl:template match="/">
		<html>
		<body>
	  		<xsl:apply-templates select="/collection/entity" />
				
		</body>
		</html>
		</xsl:template>
	
	<!-- Template -->
		<xsl:template match="/collection/entity">
		  <div id="address">
			<xsl:value-of select="item[name='name']/value" /><br />
			<xsl:value-of select="item[name='address']/value" /><br />
			<xsl:value-of select="item[name='street']/value" /><br />
			<xsl:value-of select="item[name='zip']/value" /><xsl:text> </xsl:text>
			<xsl:value-of select="item[name='city']/value" /><br />
		</div>
		</xsl:template>
	</xsl:stylesheet>

###Providing JPQL Parameters
A report definition can also contain dynamic JPQL parameters. These parameters can be defined in the JPQL statement of the report. See the following example of JPQL statement:
  
	 SELECT workitem FROM Entity AS workitem
	 JOIN workitem.integerItems AS p
	  WHERE workitem.type = 'workitem' 
	  AND p.itemName = '$processid' 
	  AND p.itemValue = ?1

To provide the Report with the expected parameter ?1 the parameter can be appended into the query string.
 
    http://Host/WorkflowApp/report/reportfile.ixr&1=5130
 
In this example request the URL contains the parameter "?1=5130" which will be inserted into the JPQL statement during the report execution.


###Dynamic Date Values
The JPQL Statement executed by the ReportService may contain dynamic date values. These XML Tags can be used to compute a 
date during execution time. A dynamic date value is embraced by the 'date' tag:

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

 
 
##Apache FOP / PDF Reports
The Imixs Report rest service provides the option to generate PDF Reports based on the [Apache FOP API](http://xmlgraphics.apache.org/fop/). This  is a flexible way to display workitems in PDF or other File formats supported by Apache FOP. To use FOP API during report processing the Apache FOP API need to be included into the Web Module of the rest service. A report definition also need to define the corresponding content type. This is for example 'application/pdf' to create a 
 pdf file. The XSL instructions need to be replaced with the XSL formatting objects (XSL-FO) instructions.  The following example shows a simple FO template
 
	<?xml version="1.0" encoding="UTF-8"?>
	<xsl:stylesheet version="1.1" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format" exclude-result-prefixes="fo">
	  <xsl:output method="xml" version="1.0" omit-xml-declaration="no" indent="yes"/>
	  <xsl:template match="/">
	    <fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
	      <fo:layout-master-set>
	        <fo:simple-page-master master-name="simpleA4" page-height="29.7cm" page-width="21cm" margin-top="2cm" margin-bottom="2cm" margin-left="2cm" margin-right="2cm">
	          <fo:region-body/>
	        </fo:simple-page-master>
	      </fo:layout-master-set>
	      <fo:page-sequence master-reference="simpleA4">
	        <fo:flow flow-name="xsl-region-body">
	          <fo:block font-size="16pt" font-weight="bold" space-after="5mm">Project: <xsl:value-of select="projectname"/>
	          </fo:block>
	          <fo:block font-size="12pt" space-after="5mm">Version <xsl:value-of select="$versionParam"/>
	          </fo:block>
	          <fo:block font-size="10pt">
	            <xsl:for-each select="collection/entity">
			Workitem ID:<xsl:value-of select="item[name='$uniqueid']/value" />
		   </xsl:for-each>
	          </fo:block>
	        </fo:flow>
	      </fo:page-sequence>
	    </fo:root>
	  </xsl:template>
	</xsl:stylesheet>


###Apache FOP / PDF Report Plugin
Reports can also be computed during the processing life cycle by adding the following plug-in into the process model:

    org.imixs.workflow.jee.plugins.ReportPlugin
 
The Report Plugin can compute dynamic collections of workitems based on a JPQL statement as explained before.  As the Plugin runs in the phase of workflow processing the current workitem is typical not selectable with JPQL. Therefore the Report Plugin replaces the result-set with a new instance of the current workitem if the workitem is included in the result-set.
 

## The /report resources DELETE
The /report DELETE resource URIs are used to delete business objects:


| URI                                           | Description                               | 
|-----------------------------------------------|-------------------------------------------|
| /report/reports/{name}                        | deletes a specified report                |


## The /report resources PUT and POST
The /report PUT and POST resources URIs are used to write business objects:


| URI                                           | Description                               | 
|-----------------------------------------------|-------------------------------------------|
| /report                                       | creates or update a specified report      |


 
## Resource Options
You can specify additional URI parameters to access only a subset of workitems by a collection  method URI. This is useful to get only a subset of the whole worklist and to navigate through a list of workitems. Append optional arguments to define the number of workitems returned by a URL and the starting point inside the list. Combine any of the following arguments for the desired result. 

| option      | description                                         | example               |
|-------------|-----------------------------------------------------|-----------------------|
| count       | number of workitems returned by a collection        | ..?count=10           |
| start       | position to start  workitems returned by a  collection        | ..?start=5&count=10   |
| type        | Optional type property workitems are filtered       | ..?type=workitem      | 
| download    | Optional filename for a download request This generates the HTTP Header   Content-disposition,attachment;filename=example.pdf   |download=example.pdf   |


<strong>Note:</strong> The Imixs JEE Workflow manages the access to workitems by individual access lists per each entity. So the result of a collection of workitems will only contain entities where the current user has a  read access right. Without that right the workitem will not be returned by the workflowManager and so it will not be included in the list. 
  
   