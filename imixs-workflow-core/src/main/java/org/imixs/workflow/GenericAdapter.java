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
 * A GenericAdapter extends the Adapter Interface. This Adapter is independent
 * from the BPMN Model and should not be associated with a BPMN Signal Event. A
 * GenericAdapter is called by the WorkfklowKernel during the processing
 * life-cycle before the plugin life-cycle.
 * <p>
 * A GenericAdapter can be a CDI implementation.
 * <p>
 * GenericAdapter are called before any Signal-Adapter or plugin was executed.
 * <p>
 * 
 * @author Ralph Soika
 * @version 1.0
 * @see org.imixs.workflow.WorkflowKernel
 */

public interface GenericAdapter extends Adapter {

}
