package org.imixs.workflow.engine.solr;

import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Test striping invalid characters
 * 
 * @author rsoika
 * 
 */
public class TestStripInvalidCharacters {
	SolrIndexService solrIndexService;
	@Before
	public void setUp() throws PluginException, ModelException {
		solrIndexService=new SolrIndexService();
	}
	

	/**
	 * Test 
	 * 
	 */
	@Test
	public void testCDATA() {
		
		String testString = "Hello <![CDATA[<XX>....</XX>]]> Data!";
		
		
		
		String result=solrIndexService.stripCDATA(testString);
		
		
		Assert.assertEquals("Hello <XX>....</XX> Data!",result);
		
		
		
	}

}
