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
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.xml.XMLDocument;
import org.imixs.workflow.xml.XMLDocumentAdapter;

/**
 * This class translates a XMLDocument into a HTML representation.
 * 
 * @author rsoika
 * @version 1.1
 */
@Provider
@Produces(MediaType.TEXT_HTML)
public class XMLItemCollectionWriter implements MessageBodyWriter<XMLDocument> {

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return XMLDocument.class.isAssignableFrom(type);
    }

    public void writeTo(XMLDocument xmlItemCollection, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(entityStream));

        bw.write("<html>");
        printHead(bw, mediaType.toString(), null);
        bw.write("<body>");
        try {
            bw.write("<h1>Entity</h1>");

            printXMLItemCollectionHTML(bw, xmlItemCollection);

        } catch (Exception e) {
            bw.write("ERROR<br>");
            // e.printStackTrace(bw.);
        }

        bw.write("</body>");
        bw.write("</html>");

        bw.flush();
    }

    public long getSize(XMLDocument arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
        return -1;
    }

    /**
     * This Method prints a single XMLItemCollection in html format. The items are
     * sorted by name
     * 
     * @param out
     * @param workItem
     * @throws IOException
     */
    @SuppressWarnings("rawtypes")
    public static void printXMLItemCollectionHTML(BufferedWriter bw, XMLDocument xmlworkItem) throws IOException {
        boolean trClass = false;

        ItemCollection workItem = XMLDocumentAdapter.putDocument(xmlworkItem);
        bw.write("<table><tbody>");

        bw.write("<tr class=\"a\">");
        bw.write("<th colspan=\"2\"><b>UniqueID: " + workItem.getItemValueString("$uniqueid") + "</b></th></tr>");
        bw.write("<tr class=\"a\">");
        bw.write("<th>Name</th><th>Value</th></tr>");

        // sort values by using a treemap.....
        Map<String, Object> map = new TreeMap<String, Object>(workItem.getAllItems());
        Set set2 = map.entrySet();
        Iterator iter = set2.iterator();
        while (iter.hasNext()) {
            // WorkItemAttribute da = new WorkItemAttribute();
            Map.Entry mapEntry = (Map.Entry) iter.next();
            String sName = mapEntry.getKey().toString();
            List value = (List) mapEntry.getValue();

            if (trClass)
                bw.write("<tr class=\"a\">");
            else
                bw.write("<tr class=\"b\">");
            trClass = !trClass;

            bw.write("<td>" + sName + "</td><td>" + convertValuesToString(value) + "</td></tr>");
        }
        bw.write("</tbody></table>");
        bw.write("<br/><br/>");
    }

    /**
     * This method converts the Values of a vector into a string representation.
     * 
     * Multivalues will be separated with '~' characters. Date Objects will be
     * converted into a short String representation taking the server locale
     * 
     * 
     * @param values
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static String convertValuesToString(List values) {
        String convertedValue = "";

        if (values == null)
            return convertedValue;

        boolean bFirstValue = true;
        // Iterate over vector list
        Iterator iter = values.iterator();
        while (iter.hasNext()) {
            Object o = iter.next();
            Date dateValue = null;
            // now test the objct type to date
            if (o instanceof Date) {
                dateValue = (Date) o;
            }
            if (o instanceof Calendar) {
                Calendar cal = (Calendar) o;
                dateValue = cal.getTime();
            }

            // if it is not the first value then add the delimiter ~
            String singleValue = "";
            if (!bFirstValue)
                convertedValue += "~";

            // if value is a date object format date into a string
            // otherwise take the value as it is
            if (dateValue != null) {
                singleValue = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(dateValue);
            } else {
                // take value as it is
                if (o != null)
                    singleValue = singleValue + o.toString();
            }

            convertedValue += singleValue;
            bFirstValue = false;
        }

        // return values as string
        return convertedValue;
    }

    /**
     * This method prints the generic HTML Header for HTML output, including a
     * default CSS definition for table layout. The meta tag Content-Type is
     * generated in case a contentType and encoding is provided.
     * 
     * @param bw
     * @throws IOException
     */
    public static void printHead(BufferedWriter bw, String contentType, String encoding) throws IOException {
        bw.write("<head>");

        if (contentType != null && encoding != null) {
            bw.write("<meta http-equiv=\"Content-Type\" content=\"" + contentType + "; charset=" + encoding + "\"/>");
        }
        bw.write("<style>");
        bw.write("table {width: 100%;font-size:small;} ");
        bw.write("body,td,select,input,li {font-family: Verdana, Helvetica, Arial, sans-serif;} ");
        bw.write("table th {border-bottom:1px solid #ccc;text-align: left;font-weight: bold;} ");
        bw.write("table tr td {border-bottom:1px solid #ccc;} ");
        bw.write("table tr.a {} table tr.b {}");
        bw.write("</style>");
        bw.write("</head>");
    }

}
