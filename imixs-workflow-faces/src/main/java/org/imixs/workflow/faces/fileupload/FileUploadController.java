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

package org.imixs.workflow.faces.fileupload;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.FileData;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentEvent;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.faces.data.WorkflowEvent;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Conversation;
import jakarta.enterprise.context.ConversationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.interceptor.Interceptor;
import jakarta.servlet.http.Part;

/**
 * The FileUploadController is a conversation scoped bean and used to transfer
 * the parts to the associated workitem.
 * <code>jakarta.servlet.http.Part</code> is a JSF 4.0 component
 * 
 * @author rsoika
 */
@Named
@ConversationScoped
public class FileUploadController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static Logger logger = Logger.getLogger(FileUploadController.class.getName());

    public static final String FILEUPLOAD_ERROR = "FILEUPLOAD_ERROR";

    private List<Part> files;
    private ItemCollection workitem = null;
    private boolean isCompleted = true;

    @Inject
    private Conversation conversation;

    public List<Part> getFiles() {
        return files;
    }

    public void setFiles(List<Part> files) {
        this.files = files;
        isCompleted = false;
    }

    public ItemCollection getWorkitem() {
        return workitem;
    }

    public void setWorkitem(ItemCollection workitem) {
        this.workitem = workitem;
    }

    /**
     * WorkflowEvent listener to add uploaded file parts into the current workitem.
     * If no workitem was specified the component takes the workitem of the
     * <code>WorkflowController</code>.
     * 
     * The observer runs before normal application observers
     * (Interceptor.Priority.LIBRARY_BEFORE)
     * 
     * @param workflowEvent
     * @throws AccessDeniedException
     */
    public void onWorkflowEvent(@Observes @Priority(Interceptor.Priority.LIBRARY_BEFORE) WorkflowEvent workflowEvent)
            throws PluginException {

        int eventType = workflowEvent.getEventType();

        // before the workitem is saved we update the field txtOrderItems
        if (WorkflowEvent.WORKITEM_BEFORE_PROCESS == eventType) {
            if (workitem != null) {
                attacheFiles(workitem);
            } else {
                attacheFiles(workflowEvent.getWorkitem());
            }

        }

    }

    /**
     * In case no WorkflowController was used we observer also the before Save event
     * 
     * @param documentEvent
     * @throws PluginException
     */
    public void onDocumentEvent(@Observes @Priority(Interceptor.Priority.LIBRARY_BEFORE) DocumentEvent documentEvent)
            throws PluginException {

        int eventType = documentEvent.getEventType();

        // before the workitem is saved we update the field txtOrderItems
        if (DocumentEvent.ON_DOCUMENT_SAVE == eventType) {
            if (workitem != null) {
                attacheFiles(workitem);
            } else {
                attacheFiles(documentEvent.getDocument());
            }
        }

    }

    /**
     * Attaches the uploaded file objects to the current workitem.
     * 
     * @throws PluginException
     */
    public void attacheFiles(ItemCollection workitem) throws PluginException {
        if (files == null || files.size() == 0 || isCompleted) {
            return; // no attachments to upload!
        }
        logger.log(Level.FINE, "uploaded file size:{0}", files.size());
        for (Part part : files) {
            String submittedFilename = part.getSubmittedFileName();
            String name = Paths.get(part.getSubmittedFileName()).getFileName().toString();
            long size = part.getSize();
            String contentType = part.getContentType();
            logger.log(Level.FINE, "uploaded file: submitted filename: {0}, name:{1}, size:{2}, content type: {3}",
                    new Object[] {
                            submittedFilename, name, size, contentType
                    });

            part.getHeaderNames()
                    .forEach(headerName -> logger.log(Level.FINE, "header name: {0}, value: {1}", new Object[] {
                            headerName, part.getHeader(headerName)
                    }));

            try {
                InputStream inputStream = part.getInputStream();
                final byte[] bytes;
                try (inputStream) {
                    bytes = inputStream.readAllBytes();
                }

                FileData filedata = new FileData(name, bytes, contentType, null);

                workitem.addFileData(filedata);

            } catch (IOException e) {
                throw new PluginException(FileUploadController.class.getSimpleName(),
                        "FILEUPLOAD_ERROR",
                        "failed to uplaod file parts: " + e.getMessage(), e);
            }

        }
        // Set completed status
        isCompleted = true;
    }

    /**
     * get the file size for a given filename in human readable format
     * <p>
     * In case the Imixs-Archive API is connected, the file size is stored in the
     * attriubte 'size'
     * 
     * @param sFilename - filename to be removed
     * @return - filsize in human readable string
     */
    @SuppressWarnings("unchecked")
    public String getFileSize(String aFilename) {
        if (workitem != null) {
            FileData fileData = workitem.getFileData(aFilename);
            double bytes = fileData.getContent().length;
            if (bytes == 0) {
                // test if we have the attribute size
                List<Object> sizeAttribute = (List<Object>) fileData.getAttribute("size");
                if (sizeAttribute != null && sizeAttribute.size() > 0) {
                    try {
                        bytes = Double.parseDouble(sizeAttribute.get(0).toString());
                    } catch (NumberFormatException n) {
                        logger.log(Level.WARNING, "unable to parse size attribute in FileData for file ''{0}''",
                                aFilename);
                    }
                }
            }
            if (bytes >= 1000000000) {
                bytes = (bytes / 1000000000);
                return round(bytes) + " GB";
            } else if (bytes >= 1000000) {
                bytes = (bytes / 1000000);
                return round(bytes) + " MB";
            } else if (bytes >= 1000) {
                bytes = (bytes / 1000);
                return round(bytes) + " KB";
            } else {
                return round(bytes) + " bytes";
            }
        }
        return "";
    }

    /**
     * helper method to round for 2 digits.
     * 
     * @param value
     * @param places
     * @return
     */
    public static double round(double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /**
     * Returns the current conversation id. This id is passed to the
     * AjaxFileUploadServelt URIs so make sure that the correct FileUploadController
     * is injected.
     * 
     * @return
     */
    public String getCID() {
        if (conversation != null) {
            return conversation.getId();
        } else {
            // no conversation injected!
            return "";
        }
    }

    /**
     * Removes a file object from a given workitem.
     * 
     * @param sFilename - filename to be removed
     * @return - null
     */
    public void removePersistedFile(String aFilename) {
        if (workitem != null) {
            workitem.removeFile(aFilename);
        }
    }
}
