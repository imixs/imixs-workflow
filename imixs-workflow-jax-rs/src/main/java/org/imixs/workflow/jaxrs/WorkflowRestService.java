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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.Encoded;
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

import org.imixs.workflow.FileData;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ImixsExceptionHandler;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.util.ImixsJSONParser;
import org.imixs.workflow.xml.XMLDataCollection;
import org.imixs.workflow.xml.XMLDataCollectionAdapter;
import org.imixs.workflow.xml.XMLDocument;
import org.imixs.workflow.xml.XMLDocumentAdapter;

import jakarta.ejb.Stateless;
import jakarta.servlet.http.HttpServletRequest;
import java.util.logging.Level;

/**
 * The WorkflowService Handler supports methods to process different kind of
 * request URIs
 * 
 * @author rsoika
 * 
 */
@Path("/workflow")
@Produces({ MediaType.TEXT_HTML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.TEXT_XML })
@Stateless
public class WorkflowRestService {

    @Inject
    private WorkflowService workflowService;

    @Inject
    private DocumentRestService documentRestService;

    @jakarta.ws.rs.core.Context
    private HttpServletRequest servletRequest;

    private static final Logger logger = Logger.getLogger(WorkflowRestService.class.getName());

    @GET
    @Produces("text/html")
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
                out.write("<h1>Imixs-Workflow REST Service</h1>".getBytes());
                out.write(
                        "<p>See the <a href=\"http://www.imixs.org/doc/restapi/workflowservice.html\" target=\"_blank\">Imixs-Workflow REST API</a> for more information about this Service.</p>"
                                .getBytes());

