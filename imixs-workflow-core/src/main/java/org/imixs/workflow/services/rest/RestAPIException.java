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

package org.imixs.workflow.services.rest;

/**
 * RestAPIException signals an error in the communication with the Imixs Rest
 * API using the Imixs RestClient. The error code is the HTTP responce code. 
 * 
 * @see RestClient
 * @author rsoika
 * @version 2.0
 */
public class RestAPIException extends Exception {

	private static final long serialVersionUID = 1L;

	protected String errorContext = "UNDEFINED";
	protected int errorCode = 0;

	public RestAPIException(int aErrorCode, String message) {
		super(message);
		errorCode = aErrorCode;

	}

	public RestAPIException(String aErrorContext, int aErrorCode, String message) {
		super(message);
		errorContext = aErrorContext;
		errorCode = aErrorCode;

	}

	public RestAPIException(String aErrorContext, int aErrorCode, String message, Exception e) {
		super(message, e);
		errorContext = aErrorContext;
		errorCode = aErrorCode;

	}

	public RestAPIException(int aErrorCode, String message, Exception e) {
		super(message, e);

		errorCode = aErrorCode;

	}

	public String getErrorContext() {
		return errorContext;
	}

	public void setErrorContext(String errorContext) {
		this.errorContext = errorContext;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

}
