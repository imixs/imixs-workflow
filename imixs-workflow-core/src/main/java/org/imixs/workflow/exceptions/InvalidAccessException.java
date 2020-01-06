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

package org.imixs.workflow.exceptions;

/**
 * An InvalidAccessException is a runtime exception which should be thrown by a
 * Imixs Workflow component if a method call is invalid or the data structure is
 * in an invalid state.
 * 
 * The property errorCode specifies the exception type. Extensions of this
 * Exception may add additional errorCodes.
 * 
 * @author rsoika
 * 
 */
public class InvalidAccessException extends RuntimeException {

    public static final String OPERATION_NOTALLOWED = "OPERATION_NOTALLOWED";
    public static final String INVALID_ID = "INVALID_ID";
    public static final String INVALID_INDEX = "INVALID_INDEX";

    protected String errorCode = "UNDEFINED";
    protected String errorContext = "UNDEFINED";

    private static final long serialVersionUID = 1L;

    public InvalidAccessException(String message) {
        super(message);
    }

    public InvalidAccessException(String message, Exception e) {
        super(message, e);
    }

    public InvalidAccessException(String aErrorCode, String message) {
        super(message);
        errorCode = aErrorCode;
    }

    public InvalidAccessException(String aErrorCode, String message, Exception e) {
        super(message, e);
        errorCode = aErrorCode;
    }

    public InvalidAccessException(String aErrorContext, String aErrorCode, String message) {
        super(message);
        errorContext = aErrorContext;
        errorCode = aErrorCode;

    }

    public InvalidAccessException(String aErrorContext, String aErrorCode, String message, Exception e) {
        super(message, e);
        errorContext = aErrorContext;
        errorCode = aErrorCode;

    }

    public String getErrorContext() {
        return errorContext;
    }

    public void setErrorContext(String errorContext) {
        this.errorContext = errorContext;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
}
