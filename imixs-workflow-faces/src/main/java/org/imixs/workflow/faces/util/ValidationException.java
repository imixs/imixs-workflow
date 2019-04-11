package org.imixs.workflow.faces.util;

import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.WorkflowException;

/**
 * A ValidationException should be thrown by a JSF managed bean or CDI bean
 * 
 * @see PluginException
 * @author rsoika
 */
public class ValidationException extends WorkflowException {
 
	private static final long serialVersionUID = 1L;
	private java.lang.Object[] params = null;

	public ValidationException(String aErrorContext, String aErrorCode,
			String message) {
		super(aErrorContext, aErrorCode, message);
	} 

	public ValidationException(String aErrorContext, String aErrorCode,
			String message, Exception e) {
		super(aErrorContext, aErrorCode, message, e);
	}

	public ValidationException(String aErrorContext, String aErrorCode,
			String message, java.lang.Object[] params) {
		super(aErrorContext, aErrorCode, message);
		this.params = params;
	}

	public java.lang.Object[] getErrorParameters() {
		return params;
	}

	protected void setErrorParameters(java.lang.Object[] aparams) {
		this.params = aparams;
	}

}
