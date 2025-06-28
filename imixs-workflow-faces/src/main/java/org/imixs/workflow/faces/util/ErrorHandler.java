/****************************************************************************
 * Copyright (c) 2022-2025 Imixs Software Solutions GmbH and others.
 * https://www.imixs.com
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * This Source Code may also be made available under the terms of the
 * GNU General Public License, version 2 or later (GPL-2.0-or-later),
 * which is available at https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-or-later
 ****************************************************************************/

package org.imixs.workflow.faces.util;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.engine.plugins.RulePlugin;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.WorkflowException;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;

public class ErrorHandler {

    private static final Logger logger = Logger.getLogger(ErrorHandler.class.getName());

    /**
     * The Method expects a PluginException and adds the corresponding Faces Error
     * Message into the FacesContext.
     * 
     * If the PluginException was thrown from the RulePLugin then the method test
     * this exception for ErrorParams and generate separate Faces Error Messages for
     * each param.
     */
    public static void handlePluginException(PluginException pe) {
        // if the PluginException was throws from the RulePlugin then test
        // for VALIDATION_ERROR and ErrorParams
        if (RulePlugin.class.getName().equals(pe.getErrorContext())
                && (RulePlugin.VALIDATION_ERROR.equals(pe.getErrorCode())) && pe.getErrorParameters() != null
                && pe.getErrorParameters().length > 0) {

            String errorCode = pe.getErrorCode();
            // try to find the message text in resource bundle...
            try {
                Locale browserLocale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
                ResourceBundle rb = ResourceBundle.getBundle("bundle.app", browserLocale);
                errorCode = rb.getString(pe.getErrorCode());
            } catch (MissingResourceException mre) {
                logger.log(Level.WARNING, "ErrorHandler: {0}", mre.getMessage());
            }
            // create a faces message for each parameter
            Object[] messages = pe.getErrorParameters();
            for (Object aMessage : messages) {

                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, errorCode, aMessage.toString()));
            }
        } else {
            // default behavior
            addErrorMessage(pe);
        }

        logger.log(Level.WARNING, "ErrorHandler cauth PluginException - error code={0} - {1}",
                new Object[]{pe.getErrorCode(), pe.getMessage()});
        if (logger.isLoggable(Level.FINE)) {

            pe.printStackTrace(); // Or use a logger.
        }
    }

    /**
     * The Method expects a ModelException and adds the corresponding Faces Error
     * Message into the FacesContext.
     * 
     * In case of a model exception, the exception message will become part of the
     * error message. ErrorParams are not supported by a ModelException.
     */
    public static void handleModelException(ModelException me) {

        // try to get the message code...
        String message = me.getErrorCode();
        // try to find the message text in resource bundle...
        try {
            Locale browserLocale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
            ResourceBundle rb = ResourceBundle.getBundle("bundle.app", browserLocale);
            message = rb.getString(me.getErrorCode());
        } catch (MissingResourceException mre) {
            logger.log(Level.WARNING, "ErrorHandler: {0}", mre.getMessage());
        }

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, message, me.getMessage()));

        logger.log(Level.WARNING, "ErrorHandler cauth ModelException - error code={0} - {1}",
                new Object[]{me.getErrorCode(), me.getMessage()});
        if (logger.isLoggable(Level.FINE)) {
            me.printStackTrace(); // Or use a logger.
        }
    }

    /**
     * This helper method adds a error message to the faces context, based on the
     * data in a WorkflowException. This kind of error message can be displayed in a
     * page using:
     * 
     * <code>
     *       <h:messages globalOnly="true" />
     * </code>
     * 
     * If a PluginException or ValidationException contains an optional object array
     * the message is parsed for params to be replaced
     * 
     * Example:
     * 
     * <code>
     * ERROR_MESSAGE=Value should not be greater than {0} or lower as {1}.
     * </code>
     * 
     * @param pe
     */
    public static void addErrorMessage(WorkflowException pe) {

        String errorCode = pe.getErrorCode();
        String message = pe.getMessage();
        // try to find the message text in resource bundle...
        try {
            String messageFromBundle = getMessageFromBundle(pe.getErrorCode());
            if (messageFromBundle != null && !messageFromBundle.isEmpty()) {
                message = messageFromBundle;
            }
        } catch (MissingResourceException mre) {
            logger.log(Level.WARNING, "ErrorHandler: {0}", mre.getMessage());
        }

        // parse message for params
        pe.formatErrorMessageWithParameters(message);
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, errorCode, message));

    }

    /**
     * Returns a message string from one of the following bundles:
     * 
     * app custom
     * 
     * @return
     */
    private static String getMessageFromBundle(String messageFromBundle) {
        String result = "";

        if (messageFromBundle != null && !messageFromBundle.isEmpty()) {
            Locale browserLocale = FacesContext.getCurrentInstance().getViewRoot().getLocale();

            try {

                ResourceBundle rb = ResourceBundle.getBundle("bundle.custom", browserLocale);
                if (rb != null) {
                    result = rb.getString(messageFromBundle);
                }

                if (result == null || result.isEmpty()) {
                    // try second bundle
                    rb = ResourceBundle.getBundle("bundle.app", browserLocale);
                    if (rb != null) {
                        result = rb.getString(messageFromBundle);
                    }
                }

            } catch (MissingResourceException mre) {
                logger.log(Level.WARNING, "ErrorHandler: {0}", mre.getMessage());
            }
        }

        return result;
    }

}
