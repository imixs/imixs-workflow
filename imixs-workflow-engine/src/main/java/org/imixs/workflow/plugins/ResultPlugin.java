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

package org.imixs.workflow.plugins;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
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
 * The provided value will be assigend to the named property. The value can also
 * be evaluated with the tag 'itemValue'
 * 
 * <code>
 *   <item name="fieldname"><itemvalue>namCreator</itemvalue></item> 
 * </code>
 * 
 * 
 * The final result will be stored into the attribute
 * "txtworkflowresultmessage".
 * 
 * This field could be used by an application to display individual messages
 * (e.g HTML Code) or return result Strings (e.g. JSF Action Results)
 * 
 * @author Ralph Soika
 * @version 1.0
 * @see org.imixs.workflow.WorkflowManager
 * 
 */
public class ResultPlugin extends AbstractPlugin {

	public static final String INVALID_FORMAT = "INVALID_FORMAT";

	ItemCollection documentContext;
	String sActivityResult;
	private static Logger logger = Logger.getLogger(ResultPlugin.class.getName());

	public int run(ItemCollection adocumentContext, ItemCollection adocumentActivity) throws PluginException {
		documentContext = adocumentContext;
		// evaluate new items....
		ItemCollection evalItemCollection = new ItemCollection();
		evalItemCollection=adocumentContext=evaluateWorkflowResult(adocumentActivity,documentContext);
		// copy values
		if (evalItemCollection!=null) {
			documentContext.replaceAllItems(evalItemCollection.getAllItems());
		}
		return Plugin.PLUGIN_OK;
	}

	public void close(int status) throws PluginException {
		try {
			// restore changes?
			if (status < Plugin.PLUGIN_ERROR) {
				documentContext.replaceItemValue("txtworkflowresultmessage", sActivityResult);
			}
		} catch (Exception e) {
			System.out.println("[ResultPlugin] Error close(): " + e.toString());

		}
	}

	/**
	 * This method evaluates the given activityEntiy and returns a new
	 * ItemCollection containing all items defined by the activity property
	 * 'txtActivityResult'.
	 * 
	 * @see method evaluate(String aString, ItemCollection documentContext)
	 * @throws PluginException
	 * 
	 */
	@Deprecated
	private static ItemCollection evaluate(ItemCollection activityEntity, ItemCollection documentContext)
			throws PluginException {

		String sResult = activityEntity.getItemValueString("txtActivityResult");
		// replace dynamic values
		sResult = new ResultPlugin().replaceDynamicValues(sResult, documentContext);

		ItemCollection evalItemCollection = new ItemCollection();
		ResultPlugin.evaluate(sResult, evalItemCollection);

		return evalItemCollection;
	}

