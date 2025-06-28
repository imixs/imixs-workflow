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

package org.imixs.workflow.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.bpmn.BPMNUtil;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Test class for WorkflowService
 * <p>
 * This test verifies the evaluation of the workflow result.
 * 
 * @author rsoika
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class TestEvaluateWorkflowResult {

    private final static Logger logger = Logger.getLogger(TestEvaluateWorkflowResult.class.getName());

    protected MockWorkflowEnvironment workflowEngine;

    @BeforeEach
    public void setUp() throws PluginException, ModelException {

        workflowEngine = new MockWorkflowEnvironment();
        workflowEngine.setUp();
        workflowEngine.loadBPMNModelFromFile("/bpmn/TestWorkflowService.bpmn");

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
            ItemCollection result = workflowEngine.workflowService.evalWorkflowResult(activityEntity,
                    "item", new ItemCollection());
            assertNotNull(result);
            assertTrue(result.hasItem("comment"));
            assertEquals("some data", result.getItemValueString("comment"));
            assertEquals("true", result.getItemValueString("comment.ignore"));
        } catch (PluginException e) {
            e.printStackTrace();
            fail();
        }

        // test an empty item tag
        try {
            activityEntity.replaceItemValue("txtActivityResult", "<item ignore=\"true\" name=\"comment\" />");
            ItemCollection result = workflowEngine.workflowService.evalWorkflowResult(activityEntity,
                    "item", new ItemCollection());
            assertNotNull(result);
            assertTrue(result.hasItem("comment"));
            assertEquals("", result.getItemValueString("comment"));
            assertEquals("true", result.getItemValueString("comment.ignore"));
        } catch (PluginException e) {
            e.printStackTrace();
            fail();
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
            ItemCollection result = workflowEngine.workflowService.evalWorkflowResult(activityEntity,
                    "item", new ItemCollection());
            assertNotNull(result);
            assertTrue(result.hasItem("count"));
            assertEquals(55, result.getItemValueInteger("count"));
        } catch (PluginException e) {
            e.printStackTrace();
            fail();
        }

        // test double
        try {
            activityEntity.replaceItemValue("txtActivityResult", "<item name=\"count\" type=\"double\">55.11</item>");
            ItemCollection result = workflowEngine.workflowService.evalWorkflowResult(activityEntity,
                    "item", new ItemCollection());
            assertNotNull(result);
            assertTrue(result.hasItem("count"));
            assertEquals(55.11, result.getItemValueDouble("count"), 0);
        } catch (PluginException e) {
            e.printStackTrace();
            fail();
        }

        // test empty string for Double
        try {
            activityEntity.replaceItemValue("txtActivityResult", "<item name=\"count\" type=\"double\"></item>");
            ItemCollection result = workflowEngine.workflowService.evalWorkflowResult(activityEntity,
                    "item", new ItemCollection());
            assertNotNull(result);
            assertTrue(result.hasItem("count"));
            assertEquals(0.0, result.getItemValueDouble("count"), 0);
        } catch (PluginException e) {
            e.printStackTrace();
            fail();
        }

        // test empty string for Integer
        try {
            activityEntity.replaceItemValue("txtActivityResult", "<item name=\"count\" type=\"integer\"></item>");
            ItemCollection result = workflowEngine.workflowService.evalWorkflowResult(activityEntity,
                    "item", new ItemCollection());
            assertNotNull(result);
            assertTrue(result.hasItem("count"));
            assertEquals(0, result.getItemValueInteger("count"));
        } catch (PluginException e) {
            e.printStackTrace();
            fail();
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

            activityEntity.replaceItemValue(BPMNUtil.EVENT_ITEM_WORKFLOW_RESULT,
                    "<item name=\"count\" type=\"double\"><itemvalue>amount</itemvalue></item>");

            ItemCollection result = workflowEngine.workflowService.evalWorkflowResult(activityEntity,
                    "item", source);
            assertNotNull(result);
            assertTrue(result.hasItem("count"));
            assertEquals(55.123, result.getItemValueDouble("count"), 0);
        } catch (PluginException e) {
            e.printStackTrace();
            fail();
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
        evalItemCollection = workflowEngine.workflowService.evalWorkflowResult(activityEntity, "item",
                new ItemCollection());

        assertTrue(evalItemCollection.hasItem("txtName"));

        List<?> result = evalItemCollection.getItemValue("txtname");

        assertEquals(3, result.size());

        assertTrue(result.contains("Manfred"));
        assertTrue(result.contains("Sam"));
        assertTrue(result.contains("Anna"));

        // test test item
        assertEquals("XXX", evalItemCollection.getItemValueString("test"));
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

        // workflowEngine.getWorkflowService();
        long l = System.currentTimeMillis();
        ItemCollection evalItemCollection = new ItemCollection();
        evalItemCollection = workflowEngine.workflowService.evalWorkflowResult(activityEntity, "item",
                new ItemCollection());

        logger.log(Level.INFO, "...evaluated result in {0}ms...", System.currentTimeMillis() - l);

        // expected: comment_ignore=true
        assertTrue(evalItemCollection.hasItem("comment.ignore"));
        assertTrue(evalItemCollection.getItemValueBoolean("comment.ignore"));

        // expected: action = "home"
        assertTrue(evalItemCollection.hasItem("action"));
        assertEquals("home", evalItemCollection.getItemValueString("action"));

        // now test the different order....
        // we expect the same result
        sResult = "<item name=\"action\">home</item>";
        sResult += "\n<item name=\"comment\" ignore=\"true\"/>";

        activityEntity = new ItemCollection();
        activityEntity.replaceItemValue("txtActivityResult", sResult);
        l = System.currentTimeMillis();
        evalItemCollection = new ItemCollection();
        evalItemCollection = workflowEngine.workflowService.evalWorkflowResult(activityEntity, "item",
                new ItemCollection());
        logger.log(Level.INFO, "...evaluated result in {0}ms...", System.currentTimeMillis() - l);

        // expected: comment_ignore=true
        assertTrue(evalItemCollection.hasItem("comment.ignore"));
        assertTrue(evalItemCollection.getItemValueBoolean("comment.ignore"));

        // expected: action = "home"
        assertTrue(evalItemCollection.hasItem("action"));
        assertEquals("home", evalItemCollection.getItemValueString("action"));
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
            ItemCollection result = workflowEngine.workflowService.evalWorkflowResult(activityEntity,
                    "item", new ItemCollection());
            assertNotNull(result);
            assertTrue(result.hasItem("subprocess_create"));
            String xmlContent = result.getItemValueString("subprocess_create");
            assertFalse(xmlContent.isEmpty());
            assertTrue(xmlContent.contains("<modelversion>analyse-1.0.0</modelversion>"));

            // 2) create test result unix mode.....
            activityResult = "<item name=\"subprocess_create\">\n" + "    <modelversion>analyse-1.0.0</modelversion>\n"
                    + "	    <processid>1000</processid>\n" + "	    <activityid>100</activityid>\n"
                    + "	    <items>_subject,_sender,_receipients,$file</items>\n" + "	</item>";

            activityEntity.replaceItemValue("txtActivityResult", activityResult);
            result = workflowEngine.workflowService.evalWorkflowResult(activityEntity, "item",
                    new ItemCollection());
            assertNotNull(result);
            assertTrue(result.hasItem("subprocess_create"));
            xmlContent = result.getItemValueString("subprocess_create");
            assertFalse(xmlContent.isEmpty());
            assertTrue(xmlContent.contains("<modelversion>analyse-1.0.0</modelversion>"));

            // 3) create test result windows mode.....
            activityResult = "<item name=\"subprocess_create\">\r\n"
                    + "    <modelversion>analyse-1.0.0</modelversion>\r\n" + "	    <processid>1000</processid>\r\n"
                    + "	    <activityid>100</activityid>\r\n"
                    + "	    <items>_subject,_sender,_receipients,$file</items>\r\n" + "	</item>";

            activityEntity.replaceItemValue("txtActivityResult", activityResult);
            result = workflowEngine.workflowService.evalWorkflowResult(activityEntity, "item",
                    new ItemCollection());
            assertNotNull(result);
            assertTrue(result.hasItem("subprocess_create"));
            xmlContent = result.getItemValueString("subprocess_create");
            assertFalse(xmlContent.isEmpty());
            assertTrue(xmlContent.contains("<modelversion>analyse-1.0.0</modelversion>"));
        } catch (PluginException e) {
            e.printStackTrace();
            fail();
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
            workflowEngine.workflowService.evalWorkflowResult(activityEntity, "item",
                    new ItemCollection());
            fail();
        } catch (PluginException e) {
            // ok
        }

        try {
            // test wrong closing tag
            activityEntity.replaceItemValue("txtActivityResult",
                    "<item ignore=\"true\" name=\"comment\" >some data</xitem>");
            workflowEngine.workflowService.evalWorkflowResult(activityEntity, "item",
                    new ItemCollection());
            fail();
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
            ItemCollection result = workflowEngine.workflowService.evalWorkflowResult(activityEntity,
                    "item", new ItemCollection());
            // we expect a null object as no item tags are included
            assertNull(result);
        } catch (PluginException e) {
            fail();
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
            result = workflowEngine.workflowService.evalWorkflowResult(activityEntity, "item",
                    new ItemCollection());
            assertNull(result);

            // test whitespace
            activityEntity.replaceItemValue("txtActivityResult", " ");
            result = workflowEngine.workflowService.evalWorkflowResult(activityEntity, "item",
                    new ItemCollection());
            assertNull(result);

            // test empty lines
            activityEntity.replaceItemValue("txtActivityResult", " \n ");
            result = workflowEngine.workflowService.evalWorkflowResult(activityEntity, "item",
                    new ItemCollection());
            assertNull(result);

            // test empty lines with valid content between
            String s = "\n";
            s += "<item ignore=\"true\" name=\"comment\" >some data</item>";
            s += "\n ";

            activityEntity.replaceItemValue("txtActivityResult", s);
            result = workflowEngine.workflowService.evalWorkflowResult(activityEntity, "item",
                    new ItemCollection());
            assertNotNull(result);
            assertTrue(result.hasItem("comment"));
            assertEquals("some data", result.getItemValueString("comment"));
            assertEquals("true", result.getItemValueString("comment.ignore"));

            // test valid content over multiple lines
            s = "\n";
            s += "<item ignore=\"true\" \n";
            s += "name=\"comment\" >some data</item>";
            s += "\n ";

            activityEntity.replaceItemValue("txtActivityResult", s);
            result = workflowEngine.workflowService.evalWorkflowResult(activityEntity, "item",
                    new ItemCollection());
            assertNotNull(result);
            assertTrue(result.hasItem("comment"));
            assertEquals("some data", result.getItemValueString("comment"));
            assertEquals("true", result.getItemValueString("comment.ignore"));

        } catch (PluginException e) {
            // failed
            fail();
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
            ItemCollection result = workflowEngine.workflowService.evalWorkflowResult(activityEntity,
                    "item", new ItemCollection());
            assertNotNull(result);
            assertTrue(result.hasItem("comment"));
            assertEquals("some data", result.getItemValueString("comment"));
            assertEquals("true", result.getItemValueString("comment.ignore"));
        } catch (PluginException e) {
            e.printStackTrace();
            fail();
        }

        // test an empty item tag
        try {
            activityEntity.replaceItemValue("txtActivityResult",
                    "<garbage1><item ignore=\"true\" name=\"comment\" /></wrongGarbageCloseingTag>");
            ItemCollection result = workflowEngine.workflowService.evalWorkflowResult(activityEntity,
                    "item", new ItemCollection());
            assertNotNull(result);
            assertTrue(result.hasItem("comment"));
            assertEquals("", result.getItemValueString("comment"));
            assertEquals("true", result.getItemValueString("comment.ignore"));
        } catch (PluginException e) {
            e.printStackTrace();
            fail();
        }
    }

    /**
     * This test evaluates a event XML result by calling the the method
     * evalWorkflowResultXMLTag
     * 
     * @throws PluginException
     */
    @Test
    public void testEvaluateWorkflowResultXML() {
        ItemCollection activityEntity = new ItemCollection();

        try {
            activityEntity.replaceItemValue("txtActivityResult",
                    "  <imixs-config name=\"CONFIG\">\n"
                            + "            <items>abc</items>\n"
                            + "            <mode>123</mode>\n"
                            + "        </imixs-config>");
            List<ItemCollection> result = workflowEngine.workflowService.evalWorkflowResultXML(
                    activityEntity,
                    "imixs-config", "CONFIG", new ItemCollection(), false);
            assertNotNull(result);

            assertEquals(1, result.size());

            ItemCollection config = result.get(0);
            assertNotNull(config);
            assertEquals("abc", config.getItemValueString("items"));

        } catch (PluginException e) {
            e.printStackTrace();
            fail();
        }

    }
}
