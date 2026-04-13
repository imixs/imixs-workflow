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
import java.util.List;
import java.util.logging.Logger;

import org.imixs.workflow.engine.cluster.DataService;
import org.imixs.workflow.engine.cluster.exceptions.ClusterException;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.StreamingOutput;

/**
 * The ClusterRestService provides methods to access Snapshot documents form the
 * Cluster Service
 * 
 * @author rsoika
 * 
 */
@Path("/snapshots")
@Produces({ MediaType.TEXT_HTML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.TEXT_XML })
@Stateless
public class SnapshotRestService {

    @Inject
    private DataService dataService;

    @jakarta.ws.rs.core.Context
    private HttpServletRequest servletRequest;

    @Resource
    private SessionContext ctx;

    private static final Logger logger = Logger.getLogger(SnapshotRestService.class.getName());

    @GET
    @Produces(MediaType.APPLICATION_XHTML_XML)
    // @Path("/") generates jersey warning
    public StreamingOutput getRoot() {

        return new StreamingOutput() {
            public void write(OutputStream out) throws IOException, WebApplicationException {

                out.write("<div class=\"root\">".getBytes());
                out.write("<a href=\"/{uniqueid}\" type=\"application/xml\" rel=\"{uniqueid}\"/>".getBytes());

                out.write("</div>".getBytes());
            }
        };

    }

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
                out.write("<h1>Imixs-Cluster REST Service</h1>".getBytes());
                out.write(
                        "<p>See the <a href=\"http://www.imixs.org/xml/restservice/snapshotservice.html\" target=\"_blank\">Imixs REST Service API</a> for more information about this Service.</p>"
                                .getBytes());

                // end
                out.write("</body></html>".getBytes());
            }
        };

    }

    /**
     * returns the list of all snapshot documents associated with a $uniqueid
     * 
     * Regex for
     * 
     * UID - e.g: bcc776f9-4e5a-4272-a613-9f5ebf35354d
     * 
     * Snapshot: bcc776f9-4e5a-4272-a613-9f5ebf35354d-9b6655
     * 
     * deprecated format : 132d37bfd51-9a7868
     * 
     * @param uniqueid
     * @return
     */
    @GET
    @Path("/{uniqueid : ([0-9a-f]{8}-.*|[0-9a-f]{11}-.*)}")
    public StreamingOutput getSnapshotsByUID(@PathParam("uniqueid") String uniqueid, @QueryParam("items") String items,
            @QueryParam("format") String format,
            @QueryParam("count") int count) {

        return new StreamingOutput() {
            public void write(OutputStream out) throws IOException, WebApplicationException {
                out.write("<html><head>".getBytes());
                out.write("<style>".getBytes());
                out.write("table {padding:0px;width: 75%;margin-left: -2px;margin-right: -2px;}".getBytes());
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

                out.write("<h1>Imixs-Workflow Snapshot Service</h1>".getBytes());
                out.write("<p>".getBytes());

                // print table

                try {
                    List<String> result = dataService.loadSnapshotsByUniqueID(uniqueid, count, true);
                    printSnapshotTable(out, result);
                } catch (ClusterException e) {

                    e.printStackTrace();
                }

                out.write("</p>".getBytes());
                // footer
                out.write(
                        "<p>See the <a href=\"http://www.imixs.org/doc/restapi/modelservice.html\" target=\"_bank\">Imixs-Workflow REST API</a> for more information.</p>"
                                .getBytes());

                // end
                out.write("</body></html>".getBytes());
            }
        };

    }

    /**
     * This helper method prints the list of snapshot ids in html format
     */
    private void printSnapshotTable(OutputStream out, List<String> snapshots) {
        try {
            StringBuffer buffer = new StringBuffer();

            // compute rootContext:
            String rootContext = servletRequest.getContextPath() + servletRequest.getServletPath();

            buffer.append("<table>");
            buffer.append("<tr><th>SnapshotID</th></tr>");
            // append current model version table as a html string

            for (String id : snapshots) {
                buffer.append("<td><a href=\"" + rootContext + "/snapshot/" + id + "\">" + id
                        + "</a></td>");
            }

            buffer.append("</table>");
            out.write(buffer.toString().getBytes());
        } catch (IOException e) {
            // no opp!
            try {
                out.write("No model definition found.".getBytes());
            } catch (IOException e1) {

                e1.printStackTrace();
            }
        }
    }

}
