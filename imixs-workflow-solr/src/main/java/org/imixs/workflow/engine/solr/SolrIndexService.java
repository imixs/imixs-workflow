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
	 */
	@SuppressWarnings("unused")
	public void setup(@Observes SetupEvent setupEvent) {

		logger.info("...verify solr core '" + core + "'...");

		// try to get the schma of the core...
		try {
			String schema = restClient.get(host + "/api/cores/" + core + "/schema");
			logger.info("...solr core '" + core + "' OK ");
		} catch (RestAPIException e) {
			// no schema found
			logger.info("...no solr core '" + core + "' found!");
			try {
				createCore();
			} catch (RestAPIException e1) {
				logger.warning("Failed to verify solr core - " + e1.getMessage());
				e1.printStackTrace();
			}
		}

	}

	/**
	 * This method returns a JSON structure to create all fields to be added into a
	 * empty schema.
	 * <p>
	 * This is used during creation of a new empty core
	 * 
	 * @return
	 */
	public String getSchemaUpdate() {

		StringBuffer schema = new StringBuffer();
		List<String> fieldListStore = schemaService.getFieldListStore();
		List<String> fieldListAnalyse = schemaService.getFieldListAnalyse();
		List<String> fieldListNoAnalyse = schemaService.getFieldListNoAnalyse();
		schema.append("{");

		// finally add the default content field
		schema.append("\"add-field\":{\"name\":\"content\",\"type\":\"TextField\",\"stored\":false },");

		// add each field from the fieldListAnalyse
		for (String field : fieldListAnalyse) {
			boolean store = fieldListStore.contains(field);
			schema.append("\"add-field\":{\"name\":\"" + field + "\",\"type\":\"StrField\",\"stored\":" + store + " },");
		}
		// add each field from the fieldListAnalyse
		for (String field : fieldListAnalyse) {
			boolean store = fieldListStore.contains(field);
			schema.append("\"add-field\":{\"name\":\"" + field + "\",\"type\":\"TextField\",\"stored\":" + store + " },");
		}

		// add each field from the fieldListNoAnalyse
		for (String field : fieldListNoAnalyse) {
			boolean store = fieldListStore.contains(field);
			schema.append("\"add-field\":{\"name\":\"" + field + "\",\"type\":\"StrField\",\"stored\":" + store + " },");
		}

		// doc.add(new TextField("content", sContent, Store.NO));

		// finally add the $uniqueid field
		schema.append("\"add-field\":{\"name\":\"$uniqueid\",\"type\":\"StrField\",\"stored\":true }");

		schema.append("}");

		return schema.toString();
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
	private void createCore() throws RestAPIException {

		String uri = host + "/solr/admin/cores?action=CREATE&name=" + core + "&instanceDir=" + core
				+ "&configSet=/opt/solr/server/solr/configsets/" + configset;

		logger.info("...creating solr core '" + core + "' with configset '" + configset + "'");
		restClient.get(uri);

		// create the schema....
		String schemaUpdate = getSchemaUpdate();

		uri = host + "/api/cores/" + core + "/schema";

		logger.info("...update schema information");
		restClient.post(uri, schemaUpdate, "application/json");

	}

}
