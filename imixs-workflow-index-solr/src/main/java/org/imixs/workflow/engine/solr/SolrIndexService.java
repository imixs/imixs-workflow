/*******************************************************************************
 * <pre>
 *  Imixs Workflow 
 *  Copyright (C) 2001-2020 Imixs Software Solutions GmbH,  
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
 *      https://www.imixs.org
 *      https://github.com/imixs/imixs-workflow
 *  
 *  Contributors:  
 *      Imixs Software Solutions GmbH - initial API and implementation
 *      Ralph Soika - Software Developer
 * </pre>
 *******************************************************************************/

package org.imixs.workflow.engine.solr;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.function.IntPredicate;
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
import org.imixs.workflow.engine.index.DefaultOperator;
import org.imixs.workflow.engine.index.SchemaService;
import org.imixs.workflow.engine.index.SortOrder;
import org.imixs.workflow.engine.jpa.EventLog;
import org.imixs.workflow.exceptions.IndexException;
import org.imixs.workflow.exceptions.QueryException;
import org.imixs.workflow.services.rest.BasicAuthenticator;
import org.imixs.workflow.services.rest.RestAPIException;
import org.imixs.workflow.services.rest.RestClient;

/**
 * The SolrIndexService provides methods to add, update and remove imixs documents from a solr
 * index.
 * <p>
 * The service validates the solr index schema and updates the schema it changed.
 * <p>
 * The SolrIndexService is used by the SolrUpdateService and the SolrSearchService which are
 * extending and implementing the Imix-Index concept.
 * <p>
 * The SolrIndexService can be configured by the following properties:
 * <p>
 * <ul>
 * <li>solr.api - api endpoint for the solr index</li>
 * <li>solr.core - name of the solr index core (default 'imixs-workflow')</li>
 * <li>solr.configset - an optinal solr configset (default '_default')</li>
 * <li>solr.user - userid for optional basic authentication</li>
 * <li>solr.password - password for optional basic authentication</li>
 * </ul>
 * 
 * @version 1.0
 * @author rsoika
 */
@DeclareRoles({"org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
    "org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
    "org.imixs.ACCESSLEVEL.MANAGERACCESS"})
@RolesAllowed({"org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
    "org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
    "org.imixs.ACCESSLEVEL.MANAGERACCESS"})
@Stateless
public class SolrIndexService {

  public static final int EVENTLOG_ENTRY_FLUSH_COUNT = 16;

  public static final String DEFAULT_SEARCH_FIELD = "_text_";
  public static final int DEFAULT_MAX_SEARCH_RESULT = 9999; // limiting the
                                                            // total
  // number of hits
  public static final int DEFAULT_PAGE_SIZE = 100; // default docs in one page

  @Inject
  @ConfigProperty(name = "solr.api", defaultValue = "http://solr:8983")
  private String api;

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

  @PersistenceContext(unitName = "org.imixs.workflow.jpa")
  EntityManager manager;


  private RestClient restClient;

  private static Logger logger = Logger.getLogger(SolrIndexService.class.getName());

  /**
   * Create a rest client instance
   */
  @PostConstruct
  public void init() {
    // create rest client
    restClient = new RestClient(api);
    if (user != null && !user.isEmpty()) {
      BasicAuthenticator authenticator = new BasicAuthenticator(user, password);
      restClient.registerRequestFilter(authenticator);
    }
  }

