/*******************************************************************************
 *  Imixs Workflow 
 *  Copyright (C) 2001, 2011 Imixs Software Solutions GmbH,  
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
 *  	http://www.imixs.org
 *  	http://java.net/projects/imixs-workflow
 *  
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika - Software Developer
 *******************************************************************************/

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

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.imixs.workflow.FileData;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.WorkflowException;
import org.imixs.workflow.util.JSONParser;
import org.imixs.workflow.xml.XMLDataCollection;
import org.imixs.workflow.xml.XMLDataCollectionAdapter;
import org.imixs.workflow.xml.XMLDocument;
import org.imixs.workflow.xml.XMLDocumentAdapter;

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

	@javax.ws.rs.core.Context
	private HttpServletRequest servletRequest;

	private static Logger logger = Logger.getLogger(WorkflowRestService.class.getName());

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
	@Path("/workitem/{uniqueid}")
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
	@Path("/workitem/{uniqueid}/file/{file}")
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
					logger.warning("WorklfowRestService unable to open file: '" + file + "' in workitem '" + uniqueid
							+ "' - error: Filename not found!");
					// workitem not found
					return Response.status(Response.Status.NOT_FOUND).build();
				}

			} else {
				logger.warning("WorklfowRestService unable to open file: '" + file + "' in workitem '" + uniqueid
						+ "' - error: Workitem not found!");
				// workitem not found
				return Response.status(Response.Status.NOT_FOUND).build();
			}

		} catch (Exception e) {
			logger.severe("WorklfowRestService unable to open file: '" + file + "' in workitem '" + uniqueid
					+ "' - error: " + e.getMessage());
			e.printStackTrace();
		}

		logger.severe("WorklfowRestService unable to open file: '" + file + "' in workitem '" + uniqueid + "'");
		return Response.status(Response.Status.NOT_FOUND).build();

	}

	/**
	 * Returns a collection of events of a workitem, visible to the current user
	 * 
	 * @param uniqueid
	 *            of workitem
	 * @return list of event entities
	 */
	@GET
	@Path("/workitem/events/{uniqueid}")
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
	 * Returns a collection of workitems representing the worklist by the current
	 * user
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
	@Path("/tasklist/ref/{uniqueid}")
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
	 * @param requestBodyStream
	 *            - form content
	 * @param action
	 *            - return URI
	 * @return
	 */
	@POST
	@Path("/workitem")
	@Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
	public Response postFormWorkitem(InputStream requestBodyStream) {
		logger.fine("postFormWorkitem @POST /workitem  method:postWorkitem....");
		// parse the workItem.
		ItemCollection workitem = parseWorkitem(requestBodyStream);
		return processWorkitem(workitem, null);
	}

	/**
	 * This method expects a form post and processes the WorkItem by the
	 * WorkflowService EJB. After the workItem was processed the method redirect the
	 * request to the provided action URI. The action URI can also be computed by
	 * the Imixs Workflow ResutlPlugin
	 * 
	 * @param requestBodyStream
	 *            - form content
	 * @param action
	 *            - return URI
	 * @return
	 */
	@POST
	@Path("/workitem/{uniqueid : ([0-9a-f]{8}-.*|[0-9a-f]{11}-.*)}")
	@Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
	public Response postFormWorkitemByUnqiueID(@PathParam("uniqueid") String uid, InputStream requestBodyStream) {
		logger.finest("......postFormWorkitem @POST /workitem  method:postWorkitem....");
		// parse the workItem.
		ItemCollection workitem = parseWorkitem(requestBodyStream);
		return processWorkitem(workitem, uid);
	}

	/**
	 * This method expects a form post.
	 * 
	 * @see putWorkitemDefault
	 * @param requestBodyStream
	 * @return
	 */
	@PUT
	@Path("/workitem")
	@Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
	public Response putFormWorkitem(InputStream requestBodyStream) {
		logger.fine("putFormWorkitem @POST /workitem  delegate to POST....");
		return postFormWorkitem(requestBodyStream);
	}

	/**
	 * This method post a ItemCollection object to be processed by the
	 * WorkflowManager. The method test for the properties $taskidid and $eventid
	 * 
	 * NOTE!! - this method did not update an existing instance of a workItem. The
	 * behavior is different to the method putWorkitem(). It need to be discussed if
	 * the behavior is wrong or not.
	 * 
	 * @param workitem
	 *            - new workItem data
	 */
	@POST
	@Path("/workitem")
	@Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
	public Response postXMLWorkitem(XMLDocument xmlworkitem) {
		logger.fine("postXMLWorkitem @POST /workitem  method:postWorkitemXML....");
		ItemCollection workitem = XMLDocumentAdapter.putDocument(xmlworkitem);
		return processWorkitem(workitem, null);
	}

	/**
	 * Delegater
	 * 
	 * @param workitem
	 * @return
	 */
	@PUT
	@Path("/workitem")
	@Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
	public Response putXMLWorkitem(XMLDocument workitem) {
		logger.fine("putXMLWorkitem @PUT /workitem  delegate to POST....");
		return postXMLWorkitem(workitem);
	}

	@POST
	@Path("/workitem/{uniqueid : ([0-9a-f]{8}-.*|[0-9a-f]{11}-.*)}")
	@Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
	public Response postXMLWorkitemByUniqueID(@PathParam("uniqueid") String uniqueid, XMLDocument xmlworkitem) {
		logger.fine("postXMLWorkitemByUniqueID @POST /workitem/" + uniqueid + "  method:postWorkitemXML....");
		ItemCollection workitem;
		workitem = XMLDocumentAdapter.putDocument(xmlworkitem);
		return processWorkitem(workitem, uniqueid);
	}

	/**
	 * Delegater for PUT postXMLWorkitemByUniqueID
	 * 
	 * @param workitem
	 * @return
	 */
	@PUT
	@Path("/workitem/{uniqueid : ([0-9a-f]{8}-.*|[0-9a-f]{11}-.*)}")
	@Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
	public Response putXMLWorkitemByUniqueID(@PathParam("uniqueid") String uniqueid, XMLDocument xmlworkitem) {
		logger.fine("putXMLWorkitem @PUT /workitem/{uniqueid}  delegate to POST....");
		return postXMLWorkitemByUniqueID(uniqueid, xmlworkitem);
	}

	/**
	 * This method expects a form post and processes the WorkItem by the
	 * WorkflowService EJB.
	 * 
	 * The Method returns a JSON object with the new data. If a processException
	 * Occurs the method returns a JSON object with the error code
	 * 
	 * The JSON result is computed by the service because JSON is not standardized
	 * and differs between different jax-rs implementations. For that reason it can
	 * not be directly re-converted XMLItemCollection
	 * 
	 * generated by this method Output format: <code>
	 * ... value":{"@type":"xs:int","$":"10"}
	 * </code>
	 * 
	 * 
	 * @param requestBodyStream
	 *            - form content
	 * @return JSON object
	 * @throws Exception
	 */
	@POST
	@Path("/workitem")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response postJSONWorkitem(InputStream requestBodyStream, @QueryParam("error") String error,
			@QueryParam("encoding") String encoding) {

		logger.fine("postWorkitem_JSON @POST workitem  postWorkitemJSON....");

		// determine encoding from servlet request ....
		if (encoding == null || encoding.isEmpty()) {
			encoding = servletRequest.getCharacterEncoding();
			logger.fine("postJSONWorkitem using request econding=" + encoding);
		} else {
			logger.fine("postJSONWorkitem set econding=" + encoding);
		}

		ItemCollection workitem = null;
		try {
			workitem = JSONParser.parseWorkitem(requestBodyStream, encoding);
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

		return processWorkitem(workitem, null);
	}

	/**
	 * Delegater for PUT postXMLWorkitemByUniqueID
	 * 
	 * @param workitem
	 * @return
	 */
	@PUT
	@Path("/workitem")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response putJSONWorkitem(InputStream requestBodyStream, @QueryParam("error") String error,
			@QueryParam("encoding") String encoding) {

		logger.fine("putJSONWorkitem @PUT /workitem/{uniqueid}  delegate to POST....");
		return postJSONWorkitem(requestBodyStream, error, encoding);
	}

	@POST
	@Path("/workitem/{uniqueid : ([0-9a-f]{8}-.*|[0-9a-f]{11}-.*)}")
	@Consumes({ MediaType.APPLICATION_JSON })
	public Response postJSONWorkitemByUniqueID(@PathParam("uniqueid") String uniqueid, InputStream requestBodyStream,
			@QueryParam("error") String error, @QueryParam("encoding") String encoding) {
		logger.fine("postJSONWorkitemByUniqueID @POST /workitem/" + uniqueid + "....");

		// determine encoding from servlet request ....
		if (encoding == null || encoding.isEmpty()) {
			encoding = servletRequest.getCharacterEncoding();
			logger.fine("postJSONWorkitemByUniqueID using request econding=" + encoding);
		} else {
			logger.fine("postJSONWorkitemByUniqueID set econding=" + encoding);
		}

		ItemCollection workitem = null;
		try {
			workitem = JSONParser.parseWorkitem(requestBodyStream, encoding);
		} catch (ParseException e) {
			logger.severe("postJSONWorkitemByUniqueID wrong json format!");
			e.printStackTrace();
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		} catch (UnsupportedEncodingException e) {
			logger.severe("postJSONWorkitemByUniqueID wrong json format!");
			e.printStackTrace();
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		}
		return processWorkitem(workitem, uniqueid);
	}

	/**
	 * Delegater for PUT postJSONWorkitemByUniqueID
	 * 
	 * @param workitem
	 * @return
	 */
	@PUT
	@Path("/workitem/{uniqueid : ([0-9a-f]{8}-.*|[0-9a-f]{11}-.*)}")
	@Consumes({ MediaType.APPLICATION_JSON })
	public Response putJSONWorkitemByUniqueID(@PathParam("uniqueid") String uniqueid, InputStream requestBodyStream,
			@QueryParam("error") String error, @QueryParam("encoding") String encoding) {

		logger.fine("postJSONWorkitemByUniqueID @PUT /workitem/{uniqueid}  delegate to POST....");
		return postJSONWorkitemByUniqueID(uniqueid, requestBodyStream, error, encoding);
	}

	/**
	 * This method post a collection of ItemCollection objects to be processed by
	 * the WorkflowManager.
	 * 
	 * @param worklist
	 *            - workitem list data
	 */
	@POST
	@Path("/workitems")
	@Consumes({ MediaType.APPLICATION_XML, "text/xml" })
	public Response postWorkitems_XML(XMLDataCollection worklist) {

		logger.fine("postWorkitems_XML @POST /workitems  method:postWorkitemsXML....");

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
	@Consumes({ MediaType.APPLICATION_XML, "text/xml" })
	public Response putWorkitems_XML(XMLDataCollection worklist) {
		logger.fine("pupWorkitems_XML @PUT /workitems  delegate to @POST....");
		return postWorkitems_XML(worklist);
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
					logger.finest("[WorkflowRestService] parse line:" + fieldValue + "");
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
							logger.fine("[WorkflowRestService] no value for '" + fieldName + "'");
						} else {
							fieldValue = fieldValue.substring(fieldValue.indexOf('=') + 1);
							// test for a multiValue field - did we know
							// this
							// field....?
							fieldName = fieldName.toLowerCase();
							if (vMultiValueFieldNames.indexOf(fieldName) > -1) {

								List v = workitem.getItemValue(fieldName);
								v.add(fieldValue);
								logger.fine("[WorkflowRestService] multivalue for '" + fieldName + "' = '" + fieldValue
										+ "'");
								workitem.replaceItemValue(fieldName, v);
							} else {
								// first single value....
								logger.fine(
										"[WorkflowRestService] value for '" + fieldName + "' = '" + fieldValue + "'");
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

	@Deprecated
	@POST
	@Path("/workitem.json/{uniqueid}")
	@Consumes({ MediaType.APPLICATION_JSON })
	public Response postWorkitemByUniqueIDJSONDeprecated(@PathParam("uniqueid") String uniqueid,
			InputStream requestBodyStream, @QueryParam("error") String error, @QueryParam("encoding") String encoding) {
		return postJSONWorkitemByUniqueID(uniqueid, requestBodyStream, error, encoding);
	}

	/**
	 * uri was introduced with 4.0 but should be removed with 4.1
	 * 
	 * @param requestBodyStream
	 * @param error
	 * @param encoding
	 * @return
	 */
	@Deprecated
	@POST
	@Path("/workitem.json")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response postWorkitemJSONDeprecated(InputStream requestBodyStream, @QueryParam("error") String error,
			@QueryParam("encoding") String encoding) {
		return postJSONWorkitem(requestBodyStream, error, encoding);
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
	 * @param uid
	 *            - optional $uniqueid, will be validated.
	 * @return
	 */
	private Response processWorkitem(ItemCollection workitem, String uid) {

		// test for null values
		if (workitem == null) {
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		}

		// validate optional uniqueId
		if (uid != null && !uid.equals(workitem.getUniqueID())) {
			logger.severe("@POST/@PUT workitem/" + uid
					+ " : $UNIQUEID did not match, remove $uniqueid to create a new instnace!");
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
			workitem = this.addErrorMessage(e, workitem);
		} catch (PluginException e) {
			workitem = this.addErrorMessage(e, workitem);
		} catch (RuntimeException e) {
			workitem = this.addErrorMessage(e, workitem);
		} catch (ModelException e) {
			workitem = this.addErrorMessage(e, workitem);
		}

		// return workitem
		try {
			if (workitem.hasItem("$error_code"))
				return Response.ok(XMLDataCollectionAdapter.getDataCollection(workitem))
						.status(Response.Status.NOT_ACCEPTABLE).build();
			else
				return Response.ok(XMLDataCollectionAdapter.getDataCollection(workitem)).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		}

	}

	/**
	 * This helper method adds a error message to the given workItem, based on the
	 * data in a WorkflowException. This kind of error message can be displayed in a
	 * page evaluating the properties '$error_code' and '$error_message'. These
	 * attributes will not be stored.
	 * 
	 * 
	 * If a PluginException or ValidationException contains an optional object array
	 * the message is parsed for params to be replaced
	 * 
	 * Example:
	 * 
	 * <code>
	 * $error_message=Value should not be greater than {0} or lower as {1}.
	 * </code>
	 * 
	 * @param pe
	 */
	private ItemCollection addErrorMessage(Exception pe, ItemCollection aworkitem) {

		if (pe instanceof RuntimeException && pe.getCause() != null) {
			pe = (RuntimeException) pe.getCause();
		}

		if (pe instanceof WorkflowException) {
			String message = ((WorkflowException) pe).getErrorCode();

			// parse message for params
			if (pe instanceof PluginException) {
				PluginException p = (PluginException) pe;
				if (p.getErrorParameters() != null && p.getErrorParameters().length > 0) {
					for (int i = 0; i < p.getErrorParameters().length; i++) {
						message = message.replace("{" + i + "}", p.getErrorParameters()[i].toString());
					}
				}
			}
			aworkitem.replaceItemValue("$error_code", ((WorkflowException) pe).getErrorCode());
			aworkitem.replaceItemValue("$error_message", message);
		} else if (pe instanceof InvalidAccessException) {
			aworkitem.replaceItemValue("$error_code", ((InvalidAccessException) pe).getErrorCode());
			aworkitem.replaceItemValue("$error_message", pe.getMessage());
		} else {
			aworkitem.replaceItemValue("$error_code", "INTERNAL ERROR");
			aworkitem.replaceItemValue("$error_message", pe.getMessage());
		}

		return aworkitem;
	}

}
