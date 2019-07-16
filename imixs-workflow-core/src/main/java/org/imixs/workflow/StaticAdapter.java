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
 * A StaticAdapter extends the Adapter Interface. A StaticAdapter is called by
 * the WorkfklowKernel during the processing life-cycle. A StaticAdapter can be
 * a CDI implementation.
 * <p>
 * StaticAdapters are called before any Signal-Adapter or plugin was executed.
 * <p>
 * A StaticAdapter is independent from the BPMN Model and should not be
 * associated with a BPMN Signal Event. A StaticAdapter provides static code to
 * be executed before the plugin life cylce.
 * 
 * @author Ralph Soika
 * @version 1.0
 * @see org.imixs.workflow.engine.WorkflowKernel
 */

public interface StaticAdapter extends Adapter {

}
