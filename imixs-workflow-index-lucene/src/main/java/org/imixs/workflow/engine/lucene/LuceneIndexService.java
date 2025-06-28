/****************************************************************************
 * Copyright (c) 2022-2025 Imixs Software Solutions GmbH and others.
 * https://www.imixs.com
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * This Source Code may also be made available under the terms of the
 * GNU General Public License, version 2 or later (GPL-2.0-or-later),
 * which is available at https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-or-later
 ****************************************************************************/

package org.imixs.workflow.engine.lucene;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.ClassicAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
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
import org.imixs.workflow.engine.index.IndexEvent;
import org.imixs.workflow.engine.index.SchemaService;
import org.imixs.workflow.engine.jpa.EventLog;
import org.imixs.workflow.exceptions.IndexException;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * This session ejb provides functionality to maintain a local Lucene index.
 * 
 * @version 1.0
 * @author rsoika
 */
@Stateless
public class LuceneIndexService {

    public static final int EVENTLOG_ENTRY_FLUSH_COUNT = 16;

    public static final String ANONYMOUS = "ANONYMOUS";
    public static final String DEFAULT_ANALYZER = "org.apache.lucene.analysis.standard.ClassicAnalyzer";
    public static final String DEFAULT_INDEX_DIRECTORY = "imixs-workflow-index";
    public static final String TAXONOMY_INDEXFIELD_PRAFIX = ".taxonomy";

    @PersistenceContext(unitName = "org.imixs.workflow.jpa")
    private EntityManager manager;

    private SimpleDateFormat luceneDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    @Inject
    @ConfigProperty(name = "lucence.indexDir", defaultValue = DEFAULT_INDEX_DIRECTORY)
    private String luceneIndexDir;

    @Inject
    @ConfigProperty(name = "lucence.analyzerClass", defaultValue = DEFAULT_ANALYZER)
    private String luceneAnalyzerClass;

    @Inject
    private LuceneItemAdapter luceneItemAdapter;

    private static final Logger logger = Logger.getLogger(LuceneIndexService.class.getName());

    @Inject
    private AdminPService adminPService;

    @Inject
    private EventLogService eventLogService;

    @Inject
    private SchemaService schemaService;

    @Inject
    protected Event<IndexEvent> indexEvents;

    private boolean bRebuildIndex; // indicator if we have triggered a rebuild index job.

    public String getLuceneIndexDir() {
        // issue #599
        return luceneIndexDir.trim();
    }

    public void setLuceneIndexDir(String luceneIndexDir) {
        if (luceneIndexDir != null) {
            this.luceneIndexDir = luceneIndexDir.trim();
        }
    }

    public String getLuceneAnalyzerClass() {
        return luceneAnalyzerClass;
    }

    public void setLuceneAnalyzerClass(String luceneAnalyzerClass) {
        this.luceneAnalyzerClass = luceneAnalyzerClass;
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
                        logger.log(Level.FINEST, "...flush event log: {0} entries in {1}ms...",
                                new Object[] { total, System.currentTimeMillis() - l });
                        count = 0;
                    }

