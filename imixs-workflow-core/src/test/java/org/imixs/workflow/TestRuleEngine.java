package org.imixs.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.script.ScriptException;

import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for RuleEngine
 * 
 * @author rsoika
 */
public class TestRuleEngine {
	protected RuleEngine ruleEngine = null;

	@BeforeEach
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
		workitem.setItemValue("name", "Anna");
		// define a script
		String js = "var result={}; if ('Anna' == workitem.getItemValueString('name')) result.colleague='Melman';";
		// evaluate the business rule
		workitem = ruleEngine.evaluateBusinessRule(js, workitem, null);
		assertNotNull(workitem);
		assertEquals("Melman", workitem.getItemValueString("colleague"));
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
		assertTrue(result);

		// evaluate false
		script = "(workitem._budget && workitem._budget[0]<=100)";

		// test
		result = ruleEngine.evaluateBooleanExpression(script, workitem);
		assertFalse(result);

	}

}
