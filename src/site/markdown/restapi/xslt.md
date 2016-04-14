#XSLT
You can use XSLT to transform the XML result of report into a new output format.

The following section shows some examples how to transorm the XML format of an Imixs EntityCollection into new formats using XSLT.



##Select a WorkflowGroup

The following Example shows how to select only workitems with a specific workflowgroup named 'Invoice' and print out the attribute 'txtworkflowstatus' of each matching entity:

    <?xml version="1.0" encoding="UTF-8" ?>
    <xsl:stylesheet xmlns="http://www.w3.org/1999/xhtml" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	   <xsl:output method="xml" indent="yes"/>
	    	<xsl:template match="/">
	    	<collection>
		    	<xsl:apply-templates select="/collection/entity[normalize-space(item[name = 'txtworkflowgroup']/value) = 'Rechnungsausgang']" />			
		  </collection>
	  </xsl:template>
	  <!-- Template Definition -->
      <xsl:template
		match="/collection/entity[normalize-space(item[name = 'txtworkflowgroup']/value) = 'Rechnungsausgang']">
      	 Status=<xsl:value-of select="item[name='txtworkflowstatus']/value" />
       </xsl:template>
    </xsl:stylesheet>



##Group Workitems by Category

The next example groups all workitems by txtworkflowstatus and print out the created and _amount property in each category


    <?xml version="1.0" encoding="UTF-8" ?>
      <xsl:stylesheet xmlns="http://www.w3.org/1999/xhtml" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	    <xsl:output method="xml" indent="yes" />
        	<xsl:key name="groups" match="/collection/entity"
		    use="item[name='txtworkflowstatus']/value" />
    	<xsl:template match="/">
	    	<collection>
		    	<xsl:apply-templates select="/collection/entity[generate-id() = generate-id(key('groups', item[name='txtworkflowstatus']/value)[1])]" />
		    </collection>
	  </xsl:template>
    	<xsl:template match="/collection/entity">
	    	<status>
		    	<xsl:value-of select="item[name='txtworkflowstatus']/value" />
		   </status>
     		<xsl:for-each select="key('groups', item[name='txtworkflowstatus']/value)">
	    		<created>
		    		<xsl:value-of select="item[name='$created']/value" />
			    </created>
		    	<amount>
			    	<xsl:value-of select="item[name='_amount']/value" />
			   </amount>
		    </xsl:for-each>
	   </xsl:template>
     </xsl:stylesheet>



##Sum total by Category

The next example grops all workitems by txtworkflowstatus and returns the sum of the value '_amount'


    <?xml version="1.0" encoding="UTF-8" ?>
      <xsl:stylesheet xmlns="http://www.w3.org/1999/xhtml" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
      <xsl:output method="xml" indent="yes" />
        <xsl:key name="groups" match="/collection/entity" use="item[name='txtworkflowstatus']/value" />
    	<xsl:template match="/">
		<collection>
			<xsl:apply-templates
				select="/collection/entity[generate-id() = generate-id(key('groups', item[name='txtworkflowstatus']/value)[1])]" />
		</collection>
	  </xsl:template>
	  <xsl:template match="/collection/entity">
		<status>
			<xsl:value-of select="item[name='txtworkflowstatus']/value" />
		</status>
		<total>
			<xsl:value-of select="sum(key('groups', item[name='txtworkflowstatus']/value)//item[name='_amount']/value)" />
		</total>       
	   </xsl:template>
     </xsl:stylesheet>
