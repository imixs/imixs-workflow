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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

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
 * XMLParser provides helper methods to parse xml strings
 * 
 * @author rsoika
 *
 */
public class XMLParser {

    private static final Logger logger = Logger.getLogger(XMLParser.class.getName());

    /**
     * This method parses a xml tag for attributes. The method returns a Map with
     * all attributes found in the content string
     * 
     * e.g. <item data="abc" value="def" />
     * 
     * returns Map: {field=a, number=1}
     * 
     * @param content
     * @return
     */
    public static Map<String, String> findAttributes(String content) {
        Map<String, String> result = new HashMap<String, String>();
        Pattern p = null;
        // short version of [A-Za-z0-9\-]
        // Pattern p =
        // Pattern.compile("([\\w\\-]+)=\"*((?<=\")[^\"]+(?=\")|([^\\s]+))\"*");
        // Pattern p =
        // Pattern.compile("([\\w\\-]+)=\"*((?<=\")[^\"]+(?=\")|([^\\s]+))\"*");
        // Pattern p =
        // Pattern.compile("(\\S+)=[\"']?((?:.(?![\"']?\\s+(?:\\S+)=|[>\"']))+.)[\"']?");

        p = Pattern.compile("(\\S+)\\s*=\\s*[\"']?((?:.(?![\"']?\\s+(?:\\S+)=|[>\"']))?[^\"']*)[\"']?");

        Matcher m = p.matcher(content);
        while (m.find()) {
            result.put(m.group(1), m.group(2));
        }
        return result;
    }

    /**
     * This method parses a xml tag for a single named attribute. The method returns
     * the value of the attribute found in the content string
     * 
     * e.g. <item data="abc" />
     * 
     * returns "abc"
     * 
     * @param content
     * @return
     */
    public static String findAttribute(String content, String name) {
        Map<String, String> attriubtes = findAttributes(content);
        return attriubtes.get(name);
    }

    /**
     * This method find specific tags inside a string and returns a list with all
     * tags.
     * <p>
     * e.g. an empty tag:
     * <p>
     * {@code<date field="abc" />}
     * <p>
     * or a tag with content:
     * <p>
     * {@code<date field="abc">def</date>}
     * <p>
     * <strong>Note:</strong> In case of complex XML with not empty tags use the
     * method 'findNoEmptyTags'
     * 
     * @param content - XML data
     * @param tag     - XML tag
     * @return
     */
    public static List<String> findTags(String content, String tag) {
        List<String> result = new ArrayList<String>();

        String regex = "<(?i)(" + tag + ")" + // matches the tag itself
                "([^<]+)" + // then anything in between the opening and closing
                // of the tag
                "(</" + tag + ">|/>)"; // and finally the end tag corresponding
        // to what we matched as the first group
        // (Exony_Credit_Card_ID, tag1 or tag2)

        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(content);
        while (m.find()) {
            result.add(m.group());
        }
        return result;
    }

