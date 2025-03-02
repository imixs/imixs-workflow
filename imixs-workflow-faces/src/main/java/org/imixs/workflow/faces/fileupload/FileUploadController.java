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
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.FileData;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.faces.data.WorkflowController;
import org.imixs.workflow.faces.data.WorkflowEvent;

import jakarta.enterprise.context.ConversationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;
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

    @Inject
    WorkflowController workflowController;

    public List<Part> getFiles() {
        return files;
    }

    public void setFiles(List<Part> files) {
        this.files = files;
    }

    /**
     * WorkflowEvent listener to add uploaded file parts into the current workitem
     * of the <code>WorkflowController</code>.
     * 
     * @param workflowEvent
     * @throws AccessDeniedException
     */
    public void onWorkflowEvent(@Observes WorkflowEvent workflowEvent) throws PluginException {

        int eventType = workflowEvent.getEventType();
        ItemCollection workitem = workflowEvent.getWorkitem();
        if (workitem == null || workflowController == null) {
            return;
        }

        // before the workitem is saved we update the field txtOrderItems
        if (WorkflowEvent.WORKITEM_BEFORE_PROCESS == eventType) {
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
                    workflowController.getWorkitem().addFileData(filedata);

                } catch (IOException e) {
                    throw new PluginException(FileUploadController.class.getSimpleName(),
                            "FILEUPLOAD_ERROR",
                            "failed to uplaod file parts: " + e.getMessage(), e);
                }

            }
        }

    }

}
