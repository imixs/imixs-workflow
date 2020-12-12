package org.imixs.workflow;

import java.util.logging.Logger;

import javax.script.ScriptException;

import org.imixs.workflow.exceptions.PluginException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for the GraalJS RuleEngine
 * 
 * @author rsoika
 */
public class TestRuleEngineGraalJSDeprecatedScripts {
    protected RuleEngine ruleEngine = null;
    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(TestRuleEngineGraalJSDeprecatedScripts.class.getName());

    @Before
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
        Assert.assertNotNull(workitem);

        Assert.assertEquals(50, workitem.getItemValueInteger("numage"));
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
        Assert.assertNotNull(result);

        Assert.assertEquals(50, result.getItemValueInteger("numage"));
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
        Assert.assertNotNull(workitem);

        Assert.assertEquals(50, workitem.getItemValueInteger("numage"));
    }

}
