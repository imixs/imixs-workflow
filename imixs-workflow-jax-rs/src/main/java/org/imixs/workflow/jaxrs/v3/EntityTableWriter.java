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

package org.imixs.workflow.jaxrs.v3;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.util.XMLParser;

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
	 * 
	 */
	@SuppressWarnings({ "rawtypes" })
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
			String sLabel=null;
			// test if the attribute name contains a formating definition
			int fPos=attr.toLowerCase().indexOf("<format");
			if (fPos>-1) {
				// test if a label is defined
				sLabel = XMLParser.findAttribute(attr, "label");
				if (sLabel != null) {
					bw.write("<th>" + sLabel + "</th>");
				} else {
					bw.write("<th>" + attr.substring(0,fPos) + "</th>");
				}
			}
			else {
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
				for (String itemName : entityCollection.getAttributeList()) {
					// test if item name contains format or converter definition
					int i=itemName.toLowerCase().indexOf("<format");
					if (i>-1) {
						itemName=itemName.substring(0,i);
					}
					i=itemName.toLowerCase().indexOf("<convert");
					if (i>-1) {
						itemName=itemName.substring(0,i);
					}
					
					List vValues = itemCol.getItemValue(itemName);
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

}
