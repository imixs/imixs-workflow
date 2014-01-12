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
 * This Interface extends teh WorkflowContext to support a extended Workflow Model.
 * Extended Workflowmodels allow the managmenet of multi-model versions
 * @author Ralph Soka
 * @version 1.0 
 * @see    org.imixs.workflow.WorkflowContext 
 */


public interface ExtendedWorkflowContext extends WorkflowContext{
	
	
	/**
	 * This Methode returns a defined Model Implementation.
	 * 
	 * @return Model
	 */
	public ExtendedModel getExtendedModel();

	
}
