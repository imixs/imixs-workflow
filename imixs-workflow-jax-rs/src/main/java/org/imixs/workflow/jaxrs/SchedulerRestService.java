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
import java.text.ParseException;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.scheduler.SchedulerService;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ImixsExceptionHandler;
import org.imixs.workflow.xml.XMLDataCollectionAdapter;
import org.imixs.workflow.xml.XMLDocument;
import org.imixs.workflow.xml.XMLDocumentAdapter;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

/**
 * The SchedulerRestService provides methods to
 * <ul>
 * <li>Load a Scheduler configuration
 * <li>Save a Scheduler configuration
 * <li>Start a Scheduler
 * <li>Stop a Scheduler
 * </ul>
 * 
 * @see SchedulerService
 * @author rsoika
 * 
 */
@Path("/scheduler")
@Produces({ MediaType.TEXT_HTML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.TEXT_XML })
@Stateless
public class SchedulerRestService {

    @Inject
    private DocumentRestService documentRestService;

    @Inject
    SchedulerService schedulerService;

    @Resource
    private SessionContext ctx;

    private static final Logger logger = Logger.getLogger(SchedulerRestService.class.getName());

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
                out.write("<h1>Imixs-Workflow Scheduler REST Service</h1>".getBytes());
                out.write(
                        "<p>See the <a href=\"https://www.imixs.org/doc/engine/scheduling.html\" target=\"_blank\">Imixs Workflow - Scheduling API</a> for more information about this Service.</p>"
                                .getBytes());
                // end
                out.write("</body></html>".getBytes());
            }
        };

    }

    /**
     * Update a scheduler configuration
     * <p>
     * If the scheduler configuration is 'enabled' the scheduler will be started
     * automatically. Otherwise the scheduler will be stopped if running
     * 
     * @param xmlworkitem - entity to be saved
     * @return
     */
    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_XML)
    @Consumes({ MediaType.APPLICATION_XML, "text/xml" })
    public Response updateSchedulerConfiguration(XMLDocument xmlworkitem) {
        if (ctx.isCallerInRole("org.imixs.ACCESSLEVEL.MANAGERACCESS") == false) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        ItemCollection configuration;
        configuration = XMLDocumentAdapter.putDocument(xmlworkitem);

        if (configuration == null) {
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }

        try {

            boolean enabled = configuration.getItemValueBoolean("_scheduler_enabled");
            if (enabled) {
                configuration = schedulerService.start(configuration);
            } else {
                configuration = schedulerService.stop(configuration);
            }
            schedulerService.updateTimerDetails(configuration);
            schedulerService.saveConfiguration(configuration);

        } catch (AccessDeniedException e) {
            logger.severe(e.getMessage());
            configuration = ImixsExceptionHandler.addErrorMessage(e, configuration);
        } catch (RuntimeException e) {
            logger.severe(e.getMessage());
            configuration = ImixsExceptionHandler.addErrorMessage(e, configuration);
        } catch (ParseException e) {
            logger.severe(e.getMessage());
            configuration = ImixsExceptionHandler.addErrorMessage(e, configuration);
        }

        // return config
        try {
            if (configuration.hasItem("$error_code"))
                return Response.ok(XMLDataCollectionAdapter.getDataCollection(configuration), MediaType.APPLICATION_XML)
                        .status(Response.Status.NOT_ACCEPTABLE).build();
            else
                return Response.ok(XMLDataCollectionAdapter.getDataCollection(configuration), MediaType.APPLICATION_XML)
                        .build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
    }

    /**
     * Returns a scheduler configuration by its name
     * 
     * @param name - name of the configuration
     * @return the scheduler configuration entity
     */
    @GET
    @Path("/{name}")
    public Response loadConfiguration(@PathParam("name") String name, @QueryParam("items") String items,
            @QueryParam("format") String format) {

        ItemCollection configuration = schedulerService.loadConfiguration(name);
        if (configuration == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        schedulerService.updateTimerDetails(configuration);

        return documentRestService.convertResult(configuration, items, format);
    }

}
