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

package org.imixs.workflow.engine.scheduler;

import org.imixs.workflow.exceptions.InvalidAccessException;

/**
 * The SchedulerException is thrown from the generic scheduler service
 * 
 * @see  org.imixs.workflow.engine.scheduler.GenericScheduelrService
 * @author rsoika
 * 
 */
public class SchedulerException extends InvalidAccessException {

	private static final long serialVersionUID = 1L;

	public static final String INVALID_MODELVERSION = "INVALID_MODELVERSION";
	public static final String INVALID_WORKITEM = "INVALID_WORKITEM";
	public static final String INVALID_PROCESSID = "INVALID_PROCESSID";

	public SchedulerException(String aErrorCode, String message) {
		super(aErrorCode, message);
	}
	
	public SchedulerException(String aErrorContext, String aErrorCode,
			String message) {
		super(message);
		errorContext = aErrorContext;
		errorCode = aErrorCode;

	}

	public SchedulerException(String aErrorContext, String aErrorCode,
			String message, Exception e) {
		super(message, e);
		errorContext = aErrorContext;
		errorCode = aErrorCode;

	}
}
