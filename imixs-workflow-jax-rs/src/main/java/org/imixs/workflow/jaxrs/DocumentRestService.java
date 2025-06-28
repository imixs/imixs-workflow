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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.index.SchemaService;
import org.imixs.workflow.engine.index.SearchService;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ImixsExceptionHandler;
import org.imixs.workflow.exceptions.QueryException;
import org.imixs.workflow.xml.XMLCount;
import org.imixs.workflow.xml.XMLDataCollectionAdapter;
import org.imixs.workflow.xml.XMLDocument;
import org.imixs.workflow.xml.XMLDocumentAdapter;

import jakarta.ejb.Stateless;
import jakarta.servlet.http.HttpServletRequest;
import java.util.logging.Level;

/**
 * The DocumentService provides methods to access the DocumentService EJB
 * 
 * @author rsoika
 * 
 */
@Path("/documents")
@Produces({ MediaType.TEXT_HTML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.TEXT_XML })
@Stateless
public class DocumentRestService {

    @Inject
    private DocumentService documentService;

    @Inject
    private SchemaService schemaService;

    @Resource
    private SessionContext ctx;

    private static final Logger logger = Logger.getLogger(DocumentRestService.class.getName());

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
                out.write("<h1>Imixs-Document REST Service</h1>".getBytes());
                out.write(
                        "<p>See the <a href=\"http://www.imixs.org/xml/restservice/documentservice.html\" target=\"_blank\">Imixs REST Service API</a> for more information about this Service.</p>"
                                .getBytes());

