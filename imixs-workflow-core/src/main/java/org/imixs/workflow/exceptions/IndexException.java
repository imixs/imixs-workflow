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
 * An IndexException is a runtime exception which is thrown by a Imixs Workflow component if a index
 * is not read or writable. .
 * 
 * @see org.imixs.workflow.engine.lucene.LuceneUpdateService
 * @author rsoika
 * 
 */
public class IndexException extends RuntimeException {

  public static final String INVALID_INDEX = "INVALID_INDEX";

  protected String errorCode = "UNDEFINED";
  protected String errorContext = "UNDEFINED";

  private static final long serialVersionUID = 1L;

  public IndexException(String message) {
    super(message);
  }

  public IndexException(String message, Exception e) {
    super(message, e);
  }

  public IndexException(String aErrorCode, String message) {
    super(message);
    errorCode = aErrorCode;
  }

  public IndexException(String aErrorCode, String message, Exception e) {
    super(message, e);
    errorCode = aErrorCode;
  }

  public IndexException(String aErrorContext, String aErrorCode, String message) {
    super(message);
    errorContext = aErrorContext;
    errorCode = aErrorCode;

  }

  public IndexException(String aErrorContext, String aErrorCode, String message, Exception e) {
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
