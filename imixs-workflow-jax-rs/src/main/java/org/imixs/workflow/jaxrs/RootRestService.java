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

package org.imixs.workflow.jaxrs;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.StreamingOutput;

import jakarta.ejb.Stateless;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.logging.Level;

/**
 * The RootService provides the api description
 * 
 * @author rsoika
 * 
 */
@Path("/")
@Produces({ MediaType.TEXT_HTML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.TEXT_XML })
@Stateless
public class RootRestService {
    
    @jakarta.ws.rs.core.Context
    private HttpServletRequest servletRequest;
   
    @jakarta.ws.rs.core.Context
    private HttpServletResponse servletResponse;
   
    
    private static final Logger logger = Logger.getLogger(RootRestService.class.getName());


    @GET
    @Produces(MediaType.APPLICATION_XHTML_XML)
    // @Path("/") generates jersey warning
    public StreamingOutput getRoot() {

        return new StreamingOutput() {
            public void write(OutputStream out) throws IOException, WebApplicationException {

                out.write("<div class=\"root\">".getBytes());
                out.write("<a href=\"/documents\" type=\"application/xml\" rel=\"documents\"/>".getBytes());
                out.write("<a href=\"/workflow\" type=\"application/xml\" rel=\"workflow\"/>".getBytes());
                out.write("<a href=\"/model\" type=\"application/xml\" rel=\"model\"/>".getBytes());
                out.write("<a href=\"/report\" type=\"application/xml\" rel=\"report\"/>".getBytes());
                out.write("<a href=\"/adminp\" type=\"application/xml\" rel=\"adminp\"/>".getBytes());
                out.write("<a href=\"/eventlog\" type=\"application/xml\" rel=\"eventlog\"/>".getBytes());
                out.write("</div>".getBytes());
            }
        };

    }
    
    
    /**
     * Method to invalidate the current user session 
     * <p>
     * Should be called by a client 
     */
    @GET
    @Path("/logout")
    public void logout() {
        try {
            servletRequest.logout();
            HttpSession session = servletRequest.getSession(false);
            if (servletRequest.isRequestedSessionIdValid() && session != null) {
                session.invalidate();
            }
        }
        catch (ServletException e) {
            logger.log(Level.WARNING, "Failed to logout from API endpoint /logout : {0}", e.getMessage());
            return;
        }
        logger.finest("Logout successfull");
    }
    
  
}
