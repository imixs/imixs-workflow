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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.TransformerException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.bpmn.BPMNEntityBuilder;
import org.imixs.workflow.bpmn.BPMNUtil;
import org.imixs.workflow.engine.ModelService;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.exceptions.ModelException;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.elements.Activity;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.core.UriInfo;

/**
 * The WorkflowService Handler supports methods to process different kind of
 * request URIs
 * 
 * @author rsoika
 * 
 */
@Path("/model")
@Produces({ MediaType.TEXT_HTML, MediaType.APPLICATION_XHTML_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON,
        MediaType.TEXT_XML })
@Stateless
public class ModelRestService {
    private static final Logger logger = Logger.getLogger(ModelRestService.class.getName());

    static List<String> modelEntityTypes = Arrays.asList("WorkflowEnvironmentEntity", "processentity",
            "activityentity");

    @Inject
    private WorkflowService workflowService;

    @Inject
    private ModelService modelService;

    @Inject
    private DocumentRestService documentRestService;

    @jakarta.ws.rs.core.Context
    private HttpServletRequest servletRequest;

    @GET
    @Produces({ MediaType.TEXT_HTML })
    public StreamingOutput getModelOverview() {
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

                out.write("<h1>Imixs-Workflow Model Service</h1>".getBytes());
                out.write("<p>".getBytes());
                printVersionTable(out);
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
     * This helper method prints the current model information in html format
     */
    private void printVersionTable(OutputStream out) {
        try {
            StringBuffer buffer = new StringBuffer();

            // compute rootContext:
            String rootContext = servletRequest.getContextPath() + servletRequest.getServletPath();

            buffer.append("<table>");
            buffer.append("<tr><th>Version</th><th>Uploaded</th><th>Workflow Groups</th></tr>");
            // append current model version table as a html string
            buffer.append(modelVersionTableToString(rootContext));

            buffer.append("</table>");
            out.write(buffer.toString().getBytes());
        } catch (ModelException | IOException e) {
            // no opp!
            try {
                out.write("No model definition found.".getBytes());
            } catch (IOException e1) {

                e1.printStackTrace();
            }
        }
    }

    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public String getModelXML() {
        List<String> col = null;
        StringBuffer sb = new StringBuffer();
        sb.append("<model>");
        try {
            col = modelService.getVersions();

            for (String aversion : col) {
                sb.append("<version>" + aversion + "</version>");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        sb.append("</model>");
        return sb.toString();
    }

    @GET
    @Path("/{version}/tasks/")
    public Response findAllTasks(@PathParam("version") String version, @QueryParam("items") String items,
            @QueryParam("format") String format) {
        List<ItemCollection> result = new ArrayList<>();

        BPMNModel model;
        try {
            model = modelService.getBPMNModel(version);
        } catch (ModelException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Model version not found: " + e.getMessage())
                    .build();
        }

        // find all tasks
        Set<Activity> activities = model.findAllActivities();
        for (Activity task : activities) {
            if (BPMNUtil.isImixsTaskElement(task)) {
                ItemCollection taskEntity = BPMNEntityBuilder.build(task);
                result.add(taskEntity);
            }
        }

        return documentRestService.convertResultList(result, items, format);
    }

    /**
     * Returns the XML representation of a BPMN model
     * 
     * @param version
     * @param uriInfo
     * @return
     */
    @GET
    @Path("/{version}/bpmn")
    public Response getModelFile(@PathParam("version") String version, @Context UriInfo uriInfo) {

        // lookup model
        BPMNModel bpmnModel;
        try {
            bpmnModel = modelService.getBPMNModel(version);
        } catch (ModelException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Model version not found: " + e.getMessage())
                    .build();
        }
        if (bpmnModel != null) {
            StreamingOutput stream = output -> {
                try {
                    bpmnModel.writeToOutputStream(bpmnModel.getDoc(), output);
                } catch (TransformerException e) {
                    // Handle exception: Log it or rethrow as a WebApplicationException
                    throw new WebApplicationException("Error while transforming BPMN Model to XML", e);
                }
            };
            return Response.ok(stream)
                    .type(MediaType.APPLICATION_XML)
                    .build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

    }

    /**
     * Returns the model definition containing general model information (e.g.
     * $ModelVersion).
     * 
     */
    @GET
    @Path("/{version}/definition")
    public Response getDefiniton(@PathParam("version") String version, @QueryParam("items") String items,
            @QueryParam("format") String format) {
        ItemCollection definition = null;
        try {
            BPMNModel model = modelService.getBPMNModel(version);
            ModelManager modelManager = new ModelManager(workflowService);
            definition = modelManager.loadDefinition(model);
        } catch (Exception e) {
            throw new WebApplicationException("BPMN Model Error: ", e);
        }
        return documentRestService.convertResult(definition, items, format);
    }

    /**
     * Returns the Task BPMN element by its ID and VersionID.
     * 
     * @param version
     * @param taskID
     * @param items
     * @param format
     * @return
     */
    @GET
    @Path("/{version}/tasks/{taskid}")
    public Response getTask(@PathParam("version") String version, @PathParam("taskid") int taskID,
            @QueryParam("items") String items, @QueryParam("format") String format) {
        ItemCollection task = null;
        try {
            BPMNModel model = modelService.getBPMNModel(version);
            ModelManager modelManager = new ModelManager(workflowService);
            task = modelManager.findTaskByID(model, taskID);
        } catch (Exception e) {
            throw new WebApplicationException("BPMN Model Error: ", e);
        }
        return documentRestService.convertResult(task, items, format);
    }

    /**
     * Returns the Task BPMN element by its ID and VersionID.
     * 
     * @param version
     * @param taskID
     * @param items
     * @param format
     * @return
     */
    @GET
    @Path("/{version}/tasks/{taskid}/events/{eventid}")
    public Response getEvent(@PathParam("version") String version,
            @PathParam("taskid") int taskID,
            @PathParam("eventid") int eventID,
            @QueryParam("items") String items, @QueryParam("format") String format) {
        ItemCollection event = null;
        try {
            BPMNModel model = modelService.getBPMNModel(version);
            ModelManager modelManager = new ModelManager(workflowService);
            event = modelManager.findEventByID(model, taskID, eventID);
        } catch (Exception e) {
            throw new WebApplicationException("BPMN Model Error: ", e);
        }
        return documentRestService.convertResult(event, items, format);
    }

    @GET
    @Path("/{version}/tasks/{taskid}/events")
    public Response findAllEventsByTask(@PathParam("version") String version, @PathParam("taskid") int taskID,
            @QueryParam("items") String items, @QueryParam("format") String format) {
        List<ItemCollection> result = null;

        BPMNModel model;
        try {
            model = modelService.getBPMNModel(version);
        } catch (ModelException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Model version not found: " + e.getMessage())
                    .build();
        }
        ModelManager modelManager = new ModelManager(workflowService);
        result = modelManager.findEventsByTask(model, taskID);

        return documentRestService.convertResultList(result, items, format);
    }

    /**
     * Returns a list of all Workflow Groups of the given model
     * 
     * @param version
     * @return
     */
    @GET
    @Path("/{version}/groups")
    public Response getGroups(@PathParam("version") String version, @QueryParam("items") String items,
            @QueryParam("format") String format) {
        List<ItemCollection> result = new ArrayList<>();
        try {
            BPMNModel model = modelService.getBPMNModel(version);
            ModelManager modelManager = new ModelManager(workflowService);
            Set<String> groups = modelManager.findAllGroupsByModel(model);

            for (String group : groups) {
                ItemCollection itemColGroup = new ItemCollection();
                itemColGroup.setModelVersion(version);
                itemColGroup.setWorkflowGroup(group);
                result.add(itemColGroup);
            }
        } catch (ModelException e) {
            throw new WebApplicationException("BPMN Model Error: ", e);
        }
        return documentRestService.convertResultList(result, items, format);
    }

    /**
     * Returns a list of all Tasks of a specific workflow group.
     * 
     * @param version
     * @return
     */
    @GET
    @Path("/{version}/groups/{group}")
    public Response findTasksByGroup(@PathParam("version") String version, @PathParam("group") String group,
            @QueryParam("items") String items, @QueryParam("format") String format) {
        List<ItemCollection> result = new ArrayList<>();
        try {
            BPMNModel model = modelService.getBPMNModel(version);
            ModelManager modelManager = new ModelManager(workflowService);
            result = modelManager.findTasks(model, group);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return documentRestService.convertResultList(result, items, format);
    }

    /**
     * Returns a list of start Tasks of a given Process Group
     * <p>
     * In case of a collaboration diagram only Pool names are compared. The default
     * process (Public Process) will be ignored.
     * 
     * @param version
     * @return
     */
    @GET
    @Path("/{version}/groups/{group}/start")
    @Produces(MediaType.APPLICATION_JSON) // or MediaType.APPLICATION_JSON
    public Response findStartTasksByGroup(@PathParam("version") String version, @PathParam("group") String group,
            @QueryParam("items") String items, @QueryParam("format") String format) {
        List<ItemCollection> result = null;
        try {
            BPMNModel model = modelService.getBPMNModel(version);
            ModelManager modelManager = new ModelManager(workflowService);
            result = modelManager.findStartTasks(model, group);
            return documentRestService.convertResultList(result, items, format);
        } catch (ModelException e) {
            throw new WebApplicationException("BPMN Model Error: ", e);
        }
    }

    /**
     * Returns a list of end Tasks of a given Process Group
     * <p>
     * In case of a collaboration diagram only Pool names are compared. The default
     * process (Public Process) will be ignored.
     * 
     * @param version
     * @return
     */
    @GET
    @Path("/{version}/groups/{group}/end")
    @Produces(MediaType.APPLICATION_JSON) // or MediaType.APPLICATION_JSON
    public Response findEndTasksByGroup(@PathParam("version") String version, @PathParam("group") String group,
            @QueryParam("items") String items, @QueryParam("format") String format) {
        List<ItemCollection> result = null;
        try {
            BPMNModel model = modelService.getBPMNModel(version);
            ModelManager modelManager = new ModelManager(workflowService);
            result = modelManager.findEndTasks(model, group);
            return documentRestService.convertResultList(result, items, format);
        } catch (ModelException e) {
            throw new WebApplicationException("BPMN Model Error: ", e);
        }

    }

    @DELETE
    @Path("/{version}")
    public void deleteModel(@PathParam("version") String version) {
        try {
            modelService.deleteModelData(version);
        } catch (Exception e) {
            throw new WebApplicationException("BPMN Model Error: ", e);
        }

    }

    /**
     * This method consumes a Imixs BPMN model file and updates the corresponding
     * model information.
     * 
     * @param bpmnModel
     * @return
     */
    @PUT
    @Path("/bpmn/{filename}")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response putBPMNModel(@PathParam("filename") String filename, InputStream inputStream) {
        try {
            logger.fine("BPMN Model file upload started for file: " + filename);
            // Validate filename
            if (!filename.toLowerCase().endsWith(".bpmn")) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("File must have .bpmn extension")
                        .build();
            }

            // first create a new InputStream from content (see issue #896)
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = inputStream.read(buffer)) > -1) {
                baos.write(buffer, 0, len);
            }
            byte[] content = baos.toByteArray();
            logger.fine("Reading BPMN Model from input stream (size: " + content.length + " bytes)");
            InputStream contentStream = new ByteArrayInputStream(content);
            // now we are ready to parse the inputStream to BPMN model
            BPMNModel model = BPMNModelFactory.read(contentStream);

            // Save model using the model service
            modelService.saveModel(model);
            return Response.status(Response.Status.OK).build();

        } catch (BPMNModelException e) {
            logger.log(Level.WARNING, "Failed to parse BPMN model file: {0}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid BPMN file format: " + e.getMessage())
                    .build();
        } catch (ModelException e) {
            logger.log(Level.WARNING, "Failed to save model: {0}", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to save model: " + e.getMessage())
                    .build();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to save model: {0}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Failed to save model: " + e.getMessage())
                    .build();
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to close input stream", e);
            }
        }
    }

    @POST
    @Path("/bpmn/{filename}")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response postBPMNModel(@PathParam("filename") String filename, InputStream inputStream) {
        return putBPMNModel(filename, inputStream);
    }

    @PUT
    @Path("/bpmn")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response putBPMNModel(InputStream inputStream) {
        try {
            logger.fine("BPMN Model file upload started... ");

            // Parse input stream to BPMN model
            BPMNModel model = BPMNModelFactory.read(inputStream);

            // Save model using the model service
            modelService.saveModel(model);

            logger.fine("BPMN Model file upload completed successfully");
            return Response.status(Response.Status.OK).build();

        } catch (BPMNModelException e) {
            logger.log(Level.WARNING, "Failed to parse BPMN model file: {0}",
                    e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid BPMN file format: " + e.getMessage())
                    .build();
        } catch (ModelException e) {
            logger.log(Level.WARNING, "Failed to save model: {0}", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to save model: " + e.getMessage())
                    .build();
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to close input stream", e);
            }
        }
    }

    @POST
    @Path("/bpmn")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response postBPMNModel(InputStream inputStream) {
        return putBPMNModel(inputStream);
    }

    /**
     * Returns the current model information in html format
     * 
     * @param rootContext
     * @return model version table as a html string
     * @throws ModelException
     */
    private String modelVersionTableToString(String rootContext) throws ModelException {

        List<String> modelVersionList = modelService.getVersions();
        StringBuffer buffer = new StringBuffer();

        for (String modelVersion : modelVersionList) {
            appendTagsToBuffer(modelVersion, rootContext, buffer);
        }
        return buffer.toString();
    }

    /**
     * Helper method to build a HTML table row entry with model information for a
     * given model version.
     * 
     * @param modelVersion
     * @param rootContext
     * @param buffer
     * @throws ModelException
     */
    private void appendTagsToBuffer(String modelVersion, String rootContext, StringBuffer buffer)
            throws ModelException {
        BPMNModel model = modelService.getBPMNModel(modelVersion);
        ModelManager modelManager = new ModelManager(workflowService);
        ItemCollection modelEntity = modelService.loadModelMetaData(modelVersion);

        // now check groups...
        Set<String> groupList = modelManager.findAllGroupsByModel(model);
        buffer.append("<tr>");

        if (modelEntity != null) {
            buffer.append("<td><a href=\"" + rootContext + "/model/" + modelVersion + "/bpmn\">" + modelVersion
                    + "</a></td>");
            // print upload date...
            if (modelEntity != null) {
                Date dat = modelEntity.getItemValueDate("$Modified");
                SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                buffer.append("<td>" + formater.format(dat) + "</td>");
            }
        } else {
            buffer.append("<td>" + modelVersion + "</td>");
            buffer.append("<td> - </td>");
        }

        // Groups
        buffer.append("<td>");
        for (String group : groupList) {
            // build a link for each group to get the Tasks

            buffer.append("<a href=\"" + rootContext + "/model/" + modelVersion + "/groups/" + group + "\">"
                    + group + "</a></br>");
        }
        buffer.append("</td>");
        buffer.append("</tr>");
    }
}
