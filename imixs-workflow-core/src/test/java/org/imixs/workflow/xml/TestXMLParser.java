package org.imixs.workflow.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Map;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.util.XMLParser;
import org.junit.jupiter.api.Test;

/**
 * Test class for XMLParser
 * 
 * @author rsoika
 * 
 */
public class TestXMLParser {

    @Test
    public void testAllAttributes() {

        String test = "<date field=\"a\"   number='1' />";

        Map<String, String> result = XMLParser.findAttributes(test);

        System.out.println(result);

        assertNotNull(result);

        assertTrue(result.containsKey("field"));
        assertTrue(result.containsKey("number"));

        assertEquals("a", result.get("field"));
        assertEquals("1", result.get("number"));
    }

    @Test
    public void testSingleAttribute() {

        String test = "<date field=\"a\"   number='1' />";

        String result = XMLParser.findAttribute(test, "field");
        assertNotNull(result);
        assertEquals("a", result);

        result = XMLParser.findAttribute(test, "number");
        assertNotNull(result);
        assertEquals("1", result);
    }

    @Test
    public void testAttributeWithDoubleQuotation() {

        String test = "<item name=\"txtName\" type=\"x\">true</item>";

        // verify attribute
        String result = XMLParser.findAttribute(test, "name");
        assertNotNull(result);
        assertEquals("txtName", result);

        result = XMLParser.findAttribute(test, "type");
        assertNotNull(result);
        assertEquals("x", result);
    }

    @Test
    public void testAttributeWithSingleQuotation() {

        String test = "<item name='txtName' type='x'>true</item>";

        // verify attribute
        String result = XMLParser.findAttribute(test, "name");
        assertNotNull(result);
        assertEquals("txtName", result);

        result = XMLParser.findAttribute(test, "type");
        assertNotNull(result);
        assertEquals("x", result);
    }

    @Test
    public void testSingleAttributeUppercase() {
        String test = "<date FIELD=\"a\"   nUMBER='1' />";
        String result = XMLParser.findAttribute(test, "FIELD");
        assertNotNull(result);
        assertEquals("a", result);

        result = XMLParser.findAttribute(test, "nUMBER");
        assertNotNull(result);
        assertEquals("1", result);
    }

    /**
     * issue #201
     */
    @Test
    public void testSingleAttributeWithDash() {

        String test = "<format aggreation-type=\"SUM\">some data</format>";

        // verify attribute
        String result = XMLParser.findAttribute(test, "aggreation-type");
        assertNotNull(result);
        assertEquals("SUM", result);

        // verify value
        List<String> values = XMLParser.findTagValues(test, "format");
        assertNotNull(values);
        assertEquals(1, values.size());
        assertEquals("some data", values.get(0));

    }

    @Test
    public void testBooleanAttribute() {

        String test = "<item name=\"txtName\" type=\"boolean\">true</item>";

        // verify attribute
        String result = XMLParser.findAttribute(test, "name");
        assertNotNull(result);
        assertEquals("txtName", result);

        result = XMLParser.findAttribute(test, "type");
        assertNotNull(result);
        assertEquals("boolean", result);

        // verify attribute
        List<String> values = XMLParser.findTagValues(test, "item");
        assertNotNull(values);
        assertEquals(1, values.size());
        assertEquals("true", values.get(0));

    }

    @Test
    public void testSingelTags() {

        // test simple tag <date>..</date>
        String test = "abc <date field=\"a\"   number=1 >def</date>ghi";
        List<String> result = XMLParser.findTags(test, "date");
        System.out.println(result);
        assertNotNull(result);
        assertEquals("<date field=\"a\"   number=1 >def</date>", result.get(0));

        // test simple singel tag <date/>
        test = "abc <date field=\"a\"   number=1 />def";
        result = XMLParser.findTags(test, "date");
        System.out.println(result);
        assertNotNull(result);
        assertEquals("<date field=\"a\"   number=1 />", result.get(0));

        // test simple singel tag lowercase <DATE/>
        test = "abc <DATE field=\"a\"   number=1 />def";
        result = XMLParser.findTags(test, "date");
        System.out.println(result);
        assertNotNull(result);
        assertEquals("<DATE field=\"a\"   number=1 />", result.get(0));

        // test simple tag <DATE>..</DATE>
        test = "abc <DATE field=\"a\"   number=1 >def</DATE>ghi";
        result = XMLParser.findTags(test, "date");
        System.out.println(result);
        assertNotNull(result);
        assertEquals("<DATE field=\"a\"   number=1 >def</DATE>", result.get(0));

    }

