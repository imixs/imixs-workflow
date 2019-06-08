package org.imixs.workflow.engine;
/*******************************************************************************
 *  Imixs Workflow Technology
 *  Copyright (C) 2003, 2008 Imixs Software Solutions GmbH,  
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
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika
 *  
 *******************************************************************************/

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

import org.imixs.workflow.engine.scheduler.SchedulerController;

/**
 * The DatevController is used to configure the DatevScheduler. This service is
 * used to generate datev export workitems.
 * <p>
 * The Controller creates a configuration entity "type=configuration;
 * txtname=datev".
 * <p>
 * The following config items are defined:
 * 
 * The following config items are defined:
 * 
 * <pre>
 * _model_version = model version for the SEPA export
 * _initial_task = inital task ID
 * </pre>
 * 
 * 
 * @author rsoika
 * 
 */
@Named
@RequestScoped
public class WorkflowSchedulerController extends SchedulerController {

	private static final long serialVersionUID = 1L;

	@Override
	public String getName() {
		return WorkflowScheduler.NAME;
	}

	/**
	 * Returns the sepa scheduler class name. This name depends on the _export_type.
	 * 
	 * There are two export interfaces available - csv and XML
	 * 
	 */
	@Override
	public String getSchedulerClass() {
		return WorkflowScheduler.class.getName();
	}

}
