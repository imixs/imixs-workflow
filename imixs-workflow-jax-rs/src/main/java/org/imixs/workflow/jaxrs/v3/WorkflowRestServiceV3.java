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

package org.imixs.workflow.jaxrs.v3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Stateless;
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

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.imixs.workflow.exceptions.WorkflowException;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.util.JSONParser;

/**
 * The WorkflowService Handler supports methods to process different kind of
 * request URIs
 * 
 * @author rsoika
 * 
 */
@Deprecated
@Path("/v3/workflow")
@Produces({ MediaType.TEXT_HTML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.TEXT_XML })
@Stateless
public class WorkflowRestServiceV3 {

	@EJB
	private WorkflowService workflowService;
	
	@EJB
	private EntityService entityService;

	@javax.ws.rs.core.Context
	private static HttpServletRequest servletRequest;

	private static Logger logger = Logger.getLogger(WorkflowRestServiceV3.class.getName());

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
				out.write("<h1>Imixs-Workflow REST Service</h1>".getBytes());
				out.write(
						"<p>See the <a href=\"http://www.imixs.org/xml/restservice/workflowservice.html\" target=\"_blank\">Imixs REST Service API</a> for more information about this Service.</p>"
								.getBytes());

				// end
				out.write("</body></html>".getBytes());
			}
		};

	}

	/**
	 * returns a singel workitem defined by $uniqueid
	 * 
	 * @param uniqueid
	 * @return
	 */
	@GET
	@Path("/workitem/{uniqueid}")
	public XMLItemCollection getWorkItem(@PathParam("uniqueid") String uniqueid, @QueryParam("items") String items) {
	
		ItemCollection workitem;
		try {
			workitem = workflowService.getWorkItem(uniqueid);
			return XMLItemCollectionAdapter.putItemCollection(workitem, EntityRestServiceV3.getItemList(items));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Returns a file attachment located in the property $file of the specified
	 * workitem
	 * 
	 * The file name will be encoded. With a URLDecode the filename is decoded
	 * in different formats and searched in the file list. This is not a nice
	 * solution.
	 * 
	 * @param uniqueid
	 * @return
	 */
	@SuppressWarnings({ "rawtypes" })
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
	
				// fetch $file from hashmap....
				Map mapFiles = workItem.getFiles();
				if (mapFiles != null) {
					Object fileInfoObject = null;
					// try to guess encodings.....
					fileInfoObject = mapFiles.get(fileNameUTF8);
					if (fileInfoObject == null)
						fileInfoObject = mapFiles.get(fileNameISO);
					if (fileInfoObject == null)
						fileInfoObject = mapFiles.get(file);
	
					if (fileInfoObject != null) {
						String sContentType = null;
						byte[] fileContent = null;
						// fileInfoObject can be a List or a an Array
						if (fileInfoObject instanceof List) {
							sContentType = ((List) fileInfoObject).get(0).toString();
							fileContent = (byte[]) ((List) fileInfoObject).get(1);
						} else {
							// seems to be an array...
							sContentType = ((Object[]) fileInfoObject)[0].toString();
							fileContent = (byte[]) ((Object[]) fileInfoObject)[1];
						}
	
						// Set content type in order of the contentType stored
						// in the $file attribute
						Response.ResponseBuilder builder = Response.ok(fileContent, sContentType);
	
						return builder.build();
	
					} else {
						logger.warning("WorklfowRestService unable to open file: '" + file + "' in workitem '"
								+ uniqueid + "' - error: Filename not found!");
	
						// workitem not found
						return Response.status(Response.Status.NOT_FOUND).build();
					}
				} else {
					logger.warning("WorklfowRestService unable to open file: '" + file + "' in workitem '" + uniqueid
							+ "' - error: No files available!");
	
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
	public EntityCollection getEvents(@PathParam("uniqueid") String uniqueid) {
		Collection<ItemCollection> eventList = null;
		try {
			eventList = workflowService.getEvents(entityService._load(uniqueid));
			return XMLItemCollectionAdapter.putCollection(eventList);
	
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new EntityCollection();
	}

	/**
	 * Returns a collection of workitems representing the worklist by the
	 * current user
	 * 
	 * @param start
	 * @param count
	 * @param type
	 * @param sortorder
	 */
	@GET
	@Path("/worklist")
	public EntityCollection getWorkList(@DefaultValue("0") @QueryParam("start") int start,
			@DefaultValue("10") @QueryParam("count") int count, @QueryParam("type") String type,
			@DefaultValue("0") @QueryParam("sortorder") int sortorder, @QueryParam("items") String items) {
		
		return getTaskListByOwner(null, start, count, type, sortorder, items);
	}

	@GET
	@Path("/tasklist/owner/{owner}")
	public EntityCollection getTaskListByOwner(@PathParam("owner") String owner,
			@DefaultValue("0") @QueryParam("start") int start, @DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("type") String type, @DefaultValue("0") @QueryParam("sortorder") int sortorder,
			@QueryParam("items") String items) {
		Collection<ItemCollection> col = null;
		try {
			if ("null".equalsIgnoreCase(owner))
				owner = null;
	
			// decode URL param
			if (owner != null)
				owner = URLDecoder.decode(owner, "UTF-8");
	
			col = workflowService.getWorkListByOwner(owner,type,count, start, null,false);
		
			return XMLItemCollectionAdapter.putCollection(col, EntityRestServiceV3.getItemList(items));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new EntityCollection();
	}

	/**
	 * Returns a collection of workitems representing the worklist by the
	 * current user
	 * 
	 * @param start
	 * @param count
	 * @param type
	 * @param sortorder
	 */
	@GET
	@Path("/tasklist/author/{user}")
	public EntityCollection getTaskListByAuthor(@PathParam("user") String user,
			@DefaultValue("0") @QueryParam("start") int start, @DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("type") String type, @DefaultValue("0") @QueryParam("sortorder") int sortorder,
			@QueryParam("items") String items) {
		Collection<ItemCollection> col = null;
		try {
			if ("null".equalsIgnoreCase(user))
				user = null;

			// decode URL param
			if (user != null)
				user = URLDecoder.decode(user, "UTF-8");

			col = workflowService.getWorkListByAuthor(user,type, count,start, null,false);
			
						
			return XMLItemCollectionAdapter.putCollection(col, EntityRestServiceV3.getItemList(items));

		} catch (Exception e) {
			e.printStackTrace();
		}
		return new EntityCollection();
	}


	@GET
	@Path("/tasklist/creator/{creator}")
	public EntityCollection getTaskListByCreator(@PathParam("creator") String creator,
			@DefaultValue("0") @QueryParam("start") int start, @DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("type") String type, @DefaultValue("0") @QueryParam("sortorder") int sortorder,
			@QueryParam("items") String items) {
		Collection<ItemCollection> col = null;
		try {
			if ("null".equalsIgnoreCase(creator))
				creator = null;

			// decode URL param
			if (creator != null)
				creator = URLDecoder.decode(creator, "UTF-8");

			
			col = workflowService.getWorkListByCreator(creator,type,  count, start, null,false);
			return XMLItemCollectionAdapter.putCollection(col, EntityRestServiceV3.getItemList(items));

		} catch (Exception e) {
			e.printStackTrace();
		}
		return new EntityCollection();
	}



	@GET
	@Path("/tasklist/processid/{processid}")
	public EntityCollection getTaskListByProcessID(@PathParam("processid") int processid,
			@DefaultValue("0") @QueryParam("start") int start, @DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("type") String type, @DefaultValue("0") @QueryParam("sortorder") int sortorder,
			@QueryParam("items") String items) {
		Collection<ItemCollection> col = null;
		try {
			col = workflowService.getWorkListByProcessID(processid, type,count,start, null,false);
						return XMLItemCollectionAdapter.putCollection(col, EntityRestServiceV3.getItemList(items));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new EntityCollection();
	}


	@GET
	@Path("/tasklist/group/{processgroup}")
	public EntityCollection getTaskListByGroup(@PathParam("processgroup") String processgroup,
			@DefaultValue("0") @QueryParam("start") int start, @DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("type") String type, @DefaultValue("0") @QueryParam("sortorder") int sortorder,
			@QueryParam("items") String items) {
		Collection<ItemCollection> col = null;
		try {

			// decode URL param
			if (processgroup != null)
				processgroup = URLDecoder.decode(processgroup, "UTF-8");

			col = workflowService.getWorkListByGroup(processgroup, type,count,start, null,false);
						return XMLItemCollectionAdapter.putCollection(col, EntityRestServiceV3.getItemList(items));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new EntityCollection();
	}


	@GET
	@Path("/tasklist/ref/{uniqueid}")
	public EntityCollection getTaskListByRef(@PathParam("uniqueid") String uniqueid,
			@DefaultValue("0") @QueryParam("start") int start, @DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("type") String type, @DefaultValue("0") @QueryParam("sortorder") int sortorder,
			@QueryParam("items") String items) {
		Collection<ItemCollection> col = null;
		try {
			col = workflowService.getWorkListByRef(uniqueid,type,count, start, null,false);
						
			return XMLItemCollectionAdapter.putCollection(col, EntityRestServiceV3.getItemList(items));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new EntityCollection();
	}

	

	

	/**
	 * This method expects a form post and processes the WorkItem by the
	 * WorkflowService EJB. After the workItem was processed the method redirect
	 * the request to the provided action URI. The action URI can also be
	 * computed by the Imixs Workflow ResutlPlugin
	 * 
	 * @param requestBodyStream
	 *            - form content
	 * @param action
	 *            - return URI
	 * @return
	 */
	@POST
	@Path("/workitem")
	@Consumes({ "application/x-www-form-urlencoded" })
	@Produces(MediaType.APPLICATION_XML)
	public Response putWorkitem(InputStream requestBodyStream) {

		logger.fine("[WorkflowRestService] @POST /workitem  method:postWorkitem....");
		// parse the workItem.
		ItemCollection workitem = parseWorkitem(requestBodyStream);

		if (workitem == null) {
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		}

		try {
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
				return Response.ok(XMLItemCollectionAdapter.putItemCollection(workitem), MediaType.APPLICATION_XML)
						.status(Response.Status.NOT_ACCEPTABLE).build();
			else
				return Response.ok(XMLItemCollectionAdapter.putItemCollection(workitem), MediaType.APPLICATION_XML)
						.build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		}

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
	@Consumes({ "application/x-www-form-urlencoded" })
	@Produces(MediaType.APPLICATION_XML)
	public Response postWorkitem(InputStream requestBodyStream) {
		logger.fine("[WorkflowRestService] @POST /workitem  delegate to POST....");

		return putWorkitem(requestBodyStream);
	}

	/**
	 * This method post a ItemCollection object to be processed by the
	 * WorkflowManager. The method test for the properties $processid and
	 * $activityid
	 * 
	 * NOTE!! - this method did not update an existing instance of a workItem.
	 * The behavior is different to the method putWorkitem(). It need to be
	 * discussed if the behavior is wrong or not.
	 * 
	 * @param workitem
	 *            - new workItem data
	 */
	@POST
	@Path("/workitem")
	@Produces(MediaType.APPLICATION_XML)
	@Consumes({ MediaType.APPLICATION_XML, "text/xml" })
	public Response postWorkitemXML(XMLItemCollection xmlworkitem) {

		logger.fine("[WorkflowRestService] @POST /workitem  method:postWorkitemXML....");

		ItemCollection workitem;
		workitem = XMLItemCollectionAdapter.getItemCollection(xmlworkitem);

		if (workitem == null) {
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		}

		try {
			workitem.removeItem("$error_code");
			workitem.removeItem("$error_message");
			// now lets try to process the workitem...
			workitem = workflowService.processWorkItem(workitem);

		} catch (AccessDeniedException e) {
			logger.severe(e.getMessage());
			workitem = this.addErrorMessage(e, workitem);
		} catch (PluginException e) {
			logger.severe(e.getMessage());
			workitem = this.addErrorMessage(e, workitem);
		} catch (RuntimeException e) {
			logger.severe(e.getMessage());
			workitem = this.addErrorMessage(e, workitem);
		} catch (ModelException e) {
			workitem = this.addErrorMessage(e, workitem);
		} 

		// return workitem
		try {
			if (workitem.hasItem("$error_code"))
				return Response.ok(XMLItemCollectionAdapter.putItemCollection(workitem), MediaType.APPLICATION_XML)
						.status(Response.Status.NOT_ACCEPTABLE).build();
			else
				return Response.ok(XMLItemCollectionAdapter.putItemCollection(workitem), MediaType.APPLICATION_XML)
						.build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		}

	}
	
	@POST
	@Path("/workitem/{uniqueid}")
	@Produces(MediaType.APPLICATION_XML)
	@Consumes({ MediaType.APPLICATION_XML, "text/xml" })
	public Response postWorkitemByUniqueIDXML(@PathParam("uniqueid") String uniqueid,XMLItemCollection xmlworkitem) {
		logger.fine("[WorkflowRestService] @POST /workitem/" + uniqueid+"  method:postWorkitemXML....");
		ItemCollection workitem;
		workitem = XMLItemCollectionAdapter.getItemCollection(xmlworkitem);

		if (workitem != null && !uniqueid.equals(workitem.getUniqueID())) {
			logger.warning("@POST /workitem/" + uniqueid+"  $UNIQUEID did not match!" );
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		}		
		return postWorkitemXML(xmlworkitem);

	}

		
		
	@PUT
	@Path("/workitem")
	@Produces(MediaType.APPLICATION_XML)
	@Consumes({ MediaType.APPLICATION_XML, "text/xml" })
	public Response putWorkitemXML(XMLItemCollection workitem) {
		logger.fine("[WorkflowRestService] @PUT /workitem  delegate to POST....");
		return postWorkitemXML(workitem);
	}

	/**
	 * This method expects a form post and processes the WorkItem by the
	 * WorkflowService EJB.
	 * 
	 * The Method returns a JSON object with the new data. If a processException
	 * Occurs the method returns a JSON object with the error code
	 * 
	 * The JSON result is computed by the service because JSON is not
	 * standardized and differs between different jax-rs implementations. For
	 * that reason it can not be directly re-converted XMLItemCollection
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
	@Path("/workitem.json")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response postWorkitemJSON(InputStream requestBodyStream, @QueryParam("error") String error,
			@QueryParam("encoding") String encoding) {

		logger.fine("[WorkflowRestService] @POST workitem  postWorkitemJSON....");

		// determine encoding from servlet request ....
		if (encoding == null || encoding.isEmpty()) {
			encoding = servletRequest.getCharacterEncoding();
			logger.fine("[WorkflowRestService] postWorkitemJSON using request econding=" + encoding);
		} else {
			logger.fine("[WorkflowRestService] postWorkitemJSON set econding=" + encoding);
		}
		// set defautl encoding UTF-8
		if (encoding == null || encoding.isEmpty()) {
			encoding = "UTF-8";
			logger.fine(
					"[WorkflowRestService] postWorkitemJSON no encoding defined, set default econding to" + encoding);
		}

		ItemCollection workitem = null;
		try {
			workitem = JSONParser.parseWorkitem(requestBodyStream, encoding);

		} catch (ParseException e) {
			logger.severe("postWorkitemJSON wrong json format!");
			e.printStackTrace();
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		} catch (UnsupportedEncodingException e) {
			logger.severe("postWorkitemJSON wrong json format!");
			e.printStackTrace();
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		}

		if (workitem == null) {
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		}

		try {
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
				return Response.ok(XMLItemCollectionAdapter.putItemCollection(workitem), MediaType.APPLICATION_JSON)
						.status(Response.Status.NOT_ACCEPTABLE).build();
			else
				return Response.ok(XMLItemCollectionAdapter.putItemCollection(workitem), MediaType.APPLICATION_JSON)
						.build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		}

	}
	
	
	
	@POST
	@Path("/workitem.json/{uniqueid}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes({ MediaType.APPLICATION_JSON })
	public Response postWorkitemByUniqueIDJSON(@PathParam("uniqueid") String uniqueid,InputStream requestBodyStream
			, @QueryParam("error") String error,
			@QueryParam("encoding") String encoding) {
		logger.fine("[WorkflowRestService] @POST /workitem/" + uniqueid+"  method:postWorkitemXML....");
		ItemCollection workitem=null;
		try {
			workitem = JSONParser.parseWorkitem(requestBodyStream, encoding);
		} catch (UnsupportedEncodingException | ParseException e) {
			logger.warning(e.getMessage());
		}
		
		if (workitem != null && !uniqueid.equals(workitem.getUniqueID())) {
			logger.warning("@POST /workitem/" + uniqueid+"  $UNIQUEID did not match!" );
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		}		
		return postWorkitemJSON(requestBodyStream, error, encoding);

	}
	
	

	/**
	 * This method post a collection of ItemCollection objects to be processed
	 * by the WorkflowManager.
	 * 
	 * @param worklist
	 *            - workitem list data
	 */
	@POST
	@Path("/workitems")
	@Consumes({ MediaType.APPLICATION_XML, "text/xml" })
	public Response putWorkitemsXML(EntityCollection worklist) {

		logger.fine("[WorkflowRestService] @POST /workitems  method:postWorkitemsXML....");

		XMLItemCollection entity;
		ItemCollection itemCollection;
		try {
			// save new entities into database and update modelversion.....
			for (int i = 0; i < worklist.getEntity().length; i++) {
				entity = worklist.getEntity()[i];
				itemCollection = XMLItemCollectionAdapter.getItemCollection(entity);
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
	public Response postWorkitemsXML(EntityCollection worklist) {
		logger.fine("[WorkflowRestService] @PUT /workitems  delegate to @POST....");
		return putWorkitemsXML(worklist);
	}

	/**
	 * This method expects a form post. The method parses the input stream to
	 * extract the provides field/value pairs. NOTE: The method did not(!)
	 * assume that the put/post request contains a complete workItem. For this
	 * reason the method loads the existing instance of the corresponding
	 * workItem (identified by the $uniqueid) and adds the values provided by
	 * the put/post request into the existing instance.
	 * 
	 * The following kind of lines which can be included in the InputStream will
	 * be skipped
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
	public static ItemCollection parseWorkitem(InputStream requestBodyStream) {
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

	/**
	 * This helper method adds a error message to the given workItem, based on
	 * the data in a WorkflowException. This kind of error message can be
	 * displayed in a page evaluating the properties '$error_code' and
	 * '$error_message'. These attributes will not be stored.
	 * 
	 * 
	 * If a PluginException or ValidationException contains an optional object
	 * array the message is parsed for params to be replaced
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
		} else if (pe instanceof AccessDeniedException) {
			aworkitem.replaceItemValue("$error_code", ((AccessDeniedException) pe).getErrorCode());
			aworkitem.replaceItemValue("$error_message", pe.getMessage());
		} else if (pe instanceof ProcessingErrorException) {
			aworkitem.replaceItemValue("$error_code", ((ProcessingErrorException) pe).getErrorCode());
			aworkitem.replaceItemValue("$error_message", pe.getMessage());
		}

		return aworkitem;
	}

	
	/**
	 * generates a sort order clause depending on a sororder id
	 * 
	 * @param asortorder
	 * @return
	 */
	private String createSortOrderClause(int asortorder) {
		switch (asortorder) {

		case WorkflowService.SORT_ORDER_CREATED_ASC: {
			return " ORDER BY wi.created asc";
		}
		case WorkflowService.SORT_ORDER_MODIFIED_ASC: {
			return " ORDER BY wi.modified asc";
		}
		case WorkflowService.SORT_ORDER_MODIFIED_DESC: {
			return " ORDER BY wi.modified desc";
		}
		default:
			return " ORDER BY wi.created desc";
		}

	}
}
