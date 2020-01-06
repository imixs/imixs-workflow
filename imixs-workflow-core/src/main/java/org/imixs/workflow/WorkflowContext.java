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

/**
 * This Interface defines the Context which is used to supply a basic enviroment
 * for the exchange between a WorkflowManager an the registered Plugin Moduls.
 * Normaly the WorkflowManager Implementation itself implents this Interface to
 * provide the Context for the Workflow components.
 * 
 * @author imixs.com
 * @version 1.0
 * @see org.imixs.workflow.WorkflowKernel
 */

public interface WorkflowContext {

    /**
     * This Methode returns the Runtime enviroment for a workflow Implementation. is
     * usesd to initialize the plugin.
     * 
     * @return a Session Object
     */
    public Object getSessionContext();

    /**
     * This method returns an instance of a IModelManager to access model
     * information
     * 
     * @return ModelManager
     */
    public ModelManager getModelManager();

}
