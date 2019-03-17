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

package org.imixs.workflow.engine.lucene;

import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.ClassicAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.PropertyService;
import org.imixs.workflow.exceptions.IndexException;
import org.imixs.workflow.exceptions.PluginException;

/**
 * The LuceneUpdateService provides methods to write Imixs Workitems into a
 * Lucene search index. With the method <code>addWorkitem()</code> a
 * ItemCollection can be added to a lucene search index. The service init method
 * reads the property file 'imixs.properties' from the current classpath to
 * determine the configuration.
 * 
 * <ul>
 * <li>The property "IndexDir" defines the location of the lucene index
 * <li>The property "FulltextFieldList" lists all fields which should be
 * searchable after a workitem was updated
 * <li>The property "IndexFieldList" lists all fields which should be indexed as
 * keywords by the lucene search engine
 * </ul>
 * 
 * The singleton pattern is used to avoid conflicts within multi-thread
 * scenarios. The service is used by the LucenPlugin to update the lucene index
 * during a workflow processing step.
 * 
 * 
 * @see http://stackoverflow.com/questions/34880347/why-did-lucene-indexwriter-
 *      did-not-update-the-index-when-called-from-a-web-modul
 * @see LucenePlugin
 * @version 1.2
 * @author rsoika
 */
@Singleton
public class LuceneUpdateService {

	protected static final String DEFAULT_ANALYSER = ClassicAnalyzer.class.getName();
	protected static final String DEFAULT_INDEX_DIRECTORY = "imixs-workflow-index";
	protected static final String ANONYMOUS = "ANONYMOUS";

	public static final String EVENTLOG_TYPE_ADD = "lucene_event_add";
	public static final String EVENTLOG_TYPE_REMOVE = "lucene_event_remove";
	protected static final String EVENTLOG_ID_PRAFIX = "lucene_event_id_";
	protected static final int EVENTLOG_ENTRY_FLUSH_COUNT = 16;

	private List<String> searchFieldList = null;
	private List<String> indexFieldListAnalyse = null;
	private List<String> indexFieldListNoAnalyse = null;
	private String indexDirectoryPath = null;
	private String analyserClass = null;
	private Properties properties = null;

	// default field lists
	private static List<String> DEFAULT_SEARCH_FIELD_LIST = Arrays.asList("$workflowsummary", "$workflowabstract");
	private static List<String> DEFAULT_NOANALYSE_FIELD_LIST = Arrays.asList("$modelversion", "$taskid", "$processid",
			"$workitemid", "$uniqueidref", "type", "$writeaccess", "$modified", "$created", "namcreator", "$creator",
			"$editor", "$lasteditor", "$workflowgroup", "$workflowstatus", "txtworkflowgroup", "txtname", "namowner",
			"txtworkitemref", "$uniqueidsource", "$uniqueidversions", "$lasttask", "$lastevent", "$lasteventdate");

	@EJB
	PropertyService propertyService;

	@PersistenceContext(unitName = "org.imixs.workflow.jpa")
	private EntityManager manager;

	@Inject
	LuceneItemAdapter luceneItemAdapter;

	private static Logger logger = Logger.getLogger(LuceneUpdateService.class.getName());

