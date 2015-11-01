#XML

The Imixs XML API provides XML Dataobjects to exchanged data between an Imixs Workflow Implementation and a 
service client. The API provides also Client implementations for consuming SOAP or REST Services provided
by an  Workflow Enterprise Application. 
 
The Imixs XML API is based on the 
{{{http://java.sun.com/developer/technicalArticles/WebServices/jaxb/}JAXB specification (JSR-222)}}. 
These means that you can integrate the library very easy in any java framework. 
 
This XML API is used in different components of the Imixs Workflow 
Framework. If you plan to develop a XML interface to the Imixs Workflow
like the JEE Rest Service or JEE Web Service this XML API helps you developing such interfaces.
 
##Dataobjects

The Imixs XML API make use of the Java Architecture for XML Binding (JAXB) to provide the Imixs data objects in XML. JAXB defines a standard to bind java representation to XML and vice versa. It manages XML dcouments and XML Schema Defintions (XSD) in a transparent, object-oriented way that hides the complexity of the XSD Language.
 
The Imixs XML API defines the following XML Root Elements:
 
  * XMLItem - Represents a single item inside a XMLItemCollection. An XMLItem has a name and a
       value. The value can be any serializable collection of objects.
       
  * XMLItemCollection - The XMLitemCollection is the basic serializable representation of a pojo to map
      the org.imixs.workflow.ItemCollection into a xml
      
  * EntityCollection - An EntityCollection represents a list of XMLItemCollections 
    
  * EntityTable -  An EntityTabe represents a list of XMLItemCollections to be used by JAXB api.
      Each XMLItemCollection in the list represents the same properties. So the 
      EntityTable can be used to generate a table representation of XMLItemCollections
  
    


##XML Schema

The purpose of a XML Schema is to define the legal building blocks of an XML document. XML Schemas are used in Web Services to define data objects but also in other components working with XML files.The Imixs XML provides a XML Schema describing the XML Dataobjects provided by the Imixs XML API. This schema can be used for different requirements but the 
Imixs Web Services are the main purpose for this schema. The following code shows the XML schmea which can be saved in a .xsd file.


       	<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    	    <xs:schema version="1.0" xmlns:xs="http://www.w3.org/2001/XMLSchema">
	
    	  <xs:element name="collection" type="entityCollection"/>
    	  <xs:element name="entity" type="xmlItemCollection"/>
    	  <xs:element name="item" type="xmlItem"/>
    	  <xs:complexType name="xmlItem">
    	    <xs:sequence>
    	      <xs:element name="name" type="xs:string" minOccurs="0"/>
    	      <xs:element name="value" type="xs:anyType" nillable="true" minOccurs="0" maxOccurs="unbounded"/>
    	    </xs:sequence>
    	  </xs:complexType>
        	   <xs:complexType name="xmlItemCollection">
    	    <xs:sequence>
          <xs:element name="item" type="xmlItem" nillable="true" minOccurs="0" maxOccurs="unbounded"/>
    	    </xs:sequence>
    	  </xs:complexType>
    	  <xs:complexType name="entityCollection">
    	    <xs:sequence>
    	      <xs:element name="entity" type="xmlItemCollection" nillable="true" minOccurs="0" maxOccurs="unbounded"/>
    	    </xs:sequence>
    	  </xs:complexType>
    	</xs:schema>



The following section explains the XSD datatypes in more detail:
 
### xmlItem
The XMLItem data type represents the basic data object inside this schema definition. 

<img src="../../images/xml/xmlitem.png"/>

The Item  is a complex data type containing a key value pair to a set of different datatypes. Every Item is defined by its name element and contains a unbounded collection of values.  The Value element is defined as a "xsd:anyType" element. This element is defined by the the XMLSchema namespace. So this kind of elements can contain any XML basic data type. This data type is mapped to the java class
 
    org.imixs.workflow.xml.XMLItem



### XMLItemCollection

The XMLItemCollection data type holds a collection of XMLItem objects. 

<img src="../../images/xml/xmlitemCollection.png"/>

The XMLItemCollection represents a single workitem or document structure containing a collection of items. The wrapper class for this complex data type is the class org.imixs.workflow.ItemCollection defined in the Imixs Workflow API. This data type is mapped to the java class
 
    org.imixs.workflow.xml.XMLItemCollection
 
### EntityCollection
The EntityCollection is a complex data type containing a collection of XMLItemCollection objects.  

<img src="../../images/xml/entityCollection.png"/>

This datatype is typical used in service methods returning a collection of workitems or other entity objects. Typical a worklist provided by the Imixs Workflow Manager is represented as an EntityCollection in a service definition like the Imixs Web Services or the Imixs REST Service. This data type can be mapped to the java class
 
    org.imixs.workflow.xml.EntityCollection


## Datatype mapping 
The data types can be mapped on different platforms.  The following section will give a short overview about the usage of  datatypes in the Java and the .NET platform.
 


 
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
 
