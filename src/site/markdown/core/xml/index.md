# XML

The Imixs-Workflow core API provides a XML interface to translate all workflow and business information into an open and platform neutral data format.
The XML API provides methods to transform the generic value object *org.imixs.workflow.ItemCollection* into a XML representation. The Imixs XML API is based on the [JAXB specification (JSR-222)](http://java.sun.com/developer/technicalArticles/WebServices/jaxb/) and can be integrated very easy in any common java framework. The XML API is also used by the Imixs-Workflow Rest API. 
 
 
## Data Objects
The Imixs XML API make use of the Java Architecture for XML Binding (JAXB) to translate the Imixs class [ItemCollection](../itemcollection.html) into XML. JAXB defines a standard to bind java representation to XML and vice versa. It manages XML documents and XML schema definitions (XSD) in a transparent, object-oriented way that hides the complexity of the XSD Language. 

The Imixs XML API defines the following XML Elements:
 
  * <strong>item</strong> - Represents a single item inside a document. The element can be identified by the attribute _name_ and a
       value list of any xsd schema types.
  * <strong>document</strong> - The element _document_ is a collection of _item_ elements and represents an Imixs-Workflow data object.
  * <strong>data</strong> - The _data_ root element represents a list of _documents_. 
  
The following example shows how a Imixs Data is represented in XML:

	<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
	<data xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xs="http://www.w3.org/2001/XMLSchema">
		<document>
			<item name="$modified">
				<value xsi:type="xs:dateTime">2018-11-06T12:53:00.402Z</value>
			</item>
			<item name="$processid">
				<value xsi:type="xs:int">5800</value>
			</item>
			<item name="$uniqueid">
				<value xsi:type="xs:string">7a63950d-9b67-4dc8-8643-0baf8f8670c5</value>
			</item>
			<item name="$workflowgroup">
				<value xsi:type="xs:string">Invoice</value>
			</item>
			<item name="_invoice_numbers">
				<value xsi:type="xs:string">4711-1</value>
				<value xsi:type="xs:string">4711-2</value>
			</item>
		</document>
	</data> 
   
The following section explains the XSD datatype definitions for the elements in more detail:
 
### xmlItem
The _"xmlItem"_ data type represents the XML element _item_.  

<img src="../../images/xml/xmlitem.png"/>

The _xmlItem_ is a complex data type identified by the attribute _name_. An _xmlItem_ contains an unbounded list of values of the type "xsd:anyType". This XML data type is mapped to the java class
 
    org.imixs.workflow.xml.XMLItem


### xmlDocument

The _"xmlDocument"_ data type holds a collection of the XML element _item_. 

<img src="../../images/xml/xmlDocument.png"/>

The _xmlDocument_ represents a single workitem or document containing a collection of items. The wrapper class for this complex data type is the class org.imixs.workflow.ItemCollection defined in the Imixs Workflow API. This XML data type is mapped to the java class
 
    org.imixs.workflow.xml.XMLDocument
 
### xmlDataCollection
The complex data type _"xmlDataCollection"_ holds a collection of the XML element _document_.   

<img src="../../images/xml/xmlDataCollection.png"/>

This datatype represents a view of workitems or documents managed by the Imixs-Worklfow engine. 
This data type is mapped to the java class
 
    org.imixs.workflow.xml.XMLDataCollection


### xmlItemArray

The _"xmlItemArray"_ data type holds a collection of _xmlItem_ elements and is used to embedd _xmlItem_ into itself.  

<img src="../../images/xml/xmlItemArray.png"/>

This XML data type is mapped to an array of the java class
 
    org.imixs.workflow.xml.XMLItem

## Data Type Mapping 
The data types used in the value list of a _xmlItem_ can be mapped on different platforms.  The following section will give a short overview about the usage of  datatypes in the Java and the .NET platform.
 

 
| XML Schema Type    | Java type         |.NET type          | 
|--------------------|----------------|------------------|
| xsd:byte           | Byte,byte         |                   | 
| xsd:boolean        | Boolean,boolean   | boolean           | 
| xsd:short          | Short,short       |                   | 
| xsd:int            | Integer,int       |                   | 
| xsd:long           | Long,long         |                   | 
| xsd:float          | Float, float      | float             | 
| xsd:double         | Double, double    | double            | 
| xsd:string         | java.lang.String  | string            | 
| xsd:dateTime       | java.util.Calendar| dateTime          | 
| xsd:integer        | java.math.BigInteger|                 | 
| xsd:decimal        | java.math.BigDecimal| decimal         | 
 
   

## XML Schema

The purpose of a XML Schema is to define the legal building blocks of an XML document. XML Schemas are used in Web Services to define data objects but also in other components working with XML files.The Imixs XML API provides a XML Schema describing the XML Dataobjects. This schema can be used for different requirements, e.g. the Imixs REST Services API. The following code shows the XML schema:


	<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
	<xs:schema attributeFormDefault="unqualified" elementFormDefault="qualified" version="1.0" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
	
	  <xs:element name="data" type="xmlDataCollection"/>
	  <xs:element name="document" type="xmlDocument"/>
	  <xs:element name="item" type="xmlItem"/>
	  <xs:element name="count" type="xmlCount"/>
	
	  <xs:simpleType name="xmlCount">
	    <xs:restriction base="xs:long"/>
	  </xs:simpleType>
	
	  <xs:complexType name="xmlDocument">
	    <xs:sequence>
	      <xs:element name="item" type="xmlItem" form="unqualified" nillable="true" minOccurs="0" maxOccurs="unbounded"/>
	    </xs:sequence>
	  </xs:complexType>
	
	  <xs:complexType name="xmlItem">
	    <xs:sequence>
	      <xs:element name="value" type="xs:anyType" form="unqualified" nillable="true" minOccurs="0" maxOccurs="unbounded"/>
	    </xs:sequence>
	    <xs:attribute name="name" type="xs:string"/>
	  </xs:complexType>
	
	  <xs:complexType name="xmlDataCollection">
	    <xs:sequence>
	      <xs:element name="document" type="xmlDocument" form="unqualified" nillable="true" minOccurs="0" maxOccurs="unbounded"/>
	    </xs:sequence>
	  </xs:complexType>
	
	  <xs:complexType name="xmlItemArray" final="#all">
	    <xs:sequence>
	      <xs:element name="item" type="xmlItem" minOccurs="0" maxOccurs="unbounded" nillable="true"/>
	    </xs:sequence>
	  </xs:complexType>
	</xs:schema>


 