	/**
	 * PostContruct event - The method loads the lucene index properties from the
	 * imixs.properties file from the classpath. If no properties are defined the
	 * method terminates.
	 * 
	 */
	@PostConstruct
	void init() {

		// read configuration
		properties = propertyService.getProperties();
		indexDirectoryPath = properties.getProperty("lucence.indexDir", DEFAULT_INDEX_DIRECTORY);
		// luceneLockFactory = properties.getProperty("lucence.lockFactory");
		// get Analyzer Class -
		// default=org.apache.lucene.analysis.standard.ClassicAnalyzer
		analyserClass = properties.getProperty("lucence.analyzerClass", DEFAULT_ANALYSER);

		String sFulltextFieldList = properties.getProperty("lucence.fulltextFieldList");
		String sIndexFieldListAnalyse = properties.getProperty("lucence.indexFieldListAnalyze");
		String sIndexFieldListNoAnalyse = properties.getProperty("lucence.indexFieldListNoAnalyze");

		logger.finest("......lucene IndexDir=" + indexDirectoryPath);
		logger.finest("......lucene FulltextFieldList=" + sFulltextFieldList);
		logger.finest("......lucene IndexFieldListAnalyse=" + sIndexFieldListAnalyse);
		logger.finest("......lucene IndexFieldListNoAnalyse=" + sIndexFieldListNoAnalyse);

		// compute search field list
		searchFieldList = new ArrayList<String>();
		// add all static default field list
		searchFieldList.addAll(DEFAULT_SEARCH_FIELD_LIST);
		if (sFulltextFieldList != null && !sFulltextFieldList.isEmpty()) {
			StringTokenizer st = new StringTokenizer(sFulltextFieldList, ",");
			while (st.hasMoreElements()) {
				String sName = st.nextToken().toLowerCase().trim();
				// do not add internal fields
				if (!"$uniqueid".equals(sName) && !"$readaccess".equals(sName) && !searchFieldList.contains(sName))
					searchFieldList.add(sName);
			}
		}

		// compute Index field list (Analyze)
		indexFieldListAnalyse = new ArrayList<String>();
		if (sIndexFieldListAnalyse != null && !sIndexFieldListAnalyse.isEmpty()) {
			StringTokenizer st = new StringTokenizer(sIndexFieldListAnalyse, ",");
			while (st.hasMoreElements()) {
				String sName = st.nextToken().toLowerCase().trim();
				// do not add internal fields
				if (!"$uniqueid".equals(sName) && !"$readaccess".equals(sName))
					indexFieldListAnalyse.add(sName);
			}
		}

		// compute Index field list (NoAnalyze)

		indexFieldListNoAnalyse = new ArrayList<String>();
		// add all static default field list
		indexFieldListNoAnalyse.addAll(DEFAULT_NOANALYSE_FIELD_LIST);
		if (sIndexFieldListNoAnalyse != null && !sIndexFieldListNoAnalyse.isEmpty()) {
			// add additional field list from imixs.properties
			StringTokenizer st = new StringTokenizer(sIndexFieldListNoAnalyse, ",");
			while (st.hasMoreElements()) {
				String sName = st.nextToken().toLowerCase().trim();
				if (!indexFieldListNoAnalyse.contains(sName))
					indexFieldListNoAnalyse.add(sName);
			}
		}
	}

	/**
	 * Returns the Lucene configuration
	 * 
	 * @return
	 */
	public ItemCollection getConfiguration() {
		ItemCollection config = new ItemCollection();

		config.replaceItemValue("lucence.indexDir", indexDirectoryPath);
		// config.replaceItemValue("lucence.lockFactory", luceneLockFactory);
		config.replaceItemValue("lucence.analyzerClass", analyserClass);
		config.replaceItemValue("lucence.fulltextFieldList", searchFieldList);
		config.replaceItemValue("lucence.indexFieldListAnalyze", indexFieldListAnalyse);
		config.replaceItemValue("lucence.indexFieldListNoAnalyze", indexFieldListNoAnalyse);

		return config;
	}

	/**
	 * This method adds a single document into the to the Lucene index. Before the
	 * document is added to the index, a new eventLogEntry is created. The document
	 * will be indexed after the method flushEventLog is called. This method is
	 * called by the LuceneSearchService finder methods.
	 * <p>
	 * The method supports committed read. This means that a running transaction
	 * will not read an uncommitted document from the Lucene index.
	 * 
	 * 
	 * @param documentContext
	 */
	public void updateDocument(ItemCollection documentContext) {
		// adds the document into a empty Collection and call the method
		// updateDocuments.
		List<ItemCollection> documents = new ArrayList<ItemCollection>();
		documents.add(documentContext);
		updateDocuments(documents);
	}

