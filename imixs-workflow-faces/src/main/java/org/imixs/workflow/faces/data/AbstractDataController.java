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

package org.imixs.workflow.faces.data;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentService;

import jakarta.enterprise.context.Conversation;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;

/**
 * This is the abstract base class for the CDI beans DocumentController and
 * WorkflowController.
 * <p>
 * To load a document the methods load(id) and onLoad() can be used. The method
 * load expects the uniqueId of a document to be loaded. The onLoad() method
 * extracts the uniqueid from the query parameter 'id'. This is the recommended
 * way to support bookmarkable URLs. To load a document the onLoad method can be
 * triggered by an jsf viewAction placed in the header of a JSF page:
 * 
 * <pre>
 * {@code
    <f:metadata>
      <f:viewAction action="... documentController.onLoad()" />
    </f:metadata> }
 * </pre>
 * <p>
 * A bookmarkable URL looks like this:
 * <p>
 * {@code /myForm.xthml?id=[UNIQUEID] }
 * <p>
 * In combination with the viewAction the DocumentController is automatically
 * initialized.
 * <p>
 * https://stackoverflow.com/questions/6377798/what-can-fmetadata-fviewparam-and-fviewaction-be-used-for
 * 
 *
 * @author rsoika
 * @version 1.0
 */
public abstract class AbstractDataController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(AbstractDataController.class.getName());

    private String defaultType;

    @Inject
    private Conversation conversation;

    protected ItemCollection data = null;

    @Inject
    private DocumentService documentService;

    public DocumentService getDocumentService() {
        return documentService;
    }

    /**
     * This method returns the Default 'type' attribute of the local workitem.
     */
    public String getDefaultType() {
        return defaultType;
    }

    /**
     * This method set the default 'type' attribute of the local workitem.
     * 
     * Subclasses may overwrite the type
     * 
     * @param type
     */
    public void setDefaultType(String type) {
        this.defaultType = type;
    }

    /**
     * Reset current document
     */
    public void reset() {
        data = new ItemCollection();
        data.replaceItemValue("type", getDefaultType());
    }

    /**
     * Returns true if the current document was never saved before by the
     * DocumentService. This is indicated by the time difference of $modified and
     * $created.
     * 
     * @return
     */
    public boolean isNewWorkitem() {
        if (data == null) {
            return false;
        }
        Date created = data.getItemValueDate("$created");
        Date modified = data.getItemValueDate("$modified");
        return (modified == null || created == null);
    }

    /**
     * This method extracts a $uniqueid from the query param 'id' and loads the
     * workitem. After the workitm was loaded, a new conversation is started.
     * <p>
     * The method is not running during a JSF Postback of in case of a JSF
     * validation error.
     */
    public void onLoad() {
        logger.finest("......onload...");
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (!facesContext.isPostback() && !facesContext.isValidationFailed()) {
            // ...
            FacesContext fc = FacesContext.getCurrentInstance();
            Map<String, String> paramMap = fc.getExternalContext().getRequestParameterMap();
            // try to extract the uniqueid form the query string...
            String uniqueid = paramMap.get("id");
            if (uniqueid == null || uniqueid.isEmpty()) {
                // alternative 'workitem=...'
                uniqueid = paramMap.get("workitem");
            }
            load(uniqueid);
        }
    }

    /**
     * Loads a workitem by a given $uniqueid and starts a new conversaton. The
     * conversaion will be ended after the workitem was processed or after the
     * MaxInactiveInterval from the session.
     * 
     * @param uniqueid
     */
    public void load(String uniqueid) {
        if (uniqueid != null && !uniqueid.isEmpty()) {
            logger.log(Level.FINEST, "......load uniqueid={0}", uniqueid);
            data = documentService.load(uniqueid);
            if (data == null) {
                data = new ItemCollection();
            }
            startConversation();

        }
    }

    /**
     * Closes the current conversation and reset the data item. A conversation is
     * automatically started by the methods load() and onLoad(). You can call the
     * close() method in a actionListener on any JSF navigation action.
     */
    public void close() {
        if (!conversation.isTransient()) {
            logger.log(Level.FINEST, "......stopping conversation, id={0}", conversation.getId());
            conversation.end();
        }
    }

    /**
     * Starts a new conversation
     */
    protected void startConversation() {
        if (conversation.isTransient()) {
            conversation.setTimeout(
                    ((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest())
                            .getSession().getMaxInactiveInterval() * 1000);
            conversation.begin();
            logger.log(Level.FINEST, "......start new conversation, id={0}", conversation.getId());
        }
    }

}
