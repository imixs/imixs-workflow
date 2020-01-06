/*******************************************************************************
 * <pre>
 *  Imixs Workflow 
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
 *      Imixs Software Solutions GmbH - initial API and implementation
 *      Ralph Soika - Software Developer
 * </pre>
 *******************************************************************************/

package org.imixs.workflow.exceptions;

/**
 * WorkflowException is the abstract super class for all Imixs Workflow Exception classes. A
 * WorkflowException signals an error in the business logic. WorkflowExceptions need to be caught.
 * 
 * @author rsoika
 */
public abstract class WorkflowException extends Exception {

  private static final long serialVersionUID = 1L;

  protected String errorContext = "UNDEFINED";
  protected String errorCode = "UNDEFINED";

  public WorkflowException(String aErrorCode, String message) {
    super(message);
    errorCode = aErrorCode;

  }

  public WorkflowException(String aErrorContext, String aErrorCode, String message) {
    super(message);
    errorContext = aErrorContext;
    errorCode = aErrorCode;

  }

  public WorkflowException(String aErrorContext, String aErrorCode, String message, Exception e) {
    super(message, e);
    errorContext = aErrorContext;
    errorCode = aErrorCode;

  }


  public WorkflowException(String aErrorCode, String message, Exception e) {
    super(message, e);

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
