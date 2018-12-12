package org.imixs.workflow.faces.util;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.imixs.workflow.engine.plugins.RulePlugin;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.WorkflowException;

public class ErrorHandler {

	private static Logger logger = Logger.getLogger(ErrorHandler.class.getName());

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
				logger.warning("ErrorHandler: " + mre.getMessage());
			}
			// create a faces message for each parameter
			Object[] messages = pe.getErrorParameters();
			for (Object aMessage : messages) {

				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_INFO,errorCode, aMessage.toString()));
			} 
		} else {
			// default behavior
			addErrorMessage(pe);
		}

		logger.warning(
				"ErrorHandler cauth PluginException - error code=" + pe.getErrorCode() + " - " + pe.getMessage());
		if (logger.isLoggable(Level.FINE)) {

			pe.printStackTrace(); // Or use a logger.
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
			Locale browserLocale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
			ResourceBundle rb = ResourceBundle.getBundle("bundle.app", browserLocale);
			String messageFromBundle = rb.getString(pe.getErrorCode());
			if (messageFromBundle!=null && !messageFromBundle.isEmpty()) {
				message=messageFromBundle;
			}
		} catch (MissingResourceException mre) {
			logger.warning("ErrorHandler: " + mre.getMessage());
		}

		// parse message for params
		if (pe instanceof PluginException) {
			PluginException p = (PluginException) pe;
			if (p.getErrorParameters() != null && p.getErrorParameters().length > 0) {
				for (int i = 0; i < p.getErrorParameters().length; i++) {
					message = message.replace("{" + i + "}", p.getErrorParameters()[i].toString());
				}
			}
		} else {
			if (pe instanceof ValidationException) {
				ValidationException p = (ValidationException) pe;
				if (p.getErrorParameters() != null && p.getErrorParameters().length > 0) {
					for (int i = 0; i < p.getErrorParameters().length; i++) {
						message = message.replace("{" + i + "}", p.getErrorParameters()[i].toString());
					}
				}
			}
		}
		FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,errorCode, message));

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
			logger.warning("ErrorHandler: " + mre.getMessage());
		}

		FacesContext.getCurrentInstance().addMessage(null,
				new FacesMessage(FacesMessage.SEVERITY_INFO, message, me.getMessage()));

		logger.warning("ErrorHandler cauth ModelException - error code=" + me.getErrorCode() + " - " + me.getMessage());
		if (logger.isLoggable(Level.FINE)) {
			me.printStackTrace(); // Or use a logger.
		}
	}

}