                    // issue #439
                    // In some cases the flush method runs endless.
                    // experimental code: we break the flush method after 1024 flushs
                    // maybe we can remove this hard break
                    if (total >= junkSize) {
                        logger.log(Level.FINEST, "...flush event: Issue #439  ->"
                                + " total count >={0} flushEventLog will be continued...", total);
                        return false;
                    }
                }

            } catch (IndexException e) {
                logger.log(Level.WARNING, "...unable to flush lucene event log: {0}", e.getMessage());
                return true;
            }
        }
        return true;
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

        // already triggered?
        if (bRebuildIndex == false) {
            // now starting index job....
            logger.info("...rebuild lucene index job created...");
            ItemCollection job = new ItemCollection();
            job.replaceItemValue("job", AdminPService.JOB_REBUILD_INDEX);
            adminPService.createJob(job);
            bRebuildIndex = true;
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
     * @param documents of ItemCollections to be indexed
     * @throws IndexException
     */
    public void indexDocuments(Collection<ItemCollection> documents) {
        IndexWriter indexWriter = null;
        DirectoryTaxonomyWriter taxonomyWriter = null;
        long ltime = System.currentTimeMillis();
        try {
            indexWriter = createIndexWriter();
            taxonomyWriter = createTaxonomyWriter();
            // add workitem to search index....
            for (ItemCollection workitem : documents) {

                if (!workitem.getItemValueBoolean(DocumentService.NOINDEX)) {
                    // create term
                    Term term = new Term("$uniqueid", workitem.getItemValueString("$uniqueid"));
                    logger.log(Level.FINEST, "......lucene add/update uncommitted workitem ''{0}'' to index...",
                            workitem.getItemValueString(WorkflowKernel.UNIQUEID));

                    // awriter.updateDocument(term, createDocument(workitem));
                    Document lucenedoc = createDocument(workitem);
                    updateLuceneIndex(term, lucenedoc, indexWriter, taxonomyWriter);
                }
            }
        } catch (IOException luceneEx) {
            logger.log(Level.WARNING, "lucene error: {0}", luceneEx.getMessage());
            throw new IndexException(IndexException.INVALID_INDEX, "Unable to update lucene search index", luceneEx);
        } finally {
            // close writer!
            if (indexWriter != null) {
                logger.finest("......lucene close IndexWriter...");
                try {
                    indexWriter.close();
                } catch (CorruptIndexException e) {
                    throw new IndexException(IndexException.INVALID_INDEX, "Unable to close lucene IndexWriter: ", e);
                } catch (IOException e) {
                    throw new IndexException(IndexException.INVALID_INDEX, "Unable to close lucene IndexWriter: ", e);
                }
            }
            // close taxonomyWriter!
            if (taxonomyWriter != null) {
                logger.finest("......lucene close taxonomyWriter...");
                try {
                    taxonomyWriter.close();
                } catch (CorruptIndexException e) {
                    throw new IndexException(IndexException.INVALID_INDEX, "Unable to close lucene taxonomyWriter: ",
                            e);
                } catch (IOException e) {
                    throw new IndexException(IndexException.INVALID_INDEX, "Unable to close lucene taxonomyWriter: ",
                            e);
                }
            }
        }

        long updateTime = (System.currentTimeMillis() - ltime);
        if (updateTime > 5000) {
            logger.log(Level.WARNING, "... update index block in took {0} ms ! ({1} documents in total)",
                    new Object[] { updateTime, documents.size() });
        } else if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "... update index block in {0} ms ({1} documents in total)",
                    new Object[] { updateTime, documents.size() });
        }
    }

    /**
     * This method flushes a given count of eventLogEntries. The method return true
     * if no more eventLogEntries exist.
     * 
     * @param count the max size of a eventLog engries to remove.
     * @return true if the cache was totally flushed.
     */
    protected boolean flushEventLogByCount(int count) {
        Date lastEventDate = null;
        boolean cacheIsEmpty = true;
        DirectoryTaxonomyWriter taxonomyWriter = null;
        IndexWriter indexWriter = null;
        long l = System.currentTimeMillis();
        logger.finest("......flush eventlog cache....");

        List<EventLog> events = eventLogService.findEventsByTopic(count + 1, DocumentService.EVENTLOG_TOPIC_INDEX_ADD,
                DocumentService.EVENTLOG_TOPIC_INDEX_REMOVE);

        if (events != null && events.size() > 0) {
            try {
                indexWriter = createIndexWriter();
                taxonomyWriter = createTaxonomyWriter();
                int _counter = 0;
                for (EventLog eventLogEntry : events) {
                    Term term = new Term("$uniqueid", eventLogEntry.getRef());
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
                            Document lucenedoc = createDocument(workitem);
                            // indexWriter.updateDocument(term,lucenedoc );

                            updateLuceneIndex(term, lucenedoc, indexWriter, taxonomyWriter);
                            logger.log(Level.FINEST, "......lucene add/update workitem ''{0}'' to index in {1}ms",
                                    new Object[] { doc.getId(), System.currentTimeMillis() - l2 });
                        }
                    } else {
                        long l2 = System.currentTimeMillis();
                        indexWriter.deleteDocuments(term);
                        logger.log(Level.FINEST, "......lucene remove workitem ''{0}'' from index in {1}ms",
                                new Object[] { term, System.currentTimeMillis() - l2 });
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
                logger.log(Level.WARNING, "...unable to flush lucene event log: {0}", luceneEx.getMessage());
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
                // close taxonomyWriter!
                if (taxonomyWriter != null) {
                    logger.finest("......lucene close taxoWriter...");
                    try {
                        taxonomyWriter.close();
                    } catch (CorruptIndexException e) {
                        throw new IndexException(IndexException.INVALID_INDEX,
                                "Unable to close lucene taxonomyWriter: ", e);
                    } catch (IOException e) {
                        throw new IndexException(IndexException.INVALID_INDEX,
                                "Unable to close lucene taxonomyWriter: ", e);
                    }
                }
            }

        }

        logger.log(Level.FINE, "...flushEventLog - {0} events in {1} ms - last log entry: {2}",
                new Object[] { events.size(), System.currentTimeMillis() - l, lastEventDate });

        return cacheIsEmpty;

    }

    /**
     * THis helper method is used to write the lucene document into the search index
     * and into the taxonomy index.
     * 
     * @param term
     * @param lucenedoc
     * @param indexWriter
     * @param taxoWriter
     * @throws IOException
     */
    private void updateLuceneIndex(Term term, Document lucenedoc, IndexWriter indexWriter,
            DirectoryTaxonomyWriter taxoWriter) throws IOException {

        FacetsConfig config = getFacetsConfig();
        // update the indices....
        indexWriter.updateDocument(term, config.build(taxoWriter, lucenedoc));
    }

    /**
     * This method builds a facetcConfig for the taxonomy index writer where each
     * category item is marked as a multiValued field.
     * 
     * @return
     */
    public FacetsConfig getFacetsConfig() {
        // build the facetsConfig object based on the category field list
        FacetsConfig config = new FacetsConfig();
        /* place to customize the taxonomy - currently not used */
        List<String> indexFieldListCategory = schemaService.getFieldListCategory();
        for (String aFieldname : indexFieldListCategory) {
            aFieldname = aFieldname.toLowerCase().trim();
            config.setMultiValued(aFieldname + TAXONOMY_INDEXFIELD_PRAFIX, true);
        }
        return config;
    }

    /**
     * This method creates a lucene document based on a ItemCollection. The Method
     * creates for each field specified in the FieldList a separate index field for
     * the lucene document.
     * 
     * The property 'AnalyzeIndexFields' defines if a indexfield value should by
     * analyzed by the Lucene Analyzer (default=false)
     * 
     * @param document - the Imixs document to be indexed
     * @return - a lucene document instance
     */
    @SuppressWarnings("unchecked")
    protected Document createDocument(ItemCollection document) {
        String sValue = null;
        Document doc = new Document();
        // combine all search fields from the search field list into one field
        // ('content') for the lucene document
        String textContent = "";

        List<String> searchFieldList = schemaService.getFieldList();
        for (String aFieldname : searchFieldList) {
            sValue = "";
            // check value list - skip empty fields
            List<?> vValues = document.getItemValue(aFieldname);
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
                        sDateValue = luceneDateFormat.format(((Calendar) o).getTime());
                    else
                        sDateValue = luceneDateFormat.format((Date) o);
                    sValue += sDateValue + ",";

                } else
                    // simple string representation
                    sValue += o.toString() + ",";
            }
            if (sValue != null) {
                textContent += sValue + ",";
            }
        }

        // fire IndexEvent to update the text content if needed
        if (indexEvents != null) {
            IndexEvent indexEvent = new IndexEvent(IndexEvent.ON_INDEX_UPDATE, document);
            indexEvent.setTextContent(textContent);
            indexEvents.fire(indexEvent);
            textContent = indexEvent.getTextContent();
        } else {
            logger.warning("Missing CDI support for Event<IndexEvent> !");
        }

        logger.log(Level.FINEST, "......add lucene field content={0}", textContent);
        doc.add(new TextField("content", textContent, Store.NO));

        // add each field from the indexFieldList into the lucene document
        List<String> _localFieldListStore = new ArrayList<String>();
        _localFieldListStore.addAll(schemaService.getFieldListStore());

        // analyzed...
        List<String> indexFieldListAnalyze = schemaService.getFieldListAnalyze();
        for (String aFieldname : indexFieldListAnalyze) {
            addItemValues(doc, document, aFieldname, true, _localFieldListStore.contains(aFieldname));
            // avoid duplication.....
            _localFieldListStore.remove(aFieldname);
        }

        // ... and not analyzed...
        List<String> indexFieldListNoAnalyze = schemaService.getFieldListNoAnalyze();
        for (String aFieldname : indexFieldListNoAnalyze) {
            addItemValues(doc, document, aFieldname, false, _localFieldListStore.contains(aFieldname));
        }

        // add $uniqueid not analyzed
        doc.add(new StringField("$uniqueid", document.getItemValueString("$uniqueid"), Store.YES));

        // add $readAccess not analyzed
        List<String> vReadAccess = (List<String>) document.getItemValue(DocumentService.READACCESS);
        if (vReadAccess.size() == 0 || (vReadAccess.size() == 1 && "".equals(vReadAccess.get(0).toString()))) {
            // if emtpy add the ANONYMOUS default entry
            sValue = ANONYMOUS;
            doc.add(new StringField(DocumentService.READACCESS, sValue, Store.NO));
        } else {
            sValue = "";
            // add each role / username as a single field value
            for (String sReader : vReadAccess) {
                doc.add(new StringField(DocumentService.READACCESS, sReader, Store.NO));
            }

        }

        // add optional categories
        List<String> indexFieldListCategory = schemaService.getFieldListCategory();
        // example
        // doc.add(new FacetField("Author", "Bob"));
        for (String aFieldname : indexFieldListCategory) {
            // item name must be LowerCased and trimmed because of later usage in
            // doc.add(...)
            aFieldname = aFieldname.toLowerCase().trim();
            /**
             * SINGLE VALUE VARIANT * String value=document.getItemValueString(aFieldname);
             * if (!value.isEmpty()) { doc.add(new
             * FacetField(aFieldname+TAXONOMY_INDEXFIELD_PRAFIX, value)); }
             */

            /** MULTI VALUE VARIANT **/
            // a facetField can only be written if we have a value
            List<?> valueList = document.getItemValue(aFieldname);
            if (valueList.size() > 0 && valueList.get(0) != null) {
                for (Object singleValue : valueList) {
                    String stringValue = luceneItemAdapter.convertItemValue(singleValue);
                    try {
                        if (stringValue != null && !stringValue.isEmpty()) {
                            doc.add(new FacetField(aFieldname + TAXONOMY_INDEXFIELD_PRAFIX, stringValue));
                        }
                    } catch (IllegalArgumentException iae) {
                        logger.log(Level.WARNING, "Failed to build facete: {0}", iae.getMessage());
                    }
                }
            }
        }

        return doc;
    }

    /**
     * adds a field value into a Lucene document. The parameter store specifies if
     * the value will become part of the Lucene document which is optional.
     * 
     * @param doc          an existing lucene document
     * @param workitem     the workitem containg the values
     * @param itemName     the Fieldname inside the workitem
     * @param analyzeValue indicates if the value should be parsed by the analyzer
     * @param store        indicates if the value will become part of the Lucene
     *                     document
     */
    protected void addItemValues(final Document doc, final ItemCollection workitem, final String _itemName,
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
    protected IndexWriter createIndexWriter() throws IOException {
        logger.finest("......createIndexWriter...");
        // create a IndexWriter Instance
        Directory indexDir = createIndexDirectory();
        IndexWriterConfig indexWriterConfig;
        // indexWriterConfig = new IndexWriterConfig(new ClassicAnalyzer());
        try {
            // issue #429
            indexWriterConfig = new IndexWriterConfig((Analyzer) Class.forName(luceneAnalyzerClass).newInstance());
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new IndexException(IndexException.INVALID_INDEX,
                    "Unable to create analyzer '" + luceneAnalyzerClass + "'", e);
        }
        return new IndexWriter(indexDir, indexWriterConfig);
    }

    /**
     * Create taxonomyWriter in a separate directory from the main index with the
     * paefix '_taxÄ'
     *
     * @return
     * @throws IOException
     */
    protected DirectoryTaxonomyWriter createTaxonomyWriter() throws IOException {
        logger.finest("......createTaxonomyWriter...");
        // create a IndexWriter Instance
        Directory taxoDir = createTaxonomyDirectory();
        return new DirectoryTaxonomyWriter(taxoDir);
    }

    /**
     * Creates a Lucene FSDirectory Instance. The method uses the property
     * LockFactory to set a custom LockFactory.
     * 
     * For example: org.apache.lucene.store.SimpleFSLockFactory
     * 
     * @return
     * @throws IOException
     */
    public Directory createIndexDirectory() throws IOException {
        logger.log(Level.FINEST, "......create lucene Index Directory - path={0}", getLuceneIndexDir());
        // create Lucene Directory Instance
        Path luceneIndexDir = Paths.get(getLuceneIndexDir());
        Directory indexDir = FSDirectory.open(luceneIndexDir);
        if (!DirectoryReader.indexExists(indexDir)) {
            logger.log(Level.INFO, "...lucene index directory ''{0}'' is empty or does not yet exist,"
                    + " rebuild index now....", getLuceneIndexDir());
            rebuildIndex(indexDir);
        }
        return indexDir;
    }

    /**
     * Creates a Lucene FSDirectory Instance. The method uses the property
     * LockFactory to set a custom LockFactory.
     * <p>
     * The taxonomy directory is identified by the LuceneIndexDir with the praefix
     * '_tax'
     * 
     * @return
     * @throws IOException
     */
    public Directory createTaxonomyDirectory() throws IOException {
        String sPath = getLuceneIndexDir();
        if (sPath.endsWith("/")) {
            sPath = sPath.substring(0, sPath.lastIndexOf("/"));
        }
        sPath = sPath + "_tax";
        logger.log(Level.FINEST, "......create lucene taxonomy Directory - path={0}", sPath);
        // create Lucene Directory Instance
        Path luceneIndexDir = Paths.get(sPath);
        Directory indexDir = FSDirectory.open(luceneIndexDir);
        if (!DirectoryReader.indexExists(indexDir)) {
            logger.log(Level.INFO, "...lucene taxonomy directory ''{0}'' is empty or does not yet exist,"
                    + " rebuild taxonomy index now....", getLuceneIndexDir());
            rebuildIndex(indexDir);
        }
        return indexDir;
    }

}
