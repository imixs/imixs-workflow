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
 * The interface ModelManager manages instances of a Model. A Model instance is
 * uniquely identified by the ModelVersion. The ModelManager is used by the
 * {@link WorkflowKernel} to manage the processing live cycle of a workitem.
 * <p>
 * By analyzing the workitem model version the WorkflowKernel determines the
 * corresponding model and get the Tasks and Events from the ModelManager to
 * process the workitem and assign the workitem to the next Task defined by the
 * BPMN Model.
 * 
 * @author rsoika
 */
public interface ModelManager {

    public final static String TASK_ELEMENT = "TASK";
    public final static String EVENT_ELEMENT = "EVENT";

    /**
     * Adds a new Model to the ModelManager.
     * 
     * @param model
     * @throws ModelException
     */
    public void addModel(BPMNModel model) throws ModelException;;

    /**
     * Returns a Model by version. The method throws a ModelException in case the
     * model version did not exits.
     * 
     * @param version
     * @throws ModelException
     * @return Model
     */
    public BPMNModel getModel(String version) throws ModelException;;

    /**
     * Removes a BPMNModel from the ModelManager
     * 
     * @param version
     */
    public void removeModel(String version);

    /**
     * Returns a BPMNModel instance matching the $modelVersion of a given workitem.
     * The method throws a ModelException in case the model version did not exits.
     * <p>
     * A ModelManager may resolve a model also by regular expressions.
     * 
     * @param version
     * @throws ModelException
     * @return BPMNModel
     */
    public BPMNModel findModelByWorkitem(ItemCollection workitem) throws ModelException;

    /**
     * Returns the BPMN Task entity associated with a given workitem, based on its
     * attributes "$modelVersion" and "$taskID".
     * <p>
     * The method throws a {@link ModelException} if no Task can be resolved based
     * on the given model information.
     * <p>
     * The method is called by the {@link WorkflowKernel} during the the processing
     * live cycle.
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
     * live cycle.
     * 
     * @param workitem
     * @return BPMN Event entity - {@link ItemCollection}
     * @throws ModelException if no event was found
     */
    public ItemCollection loadEvent(ItemCollection workitem) throws ModelException;

    /**
     * Finds the next BPMN Element associated with a given workitem, based on its
     * attributes "$modelVersion", "$taskID" and "$eventID". The returned BPMN
     * Element must either be an Activity (Task) element, an Intermediate Catch
     * Event or a End Event. The method must not return any other BPMN elements
     * (e.g. Gateways, Intermediate Throw Events).
     * <p>
     * The method throws a {@link ModelException} if no Element can be resolved
     * based on the given model information.
     * <p>
     * The method is called by the {@link WorkflowKernel} during the processing live
     * cycle. The ModelManager is responsible to resolve conditional sequence flows.
     * 
     * @param workitem - current Workitem
     * @return a BPMN Element entity - {@link ItemCollection}
     * @throws ModelException - if no valid element was found
     */
    public ItemCollection nextModelElement(ItemCollection workitem) throws ModelException;

}
