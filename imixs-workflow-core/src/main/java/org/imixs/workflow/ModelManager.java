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

package org.imixs.workflow;

import org.imixs.workflow.exceptions.ModelException;
import org.openbpmn.bpmn.BPMNModel;

/**
 * The interface ModelManager provides methods to get model entities from a
 * model instance. The ModelManager is used by the {@link WorkflowKernel} to
 * manage the processing life cycle of a workitem.
 * 
 * @author rsoika
 */
public interface ModelManager {

    public final static String TASK_ELEMENT = "task";
    public final static String EVENT_ELEMENT = "intermediateCatchEvent";

    /**
     * Adds a new model into the local model store
     */
    public void addModel(BPMNModel model) throws ModelException;

    /**
     * Removes a BPMNModel form the local model store
     */
    public void removeModel(String version);

    /**
     * Returns a BPMNModel by its version ($modelVersion) from the local model store
     * <p>
     * The BPMNModel instance can be used to access all BPMN model elements.
     * 
     * @param version - $modelVersion
     * @return a BPMN model instance or null if not found by $modelVersion
     * 
     * @see https://github.com/imixs/open-bpmn/tree/master/open-bpmn.metamodel
     */
    public BPMNModel getModel(String version) throws ModelException;

    /**
     * Returns the BPMN Definition entity associated with a given workitem, based on
     * its attribute "$modelVersion". The definition holds the bpmn meta data.
     * <p>
     * The method throws a {@link ModelException} if no Process can be resolved
     * based on the given model information.
     * <p>
     * The method is called by the {@link WorkflowKernel} during the processing
     * life cycle to update the process group information.
     * 
     * @param workitem
     * @return BPMN Event entity - {@link ItemCollection}
     * @throws ModelException if no event was found
     */
    public ItemCollection loadDefinition(ItemCollection workitem) throws ModelException;

    /**
     * Returns the BPMN Process entity associated with a given workitem, based on
     * its attributes "$modelVersion", "$taskID". The process holds the name
     * for the attribute $worklfowGroup
     * <p>
     * The taskID has to be unique in a process. The method throws a
     * {@link ModelException} if no Process can be resolved based on the given model
     * information.
     * <p>
     * The method is called by the {@link WorkflowKernel} during the processing
     * life cycle to update the process group information.
     * 
     * @param workitem
     * @return BPMN Event entity - {@link ItemCollection}
     * @throws ModelException if no event was found
     */
    public ItemCollection loadProcess(ItemCollection workitem) throws ModelException;

    /**
     * Returns the BPMN Task entity associated with a given workitem, based on its
     * attributes "$modelVersion" and "$taskID".
     * <p>
     * The method throws a {@link ModelException} if no Task can be resolved based
     * on the given model information.
     * <p>
     * The method is called by the {@link WorkflowKernel} during the the processing
     * life cycle.
     * 
     * @param workitem
     * @return BPMN Event entity - {@link ItemCollection}
     * @throws ModelException if no event was found
     */
    public ItemCollection loadTask(ItemCollection workitem) throws ModelException;

    /**
     * Returns the BPMN Event entity associated with a given workitem, based on its
     * attributes "$modelVersion", "$taskID" and "$eventID".
     * <p>
     * The method throws a {@link ModelException} if no Event can be resolved based
     * on the given model information.
     * <p>
     * The method is called by the {@link WorkflowKernel} to start the processing
     * life cycle.
     * 
     * @param workitem
     * @return BPMN Event entity - {@link ItemCollection}
     * @throws ModelException if no event was found
     */
    public ItemCollection loadEvent(ItemCollection workitem) throws ModelException;

    /**
     * Finds the next BPMN Element associated with a given event element. The
     * returned BPMN Element must either be an Activity (Task) element, an
     * Intermediate Catch Event (follow-up-event) or a End Event. The method must
     * not return any other BPMN elements (e.g. Gateways, Intermediate Throw
     * Events).
     * <p>
     * The method throws a {@link ModelException} if no Element can be resolved
     * based on the given model information.
     * <p>
     * The method is called by the {@link WorkflowKernel} during the processing live
     * cycle. The ModelManager is responsible to resolve conditional sequence flows.
     * 
     * @param event    - current event
     * @param workitem - current Workitem
     * @return a BPMN Element entity - {@link ItemCollection}
     * @throws ModelException - if no valid element was found
     */
    public ItemCollection nextModelElement(ItemCollection event, ItemCollection workitem) throws ModelException;

}
