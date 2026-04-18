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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Logger;

import org.imixs.workflow.FileData;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.cluster.DataService;
import org.imixs.workflow.engine.cluster.exceptions.ClusterException;
import org.imixs.workflow.engine.cluster.exceptions.DataException;
import org.imixs.workflow.xml.XMLDataCollectionAdapter;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Encoded;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.core.UriInfo;

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

                out.write("<html><head>".getBytes(StandardCharsets.UTF_8));
                out.write("<style>".getBytes(StandardCharsets.UTF_8));
                out.write("table {padding:0px;width: 100%;margin-left: -2px;margin-right: -2px;}"
                        .getBytes(StandardCharsets.UTF_8));
                out.write(
                        "body,td,select,input,li {font-family: Verdana, Helvetica, Arial, sans-serif;font-size: 13px;}"
                                .getBytes(StandardCharsets.UTF_8));
                out.write("table th {color: white;background-color: #bbb;text-align: left;font-weight: bold;}"
                        .getBytes(StandardCharsets.UTF_8));

                out.write("table th,table td {font-size: 12px;}".getBytes(StandardCharsets.UTF_8));

                out.write("table tr.a {background-color: #ddd;}".getBytes(StandardCharsets.UTF_8));

                out.write("table tr.b {background-color: #eee;}".getBytes(StandardCharsets.UTF_8));

                out.write("</style>".getBytes(StandardCharsets.UTF_8));
                out.write("</head><body>".getBytes(StandardCharsets.UTF_8));

                // body
                out.write("<h1>Imixs-Cluster REST Service</h1>".getBytes(StandardCharsets.UTF_8));
                out.write(
                        "<p>See the <a href=\"http://www.imixs.org/xml/restservice/snapshotservice.html\" target=\"_blank\">Imixs REST Service API</a> for more information about this Service.</p>"
                                .getBytes(StandardCharsets.UTF_8));

                // end
                out.write("</body></html>".getBytes(StandardCharsets.UTF_8));
            }
        };

    }

    /**
     * Loads a snapshot from the archive and returns a HTML representation.
     * 
     * @param id - snapshot id
     * @return XMLDataCollection
     */
    @GET
    @Path("/snapshot/{id : ([0-9a-f]{8}-.*|[0-9a-f]{11}-.*)}")
    public Response getSnapshot(@PathParam("id") String id, @QueryParam("format") String format) {

        try {
            logger.info("...read snapshot...");
            ItemCollection snapshot = dataService.loadSnapshot(id);
            return convertResult(snapshot, format);
        } catch (Exception e) {
            logger.warning("...Failed to load snapshot: " + e.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Snapshot not found: " + id)
                    .build();

        } finally {

        }
    }

    @GET
    @Produces("text/html")
    @Path("/search")
    public StreamingOutput searchSnapshots() {

        return new StreamingOutput() {
            public void write(OutputStream out) throws IOException, WebApplicationException {

                // Compute the base path for the redirect
                String rootContext = servletRequest.getContextPath() + servletRequest.getServletPath();

                out.write("<html><head>".getBytes(StandardCharsets.UTF_8));
                out.write("<style>".getBytes(StandardCharsets.UTF_8));
                out.write(
                        "body,td,select,input,li {font-family: Verdana, Helvetica, Arial, sans-serif;font-size: 13px;}"
                                .getBytes(StandardCharsets.UTF_8));
                out.write("h1 {color: #444;}".getBytes(StandardCharsets.UTF_8));
                out.write(
                        ".search-box {margin: 20px 0; padding: 16px; background-color: #eee; border-radius: 4px; display:inline-block;}"
                                .getBytes());
                out.write("input[type=text] {font-size: 13px; padding: 6px 8px; width: 340px; border: 1px solid #bbb;}"
                        .getBytes(StandardCharsets.UTF_8));
                out.write(
                        "button {font-size: 13px; padding: 6px 14px; background-color: #555; color: white; border: none; cursor: pointer; margin-left: 8px;}"
                                .getBytes(StandardCharsets.UTF_8));
                out.write("button:hover {background-color: #333;}".getBytes(StandardCharsets.UTF_8));
                out.write("</style>".getBytes(StandardCharsets.UTF_8));

                // JavaScript redirect on form submit
                out.write(("<script>" +
                        "function lookup() {" +
                        "  var uid = document.getElementById('uid').value.trim();" +
                        "  if (uid) {" +
                        "    window.location.href = '" + rootContext + "/snapshots/' + uid;" +
                        "  }" +
                        "}" +
                        "function handleKey(event) {" +
                        "  if (event.key === 'Enter') { lookup(); }" +
                        "}" +
                        "</script>").getBytes(StandardCharsets.UTF_8));

                out.write("</head><body>".getBytes(StandardCharsets.UTF_8));

                // Heading
                out.write("<h1>Imixs-Cluster REST Service</h1>".getBytes(StandardCharsets.UTF_8));
                out.write(
                        "<p>Enter a $uniqueid to lookup current snapshots</p>"
                                .getBytes(StandardCharsets.UTF_8));

                // UniqueID lookup form
                out.write("<div class=\"search-box\">".getBytes(StandardCharsets.UTF_8));
                out.write("<strong>Snapshot Lookup</strong><br/><br/>".getBytes(StandardCharsets.UTF_8));
                out.write("<label for=\"uid\">UniqueID:</label><br/>".getBytes(StandardCharsets.UTF_8));
                out.write(
                        "<input type=\"text\" id=\"uid\" placeholder=\"e.g. dc5e6483-6342-4a22-82ec-f363ccfe8de0\" onkeydown=\"handleKey(event)\"/>"
                                .getBytes(StandardCharsets.UTF_8));
                out.write("<button onclick=\"lookup()\">Show Snapshots</button>".getBytes(StandardCharsets.UTF_8));
                out.write("</div>".getBytes(StandardCharsets.UTF_8));

                // end
                out.write("</body></html>".getBytes(StandardCharsets.UTF_8));
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
                out.write("<html><head>".getBytes(StandardCharsets.UTF_8));
                out.write("<style>".getBytes(StandardCharsets.UTF_8));
                out.write("table {padding:0px;width: 75%;margin-left: -2px;margin-right: -2px;}"
                        .getBytes(StandardCharsets.UTF_8));
                out.write(
                        "body,td,select,input,li {font-family: Verdana, Helvetica, Arial, sans-serif;font-size: 13px;}"
                                .getBytes(StandardCharsets.UTF_8));
                out.write("table th {color: white;background-color: #bbb;text-align: left;font-weight: bold;}"
                        .getBytes(StandardCharsets.UTF_8));

                out.write("table th,table td {font-size: 12px;}".getBytes(StandardCharsets.UTF_8));

                out.write("table tr.a {background-color: #ddd;}".getBytes(StandardCharsets.UTF_8));

                out.write("table tr.b {background-color: #eee;}".getBytes(StandardCharsets.UTF_8));

                out.write("</style>".getBytes(StandardCharsets.UTF_8));
                out.write("</head><body>".getBytes(StandardCharsets.UTF_8));

                out.write("<h1>Imixs-Workflow Snapshot Service</h1>".getBytes(StandardCharsets.UTF_8));
                out.write("<p>".getBytes(StandardCharsets.UTF_8));

                // print table

                try {
                    List<String> result = dataService.loadSnapshotsByUniqueID(uniqueid, count, true);
                    printSnapshotTable(out, result);
                } catch (ClusterException e) {

                    e.printStackTrace();
                }

                out.write("</p>".getBytes(StandardCharsets.UTF_8));
                // footer
                out.write(
                        "<p>See the <a href=\"http://www.imixs.org/doc/restapi/modelservice.html\" target=\"_bank\">Imixs-Workflow REST API</a> for more information.</p>"
                                .getBytes(StandardCharsets.UTF_8));

                // end
                out.write("</body></html>".getBytes(StandardCharsets.UTF_8));
            }
        };

    }

    /**
     * Returns a file attachment based on its MD5 Checksum
     * <p>
     * The query parameter 'contentType' can be added to specify the returned
     * content type.
     * 
     * @param md5 - md5 checksum to identify the file content
     * @return
     */
    @GET
    @Path("/md5/{md5}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getSnapshotFileByMD5Checksum(@PathParam("md5") @Encoded String md5,
            @QueryParam("contentType") String contentType) {

        // load the snapshot
        byte[] fileContent = null;
        try {

            // load snapshto without the file data
            fileContent = dataService.loadFileContent(md5);

        } catch (ClusterException | DataException e) {
            logger.warning("...failed to load file: " + e.getMessage());
            e.printStackTrace();
        }
        // extract the file...
        try {

            if (fileContent != null && fileContent.length > 0) {
                // Set content type in order of the contentType stored
                // in the $file attribute
                Response.ResponseBuilder builder = Response.ok(fileContent, contentType);
                return builder.build();
            } else {
                logger.warning("Unable to open file by md5 checksum: '" + md5 + "' - no content!");
                // workitem not found
                return Response.status(Response.Status.NOT_FOUND).build();
            }

        } catch (Exception e) {
            logger.severe(
                    "Unable to open file by md5 checksum: '" + md5 + "' - error: " + e.getMessage());

        }

        logger.severe("Unable to open file by md5 checksum: '" + md5 + "'");
        return Response.status(Response.Status.NOT_FOUND).build();

    }

    /**
     * Returns a file attachment located in the property $file of the specified
     * snapshot
     * <p>
     * The file name will be encoded. With a URLDecode the filename is decoded in
     * different formats and searched in the file list. This is not a nice solution.
     * 
     * @param uniqueid
     * @return
     */
    @GET
    @Path("/snapshot/{id}/file/{file}")
    public Response getSnapshotFileByName(@PathParam("id") String id, @PathParam("file") @Encoded String file,
            @Context UriInfo uriInfo) {

        // load the snapshot
        // Session session = null;
        // Cluster cluster = null;
        ItemCollection snapshot = null;
        FileData fileData = null;
        try {
            logger.finest("...read snapshot...");
            // cluster = clusterService.getCluster();
            // session = clusterService.getArchiveSession(cluster);
            // load snapshto without the file data
            snapshot = dataService.loadSnapshot(id, false);

            String fileNameUTF8 = URLDecoder.decode(file, "UTF-8");
            String fileNameISO = URLDecoder.decode(file, "ISO-8859-1");
            // try to guess encodings.....
            fileData = snapshot.getFileData(fileNameUTF8);
            if (fileData == null)
                fileData = snapshot.getFileData(fileNameISO);
            if (fileData == null)
                fileData = snapshot.getFileData(file);

            if (fileData != null) {
                // now we load the content
                fileData = dataService.loadFileData(fileData);
            }

        } catch (DataException | ClusterException | UnsupportedEncodingException e) {
            logger.warning("...Failed to load file: " + e.getMessage());
            e.printStackTrace();
        } finally {

        }
        // extract the file...
        try {

            if (fileData != null) {
                // Set content type in order of the contentType stored
                // in the $file attribute
                Response.ResponseBuilder builder = Response.ok(fileData.getContent(), fileData.getContentType());
                return builder.build();
            } else {
                logger.warning("ArchiveRestService unable to open file: '" + file + "' in workitem '" + id
                        + "' - error: Filename not found!");
                // workitem not found
                return Response.status(Response.Status.NOT_FOUND).build();
            }

        } catch (Exception e) {
            logger.severe("ArchiveRestService unable to open file: '" + file + "' in workitem '" + id + "' - error: "
                    + e.getMessage());
            e.printStackTrace();
        }

        logger.severe("ArchiveRestService unable to open file: '" + file + "' in workitem '" + id + "'");
        return Response.status(Response.Status.NOT_FOUND).build();

    }

    /**
     * This method converts a single ItemCollection into a Jax-rs response object.
     * <p>
     * The method expects optional items and format string (json|xml)
     * <p>
     * In case the result set is null, than the method returns an empty collection.
     * 
     * @param result list of ItemCollection
     * @param items  - optional item list
     * @param format - optional format string (json|xml)
     * @return jax-rs Response object.
     */
    private Response convertResult(ItemCollection workitem, String format) {
        if (workitem == null) {
            workitem = new ItemCollection();
        }
        if ("json".equals(format)) {
            return Response
                    // Set the status and Put your entity here.
                    .ok(XMLDataCollectionAdapter.getDataCollection(workitem, null))
                    // Add the Content-Type header to tell Jersey which format it should marshall
                    // the entity into.
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).build();
        } else if ("xml".equals(format)) {
            return Response
                    // Set the status and Put your entity here.
                    .ok(XMLDataCollectionAdapter.getDataCollection(workitem, null))
                    // Add the Content-Type header to tell Jersey which format it should marshall
                    // the entity into.
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML).build();
        } else {
            // default header param
            return Response
                    // Set the status and Put your entity here.
                    .ok(XMLDataCollectionAdapter.getDataCollection(workitem, null))
                    .build();

        }
    }

    /**
     * This helper method prints the list of snapshot ids in html format
     */
    private void printSnapshotTable(OutputStream out, List<String> snapshots) {
        try {
            StringBuffer buffer = new StringBuffer();

            // compute rootContext:
            String rootContext = servletRequest.getContextPath() + servletRequest.getServletPath();
            buffer.append("<p><a href=\"" + rootContext + "/snapshots/search\">&larr; New Search</a></p>");
            buffer.append("<table>");
            buffer.append("<tr><th>SnapshotID</th></tr>");
            // append current model version table as a html string

            for (String id : snapshots) {

                buffer.append("<tr><td><a href=\"" + rootContext + "/snapshots/snapshot/" + id + "\">" + id
                        + "</a></td></tr>");
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
