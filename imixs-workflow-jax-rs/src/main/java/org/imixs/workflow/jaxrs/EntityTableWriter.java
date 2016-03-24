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

package org.imixs.workflow.jaxrs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.plugins.AbstractPlugin;
import org.imixs.workflow.xml.EntityTable;
import org.imixs.workflow.xml.XMLItemCollection;
import org.imixs.workflow.xml.XMLItemCollectionAdapter;

/**
 * This MessageBodyWriter generates an HTML representation from a EntityTable.
 * The output is a table format where each entity has the same columns.
 * 
 * @author rsoika
 * 
 */
@Provider
@Produces("text/html")
public class EntityTableWriter implements MessageBodyWriter<EntityTable> {

	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return EntityTable.class.isAssignableFrom(type);
	}

	/**
	 * This method prints the collection data into a HTML table
	 * 
	 * issue #144
	 * 
	 * the method parses the attribute name for a formating expression e.g.
	 * 
	 * 
	 * datDate<format locale="de" label="Date">yy-dd-mm</format>
	 * 
	 * 
	 * 
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void writeTo(EntityTable entityCollection, Class<?> type, Type genericType, Annotation[] annotations,
			MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
			throws IOException, WebApplicationException {

		boolean trClass = true;

		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(entityStream));

		bw.write("<html>");
		XMLItemCollectionWriter.printHead(bw);

		bw.write("<body>");

		/* Print table header */
		bw.write("<table><tbody>");
		if (trClass)
			bw.write("<tr class=\"a\">");
		else
			bw.write("<tr class=\"b\">");
		trClass = !trClass;

		for (String attr : entityCollection.getAttributeList()) {
			
			String sLabel = extractAttribute(attr, "label");
			if (sLabel!=null) {
				bw.write("<th>" + sLabel + "</th>");
			} else {
				bw.write("<th>" + attr + "</th>");
			}
		}
		bw.write("</tr>");

		// print table body
		try {

			for (XMLItemCollection xmlworkItem : entityCollection.getEntity()) {
				/* Print row */
				if (trClass)
					bw.write("<tr class=\"a\">");
				else
					bw.write("<tr class=\"b\">");
				trClass = !trClass;

				ItemCollection itemCol = XMLItemCollectionAdapter.getItemCollection(xmlworkItem);
				for (String attr : entityCollection.getAttributeList()) {
					List vValues = null;
					// test if attr contains a formating rule
					int open = attr.toLowerCase().indexOf("<format");
					int close = attr.toLowerCase().indexOf("</format>");
					if (open != -1 && close != -1) {
						String fieldName = attr.substring(0, open );
						String format = attr.substring(open + 8, close);
						String sLocale = extractAttribute(format, "locale");
						// strip fomat string...
						format=format.substring(format.indexOf(">")+1);
						
						// create string array
						vValues = new ArrayList<String>();
						List rawValues = itemCol.getItemValue(fieldName);
						for (Object rawValue : rawValues) {
							vValues.add(formatObjectValue(rawValue, format, getLocaleFromString(sLocale)));
						}

					} else {
						vValues = itemCol.getItemValue(attr);
					}
					bw.write("<td>" + XMLItemCollectionWriter.convertValuesToString(vValues) + "</td>");
				}
				bw.write("</tr>");

			}

			bw.write("</tbody></table>");
		} catch (

		Exception e) {
			bw.write("ERROR<br>");
			// e.printStackTrace(bw.);
		}

		bw.write("</body>");
		bw.write("</html>");

		bw.flush();
	}

	public long getSize(EntityTable arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
		return -1;
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
					logger.warning("EntityTableWriter: Invalid format String '" + format + "'");
					logger.warning("EntityTableWriter: Can not format value - error: " + ef.getMessage());
					return "" + dateValue;
				}
			} else
				// use standard formate short/short
				singleValue = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(dateValue);

			return singleValue;
		}

		return o.toString();
	}

	/**
	 * Extracts an attribute from a string
	 * 
	 * 
	 * @param aString
	 * @param attributeName
	 * @return
	 */
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

	private Locale getLocaleFromString(String sLocale) {
		Locale locale = null;

		// genreate locale?
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

		return locale;
	}

}
