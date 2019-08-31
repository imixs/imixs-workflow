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

package org.imixs.workflow.engine.solr;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.workflow.engine.SetupEvent;
import org.imixs.workflow.engine.index.SchemaService;
import org.imixs.workflow.services.rest.BasicAuthenticator;
import org.imixs.workflow.services.rest.RestAPIException;
import org.imixs.workflow.services.rest.RestClient;

/**
 * The SolrCoreService checks the existence of a solr core. If no core is found,
 * than the service tries to create a new core from scratch.
 * 
 * @version 1.0
 * @author rsoika
 */
@DeclareRoles({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RolesAllowed({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
public class SolrIndexService {

	public static final int DEFAULT_MAX_SEARCH_RESULT = 9999; // limiting the
																// total
	// number of hits
	public static final int DEFAULT_PAGE_SIZE = 100; // default docs in one page

	@Inject
	@ConfigProperty(name = "solr.server", defaultValue = "http://solr:8983")
	private String host;

	@Inject
	@ConfigProperty(name = "solr.core", defaultValue = "imixs-workflow")
	private String core;

	@Inject
	@ConfigProperty(name = "solr.configset", defaultValue = "_default")
	private String configset;

	@Inject
	@ConfigProperty(name = "solr.user", defaultValue = "")
	private String user;

	@Inject
	@ConfigProperty(name = "solr.password", defaultValue = "")
	private String password;

	@Inject
	private SchemaService schemaService;

	private RestClient restClient;

	private static Logger logger = Logger.getLogger(SolrIndexService.class.getName());

	/**
	 * Create a rest client instance
	 */
	@PostConstruct
	public void init() {
		// create rest client
		restClient = new RestClient(host);
		if (user != null && !user.isEmpty()) {
			BasicAuthenticator authenticator = new BasicAuthenticator(user, password);
			restClient.registerRequestFilter(authenticator);
		}
	}

	/**
	 * The Init method verifies the status of the solr core. If no core is found,
	 * than the method creates a new empty core
	 * 
	 * @param setupEvent
	 * @throws RestAPIException
	 */
	public void setup(@Observes SetupEvent setupEvent) throws RestAPIException {

		logger.info("...verify solr core '" + core + "'...");

		// try to get the schma of the core...
		try {
			String existingSchema = restClient.get(host + "/api/cores/" + core + "/schema");
			logger.info("...core   = OK ");

			// update schema
			updateSchema(existingSchema);
		} catch (RestAPIException e) {
			// no schema found
			logger.severe("...no solr core '" + core + "' found!");
			throw e;
		}

	}

	/**
	 * Creates a new solr core and updates the schema defintion
	 * <p>
	 * In case no core yet exits, the method tries to create a new one. This
	 * typically is necessary after first deployment.
	 * 
	 * @param prop
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
	public void updateSchema(String schema) throws RestAPIException {

		// create the schema....
		String schemaUpdate = createUpdateSchema(schema);
		// test if the schemaUdpate contains instructions....
		if (!"{}".equals(schemaUpdate)) {
			String uri = host + "/api/cores/" + core + "/schema";
			logger.info("...update schema '" + core + "':");
			logger.info("..." + schemaUpdate);
			restClient.post(uri, schemaUpdate, "application/json");
		} else {
			logger.info("...schema = OK ");
		}
	}

	
	

	/**
	 * This method returns a JSON structure to to update an existing Solr schema.
	 * The method adds all fields into a solr update definition that did not yet
	 * exist in the current schema.
	 * <p>
	 * The param schema contains the current schema definition of the core.
	 * 
	 * @return
	 */
	private String createUpdateSchema(String oldSchema) {

		StringBuffer updateSchema = new StringBuffer();
		List<String> fieldListStore = schemaService.getFieldListStore();
		List<String> fieldListAnalyse = schemaService.getFieldListAnalyse();
		List<String> fieldListNoAnalyse = schemaService.getFieldListNoAnalyse();

		// remove white space from oldSchema to simplify string compare...
		oldSchema = oldSchema.replace(" ", "");

		updateSchema.append("{");

		// finally add the default content field
		addFieldIntoUpdateSchema(updateSchema, oldSchema, "content", "text_general", false);
		// add each field from the fieldListAnalyse
		for (String field : fieldListAnalyse) {
			boolean store = fieldListStore.contains(field);
			addFieldIntoUpdateSchema(updateSchema, oldSchema, field, "text_general", store);
		}

		// add each field from the fieldListNoAnalyse
		for (String field : fieldListNoAnalyse) {
			boolean store = fieldListStore.contains(field);
			addFieldIntoUpdateSchema(updateSchema, oldSchema, field, "strings", store);
		}

		// finally add the $uniqueid field
		addFieldIntoUpdateSchema(updateSchema, oldSchema, "$uniqueid", "string", true);

		// remove last ,
		int lastComma = updateSchema.lastIndexOf(",");
		if (lastComma > -1) {
			updateSchema.deleteCharAt(lastComma);
		}
		updateSchema.append("}");
		return updateSchema.toString();
	}

	/**
	 * This method adds a 'add-field' object to an updateSchema.
	 * <p>
	 * In case the same field already exists in the oldSchema then the method will
	 * not add the field to the update schema.
	 * <p>
	 * Example: <code>{name=$workflowsummary, type=text_general, stored=true}</code>
	 *
	 * <p>
	 * NOTE: The test here is very week (simple indexOf) and may cause problems in
	 * the future. TODO optimize the schema compare method.
	 *
	 * @param updateSchema
	 *            - a stringBuffer containing the update schema
	 * @param oldSchema
	 *            - the current schema definition
	 * @param name
	 *            - field name
	 * @param type
	 *            - field type
	 * @param store
	 *            - boolean store field
	 * @param addComma
	 *            - true if a ',' should be added to the end of the updateSchema.
	 * 
	 */
	private void addFieldIntoUpdateSchema(StringBuffer updateSchema, String oldSchema, String name, String type,
			boolean store) {

		String fieldDefinition = "{\"name\":\"" + name + "\",\"type\":\"" + type + "\",\"stored\":" + store + "}";
		// test if this field discription already exists
		if (!oldSchema.contains(fieldDefinition)) {
			// add new field to updateSchema....
			updateSchema.append("\"add-field\":" + fieldDefinition + ",");
		}
	}

}
