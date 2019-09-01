package org.imixs.workflow.engine.solr;

import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Test the WorkflowService method parseJSONQueyResult from SolrSerachService
 * 
 * @author rsoika
 * 
 */
public class TestParseSolrJSONResult {
		
	
	@SuppressWarnings("unused")
	private final static Logger logger = Logger.getLogger(TestParseSolrJSONResult.class.getName());

	SolrSearchService solrSearchService=null;

	@Before
	public void setUp() throws PluginException, ModelException {
		 solrSearchService=new SolrSearchService();
	}
	

	/**
	 * Test 
	 * 
	 */
	@Test
	public void testParseResult() {
		List<ItemCollection> result=null;
		String testString = "{\n" + 
				"  \"responseHeader\":{\n" + 
				"    \"status\":0,\n" + 
				"    \"QTime\":4,\n" + 
				"    \"params\":{\n" + 
				"      \"q\":\"*:*\",\n" + 
				"      \"_\":\"1567286252995\"}},\n" + 
				"  \"response\":{\"numFound\":2,\"start\":0,\"docs\":[\n" + 
				"      {\n" + 
				"        \"type\":[\"model\"],\n" + 
				"        \"id\":\"3a182d18-33d9-4951-8970-d9eaf9d337ff\",\n" + 
				"        \"_modified\":[20190831211617],\n" + 
				"        \"_created\":[20190831211617],\n" + 
				"        \"_version_\":1643418672068296704},\n" + 
				"      {\n" + 
				"        \"type\":[\"adminp\"],\n" + 
				"        \"id\":\"60825929-4d7d-4346-9333-afd7dbfca457\",\n" + 
				"        \"_modified\":[20190831211618],\n" + 
				"        \"_created\":[20190831211618],\n" + 
				"        \"_version_\":1643418672172105728}]\n" + 
				"  }}";
		
		
		
		result=solrSearchService.parseQueryResult(testString);
		Assert.assertEquals(2,result.size());
		
		ItemCollection document=null;

		document=result.get(0);
		Assert.assertEquals("model", document.getItemValueString("type"));
		Assert.assertEquals("3a182d18-33d9-4951-8970-d9eaf9d337ff", document.getItemValueString("id"));
		
		Calendar cal=Calendar.getInstance();
		cal.setTime(document.getItemValueDate("_modified"));
		Assert.assertEquals(7,cal.get(Calendar.MONTH));
		Assert.assertEquals(31,cal.get(Calendar.DAY_OF_MONTH));
		
		document=result.get(1);
		Assert.assertEquals("adminp", document.getItemValueString("type"));
		
		
	}

}
