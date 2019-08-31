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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.SetupEvent;
import org.imixs.workflow.engine.adminp.AdminPService;
import org.imixs.workflow.engine.index.SchemaService;
import org.imixs.workflow.exceptions.IndexException;
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
	
	@Inject
	private AdminPService adminPService;


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
		String schemaUpdate = createUpdateSchemaJSONRequest(schema);
		// test if the schemaUdpate contains instructions....
		if (!"{}".equals(schemaUpdate)) {
			String uri = host + "/api/cores/" + core + "/schema";
			logger.info("...update schema '" + core + "':");
			logger.info("..." + schemaUpdate);
			restClient.post(uri, schemaUpdate, "application/json");
			
			// force rebuild index
			rebuildIndex();
		} else {
			logger.info("...schema = OK ");
		}
	}

	/**
	 * This method adds a collection of documents to the Lucene solr index. The
	 * documents are added immediately to the index. Calling this method within a
	 * running transaction leads to a uncommitted reads in the index. For
	 * transaction control, it is recommended to use instead the the method
	 * updateDocumetns() which takes care of uncommitted reads.
	 * <p>
	 * This method is used by the JobHandlerRebuildIndex only.
	 * 
	 * @param documents
	 *            of ItemCollections to be indexed
	 * @throws RestAPIException
	 * @throws IndexException
	 */
	public void updateDocumentsUncommitted(List<ItemCollection> documents) throws RestAPIException {
		long ltime = System.currentTimeMillis();
	
		if (documents == null || documents.size() == 0) {
			// no op!
			return;
		} else {
	
			String xmlRequest = createAddDocumentsXMLRequest(documents);
			String uri = host + "/solr/" + core + "/update";
			logger.info("...update documents '" + core + "':");
			restClient.post(uri, xmlRequest, "text/xml");
		}
	
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("... update index block in " + (System.currentTimeMillis() - ltime) + " ms (" + documents.size()
					+ " workitems total)");
		}
	}

	
	/**
	 * This method forces an update of the full text index. 
	 */
	public void rebuildIndex() {
		// now starting index job....
		logger.info("...rebuild lucene index job created...");
		ItemCollection job = new ItemCollection();
		job.replaceItemValue("numinterval", 2); // 2 minutes
		job.replaceItemValue("job", AdminPService.JOB_REBUILD_INDEX);
		adminPService.createJob(job);
	}
	
	
	/**
	 * This method returns a JSON structure to update an existing Solr schema. The
	 * method adds all fields into a solr update definition that did not yet exist
	 * in the current schema.
	 * <p>
	 * The param schema contains the current schema definition of the core.
	 * 
	 * @return
	 */
	protected String createUpdateSchemaJSONRequest(String oldSchema) {

		StringBuffer updateSchema = new StringBuffer();
		List<String> fieldListStore = schemaService.getFieldListStore();
		List<String> fieldListAnalyse = schemaService.getFieldListAnalyse();
		List<String> fieldListNoAnalyse = schemaService.getFieldListNoAnalyse();

		// remove white space from oldSchema to simplify string compare...
		oldSchema = oldSchema.replace(" ", "");

		updateSchema.append("{");

		// finally add the default content field
		addFieldDefinitionToUpdateSchema(updateSchema, oldSchema, "content", "text_general", false);
		// add each field from the fieldListAnalyse
		for (String field : fieldListAnalyse) {
			boolean store = fieldListStore.contains(field);
			addFieldDefinitionToUpdateSchema(updateSchema, oldSchema, field, "text_general", store);
		}

		// add each field from the fieldListNoAnalyse
		for (String field : fieldListNoAnalyse) {
			boolean store = fieldListStore.contains(field);
			addFieldDefinitionToUpdateSchema(updateSchema, oldSchema, field, "strings", store);
		}

		// finally add the $uniqueid field
		addFieldDefinitionToUpdateSchema(updateSchema, oldSchema, "$uniqueid", "string", true);

		// remove last ,
		int lastComma = updateSchema.lastIndexOf(",");
		if (lastComma > -1) {
			updateSchema.deleteCharAt(lastComma);
		}
		updateSchema.append("}");
		return updateSchema.toString();
	}

	/**
	 * This method returns a XNK structure to add new documents into the solr index.
	 * 
	 * @return xml content to update documents
	 */
	protected String createAddDocumentsXMLRequest(List<ItemCollection> documents) {

		List<String> fieldList = schemaService.getFieldList();
		List<String> fieldListAnalyse = schemaService.getFieldListAnalyse();
		List<String> fieldListNoAnalyse = schemaService.getFieldListNoAnalyse();
		SimpleDateFormat dateformat = new SimpleDateFormat("yyyyMMddHHmmss");

		StringBuffer xmlContent = new StringBuffer();

		xmlContent.append("<add commitWithin=\"5000\" overwrite=\"true\">");

		for (ItemCollection document : documents) {
			xmlContent.append("<doc>");

			// add all content fields defined in the schema
			String content = "";
			for (String field : fieldList) {
				String sValue = "";
				// check value list - skip empty fields
				List<?> vValues = document.getItemValue(field);
				if (vValues.size() == 0)
					continue;
				// get all values of a value list field
				for (Object o : vValues) {
					if (o == null)
						// skip null values
						continue;

					if (o instanceof Calendar || o instanceof Date) {

						// convert calendar to string
						String sDateValue;
						if (o instanceof Calendar)
							sDateValue = dateformat.format(((Calendar) o).getTime());
						else
							sDateValue = dateformat.format((Date) o);
						sValue += sDateValue + ",";

					} else
						// simple string representation
						sValue += o.toString() + ",";
				}
				if (sValue != null) {
					content += sValue + ",";
				}
			}
			logger.finest("......add index field content=" + content);
			xmlContent.append("<field name=\"content\">" + content + "</field>");

			// now add all analyzed fields...
			for (String aFieldname : fieldListAnalyse) {
				addFieldValuesToUpdateRequest(xmlContent, document, aFieldname);
			}
			// now add all notanalyzed fields...
			for (String aFieldname : fieldListNoAnalyse) {
				addFieldValuesToUpdateRequest(xmlContent, document, aFieldname);
			}

			xmlContent.append("</doc>");
		}

		xmlContent.append("</add>");

		return xmlContent.toString();
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
	private void addFieldDefinitionToUpdateSchema(StringBuffer updateSchema, String oldSchema, String name, String type,
			boolean store) {

		String fieldDefinition = "{\"name\":\"" + name + "\",\"type\":\"" + type + "\",\"stored\":" + store + "}";
		// test if this field discription already exists
		if (!oldSchema.contains(fieldDefinition)) {
			// add new field to updateSchema....
			updateSchema.append("\"add-field\":" + fieldDefinition + ",");
		}
	}

	/**
	 * This method adds a field value into a xml update request.
	 * 
	 * @param doc
	 *            an existing lucene document
	 * @param workitem
	 *            the workitem containing the values
	 * @param _itemName
	 *            the item name inside the workitem
	 */
	private void addFieldValuesToUpdateRequest(StringBuffer xmlContent, final ItemCollection workitem,
			final String _itemName) {

		SimpleDateFormat dateformat = new SimpleDateFormat("yyyyMMddHHmmss");

		if (_itemName == null) {
			return;
		}

		List<?> vValues = workitem.getItemValue(_itemName);
		if (vValues.size() == 0) {
			return;
		}
		if (vValues.get(0) == null) {
			return;
		}

		String itemName = _itemName.toLowerCase().trim();
		for (Object singleValue : vValues) {
			String convertedValue = "";
			if (singleValue instanceof Calendar || singleValue instanceof Date) {
				// convert calendar to lucene string representation
				String sDateValue;
				if (singleValue instanceof Calendar) {
					sDateValue = dateformat.format(((Calendar) singleValue).getTime());
				} else {
					sDateValue = dateformat.format((Date) singleValue);
				}
				convertedValue = sDateValue;
			} else {
				// default
				convertedValue = singleValue.toString();
			}
			xmlContent.append("<field name=\"" + itemName + "\">" + convertedValue + "</field>");
		}

	}
}
