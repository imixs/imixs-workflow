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
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.imixs.workflow.ItemCollection;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

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
    private static final Logger logger = Logger.getLogger(XSLHandler.class.getName());

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
                logger.log(Level.FINEST, "......xslTransformation: encoding={0}", encoding);
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
