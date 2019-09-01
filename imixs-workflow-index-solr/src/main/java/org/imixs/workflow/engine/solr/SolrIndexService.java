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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.EventLogService;
import org.imixs.workflow.engine.SetupEvent;
import org.imixs.workflow.engine.adminp.AdminPService;
import org.imixs.workflow.engine.index.SchemaService;
import org.imixs.workflow.engine.jpa.EventLog;
import org.imixs.workflow.exceptions.IndexException;
import org.imixs.workflow.exceptions.QueryException;
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

	public static final int EVENTLOG_ENTRY_FLUSH_COUNT = 16;
	public static final int DEFAULT_MAX_SEARCH_RESULT = 9999; // limiting the
																// total
	// number of hits
	public static final int DEFAULT_PAGE_SIZE = 100; // default docs in one page

	@PersistenceContext(unitName = "org.imixs.workflow.jpa")
	private EntityManager manager;

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
	private EventLogService eventLogService;

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
	 * This method verifies the schema of the Solr core. If field definitions have
	 * change, a schema update is posted to the Solr rest API.
	 * <p>
	 * The method assumes that a core is already created with a manageable schema.
	 * 
	 * @param setupEvent
	 * @throws RestAPIException
	 */
	public void setup(@Observes SetupEvent setupEvent) throws RestAPIException {

		logger.info("...verify solr core '" + core + "'...");

		// try to get the schma of the core...
		try {
			String existingSchema = restClient.get(host + "/api/cores/" + core + "/schema");
			logger.info("...core   - OK ");

			// update schema
			updateSchema(existingSchema);
		} catch (RestAPIException e) {
			// no schema found
			logger.severe("...no solr core '" + core + "' found - verify the solr instance!");
			throw e;
		}

	}

	/**
	 * Updates the schema definition of an existing Solr core.
	 * <p>
	 * The schema definition is build by the method builUpdateSchema(). The
	 * updateSchema adds or replaces field definitions depending on the fieldList
	 * definitions provided by the Imixs SchemaService. See the method
	 * builUpdateSchema() for details.
	 * <p>
	 * The method asumes that a core already exits. Otherwise an exception is
	 * thrown.
	 * 
	 * @param schema
	 *            - existing schema defintion
	 * @return - an update Schema definition to be POST to the Solr rest api.
	 * @throws RestAPIException
	 */
	public void updateSchema(String schema) throws RestAPIException {

		
		
		// create the schema....
		String schemaUpdate = buildUpdateSchema(schema);
		// test if the schemaUdpate contains instructions....
		if (!"{}".equals(schemaUpdate)) {
			String uri = host + "/api/cores/" + core + "/schema";
			logger.info("...updating schema '" + core + "':");
			logger.finest("..." + schemaUpdate);
			restClient.post(uri, schemaUpdate, "application/json");
			logger.info("...schema update - successfull ");
			// force rebuild index
			rebuildIndex();
		} else {
			logger.info("...schema - OK ");
		}
	}

	/**
	 * This method adds a collection of documents to the Lucene Solr index. The
	 * documents are added immediately to the index. Calling this method within a
	 * running transaction leads to a uncommitted reads in the index. For
	 * transaction control, it is recommended to use instead the the method
	 * solrUpdateService.updateDocuments() which takes care of uncommitted reads.
	 * <p>
	 * This method is used by the JobHandlerRebuildIndex only.
	 * 
	 * @param documents
	 *            of ItemCollections to be indexed
	 * @throws RestAPIException
	 */
	public void indexDocuments(List<ItemCollection> documents) throws RestAPIException {
		long ltime = System.currentTimeMillis();

		if (documents == null || documents.size() == 0) {
			// no op!
			return;
		} else {

			String xmlRequest = buildAddDoc(documents);
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
	 * This method adds a single document to the Lucene Solr index. The document is
	 * added immediately to the index. Calling this method within a running
	 * transaction leads to a uncommitted reads in the index. For transaction
	 * control, it is recommended to use instead the the method
	 * solrUpdateService.updateDocuments() which takes care of uncommitted reads.
	 * 
	 * @param documents
	 *            of ItemCollections to be indexed
	 * @throws RestAPIException
	 */
	public void indexDocument(ItemCollection document) throws RestAPIException {
		List<ItemCollection> col = new ArrayList<ItemCollection>();
		col.add(document);
		indexDocuments(col);
	}

	/**
	 * This method removes a collection of documents from the Lucene Solr index.
	 * 
	 * @param documents
	 *            of collection of UniqueIDs to be removed from the index
	 * @throws RestAPIException
	 */
	public void removeDocuments(List<String> documents) throws RestAPIException {
		long ltime = System.currentTimeMillis();

		if (documents == null || documents.size() == 0) {
			// no op!
			return;
		} else {
			StringBuffer xmlDelete = new StringBuffer();
			xmlDelete.append("<delete>");
			for (String id : documents) {
				xmlDelete.append("<id>" + id + "</id>");
			}
			xmlDelete.append("</delete>");
			String xmlRequest = xmlDelete.toString();
			String uri = host + "/solr/" + core + "/update";
			logger.info("...delete documents '" + core + "':");
			restClient.post(uri, xmlRequest, "text/xml");
		}

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("... update index block in " + (System.currentTimeMillis() - ltime) + " ms (" + documents.size()
					+ " workitems total)");
		}
	}

	/**
	 * This method removes a single document from the Lucene Solr index.
	 * 
	 * @param document
	 *            - UniqueID of the document to be removed from the index
	 * 
	 * @throws RestAPIException
	 */
	public void removeDocument(String id) throws RestAPIException {
		List<String> col = new ArrayList<String>();
		col.add(id);
		removeDocuments(col);
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
	 * This method post a search query and returns the result.
	 * 
	 * @param searchterm
	 * @return
	 * @throws QueryException
	 */
	public String query(String searchTerm) throws QueryException {

		logger.info("...search solr index: " + searchTerm + "...");

		// URL Encode the query string....
		try {
			String uri = host + "/solr/" + core + "/query?q=" + URLEncoder.encode(searchTerm, "UTF-8");

			logger.info("... uri=" + uri);
			String result = restClient.get(uri);

			return result;
		} catch (RestAPIException | UnsupportedEncodingException e) {

			logger.severe("Solr search error: " + e.getMessage());
			throw new QueryException(QueryException.QUERY_NOT_UNDERSTANDABLE, e.getMessage(), e);
		}

	}

	/**
	 * This method builds a JSON structure to be used to update an existing Solr
	 * schema. The method adds or replaces field definitions into a solr update
	 * schema.
	 * <p>
	 * The param oldSchema contains the current schema definition of the core.
	 * <p>
	 * In Solr there a two field types defining if the value of a field is stored
	 * and returned by a
	 * <p>
	 * <code>{"add-field":{name=field1, type=text_general, stored=true, docValues=true}}</code>
	 * <p>
	 * For both cases the values are stored in the lucene index and returned by a
	 * query.
	 * </p>
	 * <p>
	 * Stored fields (stored=true) are row orientated. That means that like in a sql
	 * table the values are stored based on the ID of the document.
	 * <p>
	 * In difference the docValues are stored column orientated (forward index). The
	 * values are ordered based on the search term. For features like sorting,
	 * grouping or faceting, docValues increase the performance in general. So it
	 * may look like docValues are the better choice. But one important different is
	 * how the values are stored. In case of a stored field with multi-values, the
	 * values are exactly stored in the same order as they were indexed. DocValues
	 * instead are sorted and reordered. So this will falsify the result of a
	 * document returned by a query.
	 * <p>
	 * <strong>In Imixs-Workflow we use the stored attribute to return parts of a
	 * document at query time. We call this a document-stub which contains only a
	 * subset of fields. Later we load the full document from the SQL database. As
	 * stored fields in our workflow application are also often used for sorting we
	 * combine both attributes. In case of a non-stored field we set also
	 * docValues=false to avoid unnecessary storing of fields. </strong>
	 * 
	 * @see https://lucene.apache.org/solr/guide/8_0/docvalues.html
	 * @return
	 */
	protected String buildUpdateSchema(String oldSchema) {

		StringBuffer updateSchema = new StringBuffer();
		List<String> fieldListStore = schemaService.getFieldListStore();
		List<String> fieldListAnalyse = schemaService.getFieldListAnalyse();
		List<String> fieldListNoAnalyse = schemaService.getFieldListNoAnalyse();

		// remove white space from oldSchema to simplify string compare...
		oldSchema = oldSchema.replace(" ", "");

		
		logger.finest("......old schema="+oldSchema);	
		
		updateSchema.append("{");

		// finally add the default content field
		addFieldDefinitionToUpdateSchema(updateSchema, oldSchema, "content", "text_general", false, false);
		// add each field from the fieldListAnalyse
		for (String field : fieldListAnalyse) {
			boolean store = fieldListStore.contains(field);
			// text_general - docValues are not supported!
			addFieldDefinitionToUpdateSchema(updateSchema, oldSchema, field, "text_general", store, false);
		}

		// add each field from the fieldListNoAnalyse
		for (String field : fieldListNoAnalyse) {
			boolean store = fieldListStore.contains(field);
			// strings - docValues are supported so set it independently from the store flag
			// to true. This is to increase sort and grouping performance
			addFieldDefinitionToUpdateSchema(updateSchema, oldSchema, field, "strings", store, true);
		}

		// finally add the $uniqueid field
		addFieldDefinitionToUpdateSchema(updateSchema, oldSchema, "$uniqueid", "string", true, false);

		// remove last ,
		int lastComma = updateSchema.lastIndexOf(",");
		if (lastComma > -1) {
			updateSchema.deleteCharAt(lastComma);
		}
		updateSchema.append("}");
		return updateSchema.toString();
	}

	/**
	 * This method returns a XML structure to add new documents into the solr index.
	 * 
	 * @return xml content to update documents
	 */
	protected String buildAddDoc(List<ItemCollection> documents) {

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

			// add $uniqueid not analyzed
			addFieldValuesToUpdateRequest(xmlContent, document, WorkflowKernel.UNIQUEID);
			
			xmlContent.append("</doc>");
		}

		xmlContent.append("</add>");

		return xmlContent.toString();
	}

	/**
	 * This method adds a field definition object to an updateSchema.
	 * <p>
	 * In case the same field already exists in the oldSchema then the method will
	 * replace the field. In case id does not exist, the field definition is added
	 * to the update schema.
	 * <p>
	 * Example:
	 * <p>
	 * <code>add-field:{name:"$workflowsummary", type:"text_general", stored:true, docValues:false}</code><br
	 * />
	 * <code>replace-field:{name:"$workflowstatus", type:"strings", stored:true, docValues:true}</code>
	 * <p>
	 * To verify the existence of the field we parse the fieldname in the old schema
	 * definition.
	 *
	 * @param updateSchema
	 *            - a stringBuffer to build the update schema
	 * @param oldSchema
	 *            - the existing schema definition
	 * @param name
	 *            - field name
	 * @param type
	 *            - field type
	 * @param store
	 *            - boolean store field
	 * @param docValue
	 *            - true if docValues should be set to true
	 * 
	 */
	private void addFieldDefinitionToUpdateSchema(StringBuffer updateSchema, String oldSchema, String name, String type,
			boolean store, boolean docvalue) {

		String fieldDefinition = "{\"name\":\"" + name + "\",\"type\":\"" + type + "\",\"stored\":" + store
				+ ",\"docValues\":" + docvalue + "}";

		// test if this field already exists in the old schema. If not we add the new
		// field to the schema with the 'add-field' command.
		// If it already exits, than we need to replace the definition with
		// 'replace-field'.
		String testSchemaField = "{\"name\":\"" + name + "\",";
		if (oldSchema == null || !oldSchema.contains(testSchemaField)) {
			// add new field to updateSchema....
			updateSchema.append("\"add-field\":" + fieldDefinition + ",");
		} else {
			// the field exists in the schema - so replace it if the definition has changed
			if (!oldSchema.contains(fieldDefinition)) {
				updateSchema.append("\"replace-field\":" + fieldDefinition + ",");
			}
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

	/**
	 * This method flushes a given count of eventLogEntries. The method return true
	 * if no more eventLogEntries exist.
	 * 
	 * @param count
	 *            the max size of a eventLog engries to remove.
	 * @return true if the cache was totally flushed.
	 */
	protected boolean flushEventLogByCount(int count) {
		Date lastEventDate = null;
		boolean cacheIsEmpty = true;

		long l = System.currentTimeMillis();
		logger.finest("......flush eventlog cache....");

		List<EventLog> events = eventLogService.findEventsByTopic(count + 1, DocumentService.EVENTLOG_TOPIC_INDEX_ADD,
				DocumentService.EVENTLOG_TOPIC_INDEX_REMOVE);

		if (events != null && events.size() > 0) {
			try {

				int _counter = 0;
				for (EventLog eventLogEntry : events) {

					// lookup the Document Entity...
					org.imixs.workflow.engine.jpa.Document doc = manager
							.find(org.imixs.workflow.engine.jpa.Document.class, eventLogEntry.getRef());

					// if the document was found we add/update the index. Otherwise we remove the
					// document form the index.
					if (doc != null && DocumentService.EVENTLOG_TOPIC_INDEX_ADD.equals(eventLogEntry.getTopic())) {
						// add workitem to search index....
						long l2 = System.currentTimeMillis();
						ItemCollection workitem = new ItemCollection();
						workitem.setAllItems(doc.getData());
						if (!workitem.getItemValueBoolean(DocumentService.NOINDEX)) {
							indexDocument(workitem);
							logger.info("......solr added workitem '" + eventLogEntry.getId() + "' to index in "
									+ (System.currentTimeMillis() - l2) + "ms");
						}
					} else {
						long l2 = System.currentTimeMillis();
						removeDocument(eventLogEntry.getId());
						logger.info("......solr remove workitem '" + eventLogEntry.getId() + "' from index in "
								+ (System.currentTimeMillis() - l2) + "ms");
					}

					// remove the eventLogEntry.
					lastEventDate = eventLogEntry.getCreated().getTime();
					eventLogService.removeEvent(eventLogEntry);

					// break?
					_counter++;
					if (_counter >= count) {
						// we skipp the last one if the maximum was reached.
						cacheIsEmpty = false;
						break;
					}
				}

			} catch (RestAPIException e) {
				logger.warning("...unable to flush lucene event log: " + e.getMessage());
				// We just log a warning here and close the flush mode to no longer block the
				// writer.
				// NOTE: maybe throwing a IndexException would be an alternative:
				//
				// throw new IndexException(IndexException.INVALID_INDEX, "Unable to update
				// lucene search index",
				// luceneEx);
				return true;
			}
		}

		logger.fine("...flushEventLog - " + events.size() + " events in " + (System.currentTimeMillis() - l)
				+ " ms - last log entry: " + lastEventDate);

		return cacheIsEmpty;

	}

	/**
	 * Flush the EventLog cache. This method is called by the LuceneSerachService
	 * only.
	 * <p>
	 * The method flushes the cache in smaller blocks of the given junkSize. to
	 * avoid a heap size problem. The default flush size is 16. The eventLog cache
	 * is tracked by the flag 'dirtyIndex'.
	 * <p>
	 * issue #439 - The method returns false if the event log contains more entries
	 * as defined by the given JunkSize. In this case the caller should recall the
	 * method which runs always in a new transaction. The goal of this mechanism is
	 * to reduce the event log even in cases the outer transaction breaks.
	 * 
	 * @see LuceneSearchService
	 * @return true if the the complete event log was flushed. If false the method
	 *         must be recalled.
	 */
	@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
	public boolean flushEventLog(int junkSize) {
		long total = 0;
		long count = 0;
		boolean dirtyIndex = true;
		long l = System.currentTimeMillis();

		while (dirtyIndex) {
			try {
				dirtyIndex = !flushEventLogByCount(EVENTLOG_ENTRY_FLUSH_COUNT);
				if (dirtyIndex) {
					total = total + EVENTLOG_ENTRY_FLUSH_COUNT;
					count = count + EVENTLOG_ENTRY_FLUSH_COUNT;
					if (count >= 100) {
						logger.finest("...flush event log: " + total + " entries in " + (System.currentTimeMillis() - l)
								+ "ms...");
						count = 0;
					}

					// issue #439
					// In some cases the flush method runs endless.
					// experimental code: we break the flush method after 1024 flushs
					// maybe we can remove this hard break
					if (total >= junkSize) {
						logger.finest("...flush event: Issue #439  -> total count >=" + total
								+ " flushEventLog will be continued...");
						return false;
					}
				}

			} catch (IndexException e) {
				logger.warning("...unable to flush lucene event log: " + e.getMessage());
				return true;
			}
		}
		return true;
	}

}
