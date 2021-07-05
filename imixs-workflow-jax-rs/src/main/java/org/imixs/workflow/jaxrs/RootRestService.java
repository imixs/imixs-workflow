/*  
 *  Imixs-Workflow 
 *  
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
 *      Imixs Software Solutions GmbH - Project Management
 *      Ralph Soika - Software Developer
 */

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
   
    
    private static Logger logger = Logger.getLogger(RootRestService.class.getName());


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
            logger.warning("Failed to logout from API endpoint /logout : " + e.getMessage());
            return;
        }
        logger.finest("Logout successfull");
    }
    
  
}