                // end
                out.write("</body></html>".getBytes());
            }
        };

    }

    /**
     * returns a single workitem defined by $uniqueid
     * 
     * @param uniqueid
     * @return
     */
    @GET
    @Path("/workitem/{uniqueid : ([0-9a-f]{8}-.*|[0-9a-f]{11}-.*)}")
    public Response getWorkItem(@PathParam("uniqueid") String uniqueid, @QueryParam("items") String items,
            @QueryParam("format") String format) {

        ItemCollection workitem;
        try {
            workitem = workflowService.getWorkItem(uniqueid);
            if (workitem == null) {
                // workitem not found
                return Response.status(Response.Status.NOT_FOUND).build();
            }

        } catch (Exception e) {
            e.printStackTrace();
            workitem = null;
        }

        return documentRestService.convertResult(workitem, items, format);
    }

    /**
     * Returns a file attachment located in the property $file of the specified
     * workitem
     * <p>
     * The file name will be encoded. With a URLDecode the filename is decoded in
     * different formats and searched in the file list. This is not a nice solution.
     * 
     * @param uniqueid
     * @return
     */
    @GET
    @Path("/workitem/{uniqueid : ([0-9a-f]{8}-.*|[0-9a-f]{11}-.*)}/file/{file}")
    public Response getWorkItemFile(@PathParam("uniqueid") String uniqueid, @PathParam("file") @Encoded String file,
            @Context UriInfo uriInfo) {

        ItemCollection workItem;
        try {
            workItem = workflowService.getWorkItem(uniqueid);

            if (workItem != null) {

                String fileNameUTF8 = URLDecoder.decode(file, "UTF-8");
                String fileNameISO = URLDecoder.decode(file, "ISO-8859-1");

                // fetch FileData object
                FileData fileData = null;
                // try to guess encodings.....
                fileData = workItem.getFileData(fileNameUTF8);
                if (fileData == null)
                    fileData = workItem.getFileData(fileNameISO);
                if (fileData == null)
                    fileData = workItem.getFileData(file);

                if (fileData != null) {
                    // Set content type in order of the contentType stored
                    // in the $file attribute
                    Response.ResponseBuilder builder = Response.ok(fileData.getContent(), fileData.getContentType());
                    return builder.build();
                } else {
                    logger.log(Level.WARNING, "WorkflowRestService unable to open file: ''{0}''"
                            + " in workitem ''{1}'' - error: Filename not found!", new Object[]{file, uniqueid});
                    // workitem not found
                    return Response.status(Response.Status.NOT_FOUND).build();
                }

            } else {
                logger.log(Level.WARNING, "WorkflowRestService unable to open file: ''{0}''"
                        + " in workitem ''{1}'' - error: Workitem not found!", new Object[]{file, uniqueid});
                // workitem not found
                return Response.status(Response.Status.NOT_FOUND).build();
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "WorkflowRestService unable to open file: ''{0}''"
                    + " in workitem ''{1}'' - error: {2}", new Object[]{file, uniqueid, e.getMessage()});
            e.printStackTrace();
        }

        logger.log(Level.SEVERE, "WorkflowRestService unable to open file: ''{0}''"
                + " in workitem ''{1}''", new Object[]{file, uniqueid});
        return Response.status(Response.Status.NOT_FOUND).build();

    }

    /**
     * Returns a collection of events of a workitem, visible to the current user
     * 
     * @param uniqueid of workitem
     * @return list of event entities
     */
    @GET
    @Path("/workitem/events/{uniqueid : ([0-9a-f]{8}-.*|[0-9a-f]{11}-.*)}")
    public Response getEvents(@PathParam("uniqueid") String uniqueid, @QueryParam("format") String format) {
        List<ItemCollection> result = null;
        try {
            result = workflowService.getEvents(this.workflowService.getDocumentService().load(uniqueid));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return documentRestService.convertResultList(result, null, format);
    }

    /**
     * Returns a collection of workitems representing the worklist by the current
     * user
     * 
     * @param start
     * @param count
     * @param type
     * @param sortorder
     */
    @GET
    @Path("/worklist")
    public Response getWorkList(@QueryParam("type") String type,
            @DefaultValue("0") @QueryParam("pageIndex") int pageIndex,
            @DefaultValue("10") @QueryParam("pageSize") int pageSize,
            @DefaultValue("") @QueryParam("sortBy") String sortBy,
            @DefaultValue("false") @QueryParam("sortReverse") Boolean sortReverse, @QueryParam("items") String items,
            @QueryParam("format") String format) {

        return getTaskListByOwner(null, type, pageSize, pageIndex, sortBy, sortReverse, items, format);
    }

    @GET
    @Path("/tasklist/owner/{owner}")
    public Response getTaskListByOwner(@PathParam("owner") String owner, @QueryParam("type") String type,
            @DefaultValue("0") @QueryParam("pageIndex") int pageIndex,
            @DefaultValue("10") @QueryParam("pageSize") int pageSize,
            @DefaultValue("") @QueryParam("sortBy") String sortBy,
            @DefaultValue("false") @QueryParam("sortReverse") Boolean sortReverse, @QueryParam("items") String items,
            @QueryParam("format") String format) {
        List<ItemCollection> result = null;
        try {
            if ("null".equalsIgnoreCase(owner))
                owner = null;

            // decode URL param
            if (owner != null)
                owner = URLDecoder.decode(owner, "UTF-8");

            result = workflowService.getWorkListByOwner(owner, type, pageSize, pageIndex, sortBy, sortReverse);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return documentRestService.convertResultList(result, items, format);
    }

    /**
     * Returns a collection of workitems for which the specified user has explicit write permission.
     * 
     * @param start
     * @param count
     * @param type
     * @param sortorder
     */
    @GET
    @Path("/tasklist/author/{user}")
    public Response getTaskListByAuthor(@PathParam("user") String user, @QueryParam("type") String type,
            @DefaultValue("0") @QueryParam("pageIndex") int pageIndex,
            @DefaultValue("10") @QueryParam("pageSize") int pageSize,
            @DefaultValue("") @QueryParam("sortBy") String sortBy,
            @DefaultValue("false") @QueryParam("sortReverse") Boolean sortReverse, @QueryParam("items") String items,
            @QueryParam("format") String format) {
        List<ItemCollection> result = null;
        try {
            if ("null".equalsIgnoreCase(user))
                user = null;

            // decode URL param
            if (user != null)
                user = URLDecoder.decode(user, "UTF-8");

            result = workflowService.getWorkListByAuthor(user, type, pageSize, pageIndex, sortBy, sortReverse);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return documentRestService.convertResultList(result, items, format);
    }

    /**
     * Returns a collection of workitems where the current user has a write permission.
     * This means that the current userID or at least one of its roles is contained in
     * the $writeaccess property.
     * 
     * @param start
     * @param count
     * @param type
     * @param sortorder
     */
    @GET
    @Path("/tasklist/writeaccess")
    public Response getTaskListByWriteAccess( @QueryParam("type") String type,
            @DefaultValue("0") @QueryParam("pageIndex") int pageIndex,
            @DefaultValue("10") @QueryParam("pageSize") int pageSize,
            @DefaultValue("") @QueryParam("sortBy") String sortBy,
            @DefaultValue("false") @QueryParam("sortReverse") Boolean sortReverse, @QueryParam("items") String items,
            @QueryParam("format") String format) {
        List<ItemCollection> result = null;
        try {
            result = workflowService.getWorkListByWriteAccess(type, pageSize, pageIndex, sortBy, sortReverse);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return documentRestService.convertResultList(result, items, format);
    }

    @GET
    @Path("/tasklist/creator/{creator}")
    public Response getTaskListByCreator(@PathParam("creator") String creator, @QueryParam("type") String type,
            @DefaultValue("0") @QueryParam("pageIndex") int pageIndex,
            @DefaultValue("10") @QueryParam("pageSize") int pageSize,
            @DefaultValue("") @QueryParam("sortBy") String sortBy,
            @DefaultValue("false") @QueryParam("sortReverse") Boolean sortReverse, @QueryParam("items") String items,
            @QueryParam("format") String format) {
        List<ItemCollection> result = null;
        try {
            if ("null".equalsIgnoreCase(creator))
                creator = null;

            // decode URL param
            if (creator != null)
                creator = URLDecoder.decode(creator, "UTF-8");

            result = workflowService.getWorkListByCreator(creator, type, pageSize, pageIndex, sortBy, sortReverse);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return documentRestService.convertResultList(result, items, format);
    }

    @GET
    @Path("/tasklist/processid/{processid}")
    public Response getTaskListByProcessID(@PathParam("processid") int processid, @QueryParam("type") String type,
            @DefaultValue("0") @QueryParam("pageIndex") int pageIndex,
            @DefaultValue("10") @QueryParam("pageSize") int pageSize,
            @DefaultValue("") @QueryParam("sortBy") String sortBy,
            @DefaultValue("false") @QueryParam("sortReverse") Boolean sortReverse, @QueryParam("items") String items,
            @QueryParam("format") String format) {
        List<ItemCollection> result = null;
        try {
            result = workflowService.getWorkListByProcessID(processid, type, pageSize, pageIndex, sortBy, sortReverse);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return documentRestService.convertResultList(result, items, format);
    }

    @GET
    @Path("/tasklist/group/{processgroup}")
    public Response getTaskListByGroup(@PathParam("processgroup") String processgroup, @QueryParam("type") String type,
            @DefaultValue("0") @QueryParam("pageIndex") int pageIndex,
            @DefaultValue("10") @QueryParam("pageSize") int pageSize,
            @DefaultValue("") @QueryParam("sortBy") String sortBy,
            @DefaultValue("false") @QueryParam("sortReverse") Boolean sortReverse, @QueryParam("items") String items,
            @QueryParam("format") String format) {
        List<ItemCollection> result = null;
        try {
            // decode URL param
            if (processgroup != null)
                processgroup = URLDecoder.decode(processgroup, "UTF-8");
            result = workflowService.getWorkListByGroup(processgroup, type, pageSize, pageIndex, sortBy, sortReverse);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return documentRestService.convertResultList(result, items, format);
    }

    @GET
    @Path("/tasklist/ref/{uniqueid : ([0-9a-f]{8}-.*|[0-9a-f]{11}-.*)}")
    public Response getTaskListByRef(@PathParam("uniqueid") String uniqueid, @QueryParam("type") String type,
            @DefaultValue("0") @QueryParam("pageIndex") int pageIndex,
            @DefaultValue("10") @QueryParam("pageSize") int pageSize,
            @DefaultValue("") @QueryParam("sortBy") String sortBy,
            @DefaultValue("false") @QueryParam("sortReverse") Boolean sortReverse, @QueryParam("items") String items,
            @QueryParam("format") String format) {
        List<ItemCollection> result = null;
        try {
            result = workflowService.getWorkListByRef(uniqueid, type, pageSize, pageIndex, sortBy, sortReverse);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return documentRestService.convertResultList(result, items, format);
    }

    /**
     * This method expects a form post and processes the WorkItem by the
     * WorkflowService EJB. After the workItem was processed the method redirect the
     * request to the provided action URI. The action URI can also be computed by
     * the Imixs Workflow ResutlPlugin
     * 
     * @param requestBodyStream - form content
     * @param action            - return URI
     * @return
     */
    @POST
    @Path("/workitem")
    @Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
    public Response postFormWorkitem(InputStream requestBodyStream, @QueryParam("items") String items) {
        logger.fine("postFormWorkitem @POST /workitem  method:postWorkitem....");
        // parse the workItem.
        ItemCollection workitem = parseWorkitem(requestBodyStream);
        return processWorkitem(workitem, null, items);
    }

    /**
     * This method expects a form post and processes the WorkItem by the
     * WorkflowService EJB. After the workItem was processed the method redirect the
     * request to the provided action URI. The action URI can also be computed by
     * the Imixs Workflow ResutlPlugin
     * 
     * @param requestBodyStream - form content
     * @param items             - optional item list to be returned in the result
     * @return
     */
    @POST
    @Path("/workitem/{uniqueid : ([0-9a-f]{8}-.*|[0-9a-f]{11}-.*)}")
    @Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
    public Response postFormWorkitemByUnqiueID(@PathParam("uniqueid") String uid, InputStream requestBodyStream,
            @QueryParam("items") String items) {
        logger.finest("......postFormWorkitem @POST /workitem  method:postWorkitem....");
        // parse the workItem.
        ItemCollection workitem = parseWorkitem(requestBodyStream);
        return processWorkitem(workitem, uid, items);
    }

    /**
     * This method expects a form post.
     * 
     * @see putWorkitemDefault
     * @param requestBodyStream
     * @param items             - optional item list to be returned in the result
     * @return
     */
    @PUT
    @Path("/workitem")
    @Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
    public Response putFormWorkitem(InputStream requestBodyStream, @QueryParam("items") String items) {
        logger.fine("putFormWorkitem @POST /workitem  delegate to POST....");
        return postFormWorkitem(requestBodyStream, items);
    }

    /**
     * This method post a ItemCollection object to be processed by the
     * WorkflowManager. The method test for the properties $taskidid and $eventid
     * 
     * NOTE!! - this method did not update an existing instance of a workItem. The
     * behavior is different to the method putWorkitem(). It need to be discussed if
     * the behavior is wrong or not.
     * 
     * @param workitem - new workItem data
     * @param items    - optional item list to be returned in the result
     */
    @POST
    @Path("/workitem")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.APPLICATION_JSON })
    public Response postWorkitem(XMLDocument xmlworkitem, @QueryParam("items") String items) {
        logger.fine("postWorkitem @POST /workitem  method:postWorkitem....");
        ItemCollection workitem = XMLDocumentAdapter.putDocument(xmlworkitem);
        return processWorkitem(workitem, null, items);
    }

    /**
     * Delegater
     * 
     * @param workitem
     * @param items    - optional item list to be returned in the result
     * @return
     */
    @PUT
    @Path("/workitem")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.APPLICATION_JSON })
    public Response putWorkitem(XMLDocument workitem, @QueryParam("items") String items) {
        logger.fine("putWorkitem @PUT /workitem  delegate to POST....");
        return postWorkitem(workitem, items);
    }

    @POST
    @Path("/workitem/{uniqueid : ([0-9a-f]{8}-.*|[0-9a-f]{11}-.*)}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.APPLICATION_JSON })
    public Response postWorkitemByUniqueID(@PathParam("uniqueid") String uniqueid, XMLDocument xmlworkitem,
            @QueryParam("items") String items) {
        logger.log(Level.FINE, "postWorkitemByUniqueID @POST /workitem/{0}  method:postWorkitemXML....", uniqueid);
        ItemCollection workitem;
        workitem = XMLDocumentAdapter.putDocument(xmlworkitem);
        return processWorkitem(workitem, uniqueid, items);
    }

    /**
     * Delegater for PUT postXMLWorkitemByUniqueID
     * 
     * @param workitem
     * @param items    - optional item list to be returned in the result
     * @return
     */
    @PUT
    @Path("/workitem/{uniqueid : ([0-9a-f]{8}-.*|[0-9a-f]{11}-.*)}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.APPLICATION_JSON })
    public Response putWorkitemByUniqueID(@PathParam("uniqueid") String uniqueid, XMLDocument xmlworkitem,
            @QueryParam("items") String items) {
        logger.fine("putWorkitem @PUT /workitem/{uniqueid}  delegate to POST....");
        return postWorkitemByUniqueID(uniqueid, xmlworkitem, items);
    }

    /**
     * This method post a collection of ItemCollection objects to be processed by
     * the WorkflowManager.
     * 
     * @param worklist - workitem list data
     * @param items    - optional item list to be returned in the result
     */
    @POST
    @Path("/workitems")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.APPLICATION_JSON })
    public Response postWorkitems(XMLDataCollection worklist) {

        logger.fine("postWorkitems @POST /workitems  method:postWorkitems....");

        XMLDocument entity;
        ItemCollection itemCollection;
        try {
            // save new entities into database and update modelversion.....
            for (int i = 0; i < worklist.getDocument().length; i++) {
                entity = worklist.getDocument()[i];
                itemCollection = XMLDocumentAdapter.putDocument(entity);
                // process entity
                workflowService.processWorkItem(itemCollection);
            }
            return Response.status(Response.Status.OK).build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Response.status(Response.Status.NOT_ACCEPTABLE).build();
    }

    @PUT
    @Path("/workitems")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.APPLICATION_JSON })
    public Response putWorkitems(XMLDataCollection worklist) {
        logger.fine("pupWorkitems @PUT /workitems  delegate to @POST....");
        return postWorkitems(worklist);
    }

    /**
     * This method expects JSON in BADGARFISH notation to processed by the
     * WorkflowService EJB.
     * <p>
     * The Method returns a workitem with the new data. If a processException Occurs
     * the method returns an object with the error code
     * <p>
     * The JSON is parsed manually by teh imixs json parser. The expreced notation
     * is:
     * <p>
     * <code>... value":{"@type":"xs:int","$":"10"}</code>
     * 
     * 
     * @param requestBodyStream
     * @param items             - optional item list to be returned in the result
     * @return JSON object
     * @throws Exception
     */
    @POST
    @Path("/workitem/typed")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postTypedWorkitemJSON(InputStream requestBodyStream, @QueryParam("error") String error,
            @QueryParam("items") String items) {

        logger.fine("postTypedWorkitemJSON @POST workitem....");
        ItemCollection workitem = null;
        try {
            List<ItemCollection> result = ImixsJSONParser.parse(requestBodyStream);
            if (result != null && result.size() > 0) {
                workitem = result.get(0);
            }
            // workitem = JSONParser.parseWorkitem(requestBodyStream, encoding);
        } catch (ParseException e) {
            logger.severe("postJSONWorkitem wrong json format!");
            e.printStackTrace();
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        } catch (UnsupportedEncodingException e) {
            logger.severe("postJSONWorkitem wrong json format!");
            e.printStackTrace();
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
        if (workitem == null) {
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }

        return processWorkitem(workitem, null, items);
    }

    /**
     * Delegater for PUT postJSONTypedWorkitem
     * 
     * @param workitem
     * @param items    - optional item list to be returned in the result
     * @return
     */
    @PUT
    @Path("/workitem/typed")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putTypedWorkitemJSON(InputStream requestBodyStream, @QueryParam("error") String error,
            @QueryParam("items") String items) {

        logger.fine("putTypedWorkitemJSON @PUT /workitem/{uniqueid}  delegate to POST....");
        return postTypedWorkitemJSON(requestBodyStream, error, items);
    }

    @POST
    @Path("/workitem/{uniqueid : ([0-9a-f]{8}-.*|[0-9a-f]{11}-.*)}/typed")
    @Consumes({ MediaType.APPLICATION_JSON })
    public Response postTypedWorkitemJSONByUniqueID(@PathParam("uniqueid") String uniqueid,
            InputStream requestBodyStream, @QueryParam("error") String error, @QueryParam("items") String items) {
        logger.log(Level.FINE, "postJSONWorkitemByUniqueID @POST /workitem/{0}....", uniqueid);

        ItemCollection workitem = null;
        try {
            List<ItemCollection> result = ImixsJSONParser.parse(requestBodyStream);
            if (result != null && result.size() > 0) {
                workitem = result.get(0);
            }
            // workitem = JSONParser.parseWorkitem(requestBodyStream, encoding);
        } catch (ParseException e) {
            logger.severe("postJSONWorkitemByUniqueID wrong json format!");
            e.printStackTrace();
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        } catch (UnsupportedEncodingException e) {
            logger.severe("postJSONWorkitemByUniqueID wrong json format!");
            e.printStackTrace();
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
        return processWorkitem(workitem, uniqueid, items);
    }

    /**
     * Delegater for PUT postJSONWorkitemByUniqueID
     * 
     * @param workitem
     * @param items    - optional item list to be returned in the result
     * @return
     */
    @PUT
    @Path("/workitem/{uniqueid : ([0-9a-f]{8}-.*|[0-9a-f]{11}-.*)}/typed")
    @Consumes({ MediaType.APPLICATION_JSON })
    public Response putTypedWorkitemJSONByUniqueID(@PathParam("uniqueid") String uniqueid,
            InputStream requestBodyStream, @QueryParam("error") String error, @QueryParam("items") String items) {

        logger.fine("postJSONWorkitemByUniqueID @PUT /workitem/{uniqueid}  delegate to POST....");
        return postTypedWorkitemJSONByUniqueID(uniqueid, requestBodyStream, error, items);
    }

    /**
     * This method expects a form post. The method parses the input stream to
     * extract the provides field/value pairs. NOTE: The method did not(!) assume
     * that the put/post request contains a complete workItem. For this reason the
     * method loads the existing instance of the corresponding workItem (identified
     * by the $uniqueid) and adds the values provided by the put/post request into
     * the existing instance.
     * 
     * The following kind of lines which can be included in the InputStream will be
     * skipped
     * 
     * <code>
     * 	------------------------------1a26f3661ff7
    	Content-Disposition: form-data; name="query"
    	Connection: keep-alive
    	Content-Type: multipart/form-data; boundary=---------------------------195571638125373
    	Content-Length: 5680
    
    	-----------------------------195571638125373
     * </code>
     * 
     * @param requestBodyStream
     * @return a workitem
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ItemCollection parseWorkitem(InputStream requestBodyStream) {
        Vector<String> vMultiValueFieldNames = new Vector<String>();
        BufferedReader in = new BufferedReader(new InputStreamReader(requestBodyStream));
        String inputLine;
        ItemCollection workitem = new ItemCollection();

        logger.fine("[WorkflowRestService] parseWorkitem....");

        try {
            while ((inputLine = in.readLine()) != null) {
                // System.out.println(inputLine);

                // split params separated by &
                StringTokenizer st = new StringTokenizer(inputLine, "&", false);
                while (st.hasMoreTokens()) {
                    String fieldValue = st.nextToken();
                    logger.log(Level.FINEST, "[WorkflowRestService] parse line:{0}", fieldValue);
                    try {
                        fieldValue = URLDecoder.decode(fieldValue, "UTF-8");

                        if (!fieldValue.contains("=")) {
                            logger.finest("[WorkflowRestService] line will be skipped");
                            continue;
                        }

                        // get fieldname
                        String fieldName = fieldValue.substring(0, fieldValue.indexOf('='));

                        // if fieldName contains blank or : or --- we skipp the
                        // line
                        if (fieldName.contains(":") || fieldName.contains(" ") || fieldName.contains(";")) {
                            logger.finest("[WorkflowRestService] line will be skipped");
                            continue;
                        }

                        // test for value...
                        if (fieldValue.indexOf('=') == fieldValue.length()) {
                            // no value
                            workitem.replaceItemValue(fieldName, "");
                            logger.log(Level.FINE, "[WorkflowRestService] no value for ''{0}''", fieldName);
                        } else {
                            fieldValue = fieldValue.substring(fieldValue.indexOf('=') + 1);
                            // test for a multiValue field - did we know
                            // this
                            // field....?
                            fieldName = fieldName.toLowerCase();
                            if (vMultiValueFieldNames.indexOf(fieldName) > -1) {

                                List v = workitem.getItemValue(fieldName);
                                v.add(fieldValue);
                                logger.log(Level.FINE, "[WorkflowRestService] multivalue for ''{0}'' = ''{1}''",
                                        new Object[]{fieldName, fieldValue});
                                workitem.replaceItemValue(fieldName, v);
                            } else {
                                // first single value....
                                logger.log(Level.FINE, "[WorkflowRestService] value for ''{0}'' = ''{1}''",
                                        new Object[]{fieldName, fieldValue});
                                workitem.replaceItemValue(fieldName, fieldValue);
                                vMultiValueFieldNames.add(fieldName);
                            }
                        }

                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        } catch (IOException e1) {
            logger.severe("[WorkflowRestService] Unable to parse workitem data!");
            e1.printStackTrace();
            return null;
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        return workitem;
    }

    /**
     * This helper method processes a workitem. The response code of the response
     * object is set to 200 if case the processing was successful. In case of an
     * Exception a error message is generated and the status NOT_ACCEPTABLE is
     * returned.
     * <p>
     * The param 'uid' is optional and will be validated against the workitem data
     * <p>
     * This method is called by the POST/PUT methods.
     * 
     * @param workitem
     * @param uid      - optional $uniqueid, will be validated.
     * @param items    - optional list of items to be returned
     * @return
     */
    private Response processWorkitem(ItemCollection workitem, String uid, String items) {

        // test for null values
        if (workitem == null) {
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }

        // validate optional uniqueId
        if (uid != null && !uid.isEmpty() && !workitem.getUniqueID().isEmpty() && !uid.equals(workitem.getUniqueID())) {
            logger.log(Level.SEVERE, "@POST/@PUT workitem/{0} : $UNIQUEID did not match,"
                    + " remove $uniqueid to create a new instnace!", uid);
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }

        if (uid != null && !uid.isEmpty()) {
            // set provided uniqueid
            workitem.replaceItemValue(WorkflowKernel.UNIQUEID, uid);
        }

        try {
            // remove old error code and message
            workitem.removeItem("$error_code");
            workitem.removeItem("$error_message");
            // now lets try to process the workitem...
            workitem = workflowService.processWorkItem(workitem);

        } catch (AccessDeniedException e) {
            workitem = ImixsExceptionHandler.addErrorMessage(e, workitem);
        } catch (PluginException e) {
            workitem = ImixsExceptionHandler.addErrorMessage(e, workitem);
        } catch (RuntimeException e) {
            workitem = ImixsExceptionHandler.addErrorMessage(e, workitem);
        } catch (ModelException e) {
            workitem = ImixsExceptionHandler.addErrorMessage(e, workitem);
        }

        // return workitem
        try {
            if (workitem.hasItem("$error_code")) {
                logger.log(Level.SEVERE, "{0}: {1}",
                        new Object[]{workitem.getItemValueString("$error_code"), workitem.getItemValueString("$error_message")});
                return Response.ok(XMLDataCollectionAdapter.getDataCollection(workitem))
                        .status(Response.Status.NOT_ACCEPTABLE).build();
            } else {
                return Response.ok(
                        XMLDataCollectionAdapter.getDataCollection(workitem, RestAPIUtil.getItemList(items)))
                        .build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }

    }

}
