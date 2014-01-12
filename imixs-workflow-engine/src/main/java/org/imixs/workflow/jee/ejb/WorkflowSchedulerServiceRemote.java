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

import java.text.ParseException;

import javax.ejb.Remote;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;

/**
 * The WorkflowScheduler remote Interface This Interface represents a schedueld
 * Workflow Service. It is used by the WorkflowSchedulerBean which implements a
 * timer service to schedule workflow processing
 * 
 * 
 * @author rsoika
 * 
 */
@Remote
public interface WorkflowSchedulerServiceRemote {

	/**
	 * This method loads the current scheduler configuration. If no
	 * configuration entity yet exists the method returns an empty
	 * ItemCollection. The method updates the timer details netxtTimeout and
	 * timeRemaining of a running timer service.
	 * 
	 * @return configuration ItemCollection
	 */
	public ItemCollection loadConfiguration();

	/**
	 * This method saves the timer configuration. The method updates the timer
	 * details netxtTimeout and timeRemaining of a running timer service.
	 * 
	 * @param configuration
	 * @return
	 */
	public ItemCollection saveConfiguration(ItemCollection configItemCollection)
			throws AccessDeniedException;

	/**
	 * This Method starts the TimerService.
	 * 
	 * The Timer can be started based on a Calendar setting stored in the
	 * property txtConfiguration, or by interval based on the properties
	 * datStart, datStop, numIntervall.
	 **/
	public ItemCollection start() throws AccessDeniedException, ParseException;

	/**
	 * Stops a running timer instance. After the timer was canceled the
	 * configuration will be updated.
	 */
	public ItemCollection stop() throws AccessDeniedException;

	/**
	 * Returns true if the workflowSchedulerService was started
	 */
	public boolean isRunning();

}
