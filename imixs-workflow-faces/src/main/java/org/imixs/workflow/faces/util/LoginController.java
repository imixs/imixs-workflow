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

package org.imixs.workflow.faces.util;

import java.util.List;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.imixs.workflow.engine.DocumentService;

/**
 * This Backing Bean acts as a Login Helper Class. Can be used to identify the
 * login state
 * 
 * @author rsoika
 * 
 */
@Named
@RequestScoped
public class LoginController {

	@Inject
	private DocumentService documentService;

	/**
	 * returns true if user is authenticated and has at least on of the Imixs
	 * Access Roles
	 * 
	 * @return
	 */
	public boolean isAuthenticated() {
		if (getUserPrincipal() == null)
			return false;
		else {
			ExternalContext ectx = FacesContext.getCurrentInstance().getExternalContext();
			// test if at least one of the Imixs Acces Roles are granted
			if (ectx.isUserInRole("org.imixs.ACCESSLEVEL.AUTHORACCESS"))
				return true;
			if (ectx.isUserInRole("org.imixs.ACCESSLEVEL.EDITORACCESS"))
				return true;
			if (ectx.isUserInRole("org.imixs.ACCESSLEVEL.MANAGERACCESS"))
				return true;
			if (ectx.isUserInRole("org.imixs.ACCESSLEVEL.READERACCESS"))
				return true;
		}
		return false;
	}

	/**
	 * Test security context isUserInRole
	 * 
	 * @param aRoleName
	 * @return
	 */
	public boolean isUserInRole(String aRoleName) {
		ExternalContext ectx = FacesContext.getCurrentInstance().getExternalContext();

		if (aRoleName == null || aRoleName.isEmpty())
			return false;

		return ectx.isUserInRole(aRoleName);
	}

	/**
	 * returns the userPrincipal Name
	 * 
	 * @return
	 */
	public String getUserPrincipal() {
		FacesContext context = FacesContext.getCurrentInstance();
		ExternalContext externalContext = context.getExternalContext();
		return externalContext.getUserPrincipal() != null ? externalContext.getUserPrincipal().toString() : null;
	}

	/**
	 * returns the remote user Name
	 * 
	 * @return
	 */
	public String getRemoteUser() {
		FacesContext context = FacesContext.getCurrentInstance();
		ExternalContext externalContext = context.getExternalContext();
		String remoteUser = externalContext.getRemoteUser();
		return remoteUser;
	}

	/**
	 * Returns the current user name list including userId, roles and context
	 * groups.
	 * 
	 * @return
	 */
	public List<String> getUserNameList() {
		return documentService.getUserNameList();
	}

	/**
	 * returns the full qualified server URI from the current web context
	 * 
	 * @return
	 */
	public String getServerURI() {
		HttpServletRequest servletRequest = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext()
				.getRequest();

		String port = "" + servletRequest.getLocalPort();

		String server = servletRequest.getServerName();
		return "http://" + server + ":" + port + "";

	}

	/**
	 * invalidates the current user session
	 * 
	 * @param event
	 */
	public void doLogout(ActionEvent event) {
		FacesContext context = FacesContext.getCurrentInstance();
		ExternalContext externalContext = context.getExternalContext();

		HttpSession session = (HttpSession) externalContext.getSession(false);

		session.invalidate();

	}

}
