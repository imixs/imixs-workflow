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
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.ClassicAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.EventLogService;
import org.imixs.workflow.engine.adminp.AdminPService;
import org.imixs.workflow.engine.jpa.EventLog;
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

	protected static final String DEFAULT_ANALYSER = "org.apache.lucene.analysis.standard.ClassicAnalyzer";
	protected static final String DEFAULT_INDEX_DIRECTORY = "imixs-workflow-index";
	protected static final String ANONYMOUS = "ANONYMOUS";

	public static final String EVENTLOG_TOPIC_ADD = "lucene.add";
	public static final String EVENTLOG_TOPIC_REMOVE = "lucene.remove";
	protected static final int EVENTLOG_ENTRY_FLUSH_COUNT = 16;

	@Inject
	@ConfigProperty(name = "lucence.indexDir", defaultValue = LuceneUpdateService.DEFAULT_INDEX_DIRECTORY)
	private String luceneIndexDir;

	@Inject
	@ConfigProperty(name = "lucence.analyzerClass", defaultValue = LuceneUpdateService.DEFAULT_ANALYSER)
	private String luceneAnalyserClass;

	@Inject
	@ConfigProperty(name = "lucence.fulltextFieldList", defaultValue = "")
	private String luceneFulltextFieldList;

	@Inject
	@ConfigProperty(name = "lucence.indexFieldListAnalyze", defaultValue = "")
	private String luceneIndexFieldListAnalyse;

	@Inject
	@ConfigProperty(name = "lucence.indexFieldListNoAnalyze", defaultValue = "")
	private String luceneIndexFieldListNoAnalyse;

	@Inject
	@ConfigProperty(name = "lucence.indexFieldListStore", defaultValue = "")
	private String luceneIndexFieldListStore;

	private List<String> searchFieldList = null;
	private List<String> indexFieldListAnalyse = null;
	private List<String> indexFieldListNoAnalyse = null;
	private List<String> indexFieldListStore = null;

	// default field lists
	private static List<String> DEFAULT_SEARCH_FIELD_LIST = Arrays.asList("$workflowsummary", "$workflowabstract");
	private static List<String> DEFAULT_NOANALYSE_FIELD_LIST = Arrays.asList("$modelversion", "$taskid", "$processid",
			"$workitemid", "$uniqueidref", "type", "$writeaccess", "$modified", "$created", "namcreator", "$creator",
			"$editor", "$lasteditor", "$workflowgroup", "$workflowstatus", "txtworkflowgroup", "name", "txtname",
			"namowner", "txtworkitemref", "$uniqueidsource", "$uniqueidversions", "$lasttask", "$lastevent",
			"$lasteventdate");
	private static List<String> DEFAULT_STORE_FIELD_LIST = Arrays.asList("type", "$taskid", "$writeaccess",
			"$workflowsummary", "$workflowabstract", "$workflowgroup", "$workflowstatus", "$modified", "$created",
			"$lasteventdate", "$creator", "$editor", "$lasteditor", "namowner");

	@EJB
	private EventLogService eventLogService;

	@Inject
	private LuceneItemAdapter luceneItemAdapter;

	@EJB
	private AdminPService adminPService;

	@PersistenceContext(unitName = "org.imixs.workflow.jpa")
	private EntityManager manager;

	private static Logger logger = Logger.getLogger(LuceneUpdateService.class.getName());

	/**
	 * PostContruct event - The method loads the lucene index properties from the
	 * imixs.properties file from the classpath. If no properties are defined the
	 * method terminates.
	 * 
	 */
	@PostConstruct
	void init() {

		logger.finest("......lucene IndexDir=" + luceneIndexDir);
		logger.finest("......lucene FulltextFieldList=" + luceneFulltextFieldList);
		logger.finest("......lucene IndexFieldListAnalyse=" + luceneIndexFieldListAnalyse);
		logger.finest("......lucene IndexFieldListNoAnalyse=" + luceneIndexFieldListNoAnalyse);

		// compute search field list
		searchFieldList = new ArrayList<String>();
		// add all static default field list
		searchFieldList.addAll(DEFAULT_SEARCH_FIELD_LIST);
		if (luceneFulltextFieldList != null && !luceneFulltextFieldList.isEmpty()) {
			StringTokenizer st = new StringTokenizer(luceneFulltextFieldList, ",");
			while (st.hasMoreElements()) {
				String sName = st.nextToken().toLowerCase().trim();
				// do not add internal fields
				if (!"$uniqueid".equals(sName) && !"$readaccess".equals(sName) && !searchFieldList.contains(sName))
					searchFieldList.add(sName);
			}
		}

		// compute Index field list (Analyze)
		indexFieldListAnalyse = new ArrayList<String>();
		if (luceneIndexFieldListAnalyse != null && !luceneIndexFieldListAnalyse.isEmpty()) {
			StringTokenizer st = new StringTokenizer(luceneIndexFieldListAnalyse, ",");
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
		if (luceneIndexFieldListNoAnalyse != null && !luceneIndexFieldListNoAnalyse.isEmpty()) {
			// add additional field list from imixs.properties
			StringTokenizer st = new StringTokenizer(luceneIndexFieldListNoAnalyse, ",");
			while (st.hasMoreElements()) {
				String sName = st.nextToken().toLowerCase().trim();
				if (!indexFieldListNoAnalyse.contains(sName))
					indexFieldListNoAnalyse.add(sName);
			}
		}

		// compute Index field list (Store)
		indexFieldListStore = new ArrayList<String>();
		// add all static default field list
		indexFieldListStore.addAll(DEFAULT_STORE_FIELD_LIST);
		if (luceneIndexFieldListStore != null && !luceneIndexFieldListStore.isEmpty()) {
			// add additional field list from imixs.properties
			StringTokenizer st = new StringTokenizer(luceneIndexFieldListStore, ",");
			while (st.hasMoreElements()) {
				String sName = st.nextToken().toLowerCase().trim();
				if (!indexFieldListStore.contains(sName))
					indexFieldListStore.add(sName);
			}
		}

		// Issue #518:
		// if a field of the indexFieldListStore is not part of the
		// indexFieldListAnalyse , than we add these fields to the indexFieldListAnalyse
		// This is to guaranty that we store the field value in any case.
		for (String fieldName : indexFieldListStore) {
			if (!indexFieldListAnalyse.contains(fieldName)) {
				// add this field into he indexFieldListAnalyse
				indexFieldListAnalyse.add(fieldName);
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

		config.replaceItemValue("lucence.indexDir", luceneIndexDir);
		// config.replaceItemValue("lucence.lockFactory", luceneLockFactory);
		config.replaceItemValue("lucence.analyzerClass", luceneAnalyserClass);
		config.replaceItemValue("lucence.fulltextFieldList", searchFieldList);
		config.replaceItemValue("lucence.indexFieldListAnalyze", indexFieldListAnalyse);
		config.replaceItemValue("lucence.indexFieldListNoAnalyze", indexFieldListNoAnalyse);
		config.replaceItemValue("lucence.indexFieldListStore", indexFieldListStore);

		return config;
	}

	/**
	 * This method adds a single document into the to the Lucene index. Before the
	 * document is added to the index, a new eventLog is created. The document will
	 * be indexed after the method flushEventLog is called. This method is called by
	 * the LuceneSearchService finder methods.
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
	 * document in a given selection a new eventLog is created. The documents will
	 * be indexed after the method flushEventLog is called. This method is called by
	 * the LuceneSearchService finder methods.
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
				eventLogService.createEvent(EVENTLOG_TOPIC_ADD, workitem.getUniqueID());
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
	 * This method adds a new eventLog for a document to be deleted from the index.
	 * The document will be removed from the index after the method fluschEventLog
	 * is called. This method is called by the LuceneSearchService finder method
	 * only.
	 * 
	 * 
	 * @param uniqueID
	 *            of the workitem to be removed
	 * @throws PluginException
	 */
	public void removeDocument(String uniqueID) {
		long ltime = System.currentTimeMillis();
		eventLogService.createEvent(EVENTLOG_TOPIC_REMOVE, uniqueID);
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
	private boolean flushEventLogByCount(int count) {
		Date lastEventDate = null;
		boolean cacheIsEmpty = true;
		IndexWriter indexWriter = null;
		long l = System.currentTimeMillis();
		logger.finest("......flush eventlog cache....");

		List<EventLog> events = eventLogService.findEventsByTopic(count + 1, EVENTLOG_TOPIC_ADD, EVENTLOG_TOPIC_REMOVE);

		if (events != null && events.size() > 0) {
			try {
				indexWriter = createIndexWriter();
				int _counter = 0;
				for (EventLog eventLogEntry : events) {
					Term term = new Term("$uniqueid", eventLogEntry.getRef());
					// lookup the Document Entity...
					org.imixs.workflow.engine.jpa.Document doc = manager
							.find(org.imixs.workflow.engine.jpa.Document.class, eventLogEntry.getRef());

					// if the document was found we add/update the index. Otherwise we remove the
					// document form the index.
					if (doc != null && EVENTLOG_TOPIC_ADD.equals(eventLogEntry.getTopic())) {
						// add workitem to search index....
						long l2 = System.currentTimeMillis();
						ItemCollection workitem = new ItemCollection();
						workitem.setAllItems(doc.getData());
						if (!workitem.getItemValueBoolean(DocumentService.NOINDEX)) {
							indexWriter.updateDocument(term, createDocument(workitem));
							logger.finest("......lucene add/update workitem '" + doc.getId() + "' to index in "
									+ (System.currentTimeMillis() - l2) + "ms");
						}
					} else {
						long l2 = System.currentTimeMillis();
						indexWriter.deleteDocuments(term);
						logger.finest("......lucene remove workitem '" + term + "' from index in "
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

		logger.fine("...flushEventLog - " + events.size() + " events in " + (System.currentTimeMillis() - l)
				+ " ms - last log entry: " + lastEventDate);

		return cacheIsEmpty;

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
	private Document createDocument(ItemCollection aworkitem) {
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
		List<String> _localFieldListStore = new ArrayList<String>();
		_localFieldListStore.addAll(indexFieldListStore);

		// analyzed...
		for (String aFieldname : indexFieldListAnalyse) {
			addItemValues(doc, aworkitem, aFieldname, true, _localFieldListStore.contains(aFieldname));
			// avoid duplication.....
			_localFieldListStore.remove(aFieldname);
		}
		// ... and not analyzed...
		for (String aFieldname : indexFieldListNoAnalyse) {
			addItemValues(doc, aworkitem, aFieldname, false, _localFieldListStore.contains(aFieldname));
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
	 * adds a field value into a Lucene document. The parameter store specifies if
	 * the value will become part of the Lucene document which is optional.
	 * 
	 * @param doc
	 *            an existing lucene document
	 * @param workitem
	 *            the workitem containg the values
	 * @param itemName
	 *            the Fieldname inside the workitem
	 * @param analyzeValue
	 *            indicates if the value should be parsed by the analyzer
	 * @param store
	 *            indicates if the value will become part of the Lucene document
	 */
	private void addItemValues(final Document doc, final ItemCollection workitem, final String _itemName,
			final boolean analyzeValue, final boolean store) {

		String itemName = _itemName;

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
			IndexableField indexableField = null;
			if (store) {
				indexableField = luceneItemAdapter.adaptItemValue(itemName, singleValue, analyzeValue, Store.YES);
			} else {
				indexableField = luceneItemAdapter.adaptItemValue(itemName, singleValue, analyzeValue, Store.NO);
			}
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
	 * This method creates a new instance of a lucene IndexWriter.
	 * 
	 * The location of the lucene index in the filesystem is read from the
	 * imixs.properties
	 * 
	 * @return
	 * @throws IOException
	 */
	private IndexWriter createIndexWriter() throws IOException {
		// create a IndexWriter Instance
		Directory indexDir = FSDirectory.open(Paths.get(luceneIndexDir));
		// verify existence of index directory...
		if (!DirectoryReader.indexExists(indexDir)) {
			logger.info("...lucene index does not yet exist, initialize the index now....");
			rebuildIndex(indexDir);
		}
		IndexWriterConfig indexWriterConfig;
		// indexWriterConfig = new IndexWriterConfig(new ClassicAnalyzer());
		try {
			// issue #429
			indexWriterConfig = new IndexWriterConfig((Analyzer) Class.forName(luceneAnalyserClass).newInstance());
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			throw new IndexException(IndexException.INVALID_INDEX,
					"Unable to create analyzer '" + luceneAnalyserClass + "'", e);
		}

		return new IndexWriter(indexDir, indexWriterConfig);
	}

	/**
	 * This method forces an update of the full text index. The method also creates
	 * the index directory if it does not yet exist.
	 */
	public void rebuildIndex(Directory indexDir) throws IOException {
		// create a IndexWriter Instance to make sure we have created the index
		// directory..
		IndexWriterConfig indexWriterConfig;
		indexWriterConfig = new IndexWriterConfig(new ClassicAnalyzer());
		indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
		IndexWriter indexWriter = new IndexWriter(indexDir, indexWriterConfig);
		indexWriter.close();
		// now starting index job....
		logger.info("...rebuild lucene index job created...");
		ItemCollection job = new ItemCollection();
		job.replaceItemValue("numinterval", 2); // 2 minutes
		job.replaceItemValue("job", AdminPService.JOB_REBUILD_LUCENE_INDEX);
		adminPService.createJob(job);
	}

}
