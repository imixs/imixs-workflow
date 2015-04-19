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

package org.imixs.workflow.jee.ejb;

import java.io.InputStream;
import java.util.List;

import javax.ejb.Remote;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;

/**
 * The Remote interface for the ModelService EJB. Can be used by standalone java
 * clients.
 * 
 * @see org.imixs.workflow.jee.ejb.ModelService
 * @author rsoika
 * 
 */
@Remote
public interface ModelServiceRemote {

	/**
	 * This method returns the ActivityEntity with the highest Version number
	 * ($modelversion)
	 */

	public abstract ItemCollection getActivityEntity(int processid,
			int activityid, String modelVersion);

	public abstract ItemCollection getProcessEntity(int processid,
			String modelversion);

	/**
	 * returns a collection of ItemCollections representing the model activity
	 * Entities for the coresponding processid
	 * 
	 * @throws ModelException
	 * 
	 */

	public abstract List<ItemCollection> getActivityEntityList(int processid,
			String aModelVersion);

	/**
	 * returns a collection of ItemCollections representing the model process
	 * Entities
	 * 
	 */
	public abstract List<ItemCollection> getProcessEntityList(
			String aModelVersion);

	/**
	 * Saves or updates an ActivityEntity represented by an ItemCollection. The
	 * Entity is unique identified of its Attributes 'numProcessID',
	 * 'numActivityID' and '$modelversion' The Method verifies that an existing
	 * instance will be updated.
	 * 
	 * @param ic
	 * 
	 * @throws AccessDeniedException
	 * @throws Exception
	 */
	public abstract void saveActivityEntity(ItemCollection ic)
			throws ModelException, AccessDeniedException;

	/**
	 * Saves or updates a ProcessEntitiy represented by an ItemCollection. The
	 * Entity is unique identified of its Attributes 'numProcessID' and
	 * '$modelversion' The Method verifies that an existing instance will be
	 * updated.
	 * 
	 * @param ic
	 * 
	 * @throws AccessDeniedException
	 * @throws ModelException
	 */
	public abstract void saveProcessEntity(ItemCollection ic)
			throws ModelException, AccessDeniedException;

	/**
	 * Saves or updates an EnvironmentEntity represented by an ItemCollection.
	 * The Entity is unique identified of its Attributes 'txtName' and
	 * '$modelversion' The Method verifies that an existing instance will be
	 * updated.
	 * 
	 * @param ic
	 * @throws AccessDeniedException
	 * @throws Exception
	 */
	public abstract void saveEnvironmentEntity(ItemCollection ic)
			throws ModelException, AccessDeniedException;

	/**
	 * This method removes a specific ModelVersion. If modelVersion is null the
	 * method will remove all models
	 * 
	 * @throws AccessDeniedException
	 */
	public abstract void removeModel(String modelversion)
			throws ModelException, AccessDeniedException;

	/**
	 * This method removes a specific WorkflowGroup for the defined
	 * modelVersion. If modelVersion is null the method will remove all models
	 * 
	 * @throws AccessDeniedException
	 */
	public abstract void removeModelGroup(String workflowgroup,
			String modelversion) throws ModelException, AccessDeniedException;

	/**
	 * This helper method finds the highest Model Version available in the
	 * system. Returns an empty String if no version was found!
	 * 
	 * @return String with the latest model version
	 */
	public abstract String getLatestVersion() throws ModelException;

	/**
	 * returns a String list of all model profile entities
	 * 
	 * @return
	 */
	public abstract List<ItemCollection> getAllModelProfiles();
	
	/**
	 * returns a String list of all model versions
	 * 
	 * @return
	 */
	public abstract List<String> getAllModelVersions();

	/**
	 * returns all the activities in a list for a corresponding process entity
	 * The method returns only Activities where keypublicresult != "0"
	 * 
	 * @return List<ItemCollection> of activity Entities
	 */
	public abstract List<ItemCollection> getPublicActivities(int aprocessid,
			String version);

	/**
	 * returns a String list of all existing ProcessGroup Names
	 * 
	 * @return
	 */
	public abstract List<String> getAllWorkflowGroups(String version);

	/**
	 * returns a list of all ProcessEntities which are the first one in each
	 * ProcessGroup. The ModelVersion specifies the Model to be analiezed.
	 * 
	 * So for each ProcessGroup the ProcessEntity with the lowest processID will
	 * be returned. The method builds a cash with the best (lowest) ProcessID
	 * for each process group.
	 * 
	 * The collection returned will be sorted by the numProcessID
	 * 
	 * @return
	 */
	public abstract List<ItemCollection> getAllStartProcessEntities(
			String version);

	/**
	 * returns a list of all ProcessEntities for a specific ProcessGroup and
	 * modelversion.
	 * 
	 * @param aGroup
	 * @param aversion
	 * @return
	 */
	public abstract List<ItemCollection> getAllProcessEntitiesByGroup(
			String aGroup, String aversion);

	/**
	 * This method imports a workflow model file
	 */
	public abstract void importModel(InputStream input) throws ModelException,
			AccessDeniedException;
}
