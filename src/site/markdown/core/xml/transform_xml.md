#XSL Transformation 
Based on a XSL template external XML data sources can be transformed into the Imixs XML schema.
The result of thus a transformation can be posted to the Imixs-Workflow REST service API. 
This is how a interface between an external system and the Imixs Workflow engine can be realized.
The following example shows how to transform xml data:
 
## The XML Data 
This is a sample of a xml data structure provided by an external system. The structure contains several 'Documents' containing detail information and DocumentLevelFields
 
	<?xml version="1.0" encoding="UTF-8"?>
	<Batch>
		<Documents>
			<Document>
				<Identifier>DOC1</Identifier>
				<Type>Imixs-RG</Type>
				<Valid>true</Valid>
				<Reviewed>true</Reviewed>
				<ErrorMessage></ErrorMessage>
				<DocumentLevelFields>
					<DocumentLevelField>
						<Name>RG-NR</Name>
						<Value>05052011</Value>
						<Type>STRING</Type>
						<Page>PG0</Page>
						<FieldOrderNumber>1</FieldOrderNumber>
					</DocumentLevelField>
				</DocumentLevelFields>
			</Document>
			<Document>
				<Identifier>DOC2</Identifier>
				<Type>Imixs-RG</Type>
				<Valid>true</Valid>
				<Reviewed>true</Reviewed>
				<ErrorMessage></ErrorMessage>
				<DocumentLevelFields>
					<DocumentLevelField>
						<Name>RG-NR</Name>
						<Value>44032011</Value>
						<Type>STRING</Type>
						<Page>PG0</Page>
						<FieldOrderNumber>1</FieldOrderNumber>
					</DocumentLevelField>
				</DocumentLevelFields>
				
			</Document>
		</Documents>
	</Batch>
  

##Transforming the XML Data with XSLT 
With a XSL template it is possible to transform the given XML data into the Imixs XML schema. This XML format of an Imixs EntityCollection has the following structure:

	<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
	<data>
	    <document>
	       <item>
	         <name>numsequencenummer</name>
	         <value xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	          xmlns:xs="http://www.w3.org/2001/XMLSchema" xsi:type="xs:string">1</value>
	       </item>
	       <item>
	         <name>namlasteditor</name>
	         <value xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	          xmlns:xs="http://www.w3.org/2001/XMLSchema" xsi:type="xs:string">sally</value>
	       </item>
	       <item>
	         <name>$uniqueid</name>
	         <value xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	          xmlns:xs="http://www.w3.org/2001/XMLSchema" xsi:type="xs:string">13189f34ce7-5668d41
	         </value>
	       </item>
	       <item>
	         <name>_ordernumber</name>
	         <value xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	          xmlns:xs="http://www.w3.org/2001/XMLSchema" xsi:type="xs:string">345</value>
	       </item>
	    </document>
	    <document>
	       <item>
	         <name>numsequencenummer</name>
	         <value xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	          xmlns:xs="http://www.w3.org/2001/XMLSchema" xsi:type="xs:string">2</value>
	       </item>
	       <item>
	         <name>namlasteditor</name>
	         <value xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	          xmlns:xs="http://www.w3.org/2001/XMLSchema" xsi:type="xs:string">Karl</value>
	       </item>
	       <item>
	         <name>$uniqueid</name>
	         <value xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	          xmlns:xs="http://www.w3.org/2001/XMLSchema" xsi:type="xs:string">13189f34ce7-5668d41
	         </value>
	       </item>
	       <item>
	         <name>_ordernumber</name>
	         <value xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	          xmlns:xs="http://www.w3.org/2001/XMLSchema" xsi:type="xs:string">345555</value>
	       </item>
	    </document>
	</data>

See details about the xml format in the section [XML](../index.html). To transform the external data source a simple XSL template can be used like shown in the following example:
 
	<?xml version="1.0"?>
	<xsl:stylesheet version="1.0"
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	
		<xsl:template match="/">
			<data>
				<xsl:apply-templates select="Batch/Documents/Document[Type='Imixs-RG']">
				</xsl:apply-templates>
			</data>
		</xsl:template>
	
		<xsl:template match="Batch/Documents/Document[Type='Imixs-RG']">
			<document>
			
			<!-- setting General Workflow Settings -->
				<item>
					<name>type</name>
					<value xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
						xmlns:xs="http://www.w3.org/2001/XMLSchema" xsi:type="xs:string">workitem</value>
				</item>
				<item>
					<name>namcreator</name>
					<value xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
						xmlns:xs="http://www.w3.org/2001/XMLSchema" xsi:type="xs:string">ephesoft-batch-process</value>
				</item>
				
				<item>
					<name>$modelversion</name>
					<value xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
						xmlns:xs="http://www.w3.org/2001/XMLSchema" xsi:type="xs:string">office-de-0.0.1</value>
				</item>
				
				<item>
					<name>$processid</name>
					<value xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
						xmlns:xs="http://www.w3.org/2001/XMLSchema" xsi:type="xs:int">4000</value>
				</item>
				<item>
					<name>$activityid</name>
					<value xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
						xmlns:xs="http://www.w3.org/2001/XMLSchema" xsi:type="xs:int">20</value>
				</item>
				
				
				<!-- extract _ordernumber -->
				<item>
					<name>_ordernumber</name>
					<value xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
						xmlns:xs="http://www.w3.org/2001/XMLSchema" xsi:type="xs:string">
						<xsl:value-of
							select="DocumentLevelFields/DocumentLevelField[Name='RG-NR']/Value" />
					</value>
				</item>
			</document>
		</xsl:template>
	</xsl:stylesheet>  

This XSL template selects all Document nodes with the type='Imixs-RG'. For each document node  the condition xsl:apply-templates creates a new entity tag with different item nodes. The template creates the entity tag and provides some workitem fields with fixed values.  In the last section of the xsl:template a value from the provided DocumentLevelField tag is extracted and transformed into an item with the name "\_ordernumer" and the corresponding value. The result of such a transformation can be used to be posted to the Imixs Rest Service interface. See the section [Post XML Data](./post_xml.html).
 