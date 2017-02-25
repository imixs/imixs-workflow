/*******************************************************************************
 *  Imixs Workflow 
 *  Copyright (C) 2001, 2011 Imixs Software Solutions GmbH,  
 *  http://www.imixs.com
 *  
 *  This program is free software; you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License 
 *  as published by the Free Software Foundation; either version 2 
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 *  General Public License for more details.
 *  
 *  You can receive a copy of the GNU General Public
 *  License at http://www.gnu.org/licenses/gpl.html
 *  
 *  Project: 
 *  	http://www.imixs.org
 *  	http://java.net/projects/imixs-workflow
 *  
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika - Software Developer
 *******************************************************************************/

package org.imixs.workflow.engine.plugins;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This Plug-In evaluates the result message provided by the Activity property
 * 'txtActivityResult'. The value will be parsed for the xml tag 'item'
 * 
 * <code>
 * 		<item name="fieldname">value</item> 
 * </code>
 * 
 * The provided value will be assigned to the named property. The value can also
 * be evaluated with the tag 'itemValue'
 * 
 * <code>
 *   <item name="fieldname"><itemvalue>namCreator</itemvalue></item> 
 * </code>
 * 
 * 
 * 
 * @author Ralph Soika
 * @version 1.0
 * @see org.imixs.workflow.WorkflowManager
 * 
 */
public class ResultPlugin extends AbstractPlugin {

	public static final String INVALID_FORMAT = "INVALID_FORMAT";

	private static Logger logger = Logger.getLogger(ResultPlugin.class.getName());

	public ItemCollection run(ItemCollection documentContext, ItemCollection adocumentActivity) throws PluginException {
		// evaluate new items....
		ItemCollection evalItemCollection = evaluateWorkflowResult(adocumentActivity, documentContext, true);
		// copy values
		if (evalItemCollection != null) {
			documentContext.replaceAllItems(evalItemCollection.getAllItems());
		}
		return documentContext;
	}

	/**
	 * This method parses the xml content of a item element and returns a new
	 * ItemCollection containing all item values. Each tag is evaluated as the
	 * item name.
	 * 
	 * MultiValues are currently not supported.
	 * 
	 * Example:
	 * 
	 * <code>	  
				<modelversion>1.0.0</modelversion>
				<processid>1000</processid>
				<activityid>10</activityid>
				<items>namTeam</items>	  
	 * </code>
	 * 
	 * @param evalItemCollection
	 * @throws PluginException
	 */
	public static ItemCollection parseItemStructure(String xmlContent) throws PluginException {
		logger.fine("parseItemStructure...");
		ItemCollection result = new ItemCollection();
		if (xmlContent.length() > 0) {
			// surround with a root element
			xmlContent = "<item>" + xmlContent.trim() + "</item>";
			try {

				// parse item list...
				DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				Document doc = documentBuilder.parse(new InputSource(new StringReader(xmlContent)));
				Node node = doc.importNode(doc.getDocumentElement(), true);
				DocumentFragment docfrag = doc.createDocumentFragment();
				while (node.hasChildNodes()) {
					docfrag.appendChild(node.removeChild(node.getFirstChild()));
				}

				// append all items into the evalItemCollection...
				NodeList childs = docfrag.getChildNodes();
				int itemCount = childs.getLength();
				for (int i = 0; i < itemCount; i++) {
					Node childNode = childs.item(i);
					if (childNode instanceof Element && childNode.getFirstChild() != null) {
						String name = childNode.getNodeName();
						// String value =
						// childNode.getFirstChild().getNodeValue();
						String value = innerXml(childNode);

						result.replaceItemValue(name, value);
						logger.fine("[ResultPlugin] parsing item '" + name + "' value=" + value);
					}
				}

			} catch (ParserConfigurationException | TransformerFactoryConfigurationError | TransformerException | SAXException | IOException e) {
				throw new PluginException(RulePlugin.class.getName(), INVALID_FORMAT,
						"Parsing item content failed: " + e.getMessage());

			
			}
		}
		return result;
	}

