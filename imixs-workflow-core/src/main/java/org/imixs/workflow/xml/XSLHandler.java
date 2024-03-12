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

package org.imixs.workflow.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.imixs.workflow.ItemCollection;

/**
 * This class can be used to transform xml by XSL template.
 * 
 * The class is used by the ReportRestService to execute a report and also by
 * the MailPluign to transform the mail body
 * 
 * @author imixs.com - Ralph Soika
 * @version 1.0
 */

public class XSLHandler {
    private static Logger logger = Logger.getLogger(XSLHandler.class.getName());

    /**
     * This method transforms an XML source with a provided XSL template. The result
     * will be written into a output stream.
     * 
     * @param xmlSource -
     * @param xslSource
     * @param encoding  (default UTF-8)
     * @return
     * @throws UnsupportedEncodingException
     * @throws TransformerException
     */

    public static void transform(String xmlSource, String xslSource, String encoding, OutputStream output)
            throws UnsupportedEncodingException, TransformerException {
        boolean debug = logger.isLoggable(Level.FINE);
        try {
            if (encoding == null || encoding.isEmpty()) {
                encoding = "UTF-8";
            }
            // Setup XSLT
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            // Set secure process - see #852
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);


            if (debug) {
                logger.finest("......xslTransformation: encoding=" + encoding);
            }
            // generate XML InputStream Reader with encoding
            ByteArrayInputStream baisXML = new ByteArrayInputStream(xmlSource.getBytes());
            InputStreamReader isreaderXML;

            isreaderXML = new InputStreamReader(baisXML, encoding);

            Source xmlSrc = new StreamSource(isreaderXML);

            // generate XSL InputStream Reader with encoding
            ByteArrayInputStream baisXSL = new ByteArrayInputStream(xslSource.getBytes());
            InputStreamReader isreaderXSL = new InputStreamReader(baisXSL, encoding);
            Source xslSrc = new StreamSource(isreaderXSL);

            Transformer trans = transformerFactory.newTransformer(xslSrc);
            trans.transform(xmlSrc, new StreamResult(output));

        } finally {

        }

    }

    /**
     * This method transforms an Collection of Documents into XML and translates the
     * result based on a provided XSL template. The result will be written into a
     * output stream.
     * 
     * @param xmlSource -
     * @param xslSource
     * @param encoding  (default UTF-8)
     * @return
     * @throws JAXBException
     * @throws TransformerException
     * @throws IOException
     */
    public static void transform(List<ItemCollection> dataSource, String xslSource, String encoding,
            OutputStream output) throws JAXBException, TransformerException, IOException {

        if (encoding == null || encoding.isEmpty()) {
            encoding = "UTF-8";
        }
        XMLDataCollection xmlDataCollection = XMLDataCollectionAdapter.getDataCollection(dataSource);

        StringWriter writer = new StringWriter();

        JAXBContext context = JAXBContext.newInstance(XMLDataCollection.class);
        Marshaller m = context.createMarshaller();
        m.setProperty("jaxb.encoding", encoding);
        m.marshal(xmlDataCollection, writer);

        XSLHandler.transform(writer.toString(), xslSource, encoding, output);
    }

    /**
     * This method transforms a single Documents (ItemCollection) into XML and
     * translates the result based on a provided XSL template. The result will be
     * written into a output stream.
     * 
     * @param xmlSource -
     * @param xslSource
     * @param encoding  (default UTF-8)
     * @return
     * @throws JAXBException
     * @throws TransformerException
     * @throws IOException
     */
    public static void transform(ItemCollection dataSource, String xslSource, String encoding, OutputStream output)
            throws JAXBException, TransformerException, IOException {

        // byte[] result=null;
        if (encoding == null || encoding.isEmpty()) {
            encoding = "UTF-8";
        }
        XMLDocument xmlDocument = XMLDocumentAdapter.getDocument(dataSource);

        StringWriter writer = new StringWriter();

        JAXBContext context = JAXBContext.newInstance(XMLDocument.class);
        Marshaller m = context.createMarshaller();
        m.setProperty("jaxb.encoding", encoding);
        m.marshal(xmlDocument, writer);

        XSLHandler.transform(writer.toString(), xslSource, encoding, output);
    }
}
