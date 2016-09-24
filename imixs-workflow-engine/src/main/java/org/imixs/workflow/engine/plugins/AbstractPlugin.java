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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.exceptions.PluginException;

/**
 * This abstract class implements different helper methods used by subclasses
 * 
 * @author Ralph Soika
 * @version 1.1
 * @see org.imixs.workflow.WorkflowManager
 * 
 */

public abstract class AbstractPlugin implements Plugin {

	
	public static final String INVALID_ITEMVALUE_FORMAT = "INVALID_ITEMVALUE_FORMAT";
	public static final String INVALID_PROPERTYVALUE_FORMAT = "INVALID_PROPERTYVALUE_FORMAT";

	private WorkflowContext ctx;
	private WorkflowService workflowService;

	private static Logger logger = Logger.getLogger(AbstractPlugin.class.getName());

	/**
	 * Initialize Plugin and get an instance of the EJB Session Context
	 */
	public void init(WorkflowContext actx) throws PluginException {
		ctx = actx;
		// get WorkflowService by check for an instance of WorkflowService
		if (actx instanceof WorkflowService) {
			// yes we are running in a WorkflowService EJB
			workflowService = (WorkflowService) actx;
		}
	}
	
	@Override
	public void close(boolean rollbackTransaction) throws PluginException {
		
	}
	
	public WorkflowContext getCtx() {
		return ctx;
	}


	/**
	 * Returns an instance of the WorkflowService EJB.
	 * 
	 * @return
	 */
	public WorkflowService getWorkflowService() {
		return workflowService;
	}


	/**
	 * this method parses a string for xml tag <itemvalue>. Those tags will be
	 * replaced with the corresponding item value.
	 * 
	 * <code>
	 *   
	 *   hello <itemvalue>namCreator</itemvalue>
	 *   
	 *   
	 * </code>
	 * 
	 * Item values can also be formated. e.g. for date/time values
	 * 
	 * <code>
	 *  
	 *  Last access Time= <itemvalue format="mm:ss">$created</itemvalue>
	 * 
	 * </code>
	 * 
	 * If the itemValue is a multiValue object the single values can be
	 * spearated by a separator
	 * 
	 * <code>
	 *  
	 *  Phone List: <itemvalue separator="<br />">txtPhones</itemvalue>
	 * 
	 * </code>
	 * 
	 * 
	 * 
	 */
	public String replaceDynamicValues(String aString, ItemCollection documentContext) throws PluginException {
		int iTagStartPos;
		int iTagEndPos;
		int iContentStartPos;
		int iContentEndPos;

		String sFormat = "";
		String sSeparator = " ";
		String sItemValue;
		String sPropertyKey;

		if (aString == null)
			return "";

		/*
		 * 
		 * TODO This code should be refactored using the new helper class
		 * org.imixs.workflow.util.XMLParser
		 * 
		 * 
		 */
		
		
		if (aString.toLowerCase().contains("<propertyvalue")) {

			// test if a <value> tag exists...
			while ((iTagStartPos = aString.toLowerCase().indexOf("<propertyvalue")) != -1) {

				iTagEndPos = aString.toLowerCase().indexOf("</propertyvalue>", iTagStartPos);

				// if no end tag found return string unchanged...
				if (iTagEndPos == -1) {
					throw new PluginException(this.getClass().getSimpleName(), INVALID_PROPERTYVALUE_FORMAT,
							"[AbstractPlugin] invalid propertyvalue format: " + aString);
				}

				// reset pos vars
				iContentStartPos = 0;
				iContentEndPos = 0;
				sPropertyKey = "";

				// so we now search the beginning of the tag content
				iContentEndPos = iTagEndPos;
				// start pos is the last > before the iContentEndPos
				String sTestString = aString.substring(0, iContentEndPos);
				iContentStartPos = sTestString.lastIndexOf('>') + 1;

				// if no end tag found return string unchanged...
				if (iContentStartPos >= iContentEndPos) {
					logger.warning("invalid text string format: " + aString);
					break;
				}

				iTagEndPos = iTagEndPos + "</propertyvalue>".length();

				// now we have the start and end position of a tag and also the
				// start and end pos of the value

				// read the property Value
				sPropertyKey = aString.substring(iContentStartPos, iContentEndPos);

				String vValue = getWorkflowService().getPropertyService().getProperties().getProperty(sPropertyKey);
				if (vValue == null) {
					logger.warning("[AbstractPlugin] propertyvalue '" + sPropertyKey
							+ "' is not defined in imixs.properties!");
					vValue = "";
				}
				// now replace the tag with the result string
				aString = aString.substring(0, iTagStartPos) + vValue + aString.substring(iTagEndPos);

			}
		}
		

		// test if a <value> tag exists...
		while ((iTagStartPos = aString.toLowerCase().indexOf("<itemvalue")) != -1) {

			iTagEndPos = aString.toLowerCase().indexOf("</itemvalue>", iTagStartPos);

			// if no end tag found return string unchanged...
			if (iTagEndPos == -1) {
				throw new PluginException(this.getClass().getSimpleName(), INVALID_ITEMVALUE_FORMAT,
						"[AbstractPlugin] invalid itemvalue format: " + aString);
			}

			// reset pos vars
			iContentStartPos = 0;
			iContentEndPos = 0;
			sFormat = "";
			sSeparator = " ";
			sItemValue = "";

			// so we now search the beginning of the tag content
			iContentEndPos = iTagEndPos;
			// start pos is the last > before the iContentEndPos
			String sTestString = aString.substring(0, iContentEndPos);
			iContentStartPos = sTestString.lastIndexOf('>') + 1;

			// if no end tag found return string unchanged...
			if (iContentStartPos >= iContentEndPos) {
				throw new PluginException(this.getClass().getSimpleName(), INVALID_ITEMVALUE_FORMAT,
						"[AbstractPlugin] invalid itemvalue format: " + aString);
			}

			iTagEndPos = iTagEndPos + "</itemvalue>".length();

			// now we have the start and end position of a tag and also the
			// start and end pos of the value

			// next we check if the start tag contains a 'format' attribute
			sFormat = extractAttribute(aString.substring(0, iContentEndPos), "format");

			// next we check if the start tag contains a 'separator' attribute
			sSeparator = extractAttribute(aString.substring(0, iContentEndPos), "separator");

			// extract locale...
			Locale locale = null;
			String sLocale = extractAttribute(aString.substring(0, iContentEndPos), "locale");
			if (sLocale != null && !sLocale.isEmpty()) {
				// split locale
				StringTokenizer stLocale = new StringTokenizer(sLocale, "_");
				if (stLocale.countTokens() == 1) {
					// only language variant
					String sLang = stLocale.nextToken();
					String sCount = sLang.toUpperCase();
					locale = new Locale(sLang, sCount);
				} else {
					// language and country
					String sLang = stLocale.nextToken();
					String sCount = stLocale.nextToken();
					locale = new Locale(sLang, sCount);
				}
			}

			// extract Item Value
			sItemValue = aString.substring(iContentStartPos, iContentEndPos);

			// format field value
			List<?> vValue = documentContext.getItemValue(sItemValue);

			String sResult = formatItemValues(vValue, sSeparator, sFormat, locale);

			// now replace the tag with the result string
			aString = aString.substring(0, iTagStartPos) + sResult + aString.substring(iTagEndPos);
		}

		return aString;

	}

