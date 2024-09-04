package org.imixs.workflow.engine.solr;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test striping invalid characters
 * 
 * @author rsoika
 * 
 */
public class TestStripInvalidCharacters {
	SolrIndexService solrIndexService;

	@BeforeEach
	public void setUp() throws PluginException, ModelException {
		solrIndexService = new SolrIndexService();
	}

	/**
	 * Test
	 * 
	 */
	@Test
	public void testCDATA() {

		String testString = "Hello <![CDATA[<XX>....</XX>]]> Data!";

		String result = solrIndexService.stripCDATA(testString);

		assertEquals("Hello <XX>....</XX> Data!", result);

	}

}
