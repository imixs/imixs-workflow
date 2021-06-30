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
import java.util.logging.Logger;

import jakarta.enterprise.context.ConversationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.exceptions.AccessDeniedException;

import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;

/**
 * The DocumentController is a @ConversationScoped CDI bean to control the life
 * cycle of a ItemCollection in an JSF application without any workflow
 * functionality. The bean can be used in single page applications, as well for
 * complex page flows. The controller is easy to use and supports bookmarkable
 * URLs.
 * <p>
 * The DocumentController fires CDI events from the type WorkflowEvent. A CDI
 * bean can observe these events to participate in the processing life cycle.
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
 * After a document is loaded, a new conversation is started and the CDI event
 * WorkflowEvent.DOCUMENT_CHANGED is fired.
 * <p>
 * After a document was saved, the conversation is automatically closed. Stale
 * conversations will automatically timeout with the default session timeout.
 * <p>
 * After each call of the method save the Post-Redirect-Get is initialized with
 * the default URL from the start of the conversation. This guarantees
 * bookmakrable URLs.
 * <p>
 * Within a JSF form, the items of a document can be accessed by the getter
 * method getDocument().
 * 
 * <pre>
 *   #{documentController.document.item['$workflowstatus']}
 * </pre>
 * 
 * <p>
 * The default type of a entity created with the DataController is 'workitem'.
 * This property can be changed from a client.
 * 
 * 
 * 
 * 
 * @author rsoika
 * @version 0.0.1
 */
@Named
@ConversationScoped
public class DocumentController extends AbstractDataController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static Logger logger = Logger.getLogger(DocumentController.class.getName());

    @Inject
    protected Event<WorkflowEvent> events;

    @Inject
    private DocumentService documentService;

    public DocumentController() {
        super();
        setDefaultType("workitem");
    }

    /**
     * Returns the current workItem. If no workitem is defined the method
     * Instantiates a empty ItemCollection.
     * 
     * @return - current workItem or null if not set
     */
    public ItemCollection getDocument() {
        // do initialize an empty workItem here if null
        if (data == null) {
            reset();
        }
        return data;
    }

    /**
     * Set the current worktItem
     * 
     * @param workitem - new reference or null to clear the current workItem.
     */
    public void setDocument(ItemCollection document) {
        this.data = document;

        if (events != null) {
            events.fire(new WorkflowEvent(data, WorkflowEvent.DOCUMENT_CHANGED));
        } else {
            logger.warning("Missing CDI support for Event<WorkflowEvent> !");
        }

    }

    /**
     * This method creates an empty workItem with the default type property and the
     * property '$Creator' holding the current RemoteUser This method should be
     * overwritten to add additional Business logic here.
     * 
     */
    public void create() {
        reset();
        // initialize new ItemCollection
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        String sUser = externalContext.getRemoteUser();
        data.replaceItemValue("$Creator", sUser);
        data.replaceItemValue("namCreator", sUser); // backward compatibility

        startConversation();
        if (events != null) {
            events.fire(new WorkflowEvent(data, WorkflowEvent.DOCUMENT_CREATED));
        } else {
            logger.warning("Missing CDI support for Event<WorkflowEvent> !");
        }
        logger.finest("......document created");
    }

    /**
     * This method saves the current document.
     * <p>
     * The method fires the WorkflowEvents WORKITEM_BEFORE_SAVE and
     * WORKITEM_AFTER_SAVE.
     * 
     * 
     * @throws AccessDeniedException - if user has insufficient access rights.
     */
    public void save() throws AccessDeniedException {
        // save workItem ...
        if (events != null) {
            events.fire(new WorkflowEvent(data, WorkflowEvent.DOCUMENT_BEFORE_SAVE));
        } else {
            logger.warning("Missing CDI support for Event<WorkflowEvent> !");
        }
        
        // initialize new ItemCollection
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        String sUser = externalContext.getRemoteUser();
        data.replaceItemValue("$editor", sUser);
        data = documentService.save(data);
        if (events != null) {
            events.fire(new WorkflowEvent(data, WorkflowEvent.DOCUMENT_AFTER_SAVE));
        }
        // close conversation
        close();

        logger.finest("......ItemCollection saved");
    }

    /**
     * This action method deletes a workitem. The Method also deletes also all child
     * workitems recursive
     * 
     * @param currentSelection - workitem to be deleted
     * @throws AccessDeniedException
     */
    public void delete(String uniqueID) throws AccessDeniedException {
        ItemCollection _workitem = documentService.load(uniqueID);
        if (_workitem != null) {

            if (events != null) {
                events.fire(new WorkflowEvent(getDocument(), WorkflowEvent.DOCUMENT_BEFORE_DELETE));
            } else {
                logger.warning("Missing CDI support for Event<WorkflowEvent> !");
            }
            documentService.remove(_workitem);
            if (events != null) {
                events.fire(new WorkflowEvent(getDocument(), WorkflowEvent.DOCUMENT_AFTER_DELETE));
            }
            setDocument(null);
            logger.fine("......document " + uniqueID + " deleted");
        } else {
            logger.fine("......document '" + uniqueID + "' not found (null)");
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
        super.load(uniqueid);
        if (data != null && events != null) {
            // fire event
            events.fire(new WorkflowEvent(data, WorkflowEvent.DOCUMENT_CHANGED));
        }
    }

}
