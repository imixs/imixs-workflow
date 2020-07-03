package org.imixs.workflow.engine;

import java.util.List;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Test class for WorkflowService
 * <p>
 * This test verifies the evaluation of the workflow result.
 * 
 * @author rsoika
 */
public class TestEvaluateWorkflowResult {

    private final static Logger logger = Logger.getLogger(TestEvaluateWorkflowResult.class.getName());

    protected WorkflowMockEnvironment workflowMockEnvironment;

    @Before
    public void setUp() throws PluginException, ModelException {
        workflowMockEnvironment = new WorkflowMockEnvironment();
        workflowMockEnvironment.setup();

    }

    /**
     * This test evaluates a event result
     * 
     * @throws PluginException
     */
    @Test
    public void testEvaluateWorkflowResult() {
        ItemCollection activityEntity = new ItemCollection();

        try {
            activityEntity.replaceItemValue("txtActivityResult",
                    "<item ignore=\"true\" name=\"comment\" >some data</item>");
            ItemCollection result = workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity,
                    "item", new ItemCollection());
            Assert.assertNotNull(result);
            Assert.assertTrue(result.hasItem("comment"));
            Assert.assertEquals("some data", result.getItemValueString("comment"));
            Assert.assertEquals("true", result.getItemValueString("comment.ignore"));
        } catch (PluginException e) {
            e.printStackTrace();
            Assert.fail();
        }