	/**
	 * This method parses a string for xml tag <item>. Those tags will result in
	 * new workitem properties.
	 * 
	 * <code>
	 *   <item name="txtTitle">new Title</item>
	 * </code>
	 * 
	 * If an item with the same name exits multiple times the method creates a
	 * multivalue item
	 * 
	 * Additional the attribute 'type' can be provided to specify a field type
	 * 'boolean', 'string', 'integer'
	 * 
	 * The item will be added into the given documentContext.
	 * 
	 * If the item name starts with $ it will be ignored!
	 * 
	 * Also ' will be replaced with " in attribute name
	 * 
	 * @throws PluginException
	 * 
	 */
	@SuppressWarnings("unchecked")
	@Deprecated
	private static void evaluate(String aString, ItemCollection documentContext) throws PluginException {

		List<String> mulitValueItemNames = new ArrayList<String>();
		int iTagStartPos;
		int iTagEndPos;

		int iContentStartPos;
		int iContentEndPos;

		int iNameStartPos;
		int iNameEndPos;

		int iTypeStartPos;
		int iTypeEndPos;

		String sName = "";
		String sType = " ";
		String sItemValue;

		if (aString == null)
			return;

		// test if a <value> tag exists...
		while ((iTagStartPos = aString.toLowerCase().indexOf("<item")) != -1) {

			iTagEndPos = aString.toLowerCase().indexOf("</item>", iTagStartPos);

			// if no end tag found return string unchanged...
			if (iTagEndPos == -1)
				throw new PluginException(ResultPlugin.class.getSimpleName(), INVALID_FORMAT, "</item>  expected!");

			// reset pos vars
			iContentStartPos = 0;
			iContentEndPos = 0;
			iNameStartPos = 0;
			iNameEndPos = 0;
			iTypeStartPos = 0;
			iTypeEndPos = 0;
			sName = "";
			sType = " ";
			sItemValue = "";

			// so we now search the beginning of the tag content
			iContentEndPos = iTagEndPos;
			// start pos is the last > before the iContentEndPos
			String sTestString = aString.substring(0, iContentEndPos);
			iContentStartPos = sTestString.indexOf('>') + 1;

			// if no end tag found return string unchanged...
			if (iContentStartPos >= iContentEndPos)
				return;

			iTagEndPos = iTagEndPos + "</item>".length();

			// now we have the start and end position of a tag and also the
			// start and end pos of the value

			// next we check if the start tag contains a 'name' attribute
			iNameStartPos = aString.toLowerCase().indexOf("name=", iTagStartPos);
			// extract format string if available
			// ' can be used instead of " chars!
			// e.g.: name='txtName'> or name="txtName">
			if (iNameStartPos > -1 && iNameStartPos < iContentStartPos) {
				// replace ' with " before content start pos
				String sNamePart = aString.substring(0, iContentStartPos);
				sNamePart = sNamePart.replace("'", "\"");
				iNameStartPos = sNamePart.indexOf("\"", iNameStartPos) + 1;
				iNameEndPos = sNamePart.indexOf("\"", iNameStartPos + 1);
				sName = sNamePart.substring(iNameStartPos, iNameEndPos);
			}

			// next we check if the start tag contains a 'type'
			// attribute
			iTypeStartPos = aString.toLowerCase().indexOf("type=", iTagStartPos);
			// extract format string if available
			if (iTypeStartPos > -1 && iTypeStartPos < iContentStartPos) {
				String sTypePart = aString.substring(0, iContentStartPos);
				sTypePart = sTypePart.replace("'", "\"");

				iTypeStartPos = sTypePart.indexOf("\"", iTypeStartPos) + 1;
				iTypeEndPos = sTypePart.indexOf("\"", iTypeStartPos + 1);
				sType = sTypePart.substring(iTypeStartPos, iTypeEndPos);
			}

			// extract Item Value
			sItemValue = aString.substring(iContentStartPos, iContentEndPos);

			// replace the item value
			if (sName != null && !"".equals(sName)) {
				// ignore case sensetive names
				sName = sName.toLowerCase();

				// if itemname starts with $ it will be ignored
				if (sName.startsWith("$")) {
					logger.warning("ResultPlugin - item '" + sName + "' update not allowed!");
				} else {
					// convert to type...
					Object oValue = sItemValue;
					if ("boolean".equalsIgnoreCase(sType)) {
						oValue = Boolean.valueOf(sItemValue);
					}
					if ("integer".equalsIgnoreCase(sType)) {
						oValue = new Integer(sItemValue);
					}

					// test if item was already computed before

					if (!mulitValueItemNames.contains(sName)) {
						documentContext.replaceItemValue(sName, oValue);
						mulitValueItemNames.add(sName);
					} else {
						// item was processed before so we add the value into a
						// multivalue list
						List<Object> values = documentContext.getItemValue(sName);
						values.add(oValue);
						documentContext.replaceItemValue(sName, values);
					}
				}
			}

			// now cut the tag form the string
			aString = aString.substring(0, iTagStartPos) + "" + aString.substring(iTagEndPos);
		}

	}

	/**
	 * This method parses the xml content of a item element and returns a new
	 * ItemCollection containing all item values. Each tag is evaluated as the
	 * item name.
	 * 
	 * MultiValues are currently not supported
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
		logger.fine("Evaluate Subprocess Item...");

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
						String value = childNode.getFirstChild().getNodeValue();
						result.replaceItemValue(name, value);
						logger.fine("[ResultPlugin] parsing item '" + name + "' value=" + value);
					}
				}

			} catch (ParserConfigurationException e) {
				throw new PluginException(RulePlugin.class.getName(), INVALID_FORMAT,
						"Parsing item content failed: " + e.getMessage());

			} catch (SAXException e) {
				throw new PluginException(RulePlugin.class.getName(), INVALID_FORMAT,
						"Parsing item content failed: " + e.getMessage());

			} catch (IOException e) {
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
	 * 
	 * @see http://ganeshtiwaridotcomdotnp.blogspot.de/2011/12/htmlxml-tag-
	 *      parsing-using-regex-in-java.html
	 * @param activityEntity
	 * @param documentContext
	 * @return
	 * @throws PluginException
	 */
	public static ItemCollection evaluateWorkflowResult(ItemCollection activityEntity,ItemCollection documentContext)
			throws PluginException {

		boolean invalidPattern = true;

		ItemCollection result = new ItemCollection();
		String workflowResult = activityEntity.getItemValueString("txtActivityResult");
		if (workflowResult.isEmpty()) {
			return null;
		}
		// replace dynamic values
		workflowResult = new ResultPlugin().replaceDynamicValues(workflowResult, documentContext);

		// extract all <item> tags with attributes using regex (including empty
		// item tags)
		Pattern pattern = Pattern.compile("<item(.*?)>(.*?)</item>|<item(.*?)./>");
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
						result.appendItemValue(itemName, new Integer(content));
					} else if ("double".equalsIgnoreCase(sType)) {
						result.appendItemValue(itemName, new Double(content));
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
					"invalid <item> tag format - expected <item name=\"...\" ...></item>");
		}

		return result;
	}

}