    @Test
    public void testMultiTags() {

        // test simple tag <date>..</date> <date />
        String test = "abc <date field=\"a\"   number=1 >def</date>ghi\n" + "<date>abc</date>";
        List<String> result = XMLParser.findTags(test, "date");
        System.out.println(result);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("<date field=\"a\"   number=1 >def</date>", result.get(0));
        assertEquals("<date>abc</date>", result.get(1));

        // test complex list of tags
        test = "abc <date field=\"a\"   number=1 >def</date>ghi\n" + "<item name=\"test\">value</item>"
                + "<date>abc</date>" + "<date field=\"abc\">xyz</date>";
        result = XMLParser.findTags(test, "date");
        System.out.println(result);
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("<date field=\"a\"   number=1 >def</date>", result.get(0));
        assertEquals("<date>abc</date>", result.get(1));
        assertEquals("<date field=\"abc\">xyz</date>", result.get(2));

    }

    @Test
    public void testFindTagValues() {

        String test = "<date field=\"a\"   number=1 >2016-12-31</date>";

        List<String> result = XMLParser.findTagValues(test, "date");
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("2016-12-31", result.get(0));

        test = "<date>2016-12-31</date>  <date>2016-11-30</date>";

        result = XMLParser.findTagValues(test, "date");
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("2016-12-31", result.get(0));
        assertEquals("2016-11-30", result.get(1));

    }

    /**
     * Find the value of a single tag
     */
    @Test
    public void testFindSingleTagValue() {
        String test = "<itemvalue>namcreator</itemvalue>";
        String result = XMLParser.findTagValue(test, "itemvalue");
        assertNotNull(result);
        assertEquals("namcreator", result);

    }

    /**
     * Find the value of a single tag
     */
    @Test
    public void testFindSingleTagValueWithAttribute() {

        String test = "<itemvalue name=\"txtname\">anna</itemvalue>";
        String result = XMLParser.findTagValue(test, "itemvalue");
        assertNotNull(result);
        assertEquals("anna", result);

    }

    /**
     * Find the value of a single tag
     */
    @Test
    public void testFindSingleTagValueWithAttributes() {

        String test = "<date field=\"a\"   number=1 >2016-12-31</date>";

        String result = XMLParser.findTagValue(test, "date");
        assertNotNull(result);
        assertEquals("2016-12-31", result);

    }

    /**
     * Find the value of a single tag in a string with multible tags.
     */
    @Test
    public void testFindSingleTagValueWithinMultibleTags() {

        String test = "<date>2016-12-31</date>  <date>2016-11-30</date>";

        String result = XMLParser.findTagValue(test, "date");
        assertNotNull(result);
        // we expect the first value
        assertEquals("2016-12-31", result);

    }

    /**
     * Find the complex value of a tag
     */
    @Test
    public void testFindTagComplex() {
        String test = "<for-each item=\"productlist\">{\"name\":\"xx\"}\n</for-each> ";
        String result = XMLParser.findTagValue(test, "for-each");

        assertNotNull(result);
        assertEquals("{\"name\":\"xx\"}\n", result);
    }

    /**
     * Test the value of an empty tag
     */
    @Test
    public void testFindEmptyTag() {
        String test = "<item x=\"y\"/>";
        String result = XMLParser.findTagValue(test, "item");
        assertEquals("", result);
    }

    /**
     * Test a typical string used by the evaluateWorkflowResutl method.
     ***/
    @Test
    public void testItemXMLContent() {

        // create test result.....
        String activityResult = "<modelversion>1.0.0</modelversion>" + "<processid>1000</processid>"
                + "<activityid>10</activityid>" + "<items>namTeam</items>";

        try {
            ItemCollection result = XMLParser.parseItemStructure(activityResult);

            assertEquals("1.0.0", result.getItemValueString("modelversion"));
            assertEquals("1000", result.getItemValueString("processID"));
            assertEquals("10", result.getItemValueString("activityID"));
        } catch (PluginException e) {

            e.printStackTrace();
            fail();
        }
    }

