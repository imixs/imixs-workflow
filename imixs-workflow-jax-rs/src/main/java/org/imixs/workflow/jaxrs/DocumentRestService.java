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

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.lucene.LuceneUpdateService;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.QueryException;
import org.imixs.workflow.xml.DocumentCollection;
import org.imixs.workflow.xml.XMLCount;
import org.imixs.workflow.xml.XMLItemCollection;
import org.imixs.workflow.xml.XMLItemCollectionAdapter;

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

	@EJB
	private DocumentService documentService;

	@EJB
	private LuceneUpdateService lucenUpdateService;

	@javax.ws.rs.core.Context
	private static HttpServletRequest servletRequest;

	private static Logger logger = Logger.getLogger(DocumentRestService.class.getName());

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
	 * @param uniqueid
	 * @return
	 */
	@GET
	@Path("/{uniqueid}")
	public XMLItemCollection getDocument(@PathParam("uniqueid") String uniqueid, @QueryParam("items") String items) {

		ItemCollection document;
		try { 
			document = documentService.load(uniqueid);
			return XMLItemCollectionAdapter.putItemCollection(document, DocumentRestService.getItemList(items));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Returns a result set by lucene Search Query
	 * 
	 * @param query
	 * @param pageSize
	 * @param pageIndex
	 * @param items
	 * @return
	 */
	@GET
	@Path("/search/{query}")
	public DocumentCollection findDocumentsByQuery(@PathParam("query") String query,
			@DefaultValue("-1") @QueryParam("pageSize") int pageSize,
			@DefaultValue("0") @QueryParam("pageIndex") int pageIndex, @QueryParam("sortBy") String sortBy,
			@QueryParam("sortReverse") boolean sortReverse, @QueryParam("items") String items) {
		Collection<ItemCollection> col = null;
		try {
			// decode query...
			String decodedQuery = URLDecoder.decode(query, "UTF-8");
			col = documentService.find(decodedQuery, pageSize, pageIndex, sortBy, sortReverse);
			return XMLItemCollectionAdapter.putCollection(col, getItemList(items));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new DocumentCollection();
	}

	/**
	 * Returns the size of a result set by Query
	 * 
	 * @param query
	 * @return
	 */
	@GET
	@Path("/count/{query}")
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	public XMLCount countEntitiesByQuery(@PathParam("query") String query) {
		logger.fine("Query=" + query);
		XMLCount result = new XMLCount();
		result.count = (long) -1;
		try {
			// decode query...
			String decodedQuery = URLDecoder.decode(query, "UTF-8");

			int size = documentService.count(decodedQuery);
			result.count = (long) size;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * The method saves a document provided in xml format. The caller need to be
	 * assigned to the access role 'org.imixs.ACCESSLEVEL.MANAGERACCESS'
	 * 
	 * Note: the method merges the content of the given document into an existing
	 * one because the DocumentService method save() did not merge an entity. But
	 * the rest service typically consumes only a subset of attributes. So this
	 * is the reason why we merge the entity here. In different to the behavior
	 * of the DocumentService the WorkflowService method process() did this merge
	 * automatically.
	 * 
	 * @param xmlworkitem
	 *            - entity to be saved
	 * @return
	 */
	@POST
	@Path("/")
	@Produces(MediaType.APPLICATION_XML)
	@Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
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
			workitem = this.addErrorMessage(e, workitem);
		} catch (RuntimeException e) {
			logger.severe(e.getMessage());
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
	 * This method creates a backup of the result set form a JQPL query. The
	 * entity list will be stored into the file system. The backup can be
	 * restored by calling the restore method
	 * 
	 * 
	 * @param query
	 * @param start
	 * @param count
	 * @param filepath
	 *            - path in server filesystem
	 * @return
	 */
	@PUT
	@Path("/backup/{query}")
	public Response backup(@PathParam("query") String query, @QueryParam("filepath") String filepath) {

		if (servletRequest.isUserInRole("org.imixs.ACCESSLEVEL.MANAGERACCESS") == false) {
			return Response.status(Response.Status.UNAUTHORIZED).build();
		}
		try {
			documentService.backup(query, filepath);
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
	 * @param filepath
	 *            - path in server fileSystem
	 * @return
	 */
	@GET
	@Path("/restore")
	public Response restore(@QueryParam("filepath") String filepath) {

		if (servletRequest.isUserInRole("org.imixs.ACCESSLEVEL.MANAGERACCESS") == false) {
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
	public XMLItemCollection getConfiguration() throws Exception {
		if (servletRequest.isUserInRole("org.imixs.ACCESSLEVEL.MANAGERACCESS") == false) {
			return null;
		}
	
		ItemCollection config = lucenUpdateService.getConfiguration();
	
		return XMLItemCollectionAdapter.putItemCollection(config);
	
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
	private ItemCollection addErrorMessage(Exception pe, ItemCollection aworkitem) {

		if (pe instanceof RuntimeException && pe.getCause() != null) {
			pe = (RuntimeException) pe.getCause();
		}

		if (pe instanceof AccessDeniedException) {
			aworkitem.replaceItemValue("$error_code", ((AccessDeniedException) pe).getErrorCode());
			aworkitem.replaceItemValue("$error_message", pe.getMessage());
		}

		return aworkitem;
	}
}
