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

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import jakarta.enterprise.context.Conversation;
import jakarta.enterprise.context.ConversationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.imixs.workflow.FileData;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.faces.data.WorkflowEvent;

import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.logging.Level;

/**
 * The FileUploadController is a conversation scoped bean and used to hold the
 * upladed files and transfere the to the accoicated workitem. The
 * AjaxFileUploadServlet injects this bean to provide new file data.
 * 
 * @see AjaxFileUploadServlet.doPost
 * @author rsoika
 * 
 */
@Named
@ConversationScoped
public class FileUploadController implements Serializable {

    private static final long serialVersionUID = 1L;

    private ItemCollection workitem = null;

    private List<FileData> _tmpFiles = null; // temporarly file list.
    private List<FileData> _persistedFiles = null; // persisted file list.

    private static final Logger logger = Logger.getLogger(FileUploadController.class.getName());

    @Inject
    private Conversation conversation;

    /**
     * Setter method to get an instance of the current workitem the FileData should
     * be stored.
     * 
     * @return
     */
    public ItemCollection getWorkitem() {
        return workitem;
    }

    /**
     * This method set the current workitem and starts a new conversation. With this
     * mechanism the fileUploadController bean can be used in multiple browser tabs
     * or browser sessions.
     * 
     * @param workitem
     */
    public void setWorkitem(ItemCollection workitem) {
        this.workitem = workitem;

        if (workitem != null) {
            // start new conversation...
            if (conversation.isTransient()) {
                conversation.setTimeout(
                        ((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest())
                                .getSession().getMaxInactiveInterval() * 1000);
                conversation.begin();
                logger.log(Level.FINEST, "......starting new conversation, id={0}", conversation.getId());
            }
            reset();
            for (FileData fileData : workitem.getFileData()) {
                _persistedFiles.add(fileData);
            }
        }
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
     * WorkflowEvent listener
     * <p>
     * If a new WorkItem was created the file upload will be reset.
     * 
     * 
     * @param workflowEvent
     */
    public void onWorkflowEvent(@Observes WorkflowEvent workflowEvent) {
        if (workflowEvent == null)
            return;

        if (WorkflowEvent.WORKITEM_CREATED == workflowEvent.getEventType()) {
            // reset file data...
            reset();
        }

    }

    /**
     * This method is called by the AjaxFileUpload Servlet. The method adds the file
     * to the workitem but also updates the list of temporary files, which are not
     * yet persisted.
     * 
     * @param document
     * @param aFilename
     */
    public void addAttachedFile(FileData filedata) {
        if (workitem != null) {
            _tmpFiles.add(filedata);
            workitem.addFileData(filedata);
        }
    }

    /**
     * Removes a attached file object from the tmp list of uploaded files.
     * 
     * @param sFilename - filename to be removed
     * @return - null
     */
    public void removeAttachedFile(String aFilename) {
        if (workitem != null) {
            workitem.removeFile(aFilename);
            // remove from tmp list
            for (Iterator<FileData> iterator = _tmpFiles.iterator(); iterator.hasNext();) {
                FileData tmp = iterator.next();
                if (tmp.getName().equals(aFilename)) {
                    iterator.remove();
                }
            }
        }

    }

    /**
     * Removes a file object from a given workitem. Here we operate on a given
     * workitem as the imixsFileUpload.xhtml has no idea of he current conversation
     * scoped controller.
     * 
     * @param sFilename - filename to be removed
     * @return - null
     */
    public void removePersistedFile(String aFilename) {
        if (workitem != null) {
            workitem.removeFile(aFilename);
        }
        // remove from persisted list
        for (Iterator<FileData> iterator = _persistedFiles.iterator(); iterator.hasNext();) {
            FileData tmp = iterator.next();
            if (tmp.getName().equals(aFilename)) {
                iterator.remove();
            }
        }
    }

    /**
     * returns the list of currently new attached files. This list is not equal the
     * $file item!
     * 
     * @return
     */
    public List<FileData> getAttachedFiles() {
        if (_tmpFiles == null) {
            _tmpFiles = new ArrayList<FileData>();
        }
        return _tmpFiles;
    }

    /**
     * returns the list of already persisted files. This list is not equal the $file
     * item!
     * 
     * @return
     */
    public List<FileData> getPersistedFiles() {
        if (_persistedFiles == null) {
            _persistedFiles = new ArrayList<FileData>();
        }
        return _persistedFiles;
    }

    /**
     * reset the temp and persisted file variables.
     */
    public void reset() {
        _tmpFiles = new ArrayList<FileData>();
        _persistedFiles = new ArrayList<FileData>();
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
                        logger.log(Level.WARNING, "unable to parse size attribute in FileData for file ''{0}''", aFilename);
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

}
