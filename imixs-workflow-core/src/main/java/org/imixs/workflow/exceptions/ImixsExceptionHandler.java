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

import java.util.Objects;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.WorkflowException;

/**
 * The ExceptionHandler provides a method to add a error message to the given workItem, based on the
 * data in a WorkflowException or InvalidAccessException. This kind of error message can be
 * displayed in a page evaluating the properties '$error_code' and '$error_message'. These
 * attributes will not be stored.
 * <p>
 * If a PluginException or ValidationException contains an optional object array the message is
 * parsed for params to be replaced
 * <p>
 * Example:
 * 
 * <code>
 * $error_message=Value should not be greater than {0} or lower as {1}.
 * </code>
 * 
 * @author rsoika
 *
 */
public class ImixsExceptionHandler {

  /**
   * This method adds a error message to the given workItem, based on the data in a
   * WorkflowException or InvalidAccessException.
   * 
   * @param pe
   */
  public static ItemCollection addErrorMessage(Exception pe, ItemCollection aworkitem) {

    Throwable rootCause = findCauseUsingPlainJava(pe);

    if (pe instanceof WorkflowException) {
      // String message = ((WorkflowException) pe).getErrorCode();
      String message = pe.getMessage();

      // parse message for params
      if (pe instanceof PluginException) {
        PluginException p = (PluginException) pe;
        if (p.getErrorParameters() != null && p.getErrorParameters().length > 0) {
          for (int i = 0; i < p.getErrorParameters().length; i++) {
            message = message.replace("{" + i + "}", p.getErrorParameters()[i].toString());
          }
        }
      }
      aworkitem.replaceItemValue("$error_code", ((WorkflowException) pe).getErrorCode());
      aworkitem.replaceItemValue("$error_message", message);
    } else if (rootCause instanceof InvalidAccessException) {
      aworkitem.replaceItemValue("$error_code",
          ((InvalidAccessException) rootCause).getErrorCode());
      aworkitem.replaceItemValue("$error_message", rootCause.getMessage());
    } else {
      aworkitem.replaceItemValue("$error_code", "INTERNAL ERROR");
      aworkitem.replaceItemValue("$error_message", pe.getMessage());
    }

    return aworkitem;
  }

  /**
   * Find the Root Cause Using Plain Java
   * <p>
   * We'll loop through all the causes until it reaches the root. Notice that we've added an extra
   * condition in our loop to avoid infinite loops when handling recursive causes.
   * <p>
   * 
   * @see https://www.baeldung.com/java-exception-root-cause
   * @param throwable
   * @return
   */
  public static Throwable findCauseUsingPlainJava(Throwable throwable) {
    Objects.requireNonNull(throwable);
    Throwable rootCause = throwable;
    while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
      rootCause = rootCause.getCause();
    }
    return rootCause;
  }
}
