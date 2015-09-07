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
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.xml.EntityCollection;
import org.imixs.workflow.xml.XMLCount;
import org.imixs.workflow.xml.XMLIndexList;
import org.imixs.workflow.xml.XMLItemCollectionAdapter;

/**
 * The EntityService provides methods to access the EntityService EJB
 * 
 * @author rsoika
 * 
 */
@Path("/entity")
@Produces({ MediaType.TEXT_HTML, MediaType.APPLICATION_XML,
		MediaType.APPLICATION_JSON, MediaType.TEXT_XML })
@Stateless
public class EntityRestService {

	@EJB
	private EntityService entityService;

	@javax.ws.rs.core.Context
	private static HttpServletRequest servletRequest;

	private static Logger logger = Logger.getLogger(EntityRestService.class
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
	@Path("/entitiesbyquery/{query}")
	public EntityCollection getEntitiesByQuery(
			@PathParam("query") String query,
			@DefaultValue("0") @QueryParam("start") int start,
			@DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("items") String items) {
		Collection<ItemCollection> col = null;
		try {

			logger.fine("Query=" + query);
			// decode query...
			String decodedQuery = URLDecoder.decode(query, "UTF-8");

			col = entityService.findAllEntities(decodedQuery, start, count);
			return XMLItemCollectionAdapter.putCollection(col,
					getItemList(items));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new EntityCollection();
	}

	@GET
	@Path("/entitiesbyquery/{query}.xml")
	@Produces(MediaType.APPLICATION_XML)
	public EntityCollection getEntitiesByQueryXML(
			@PathParam("query") String query,
			@DefaultValue("0") @QueryParam("start") int start,
			@DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("items") String items) {
		return getEntitiesByQuery(query, start, count, items);
	}

	@GET
	@Path("/entitiesbyquery/{query}.json")
	@Produces(MediaType.APPLICATION_JSON)
	public EntityCollection getEntitiesByQueryJSON(
			@PathParam("query") String query,
			@DefaultValue("0") @QueryParam("start") int start,
			@DefaultValue("10") @QueryParam("count") int count,
			@QueryParam("items") String items) {
		return getEntitiesByQuery(query, start, count, items);
	}

	/**
	 * Returns the size of a result set by JPQL Query
	 * 
	 * @param query
	 * @return
	 */
	@GET
	@Path("/countentitiesbyquery/{query}")
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	public XMLCount getWorkListCountByQuery(@PathParam("query") String query) {
		logger.fine("Query=" + query);
		XMLCount result = new XMLCount();
		result.count = (long) -1;
		try {
			// decode query...
			String decodedQuery = URLDecoder.decode(query, "UTF-8");

			int size = entityService.countAllEntities(decodedQuery);
			result.count = (long) size;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Returns the entityService index list in XML or JSON format
	 * 
	 * @return
	 */
	@GET
	@Path("/indexlist")
	public XMLIndexList getIndexList() {
		try {
			Map<String, Integer> result = entityService.getIndices();
			return new XMLIndexList(result);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Adds a new Imixs-Entity-Index to index list of the Imixs EntityService
	 * 
	 * @param name
	 *            - name of index field
	 * @param type
	 *            - index type
	 * @return
	 */
	@PUT
	@Path("/{name}/{type}")
	public void addIndex(@PathParam("name") String name,
			@PathParam("type") int type) {
		entityService.addIndex(name, type);
	}

	/**
	 * Removes an Imixs-Entity-Index from index list of the Imixs EntityService
	 * 
	 * @param title
	 *            - name of index field
	 */
	@DELETE
	@Path("/{name}")
	public void deleteIndex(@PathParam("name") String name) {
		entityService.removeIndex(name);
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

}