  /**
   * This method verifies the schema of the Solr core. If field definitions have change, a schema
   * update is posted to the Solr rest API.
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
      String existingSchema = restClient.get(api + "/api/cores/" + core + "/schema");
      logger.info("...core   - OK ");

      // update schema
      updateSchema(existingSchema);
    } catch (RestAPIException e) {
      // no schema found
      logger.severe("...no solr core '" + core + "' found - " + e.getMessage()
          + ": verify the solr instance!");
      throw e;
    }

  }

  /**
   * Updates the schema definition of an existing Solr core.
   * <p>
   * The schema definition is build by the method builUpdateSchema(). The updateSchema adds or
   * replaces field definitions depending on the fieldList definitions provided by the Imixs
   * SchemaService. See the method builUpdateSchema() for details.
   * <p>
   * The method asumes that a core already exits. Otherwise an exception is thrown.
   * 
   * @param schema - existing schema defintion
   * @return - an update Schema definition to be POST to the Solr rest api.
   * @throws RestAPIException
   */
  public void updateSchema(String schema) throws RestAPIException {
    boolean debug = logger.isLoggable(Level.FINE);
    // create the schema....
    String schemaUpdate = buildUpdateSchema(schema);
    // test if the schemaUdpate contains instructions....
    if (!"{}".equals(schemaUpdate)) {
      String uri = api + "/api/cores/" + core + "/schema";
      logger.info("...updating schema '" + core + "':");
      if (debug) {
        logger.finest("..." + schemaUpdate);
      }
      restClient.post(uri, schemaUpdate, "application/json");
      logger.info("...schema update - successfull ");
      // force rebuild index
      rebuildIndex();
    } else {
      logger.info("...schema - OK ");
    }
  }

  /**
   * This method adds a collection of documents to the Lucene Solr index. The documents are added
   * immediately to the index. Calling this method within a running transaction leads to a
   * uncommitted reads in the index. For transaction control, it is recommended to use instead the
   * the method solrUpdateService.updateDocuments() which takes care of uncommitted reads.
   * <p>
   * This method is used by the JobHandlerRebuildIndex only.
   * 
   * @param documents of ItemCollections to be indexed
   * @throws RestAPIException
   */
  public void indexDocuments(List<ItemCollection> documents) throws RestAPIException {
    long ltime = System.currentTimeMillis();
    boolean debug = logger.isLoggable(Level.FINE);
    if (documents == null || documents.size() == 0) {
      // no op!
      return;
    } else {

      String xmlRequest = buildAddDoc(documents);
      if (debug) {
        logger.finest(xmlRequest);
      }

      String uri = api + "/solr/" + core + "/update?commit=true";
      restClient.post(uri, xmlRequest, "text/xml");
    }

    if (debug) {
      logger.fine("... update index block in " + (System.currentTimeMillis() - ltime) + " ms ("
          + documents.size() + " workitems total)");
    }
  }

  /**
   * This method adds a single document to the Lucene Solr index. The document is added immediately
   * to the index. Calling this method within a running transaction leads to a uncommitted reads in
   * the index. For transaction control, it is recommended to use instead the the method
   * solrUpdateService.updateDocuments() which takes care of uncommitted reads.
   * 
   * @param documents of ItemCollections to be indexed
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
   * @param documents of collection of UniqueIDs to be removed from the index
   * @throws RestAPIException
   */
  public void removeDocuments(List<String> documentIDs) throws RestAPIException {
    boolean debug = logger.isLoggable(Level.FINE);
    long ltime = System.currentTimeMillis();

    if (documentIDs == null || documentIDs.size() == 0) {
      // no op!
      return;
    } else {
      StringBuffer xmlDelete = new StringBuffer();
      xmlDelete.append("<delete>");
      for (String id : documentIDs) {
        xmlDelete.append("<id>" + id + "</id>");
      }
      xmlDelete.append("</delete>");
      String xmlRequest = xmlDelete.toString();
      String uri = api + "/solr/" + core + "/update?commit=true";
      if (debug) {
          logger.finest("......delete documents '" + core + "':");
      }
      restClient.post(uri, xmlRequest, "text/xml");
    }

    if (debug) {
      logger.fine("... update index block in " + (System.currentTimeMillis() - ltime) + " ms ("
          + documentIDs.size() + " workitems total)");
    }
  }

