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

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.adminp.AdminPService;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ImixsExceptionHandler;
import org.imixs.workflow.xml.XMLDataCollection;
import org.imixs.workflow.xml.XMLDataCollectionAdapter;
import org.imixs.workflow.xml.XMLDocument;
import org.imixs.workflow.xml.XMLDocumentAdapter;

import jakarta.ejb.Stateless;
import jakarta.servlet.http.HttpServletRequest;

/**
 * The AdminPRestService provides methods to access the AdminPService EJB
 * 
 * @author rsoika
 * 
 */
@Path("/adminp")
@Produces({ MediaType.TEXT_HTML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.TEXT_XML })
@Stateless
public class AdminPRestService {

    @Inject
    private DocumentService documentService;

    @Inject
    private AdminPService adminPService;

    @Resource
    private SessionContext ctx;

    private static final Logger logger = Logger.getLogger(AdminPRestService.class.getName());

    @GET
    @Produces("text/html")
    @Path("/help")
    public StreamingOutput getHelpHTML() {

        return new StreamingOutput() {
            public void write(OutputStream out) throws IOException, WebApplicationException {

                out.write("<html><head>".getBytes());
                out.write("<style>".getBytes());
                out.write("table {padding:0px;width: 100%;margin-left: -2px;margin-right: -2px;}".getBytes());
                out.write(
                        "body,td,select,input,li {font-family: Verdana, Helvetica, Arial, sans-serif;font-size: 13px;}"
                                .getBytes());
                out.write("table th {color: white;background-color: #bbb;text-align: left;font-weight: bold;}"
                        .getBytes());

                out.write("table th,table td {font-size: 12px;}".getBytes());

                out.write("table tr.a {background-color: #ddd;}".getBytes());

                out.write("table tr.b {background-color: #eee;}".getBytes());

                out.write("</style>".getBytes());
                out.write("</head><body>".getBytes());

                // body
                out.write("<h1>Imixs-AdminP REST Service</h1>".getBytes());
                out.write(
                        "<p>See the <a href=\"http://www.imixs.org/xml/restservice/adminpservice.html\" target=\"_blank\">Imixs REST Service API</a> for more information about this Service.</p>"
                                .getBytes());

                // end
                out.write("</body></html>".getBytes());
            }
        };

    }

    /**
     * Returns all existing jobs
     * 
     * @param query
     * @param pageSize
     * @param pageIndex
     * @param items
     * @return
     */
    @GET
    @Path("/jobs")
    public XMLDataCollection getAllJobs() {
        Collection<ItemCollection> col = null;
        try {
            col = documentService.getDocumentsByType("adminp");
            return XMLDataCollectionAdapter.getDataCollection(col);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new XMLDataCollection();
    }

    /**
     * This method saves a entity provided in xml format
     * 
     * Note: the method merges the content of the given entity into an existing one
     * because the EntityService method save() did not merge an entity. But the rest
     * service typically consumes only a subset of attributes. So this is the reason
     * why we merge the entity here. In different to the behavior of the
     * EntityService the WorkflowService method process() did this merge
     * automatically.
     * 
     * @param xmlworkitem - entity to be saved
     * @return
     */
    @POST
    @Path("/jobs/")
    @Produces(MediaType.APPLICATION_XML)
    @Consumes({ MediaType.APPLICATION_XML, "text/xml" })
    public Response putJob(XMLDocument xmlworkitem) {
        if (ctx.isCallerInRole("org.imixs.ACCESSLEVEL.MANAGERACCESS") == false) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        ItemCollection workitem;
        workitem = XMLDocumentAdapter.putDocument(xmlworkitem);

        if (workitem == null) {
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }

        try {
            adminPService.createJob(workitem);
        } catch (AccessDeniedException e) {
            logger.severe(e.getMessage());
            workitem = ImixsExceptionHandler.addErrorMessage(e, workitem);
        } catch (RuntimeException e) {
            logger.severe(e.getMessage());
            workitem = ImixsExceptionHandler.addErrorMessage(e, workitem);
        }

        // return workitem
        try {
            if (workitem.hasItem("$error_code"))
                return Response.ok(XMLDataCollectionAdapter.getDataCollection(workitem), MediaType.APPLICATION_XML)
                        .status(Response.Status.NOT_ACCEPTABLE).build();
            else
                return Response.ok(XMLDataCollectionAdapter.getDataCollection(workitem), MediaType.APPLICATION_XML)
                        .build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
    }


    /**
     * Restarts a Job by ID
     * 
     * @param id
     * @return
     */
    @GET
    @Path("/jobs/restart/{id}")
    public Response restartJob(@PathParam("id") String id) {
        adminPService.restartJobByID(id);
        return Response.status(Response.Status.OK).build();
    }

    /**
     * This method deletes an entity
     * 
     */
    @DELETE
    @Path("/jobs/{uniqueid}")
    public Response deleteJob(@PathParam("uniqueid") String uniqueid) {
        if (ctx.isCallerInRole("org.imixs.ACCESSLEVEL.MANAGERACCESS") == false) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        adminPService.deleteJob(uniqueid);
        return Response.status(Response.Status.OK).build();
    }

}