	/**
	 * this method formats a string object depending of an attribute type.
	 * MultiValues will be separated by the provided separator
	 */
	public static String formatItemValues(Collection<?> aItem, String aSeparator, String sFormat, Locale locale) {

		StringBuffer sBuffer = new StringBuffer();

		if (aItem == null)
			return "";

		for (Object aSingleValue : aItem) {
			String aValue = formatObjectValue(aSingleValue, sFormat, locale);
			sBuffer.append(aValue);
			// append delimiter
			if (aSeparator != null) {
				sBuffer.append(aSeparator);
			}
		}

		String sString = sBuffer.toString();

		// cut last separator
		if (aSeparator != null && sString.endsWith(aSeparator)) {
			sString = sString.substring(0, sString.lastIndexOf(aSeparator));
		}

		return sString;

	}

	/**
	 * this method formats a string object depending of an attribute type.
	 * MultiValues will be separated by the provided separator
	 */
	public static String formatItemValues(Collection<?> aItem, String aSeparator, String sFormat) {
		return formatItemValues(aItem, aSeparator, sFormat, null);
	}

	/**
	 * This helper method test the type of an object provided by a
	 * itemcollection and formats the object into a string value.
	 * 
	 * Only Date Objects will be formated into a modified representation. other
	 * objects will be returned using the toString() method.
	 * 
	 * If an optional format is provided this will be used to format date
	 * objects.
	 * 
	 * @param o
	 * @return
	 */
	private static String formatObjectValue(Object o, String format, Locale locale) {

		Date dateValue = null;

		// now test the objct type to date
		if (o instanceof Date) {
			dateValue = (Date) o;
		}

		if (o instanceof Calendar) {
			Calendar cal = (Calendar) o;
			dateValue = cal.getTime();
		}

		// format date string?
		if (dateValue != null) {
			String singleValue = "";
			if (format != null && !"".equals(format)) {
				// format date with provided formater
				try {
					SimpleDateFormat formatter = null;
					if (locale != null) {
						formatter = new SimpleDateFormat(format, locale);
					} else {
						formatter = new SimpleDateFormat(format);
					}
					singleValue = formatter.format(dateValue);
				} catch (Exception ef) {
					Logger logger = Logger.getLogger(AbstractPlugin.class.getName());
					logger.warning("AbstractPlugin: Invalid format String '" + format + "'");
					logger.warning("AbstractPlugin: Can not format value - error: " + ef.getMessage());
					return "" + dateValue;
				}
			} else
				// use standard formate short/short
				singleValue = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(dateValue);

			return singleValue;
		}

		return o.toString();
	}

