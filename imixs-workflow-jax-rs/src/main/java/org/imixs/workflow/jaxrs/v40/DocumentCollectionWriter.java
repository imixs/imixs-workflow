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

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;



/**
 * This MessageBodyWriter generates an HTML representation from a DocumetCollection
 * 
 * @author rsoika
 *
 */
@Provider
@Produces("text/html")
public class DocumentCollectionWriter implements MessageBodyWriter<DocumentCollection> {

	public boolean isWriteable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return DocumentCollection.class.isAssignableFrom(type);
	}

	public void writeTo(DocumentCollection entityCollection, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders,
			OutputStream entityStream) throws IOException,
			WebApplicationException {
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
				entityStream));
		
		
		
		bw.write("<html>");
		XMLItemCollectionWriter.printHead(bw);
		bw.write("<body>");
		try {
			bw.write("<h1>EntityCollection</h1>");
			bw.write("<h2>" + entityCollection.getDocument().length + " Entries</h2>");

			for (XMLItemCollection xmlworkItem : entityCollection.getDocument()) {
				XMLItemCollectionWriter.printXMLItemCollectionHTML(bw, xmlworkItem);

			}
		} catch (Exception e) {
			bw.write("ERROR<br>");
			//e.printStackTrace(bw.);
		}
		

		bw.write("</body>");
		bw.write("</html>");
		
		bw.flush();
	}

	public long getSize(DocumentCollection arg0, Class<?> arg1, Type arg2,
			Annotation[] arg3, MediaType arg4) {
		return -1;
	}

	
	
	

	
	
	
}
