/****************************************************************************
 * Copyright (c) 2022-2025 Imixs Software Solutions GmbH and others.
 * https://www.imixs.com
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * This Source Code may also be made available under the terms of the
 * GNU General Public License, version 2 or later (GPL-2.0-or-later),
 * which is available at https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-or-later
 ****************************************************************************/

package org.imixs.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Logger;

import javax.script.ScriptException;

import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for the GraalJS RuleEngine
 * 
 * @author rsoika
 */
public class TestRuleEngineGraalJSDeprecatedScripts {
	protected RuleEngine ruleEngine = null;
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(TestRuleEngineGraalJSDeprecatedScripts.class.getName());

	@BeforeEach
	public void setup() throws PluginException {
		ruleEngine = new RuleEngine();

	}

	/**
	 * This test verifies the evaluation technics
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Test
	public void testEvalDeprecatedGetCall() throws ScriptException, PluginException {

		ItemCollection workitem = new ItemCollection();
		workitem.replaceItemValue("txtname", "Anna");
		workitem.replaceItemValue("subject", "test..");
		ItemCollection event = new ItemCollection();

		// access single value
		String script = "var result={};\n \n  if (workitem.get('txtname')) result.numage=50;else result.numage=20;  if (workitem.get('txtname')) result.some='abc';";

		// run plugin
		workitem = ruleEngine.evaluateBusinessRule(script, workitem, event);
		assertNotNull(workitem);

		assertEquals(50, workitem.getItemValueInteger("numage"));
	}

	/**
	 * This test verifies the evaluation technics
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Test
	public void testEvalDeprecatedDirectItemAccess() throws ScriptException, PluginException {

		ItemCollection workitem = new ItemCollection();
		workitem.replaceItemValue("txtname", "Anna");
		workitem.replaceItemValue("subject", "test..");
		ItemCollection event = new ItemCollection();

		// access single value
		String script = "var result={};\n \n  if (workitem.txtname) result.numage=50;else result.numage=20;";

		// run plugin
		ItemCollection result = ruleEngine.evaluateBusinessRule(script, workitem, event);
		assertNotNull(result);

		assertEquals(50, result.getItemValueInteger("numage"));
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

		// access single value
		String script = " var result={}; result.test=workitem.txtname[0]; if (workitem.txtname && workitem.txtname[0]==='Anna') result.numage=50;else result.numage=20;";

		// run plugin
		workitem = ruleEngine.evaluateBusinessRule(script, workitem, null);
		assertNotNull(workitem);

		assertEquals(50, workitem.getItemValueInteger("numage"));
	}

	/**
	 * This test verifies the deprecated conditional event
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Test
	public void testEvalComplexConditionalEvent() throws ScriptException, PluginException {

		ItemCollection workitem = new ItemCollection();
		workitem.replaceItemValue("_budget_amount", 8400000.00);
		workitem.replaceItemValue("_budget_amount_additional", 1.00);
		workitem.replaceItemValue("_amount_brutto", 900000.00);

		// complex rule
		String script = "";
		script = script + "var contract_sum=workitem._budget_amount[0];";
		script = script + "if (workitem._budget_amount_additional && workitem._budget_amount_additional[0]) {";
		script = script + "   contract_sum=contract_sum+workitem._budget_amount_additional[0];";
		script = script + "}";
		script = script + "( ( contract_sum <= 8500000 && workitem._amount_brutto[0] <= 1000000)";
		script = script + "   || ";
		script = script
				+ "    ( contract_sum > 8500000 && workitem._amount_brutto[0] <= ( 0.12 *  workitem._budget_amount[0] ) ) )";
		// evaluate
		boolean bmatch = ruleEngine.evaluateBooleanExpression(script, workitem);
		assertTrue(bmatch);

	}

	/**
	 * This test verifies the evaluation of a simple scripts
	 * 
	 * workitem['space.team'] && workitem['space.team'][0]!=""
	 * 
	 * !workitem['space.team'] || workitem['space.team'][0]==""
	 * 
	 * See Issue #824
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Test
	public void testBasicScript_Issue824() throws ScriptException, PluginException {
		ItemCollection workitem = new ItemCollection();
		workitem.replaceItemValue("space.team", "Anna");
		workitem.appendItemValue("space.team", "Rico");

		// access single value
		String script = "workitem['space.team'] && workitem['space.team'][0]!=\"\"";

		assertTrue(ruleEngine.evaluateBooleanExpression(script, workitem));

		// test empty sting szenario...
		workitem.replaceItemValue("space.team", "");
		assertFalse(ruleEngine.evaluateBooleanExpression(script, workitem));

		// test missing item szenario...
		workitem.removeItem("space.team");
		assertFalse(ruleEngine.evaluateBooleanExpression(script, workitem));

		// test inverted script
		script = "!workitem['space.team'] || workitem['space.team'][0]===\"\"";
		assertTrue(ruleEngine.evaluateBooleanExpression(script, workitem));

		// test empty sting szenario...
		workitem.replaceItemValue("space.team", "");
		assertTrue(ruleEngine.evaluateBooleanExpression(script, workitem));

		// test empty sting value...
		workitem.replaceItemValue("space.team", "some value");
		assertFalse(ruleEngine.evaluateBooleanExpression(script, workitem));
	}

}