    /**
     * This method finds no-empty tags by name inside a string and returns a list
     * with all tags including the tag itself.
     * <p>
     * e.g.: {@code<date field="abc">def</date>}
     * <p>
     * <strong>Note:</strong> To find also empty tags use 'findTags'
     * 
     * @param content - XML data
     * @param tag     - XML tag
     * @return
     */
    public static List<String> findNoEmptyTags(String content, String tag) {
        List<String> result = new ArrayList<String>();
        // Use a regular expression to extract the tag
        String regex = "(<" + tag + "[^>]*>[\\s\\S]*?</" + tag + ">)";
        Pattern pattern = java.util.regex.Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            result.add(matcher.group());
        }
        return result;
    }

    /**
     * This method finds no-empty tags by name inside a string and returns the
     * embedded XML content. The method returns a list of xml tags including the tag
     * itself.
     * <p>
     * e.g.: {@code<date field="abc">def</date>}
     * <p>
     * <strong>Note:</strong> To find also empty tags use 'findTags'
     * 
     * @param content - XML data
     * @param tag     - XML tag
     * @return
     */
    public static List<String> findNoEmptyXMLTags(String content, String tag) {
        List<String> result = new ArrayList<>();
        // Regex to match the specified tag, capturing its content
        String regex = "<" + tag + "(?:[^>]*)>(.*?)</" + tag + ">";
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String tagContent = matcher.group(1).trim();
            if (!tagContent.isEmpty()) {
                if (isXMLContent(tagContent)) {
                    // only return the value if it is XML content
                    result.add(matcher.group(0));
                }
            }
        }
        return result;
    }

    /**
     * Test if a given String is XML Content
     * 
     * @param content
     * @return
     */
    public static boolean isXMLContent(String content) {
        // Remove all whitespace, including newlines
        String cleaned = content.replaceAll("\\s", "");
        return cleaned.startsWith("<") && cleaned.endsWith(">");
    }

    /**
     * This method returns all tag values within a string with multiple xml tags.
     * E.g.
     * <p>
     * {@code<date>2016-12-31</date>...<date>2016-11-30</date>}
     * <p>
     * returns
     * <p>
     * {@code 2016-12-31|2016-12-31}
     * 
     * @param content - XML data
     * @param tag     - XML tag
     * @return
     */
    public static List<String> findTagValues(String content, String tag) {
        List<String> result = new ArrayList<String>();
        List<String> tags = findTags(content, tag);

        for (String singleTag : tags) {
            result.add(extractTagValue(singleTag, tag));
        }

        return result;
    }

    /**
     * This method returns the tag value of a single tag. The method returns the
     * first match! Use findTagValues to parse all tag values in a string with
     * multiple tags.
     * <p>
     * E.g.
     * <p>
     * {@code<date>2016-12-31</date>...<date>2016-11-30</date>}
     * <p>
     * returns
     * <p>
     * {@code 2016-12-31}
     * 
     * @see findTagValues
     * @param content - XML data
     * @param tag     - XML tag
     * @return
     */
    public static String findTagValue(String content, String tag) {
        List<String> tags = findTags(content, tag);
        if (tags.size() > 0) {
            // only first tag...
            content = tags.get(0);
        }
        return extractTagValue(content, tag);
    }

    /**
     * This helper method extracts the tag value of a single tag.
     * <p>
     * {@code<date>2016-12-31</date>}
     * <p>
     * returns
     * <p>
     * {@code 2016-12-31}
     * 
     * @param content - XML data
     * @param tag     - XML tag
     * @return
     */
    private static String extractTagValue(String content, String tag) {
        String startTag = "<" + tag;
        String endTag = "</" + tag + ">";

        // search the start tag.....
        int iStart = content.indexOf(startTag);
        if (iStart < 0) {
            // not found - no value!
            return "";
        } else {
            // find the closing tag position '>'
            iStart = content.indexOf('>', startTag.length()) + 1;
        }

        // search the last occurrence of the end tag
        int iEnd = content.lastIndexOf(endTag);
        if (iEnd < 0) {
            // no end tag found so this tag has no value at all
            return "";
        }
        // extract the tag content
        String value = content.substring(iStart, iEnd);
        return value;
    }

    @Deprecated
    public static String findTagValueOld(String content, String tag) {
        // opening tag can contain optional attributes
        List<String> tags = findTags(content, tag);
        if (tags.size() > 0) {
            // only first tag...
            content = tags.get(0);
        }
        String regex = "(<" + tag + ".+?>|<" + tag + ">)(.+?)(</" + tag + ")";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(content);
        if (m.find()) {
            return m.group(2);
        }
        // no result
        return "";
    }

    /**
     * This method parses the xml content of a item element and returns a new
     * ItemCollection containing all item values. Each tag is evaluated as the item
     * name.
     * 
     * MultiValues are currently not supported.
     * 
     * Example:
     * 
     * <pre>
     * {@code
     * <item>	  
     *    <modelversion>1.0.0</modelversion>
     *    <task>1000</task>
     *    <event>10</event>
     * </item>
     * }
     * </pre>
     * 
     * @param evalItemCollection
     * @throws PluginException
     */
    public static ItemCollection parseItemStructure(String xmlContent) throws PluginException {
        return parseTag("<item>" + xmlContent + "</item>", "item");
    }

    /**
     * This method parses the xml content of a XML tag and returns a new
     * ItemCollection containing all embedded tags. Each tag is evaluated as the
     * item name. The tag value is returned as a item value.
     * <p>
     * MultiValues are currently not supported.
     * <p>
     * Example:
     * <p>
     * The tag 'code' of:
     * 
     * <pre>
     * {@code
     * <code>	  
     *    <modelversion>1.0.0</modelversion>
     *    <task>1000</task>
     *    <event>10</event>
     * </code>
     * }
     * </pre>
     * <p>
     * Returns an ItemCollection with 3 items (modelversion, task and event)
     * 
     * @param evalItemCollection
     * @throws PluginException
     */
    public static ItemCollection parseTag(String xmlContent, String tag) throws PluginException {
        boolean debug = logger.isLoggable(Level.FINE);
        if (debug) {
            logger.finest("......parseItemStructure...");
        }
        ItemCollection result = new ItemCollection();
        if (xmlContent.length() > 0) {
            try {

                // parse item list...
                DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = documentBuilder.parse(new InputSource(new StringReader(xmlContent)));
                Node node = doc.importNode(doc.getDocumentElement(), true);

                // we expect the tag name as the root tag of the xml structure!
                if (node != null && node.getNodeName().equals(tag)) {
                    DocumentFragment docfrag = doc.createDocumentFragment();
                    while (node.hasChildNodes()) {
                        docfrag.appendChild(node.removeChild(node.getFirstChild()));
                    }

                    // append all items into the evalItemCollection...
                    NodeList children = docfrag.getChildNodes();
                    parseAndAppendChildNodes(children, result, debug);
                }

            } catch (ParserConfigurationException | TransformerFactoryConfigurationError | SAXException
                    | IOException e) {
                throw new PluginException(XMLParser.class.getName(), "INVALID_FORMAT",
                        "Parsing item content failed: " + e.getMessage());

            }
        }
        return result;
    }

    /**
     * This helper method parses all child elements and creates / or appends the tag
     * value as an Item value into the
     * result ItemCollection.
     * The method is called from parseTag.
     **/
    private static void parseAndAppendChildNodes(NodeList children, ItemCollection result, boolean debug) {
        // collect all child nodes...
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
     * This method parses the xml content and returns a list of
     * ItemCollection elements for each tag with the given tag name
     * <p>
     *
     * Example:
     * <p>
     * The tag 'code' of:
     * 
     * <pre>
     * {@code
     * <code>	  
     *    <modelversion>1.0.0</modelversion>
     *    <task>1000</task>
     *    <event>10</event>
     * </code>
     * <code>	  
     *    <modelversion>2.0.0</modelversion>
     *    <task>2000</task>
     *    <event>20</event>
     * </code>
     * }
     * </pre>
     * <p>
     * Returns a List with two ItemCollections each with 3 items (modelversion, task
     * and event)
     * 
     * @param evalItemCollection
     * @throws PluginException
     */
    public static List<ItemCollection> parseTagList(String content, String tag) throws PluginException {
        List<ItemCollection> result = new ArrayList<>();
        List<String> _tagList = XMLParser.findNoEmptyTags(content, tag);
        logger.finest("..found " + _tagList.size() + " tags matching " + tag);
        for (String _tag : _tagList) {
            result.add(XMLParser.parseTag(_tag, tag));
        }
        return result;
    }

    /**
     * This method extracts the content of a XML node and prevents inner XML tags
     * 
     * @see https://stackoverflow.com/questions/3300839/get-a-nodes-inner-xml-as-string-in-java-dom?noredirect=1#comment90136258_42456679
     * @param node
     * @return
     * @throws TransformerFactoryConfigurationError
     * @throws TransformerException
     */
    private static String innerXml(Node node) {
        DOMImplementationLS lsImpl = (DOMImplementationLS) node.getOwnerDocument().getImplementation().getFeature("LS",
                "3.0");
        LSSerializer lsSerializer = lsImpl.createLSSerializer();
        lsSerializer.getDomConfig().setParameter("xml-declaration", false);
        NodeList childNodes = node.getChildNodes();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node innerNode = childNodes.item(i);
            // verify innerNode...
            if (innerNode != null) {
                if (innerNode.hasChildNodes()) {
                    // lets do the stuff by the LSSerializer...
                    sb.append(lsSerializer.writeToString(innerNode));
                } else {
                    // just write the node value...
                    sb.append(innerNode.getNodeValue());
                }
            }
        }
        return sb.toString();
    }

    /**
     * @see https://stackoverflow.com/questions/3300839/get-a-nodes-inner-xml-as-string-in-java-dom?noredirect=1#comment90136258_42456679
     */
    @SuppressWarnings("unused")
    @Deprecated
    private static String oldinnerXml(Node node) throws TransformerFactoryConfigurationError, TransformerException {
        long l = System.currentTimeMillis();
        StringWriter writer = new StringWriter();
        String xml = null;
        Transformer transformer;
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        // Set secure process - see #852
        transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
        transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(new DOMSource(node), new StreamResult(writer));
        // now we remove the outer tag....
        xml = writer.toString();
        xml = xml.substring(xml.indexOf(">") + 1, xml.lastIndexOf("</"));
        System.out.println(" ....time= " + (System.currentTimeMillis() - l));
        return xml;
    }

}
