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