  /**
   * This method removes a single document from the Lucene Solr index.
   * 
   * @param document - UniqueID of the document to be removed from the index
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
   * <p>
   * The method will return the documents containing all stored or DocValues fields. Only if the
   * param 'loadStubs' is false, then only the field '$uniqueid' will be returnded by the method.
   * The caller is responsible to load the full document from DocumentService.
   * <p>
   * Because fieldnames must not contain $ symbols we need to replace those field names used in a
   * query.
   * 
   * 
   * 
   * @param searchterm
   * @return
   * @throws QueryException
   */
  public String query(String searchTerm, int pageSize, int pageIndex, SortOrder sortOrder,
      DefaultOperator defaultOperator, boolean loadStubs) throws QueryException {
    boolean debug = logger.isLoggable(Level.FINE);
    if (debug) {
      logger.fine("...search solr index: " + searchTerm + "...");
    }
    StringBuffer uri = new StringBuffer();

    // URL Encode the query string....
    try {
      uri.append(api + "/solr/" + core + "/query");

      // set default operator?
      if (defaultOperator == DefaultOperator.OR) {
        uri.append("?q.op=" + defaultOperator);
      } else {
        // if not define we default in any case to AND
        uri.append("?q.op=AND");
      }

      // set sort order....
      if (sortOrder != null) {
        // sorted by sortoder
        String sortField = sortOrder.getField();
        // for Solr we need to replace the leading $ with _
        if (sortField.startsWith("$")) {
          sortField = "_" + sortField.substring(1);
        }
        if (sortOrder.isReverse()) {
          uri.append("&sort=" + sortField + "%20desc");
        } else {
          uri.append("&sort=" + sortField + "%20asc");
        }
      }

      // page size of 0 is allowed here - this will be used by the getTotalHits method
      // of the SolrSearchService
      if (pageSize < 0) {
        pageSize = DEFAULT_PAGE_SIZE;
      }

      if (pageIndex < 0) {
        pageIndex = 0;
      }

      uri.append("&rows=" + (pageSize));
      if (pageIndex > 0) {
        uri.append("&start=" + (pageIndex * pageSize));
      }

      // if loadStubs is true, then we only request the field '$uniqueid' here.
      if (!loadStubs) {
        uri.append("&fl=_uniqueid");
      }

      // append query
      uri.append("&q=" + URLEncoder.encode(searchTerm, "UTF-8"));
      if (debug) {
        logger.finest("...... uri=" + uri.toString());
      }
      String result = restClient.get(uri.toString());

      return result;
    } catch (RestAPIException | UnsupportedEncodingException e) {
      logger.severe("Solr search error: " + e.getMessage());
      throw new QueryException(QueryException.QUERY_NOT_UNDERSTANDABLE, e.getMessage(), e);
    }

  }

  /**
   * This method adapts an Solr field name to the corresponding Imixs Item name. Because Solr does
   * not accept $ char at the beginning of an field we need to replace starting _ with $ if the item
   * is part of the Imixs Index Schema.
   * 
   * @param itemName
   * @return adapted Imixs item name
   */
  public String adaptSolrFieldName(String itemName) {
    if (itemName == null || itemName.isEmpty() || schemaService == null) {
      return itemName;
    }
    if (itemName.charAt(0) == '_') {
      String adaptedName = "$" + itemName.substring(1);
      if (schemaService.getUniqueFieldList().contains(adaptedName)) {
        return adaptedName;
      }
    }
    return itemName;
  }

  /**
   * This method adapts an Imixs item name to the corresponding Solr field name. Because Solr does
   * not accept $ char at the beginning of an field we need to replace starting $ with _ if the item
   * is part of the Imixs Index Schema.
   * 
   * @param itemName
   * @return adapted Solr field name
   */
  public String adaptImixsItemName(String itemName) {
    if (itemName == null || itemName.isEmpty() || schemaService == null) {
      return itemName;
    }
    if (itemName.charAt(0) == '$') {
      if (schemaService.getUniqueFieldList().contains(itemName)) {
        String adaptedName = "_" + itemName.substring(1);
        return adaptedName;
      }
    }
    return itemName;
  }

