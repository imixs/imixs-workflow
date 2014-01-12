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


/**
 * A ExtendedModel extends the Model Interface and supports a multi-model
 * concept
 * 
 * @author Ralph Soika
 * @version 1.0
 * @see org.imixs.workflow.Model
 */
public interface ExtendedModel extends Model {

	/**
	 * Finds and returns the ProcessEntity for a processid inside the Model.
	 * 
	 * @param processid
	 * @return ItemCollection
	 * @throws Exception
	 */
	public ItemCollection getProcessEntityByVersion(int processid,
			String modelVersion);

	/**
	 * Finds and returns the ActivityEntity for a processid and a activityid
	 * inside the Model.
	 * 
	 * @param processid
	 * @param activityid
	 * @return ItemCollection
	 */
	public ItemCollection getActivityEntityByVersion(int processid,
			int activityid, String modelVersion);

	/**
	 * retruns a collection of ProcessEntities
	 * 
	 * @return Collection org.imixs.workflow.ItemCollection
	 */
	public java.util.Collection<ItemCollection> getProcessEntityListByVersion(
			String modelVersion);

	/**
	 * retruns a collection of ActivityEntities for a specivic ProcessID. If the
	 * process ID did not exists an empty collection should be returned
	 * 
	 * @return Collection org.imixs.workflow.ItemCollection
	 */
	public java.util.Collection<ItemCollection> getActivityEntityListByVersion(
			int processid, String modelVersion);

}
