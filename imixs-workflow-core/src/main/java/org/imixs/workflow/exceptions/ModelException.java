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
 * An ModelException should be thrown by a service component if a model entity
 * is invalid or does not exist
 * 
 * @author rsoika
 * 
 */
public class ModelException extends WorkflowException {

    public static final String INVALID_MODEL = "INVALID_MODEL";
    public static final String INVALID_MODEL_ENTRY = "INVALID_MODEL_ENTRY";
    public static final String UNDEFINED_MODEL_ENTRY = "UNDEFINED_MODEL_ENTRY";
    public static final String UNDEFINED_MODEL_VERSION = "UNDEFINED_MODEL_VERSION";

    private static final long serialVersionUID = 1L;

    public ModelException(String aErrorCode, String message) {
        super(aErrorCode, message);
    }

    public ModelException(String aErrorCode, String message, Exception e) {
        super(aErrorCode, message, e);
    }

}