    /**
     * Test a special xml string containing whitespace
     ***/
    @Test
    public void testItemXMLContentWhitespace() {
        // create test result.....
        String activityResult = "\n" + "    <processid>2000</processid>\n"
                + "    <items>_subject|_parentsubject,$workflowsummary|_parentworkflowsummary</items>\n"
                + "    <activityid>100</activityid>";
        try {
            ItemCollection result = XMLParser.parseItemStructure(activityResult);
            String s = result.getItemValueString("processid");
            assertFalse(s.isEmpty());
        } catch (PluginException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testParseTag() {

        String data = "<api>" + "	<method>GET</method>\n" + "	<resource>http://ergo/details/100</resource>\n"
                + "	<mediatype>XML</mediatype>\n" + "	<items>customer.firstname,customer.lastname</items>\n"
                + "</api>";

        ItemCollection apiData;
        try {
            apiData = XMLParser.parseTag(data, "api");
            assertEquals("GET", apiData.getItemValueString("method"));
            assertEquals("XML", apiData.getItemValueString("mediatype"));
            assertEquals("customer.firstname,customer.lastname", apiData.getItemValueString("items"));

        } catch (PluginException e) {
            e.printStackTrace();
        }

    }

    /**
     * This test parses an event model tag used in an event entity to switch the
     * model version
     * 
     */
    @Test
    public void testParseEventModelTag() {

        String data = "<!-- test --> <model>\n"
                + "   <version>1.42.0</version>\n"
                + "   <event>10</event>\n"
                + "</model>";

        ItemCollection modelData;
        try {
            modelData = XMLParser.parseTag(data, "model");
            assertEquals("1.42.0", modelData.getItemValueString("version"));
            assertEquals(10, modelData.getItemValueInteger("event"));
        } catch (PluginException e) {
            e.printStackTrace();
        }

    }

    /**
     * Simple test showing how to find tags...
     * 
     * <pre>
     * {@code
     * <model>     
     *   <version>sub-model-1.0.0</version>
     *   <task>1000</task>
     *   <event>10</event>
     *  </model>
     * }
     * </pre>
     * 
     * and a variant we an embedded model tag...
     *
     * <pre>
     * {@code
     *   <imixs-micro name="CREATE">
     *   <endpoint>ws://workstation-1:8080/workflow</endpoint>
     *   <model>workstation-1-1.0.0</model>
     *   <task>1000</task>
     *   <event>10</event>
     *   <items>order.number;order.date</items>
     *   <debug>true</debug>
     *   </imixs-micro>
     *
     * }
     * </pre>
     * 
     */
    @Test
    public void testEmbeddedParseTagList() {
        // test typical model tag used by WorkflowKernel
        String xmlString = "<model>\n"
                + "    <version>sub-model-1.0.0</version>\n"
                + "   <task>1000</task>\n"
                + "   <event>10</event>\n"
                + " </model>";

        List<String> result = XMLParser.findNoEmptyXMLTags(xmlString, "model");
        // assert
        assertEquals(1, result.size());
        assertTrue(result.get(0).startsWith("<model"));

        // now test a xml with embedded model tag
        xmlString = " <imixs-micro name=\"CREATE\">\n"
                + "    <endpoint>ws://workstation-1:8080/workflow</endpoint>\n"
                + "    <model>workstation-1-1.0.0</model>\n"
                + "    <task>1000</task>\n"
                + "    <event>10</event>\n"
                + "    <items>order.number;order.date</items>\n"
                + "    <debug>true</debug>\n"
                + "  </imixs-micro>";
        result = XMLParser.findNoEmptyXMLTags(xmlString, "model");
        // not match expected
        assertEquals(0, result.size());

        // test full tag
        result = XMLParser.findNoEmptyXMLTags(xmlString, "imixs-micro");
        // assert
        assertEquals(1, result.size());
        assertTrue(result.get(0).startsWith("<imixs-micro"));
    }

    /**
     * Test variants with embedded tags
     */
    @Test
    public void testParseTagList() {
        // arrange
        String xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<library>\n" +
                "    <book>\n" +
                "        <title>Harry Potter and the Philosopher's Stone</title>\n" +
                "        <author>J.K. Rowling</author>\n" +
                "    </book>\n" +
                "    <book>\n" +
                "        <title>The Great Gatsby</title>\n" +
                "        <author>F. Scott Fitzgerald</author>\n" +
                "    </book>\n" +
                "</library>";
        try {
            // act
            List<ItemCollection> result = XMLParser.parseTagList(xmlString, "book");
            // assert
            assertEquals(2, result.size());
            assertEquals("Harry Potter and the Philosopher's Stone", result.get(0).getItemValue("title").get(0));
        } catch (PluginException e) {
        }
    }

}
