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

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.exceptions.PluginException;

/**
 * This abstract class implements a basic set of functions for implementing
 * plugins with Interfaces to JEE API
 * 
 * @author Ralph Soika
 * 
 */
public abstract class AbstractPlugin extends org.imixs.workflow.plugins.AbstractPlugin {
	javax.ejb.SessionContext jeeSessionContext;

	/**
	 * Initialize Plugin and get an instance of the EJB Session Context
	 */
	public void init(WorkflowContext actx) throws PluginException {
		super.init(actx);
		// cast Workflow Session Context to EJB Session Context
		jeeSessionContext = (javax.ejb.SessionContext) ctx.getSessionContext();
	}

	
	public abstract int run(ItemCollection documentContext,
			ItemCollection documentActivity) throws PluginException;

	
	public abstract void close(int status) throws PluginException;

	
	
	/**
	 * determines the current username (callerPrincipal)
	 * @return
	 */
	public String getUserName() {
		return jeeSessionContext.getCallerPrincipal().getName();
	}
	
	
}
