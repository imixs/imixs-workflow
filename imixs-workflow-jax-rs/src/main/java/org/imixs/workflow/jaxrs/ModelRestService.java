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

import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.xml.EntityCollection;
import org.imixs.workflow.xml.XMLItemCollection;
import org.imixs.workflow.xml.XMLItemCollectionAdapter;

/**
 * The WorkflowService Handler supports methods to process different kind of
 * request URIs
 * 
 * @author rsoika
 * 
 */
@Path("/model")
@Produces({ "text/html", "application/xml", "application/json" })
@Stateless
public class ModelRestService {

	@EJB
	org.imixs.workflow.jee.ejb.EntityService entityService;

	@EJB
	org.imixs.workflow.jee.ejb.ModelService modelService;

	@GET
	@Produces("application/xml")
	public String getAllVersions() {
		List<String> col = null;
		StringBuffer sb = new StringBuffer();
		sb.append("<model>");
		try {
			col = modelService.getAllModelVersions();

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
	@Path("/{version}")
	public EntityCollection getProcessList(
			@PathParam("version") String version,
			@QueryParam("items") String items) {
		Collection<ItemCollection> col = null;
		try {

			col = modelService.getProcessEntityListByVersion(version);
			return XMLItemCollectionAdapter.putCollection(col,
					getItemList(items));

		} catch (Exception e) {
			e.printStackTrace();
		}
		return new EntityCollection();
	}

	@GET
	@Path("/{version}.xml")
	@Produces("application/xml")
	public EntityCollection getProcessListXML(
			@PathParam("version") String version,
			@QueryParam("items") String items) {
		return getProcessList(version, items);
	}

	@GET
	@Path("/{version}.json")
	@Produces("application/json")
	public EntityCollection getProcessListJSON(
			@PathParam("version") String version,
			@QueryParam("items") String items) {
		return getProcessList(version, items);
	}

	
	
	@GET
	@Path("/{version}/process/{processid}")
	public XMLItemCollection getProcessEntity(
			@PathParam("version") String version,
			@PathParam("processid") int processid,
			@QueryParam("items") String items) {
		ItemCollection process= null;
		try {

			process = modelService.getProcessEntityByVersion(processid,version);
			return XMLItemCollectionAdapter.putItemCollection(process,
					getItemList(items));

		} catch (Exception e) {
			e.printStackTrace();
		}
		return new XMLItemCollection();
	}

	
	@GET
	@Path("/{version}/process/{processid}.xml")
	@Produces("application/xml")
	public XMLItemCollection getProcessEntityXML(
			@PathParam("version") String version,
			@PathParam("processid") int processid,
			@QueryParam("items") String items) {
		
		return  getProcessEntity(version,processid,items);
	}

	
	@GET
	@Path("/{version}/process/{processid}.json")
	@Produces("application/json")
	public XMLItemCollection getProcessEntityJSON(
			@PathParam("version") String version,
			@PathParam("processid") int processid,
			@QueryParam("items") String items) {
		
		return  getProcessEntity(version,processid,items);
	}

	
	
	/**
	 * Retuns a list of all Start Entities from each workflowgroup
	 * 
	 * @param version
	 * @return
	 */
	@GET
	@Path("/groups/{version}")
	public EntityCollection getStartProcessList(
			@PathParam("version") String version,
			@QueryParam("items") String items) {
		Collection<ItemCollection> col = null;
		try {

			col = modelService.getAllStartProcessEntitiesByVersion(version);
			return XMLItemCollectionAdapter.putCollection(col,
					getItemList(items));

		} catch (Exception e) {
			e.printStackTrace();
		}
		return new EntityCollection();
	}

	@GET
	@Path("/groups/{version}.xml")
	@Produces("application/xml")
	public EntityCollection getStartProcessListXML(
			@PathParam("version") String version,
			@QueryParam("items") String items) {
		return getStartProcessList(version, items);
	}

	@GET
	@Path("/groups/{version}.json")
	@Produces("application/json")
	public EntityCollection getStartProcessListJSON(
			@PathParam("version") String version,
			@QueryParam("items") String items) {
		return getStartProcessList(version, items);
	}

	@GET
	@Path("/{version}/activities/{processid}")
	public EntityCollection getActivityList(
			@PathParam("version") String version,
			@PathParam("processid") int processid,
			@QueryParam("items") String items) {
		Collection<ItemCollection> col = null;
		try {
			col = modelService.getActivityEntityListByVersion(processid,
					version);
			return XMLItemCollectionAdapter.putCollection(col,
					getItemList(items));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new EntityCollection();
	}

	@GET
	@Path("/{version}/activities/{processid}.xml")
	@Produces("application/xml")
	public EntityCollection getActivityListXML(
			@PathParam("version") String version,
			@PathParam("processid") int processid,
			@QueryParam("items") String items) {
		return getActivityList(version, processid,items);
	}
	
	@GET
	@Path("/{version}/activities/{processid}.json")
	@Produces("application/json")
	public EntityCollection getActivityListJSON(
			@PathParam("version") String version,
			@PathParam("processid") int processid,
			@QueryParam("items") String items) {
		return getActivityList(version, processid,items);
	}
	

	@DELETE
	@Path("/{version}")
	public void deleteModel(@PathParam("version") String version) {
		try {
			modelService.removeModelVersion(version);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * This method updates a Model provided in a EntityCollection object for a
	 * provided model version. The Method expects a subresource with a
	 * ModelVersion. Next the method updates each Entity object with the
	 * property $ModelVersion. An old version will be automatically removed
	 * before update.
	 * 
	 * @param version
	 *            - $modelversion
	 * @param ecol
	 *            - model data
	 */
	@PUT
	@Path("/{version}")
	@Consumes({ "application/xml", "text/xml" })
	public void putModelByVersion(@PathParam("version") String sModelVersion,
			EntityCollection ecol) {

		XMLItemCollection entity;
		ItemCollection itemCollection;
		try {
			if (ecol.getEntity().length > 0) {
				/*
				 * first we need to delete the old model if available.
				 */
				if (sModelVersion == null)
					sModelVersion = "";

				// delete old model if a modelversion is available
				if (!"".equals(sModelVersion))
					modelService.removeModelVersion(sModelVersion);

				// save new entities into database and update modelversion.....
				for (int i = 0; i < ecol.getEntity().length; i++) {
					entity = ecol.getEntity()[i];
					itemCollection = XMLItemCollectionAdapter
							.getItemCollection(entity);
					// update model version
					itemCollection.replaceItemValue("$modelVersion",
							sModelVersion);
					// save entity
					entityService.save(itemCollection);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * This method updates a Model provided in a EntityCollection object.
	 * 
	 * The method takes the first entity to get the provided $modelVersion. An
	 * old version will be automatically removed before update.
	 * 
	 * @param ecol
	 */
	@PUT
	@Consumes({ "application/xml", "text/xml" })
	public void putModel(EntityCollection ecol) {
		String sModelVersion = null;
		XMLItemCollection entity;
		ItemCollection itemCollection;
		try {
			if (ecol.getEntity().length > 0) {
				/*
				 * first we need get model version from first entity
				 */
				entity = ecol.getEntity()[0];
				itemCollection = XMLItemCollectionAdapter
						.getItemCollection(entity);
				sModelVersion = itemCollection
						.getItemValueString("$ModelVersion");

				putModelByVersion(sModelVersion, ecol);

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@POST
	@Path("/{version}")
	@Consumes({ "application/xml", "text/xml" })
	public void postModelByVersion(@PathParam("version") String sModelVersion,
			EntityCollection ecol) {
		putModelByVersion(sModelVersion, ecol);
	}

	@POST
	@Consumes({ "application/xml", "text/xml" })
	public void postModel(EntityCollection ecol) {
		putModel(ecol);
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
