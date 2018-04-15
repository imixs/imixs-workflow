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

package org.imixs.workflow.jaxrs.v40;

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


/**
 * This MessageBodyWriter generates an HTML representation from a EntityTable.
 * The output is a table format where each entity has the same columns.
 * 
 * @author rsoika
 * 
 */
@Provider
@Produces("text/html")
public class DocumentTableWriter implements MessageBodyWriter<DocumentTable> {

	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return DocumentTable.class.isAssignableFrom(type);
	}

	/**
	 * This method prints the collection data into a HTML table
	 * 
	 * 
	 */
	@SuppressWarnings({ "rawtypes" })
	public void writeTo(DocumentTable documentTable, Class<?> type, Type genericType, Annotation[] annotations,
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

		for (String label : documentTable.getLabels()) {
			bw.write("<th>" + label + "</th>");
		}
		bw.write("</tr>");

		// print table body
		try {

			for (XMLItemCollection xmlworkItem : documentTable.getDocument()) {
				/* Print row */
				if (trClass)
					bw.write("<tr class=\"a\">");
				else
					bw.write("<tr class=\"b\">");
				trClass = !trClass;

				ItemCollection itemCol = XMLItemCollectionAdapter.getItemCollection(xmlworkItem);
				for (String itemName : documentTable.getItems()) {
					// test if item name contains format or converter definition
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

	public long getSize(DocumentTable arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
		return -1;
	}

}
