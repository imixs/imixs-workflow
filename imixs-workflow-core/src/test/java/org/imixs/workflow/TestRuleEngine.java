package org.imixs.workflow;

import javax.script.ScriptException;

import org.imixs.workflow.exceptions.PluginException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


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
	

}
