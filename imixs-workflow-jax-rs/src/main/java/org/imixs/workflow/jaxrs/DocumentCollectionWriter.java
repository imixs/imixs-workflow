/*  
 *  Imixs-Workflow 
 *  
 *  Copyright (C) 2001-2020 Imixs Software Solutions GmbH,  
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
 *      https://www.imixs.org
 *      https://github.com/imixs/imixs-workflow
 *  
 *  Contributors:  
 *      Imixs Software Solutions GmbH - Project Management
 *      Ralph Soika - Software Developer
 */

package org.imixs.workflow.jaxrs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import org.imixs.workflow.xml.XMLDataCollection;
import org.imixs.workflow.xml.XMLDocument;

/**
 * This MessageBodyWriter generates an HTML representation from a
 * DocumetCollection
 * 
 * @author rsoika
 *
 */
@Provider
@Produces(MediaType.TEXT_HTML)
public class DocumentCollectionWriter implements MessageBodyWriter<XMLDataCollection> {

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return XMLDataCollection.class.isAssignableFrom(type);
    }

    public void writeTo(XMLDataCollection entityCollection, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(entityStream));

        bw.write("<html>");
        XMLItemCollectionWriter.printHead(bw, mediaType.toString(), null);
        bw.write("<body>");
        try {
            bw.write("<h1>EntityCollection</h1>");
            bw.write("<h2>" + entityCollection.getDocument().length + " Entries</h2>");

            for (XMLDocument xmlworkItem : entityCollection.getDocument()) {
                XMLItemCollectionWriter.printXMLItemCollectionHTML(bw, xmlworkItem);

            }
        } catch (Exception e) {
            bw.write("ERROR<br>");
            // e.printStackTrace(bw.);
        }

        bw.write("</body>");
        bw.write("</html>");

        bw.flush();
    }

    public long getSize(XMLDataCollection arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
        return -1;
    }

}
