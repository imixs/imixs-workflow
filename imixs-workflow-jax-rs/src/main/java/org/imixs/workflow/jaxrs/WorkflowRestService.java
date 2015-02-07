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
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.imixs.workflow.jee.ejb.WorkflowService;
import org.imixs.workflow.util.JSONParser;
import org.imixs.workflow.xml.EntityCollection;
import org.imixs.workflow.xml.XMLItem;
import org.imixs.workflow.xml.XMLItemCollection;
import org.imixs.workflow.xml.XMLItemCollectionAdapter;

/**
 * The WorkflowService Handler supports methods to process different kind of
 * request URIs
 * 
 * @author rsoika
 * 
 */
@Path("/workflow")
@Produces({ "text/html", "application/xml", "application/json" })
@Stateless
public class WorkflowRestService {

	@EJB
	private WorkflowService workflowService;

	@javax.ws.rs.core.Context
	private static HttpServletRequest servletRequest;

	private static Logger logger = Logger.getLogger(WorkflowRestService.class
			.getName());

	@GET
	@Produces("text/html")
	@Path("/help")
	public StreamingOutput getHelpHTML() {

		return new StreamingOutput() {
			public void write(OutputStream out) throws IOException,
					WebApplicationException {

				out.write("<html><head>".getBytes());
				out.write("<style>".getBytes());
				out.write("table {padding:0px;width: 100%;margin-left: -2px;margin-right: -2px;}"
						.getBytes());
				out.write("body,td,select,input,li {font-family: Verdana, Helvetica, Arial, sans-serif;font-size: 13px;}"
						.getBytes());
				out.write("table th {color: white;background-color: #bbb;text-align: left;font-weight: bold;}"
						.getBytes());

				out.write("table th,table td {font-size: 12px;}".getBytes());

				out.write("table tr.a {background-color: #ddd;}".getBytes());

				out.write("table tr.b {background-color: #eee;}".getBytes());

				out.write("</style>".getBytes());
				out.write("</head><body>".getBytes());

				// body
				out.write("<h1>Imixs Workflow REST Service</h1>".getBytes());
				out.write("<p>Read the Imixs REST Service <a href=\"http://doc.imixs.org/xml/restservice.html\">Online Help</a> for a detailed description of this Service.</p>"
						.getBytes());
				out.write("<p>See the <a href=\"http://www.imixs.org\">Imixs Workflow Project Site</a> for general informations.</p>"
						.getBytes());

				// end
				out.write("</body></html>".getBytes());
			}
		};

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
	public EntityCollection getWorkList(
			@DefaultValue("0") @QueryParam("start") int start,
			@DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("type") String type,
			@DefaultValue("0") @QueryParam("sortorder") int sortorder,
			@QueryParam("items") String items) {
		Collection<ItemCollection> col = null;
		try {
			col = workflowService.getWorkList(start, count, type, sortorder);
			return XMLItemCollectionAdapter.putCollection(col,
					getItemList(items));

		} catch (Exception e) {
			e.printStackTrace();
		}
		return new EntityCollection();
	}

	/**
	 * 
	 * @param start
	 * @param count
	 * @param type
	 * @param sortorder
	 * @return
	 */
	@GET
	@Path("/worklist.xml")
	@Produces("application/xml")
	public EntityCollection getWorkListXML(
			@DefaultValue("0") @QueryParam("start") int start,
			@DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("type") String type,
			@DefaultValue("0") @QueryParam("sortorder") int sortorder,
			@QueryParam("items") String items) {
		return getWorkList(start, count, type, sortorder, items);
	}

	@GET
	@Path("/worklist.json")
	@Produces("application/json")
	public EntityCollection getWorkListJSON(
			@DefaultValue("0") @QueryParam("start") int start,
			@DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("type") String type,
			@DefaultValue("0") @QueryParam("sortorder") int sortorder,
			@QueryParam("items") String items) {
		return getWorkList(start, count, type, sortorder, items);
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
	@Path("/worklistbyauthor/{user}")
	public EntityCollection getWorkListByAuthor(@PathParam("user") String user,
			@DefaultValue("0") @QueryParam("start") int start,
			@DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("type") String type,
			@DefaultValue("0") @QueryParam("sortorder") int sortorder,
			@QueryParam("items") String items) {
		Collection<ItemCollection> col = null;
		try {
			if ("null".equalsIgnoreCase(user))
				user = null;

			// decode URL param
			if (user != null)
				user = URLDecoder.decode(user, "UTF-8");

			col = workflowService.getWorkListByAuthor(user, start, count, type,
					sortorder);
			return XMLItemCollectionAdapter.putCollection(col,
					getItemList(items));

		} catch (Exception e) {
			e.printStackTrace();
		}
		return new EntityCollection();
	}

	/**
	 * 
	 * @param start
	 * @param count
	 * @param type
	 * @param sortorder
	 * @return
	 */
	@GET
	@Path("/worklistbyauthor/{user}.xml")
	@Produces("application/xml")
	public EntityCollection getWorkListByAuthorXML(
			@PathParam("user") String user,
			@DefaultValue("0") @QueryParam("start") int start,
			@DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("type") String type,
			@DefaultValue("0") @QueryParam("sortorder") int sortorder,
			@QueryParam("items") String items) {
		return getWorkListByAuthor(user, start, count, type, sortorder, items);
	}

	@GET
	@Path("/worklistbyauthor/{user}.json")
	@Produces("application/json")
	public EntityCollection getWorkListByAuthorJSON(
			@PathParam("user") String user,
			@DefaultValue("0") @QueryParam("start") int start,
			@DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("type") String type,
			@DefaultValue("0") @QueryParam("sortorder") int sortorder,
			@QueryParam("items") String items) {
		return getWorkListByAuthor(user, start, count, type, sortorder, items);
	}

	@GET
	@Path("/worklistbycreator/{creator}")
	public EntityCollection getWorkListByCreator(
			@PathParam("creator") String creator,
			@DefaultValue("0") @QueryParam("start") int start,
			@DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("type") String type,
			@DefaultValue("0") @QueryParam("sortorder") int sortorder,
			@QueryParam("items") String items) {
		Collection<ItemCollection> col = null;
		try {
			if ("null".equalsIgnoreCase(creator))
				creator = null;

			// decode URL param
			if (creator != null)
				creator = URLDecoder.decode(creator, "UTF-8");

			col = workflowService.getWorkListByCreator(creator, start, count,
					type, sortorder);
			return XMLItemCollectionAdapter.putCollection(col,
					getItemList(items));

		} catch (Exception e) {
			e.printStackTrace();
		}
		return new EntityCollection();
	}

	@GET
	@Path("/worklistbycreator/{creator}.xml")
	@Produces("application/xml")
	public EntityCollection getWorkListByCreatorXML(
			@PathParam("creator") String creator,
			@DefaultValue("0") @QueryParam("start") int start,
			@DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("type") String type,
			@DefaultValue("0") @QueryParam("sortorder") int sortorder,
			@QueryParam("items") String items) {
		return getWorkListByCreator(creator, start, count, type, sortorder,
				items);
	}

	@GET
	@Path("/worklistbycreator/{creator}.json")
	@Produces("application/json")
	public EntityCollection getWorkListByCreatorJSON(
			@PathParam("creator") String creator,
			@DefaultValue("0") @QueryParam("start") int start,
			@DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("type") String type,
			@DefaultValue("0") @QueryParam("sortorder") int sortorder,
			@QueryParam("items") String items) {
		return getWorkListByCreator(creator, start, count, type, sortorder,
				items);
	}

	@GET
	@Path("/worklistbyprocessid/{processid}")
	public EntityCollection getWorkListByProcessID(
			@PathParam("processid") int processid,
			@DefaultValue("0") @QueryParam("start") int start,
			@DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("type") String type,
			@DefaultValue("0") @QueryParam("sortorder") int sortorder,
			@QueryParam("items") String items) {
		Collection<ItemCollection> col = null;
		try {
			col = workflowService.getWorkListByProcessID(processid, start,
					count, type, sortorder);
			return XMLItemCollectionAdapter.putCollection(col,
					getItemList(items));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new EntityCollection();
	}

	@GET
	@Path("/worklistbyprocessid/{processid}.xml")
	@Produces("application/xml")
	public EntityCollection getWorkListByProcessIDXML(
			@PathParam("processid") int processid,
			@DefaultValue("0") @QueryParam("start") int start,
			@DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("type") String type,
			@DefaultValue("0") @QueryParam("sortorder") int sortorder,
			@QueryParam("items") String items) {
		return getWorkListByProcessID(processid, start, count, type, sortorder,
				items);
	}

	@GET
	@Path("/worklistbyprocessid/{processid}.json")
	@Produces("application/json")
	public EntityCollection getWorkListByProcessIDJSON(
			@PathParam("processid") int processid,
			@DefaultValue("0") @QueryParam("start") int start,
			@DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("type") String type,
			@DefaultValue("0") @QueryParam("sortorder") int sortorder,
			@QueryParam("items") String items) {
		return getWorkListByProcessID(processid, start, count, type, sortorder,
				items);
	}

	@GET
	@Path("/worklistbygroup/{processgroup}")
	public EntityCollection getWorkListByGroup(
			@PathParam("processgroup") String processgroup,
			@DefaultValue("0") @QueryParam("start") int start,
			@DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("type") String type,
			@DefaultValue("0") @QueryParam("sortorder") int sortorder,
			@QueryParam("items") String items) {
		Collection<ItemCollection> col = null;
		try {

			// decode URL param
			if (processgroup != null)
				processgroup = URLDecoder.decode(processgroup, "UTF-8");

			col = workflowService.getWorkListByGroup(processgroup, start,
					count, type, sortorder);
			return XMLItemCollectionAdapter.putCollection(col,
					getItemList(items));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new EntityCollection();
	}

	@GET
	@Path("/worklistbygroup/{processgroup}.xml")
	@Produces("application/xml")
	public EntityCollection getWorkListByGroupXML(
			@PathParam("processgroup") String processgroup,
			@DefaultValue("0") @QueryParam("start") int start,
			@DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("type") String type,
			@DefaultValue("0") @QueryParam("sortorder") int sortorder,
			@QueryParam("items") String items) {
		return getWorkListByGroup(processgroup, start, count, type, sortorder,
				items);
	}

	@GET
	@Path("/worklistbygroup/{processgroup}.json")
	@Produces("application/json")
	public EntityCollection getWorkListByGroupJSON(
			@PathParam("processgroup") String processgroup,
			@DefaultValue("0") @QueryParam("start") int start,
			@DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("type") String type,
			@DefaultValue("0") @QueryParam("sortorder") int sortorder,
			@QueryParam("items") String items) {
		return getWorkListByGroup(processgroup, start, count, type, sortorder,
				items);
	}

	@GET
	@Path("/worklistbyowner/{owner}")
	public EntityCollection getWorkListByOwner(
			@PathParam("owner") String owner,
			@DefaultValue("0") @QueryParam("start") int start,
			@DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("type") String type,
			@DefaultValue("0") @QueryParam("sortorder") int sortorder,
			@QueryParam("items") String items) {
		Collection<ItemCollection> col = null;
		try {
			if ("null".equalsIgnoreCase(owner))
				owner = null;

			// decode URL param
			if (owner != null)
				owner = URLDecoder.decode(owner, "UTF-8");

			col = workflowService.getWorkListByOwner(owner, start, count, type,
					sortorder);
			return XMLItemCollectionAdapter.putCollection(col,
					getItemList(items));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new EntityCollection();
	}

	@GET
	@Path("/worklistbyowner/{owner}.xml")
	@Produces("application/xml")
	public EntityCollection getWorkListByOwnerXML(
			@PathParam("owner") String owner,
			@DefaultValue("0") @QueryParam("start") int start,
			@DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("type") String type,
			@DefaultValue("0") @QueryParam("sortorder") int sortorder,
			@QueryParam("items") String items) {
		return getWorkListByOwner(owner, start, count, type, sortorder, items);
	}

	@GET
	@Path("/worklistbyowner/{owner}.json")
	@Produces("application/json")
	public EntityCollection getWorkListByOwnerJSON(
			@PathParam("owner") String owner,
			@DefaultValue("0") @QueryParam("start") int start,
			@DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("type") String type,
			@DefaultValue("0") @QueryParam("sortorder") int sortorder,
			@QueryParam("items") String items) {
		return getWorkListByOwner(owner, start, count, type, sortorder, items);
	}

	@GET
	@Path("/worklistbywriteaccess")
	public EntityCollection getWorkListByWriteAccess(
			@DefaultValue("0") @QueryParam("start") int start,
			@DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("type") String type,
			@DefaultValue("0") @QueryParam("sortorder") int sortorder,
			@QueryParam("items") String items) {
		Collection<ItemCollection> col = null;
		try {
			col = workflowService.getWorkListByWriteAccess(start, count, type,
					sortorder);
			return XMLItemCollectionAdapter.putCollection(col,
					getItemList(items));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new EntityCollection();
	}

	@GET
	@Path("/worklistbywriteaccess.xml")
	@Produces("application/xml")
	public EntityCollection getWorkListByWriteAccessXML(
			@DefaultValue("0") @QueryParam("start") int start,
			@DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("type") String type,
			@DefaultValue("0") @QueryParam("sortorder") int sortorder,
			@QueryParam("items") String items) {

		return getWorkListByWriteAccess(start, count, type, sortorder, items);
	}

	@GET
	@Path("/worklistbywriteaccess.json")
	@Produces("application/json")
	public EntityCollection getWorkListByWriteAccessJSON(
			@DefaultValue("0") @QueryParam("start") int start,
			@DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("type") String type,
			@DefaultValue("0") @QueryParam("sortorder") int sortorder,
			@QueryParam("items") String items) {

		return getWorkListByWriteAccess(start, count, type, sortorder, items);
	}

	@GET
	@Path("/worklistbyref/{uniqueid}")
	public EntityCollection getWorkListByRef(
			@PathParam("uniqueid") String uniqueid,
			@DefaultValue("0") @QueryParam("start") int start,
			@DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("type") String type,
			@DefaultValue("0") @QueryParam("sortorder") int sortorder,
			@QueryParam("items") String items) {
		Collection<ItemCollection> col = null;
		try {
			col = workflowService.getWorkListByRef(uniqueid, start, count,
					type, sortorder);
			return XMLItemCollectionAdapter.putCollection(col,
					getItemList(items));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new EntityCollection();
	}

	@GET
	@Path("/worklistbyref/{uniqueid}.xml")
	@Produces("application/xml")
	public EntityCollection getWorkListByRefXML(
			@PathParam("uniqueid") String uniqueid,
			@DefaultValue("0") @QueryParam("start") int start,
			@DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("type") String type,
			@DefaultValue("0") @QueryParam("sortorder") int sortorder,
			@QueryParam("items") String items) {
		return getWorkListByRef(uniqueid, start, count, type, sortorder, items);
	}

	@GET
	@Path("/worklistbyref/{uniqueid}.json")
	@Produces("application/json")
	public EntityCollection getWorkListByRefJSON(
			@PathParam("uniqueid") String uniqueid,
			@DefaultValue("0") @QueryParam("start") int start,
			@DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("type") String type,
			@DefaultValue("0") @QueryParam("sortorder") int sortorder,
			@QueryParam("items") String items) {
		return getWorkListByRef(uniqueid, start, count, type, sortorder, items);
	}

	/**
	 * Returns a result set by JPQL Query
	 * 
	 * @param query
	 * @param start
	 * @param count
	 * @param type
	 * @param sortorder
	 * @param items
	 * @return
	 */
	@GET
	@Path("/worklistbyquery/{query}")
	public EntityCollection getWorkListByQuery(
			@PathParam("query") String query,
			@DefaultValue("0") @QueryParam("start") int start,
			@DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("items") String items) {
		Collection<ItemCollection> col = null;
		try {

			// decode query...
			String decodedQuery = URLDecoder.decode(query, "UTF-8");

			col = workflowService.getEntityService().findAllEntities(
					decodedQuery, start, count);
			return XMLItemCollectionAdapter.putCollection(col,
					getItemList(items));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new EntityCollection();
	}

	@GET
	@Path("/worklistbyquery/{query}.xml")
	@Produces("application/xml")
	public EntityCollection getWorkListByQueryXML(
			@PathParam("query") String query,
			@DefaultValue("0") @QueryParam("start") int start,
			@DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("items") String items) {
		return getWorkListByQuery(query, start, count, items);
	}

	@GET
	@Path("/worklistbyquery/{query}.json")
	@Produces("application/json")
	public EntityCollection getWorkListByQueryJSON(
			@PathParam("query") String query,
			@DefaultValue("0") @QueryParam("start") int start,
			@DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("items") String items) {
		return getWorkListByQuery(query, start, count, items);
	}

	/**
	 * returns a singel workitem defined by $uniqueid
	 * 
	 * @param uniqueid
	 * @return
	 */
	@GET
	@Path("/workitem/{uniqueid}")
	public XMLItemCollection getWorkItem(
			@PathParam("uniqueid") String uniqueid,
			@QueryParam("items") String items) {

		ItemCollection workitem;
		try {

			// test uniqueid for .json (correct wrong routing happens in
			// RestEasy / Wildfly)
			if (uniqueid.endsWith(".json")) {
				return this
						.getWorkItemJSON(uniqueid.substring(0,
								uniqueid.indexOf(".json")), items);
			}
			if (uniqueid.endsWith(".xml")) {
				return this.getWorkItemXML(
						uniqueid.substring(0, uniqueid.indexOf(".xml")), items);
			}

			workitem = workflowService.getWorkItem(uniqueid);
			return XMLItemCollectionAdapter.putItemCollection(workitem,
					getItemList(items));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@GET
	@Path("/workitem/{uniqueid}.xml")
	@Produces(MediaType.APPLICATION_XML)
	public XMLItemCollection getWorkItemXML(
			@PathParam("uniqueid") String uniqueid,
			@QueryParam("items") String items) {
		return getWorkItem(uniqueid, items);
	}
	
	@GET
	@Path("/workitem/{uniqueid}/xml")
	@Produces(MediaType.APPLICATION_XML)
	public XMLItemCollection getWorkItemXMLPath(
			@PathParam("uniqueid") String uniqueid,
			@QueryParam("items") String items) {
		return getWorkItem(uniqueid, items);
	}

	@GET 
	@Path("/workitem/{uniqueid}.json")
	@Produces(MediaType.APPLICATION_JSON)
	public XMLItemCollection getWorkItemJSON(
			@PathParam("uniqueid") String uniqueid,
			@QueryParam("items") String items) {
		return getWorkItem(uniqueid, items);
	}
	
	@GET 
	@Path("/workitem/{uniqueid}/json")
	@Produces(MediaType.APPLICATION_JSON)
	public XMLItemCollection getWorkItemJSONPath(
			@PathParam("uniqueid") String uniqueid,
			@QueryParam("items") String items) {
		return getWorkItem(uniqueid, items);
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
	@GET
	@Path("/workitem/{uniqueid}/file/{file}")
	public Response getWorkItemFile(@PathParam("uniqueid") String uniqueid,
			@PathParam("file") @Encoded String file, @Context UriInfo uriInfo) {

		ItemCollection workItem;
		try {
			workItem = workflowService.getWorkItem(uniqueid);

			if (workItem != null) {

				String fileNameUTF8 = URLDecoder.decode(file, "UTF-8");
				String fileNameISO = URLDecoder.decode(file, "ISO-8859-1");

				// fetch $file from hashmap....
				HashMap mapFiles = null;
				List vFiles = workItem.getItemValue("$file");
				if (vFiles != null && vFiles.size() > 0) {
					mapFiles = (HashMap) vFiles.get(0);

					Vector<Object> vectorFileInfo = new Vector<Object>();
					// try to guess encodings.....
					vectorFileInfo = (Vector) mapFiles.get(fileNameUTF8);
					if (vectorFileInfo == null)
						vectorFileInfo = (Vector) mapFiles.get(fileNameISO);
					if (vectorFileInfo == null)
						vectorFileInfo = (Vector) mapFiles.get(file);
					if (vectorFileInfo != null) {
						String sContentType = vectorFileInfo.elementAt(0)
								.toString();
						byte[] fileContent = (byte[]) vectorFileInfo
								.elementAt(1);

						// Set content type in order of the contentType stored
						// in the $file attribute
						Response.ResponseBuilder builder = Response.ok(
								fileContent, sContentType);

						return builder.build();

					} else {
						logger.warning("WorklfowRestService unable to open file: '"
								+ file
								+ "' in workitem '"
								+ uniqueid
								+ "' - error: Filename not found!");

						// workitem not found
						return Response.status(Response.Status.NOT_FOUND)
								.build();
					}
				} else {
					logger.warning("WorklfowRestService unable to open file: '"
							+ file + "' in workitem '" + uniqueid
							+ "' - error: No files available!");

					// workitem not found
					return Response.status(Response.Status.NOT_FOUND).build();
				}
			} else {
				logger.warning("WorklfowRestService unable to open file: '"
						+ file + "' in workitem '" + uniqueid
						+ "' - error: Workitem not found!");
				// workitem not found
				return Response.status(Response.Status.NOT_FOUND).build();
			}

		} catch (Exception e) {
			logger.severe("WorklfowRestService unable to open file: '" + file
					+ "' in workitem '" + uniqueid + "' - error: "
					+ e.getMessage());
			e.printStackTrace();
		}

		logger.severe("WorklfowRestService unable to open file: '" + file
				+ "' in workitem '" + uniqueid + "'");
		return Response.status(Response.Status.NOT_FOUND).build();

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
	@PUT
	@Path("/workitem")
	@Consumes({ "application/x-www-form-urlencoded" })
	public Response putWorkitem(InputStream requestBodyStream,
			@QueryParam("action") String action,
			@QueryParam("error") String error) {

		logger.fine("[WorkflowRestService] @PUT /workitem  method:putWorkitem....");
		// parse the workItem.
		ItemCollection workitem = parseWorkitem(requestBodyStream);

		if (workitem != null) {
			// now test if an workItem with this $uniqueId still exits...
			String unid = workitem.getItemValueString("$uniqueID");
			if (!"".equals(unid)) {
				logger.fine("[WorkflowRestService] putWorkitem $uniqueid="
						+ unid);

				ItemCollection oldWorkitem = workflowService.getWorkItem(unid);
				if (oldWorkitem != null) {
					// an instance of this WorkItem still exists! so we need
					// to update the new values....
					oldWorkitem.getAllItems().putAll(workitem.getAllItems());
					/*
					 * Map map=workitem.getAllItems();
					 * 
					 * map.p Set<String> set=map.keySet(); for (String key:set)
					 * {
					 * 
					 * oldWorkitem.replaceItemValue(key, workitem.getItem()); }
					 */
					workitem = oldWorkitem;

				}

			}
		}

		if (workitem == null) {
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		}

		try {
			// now lets try to process the workitem...
			workitem = workflowService.processWorkItem(workitem);

		} catch (AccessDeniedException e) {
			e.printStackTrace();
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		} catch (ProcessingErrorException e) {
			e.printStackTrace();
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		} catch (PluginException e) {
			e.printStackTrace();
			if (error != null && !"".equals(error)) {
				try {
					// add exception context to URL
					if (error.indexOf('?') == -1)
						error += "?";
					else
						error += "&";

					error += "error_context=" + e.getErrorContext()
							+ "&error_code=" + e.getErrorCode();

					// add params
					java.lang.Object[] params = ((PluginException) e)
							.getErrorParameters();
					if (params != null)
						for (int j = 0; j < params.length; j++) {
							error += "&error_param" + j + "="
									+ params[j].toString();
						}

					return Response.seeOther(new java.net.URI(error)).build();
				} catch (URISyntaxException errorEx) {
					errorEx.printStackTrace();
					return Response.status(Response.Status.NOT_ACCEPTABLE)
							.build();
				}
			} else {
				return Response.status(Response.Status.NOT_ACCEPTABLE).build();
			}

		}

		// try to get action from workItem if not provided by queryString...
		if (action == null || "".equals(action))
			action = workitem.getItemValueString("action");

		// now redirect if action is available...
		if (action != null && !"".equals(action)) {
			try {
				return Response.seeOther(new java.net.URI(action)).build();
			} catch (URISyntaxException e) {
				e.printStackTrace();
				return Response.status(Response.Status.NOT_ACCEPTABLE).build();
			}
		}
		return Response.status(Response.Status.OK).build();

	}

	/**
	 * This method expects a form post.
	 * 
	 * @see putWorkitemDefault
	 * @param requestBodyStream
	 * @return
	 */
	@POST
	@Path("/workitem")
	@Consumes({ "application/x-www-form-urlencoded" })
	public Response postWorkitem(InputStream requestBodyStream,
			@QueryParam("action") String action,
			@QueryParam("error") String error) {
		logger.fine("[WorkflowRestService] @POST /workitem  method:putWorkitem....");

		return putWorkitem(requestBodyStream, action, error);
	}

	/**
	 * This method post a ItemCollection object to be processed by the
	 * WorkflowManager. The method test for the propertys $processid and
	 * $activityid
	 * 
	 * NOTE!! - this method did not update an existing instance of a workItem.
	 * The behavior is different to the method putWorkitem(). It need to be
	 * discussed if the behavior is wrong or not.
	 * 
	 * @param workitem
	 *            - new workItem data
	 */
	@PUT
	@Path("/workitem")
	@Produces(MediaType.APPLICATION_XML)
	@Consumes({ "application/xml", "text/xml" })
	public Response putWorkitemXML(XMLItemCollection workitem,
			@QueryParam("action") String action) {

		logger.fine("[WorkflowRestService] @PUT /workitem  method:putWorkitemXML....");

		ItemCollection itemCollection;
		try {
			itemCollection = XMLItemCollectionAdapter
					.getItemCollection(workitem);
			itemCollection = workflowService.processWorkItem(itemCollection);

			// try to get action from workItem if not provided by queryString...
			if (action == null || "".equals(action))
				action = itemCollection.getItemValueString("action");
			// now redirect if action is available...
			if (action != null && !"".equals(action)) {
				try {
					return Response.seeOther(new java.net.URI(action)).build();
				} catch (URISyntaxException e) {
					e.printStackTrace();
					return Response.status(Response.Status.NOT_ACCEPTABLE)
							.build();
				}
			}

			return Response.ok(
					XMLItemCollectionAdapter.putItemCollection(itemCollection),
					MediaType.APPLICATION_XML).build();
			// return Response.status(Response.Status.OK).build();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Response.status(Response.Status.NOT_ACCEPTABLE).build();
	}

	@POST
	@Path("/workitem")
	@Produces(MediaType.APPLICATION_XML)
	@Consumes({ "application/xml", "text/xml" })
	public Response postWorkitemXML(XMLItemCollection workitem,
			@QueryParam("action") String action) {
		logger.fine("[WorkflowRestService] @POST /workitem  method:putWorkitemXML....");

		return putWorkitemXML(workitem, action);
	}

	/**
	 * NOTE!
	 * 
	 * This method did not work because the json format produced by
	 * getWorkitemJSOON it non consumable. The reason is @type attribute:
	 * 
	 * <code>
	 * ... value":{"@type":"xs:int","$":"10"}
	 * </code>
	 * 
	 * Seems to be a problem of jaxb jax-rs. Mybe GlassFish 4 solves this
	 * problem.
	 * 
	 * For now we can not use this method.
	 * 
	 * @see postWorkitemJSON(InputStream requestBodyStream)
	 * 
	 * @param workitem
	 * @param action
	 * @return
	 */
	@POST
	@Path("/workitem.json2")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response postWorkitemJSON(XMLItemCollection workitem,
			@QueryParam("action") String action) {
		logger.fine("[WorkflowRestService] @POST /workitem  method:putWorkitemJSON....");

		return putWorkitemXML(workitem, action);
	}

	/**
	 * This method expects a form post and processes the WorkItem by the
	 * WorkflowService EJB.
	 * 
	 * The Method returns a JSON object with the new data. If a processException
	 * Occurs the method returns a JSON object with the error code
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
	public Response postWorkitemJSON(InputStream requestBodyStream,
			@QueryParam("action") String action,
			@QueryParam("error") String error,
			@QueryParam("encoding") String encoding) {

		logger.fine("[WorkflowRestService] @PUT /workitem  method:putWorkitemJSON....");

		// determine encoding from servlet request ....
		if (encoding == null || encoding.isEmpty()) {
			encoding = servletRequest.getCharacterEncoding();
			logger.fine("[WorkflowRestService] postWorkitemJSON using request econding="
					+ encoding);
		} else {
			logger.fine("[WorkflowRestService] postWorkitemJSON set econding="
					+ encoding);
		}
		// set defautl encoding UTF-8
		if (encoding == null || encoding.isEmpty()) {
			encoding = "UTF-8";
			logger.fine("[WorkflowRestService] postWorkitemJSON no encoding defined, set default econding to"
					+ encoding);
		}

		ItemCollection workitem = null;
		XMLItemCollection responseWorkitem = null;
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

		if (workitem != null) {
			// now test if an workItem with this $uniqueId still exits...
			String unid = workitem.getItemValueString("$uniqueID");
			if (!"".equals(unid)) {

				ItemCollection oldWorkitem = workflowService.getWorkItem(unid);
				if (oldWorkitem != null) {
					// an instance of this WorkItem still exists! so we need
					// to update the new values....
					oldWorkitem.getAllItems().putAll(workitem.getAllItems());
					workitem = oldWorkitem;
				}
			}
		}

		if (workitem == null) {
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		}

		try {
			// now lets try to process the workitem...
			workitem = workflowService.processWorkItem(workitem);

			try {
				responseWorkitem = XMLItemCollectionAdapter
						.putItemCollection(workitem);
			} catch (Exception e1) {
				e1.printStackTrace();
				logger.warning("[WorkflowRestService] PostWorkitem failed: "
						+ e1.getMessage());
				return Response.status(Response.Status.NOT_ACCEPTABLE)
						.type(MediaType.APPLICATION_JSON).build();

			}
		} catch (AccessDeniedException e) {
			logger.warning("[WorkflowRestService] PostWorkitem failed: "
					+ e.getMessage());
			return Response.status(Response.Status.NOT_ACCEPTABLE)
					.type(MediaType.APPLICATION_JSON).entity(responseWorkitem)
					.build();
		} catch (ProcessingErrorException e) {
			logger.warning("[WorkflowRestService] PostWorkitem failed: "
					+ e.getMessage());
			return Response.status(Response.Status.NOT_ACCEPTABLE)
					.type(MediaType.APPLICATION_JSON).entity(responseWorkitem)
					.build();
		} catch (PluginException e) {
			// test for error code
			logger.warning("[WorkflowRestService] PostWorkitem failed: "
					+ e.getMessage());
			if (error != null && !"".equals(error)) {
				workitem.replaceItemValue("error_context", e.getErrorContext());
				workitem.replaceItemValue("error_code", e.getErrorCode());
				// add error params
				java.lang.Object[] params = ((PluginException) e)
						.getErrorParameters();
				if (params != null) {
					Vector vParams = new Vector();
					for (int j = 0; j < params.length; j++) {
						vParams.add(params[j].toString());
					}
					workitem.replaceItemValue("error_params", vParams);
				}
				return Response.status(Response.Status.NOT_ACCEPTABLE)
						.type(MediaType.APPLICATION_JSON)
						.entity(responseWorkitem).build();
			} else {
				return Response.status(Response.Status.NOT_ACCEPTABLE)
						.type(MediaType.APPLICATION_JSON)
						.entity(responseWorkitem).build();
			}
		}

		// success HTTP 200
		return Response.ok(responseWorkitem, MediaType.APPLICATION_JSON)
				.build();

	}

	/**
	 * This method post a collection of ItemCollection objects to be processed
	 * by the WorkflowManager.
	 * 
	 * @param worklist
	 *            - workitem list data
	 */
	@PUT
	@Path("/workitems")
	@Consumes({ "application/xml", "text/xml" })
	public Response putWorkitemsXML(EntityCollection worklist) {

		logger.fine("[WorkflowRestService] @PUT /workitems  method:putWorkitemsXML....");

		XMLItemCollection entity;
		ItemCollection itemCollection;
		try {
			// save new entities into database and update modelversion.....
			for (int i = 0; i < worklist.getEntity().length; i++) {
				entity = worklist.getEntity()[i];
				itemCollection = XMLItemCollectionAdapter
						.getItemCollection(entity);
				// process entity
				workflowService.processWorkItem(itemCollection);
			}
			return Response.status(Response.Status.OK).build();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Response.status(Response.Status.NOT_ACCEPTABLE).build();
	}

	@POST
	@Path("/workitems")
	@Consumes({ "application/xml", "text/xml" })
	public Response postWorkitemsXML(EntityCollection worklist) {
		logger.fine("[WorkflowRestService] @POST /workitems  method:putWorkitemsXML....");

		return putWorkitemsXML(worklist);
	}

	/**
	 * This method expects a form post. The method parses the input stream to
	 * extract the provides field/value pairs. NOTE: The method did not(!)
	 * assume that the put/post request contains a complete workItem. For this
	 * reason the method loads the existing instance of the corresponding
	 * workItem (identified by the $unqiueid) and adds the values provided by
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
	public final static ItemCollection parseWorkitem(
			InputStream requestBodyStream) {
		Vector<String> vMultiValueFieldNames = new Vector<String>();
		BufferedReader in = new BufferedReader(new InputStreamReader(
				requestBodyStream));
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
					logger.finest("[WorkflowRestService] parse line:"
							+ fieldValue + "");
					try {
						fieldValue = URLDecoder.decode(fieldValue, "UTF-8");

						if (!fieldValue.contains("=")) {
							logger.finest("[WorkflowRestService] line will be skipped");
							continue;
						}

						// get fieldname
						String fieldName = fieldValue.substring(0,
								fieldValue.indexOf('='));

						// if fieldName contains blank or : or --- we skipp the
						// line
						if (fieldName.contains(":") || fieldName.contains(" ")
								|| fieldName.contains(";")) {
							logger.finest("[WorkflowRestService] line will be skipped");
							continue;
						}

						// test for value...
						if (fieldValue.indexOf('=') == fieldValue.length()) {
							// no value
							workitem.replaceItemValue(fieldName, "");
							logger.fine("[WorkflowRestService] no value for '"
									+ fieldName + "'");
						} else {
							fieldValue = fieldValue.substring(fieldValue
									.indexOf('=') + 1);
							// test for a multiValue field - did we know
							// this
							// field....?
							fieldName = fieldName.toLowerCase();
							if (vMultiValueFieldNames.indexOf(fieldName) > -1) {

								List v = workitem.getItemValue(fieldName);
								v.add(fieldValue);
								logger.fine("[WorkflowRestService] multivalue for '"
										+ fieldName
										+ "' = '"
										+ fieldValue
										+ "'");
								workitem.replaceItemValue(fieldName, v);
							} else {
								// first single value....
								logger.fine("[WorkflowRestService] value for '"
										+ fieldName + "' = '" + fieldValue
										+ "'");
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
	 * This method returns a List object from a given comma separated string.
	 * The method returns null if no elements are found. The provided parameter
	 * looks typical like this: <code>
	 *   txtWorkflowStatus,numProcessID,txtName
	 * </code>
	 * 
	 * @param items
	 * @return
	 */
	private List<String> getItemList(String items) {
		if (items == null || "".equals(items))
			return null;
		Vector<String> v = new Vector<String>();
		StringTokenizer st = new StringTokenizer(items, ",");
		while (st.hasMoreTokens())
			v.add(st.nextToken());
		return v;
	}
}