  /**
   * This method builds a JSON structure to be used to update an existing Solr schema. The method
   * adds or replaces field definitions into a solr update schema.
   * <p>
   * The param oldSchema contains the current schema definition of the core.
   * <p>
   * In Solr there a two field types defining if the value of a field is stored and returned by a
   * <p>
   * <code>{"add-field":{name=field1, type=text_general, stored=true, docValues=true}}</code>
   * <p>
   * For both cases the values are stored in the lucene index and returned by a query.
   * </p>
   * <p>
   * Stored fields (stored=true) are row orientated. That means that like in a sql table the values
   * are stored based on the ID of the document.
   * <p>
   * In difference the docValues are stored column orientated (forward index). The values are
   * ordered based on the search term. For features like sorting, grouping or faceting, docValues
   * increase the performance in general. So it may look like docValues are the better choice. But
   * one important different is how the values are stored. In case of a stored field with
   * multi-values, the values are exactly stored in the same order as they were indexed. DocValues
   * instead are sorted and reordered. So this will falsify the result of a document returned by a
   * query.
   * <p>
   * <strong>In Imixs-Workflow we use the stored attribute to return parts of a document at query
   * time. We call this a document-stub which contains only a subset of fields. Later we load the
   * full document from the SQL database. As stored fields in our workflow application are also
   * often used for sorting we combine both attributes. In case of a non-stored field we set also
   * docValues=false to avoid unnecessary storing of fields. </strong>
   * 
   * @see https://lucene.apache.org/solr/guide/8_0/docvalues.html
   * @return
   */
  protected String buildUpdateSchema(String oldSchema) {
    boolean debug = logger.isLoggable(Level.FINE);
    StringBuffer updateSchema = new StringBuffer();
    List<String> fieldListStore = schemaService.getFieldListStore();
    List<String> fieldListAnalyze = schemaService.getFieldListAnalyze();
    List<String> fieldListNoAnalyze = schemaService.getFieldListNoAnalyze();

    // remove white space from oldSchema to simplify string compare...
    oldSchema = oldSchema.replace(" ", "");

    if (debug) {
      logger.finest("......old schema=" + oldSchema);
    }
    updateSchema.append("{");

    // finally add the default content field
    addFieldDefinitionToUpdateSchema(updateSchema, oldSchema, DEFAULT_SEARCH_FIELD, "text_general",
        false, false);
    // add each field from the fieldListAnalyze
    for (String field : fieldListAnalyze) {
      boolean store = fieldListStore.contains(field);
      // text_general - docValues are not supported!
      addFieldDefinitionToUpdateSchema(updateSchema, oldSchema, field, "text_general", store,
          false);
    }

    // add each field from the fieldListNoAnalyze
    for (String field : fieldListNoAnalyze) {
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
    boolean debug = logger.isLoggable(Level.FINE);
    List<String> fieldList = schemaService.getFieldList();
    List<String> fieldListAnalyze = schemaService.getFieldListAnalyze();
    List<String> fieldListNoAnalyze = schemaService.getFieldListNoAnalyze();
    SimpleDateFormat dateformat = new SimpleDateFormat("yyyyMMddHHmmss");

    StringBuffer xmlContent = new StringBuffer();

    xmlContent.append("<add overwrite=\"true\">");

    for (ItemCollection document : documents) {

      // if no UniqueID is defined we need to skip this document
      if (document.getUniqueID().isEmpty()) {
        continue;
      }

      xmlContent.append("<doc>");

      xmlContent.append("<field name=\"id\">" + document.getUniqueID() + "</field>");

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
      if (debug) {
        logger.finest("......add index field " + DEFAULT_SEARCH_FIELD + "=" + content);
      }
      // remove existing CDATA...
      content = stripCDATA(content);
      // strip control codes..
      content = stripControlCodes(content);
      // We need to add a wrapping CDATA, allow xml in general..
      xmlContent.append(
          "<field name=\"" + DEFAULT_SEARCH_FIELD + "\"><![CDATA[" + content + "]]></field>");

      // now add all analyzed fields...
      for (String aFieldname : fieldListAnalyze) {
        addFieldValuesToUpdateRequest(xmlContent, document, aFieldname);
      }
      // now add all notanalyzed fields...
      for (String aFieldname : fieldListNoAnalyze) {
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
   * This helper method is to strip control codes and extended characters from a string. We can not
   * put those chars into the XML request send to solr.
   * <p>
   * Background:
   * <p>
   * In ASCII, the control codes have decimal codes 0 through to 31 and 127. On an ASCII based
   * system, if the control codes are stripped, the resultant string would have all of its
   * characters within the range of 32 to 126 decimal on the ASCII table.
   * <p>
   * On a non-ASCII based system, we consider characters that do not have a corresponding glyph on
   * the ASCII table (within the ASCII range of 32 to 126 decimal) to be an extended character for
   * the purpose of this task.
   * </p>
   * 
   * @see https://rosettacode.org/wiki/Strip_control_codes_and_extended_characters_from_a_string
   * 
   * @param s
   * @param include
   * @return
   */
  protected String stripControlCodes(String s) {

    // control codes stripped (but extended characters not stripped)
    // IntPredicate include=c -> c > '\u001F' && c != '\u007F';

    // control codes and extended characters stripped
    IntPredicate include = c -> c > '\u001F' && c < '\u007F';
    return s.codePoints().filter(include::test)
        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
        .toString();
  }

  /**
   * This helper method strips CDATA blocks from a string. We can not post embedded CDATA in an
   * alredy existing CDATA when we post the xml to solr.
   * <p>
   * 
   * @param s
   * @return
   */
  protected String stripCDATA(String s) {

    if (s.contains("<![CDATA[")) {
      String result = s.replaceAll("<!\\[CDATA\\[", "");
      result = result.replaceAll("]]>", "");
      return result;
    } else {
      return s;
    }
  }



  /**
   * This method flushes a given count of eventLogEntries. The method return true if no more
   * eventLogEntries exist.
   * 
   * @param count the max size of a eventLog engries to remove.
   * @return true if the cache was totally flushed.
   */
  @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
  public boolean flushEventLogByCount(int count) {
    boolean debug = logger.isLoggable(Level.FINE);
    Date lastEventDate = null;
    boolean cacheIsEmpty = true;

    long l = System.currentTimeMillis();
    if (debug) {
      logger.finest("......flush eventlog cache....");
    }
    List<EventLog> events = eventLogService.findEventsByTopic(count + 1,
        DocumentService.EVENTLOG_TOPIC_INDEX_ADD, DocumentService.EVENTLOG_TOPIC_INDEX_REMOVE);

    if (events != null && events.size() > 0) {
      try {

        int _counter = 0;
        for (EventLog eventLogEntry : events) {

          // lookup the Document Entity...
          org.imixs.workflow.engine.jpa.Document doc =
              manager.find(org.imixs.workflow.engine.jpa.Document.class, eventLogEntry.getRef());

          // if the document was found we add/update the index. Otherwise we remove the
          // document form the index.
          if (doc != null
              && DocumentService.EVENTLOG_TOPIC_INDEX_ADD.equals(eventLogEntry.getTopic())) {
            // add workitem to search index....
            long l2 = System.currentTimeMillis();
            ItemCollection workitem = new ItemCollection();
            workitem.setAllItems(doc.getData());
            if (!workitem.getItemValueBoolean(DocumentService.NOINDEX)) {
              indexDocument(workitem);
              if (debug) {
                logger.finest("......solr added workitem '" + eventLogEntry.getRef()
                    + "' to index in " + (System.currentTimeMillis() - l2) + "ms");
              }
            }
          } else {
            long l2 = System.currentTimeMillis();
            removeDocument(eventLogEntry.getRef());
            if (debug) {
              logger.finest("......solr removed workitem '" + eventLogEntry.getRef()
                  + "' from index in " + (System.currentTimeMillis() - l2) + "ms");
            }
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

    if (debug) {
      logger.fine("...flushEventLog - " + events.size() + " events in "
          + (System.currentTimeMillis() - l) + " ms - last log entry: " + lastEventDate);
    }
    return cacheIsEmpty;

  }

  /**
   * Flush the EventLog cache. This method is called by the LuceneSerachService only.
   * <p>
   * The method flushes the cache in smaller blocks of the given junkSize. to avoid a heap size
   * problem. The default flush size is 16. The eventLog cache is tracked by the flag 'dirtyIndex'.
   * <p>
   * issue #439 - The method returns false if the event log contains more entries as defined by the
   * given JunkSize. In this case the caller should recall the method which runs always in a new
   * transaction. The goal of this mechanism is to reduce the event log even in cases the outer
   * transaction breaks.
   * 
   * @see LuceneSearchService
   * @return true if the the complete event log was flushed. If false the method must be recalled.
   */
  @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
  public boolean flushEventLog(int junkSize) {
    boolean debug = logger.isLoggable(Level.FINE);
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
          if (count >= 100 && debug) {
            logger.finest("...flush event log: " + total + " entries in "
                + (System.currentTimeMillis() - l) + "ms...");
            count = 0;
          }

          // issue #439
          // In some cases the flush method runs endless.
          // experimental code: we break the flush method after 1024 flushs
          // maybe we can remove this hard break
          if (total >= junkSize) {
            if (debug) {
              logger.finest("...flush event: Issue #439  -> total count >=" + total
                  + " flushEventLog will be continued...");
            }
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


  /**
   * This method adds a field definition object to an updateSchema.
   * <p>
   * In case the same field already exists in the oldSchema then the method will replace the field.
   * In case id does not exist, the field definition is added to the update schema.
   * <p>
   * Example:
   * <p>
   * <code>add-field:{name:"$workflowsummary", type:"text_general", stored:true, docValues:false}</code><br
   * />
   * <code>replace-field:{name:"$workflowstatus", type:"strings", stored:true, docValues:true}</code>
   * <p>
   * To verify the existence of the field we parse the fieldname in the old schema definition.
   * <p>
   * Note: In Solr field names must not start with $ symbol. For that reason we adapt the $ with _
   * for all known index fields
   *
   * @param updateSchema - a stringBuffer to build the update schema
   * @param oldSchema    - the existing schema definition
   * @param name         - field name
   * @param type         - field type
   * @param store        - boolean store field
   * @param docValue     - true if docValues should be set to true
   * 
   */
  private void addFieldDefinitionToUpdateSchema(StringBuffer updateSchema, String oldSchema,
      String _name, String type, boolean store, boolean docvalue) {

    // replace $ with _
    String name = adaptImixsItemName(_name);

    String fieldDefinition = "{\"name\":\"" + name + "\",\"type\":\"" + type + "\",\"stored\":"
        + store + ",\"docValues\":" + docvalue + "}";

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
   * <p>
   * In case the value is a date or calendar object, then the value will be converted into a lucene
   * time format.
   * <p>
   * The value will always be wrapped with a CDATA tag to avoid invalid XML.
   * 
   * @param doc       an existing lucene document
   * @param workitem  the workitem containing the values
   * @param _itemName the item name inside the workitem
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

      // remove existing CDATA...
      convertedValue = stripCDATA(convertedValue);
      // strip control codes..
      convertedValue = stripControlCodes(convertedValue);
      // wrapp value into CDATA
      convertedValue = "<![CDATA[" + stripControlCodes(convertedValue) + "]]>";

      xmlContent.append(
          "<field name=\"" + adaptImixsItemName(itemName) + "\">" + convertedValue + "</field>");
    }

  }

}
