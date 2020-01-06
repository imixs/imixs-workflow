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

import java.util.List;
import org.imixs.workflow.exceptions.ModelException;

/**
 * The IModel interface defines getter methods to navigate through a Imixs
 * Workflow Model. The IModel interface is used by the
 * <code>IModelManager</code>.
 * 
 * A Imixs-Workflow Model is defined by a collections of Tasks and Events. A
 * Task defines the state of a process instance. The Event defines the
 * transition from one state to another. A Task contains informations about the
 * processing state e.g. the name or the status description. A Task is uniquely
 * identified by its ID. A Event is unambiguously assigned to a Task and
 * uniquely identified by by an ID.
 * 
 * Task and Event elements are implemented as instances of the class
 * ItemCollection.
 * 
 * A Model holds a Definition which contains general model information.
 * 
 * @author Ralph Soika
 * @version 1.0
 * @see org.imixs.workflow.IModelwManager
 * @see org.imixs.workflow.ItemCollection
 */
public interface Model {

    /**
     * Returns the model version.
     * 
     * @return
     */
    public String getVersion();

    /**
     * Returns the model definition containing general model information (e.g.
     * $ModelVersion).
     * 
     * @return
     */
    public ItemCollection getDefinition();

    /**
     * Returns a Task by its Id.
     * 
     * @param taskid
     * @param modelVersion
     * @return ItemCollection
     */
    public ItemCollection getTask(int taskID) throws ModelException;

    /**
     * Returns a Event by its Id and Task-ID.
     * 
     * @param taskid
     * @param eventid
     * @param modelVersion
     * @return ItemCollection
     */
    public ItemCollection getEvent(int taskID, int eventID) throws ModelException;

    /**
     * Returns all Group definitions.
     * 
     * @return
     */
    public List<String> getGroups();

    /**
     * Returns all Tasks defined in the model.
     * 
     * @param modelVersion
     * @return List org.imixs.workflow.ItemCollection
     */
    public List<ItemCollection> findAllTasks();

    /**
     * Returns all Events assigned to a task.
     * 
     * @param taskid
     * @return Collection org.imixs.workflow.ItemCollection
     */
    public List<ItemCollection> findAllEventsByTask(int taskID);

    /**
     * Returns a list of Tasks assigned to a specific workflow group.
     * 
     * @param group
     * @return
     */
    public List<ItemCollection> findTasksByGroup(String group);

}
