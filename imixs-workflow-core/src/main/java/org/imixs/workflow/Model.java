/*******************************************************************************
 *  Imixs Workflow 
 *  Copyright (C) 2001, 2011 Imixs Software Solutions GmbH,  
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
 *  	http://www.imixs.org
 *  	http://java.net/projects/imixs-workflow
 *  
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika - Software Developer
 *******************************************************************************/

package org.imixs.workflow;

import java.util.List;

/**
 * A Model defines the instructions for processing a workitem. The Model is used
 * by the <code>WorkflowKernel</code> to manage the process flow of a workitem
 * in the workflow.
 * 
 * A Model is defined by collections of ProcessEntities which defines a state
 * for a worktiem inside the model and ActivityEntities which defines the
 * transition from one state to another.
 * 
 * The Model interface defines finder functions to get processEntities and
 * activityEntities. A processEntity contains informations about the
 * Processinstance inside the workflowmodel e.g. the name or the
 * statusdescription. On the other point of view a activityentity contains
 * informations about the processcontroling and processhandling. A ProcessEntity
 * is defined by its processid. A ActivityEntity defined by its activityid and
 * unambiguously assigned to a ProcessEntity by its processid. The
 * Workflowkernel determines by analyzing the workitem the appendant
 * activityentity and transfers it along with the workitem to the registerd
 * plugin moduls. After processing the workitem the workflowkernel is able to
 * analyze the new status of the workitem by the informations stored inside the
 * appendant processentity. With a Model the Workflowkernel is able to controle
 * the expiration of a workflowprocess. A Model is always instantiated by a
 * WorkflowManager and transferd as a Interface to the Workflowkernel.
 * 
 * @author Ralph Soika
 * @version 1.0
 * @see org.imixs.workflow.WorkflowManager
 * @see org.imixs.workflow.Plugin
 */
public interface Model {

	/**
	 * Returns the ProcessEntity by its processId for a specific model version.
	 * 
	 * @param processid
	 * @param modelVersion
	 * @return ItemCollection
	 */
	public ItemCollection getProcessEntity(int processid, String modelVersion);

	/**
	 * Returns the ActivityEntity defined by its processId and activityID for a
	 * specific model version.
	 * 
	 * @param processid
	 * @param activityid
	 * @param modelVersion
	 * @return ItemCollection
	 */
	public ItemCollection getActivityEntity(int processid, int activityid,
			String modelVersion);

	/**
	 * Returns all ProcessEntities defined in a specific model version.
	 * 
	 * @param modelVersion
	 * @return Collection org.imixs.workflow.ItemCollection
	 */
	public List<ItemCollection> getProcessEntityList(String modelVersion);

	/**
	 * Returns all ActivityEntities defined by its processId for a specific
	 * model version. If the process ID did not exists an empty list should be
	 * returned.
	 * 
	 * @param processid
	 * @param modelVersion
	 * @return Collection org.imixs.workflow.ItemCollection
	 */
	public List<ItemCollection> getActivityEntityList(int processid,
			String modelVersion);

}
