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

package org.imixs.workflow.plugins.jee;

import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.jee.util.PropertyService;

/**
 * This abstract class implements a basic set of functions for implementing
 * plugins with Interfaces to JEE API
 * 
 * The Plugin overwrites the replaceDynamicValue method and looks for xml tags
 * '<propertyvalue>' This feature is optinal and only avilable if the
 * PropertyService EJB can be lookuped!
 * 
 * 
 * @author Ralph Soika
 * 
 */
public abstract class AbstractPlugin extends
		org.imixs.workflow.plugins.AbstractPlugin {
	javax.ejb.SessionContext ejbSessionContext;

	public static final String INVALID_PROPERTYVALUE_FORMAT = "INVALID_PROPERTYVALUE_FORMAT";
	
	PropertyService propertyService = null;
	private static Logger logger = Logger.getLogger(AbstractPlugin.class
			.getName());

	/**
	 * Initialize Plugin and get an instance of the EJB Session Context
	 */
	public void init(WorkflowContext actx) throws PluginException {
		super.init(actx);
		// cast Workflow Session Context to EJB Session Context
		ejbSessionContext = (javax.ejb.SessionContext) ctx.getSessionContext();

		// try to lookup the propertyService for the method replaceDynamicValues
		String jndiName = "ejb/PropertyService";
		InitialContext ictx;
		try {
			ictx = new InitialContext();
			Context ctx = (Context) ictx.lookup("java:comp/env");
			propertyService = (PropertyService) ctx.lookup(jndiName);
		} catch (NamingException e) {
			// if we can not lookup the propertyService EJB we disable this
			// feature
			logger.fine("[AbstractPlugin] PropertyService not bound!");
			propertyService = null;
		}

	}

	public abstract int run(ItemCollection documentContext,
			ItemCollection documentActivity) throws PluginException;

	public abstract void close(int status) throws PluginException;

	/**
	 * Returns an instance of the imixs propertyService EJB. Returns null if not
	 * bound to the current workflow context
	 * 
	 * @return the propertyService EJB (optional)
	 */
	public PropertyService getPropertyService() {
		return propertyService;
	}

	public void setPropertyService(PropertyService propertyService) {
		this.propertyService = propertyService;
	}

	/**
	 * determines the current username (callerPrincipal)
	 * 
	 * @return
	 */
	public String getUserName() {
		return ejbSessionContext.getCallerPrincipal().getName();
	}

	/**
	 * Returns the EJB Session Context
	 * 
	 * @return SessionContext
	 */
	public javax.ejb.SessionContext getEjbSessionContext() {
		return ejbSessionContext;
	}

	public void setEjbSessionContext(javax.ejb.SessionContext ejbSessionContext) {
		this.ejbSessionContext = ejbSessionContext;
	}

	/**
	 * this method overrides the default behavior and parses a string for xml
	 * tag <propertyvalue>. Those tags will be replaced with the corresponding
	 * property value from the imixs.properties file.
	 * 
	 * <code>
	 *   hello <propertyvalue>myCustomKey</propertyvalue>
	 * </code>
	 * @throws PluginException 
	 * 
	 * 
	 */
	@Override
	public String replaceDynamicValues(String aString,
			ItemCollection documentContext) throws PluginException {

		int iTagStartPos;
		int iTagEndPos;
		int iContentStartPos;
		int iContentEndPos;
		String sPropertyKey;

		if (aString == null)
			return "";

		if (aString.toLowerCase().contains("<propertyvalue")
				&& propertyService != null) {

			// test if a <value> tag exists...
			while ((iTagStartPos = aString.toLowerCase().indexOf(
					"<propertyvalue")) != -1) {

				iTagEndPos = aString.toLowerCase().indexOf("</propertyvalue>",
						iTagStartPos);

				// if no end tag found return string unchanged...
				if (iTagEndPos == -1) {
					throw new PluginException(
							this.getClass().getSimpleName(),
							INVALID_PROPERTYVALUE_FORMAT,
							"[AbstractPlugin] invalid propertyvalue format: "+aString);
				}

				// reset pos vars
				iContentStartPos = 0;
				iContentEndPos = 0;
				sPropertyKey = "";

				// so we now search the beginning of the tag content
				iContentEndPos = iTagEndPos;
				// start pos is the last > before the iContentEndPos
				String sTestString = aString.substring(0, iContentEndPos);
				iContentStartPos = sTestString.lastIndexOf('>') + 1;

				// if no end tag found return string unchanged...
				if (iContentStartPos >= iContentEndPos) {
					logger.warning("[AbstractPlugin] invalid text string format: "
							+ aString);
					break;
				}

				iTagEndPos = iTagEndPos + "</propertyvalue>".length();

				// now we have the start and end position of a tag and also the
				// start and end pos of the value

				// read the property Value
				sPropertyKey = aString.substring(iContentStartPos,
						iContentEndPos);

				String vValue = propertyService.getProperties().getProperty(
						sPropertyKey);
				if (vValue == null) {
					logger.warning("[AbstractPlugin] propertyvalue '"
							+ sPropertyKey
							+ "' is not defined in imixs.properties!");
					vValue = "";
				}
				// now replace the tag with the result string
				aString = aString.substring(0, iTagStartPos) + vValue
						+ aString.substring(iTagEndPos);

			}
		}

		// call default behavior
		return super.replaceDynamicValues(aString, documentContext);
	}

}
