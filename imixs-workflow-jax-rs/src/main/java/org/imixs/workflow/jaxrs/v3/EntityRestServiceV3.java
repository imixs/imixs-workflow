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

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.jee.ejb.EntityService;

/**
 * The EntityService provides methods to access the EntityService EJB
 * 
 * @author rsoika
 * 
 */
@Deprecated
@Path("/v3/entity")
@Produces({ MediaType.TEXT_HTML, MediaType.APPLICATION_XML,
		MediaType.APPLICATION_JSON, MediaType.TEXT_XML })
@Stateless
public class EntityRestServiceV3 {

	//@EJB
	//private EntityService entityService;

	@EJB
	private DocumentService documentService;
	
	@javax.ws.rs.core.Context
	private static HttpServletRequest servletRequest;

	private static Logger logger = Logger.getLogger(EntityRestServiceV3.class
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
				out.write("<h1>Imixs-Entity REST Service</h1>".getBytes());
				out.write("<p>See the <a href=\"http://www.imixs.org/xml/restservice/entityservice.html\" target=\"_blank\">Imixs REST Service API</a> for more information about this Service.</p>"
						.getBytes());

				// end
				out.write("</body></html>".getBytes());
			}
		};

	}

	


	/**
	 * This method saves a entity provided in xml format
	 * 
	 * Note: the method merges the content of the given entity into an existing
	 * one because the EntityService method save() did not merge an entity. But
	 * the rest service typically consumes only a subset of attributes. So this
	 * is the reason why we merge the entity here. In different to the behavior
	 * of the EntityService the WorkflowService method process() did this merge
	 * automatically.
	 * 
	 * @param xmlworkitem
	 *            - entity to be saved
	 * @return
	 */
	@POST
	@Path("/")
	@Produces(MediaType.APPLICATION_XML)
	@Consumes({ MediaType.APPLICATION_XML, "text/xml" })
	public Response putEntity(XMLItemCollection xmlworkitem) {
		if (servletRequest.isUserInRole("org.imixs.ACCESSLEVEL.MANAGERACCESS") == false) {
			return Response.status(Response.Status.UNAUTHORIZED).build();
		}
		ItemCollection workitem;
		workitem = XMLItemCollectionAdapter.getItemCollection(xmlworkitem);

		if (workitem == null) {
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		}

		try {

			// try to load current instance of this entity
			ItemCollection currentInstance = documentService.load(workitem
					.getItemValueString(EntityService.UNIQUEID));
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
			workitem = this.addErrorMessage(e, workitem);
		} catch (RuntimeException e) {
			logger.severe(e.getMessage());
			workitem = this.addErrorMessage(e, workitem);
		}

		// return workitem
		try {
			if (workitem.hasItem("$error_code"))
				return Response
						.ok(XMLItemCollectionAdapter
								.putItemCollection(workitem),
								MediaType.APPLICATION_XML)
						.status(Response.Status.NOT_ACCEPTABLE).build();
			else
				return Response.ok(
						XMLItemCollectionAdapter.putItemCollection(workitem),
						MediaType.APPLICATION_XML).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		}
	}

	/**
	 * This method deletes an entity
	 * 
	 */
	@DELETE
	@Path("/{uniqueid}")
	public Response deleteEntity(@PathParam("uniqueid") String uniqueid) {
		if (servletRequest.isUserInRole("org.imixs.ACCESSLEVEL.MANAGERACCESS") == false) {
			return Response.status(Response.Status.UNAUTHORIZED).build();
		}
		ItemCollection entity = documentService.load(uniqueid);
		if (entity != null) {
			documentService.remove(entity);
		}

		return Response.status(Response.Status.OK).build();
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
	static List<String> getItemList(String items) {
		if (items == null || "".equals(items))
			return null;
		Vector<String> v = new Vector<String>();
		StringTokenizer st = new StringTokenizer(items, ",");
		while (st.hasMoreTokens())
			v.add(st.nextToken());
		return v;
	}

	/**
	 * This helper method adds a error message to the given entity, based on the
	 * data in a Exception. This kind of error message can be displayed in a
	 * page evaluating the properties '$error_code' and '$error_message'. These
	 * attributes will not be stored.
	 * 
	 * @param pe
	 */
	private ItemCollection addErrorMessage(Exception pe,
			ItemCollection aworkitem) {

		if (pe instanceof RuntimeException && pe.getCause() != null) {
			pe = (RuntimeException) pe.getCause();
		}

		if (pe instanceof AccessDeniedException) {
			aworkitem.replaceItemValue("$error_code",
					((AccessDeniedException) pe).getErrorCode());
			aworkitem.replaceItemValue("$error_message", pe.getMessage());
		}

		return aworkitem;
	}
}