                // end
                out.write("</body></html>".getBytes());
            }
        };

    }

    /**
     * returns a single document defined by $uniqueid
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
    public Response getDocument(@PathParam("uniqueid") String uniqueid, @QueryParam("items") String items,
            @QueryParam("format") String format) {
        ItemCollection document = null;
        try {
            document = documentService.load(uniqueid);
            if (document == null) {
                // document not found
                return Response.status(Response.Status.NOT_FOUND).build();      
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return convertResult(document, items, format);
    }
 
    /**
     * Returns a resultset for a lucene Search Query
     * 
     * @param query
     * @param pageSize
     * @param pageIndex
     * @param items
     * @return
     */
    @GET
    @Path("/search/{query}")
    public Response findDocumentsByQuery(@PathParam("query") String query,
            @DefaultValue("-1") @QueryParam("pageSize") int pageSize,
            @DefaultValue("0") @QueryParam("pageIndex") int pageIndex, @QueryParam("sortBy") String sortBy,
            @QueryParam("sortReverse") boolean sortReverse, @QueryParam("items") String items,
            @QueryParam("format") String format) {
        List<ItemCollection> result = null;
        try {
            // decode query...
            String decodedQuery = URLDecoder.decode(query, "UTF-8");
            result = documentService.find(decodedQuery, pageSize, pageIndex, sortBy, sortReverse);

        } catch (Exception e) {
            logger.log(Level.WARNING, "Invalid Search Query: {0}", e.getMessage());

            ItemCollection error = new ItemCollection();
            error.setItemValue("$error_message", e.getMessage());
            error.setItemValue("$error_code", "" + Response.Status.NOT_ACCEPTABLE);
            return Response.ok(XMLDataCollectionAdapter.getDataCollection(error)).status(Response.Status.NOT_ACCEPTABLE)
                    .build();

        }

        return convertResultList(result, items, format);
    }

    /**
     * Returns a resultset for a JPQL statement
     * 
     * @param query - JPQL statement
     * @param pageSize - page size
     * @param pageIndex - page index (default = 0)
     * @param items - optional list of items
     * @return result set.
     */
    @GET
    @Path("/jpql/{query}")
    public Response findDocumentsByJPQL(@PathParam("query") String query,
            @DefaultValue("" + SearchService.DEFAULT_PAGE_SIZE) @QueryParam("pageSize") int pageSize,
            @DefaultValue("0") @QueryParam("pageIndex") int pageIndex, @QueryParam("items") String items,
            @QueryParam("format") String format) {
        List<ItemCollection> result = null;
        try {
            // decode query...
            String decodedQuery = URLDecoder.decode(query, "UTF-8");
            // compute first result....
            int firstResult = pageIndex * pageSize;
            result = documentService.getDocumentsByQuery(decodedQuery, firstResult, pageSize);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return convertResultList(result, items, format);
    }

    /**
     * Returns a total hits for a lucene Search Query
     * 
     * @param query
     * @param pageSize
     * @param pageIndex
     * @param items
     * @return
     */
    @GET
    @Path("/count/{query}")
    public Response countTotalHitsByQuery(@PathParam("query") String query,
            @DefaultValue("-1") @QueryParam("maxResult") int maxResult, @QueryParam("format") String format) {
        XMLCount xmlcount = new XMLCount();
        String decodedQuery;
        try {
            decodedQuery = URLDecoder.decode(query, "UTF-8");
            xmlcount.count = (long) documentService.count(decodedQuery, maxResult);
        } catch (UnsupportedEncodingException | QueryException e) {
            xmlcount.count = 0l;
            logger.severe(e.getMessage());
        }

        if ("json".equals(format)) {
            return Response
                    // Set the status and Put your entity here.
                    .ok(xmlcount)
                    // Add the Content-Type header to tell Jersey which format it should marshall
                    // the entity into.
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).build();
        } else if ("xml".equals(format)) {
            return Response
                    // Set the status and Put your entity here.
                    .ok(xmlcount)
                    // Add the Content-Type header to tell Jersey which format it should marshall
                    // the entity into.
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML).build();
        } else {
            // default header param
            return Response
                    // Set the status and Put your entity here.
                    .ok(xmlcount).build();
        }
    }

    /**
     * Returns the total pages for a lucene Search Query and a given page size.
     * 
     * @param query
     * @param pageSize
     * @param pageIndex
     * @param items
     * @return
     */
    @GET
    @Path("/countpages/{query}")
    public Response countTotalPagesByQuery(@PathParam("query") String query,
            @DefaultValue("-1") @QueryParam("pageSize") int pageSize, @QueryParam("format") String format) {
        XMLCount xmlcount = new XMLCount();
        String decodedQuery;
        try {
            decodedQuery = URLDecoder.decode(query, "UTF-8");
            xmlcount.count = (long) documentService.countPages(decodedQuery, pageSize);
        } catch (UnsupportedEncodingException | QueryException e) {
            xmlcount.count = 0l;
            logger.severe(e.getMessage());

        }

        if ("json".equals(format)) {
            return Response
                    // Set the status and Put your entity here.
                    .ok(xmlcount)
                    // Add the Content-Type header to tell Jersey which format it should marshall
                    // the entity into.
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).build();
        } else if ("xml".equals(format)) {
            return Response
                    // Set the status and Put your entity here.
                    .ok(xmlcount)
                    // Add the Content-Type header to tell Jersey which format it should marshall
                    // the entity into.
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML).build();
        } else {
            // default header param
            return Response
                    // Set the status and Put your entity here.
                    .ok(xmlcount).build();
        }
    }

    /**
     * The method saves a document provided in xml format. The caller need to be
     * assigned to the access role 'org.imixs.ACCESSLEVEL.MANAGERACCESS'
     * 
     * Note: the method merges the content of the given document into an existing
     * one because the DocumentService method save() did not merge an entity. But
     * the rest service typically consumes only a subset of attributes. So this is
     * the reason why we merge the entity here. In different to the behavior of the
     * DocumentService the WorkflowService method process() did this merge
     * automatically.
     * 
     * @param xmlworkitem - document to be saved
     * @param items       - optional item list to be returned in the result
     * @return
     */
    @POST
    // @Path("/") generates jersey warning
    @Produces(MediaType.APPLICATION_XML)
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.APPLICATION_JSON, })
    public Response postDocument(XMLDocument xmlworkitem, @QueryParam("items") String items) {
        if (ctx.isCallerInRole("org.imixs.ACCESSLEVEL.MANAGERACCESS") == false) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        ItemCollection workitem;
        workitem = XMLDocumentAdapter.putDocument(xmlworkitem);

        if (workitem == null) {
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }

        try {

            // try to load current instance of this entity
            ItemCollection currentInstance = documentService.load(workitem.getItemValueString(WorkflowKernel.UNIQUEID));
            if (currentInstance != null) {
                // merge entity into current instance
                // an instance of this Entity still exists! so we update the
                // new values here....
                currentInstance.replaceAllItems(workitem.getAllItems());
                workitem = currentInstance;
            }

            workitem.removeItem("$error_code");
            workitem.removeItem("$error_message");
            // now lets try to process the workitem...
            workitem = documentService.save(workitem);

        } catch (AccessDeniedException e) {
            logger.severe(e.getMessage());
            workitem = ImixsExceptionHandler.addErrorMessage(e, workitem);
        } catch (RuntimeException e) {
            logger.severe(e.getMessage());
            workitem = ImixsExceptionHandler.addErrorMessage(e, workitem);
        }

        // return workitem
        try {
            if (workitem.hasItem("$error_code")) {
                logger.log(Level.SEVERE, "{0}: {1}", new Object[]{workitem.getItemValueString("$error_code"), workitem.getItemValueString("$error_message")});
                return Response.ok(XMLDataCollectionAdapter.getDataCollection(workitem), MediaType.APPLICATION_XML)
                        .status(Response.Status.NOT_ACCEPTABLE).build();
            } else {
                return Response.ok(
                        XMLDataCollectionAdapter.getDataCollection(workitem, RestAPIUtil.getItemList(items)),
                        MediaType.APPLICATION_XML).build();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
    }

    /**
     * Delegater putEntity @PUT
     * 
     * @see putWorkitemDefault
     * @param xmlworkitem - document to be saved
     * @param items       - optional item list to be returned in the result
     * @return
     */
    @PUT
    // @Path("/") generates jersey warning
    @Produces(MediaType.APPLICATION_XML)
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.APPLICATION_JSON, })
    public Response putDocument(XMLDocument xmlworkitem, @QueryParam("items") String items) {
        logger.finest("putDocument @PUT /  delegate to POST....");
        return postDocument(xmlworkitem, items);
    }

    /**
     * This method deletes an entity
     * 
     */
    @DELETE
    @Path("/{uniqueid : ([0-9a-f]{8}-.*|[0-9a-f]{11}-.*)}")
    public Response deleteEntity(@PathParam("uniqueid") String uniqueid) {
        if (ctx.isCallerInRole("org.imixs.ACCESSLEVEL.MANAGERACCESS") == false) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        ItemCollection entity = documentService.load(uniqueid);
        if (entity != null) {
            documentService.remove(entity);
        }

        return Response.status(Response.Status.OK).build();
    }

    /**
     * This method creates a backup of the result set form a JQPL query. The entity
     * list will be stored into the file system. The backup can be restored by
     * calling the restore method
     * 
     * 
     * @param query
     * @param filepath - path in server filesystem
     * @param snapshots - opitonal backup snapshots only
     * @return
     */
    @PUT
    @Path("/backup/{query}")
    public Response backup(@PathParam("query") String query, @QueryParam("filepath") String filepath,
            @QueryParam("snapshots") boolean snapshots) {
        
        if (ctx.isCallerInRole("org.imixs.ACCESSLEVEL.MANAGERACCESS") == false) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        try {
            // decode query...
            String decodedQuery = URLDecoder.decode(query, "UTF-8");
            documentService.backup(decodedQuery, filepath,snapshots);
        } catch (IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (QueryException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.status(Response.Status.OK).build();

    }

    /**
     * This method restores a backup from the fileSystem
     * 
     * @param filepath - path in server fileSystem
     * @return
     */
    @GET
    @Path("/restore")
    public Response restore(@QueryParam("filepath") String filepath) {

        if (ctx.isCallerInRole("org.imixs.ACCESSLEVEL.MANAGERACCESS") == false) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        try {
            documentService.restore(filepath);
        } catch (IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.status(Response.Status.OK).build();

    }

    /**
     * Returns the IndexFieldListNoAnalyse from the lucensUpdateService
     * 
     * @return
     * @throws Exception
     */
    @GET
    @Path("/configuration")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public Response getConfiguration(@QueryParam("format") String format) throws Exception {
        if (ctx.isCallerInRole("org.imixs.ACCESSLEVEL.MANAGERACCESS") == false) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        ItemCollection config = schemaService.getConfiguration();

        return convertResult(config, null, format);
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
    public Response convertResult(ItemCollection workitem, String items, String format) {
        if (workitem == null) {
            return Response.status(Response.Status.NOT_FOUND).build();            
        }
        if ("json".equals(format)) {
            return Response
                    // Set the status and Put your entity here.
                    .ok(XMLDataCollectionAdapter.getDataCollection(workitem, RestAPIUtil.getItemList(items)))
                    // Add the Content-Type header to tell Jersey which format it should marshall
                    // the entity into.
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).build();
        } else if ("xml".equals(format)) {
            return Response
                    // Set the status and Put your entity here.
                    .ok(XMLDataCollectionAdapter.getDataCollection(workitem, RestAPIUtil.getItemList(items)))
                    // Add the Content-Type header to tell Jersey which format it should marshall
                    // the entity into.
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML).build();
        } else {
            // default header param
            return Response
                    // Set the status and Put your entity here.
                    .ok(XMLDataCollectionAdapter.getDataCollection(workitem, RestAPIUtil.getItemList(items)))
                    .build();
        }
    }

    /**
     * This method converts a ItemCollection List into a Jax-rs response object.
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
    public Response convertResultList(List<ItemCollection> result, String items, String format) {
        if (result == null) {
            result = new ArrayList<ItemCollection>();
        }
        if ("json".equals(format)) {
            return Response
                    // Set the status and Put your entity here.
                    .ok(XMLDataCollectionAdapter.getDataCollection(result, RestAPIUtil.getItemList(items)))
                    // Add the Content-Type header to tell Jersey which format it should marshall
                    // the entity into.
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).build();
        } else if ("xml".equals(format)) {
            return Response
                    // Set the status and Put your entity here.
                    .ok(XMLDataCollectionAdapter.getDataCollection(result, RestAPIUtil.getItemList(items)))
                    // Add the Content-Type header to tell Jersey which format it should marshall
                    // the entity into.
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML).build();
        } else {
            // default header param
            return Response
                    // Set the status and Put your entity here.
                    .ok(XMLDataCollectionAdapter.getDataCollection(result, RestAPIUtil.getItemList(items)))
                    .build();
        }
    }

}