	private String extractAttribute(String aString, String attributeName) {
		int iTagStartPos = -1;
		String sAttributeValue = null;
		// next we check if the start tag contains a 'format' attribute
		int iAttributeStartPos = aString.toLowerCase().indexOf(attributeName + "=", iTagStartPos);
		// extract format string if available
		if (iAttributeStartPos > -1) {
			iAttributeStartPos = aString.indexOf("\"", iAttributeStartPos) + 1;
			int iAttributEndPos = aString.indexOf("\"", iAttributeStartPos + 1);
			sAttributeValue = aString.substring(iAttributeStartPos, iAttributEndPos);
		}

		return sAttributeValue;
	}

	/**
	 * This method merges the values from a SourceList into a valueList and
	 * removes duplicates.
	 * 
	 * @param valueList
	 * @param sourceList
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void mergeValueList(List valueList, List sourceList) {
		if ((sourceList != null) && (sourceList.size() > 0)) {
			for (Object o : sourceList) {
				if (valueList.indexOf(o) == -1)
					valueList.add(o);
			}
		}
	}

	/**
	 * This method merges the values of fieldList into valueList and test for
	 * duplicates.
	 * 
	 * If an entry of the fieldList is a single key value, than the values to be
	 * merged are read from the corresponding documentContext property
	 * 
	 * e.g. 'namTeam' -> maps the values of the documentContext property
	 * 'namteam' into the valueList
	 * 
	 * If an entry of the fieldList is in square brackets, than the comma
	 * separated elements are mapped into the valueList
	 * 
	 * e.g. '[user1,user2]' - maps the values 'user1' and 'user2' int the
	 * valueList. Also Curly brackets are allowed '{user1,user2}'
	 * 
	 * 
	 * @param valueList
	 * @param fieldList
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void mergeFieldList(ItemCollection documentContext, List valueList, List<String> fieldList) {
		if (valueList == null || fieldList == null)
			return;
		List<?> values = null;
		if (fieldList.size() > 0) {
			// iterate over the fieldList
			for (String key : fieldList) {
				if (key == null) {
					continue;
				}
				key = key.trim();
				// test if key contains square or curly brackets?
				if ((key.startsWith("[") && key.endsWith("]")) || (key.startsWith("{") && key.endsWith("}"))) {
					// extract the value list with regExpression (\s matches any
					// white space, The * applies the match zero or more times.
					// So \s* means "match any white space zero or more times".
					// We look for this before and after the comma.)
					values = Arrays.asList(key.substring(1, key.length() - 1).split("\\s*,\\s*"));
				} else {
					// extract value list form documentContext
					values = documentContext.getItemValue(key.toString());
				}
				// now append the values into p_VectorDestination
				if ((values != null) && (values.size() > 0)) {
					for (Object o : values) {
						// append only if not used
						if (valueList.indexOf(o) == -1)
							valueList.add(o);
					}
				}
			}
		}

	}

	/**
	 * This method removes duplicates and null values from a vector.
	 * 
	 * @param valueList
	 *            - list of elements
	 */
	public List<?> uniqueList(List<Object> valueList) {
		int iVectorSize = valueList.size();
		Vector<Object> cleanedVector = new Vector<Object>();

		for (int i = 0; i < iVectorSize; i++) {
			Object o = valueList.get(i);
			if (o == null || cleanedVector.indexOf(o) > -1 || "".equals(o.toString()))
				continue;

			// add unique object
			cleanedVector.add(o);
		}
		valueList = cleanedVector;
		// do not work with empty vectors....
		if (valueList.size() == 0)
			valueList.add("");

		return valueList;
	}
}