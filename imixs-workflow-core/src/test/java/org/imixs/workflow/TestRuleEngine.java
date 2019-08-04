package org.imixs.workflow;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.script.ScriptException;

import org.imixs.workflow.exceptions.PluginException;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Test class for RuleEngine
 * 
 * @author rsoika
 */
public class TestRuleEngine {
	protected RuleEngine ruleEngine = null;

	@Before
	public void setup() throws PluginException {
		ruleEngine = new RuleEngine();

	}

	/**
	 * This test verifies the evaluation of a simple script unsing the json objects.
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Test
	public void testBasicScript() throws ScriptException, PluginException {

		ItemCollection workitem = new ItemCollection();
		workitem.replaceItemValue("txtName", "Anna");
		ItemCollection event = new ItemCollection();

		// access single value
		String script = "var result={}; if (workitem.txtname && workitem.txtname[0]==='Anna') result.numage=50;";

		// run plugin
		workitem = ruleEngine.evaluateBusinessRule(script, workitem, event);
		Assert.assertNotNull(workitem);

		Assert.assertEquals(50, workitem.getItemValueInteger("numage"));
	}

	/**
	 * This test verifies a boolean expression
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Test
	public void testBooleanExpression() throws ScriptException, PluginException {

		ItemCollection workitem = new ItemCollection();
		workitem.replaceItemValue("_budget", 1000);

		// evaluate true
		String script = "(workitem._budget && workitem._budget[0]>100)";

		// test
		boolean result = ruleEngine.evaluateBooleanExpression(script, workitem);
		Assert.assertTrue(result);

		// evaluate false
		script = "(workitem._budget && workitem._budget[0]<=100)";

		// test
		result = ruleEngine.evaluateBooleanExpression(script, workitem);
		Assert.assertFalse(result);

	}

	/**
	 * The test parses a complex json structure...
	 * 
	 * @throws IOException
	 * @throws ScriptException
	 */
	@Test
	public void testSimpleJsonParseByScript() throws IOException {

		String json = readFromFile("/json/simple_content.json");
		String script = "var result={}; result.name=data.name;result.id=data.id;";

		RuleEngine ruleEngine = new RuleEngine();
		ItemCollection result = null;
		try {
			result = ruleEngine.evaluateJsonByScript(json, script);
		} catch (ScriptException e) {

			e.printStackTrace();
			Assert.fail();
		}
		Assert.assertNotNull(result);

		Assert.assertEquals("simple data", result.getItemValueString("name"));
		Assert.assertEquals(70805774, result.getItemValueInteger("id"));

	}

	/**
	 * Helper Method to read a file and return the content as a string.
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	private String readFromFile(String file) throws IOException {
		try (InputStream is = getClass().getResourceAsStream(file)) {

			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			int nRead;
			byte[] data = new byte[0x4000];
			while ((nRead = is.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, nRead);
			}
			buffer.flush();
			is.close();

			byte[] dataArry = buffer.toByteArray();
			return new String(dataArry);
		}
	}

}
