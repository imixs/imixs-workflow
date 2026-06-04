/****************************************************************************
 * JUnit tests for the refactored XMLParser state machine.
 * Tests focus on cases that the old regex-based implementation handled
 * incorrectly, as well as backward-compatibility with existing behavior.
 ****************************************************************************/

package org.imixs.workflow.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class XMLParserTest {

    // =========================================================================
    // parseTagMatches — core state machine
    // =========================================================================

    @Test
    void testSimpleTag() {
        String text = "Hello <itemvalue>customer.name</itemvalue> world";
        List<XMLParser.TagMatch> matches = XMLParser.parseTagMatches(text, "itemvalue");

        assertEquals(1, matches.size());
        XMLParser.TagMatch m = matches.get(0);
        assertEquals("itemvalue", m.tagName);
        assertEquals("customer.name", m.content);
        assertEquals("<itemvalue>customer.name</itemvalue>", m.fullMatch);
        assertEquals(6, m.startPos);
        assertEquals(42, m.endPos); // 6 + length("<itemvalue>customer.name</itemvalue>") = 6 + 36 = 42
    }

    @Test
    void testTagWithAttributes() {
        String text = "<itemvalue ref=\"agent.ref.invoice\" format=\"###\">invoice.total</itemvalue>";
        List<XMLParser.TagMatch> matches = XMLParser.parseTagMatches(text, "itemvalue");

        assertEquals(1, matches.size());
        XMLParser.TagMatch m = matches.get(0);
        assertEquals("agent.ref.invoice", m.getAttribute("ref"));
        assertEquals("###", m.getAttribute("format"));
        assertEquals("invoice.total", m.content);
    }

    @Test
    void testTagWithSingleQuotedAttribute() {
        String text = "<itemvalue ref='agent.ref.invoice'>invoice.total</itemvalue>";
        List<XMLParser.TagMatch> matches = XMLParser.parseTagMatches(text, "itemvalue");

        assertEquals(1, matches.size());
        assertEquals("agent.ref.invoice", matches.get(0).getAttribute("ref"));
    }

    @Test
    void testMultipleTags() {
        String text = "<itemvalue>first</itemvalue> text <itemvalue>second</itemvalue>";
        List<XMLParser.TagMatch> matches = XMLParser.parseTagMatches(text, "itemvalue");

        assertEquals(2, matches.size());
        assertEquals("first", matches.get(0).content);
        assertEquals("second", matches.get(1).content);
    }

    @Test
    void testCdataIsSkipped() {
        // The <itemvalue> inside CDATA must NOT be matched
        String text = "<![CDATA[<itemvalue>inside.cdata</itemvalue>]]> <itemvalue>real.value</itemvalue>";
        List<XMLParser.TagMatch> matches = XMLParser.parseTagMatches(text, "itemvalue");

        assertEquals(1, matches.size());
        assertEquals("real.value", matches.get(0).content);
    }

    @Test
    void testCdataInsideTagContent() {
        // CDATA inside the content of a tag we ARE looking for
        String text = "<for-each-value item=\"parts\"><![CDATA[<itemvalue>part.id</itemvalue>]]></for-each-value>";
        List<XMLParser.TagMatch> matches = XMLParser.parseTagMatches(text, "for-each-value");

        assertEquals(1, matches.size());
        // The content including CDATA is returned as-is
        assertTrue(matches.get(0).content.contains("CDATA"));
    }

    @Test
    void testCommentIsSkipped() {
        String text = "<!-- <itemvalue>ignored</itemvalue> --> <itemvalue>real</itemvalue>";
        List<XMLParser.TagMatch> matches = XMLParser.parseTagMatches(text, "itemvalue");

        assertEquals(1, matches.size());
        assertEquals("real", matches.get(0).content);
    }

    @Test
    void testNestedSameTag() {
        // Nested tags with the same name — depth counter must track correctly
        String text = "<outer><outer>inner</outer></outer>";
        List<XMLParser.TagMatch> matches = XMLParser.parseTagMatches(text, "outer");

        // Outer match should contain the full nested content
        assertEquals(1, matches.size());
        assertEquals("<outer>inner</outer>", matches.get(0).content);
    }

    @Test
    void testMultilineContent() {
        String text = "<prompt role=\"user\">\n  Please summarize:\n  order.description\n</prompt>";
        List<XMLParser.TagMatch> matches = XMLParser.parseTagMatches(text, "prompt");

        assertEquals(1, matches.size());
        assertEquals("user", matches.get(0).getAttribute("role"));
        assertTrue(matches.get(0).content.contains("Please summarize"));
    }

    @Test
    void testPositionsAreCorrect() {
        // Verify startPos/endPos so adapters can do direct replacement
        // without calling indexOf() (which breaks on duplicate content)
        String text = "AAA<itemvalue>x</itemvalue>BBB<itemvalue>x</itemvalue>CCC";
        List<XMLParser.TagMatch> matches = XMLParser.parseTagMatches(text, "itemvalue");

        assertEquals(2, matches.size());

        // First match starts at position 3 (after "AAA")
        assertEquals(3, matches.get(0).startPos);
        // First match ends right before "BBB"
        assertEquals(text.indexOf("BBB"), matches.get(0).endPos);

        // Second match — same content "x" but at a different position
        assertNotEquals(matches.get(0).startPos, matches.get(1).startPos);

        // Verify that position-based replacement produces the correct result.
        // text = "AAA<itemvalue>x</itemvalue>BBB<itemvalue>x</itemvalue>CCC"
        // Replacing the first match by position yields:
        // "AAA" + "REPLACED" + "BBB<itemvalue>x</itemvalue>CCC"
        String replaced = text.substring(0, matches.get(0).startPos)
                + "REPLACED"
                + text.substring(matches.get(0).endPos);
        assertEquals("AAAREPLACEDBB" + "B<itemvalue>x</itemvalue>CCC", replaced);
    }

    @Test
    void testSelfClosingTagIncludedInFindTags() {
        // findTags() must return self-closing tags for backward compatibility
        String text = "<itemvalue/> <itemvalue>real</itemvalue>";
        List<XMLParser.TagMatch> matches = XMLParser.parseTagMatches(text, "itemvalue");

        // Both tags are returned: the self-closing one with empty content,
        // and the normal one with its content
        assertEquals(2, matches.size());
        assertEquals("", matches.get(0).content);
        assertEquals("real", matches.get(1).content);
    }

    @Test
    void testFindNoEmptyTagsExcludesBothEmptyForms() {
        // findNoEmptyTags() must exclude both <tag/> and <tag></tag>
        String text = "<itemvalue/> <itemvalue></itemvalue> <itemvalue>real</itemvalue>";
        List<String> tags = XMLParser.findNoEmptyTags(text, "itemvalue");

        assertEquals(1, tags.size());
        assertTrue(tags.get(0).contains("real"));
    }

    @Test
    void testEmptyText() {
        List<XMLParser.TagMatch> matches = XMLParser.parseTagMatches("", "itemvalue");
        assertTrue(matches.isEmpty());
    }

    @Test
    void testNullText() {
        List<XMLParser.TagMatch> matches = XMLParser.parseTagMatches(null, "itemvalue");
        assertTrue(matches.isEmpty());
    }

    @Test
    void testTagNotFound() {
        List<XMLParser.TagMatch> matches = XMLParser.parseTagMatches("no tags here", "itemvalue");
        assertTrue(matches.isEmpty());
    }

    @Test
    void testCaseInsensitiveTagName() {
        // Tag names should match case-insensitively
        String text = "<ItemValue>customer.name</ItemValue>";
        List<XMLParser.TagMatch> matches = XMLParser.parseTagMatches(text, "itemvalue");

        assertEquals(1, matches.size());
        assertEquals("customer.name", matches.get(0).content);
    }

    // =========================================================================
    // parseAttributes — standalone attribute parser
    // =========================================================================

    @Test
    void testParseAttributesDoubleQuote() {
        Map<String, String> attrs = XMLParser.parseAttributes(
                "<itemvalue ref=\"agent.ref.invoice\" format=\"###\">");
        assertEquals("agent.ref.invoice", attrs.get("ref"));
        assertEquals("###", attrs.get("format"));
    }

    @Test
    void testParseAttributesSingleQuote() {
        Map<String, String> attrs = XMLParser.parseAttributes(
                "<itemvalue ref='my.ref' separator=', '>");
        assertEquals("my.ref", attrs.get("ref"));
        assertEquals(", ", attrs.get("separator"));
    }

    @Test
    void testParseAttributesNoAttributes() {
        Map<String, String> attrs = XMLParser.parseAttributes("<itemvalue>");
        assertTrue(attrs.isEmpty());
    }

    @Test
    void testFindAttribute() {
        String tag = "<itemvalue ref=\"agent.ref.invoice\">foo</itemvalue>";
        assertEquals("agent.ref.invoice", XMLParser.findAttribute(tag, "ref"));
        assertNull(XMLParser.findAttribute(tag, "nonexistent"));
    }

    // =========================================================================
    // Public API backward compatibility
    // =========================================================================

    @Test
    void testFindTagsBackwardCompatible() {
        String text = "Hello <itemvalue>$creator</itemvalue> world";
        List<String> tags = XMLParser.findTags(text, "itemvalue");

        assertEquals(1, tags.size());
        assertEquals("<itemvalue>$creator</itemvalue>", tags.get(0));
    }

    @Test
    void testFindNoEmptyTagsBackwardCompatible() {
        String text = "<for-each-value item=\"parts\">Part: <itemvalue>part.id</itemvalue></for-each-value>";
        List<String> tags = XMLParser.findNoEmptyTags(text, "for-each-value");

        assertEquals(1, tags.size());
        assertTrue(tags.get(0).startsWith("<for-each-value"));
    }

    @Test
    void testFindTagValue() {
        String text = "<itemvalue>customer.name</itemvalue>";
        assertEquals("customer.name", XMLParser.findTagValue(text, "itemvalue"));
    }

    @Test
    void testFindTagValueNotFound() {
        assertEquals("", XMLParser.findTagValue("no tag here", "itemvalue"));
    }

    @Test
    void testFindTagValues() {
        String text = "<itemvalue>first</itemvalue><itemvalue>second</itemvalue>";
        List<String> values = XMLParser.findTagValues(text, "itemvalue");

        assertEquals(2, values.size());
        assertEquals("first", values.get(0));
        assertEquals("second", values.get(1));
    }

    // =========================================================================
    // The critical CDATA case that broke the old regex
    // =========================================================================

    @Test
    void testItemValueInsidePromptWithCdata() {
        // This is the real-world Prompt Template case.
        // The old findTags() regex broke here because [^<]+ can't handle '<' in CDATA.
        String text = "<prompt role=\"user\">\n"
                + "  <![CDATA[\n"
                + "    Please summarize: <itemvalue>order.description</itemvalue>\n"
                + "  ]]>\n"
                + "</prompt>\n"
                + "<itemvalue ref=\"$workitemref\">customer.name</itemvalue>";

        // The <itemvalue> inside CDATA must NOT be found
        List<String> itemvalues = XMLParser.findTags(text, "itemvalue");
        assertEquals(1, itemvalues.size());
        assertEquals("customer.name", XMLParser.findTagValue(itemvalues.get(0), "itemvalue"));

        // The prompt tag itself must be found with its content intact
        List<String> prompts = XMLParser.findTags(text, "prompt");
        assertEquals(1, prompts.size());
        assertEquals("user", XMLParser.findAttribute(prompts.get(0), "role"));
    }

    @Test
    void testRefAttributeOnItemValue() {
        // The new ref= concept that triggered this whole refactoring
        String text = "<itemvalue ref=\"agent.ref.invoice\" format=\"#,###.00\" locale=\"de_DE\">invoice.total</itemvalue>";
        List<String> tags = XMLParser.findTags(text, "itemvalue");

        assertEquals(1, tags.size());
        String tag = tags.get(0);
        assertEquals("agent.ref.invoice", XMLParser.findAttribute(tag, "ref"));
        assertEquals("#,###.00", XMLParser.findAttribute(tag, "format"));
        assertEquals("de_DE", XMLParser.findAttribute(tag, "locale"));
        assertEquals("invoice.total", XMLParser.findTagValue(tag, "itemvalue"));
    }
}