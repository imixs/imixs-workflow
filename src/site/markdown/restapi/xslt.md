# XSLT
You can use XSLT to transform the XML result of a report into a new output format.

The following section shows some examples how to transform and aggregate the XML result of an Imixs EntityCollection into new formats using XSLT.

## Select a single Item Value

The following example shows an output of item values of the current document:

    <?xml version="1.0" encoding="UTF-8" ?>
    <xsl:stylesheet xmlns="http://www.w3.org/1999/xhtml" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	   <xsl:output method="xml" indent="yes"/>
	    	<xsl:template match="/">
	    		Status:<xsl:value-of select="document/item[@name='$workflowstatus']/value" />
	    		Supplier:<xsl:value-of select="document/item[@name='_supplier']/value" />
       </xsl:template>
    </xsl:stylesheet>
    
## Select an Item List

Items can contain a value list. The following example shows how to iterate over a list of values stored in the item named '_system':
    
	...
	<xsl:for-each select="document/item[@name='_system']/value">
	  <xsl:value-of select="." />
	</xsl:for-each>
	....

## Iterate over a List of embedded Items

The following example shows how you can iterate over a embedded list of items (Map) stored in an item with the name '_childitems' :


	...
	<xsl:for-each select="document/item[@name='_childitems']/value">
	  <tr>
	     <td><xsl:value-of select="item[@name='numpos']/value"/></td>
	     <td><xsl:value-of select="item[@name='article']/value"/></td>
	     <td><xsl:value-of select="item[@name='amount']/value"/></td>
	     <td><xsl:value-of select="item[@name='unit']/value"/></td>
	     <td><xsl:value-of select="item[@name='price']/value"/></td>
	  </tr>
	</xsl:for-each>
	...

 
## Select Entities by a Specific Item Value

The following Example shows how to select only entities with the specific Item $workflowgroup='Invoice' and print out the attribute '$tworkflowstatus' of each matching entity:

    <?xml version="1.0" encoding="UTF-8" ?>
    <xsl:stylesheet xmlns="http://www.w3.org/1999/xhtml" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	   <xsl:output method="xml" indent="yes"/>
	    	<xsl:template match="/">
	    	<collection>
		    	<xsl:apply-templates select="/collection/document[normalize-space(item[@name = '$workflowgroup']/value) = 'Rechnungsausgang']" />			
		  </collection>
	  </xsl:template>
	  <!-- Template Definition -->
      <xsl:template
		match="/collection/document[normalize-space(item[@name = '$workflowgroup']/value) = 'Rechnungsausgang']">
      	 Status=<xsl:value-of select="item[@name='$workflowstatus']/value" />
       </xsl:template>
    </xsl:stylesheet>




## Group Workitems by Category

You can also group entities by a Item Value. The next example groups all entities by the Item "$workflowstatus" and print out the attributes $created and _amount in each category


    <?xml version="1.0" encoding="UTF-8" ?>
      <xsl:stylesheet xmlns="http://www.w3.org/1999/xhtml" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	    <xsl:output method="xml" indent="yes" />
        	<xsl:key name="groups" match="/collection/document"
		    use="item[@name='$workflowstatus']/value" />
    	<xsl:template match="/">
	    	<collection>
		    	<xsl:apply-templates select="/collection/document[generate-id() = generate-id(key('groups', item[@name='$workflowstatus']/value)[1])]" />
		    </collection>
	  </xsl:template>
    	<xsl:template match="/collection/document">
	    	<status>
		    	<xsl:value-of select="item[@name='$workflowstatus']/value" />
		   </status>
     		<xsl:for-each select="key('groups', item[@name='$workflowstatus']/value)">
	    		<created>
		    		<xsl:value-of select="item[@name='$created']/value" />
			    </created>
		    	<amount>
			    	<xsl:value-of select="item[@name='_amount']/value" />
			   </amount>
		    </xsl:for-each>
	   </xsl:template>
     </xsl:stylesheet>



##Sum total by Category

The next example groups all entities by the Item Value "$workflowstatus" and returns the sum of the value '_amount'.


    <?xml version="1.0" encoding="UTF-8" ?>
      <xsl:stylesheet xmlns="http://www.w3.org/1999/xhtml" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
      <xsl:output method="xml" indent="yes" />
        <xsl:key name="groups" match="/collection/document" use="item[@name='$workflowstatus']/value" />
    	<xsl:template match="/">
		<collection>
			<xsl:apply-templates
				select="/collection/document[generate-id() = generate-id(key('groups', item[@name='$workflowstatus']/value)[1])]" />
		</collection>
	  </xsl:template>
	  <xsl:template match="/collection/document">
		<status>
			<xsl:value-of select="item[@name='$workflowstatus']/value" />
		</status>
		<total>
			<xsl:value-of select="sum(key('groups', item[@name='$workflowstatus']/value)//item[@name='_amount']/value)" />
		</total>       
	   </xsl:template>
     </xsl:stylesheet>
