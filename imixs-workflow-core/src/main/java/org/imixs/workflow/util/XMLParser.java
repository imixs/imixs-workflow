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

package org.imixs.workflow.util;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.PluginException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * XMLParser provides utility methods to search for and extract XML tags and
 * their attribute values from text fragments. It is used throughout the Imixs
 * Workflow engine wherever text templates are resolved.
 *
 * <h2>Design goals</h2>
 * <ul>
 * <li><b>Performance:</b> Tag scanning uses a linear state machine (O(n)) with
 * no regex backtracking and no DOM allocation. This is safe for high-frequency
 * calls on small-to-medium text templates.</li>
 * <li><b>Correctness:</b> CDATA sections ({@code <![CDATA[...]]>}) and XML
 * comments ({@code <!-- ... -->}) are skipped during scanning so that tags
 * embedded in them are never matched accidentally.</li>
 * </ul>
 *
 * <h2>Two parsing strategies</h2>
 * <ol>
 * <li><b>State machine ({@link #parseTagMatches})</b> — used for all tag-search
 * operations ({@code findTags}, {@code findTagValue}, etc.). Returns
 * {@link TagMatch} objects that carry exact start/end positions, avoiding the
 * fragile {@code indexOf(fullTag)} pattern that breaks when the same tag
 * content appears more than once.</li>
 * <li><b>DOM parser ({@link #parseTag})</b> — used only for structured
 * configuration blocks where the full XML object model is needed.</li>
 * </ol>
 */
public class XMLParser {

    private static final Logger logger = Logger.getLogger(XMLParser.class.getName());

    // =========================================================================
    // Core: state machine tag scanner
    // =========================================================================

    /**
     * Scans {@code text} for all occurrences of the named XML tag and returns one
     * {@link TagMatch} per occurrence.
     *
     * <p>
     * The scanner is a single-pass, character-level state machine. It does
     * <em>not</em> use regular expressions and does <em>not</em> build a DOM tree,
     * so it remains fast and allocation-light even when called frequently on small
     * texts.
     * </p>
     *
     * <h3>What the scanner handles correctly</h3>
     * <ul>
     * <li><b>CDATA blocks</b> ({@code <![CDATA[...]]>}): skipped entirely — any tag
     * that happens to appear inside CDATA is <em>not</em> matched. This fixes the
     * primary bug in the old regex approach, which broke on prompt templates that
     * used CDATA to protect inner XML.</li>
     * <li><b>XML comments</b> ({@code <!-- ... -->}): skipped entirely.</li>
     * <li><b>Attributes</b>: the full open tag including all attributes is parsed
     * and made available on the returned {@link TagMatch}.</li>
     * <li><b>Multiline content</b>: content between open and close tag may span any
     * number of lines.</li>
     * <li><b>Case-insensitive tag names</b>: {@code <ItemValue>} matches a search
     * for {@code "itemvalue"}.</li>
     * <li><b>Nested same-name tags</b>: depth counter ensures that the correct
     * closing tag is selected even when the tag is nested inside itself.</li>
     * <li><b>Self-closing tags</b> ({@code <tag/>}): skipped, because they carry no
     * inner content.</li>
     * <li><b>Malformed tags</b> (missing {@code >}): skipped gracefully.</li>
     * </ul>
     *
     * <h3>Outer scan loop — step by step</h3>
     * <ol>
     * <li>Skip any {@code <![CDATA[...]]>} block at the current position.</li>
     * <li>Skip any {@code <!-- ... -->} comment at the current position.</li>
     * <li>If the current character is not {@code '<'}, advance and repeat.</li>
     * <li>Read the tag name following {@code '<'}. If it starts with {@code '/'} it
     * is a close tag — skip.</li>
     * <li>Compare the tag name (lowercase) against {@code tagLower}. No match —
     * advance and repeat.</li>
     * <li>Locate the closing {@code '>'} of the open tag. If absent the text is
     * malformed — skip.</li>
     * <li>Check for self-closing ({@code />}) — skip if found.</li>
     * <li>Parse attributes from the open tag string.</li>
     * <li>Enter the <b>inner scan loop</b> to find the matching close tag.</li>
     * </ol>
     *
     * <h3>Inner scan loop — finding the matching close tag</h3>
     * <p>
     * Starting right after the open tag's {@code '>'}, the inner loop scans forward
     * maintaining a {@code depth} counter (initially 1):
     * </p>
     * <ul>
     * <li>CDATA blocks inside the content are skipped.</li>
     * <li>A nested open tag of the same name increments {@code depth}.</li>
     * <li>A close tag of the same name decrements {@code depth}.</li>
     * <li>When {@code depth} reaches 0 the matching close tag has been found. A
     * {@link TagMatch} is built from the accumulated data and added to the result
     * list.</li>
     * </ul>
     * <p>
     * After a successful match the outer loop continues <em>after</em> the close
     * tag, so adjacent tags are found correctly. When no matching close tag exists
     * the open tag is skipped and scanning resumes after it.
     * </p>
     *
     * @param text the input text to scan; may be {@code null} or empty
     * @param tag  the XML tag name to search for (case-insensitive)
     * @return ordered list of {@link TagMatch} objects; empty if nothing found
     * @throws PluginException
     */
    public static List<XMLTag> parseTagMatches(String text, String tag) {
        List<XMLTag> result = new ArrayList<>();
        if (text == null || text.isEmpty() || tag == null || tag.isEmpty()) {
            return result;
        }

        final String tagLower = tag.toLowerCase();
        final int len = text.length();
        int openTagCount = 0; // counts every opening tag found
        int i = 0; // current position in the outer scan loop

        while (i < len) {

            // ------------------------------------------------------------------
            // Step 1: skip CDATA blocks — <![CDATA[ ... ]]>
            // Any tag appearing inside CDATA is literal text, not markup.
            // ------------------------------------------------------------------
            if (i + 8 < len && text.startsWith("<![CDATA[", i)) {
                int cdataEnd = text.indexOf("]]>", i + 9);
                // If no closing ]]> is found the rest of the text is inside CDATA.
                i = (cdataEnd < 0) ? len : cdataEnd + 3;
                continue;
            }

            // ------------------------------------------------------------------
            // Step 2: skip XML comments — <!-- ... -->
            // ------------------------------------------------------------------
            if (i + 3 < len && text.startsWith("<!--", i)) {
                int commentEnd = text.indexOf("-->", i + 4);
                i = (commentEnd < 0) ? len : commentEnd + 3;
                continue;
            }

            // ------------------------------------------------------------------
            // Step 3: advance until we hit the start of a tag
            // ------------------------------------------------------------------
            if (text.charAt(i) != '<') {
                i++;
                continue;
            }

            // ------------------------------------------------------------------
            // Step 4: read the tag name that follows '<'
            // Close tags (</name>) are not open tags — skip them.
            // ------------------------------------------------------------------
            int nameStart = i + 1;
            if (nameStart < len && text.charAt(nameStart) == '/') {
                // This is a close tag </...> — not an open tag, skip the '<'
                i++;
                continue;
            }

            // Collect characters of the tag name (stops at whitespace, '>' or '/')
            int nameEnd = nameStart;
            while (nameEnd < len
                    && !Character.isWhitespace(text.charAt(nameEnd))
                    && text.charAt(nameEnd) != '>'
                    && text.charAt(nameEnd) != '/') {
                nameEnd++;
            }

            // ------------------------------------------------------------------
            // Step 5: compare tag name (case-insensitive)
            // ------------------------------------------------------------------
            String foundName = text.substring(nameStart, nameEnd).toLowerCase();
            if (!foundName.equals(tagLower)) {
                // Not the tag we are looking for — move past '<' and continue
                i++;
                continue;
            }

            // ------------------------------------------------------------------
            // Step 6: locate the closing '>' of the open tag
            // ------------------------------------------------------------------
            int openTagEnd = text.indexOf('>', nameEnd);
            if (openTagEnd < 0) {
                // Malformed tag with no closing '>' — skip and continue
                i++;
                continue;
            }

            // ------------------------------------------------------------------
            // Step 7: self-closing tag check <tag ... />
            // Self-closing tags have no inner content. They are returned as a
            // TagMatch with an empty content string so that findTags() includes
            // them (backward compatible), while findNoEmptyTags() can filter
            // them out together with explicit empty tags <tag></tag>.
            // ------------------------------------------------------------------
            String openTag = text.substring(i, openTagEnd + 1);
            Map<String, String> attributes = parseAttributes(openTag);

            if (text.charAt(openTagEnd - 1) == '/') {
                // Self-closing tag — add with empty content and advance past it
                result.add(new XMLTag(openTag, tagLower, attributes, "", i, openTagEnd + 1));
                i = openTagEnd + 1;
                continue;
            }

            // We found a non-self-closing open tag — count it
            openTagCount++;  // ← NEU

            // ------------------------------------------------------------------
            // Step 8: parse attributes from the open tag
            // e.g. <itemvalue ref="agent.ref.invoice" format="###">
            // ------------------------------------------------------------------

            // ------------------------------------------------------------------
            // Step 9: inner scan loop — find the matching close tag
            //
            // We track nesting depth so that a tag nested inside itself is
            // handled correctly. depth starts at 1 (we are inside one open tag).
            // It is incremented for every nested open tag of the same name, and
            // decremented for every close tag of the same name. When it reaches 0
            // we have found the close tag that matches our original open tag.
            //
            // We use the sentinel value -1 to signal a successful match to the
            // outer loop (Java does not support labelled break with a return value).
            // ------------------------------------------------------------------
            final String openTagStr = "<" + tagLower; // prefix of a nested open tag
            final String closeTagStr = "</" + tagLower; // prefix of a close tag
            int depth = 1;
            int searchPos = openTagEnd + 1; // start scanning right after the open tag's '>'

            while (searchPos < len && depth > 0) {

                // Skip CDATA blocks inside the tag content
                if (searchPos + 8 < len && text.startsWith("<![CDATA[", searchPos)) {
                    int cdataEnd = text.indexOf("]]>", searchPos + 9);
                    searchPos = (cdataEnd < 0) ? len : cdataEnd + 3;
                    continue;
                }

                // Only angle brackets are interesting from here on
                if (text.charAt(searchPos) != '<') {
                    searchPos++;
                    continue;
                }

                // Check for a nested open tag of the same name.
                // We guard with an extra character check to avoid false positives:
                // "<itemvalueExtra>" must not match a search for "<itemvalue".
                if (searchPos + openTagStr.length() < len) {
                    String ahead = text.substring(searchPos,
                            searchPos + openTagStr.length()).toLowerCase();
                    if (ahead.equals(openTagStr)) {
                        int afterName = searchPos + openTagStr.length();
                        if (afterName < len) {
                            char c = text.charAt(afterName);
                            if (Character.isWhitespace(c) || c == '>' || c == '/') {
                                // Confirmed: this is a nested open tag of the same name
                                depth++;
                                searchPos++;
                                continue;
                            }
                        }
                    }
                }

                // Check for a close tag of the same name.
                if (searchPos + closeTagStr.length() <= len) {
                    String ahead = text.substring(searchPos,
                            searchPos + closeTagStr.length()).toLowerCase();
                    if (ahead.equals(closeTagStr)) {
                        depth--;
                        if (depth == 0) {
                            // This is the matching close tag.
                            // Locate its closing '>' to determine the full match boundary.
                            int closeTagEnd = text.indexOf('>', searchPos);
                            if (closeTagEnd < 0) {
                                // Malformed close tag — abort inner loop
                                break;
                            }
                            // Extract inner content: everything between the open tag's '>'
                            // and the start of the close tag.
                            String content = text.substring(openTagEnd + 1, searchPos);
                            String fullMatch = text.substring(i, closeTagEnd + 1);

                            result.add(new XMLTag(
                                    fullMatch, tagLower, attributes,
                                    content, i, closeTagEnd + 1));

                            // Advance the outer loop past the close tag
                            i = closeTagEnd + 1;

                            // Sentinel: signal to the outer if-check that we succeeded
                            depth = -1;
                            break;
                        }
                    }
                }

                searchPos++;
            } // end inner scan loop

            if (depth != -1) {
                // The inner loop exhausted the text without finding a matching close tag.
                // Skip past the open tag and continue the outer scan.
                i = openTagEnd + 1;
            }

            // When depth == -1 the outer loop variable i was already updated above.

        } // end outer scan loop

        // After the outer scan loop: verify that all opening tags were properly closed.
        // If parseTagMatches found fewer results than opening tags, at least one tag
        // was never closed — this is a modelling error.
        if (result.size() < openTagCount) {
            System.out.println("openTagCount=" + openTagCount + " result.size()=" + result.size());

            throw new IllegalArgumentException(
                    "unclosed <" + tag + "> tag detected in text: " + text);
        }

        return result;
    }

    // =========================================================================
    // Core: attribute parser
    // =========================================================================

    /**
     * Parses all attributes from an XML open tag string and returns them as a
     * name-to-value map.
     *
     * <p>
     * Input example:
     * </p>
     * 
     * <pre>
     *   &lt;itemvalue ref="agent.ref.invoice" format="###" locale='de_DE'&gt;
     * </pre>
     * <p>
     * Returns:
     * {@code {ref -> "agent.ref.invoice", format -> "###", locale -> "de_DE"}}
     * </p>
     *
     * <p>
     * The parser is tolerant of:
     * </p>
     * <ul>
     * <li>Double-quoted attribute values ({@code attr="value"})</li>
     * <li>Single-quoted attribute values ({@code attr='value'})</li>
     * <li>Unquoted attribute values (non-standard, but accepted)</li>
     * <li>Arbitrary whitespace between attributes</li>
     * </ul>
     *
     * <p>
     * Attribute names are normalised to lowercase so that callers can look them up
     * in a case-insensitive manner.
     * </p>
     *
     * @param openTag the full open tag string including angle brackets, e.g.
     *                {@code <itemvalue ref="x">}
     * @return map of attribute name (lowercase) to attribute value; empty map if
     *         the tag has no attributes
     */
    static Map<String, String> parseAttributes(String openTag) {
        Map<String, String> result = new HashMap<>();
        final int len = openTag.length();

        // Skip past '<' and the tag name to reach the first attribute (if any).
        int i = 1;
        while (i < len
                && !Character.isWhitespace(openTag.charAt(i))
                && openTag.charAt(i) != '>'
                && openTag.charAt(i) != '/') {
            i++;
        }

        while (i < len) {
            char c = openTag.charAt(i);
            if (c == '>' || c == '/')
                break;
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            int attrNameStart = i;
            while (i < len
                    && openTag.charAt(i) != '='
                    && openTag.charAt(i) != '>'
                    && !Character.isWhitespace(openTag.charAt(i))) {
                i++;
            }
            // Preserve original case — lookup is done case-insensitively in findAttribute()
            String attrName = openTag.substring(attrNameStart, i);

            while (i < len
                    && (openTag.charAt(i) == '=' || Character.isWhitespace(openTag.charAt(i)))) {
                i++;
            }
            if (i >= len)
                break;

            char quote = openTag.charAt(i);
            if (quote == '"' || quote == '\'') {
                i++;
                int valueStart = i;
                while (i < len && openTag.charAt(i) != quote) {
                    i++;
                }
                result.put(attrName, openTag.substring(valueStart, i));
                i++;
            } else {
                int valueStart = i;
                while (i < len
                        && !Character.isWhitespace(openTag.charAt(i))
                        && openTag.charAt(i) != '>') {
                    i++;
                }
                result.put(attrName, openTag.substring(valueStart, i));
            }
        }
        return result;
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Returns all attributes of a tag string as a name-to-value map.
     *
     * <p>
     * Delegates to {@link #parseAttributes(String)}. Provided for backward
     * compatibility; new code may call {@code parseAttributes} directly.
     * </p>
     *
     * @param content an XML open tag string, e.g. {@code <itemvalue ref="x">}
     * @return map of lowercase attribute names to their values
     */
    public static Map<String, String> findAttributes(String content) {
        return parseAttributes(content);
    }

    /**
     * Returns the value of a single named attribute from a tag string, or
     * {@code null} if the attribute is not present.
     *
     * <p>
     * Example:
     * </p>
     * 
     * <pre>
     * findAttribute("&lt;itemvalue ref=\"agent.ref.invoice\"&gt;", "ref")
     * // returns "agent.ref.invoice"
     * </pre>
     *
     * @param content an XML open tag string
     * @param name    the attribute name to look up (case-insensitive)
     * @return the attribute value, or {@code null}
     */
    public static String findAttribute(String content, String name) {
        if (name == null)
            return null;
        // Case-insensitive lookup — attribute names preserve their original case
        // in the map, so we compare both sides lowercased.
        for (Map.Entry<String, String> entry : parseAttributes(content).entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Finds all occurrences of a named tag in {@code content} and returns the full
     * tag string (open tag + inner content + close tag) for each.
     *
     * <p>
     * Previously implemented with a regular expression whose {@code [^<]+} pattern
     * broke whenever the tag content contained a {@code '<'} character (e.g. inside
     * CDATA sections). The implementation now delegates to
     * {@link #parseTagMatches}, which handles these cases correctly.
     * </p>
     *
     * @param content text to search in
     * @param tag     tag name to search for (case-insensitive)
     * @return list of full tag strings in order of appearance
     */
    public static List<String> findTags(String content, String tag) {
        List<XMLTag> matches = parseTagMatches(content, tag);
        List<String> result = new ArrayList<>(matches.size());
        for (XMLTag m : matches) {
            result.add(m.getFullMatch());
        }
        return result;
    }

    /**
     * Finds all non-empty occurrences of a named tag in {@code content}.
     *
     * <p>
     * Previously implemented with a separate regex pattern. Now unified with
     * {@link #findTags} via the state machine: both methods behave identically
     * because the state machine skips self-closing (empty) tags by design.
     * </p>
     *
     * @param content text to search in
     * @param tag     tag name to search for (case-insensitive)
     * @return list of full tag strings for tags that have inner content
     */
    /**
     * Finds all non-empty occurrences of a named tag in {@code content}.
     *
     * <p>
     * A tag is considered empty if it has no inner content — this covers both
     * self-closing tags ({@code <tag/>}) and explicit empty tags
     * ({@code <tag></tag>}). Both forms are excluded from the result.
     * </p>
     *
     * @param content text to search in
     * @param tag     tag name to search for (case-insensitive)
     * @return list of full tag strings for tags that have inner content
     */
    public static List<String> findNoEmptyTags(String content, String tag) {
        List<XMLTag> matches = parseTagMatches(content, tag);
        List<String> result = new ArrayList<>(matches.size());
        for (XMLTag m : matches) {
            // Exclude both <tag/> and <tag></tag> — both produce an empty content string
            if (!m.getContent().isEmpty()) {
                result.add(m.getFullMatch());
            }
        }
        return result;
    }

    /**
     * Finds all non-empty occurrences of a tag whose inner content is itself XML
     * (i.e. starts with {@code '<'} and ends with {@code '>'}).
     *
     * @param content text to search in
     * @param tag     tag name to search for (case-insensitive)
     * @return list of full tag strings whose content is XML
     */
    public static List<String> findNoEmptyXMLTags(String content, String tag) {
        List<XMLTag> matches = parseTagMatches(content, tag);
        List<String> result = new ArrayList<>();
        for (XMLTag m : matches) {
            String trimmed = m.getContent().trim();
            if (!trimmed.isEmpty() && isXMLContent(trimmed)) {
                result.add(m.getFullMatch());
            }
        }
        return result;
    }

    /**
     * Returns {@code true} if {@code content} looks like an XML fragment, i.e.
     * starts with {@code '<'} and ends with {@code '>'} when stripped of
     * whitespace.
     *
     * @param content text to check
     * @return {@code true} if the content appears to be XML
     */
    public static boolean isXMLContent(String content) {
        String cleaned = content.replaceAll("\\s", "");
        return cleaned.startsWith("<") && cleaned.endsWith(">");
    }

    /**
     * Returns the inner content of every occurrence of the named tag.
     *
     * @param content text to search in
     * @param tag     tag name to search for (case-insensitive)
     * @return list of inner content strings in order of appearance
     */
    public static List<String> findTagValues(String content, String tag) {
        List<XMLTag> matches = parseTagMatches(content, tag);
        List<String> result = new ArrayList<>(matches.size());
        for (XMLTag m : matches) {
            result.add(m.getContent());
        }
        return result;
    }

    /**
     * Returns the inner content of the <em>first</em> occurrence of the named tag,
     * or an empty string if the tag is not found.
     *
     * @param content text to search in
     * @param tag     tag name to search for (case-insensitive)
     * @return inner content of the first matching tag, or {@code ""}
     */
    public static String findTagValue(String content, String tag) {
        List<XMLTag> matches = parseTagMatches(content, tag);
        if (matches.isEmpty()) {
            return "";
        }
        return matches.get(0).getContent();
    }

    // =========================================================================
    // DOM-based structured parsing
    // =========================================================================

    /**
     * Parses a plain XML content string (without a root element) into an
     * {@link ItemCollection} by wrapping it in a temporary {@code <item>} root.
     *
     * @param xmlContent XML fragment without a root element
     * @return populated {@link ItemCollection}
     * @throws PluginException if the XML is malformed
     */
    public static ItemCollection parseItemStructure(String xmlContent) throws PluginException {
        return parseTag("<item>" + xmlContent + "</item>", "item");
    }

    /**
     * Parses a single XML element identified by {@code tag} into an
     * {@link ItemCollection}. Each child element of the root becomes one item.
     *
     * <p>
     * This method uses a full DOM {@link DocumentBuilder} and is therefore more
     * expensive than the state machine methods. It should only be called for
     * structured configuration blocks (e.g. BPMN adapter result definitions), not
     * for high-frequency text template processing.
     * </p>
     *
     * @param xmlContent complete XML string containing the element
     * @param tag        name of the root element to parse
     * @return populated {@link ItemCollection}
     * @throws PluginException if the XML cannot be parsed
     */
    public static ItemCollection parseTag(String xmlContent, String tag) throws PluginException {
        boolean debug = logger.isLoggable(Level.FINE);
        if (debug) {
            logger.finest("......parseItemStructure...");
        }
        ItemCollection result = new ItemCollection();
        if (xmlContent.length() > 0) {
            try {
                DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder();
                Document doc = documentBuilder.parse(
                        new InputSource(new StringReader(xmlContent)));
                Node node = doc.importNode(doc.getDocumentElement(), true);
                if (node != null && node.getNodeName().equals(tag)) {
                    DocumentFragment docfrag = doc.createDocumentFragment();
                    while (node.hasChildNodes()) {
                        docfrag.appendChild(node.removeChild(node.getFirstChild()));
                    }
                    parseAndAppendChildNodes(docfrag.getChildNodes(), result, debug);
                }
            } catch (TransformerFactoryConfigurationError | SAXException | IOException
                    | ParserConfigurationException e) {
                throw new PluginException(XMLParser.class.getName(), "INVALID_FORMAT",
                        "Parsing item content failed: " + e.getMessage());
            }
        }
        return result;
    }

    /**
     * Iterates over the child nodes of a parsed DOM fragment and appends each
     * element's text content as an item value to {@code result}.
     *
     * @param children child nodes from the parsed document fragment
     * @param result   target {@link ItemCollection}
     * @param debug    whether to emit fine-grained log output
     */
    private static void parseAndAppendChildNodes(NodeList children,
            ItemCollection result, boolean debug) {
        int itemCount = children.getLength();
        for (int i = 0; i < itemCount; i++) {
            Node childNode = children.item(i);
            if (childNode instanceof Element && childNode.getFirstChild() != null) {
                String name = childNode.getNodeName();
                String value = innerXml(childNode);
                result.appendItemValue(name, value);
                if (debug) {
                    logger.log(Level.FINEST, "......parsing item ''{0}'' value={1}",
                            new Object[] { name, value });
                }
            }
        }
    }

    /**
     * Parses a list of structured XML elements identified by {@code tag} into a
     * list of {@link ItemCollection} objects.
     *
     * @param content XML text that may contain multiple elements named {@code tag}
     * @param tag     element name to search for
     * @return list of parsed {@link ItemCollection} objects
     * @throws PluginException if any element cannot be parsed
     */
    public static List<ItemCollection> parseTagList(String content, String tag)
            throws PluginException {
        List<ItemCollection> result = new ArrayList<>();
        List<String> tagList = findNoEmptyTags(content, tag);
        logger.finest("..found " + tagList.size() + " tags matching " + tag);
        for (String _tag : tagList) {
            result.add(parseTag(_tag, tag));
        }
        return result;
    }

    /**
     * Serialises the inner XML of a DOM {@link Node} to a string without the XML
     * declaration. Used by {@link #parseAndAppendChildNodes} to capture the full
     * text content of child elements, including any nested markup.
     *
     * @param node the DOM node whose inner XML is to be serialised
     * @return inner XML as a plain string
     */
    private static String innerXml(Node node) {
        DOMImplementationLS lsImpl = (DOMImplementationLS) node.getOwnerDocument()
                .getImplementation().getFeature("LS", "3.0");
        LSSerializer lsSerializer = lsImpl.createLSSerializer();
        lsSerializer.getDomConfig().setParameter("xml-declaration", false);
        NodeList childNodes = node.getChildNodes();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node innerNode = childNodes.item(i);
            if (innerNode != null) {
                if (innerNode.hasChildNodes()) {
                    sb.append(lsSerializer.writeToString(innerNode));
                } else {
                    sb.append(innerNode.getNodeValue());
                }
            }
        }
        return sb.toString();
    }

}