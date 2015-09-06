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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.exceptions.PluginException;

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
	private static Logger logger = Logger.getLogger(ResultPlugin.class
			.getName());

	public int run(ItemCollection adocumentContext,
			ItemCollection adocumentActivity) throws PluginException {
		documentContext = adocumentContext;

		// get ResultMessage
		sActivityResult = adocumentActivity
				.getItemValueString("txtActivityResult");
		sActivityResult = replaceDynamicValues(sActivityResult,
				adocumentContext);

		// evaluate new items....
		try {
			evaluate(sActivityResult, adocumentContext);
		} catch (Exception e) {
			throw new PluginException(ResultPlugin.class.getSimpleName(),
					INVALID_FORMAT, "ResultPlguin: invalid format : "
							+ sActivityResult + " Error: " + e.getMessage());

		}
		return Plugin.PLUGIN_OK;
	}

	public void close(int status) throws PluginException {
		try {
			// restore changes?
			if (status < Plugin.PLUGIN_ERROR) {
				documentContext.replaceItemValue("txtworkflowresultmessage",
						sActivityResult);
			}
		} catch (Exception e) {
			System.out.println("[ResultPlugin] Error close(): " + e.toString());

		}
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
	public static void evaluate(String aString, ItemCollection documentContext)
			throws PluginException {

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
				throw new PluginException(ResultPlugin.class.getSimpleName(),
						INVALID_FORMAT, "</item>  expected!");

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
			iContentStartPos = sTestString.lastIndexOf('>') + 1;

			// if no end tag found return string unchanged...
			if (iContentStartPos >= iContentEndPos)
				return;

			iTagEndPos = iTagEndPos + "</item>".length();

			// now we have the start and end position of a tag and also the
			// start and end pos of the value

			// next we check if the start tag contains a 'name' attribute
			iNameStartPos = aString.toLowerCase()
					.indexOf("name=", iTagStartPos);
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
			iTypeStartPos = aString.toLowerCase()
					.indexOf("type=", iTagStartPos);
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
					logger.warning("ResultPlugin - item '" + sName
							+ "' update not allowed!");
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
						List<Object> values = documentContext
								.getItemValue(sName);
						values.add(oValue);
						documentContext.replaceItemValue(sName, values);
					}
				}
			}

			// now cut the tag form the string
			aString = aString.substring(0, iTagStartPos) + ""
					+ aString.substring(iTagEndPos);
		}

	}

}