        // test an empty item tag
        try {
            activityEntity.replaceItemValue("txtActivityResult", "<item ignore=\"true\" name=\"comment\" />");
            ItemCollection result = workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity,
                    "item", new ItemCollection());
            Assert.assertNotNull(result);
            Assert.assertTrue(result.hasItem("comment"));
            Assert.assertEquals("", result.getItemValueString("comment"));
            Assert.assertEquals("true", result.getItemValueString("comment.ignore"));
        } catch (PluginException e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    /**
     * This test evaluates a event result
     * 
     * @throws PluginException
     */
    @Test
    public void testEvaluateWorkflowResultNumbers() {
        ItemCollection activityEntity = new ItemCollection();

        // test integer
        try {
            activityEntity.replaceItemValue("txtActivityResult", "<item name=\"count\" type=\"integer\">55</item>");
            ItemCollection result = workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity,
                    "item", new ItemCollection());
            Assert.assertNotNull(result);
            Assert.assertTrue(result.hasItem("count"));
            Assert.assertEquals(55, result.getItemValueInteger("count"));
        } catch (PluginException e) {
            e.printStackTrace();
            Assert.fail();
        }

        // test double
        try {
            activityEntity.replaceItemValue("txtActivityResult", "<item name=\"count\" type=\"double\">55.11</item>");
            ItemCollection result = workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity,
                    "item", new ItemCollection());
            Assert.assertNotNull(result);
            Assert.assertTrue(result.hasItem("count"));
            Assert.assertEquals(55.11, result.getItemValueDouble("count"));
        } catch (PluginException e) {
            e.printStackTrace();
            Assert.fail();
        }

        // test empty string for Double
        try {
            activityEntity.replaceItemValue("txtActivityResult", "<item name=\"count\" type=\"double\"></item>");
            ItemCollection result = workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity,
                    "item", new ItemCollection());
            Assert.assertNotNull(result);
            Assert.assertTrue(result.hasItem("count"));
            Assert.assertEquals(0.0, result.getItemValueDouble("count"));
        } catch (PluginException e) {
            e.printStackTrace();
            Assert.fail();
        }

        // test empty string for Integer
        try {
            activityEntity.replaceItemValue("txtActivityResult", "<item name=\"count\" type=\"integer\"></item>");
            ItemCollection result = workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity,
                    "item", new ItemCollection());
            Assert.assertNotNull(result);
            Assert.assertTrue(result.hasItem("count"));
            Assert.assertEquals(0, result.getItemValueInteger("count"));
        } catch (PluginException e) {
            e.printStackTrace();
            Assert.fail();
        }

    }

    /**
     * This test evaluates a event result for a double item copied from a source
     * item value
     * 
     * @throws PluginException
     */
    @Test
    public void testEvaluateWorkflowResultNumbersFromSource() {
        ItemCollection activityEntity = new ItemCollection();

        ItemCollection source = new ItemCollection();
        source.replaceItemValue("amount", 55.123);

        // test double
        try {
            activityEntity.replaceItemValue("txtActivityResult",
                    "<item name=\"count\" type=\"double\"><itemvalue>amount</itemvalue></item>");
            ItemCollection result = workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity,
                    "item", source);
            Assert.assertNotNull(result);
            Assert.assertTrue(result.hasItem("count"));
            Assert.assertEquals(55.123, result.getItemValueDouble("count"));
        } catch (PluginException e) {
            e.printStackTrace();
            Assert.fail();
        }

    }

    /**
     * This test verifies if multiple item tags with the same name will be evaluated
     * and added into one single property
     * 
     * @throws PluginException
     */
    @Test
    public void testEvaluateWorkflowResultMultiValue() throws PluginException {
        String sResult = "<item name=\"txtName\">Manfred</item>";
        sResult += "\n<item name=\"txtName\">Anna</item>";
        sResult += "\n<item name=\"test\">XXX</item>";
        sResult += "\n<item name=\"txtname\">Sam</item>";

        ItemCollection activityEntity = new ItemCollection();
        activityEntity.replaceItemValue("txtActivityResult", sResult);

        // expected txtname= Manfred,Anna,Sam
        ItemCollection evalItemCollection = new ItemCollection();
        evalItemCollection = workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity, "item",
                new ItemCollection());

        Assert.assertTrue(evalItemCollection.hasItem("txtName"));

        List<?> result = evalItemCollection.getItemValue("txtname");

        Assert.assertEquals(3, result.size());

        Assert.assertTrue(result.contains("Manfred"));
        Assert.assertTrue(result.contains("Sam"));
        Assert.assertTrue(result.contains("Anna"));

        // test test item
        Assert.assertEquals("XXX", evalItemCollection.getItemValueString("test"));
    }

    /**
     * This test verifies if multiple item tags with empty tags work correctly (see
     * issue #490
     * 
     * @throws PluginException
     */
    @Test
    public void testEvaluateWorkflowResultMultiValueWithEmptyTag() throws PluginException {
        String sResult = "<item name=\"comment\" ignore=\"true\"/>";
        sResult += "\n<item name=\"action\">home</item>";

        ItemCollection activityEntity = new ItemCollection();
        activityEntity.replaceItemValue("txtActivityResult", sResult);

        workflowMockEnvironment.getWorkflowService();
        long l = System.currentTimeMillis();
        ItemCollection evalItemCollection = new ItemCollection();
        evalItemCollection = workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity, "item",
                new ItemCollection());

        logger.info("...evaluated result in " + (System.currentTimeMillis() - l) + "ms...");

        // expected: comment_ignore=true
        Assert.assertTrue(evalItemCollection.hasItem("comment.ignore"));
        Assert.assertTrue(evalItemCollection.getItemValueBoolean("comment.ignore"));

        // expected: action = "home"
        Assert.assertTrue(evalItemCollection.hasItem("action"));
        Assert.assertEquals("home", evalItemCollection.getItemValueString("action"));

        // now test the different order....
        // we expect the same result
        sResult = "<item name=\"action\">home</item>";
        sResult += "\n<item name=\"comment\" ignore=\"true\"/>";

        activityEntity = new ItemCollection();
        activityEntity.replaceItemValue("txtActivityResult", sResult);
        l = System.currentTimeMillis();
        evalItemCollection = new ItemCollection();
        evalItemCollection = workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity, "item",
                new ItemCollection());
        logger.info("...evaluated result in " + (System.currentTimeMillis() - l) + "ms...");

        // expected: comment_ignore=true
        Assert.assertTrue(evalItemCollection.hasItem("comment.ignore"));
        Assert.assertTrue(evalItemCollection.getItemValueBoolean("comment.ignore"));

        // expected: action = "home"
        Assert.assertTrue(evalItemCollection.hasItem("action"));
        Assert.assertEquals("home", evalItemCollection.getItemValueString("action"));
    }

    /**
     * This test evaluates an embedded xml content with newline chars used by the
     * split plugin
     * 
     * <code>
     * <item name="subprocess_create">
    	    <modelversion>controlling-analyse-de-1.0.0</modelversion>
    	    <processid>1000</processid>
    	    <activityid>100</activityid> 
    	    <items>_subject,_sender,_receipients,$file</items>
    	</item>
     * </code>
     * 
     * The test also test string variants with different newlines!
     */
    @Test
    public void testEvaluateWorkflowResultEmbeddedXML() {
        ItemCollection activityEntity = new ItemCollection();
        try {

            // 1) create test result single line mode.....
            String activityResult = "<item name=\"subprocess_create\">"
                    + "    <modelversion>analyse-1.0.0</modelversion>" + "	    <processid>1000</processid>"
                    + "	    <activityid>100</activityid>" + "	    <items>_subject,_sender,_receipients,$file</items>"
                    + "	</item>";

            activityEntity.replaceItemValue("txtActivityResult", activityResult);
            ItemCollection result = workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity,
                    "item", new ItemCollection());
            Assert.assertNotNull(result);
            Assert.assertTrue(result.hasItem("subprocess_create"));
            String xmlContent = result.getItemValueString("subprocess_create");
            Assert.assertFalse(xmlContent.isEmpty());
            Assert.assertTrue(xmlContent.contains("<modelversion>analyse-1.0.0</modelversion>"));

            // 2) create test result unix mode.....
            activityResult = "<item name=\"subprocess_create\">\n" + "    <modelversion>analyse-1.0.0</modelversion>\n"
                    + "	    <processid>1000</processid>\n" + "	    <activityid>100</activityid>\n"
                    + "	    <items>_subject,_sender,_receipients,$file</items>\n" + "	</item>";

            activityEntity.replaceItemValue("txtActivityResult", activityResult);
            result = workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity, "item",
                    new ItemCollection());
            Assert.assertNotNull(result);
            Assert.assertTrue(result.hasItem("subprocess_create"));
            xmlContent = result.getItemValueString("subprocess_create");
            Assert.assertFalse(xmlContent.isEmpty());
            Assert.assertTrue(xmlContent.contains("<modelversion>analyse-1.0.0</modelversion>"));

            // 3) create test result windows mode.....
            activityResult = "<item name=\"subprocess_create\">\r\n"
                    + "    <modelversion>analyse-1.0.0</modelversion>\r\n" + "	    <processid>1000</processid>\r\n"
                    + "	    <activityid>100</activityid>\r\n"
                    + "	    <items>_subject,_sender,_receipients,$file</items>\r\n" + "	</item>";

            activityEntity.replaceItemValue("txtActivityResult", activityResult);
            result = workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity, "item",
                    new ItemCollection());
            Assert.assertNotNull(result);
            Assert.assertTrue(result.hasItem("subprocess_create"));
            xmlContent = result.getItemValueString("subprocess_create");
            Assert.assertFalse(xmlContent.isEmpty());
            Assert.assertTrue(xmlContent.contains("<modelversion>analyse-1.0.0</modelversion>"));
        } catch (PluginException e) {
            e.printStackTrace();
            Assert.fail();
        }

    }

    /**
     * testing invalid item tag formats
     */
    @Test
    public void testEvaluateWorkflowResultInvalidFormat() {
        ItemCollection activityEntity = new ItemCollection();

        try {
            // test no name attribute
            activityEntity.replaceItemValue("txtActivityResult",
                    "<item ignore=\"true\" noname=\"comment\" >some data</item>");
            workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity, "item",
                    new ItemCollection());
            Assert.fail();
        } catch (PluginException e) {
            // ok
        }

        try {
            // test wrong closing tag
            activityEntity.replaceItemValue("txtActivityResult",
                    "<item ignore=\"true\" name=\"comment\" >some data</xitem>");
            workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity, "item",
                    new ItemCollection());
            Assert.fail();
        } catch (PluginException e) {
            // exception expected
        }

    }

    /**
     * testing result containing no item tags at all
     */
    @Test
    public void testEvaluateWorkflowResultNothing() {
        ItemCollection activityEntity = new ItemCollection();

        try {
            // test no name attribute
            activityEntity.replaceItemValue("txtActivityResult", "<sometag>some data</sometag>");
            ItemCollection result = workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity,
                    "item", new ItemCollection());
            // we expect a null object as no item tags are included
            Assert.assertNull(result);
        } catch (PluginException e) {
            Assert.fail();
        }

    }

    /**
     * testing empty content, and empty lines (issue #372)
     */
    @Test
    public void testEvaluateWorkflowResultEmptyString() {
        ItemCollection activityEntity = new ItemCollection();
        ItemCollection result = null;
        try {
            // test no content
            activityEntity.replaceItemValue("txtActivityResult", "");
            result = workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity, "item",
                    new ItemCollection());
            Assert.assertNull(result);

            // test whitespace
            activityEntity.replaceItemValue("txtActivityResult", " ");
            result = workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity, "item",
                    new ItemCollection());
            Assert.assertNull(result);

            // test empty lines
            activityEntity.replaceItemValue("txtActivityResult", " \n ");
            result = workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity, "item",
                    new ItemCollection());
            Assert.assertNull(result);

            // test empty lines with valid content between
            String s = "\n";
            s += "<item ignore=\"true\" name=\"comment\" >some data</item>";
            s += "\n ";

            activityEntity.replaceItemValue("txtActivityResult", s);
            result = workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity, "item",
                    new ItemCollection());
            Assert.assertNotNull(result);
            Assert.assertTrue(result.hasItem("comment"));
            Assert.assertEquals("some data", result.getItemValueString("comment"));
            Assert.assertEquals("true", result.getItemValueString("comment.ignore"));

            // test valid content over multiple lines
            s = "\n";
            s += "<item ignore=\"true\" \n";
            s += "name=\"comment\" >some data</item>";
            s += "\n ";

            activityEntity.replaceItemValue("txtActivityResult", s);
            result = workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity, "item",
                    new ItemCollection());
            Assert.assertNotNull(result);
            Assert.assertTrue(result.hasItem("comment"));
            Assert.assertEquals("some data", result.getItemValueString("comment"));
            Assert.assertEquals("true", result.getItemValueString("comment.ignore"));

        } catch (PluginException e) {
            // failed
            Assert.fail();
        }

    }

    /**
     * This test evaluates a event result with garbage around the item tags
     * 
     * @throws PluginException
     */
    @Test
    public void testEvaluateWorkflowResultWithGarbage() {
        ItemCollection activityEntity = new ItemCollection();

        try {
            activityEntity.replaceItemValue("txtActivityResult",
                    ".....<item ignore=\"true\" name=\"comment\" >some data</item>...");
            ItemCollection result = workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity,
                    "item", new ItemCollection());
            Assert.assertNotNull(result);
            Assert.assertTrue(result.hasItem("comment"));
            Assert.assertEquals("some data", result.getItemValueString("comment"));
            Assert.assertEquals("true", result.getItemValueString("comment.ignore"));
        } catch (PluginException e) {
            e.printStackTrace();
            Assert.fail();
        }

        // test an empty item tag
        try {
            activityEntity.replaceItemValue("txtActivityResult",
                    "<garbage1><item ignore=\"true\" name=\"comment\" /></wrongGarbageCloseingTag>");
            ItemCollection result = workflowMockEnvironment.getWorkflowService().evalWorkflowResult(activityEntity,
                    "item", new ItemCollection());
            Assert.assertNotNull(result);
            Assert.assertTrue(result.hasItem("comment"));
            Assert.assertEquals("", result.getItemValueString("comment"));
            Assert.assertEquals("true", result.getItemValueString("comment.ignore"));
        } catch (PluginException e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

}
