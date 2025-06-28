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

import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.imixs.workflow.engine.DocumentService;

import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ActionEvent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

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
     * returns true if user is authenticated and has at least on of the Imixs Access
     * Roles
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
        return externalContext.getUserPrincipal() != null ? externalContext.getUserPrincipal().getName() : null;
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