	/**
	 * The method evaluates the WorkflowResult for a given activityEntity and
	 * returns a ItemColleciton containing all item definitions. Each item
	 * definition of a WorkflowResult contains a name and a optional list of
	 * additional attributes. The method generates a item for each content
	 * element and attribute value. <br>
	 * e.g. <item name="comment" ignore="true">text</item> <br>
	 * will result in the attributes 'comment' with value 'text' and
	 * 'comment.ignore' with the value 'true'
	 * 
	 * Also embedded itemVaues can be resolved (resolveItemValues=true):
	 * 
	 * <code>
	 * 		<somedata>ABC<itemValue>$uniqueid</itemValue></somedata>
	 * </code>
	 * 
	 * This example will result in a new item 'somedata' with the $unqiueid
	 * prafixed with 'ABC'
	 * 
	 * @see http://ganeshtiwaridotcomdotnp.blogspot.de/2011/12/htmlxml-tag-
	 *      parsing-using-regex-in-java.html
	 * @param activityEntity
	 * @param documentContext
	 * @param resolveItemValues
	 *            - if true, itemValue tags will be resolved.
	 * @return
	 * @throws PluginException
	 */
	public static ItemCollection evaluateWorkflowResult(ItemCollection activityEntity, ItemCollection documentContext,
			boolean resolveItemValues) throws PluginException {
		boolean invalidPattern = true;

		ItemCollection result = new ItemCollection();
		String workflowResult = activityEntity.getItemValueString("txtActivityResult");
		if (workflowResult.isEmpty()) {
			return null;
		}
		// replace dynamic values?
		if (resolveItemValues) {
			workflowResult = new ResultPlugin().replaceDynamicValues(workflowResult, documentContext);
		}
		// extract all <item> tags with attributes using regex (including empty
		// item tags)
		Pattern pattern = Pattern.compile("<item(.*?)>(.*?)</item>|<item(.*?)./>", Pattern.DOTALL);
		Matcher matcher = pattern.matcher(workflowResult);
		while (matcher.find()) {
			invalidPattern = false;
			// we expect up to 3 different result groups

			// group 0 contains complete item string
			String attributes = matcher.group(1);
			String content = matcher.group(2);

			// test if empty tag (group 1 and 2 empty)
			if (attributes == null || content == null) {
				attributes = matcher.group(3);
			}

			if (content == null) {
				content = "";
			}

			// now extract the attributes to verify the item name..
			if (attributes != null && !attributes.isEmpty()) {
				// parse attributes...
				String spattern = "(\\S+)=[\"']?((?:.(?![\"']?\\s+(?:\\S+)=|[>\"']))+.)[\"']?";
				Pattern attributePattern = Pattern.compile(spattern);
				Matcher attributeMatcher = attributePattern.matcher(attributes);
				Map<String, String> attrMap = new HashMap<String, String>();
				while (attributeMatcher.find()) {
					String attrName = attributeMatcher.group(1); // name
					String attrValue = attributeMatcher.group(2); // value
					attrMap.put(attrName, attrValue);
				}

				String itemName = attrMap.get("name");
				if (itemName == null) {
					throw new PluginException(ResultPlugin.class.getSimpleName(), INVALID_FORMAT,
							"<item> tag contains no name attribute.");
				}

				if (itemName.startsWith("$")) {
					throw new PluginException(ResultPlugin.class.getSimpleName(), INVALID_FORMAT,
							"<item> tag contains invalid name attribute '" + itemName + "'.");
				}

				// now add optional attributes if available
				for (String attrName : attrMap.keySet()) {
					// we need to skip the 'name' attribute
					if (!"name".equals(attrName)) {
						result.appendItemValue(itemName + "." + attrName, attrMap.get(attrName));
					}
				}

				// test if the type attribute was provided to convert content?
				String sType = result.getItemValueString(itemName + ".type");
				if (!sType.isEmpty()) {
					// convert content type
					if ("boolean".equalsIgnoreCase(sType)) {
						result.appendItemValue(itemName, Boolean.valueOf(content));
					} else if ("integer".equalsIgnoreCase(sType)) {
						result.appendItemValue(itemName, Integer.valueOf(content));
					} else if ("double".equalsIgnoreCase(sType)) {
						result.appendItemValue(itemName, Double.valueOf(content));
					} else
						// no type conversion
						result.appendItemValue(itemName, content);
				} else {
					// no type definition
					result.appendItemValue(itemName, content);
				}

			} else {
				throw new PluginException(ResultPlugin.class.getSimpleName(), INVALID_FORMAT,
						"<item> tag contains no name attribute.");

			}

		}

		// test for general invalid format
		if (invalidPattern) {
			throw new PluginException(ResultPlugin.class.getSimpleName(), INVALID_FORMAT,
					"invalid <item> tag format - expected <item name=\"...\" ...></item>  -> workflowResult="
							+ workflowResult);
		}
		return result;
	}

	/**
	 * The method evaluates the WorkflowResult and resolves embedded ItemValues.
	 * 
	 * * <code>
	 * 		<somedata>ABC<itemValue>$uniqueid</itemValue></somedata>
	 * </code>
	 * 
	 * This example will result in a new item 'somedata' with the $unqiueid
	 * prafixed with 'ABC'
	 * 
	 * @see evaluateWorkflowResult(ItemCollection activityEntity, ItemCollection
	 *      documentContext,boolean resolveItemValues)
	 * @param activityEntity
	 * @param documentContext
	 * @return
	 * @throws PluginException
	 */
	public static ItemCollection evaluateWorkflowResult(ItemCollection activityEntity, ItemCollection documentContext)
			throws PluginException {
		return evaluateWorkflowResult(activityEntity, documentContext, true);
	}

	/**
	 * This method extracts the content of a XML node and prevents inner XML
	 * tags
	 * 
	 * @param node
	 * @return
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerException
	 */
	private static String innerXml(Node node) throws TransformerFactoryConfigurationError, TransformerException {
		StringWriter writer = new StringWriter();
		String xml = null;
		Transformer transformer;
		transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.transform(new DOMSource(node), new StreamResult(writer));
		// now we remove the outer tag....
		xml = writer.toString();
		xml = xml.substring(xml.indexOf(">") + 1, xml.lastIndexOf("</"));
		return xml;
	}
}
