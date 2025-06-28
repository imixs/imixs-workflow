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
import java.util.List;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.xml.DocumentTable;
import org.imixs.workflow.xml.XMLDocument;
import org.imixs.workflow.xml.XMLDocumentAdapter;

/**
 * This MessageBodyWriter generates an HTML representation from a DocumentTable.
 * The output is a data table in HTML format where each item is represented in a
 * row which has the same columns.
 * 
 * @author rsoika
 */
@Provider
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
        XMLItemCollectionWriter.printHead(bw, mediaType.toString(), documentTable.getEncoding());

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

            for (XMLDocument xmlworkItem : documentTable.getDocument()) {
                /* Print row */
                if (trClass)
                    bw.write("<tr class=\"a\">");
                else
                    bw.write("<tr class=\"b\">");
                trClass = !trClass;

                ItemCollection itemCol = XMLDocumentAdapter.putDocument(xmlworkItem);
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