	/**
	 * This method adds a collection of documents to the Lucene index. For each
	 * document in a given selection a new eventLogEntry is created. The documents
	 * will be indexed after the method flushEventLog is called. This method is
	 * called by the LuceneSearchService finder methods.
	 * <p>
	 * The method supports committed read. This means that a running transaction
	 * will not read uncommitted documents from the Lucene index.
	 * 
	 * @see updateDocumentsUncommitted
	 * @param documents
	 *            to be indexed
	 * @throws IndexException
	 */
	public void updateDocuments(Collection<ItemCollection> documents) {
		long ltime = System.currentTimeMillis();

		// write a new EventLog entry for each document....
		for (ItemCollection workitem : documents) {
			// skip if the flag 'noindex' = true
			if (!workitem.getItemValueBoolean(DocumentService.NOINDEX)) {
				writeEventLogEntry(workitem.getUniqueID(), EVENTLOG_TYPE_ADD);
			}
		}

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("... update eventLog cache in " + (System.currentTimeMillis() - ltime) + " ms ("
					+ documents.size() + " documents to be index)");
		}
	}

	/**
	 * This method adds a collection of documents to the Lucene index. The documents
	 * are added immediately to the index. Calling this method within a running
	 * transaction leads to a uncommitted reads in the index. For transaction
	 * control, it is recommended to use instead the the method updateDocumetns()
	 * which takes care of uncommitted reads.
	 * <p>
	 * This method is used by the JobHandlerRebuildIndex only.
	 * 
	 * @param documents
	 *            of ItemCollections to be indexed
	 * @throws IndexException
	 */
	public void updateDocumentsUncommitted(Collection<ItemCollection> documents) {

		IndexWriter awriter = null;
		long ltime = System.currentTimeMillis();
		try {
			awriter = createIndexWriter();
			// add workitem to search index....
			for (ItemCollection workitem : documents) {

				if (!workitem.getItemValueBoolean(DocumentService.NOINDEX)) {
					// create term
					Term term = new Term("$uniqueid", workitem.getItemValueString("$uniqueid"));
					logger.finest("......lucene add/update uncommitted workitem '"
							+ workitem.getItemValueString(WorkflowKernel.UNIQUEID) + "' to index...");
					awriter.updateDocument(term, createDocument(workitem));
				}
			}
		} catch (IOException luceneEx) {
			logger.warning("lucene error: " + luceneEx.getMessage());
			throw new IndexException(IndexException.INVALID_INDEX, "Unable to update lucene search index", luceneEx);
		} finally {
			// close writer!
			if (awriter != null) {
				logger.finest("......lucene close IndexWriter...");
				try {
					awriter.close();
				} catch (CorruptIndexException e) {
					throw new IndexException(IndexException.INVALID_INDEX, "Unable to close lucene IndexWriter: ", e);
				} catch (IOException e) {
					throw new IndexException(IndexException.INVALID_INDEX, "Unable to close lucene IndexWriter: ", e);
				}
			}
		}

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("... update index block in " + (System.currentTimeMillis() - ltime) + " ms (" + documents.size()
					+ " workitems total)");
		}
	}

	/**
	 * This method adds a new eventLogEntry for a document to be deleted from the
	 * index. The document will be removed from the index after the method
	 * fluschEventLog is called. This method is called by the LuceneSearchService
	 * finder method only.
	 * 
	 * 
	 * @param uniqueID
	 *            of the workitem to be removed
	 * @throws PluginException
	 */
	public void removeDocument(String uniqueID) {
		long ltime = System.currentTimeMillis();
		writeEventLogEntry(uniqueID, EVENTLOG_TYPE_REMOVE);
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("... update eventLog cache in " + (System.currentTimeMillis() - ltime)
					+ " ms (1 document to be removed)");
		}
	}

	/**
	 * Flush the EventLog cache. This method is called by the LuceneSerachServicen
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

	/**
	 * This method flushes a given count of eventLogEntries. The method return true
	 * if no more eventLogEntries exist.
	 * 
	 * @param count
	 *            the max size of a eventLog engries to remove.
	 * @return true if the cache was totally flushed.
	 */
	boolean flushEventLogByCount(int count) {
		boolean cacheIsEmpty = true;
		IndexWriter indexWriter = null;
		long l = System.currentTimeMillis();
		logger.finest("......flush eventlog cache....");

		String query = "SELECT document FROM Document AS document ";
		query += "WHERE document.type IN ('" + EVENTLOG_TYPE_ADD + "','" + EVENTLOG_TYPE_REMOVE
				+ "') ORDER BY document.created ASC";

		// find all eventLogEntries....
		Query q = manager.createQuery(query);
		// we try to search one more log entry as requested to see if the cache is
		// empty...
		q.setMaxResults(count + 1);

		@SuppressWarnings("unchecked")
		Collection<org.imixs.workflow.engine.jpa.Document> documentList = q.getResultList();
		if (documentList != null && documentList.size() > 0) {
			try {
				indexWriter = createIndexWriter();
				int _counter = 0;
				for (org.imixs.workflow.engine.jpa.Document eventLogEntry : documentList) {

					String id = eventLogEntry.getId();
					// cut prafix...
					id = id.substring(EVENTLOG_ID_PRAFIX.length());
					id = id.substring(id.indexOf("]_") + 2);
					// lookup the workitem...
					org.imixs.workflow.engine.jpa.Document doc = manager
							.find(org.imixs.workflow.engine.jpa.Document.class, id);
					Term term = new Term("$uniqueid", id);

					// if the document was found we add/update the index. Otherwise we remove the
					// document form the index.
					if (doc != null && EVENTLOG_TYPE_ADD.equals(eventLogEntry.getType())) {
						// add workitem to search index....
						long l2 = System.currentTimeMillis();
						ItemCollection workitem = new ItemCollection();
						workitem.setAllItems(doc.getData());
						if (!workitem.getItemValueBoolean(DocumentService.NOINDEX)) {
							indexWriter.updateDocument(term, createDocument(workitem));
							logger.finest("......lucene add/update workitem '" + id + "' to index in "
									+ (System.currentTimeMillis() - l2) + "ms");
						}
					} else {
						long l2 = System.currentTimeMillis();
						indexWriter.deleteDocuments(term);
						logger.finest("......lucene remove workitem '" + id + "' from index in "
								+ (System.currentTimeMillis() - l2) + "ms");
					}

					// remove the eventLogEntry.
					manager.remove(eventLogEntry);

					// break?
					_counter++;
					if (_counter >= count) {
						// we skipp the last one if the maximum was reached.
						cacheIsEmpty = false;
						break;
					}
				}
			} catch (IOException luceneEx) {
				logger.warning("...unable to flush lucene event log: " + luceneEx.getMessage());
				// We just log a warning here and close the flush mode to no longer block the
				// writer.
				// NOTE: maybe throwing a IndexException would be an alternative:
				//
				// throw new IndexException(IndexException.INVALID_INDEX, "Unable to update
				// lucene search index",
				// luceneEx);
				return true;
			} finally {
				// close writer!
				if (indexWriter != null) {
					logger.finest("......lucene close IndexWriter...");
					try {
						indexWriter.close();
					} catch (CorruptIndexException e) {
						throw new IndexException(IndexException.INVALID_INDEX, "Unable to close lucene IndexWriter: ",
								e);
					} catch (IOException e) {
						throw new IndexException(IndexException.INVALID_INDEX, "Unable to close lucene IndexWriter: ",
								e);
					}
				}
			}
		}

		logger.fine("...flushEventLog - " + documentList.size() + " documents in " + (System.currentTimeMillis() - l)
				+ " ms");

		return cacheIsEmpty;

	}

	/**
	 * This method creates/updates an event log entry to indicate an uncommitted
	 * index update. This method is called by "updateDocuments".
	 * 
	 * The identifier of an eventLogEnty is sufixed with
	 * "_EVENT_LOG_ENTRY[EVENTUID]". The type of the document entity will be set to
	 * 'eventlogentry'.
	 * 
	 * @param id
	 *            - uniqueid of the document to update
	 * @param type
	 *            EVENTLOG_ENTRY_TYPE_ADD or EVENTLOG_ENTRY_TYPE_REMOVE
	 */
	void writeEventLogEntry(String id, String type) {
		org.imixs.workflow.engine.jpa.Document eventLogEntry = null;
		if (id == null || id.isEmpty()) {
			logger.warning("WriteEventLog failed - given id is empty!");
			return;
		}

		// Now set flush Mode to COMMIT
		manager.setFlushMode(FlushModeType.COMMIT);

		// now create a new event log entry
		id = EVENTLOG_ID_PRAFIX + "[" + generateEventUID() + "]_" + id;
		eventLogEntry = new org.imixs.workflow.engine.jpa.Document(id);
		eventLogEntry.setType(type);
		logger.finest("......create new eventLogEntry '" + id + "' => " + type);
		manager.persist(eventLogEntry);
	}

	/**
	 * This method creates a lucene document based on a ItemCollection. The Method
	 * creates for each field specified in the FieldList a separate index field for
	 * the lucene document.
	 * 
	 * The property 'AnalyzeIndexFields' defines if a indexfield value should by
	 * analyzed by the Lucene Analyzer (default=false)
	 * 
	 * @param aworkitem
	 * @return
	 */
	@SuppressWarnings("unchecked")
	Document createDocument(ItemCollection aworkitem) {
		String sValue = null;
		Document doc = new Document();
		// combine all search fields from the search field list into one field
		// ('content') for the lucene document
		String sContent = "";
		for (String aFieldname : searchFieldList) {
			sValue = "";
			// check value list - skip empty fields
			List<?> vValues = aworkitem.getItemValue(aFieldname);
			if (vValues.size() == 0)
				continue;
			// get all values of a value list field
			for (Object o : vValues) {
				if (o == null)
					// skip null values
					continue;

				if (o instanceof Calendar || o instanceof Date) {
					SimpleDateFormat dateformat = new SimpleDateFormat("yyyyMMddHHmmss");
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
				sContent += sValue + ",";
			}
		}
		logger.finest("......add lucene field content=" + sContent);
		doc.add(new TextField("content", sContent, Store.NO));

		// add each field from the indexFieldList into the lucene document

		// analyzed...
		for (String aFieldname : indexFieldListAnalyse) {
			addItemValues(doc, aworkitem, aFieldname, true);
		}
		// ... and not analyzed...
		for (String aFieldname : indexFieldListNoAnalyse) {
			addItemValues(doc, aworkitem, aFieldname, false);
		}

		// add $uniqueid not analyzed
		doc.add(new StringField("$uniqueid", aworkitem.getItemValueString("$uniqueid"), Store.YES));

		// add $readAccess not analyzed
		List<String> vReadAccess = (List<String>) aworkitem.getItemValue("$readAccess");
		if (vReadAccess.size() == 0 || (vReadAccess.size() == 1 && "".equals(vReadAccess.get(0).toString()))) {
			// if emtpy add the ANONYMOUS default entry
			sValue = ANONYMOUS;
			doc.add(new StringField("$readaccess", sValue, Store.NO));
		} else {
			sValue = "";
			// add each role / username as a single field value
			for (String sReader : vReadAccess) {
				doc.add(new StringField("$readaccess", sReader, Store.NO));
			}

		}
		return doc;
	}

	/**
	 * adds a field value into a lucene document
	 * 
	 * @param doc
	 *            an existing lucene document
	 * @param workitem
	 *            the workitem containg the values
	 * @param itemName
	 *            the Fieldname inside the workitem
	 * @param analyzeValue
	 *            indicates if the value should be parsed by the analyzer
	 */
	void addItemValues(Document doc, ItemCollection workitem, String itemName, boolean analyzeValue) {

		if (itemName == null) {
			return;
		}
		// item name must be LowerCased and trimmed because of later usage in
		// doc.add(...)
		itemName = itemName.toLowerCase().trim();

		List<?> vValues = workitem.getItemValue(itemName);
		if (vValues.size() == 0) {
			return;
		}
		if (vValues.get(0) == null) {
			return;
		}

		boolean firstValue = true;
		for (Object singleValue : vValues) {

			IndexableField indexableField = luceneItemAdapter.adaptItemValue(itemName, singleValue, analyzeValue);

			doc.add(indexableField);

			// we only add the first value of a multiValue field into the
			// sort index, because it seems not to make any sense to sort a
			// result set by multi-values.
			if (!analyzeValue && firstValue == true) {
				SortedDocValuesField sortedDocField = luceneItemAdapter.adaptSortableItemValue(itemName, singleValue);
				doc.add(sortedDocField);
			}

			firstValue = false;
		}

	}

	/**
	 * adds a field value into a lucene document
	 * 
	 * @param doc
	 *            an existing lucene document
	 * @param workitem
	 *            the workitem containg the values
	 * @param itemName
	 *            the Fieldname inside the workitem
	 * @param analyzeValue
	 *            indicates if the value should be parsed by the analyzer
	 */
	@Deprecated
	void addItemValuesOld(Document doc, ItemCollection workitem, String itemName, boolean analyzeValue) {
		String sValue = null;

		if (itemName == null) {
			return;
		}
		// item name must be LowerCased and trimmed because of later usage in
		// doc.add(...)
		itemName = itemName.toLowerCase().trim();

		List<?> vValues = workitem.getItemValue(itemName);
		if (vValues.size() == 0) {
			return;
		}
		if (vValues.get(0) == null) {
			return;
		}

		boolean firstValue = true;
		for (Object singleValue : vValues) {

			// Object o = vValues.firstElement();
			if (singleValue instanceof Calendar || singleValue instanceof Date) {
				SimpleDateFormat dateformat = new SimpleDateFormat("yyyyMMddHHmmss");

				// convert calendar to string
				String sDateValue;
				if (singleValue instanceof Calendar) {
					sDateValue = dateformat.format(((Calendar) singleValue).getTime());
				} else {
					sDateValue = dateformat.format((Date) singleValue);
				}
				sValue = sDateValue;

			} else {
				// simple string representation
				sValue = singleValue.toString();
			}

			logger.finest("......lucene add IndexField (analyse=" + analyzeValue + "): " + itemName + "=" + sValue);
			if (analyzeValue) {
				doc.add(new TextField(itemName, sValue, Store.NO));
			} else {
				// do not analyze content of index fields!
				doc.add(new StringField(itemName, sValue, Store.NO));

				// we only add the first value of a multiValue field into the
				// sort index, because it seems not to make any sense to sort a
				// result set by multivalues.
				// since lucene 5 we create an additional sortedSet field..
				// doc.add(new SortedSetDocValuesField(itemName, new
				// BytesRef(sValue)));
				if (firstValue) {
					doc.add(new SortedDocValuesField(itemName, new BytesRef(sValue)));
				}
			}

			firstValue = false;
		}

	}

	/**
	 * This method creates a new instance of a lucene IndexWriter.
	 * 
	 * The location of the lucene index in the filesystem is read from the
	 * imixs.properties
	 * 
	 * @return
	 * @throws IOException
	 */
	IndexWriter createIndexWriter() throws IOException {
		// create a IndexWriter Instance
		Directory indexDir = FSDirectory.open(Paths.get(indexDirectoryPath));
		IndexWriterConfig indexWriterConfig;

		// indexWriterConfig = new IndexWriterConfig(new ClassicAnalyzer());
		try {
			// issue #429
			indexWriterConfig = new IndexWriterConfig((Analyzer) Class.forName(analyserClass).newInstance());
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			throw new IndexException(IndexException.INVALID_INDEX, "Unable to create analyzer '" + analyserClass + "'",
					e);
		}

		return new IndexWriter(indexDir, indexWriterConfig);
	}

	/**
	 * Generates UID on currentTimeMillis + 6 digits of a random number. The result
	 * will be converted into a hex string.
	 * 
	 * @return hexstring
	 */
	static String generateEventUID() {
		int randomNum = ThreadLocalRandom.current().nextInt(10000, 99999 + 1);
		return "" + Long.toHexString(System.currentTimeMillis()) + "-" + Integer.toHexString(randomNum);
	}
}
