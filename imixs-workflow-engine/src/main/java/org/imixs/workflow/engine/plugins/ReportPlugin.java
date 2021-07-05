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

package org.imixs.workflow.engine.plugins;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.util.logging.Logger;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import org.imixs.workflow.FileData;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.xml.XMLDataCollection;
import org.imixs.workflow.xml.XMLDocument;
import org.imixs.workflow.xml.XMLDocumentAdapter;
import org.imixs.workflow.xml.XSLHandler;

/**
 * This plug-in executes a Imixs Report definition and stores the result either
 * into the current workitem ($file) or into the file system. The corresponding
 * BPMN event provide the following properties:
 * <p>
 * <ul>
 * <li>txtReportName=Name of the Report to be processed
 * <li>txtReportFilePath= optional filename or file path the result will be
 * saved
 * <li>txtReportTarget = where the result is saved (0=workitem, 2= disk)
 * </ul>
 * <p>
 * 
 * CHANGES V 2.0
 * <p>
 * In the current version 2.0, only the processed document will be used as the
 * xml input source for the XSL transformation. A search query will currently
 * not be evaluated.
 * 
 * 
 * @author Ralph Soika
 * @version 2.0
 */
public class ReportPlugin extends AbstractPlugin {

    public static final String INVALID_CONTEXT = "INVALID_CONTEXT";
    public static final String REPORT_UNDEFINED = "REPORT_UNDEFINED";
    public static final String INVALID_REPORT_DEFINITION = "INVALID_REPORT_DEFINITION";

    private static Logger logger = Logger.getLogger(ReportPlugin.class.getName());

    /**
     * Executes a report defined defined by the event in the attribute
     * 'txtReportName'.
     * <p>
     * The XML Source used by this method is the XML representation of the current
     * document. The Query Statement will not be evaluated
     * <p>
     * 
     */
    public ItemCollection run(ItemCollection adocumentContext, ItemCollection adocumentActivity)
            throws PluginException {

        String reportName = adocumentActivity.getItemValueString("txtReportName");
        String reportFilePath = adocumentActivity.getItemValueString("txtReportFilePath");
        if ("".equals(reportFilePath))
            reportFilePath = reportName;

        // replace dynamic field values
        reportFilePath = getWorkflowService().adaptText(reportFilePath, adocumentContext);

        String reportTarget = adocumentActivity.getItemValueString("txtReportTarget");

        if ("".equals(reportName))
            return adocumentContext;

        ItemCollection itemCol = getWorkflowService().getReportService().findReport(reportName);
        if (itemCol == null) {
            // report undefined
            throw new PluginException(ReportPlugin.class.getSimpleName(), REPORT_UNDEFINED,
                    "Report '" + reportName + " is undefined", new Object[] { reportName });
        }

        String xslTemplate = itemCol.getItemValueString("xsl").trim();
        // if no XSL is provided return
        if ("".equals(xslTemplate))
            return adocumentContext;

        String sContentType = itemCol.getItemValueString("contenttype");
        if ("".equals(sContentType))
            sContentType = "text/html";

        String encoding = itemCol.getItemValueString("encoding");
        // no encoding defined so take a default encoding
        // (UTF-8)
        if ("".equals(encoding))
            encoding = "UTF-8";

        try {
            // TODO : we need to clarify if the method call unescapeXMLContent() is
            // necessary

            XMLDocument xml = XMLDocumentAdapter.getDocument(adocumentContext);
            StringWriter writer = new StringWriter();

            JAXBContext context = JAXBContext.newInstance(XMLDataCollection.class);
            Marshaller m = context.createMarshaller();
            m.setProperty("jaxb.encoding", encoding);
            m.marshal(xml, writer);

            // create a ByteArray Output Stream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                if ("application/pdf".equals(sContentType.toLowerCase())) {
                    logger.warning("FOP Transformation is not yet implementd");
                    // org.imixs.workflow.jaxrs.ReportRestService.fopTranformation(xmlContentExtended,
                    // xslTemplate, encoding,
                    // outputStream);
                } else {
                    XSLHandler.transform(writer.toString(), xslTemplate, encoding, outputStream);

                }
            } finally {
                outputStream.close();
            }

            // write to workitem
            if (reportTarget.isEmpty() || "0".equals(reportTarget)) {
                FileData fileData = new FileData(reportFilePath, outputStream.toByteArray(), sContentType, null);
                adocumentContext.addFileData(fileData);
            }
            // write to blob
            if ("1".equals(reportTarget)) {
                logger.warning(
                        "Writing into BlobWorkitem is no longer supported - please use the DMSPlugin for transfer");

            }
            // write to filesystem
            if ("2".equals(reportTarget)) {
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(reportFilePath);
                    fos.write(outputStream.toByteArray());
                    fos.flush();
                } finally {
                    if (fos != null) {
                        fos.close();
                    }
                }
            }

            return adocumentContext;
        } catch (Exception e) {
            // report undefined
            throw new PluginException(ReportPlugin.class.getSimpleName(), INVALID_REPORT_DEFINITION,
                    "Unable to process report '" + reportName + "' ", new Object[] { reportName });
        }
    }

    public void close(int status) throws PluginException {

    }

}
