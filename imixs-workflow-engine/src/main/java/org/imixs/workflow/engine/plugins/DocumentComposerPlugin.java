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
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.xml.XMLDocument;
import org.imixs.workflow.xml.XMLDocumentAdapter;
import org.imixs.workflow.xml.XSLHandler;

/**
 * This DocumentComposer Plugin creates html output stored in a item. The
 * DocumentComposer is based on BPMN DataObjects assigned to the target task.
 * 
 * <pre>
 *  
 * {@code
 * 
 *    	<item name="document-composer" data-object="template1">htmloutput</item>
 * 
 * }
 * </pre>
 * 
 * @author rsoika
 * @version 2.0
 */
public class DocumentComposerPlugin extends AbstractPlugin {

    public static String ITEM_DOCUMENT_COMPOSER = "document-composer";
    public static String INVALID_DATA_OBJECT = "INVALID_DATA_OBJECT";
    public static String INVALID_XSL_FORMAT = "INVALID_XSL_FORMAT";

    private static Logger logger = Logger.getLogger(DocumentComposerPlugin.class.getName());

    @Override
    public void init(WorkflowContext actx) throws PluginException {

        super.init(actx);

    }

    /**
     * This method adds the attachments of the blob workitem to the MimeMessage
     */
    @Override
    public ItemCollection run(ItemCollection documentContext, ItemCollection event) throws PluginException {
        long l = System.currentTimeMillis();
        // get next process entity
        ItemCollection itemColNextProcess = null;
        try {
            itemColNextProcess = this.getWorkflowService().evalNextTask(documentContext);
        } catch (ModelException e) {
            throw new PluginException(DocumentComposerPlugin.class.getSimpleName(), e.getErrorCode(), e.getMessage());
        }

        ItemCollection evalItemCollection = this.getWorkflowService().evalWorkflowResult(event,"item",
                documentContext);

        // find the data object
        if (evalItemCollection != null && evalItemCollection.hasItem(ITEM_DOCUMENT_COMPOSER)) {

            String templateName = evalItemCollection.getItemValueString(ITEM_DOCUMENT_COMPOSER + ".data-object");

            // get the template
            List<String> dataObject = findDataObject(templateName, itemColNextProcess);
            String template;
            if (dataObject != null) {
                template = dataObject.get(1);
                String outputItem = evalItemCollection.getItemValueString(ITEM_DOCUMENT_COMPOSER);
                // process output...
                String output = transformXSLTemplate(documentContext, template);
                documentContext.replaceItemValue(outputItem, output);

                logger.fine("...composed document in " + (System.currentTimeMillis() - l) + "ms");
            }
        }

        return documentContext;
    }

    /**
     * Returns the data template for a given tempalte name. If templatename is null
     * or empty, the method retruns the first dataobject.
     * 
     * @param task
     * @return
     * @throws PluginException
     */
    private List<String> findDataObject(String objectName, ItemCollection task) throws PluginException {

        @SuppressWarnings("unchecked")
        List<List<String>> dataObjects = task.getItemValue("dataObjects");

        // iterate all objects...
        for (List<String> dataObj : dataObjects) {
            String name = dataObj.get(0);

            if (objectName == null || objectName.isEmpty()) {
                // return teh first default tempalte
                return dataObj;
            }
            if (objectName.equals(name)) {
                return dataObj;
            }

        }
        // no tempalte found
        throw new PluginException(this.getClass().getName(), INVALID_DATA_OBJECT,
                "dataobject with name '" + objectName + "' not defined in Task " + task.getItemValueInteger("numTask"));

    }

    /**
     * This method performs a XSL transformation based on an xslTemplate. The xml
     * source is generated form the current document context.
     * 
     * encoding is set to UTF-8
     * 
     * @return translated email body
     * @throws PluginException
     * 
     */
    public String transformXSLTemplate(ItemCollection documentContext, String xslTemplate) throws PluginException {
        String encoding;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        encoding = "UTF-8";

        logger.finest("......transfor mail body based on XSL template....");
        // Transform XML per XSL and generate output
        XMLDocument xml;
        try {
            xml = XMLDocumentAdapter.getDocument(documentContext);
            StringWriter writer = new StringWriter();

            JAXBContext context = JAXBContext.newInstance(XMLDocument.class);
            Marshaller m = context.createMarshaller();
            m.setProperty("jaxb.encoding", encoding);
            m.marshal(xml, writer);

            // create a ByteArray Output Stream
            XSLHandler.transform(writer.toString(), xslTemplate, encoding, outputStream);
            return outputStream.toString(encoding);

        } catch (Exception e) {
            logger.warning("Error processing XSL template!");
            throw new PluginException(this.getClass().getSimpleName(), INVALID_XSL_FORMAT, e.getMessage(), e);
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}
