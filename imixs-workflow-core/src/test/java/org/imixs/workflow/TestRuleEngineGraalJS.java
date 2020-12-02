package org.imixs.workflow;

import java.util.logging.Logger;

import javax.script.ScriptException;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for the GraalJS RuleEngine
 * 
 * @author rsoika
 */
public class TestRuleEngineGraalJS {
	protected RuleEngineGraalVM ruleEngine = null;
	private static Logger logger = Logger.getLogger(TestRuleEngineGraalJS.class.getName());

	@Before
	public void setup() throws PluginException {
		ruleEngine = new RuleEngineGraalVM();

	}

	@Test
	public void testGraalJsContext() {

		Value result = ruleEngine.eval("40+2");
		assert result.asInt() == 42;
	}

	@Test
	public void testSetVariable() {
		Context context = ruleEngine.getContext();
		context.getBindings("js").putMember("age", 42);
		Value result = ruleEngine.eval("age+2");
		assert result.asInt() == 44;
	}

	/**
	 * This test demonstrates the native access to java objects from a script
	 */
	@Test
	public void testSetItemCollectionNativ() {

		ItemCollection workitem = new ItemCollection();
		workitem.setItemValue("age", 42);

		Context context = Context.newBuilder("js").allowAllAccess(true).build();
		context.getBindings("js").putMember("workitem", workitem);
		context.eval("js", "var x = workitem.getItemValueInteger('age');");
		logger.info("result="+context.getBindings("js").getMember("x").asInt());
	}

	/**
	 * This test demonstrates how to add an Imixs ItemCollection to a script.
	 */
	@Test
	public void testSetItemCollection() {

		ItemCollection workitem = new ItemCollection();
		workitem.setItemValue("age", 42);

		ruleEngine.putMember("workitem", workitem);
		Context context = ruleEngine.getContext();

		context.eval("js", "var x = workitem.getItemValueInteger('age');");

		logger.info("result="+context.getBindings("js").getMember("x").asInt());

	}
	
	
	/**
	 * This test verifies a boolean expression
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Test
	public void testBooleanExpression() throws ScriptException, PluginException {

		boolean result = ruleEngine.evaluateBooleanExpression("true",null);
		Assert.assertTrue(result);
		
		
		ItemCollection workitem = new ItemCollection();
		workitem.replaceItemValue("_budget", 1000);
		
		// evaluate true
		String script = "(workitem.getItemValueInteger('_budget')>100)";

		// test
		 result = ruleEngine.evaluateBooleanExpression(script, workitem);
		Assert.assertTrue(result);

		// evaluate false
		script = "(workitem.getItemValueInteger('_budget')<=100)";

		// test
		result = ruleEngine.evaluateBooleanExpression(script, workitem);
		Assert.assertFalse(result);

	}

}
