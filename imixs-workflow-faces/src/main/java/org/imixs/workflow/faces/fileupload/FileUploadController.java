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
import org.imixs.workflow.faces.data.WorkflowController;
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
 * The FileUploadController is a conversation scoped JSF 4.0 component used to
 * transfer the parts to the associated workitem.
 * The controller supports two modes:
 * <p>
 * SingleMode:
 * <p>
 * In this mode one or multiple files can be transferred in a one request. This
 * is mainly covered by JavaScript code and works in a simple straight forward
 * way.
 * <p>
 * AjaxMode:
 * <p>
 * In the ajax mode one or multiple files can be transferred sequentiell in
 * separate ajax request. This mode is more user friendly.
 * <p>
 * 
 * <pre>
 * 
 * 	{@code <i:imixsFileUpload id="file_upload_id" 
       showattachments="true"
       workitem="#{workflowController.workitem}"
       labelButton="#{message.documents_upload}" 
       labelHelp="#{message.documents_upload_help}"/>
    }</pre>
 * 
 * @author rsoika
 */
@Named
@ConversationScoped
public class FileUploadController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static Logger logger = Logger.getLogger(FileUploadController.class.getName());

    public static final String FILEUPLOAD_ERROR = "FILEUPLOAD_ERROR";

    private ItemCollection uploadData = null; // holds the temporally file list.
    private List<Part> files;
    private ItemCollection workitem = null;
    private boolean isCompleted = true;

    @Inject
    private Conversation conversation;

    @Inject
    private WorkflowController workflowController;

    public FileUploadController() {
        uploadData = new ItemCollection();
    }

    public List<Part> getFiles() {
        return files;
    }

    public void setFiles(List<Part> files) {
        this.files = files;
        isCompleted = false;
    }

    public ItemCollection getWorkitem() {
        if (workitem == null && workflowController.getWorkitem() != null) {
            workitem = workflowController.getWorkitem();
        }
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

        // Ajax mode - check fileUploads
        if (uploadData != null && uploadData.getFileData().size() > 0) {
            // In single mode - no upladData exits
            for (FileData fileData : uploadData.getFileData()) {
                workitem.addFileData(fileData);
            }
            // reset uploadData !
            uploadData = new ItemCollection();
        }

        // Single mode - check files object list..
        if (files == null || files.size() == 0 || isCompleted) {
            // in ajax mode no file data list exits
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
     * The method adds a FileData object to the workitem
     * 
     * For example this method is used by the Imixs WopiController
     * 
     * @param fileData
     */
    public void addAttachedFile(FileData fileData) {
        addFileUpload(fileData);
    }

    /**
     * Removes a attached file object from the tmp list of uploaded files.
     * 
     * @param sFilename - filename to be removed
     * @return - null
     */
    public void removeAttachedFile(String aFilename) {
        if (getWorkitem() != null) {
            getWorkitem().removeFile(aFilename);
        }
    }

    /**
     * Returns the user info (creator / modified) by filename
     * 
     * @return
     */
    public String getUserInfo(String aFilename) {
        String result = "";
        if (getWorkitem() != null) {
            FileData fileData = getWorkitem().getFileData(aFilename);
            if (fileData != null) {
                List<Object> creatorList = (List<Object>) fileData.getAttribute("$creator");
                if (creatorList != null && creatorList.size() > 0) {
                    result = result + creatorList.get(0);
                }
                List<Object> createdList = (List<Object>) fileData.getAttribute("$created");
                if (createdList != null && createdList.size() > 0) {
                    result = result + " - " + createdList.get(0);
                }
            }
        }
        return result;
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
        if (getWorkitem() != null) {
            FileData fileData = getWorkitem().getFileData(aFilename);
            if (fileData != null) {
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
     * returns the list of currently new attached files. This list is not equal the
     * $file item!
     * 
     * This method is called by the AjaxFileUpload Servlet.
     * 
     * @return
     */
    public List<FileData> getFileUploads() {
        if (uploadData == null) {
            uploadData = new ItemCollection();
        }
        return uploadData.getFileData();
    }

    /**
     * returns the single fileData object form the upload list by name
     * 
     * This method is called by the AjaxFileUpload Servlet.
     * 
     * @return
     */
    public FileData getFileUpload(String fileName) {
        if (uploadData == null) {
            uploadData = new ItemCollection();
        }
        return uploadData.getFileData(fileName);
    }

    /**
     * Adds a FileData object to the uploaded File list
     * 
     * @param document
     * @param aFilename
     */
    public void addFileUpload(FileData fileData) {
        uploadData.addFileData(fileData);

    }

    /**
     * Removes a FileData object from the uploaded File list
     * 
     * @param document
     * @param aFilename
     */
    public void removeFileUpload(String aFilename) {
        uploadData.removeFile(aFilename);
    }

}
