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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;

import org.apache.lucene.analysis.standard.ClassicAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.BytesRef;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
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

	protected static final String DEFAULT_ANALYSER = "org.apache.lucene.analysis.standard.ClassicAnalyzer";
	protected static final String DEFAULT_INDEX_DIRECTORY = "imixs-workflow-index";
	protected static final String ANONYMOUS = "ANONYMOUS";

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
			"txtworkitemref","$uniqueidsource","$uniqueidversions","$lasttask","$lastevent","$lasteventdate");

	@EJB
	PropertyService propertyService;

	private static Logger logger = Logger.getLogger(LuceneUpdateService.class.getName());

	/**
	 * PostContruct event - The method loads the lucene index properties from
	 * the imixs.properties file from the classpath. If no properties are
	 * defined the method terminates.
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
	 * This method adds a single document into the search index.
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
	 * This method updates the search index for a collection of Documents.
	 * 
	 * @param documents
	 *            of ItemCollections to be indexed
	 * @throws IndexException
	 */
	public void updateDocuments(Collection<ItemCollection> documents) {

		IndexWriter awriter = null;
		long ltime = System.currentTimeMillis();
		try {
			awriter = createIndexWriter();
			// add workitem to search index....
			for (ItemCollection workitem : documents) {
				// create term
				Term term = new Term("$uniqueid", workitem.getItemValueString("$uniqueid"));

				logger.finest("......lucene add/update workitem '" + workitem.getItemValueString(WorkflowKernel.UNIQUEID)
						+ "' to index...");
				awriter.updateDocument(term, createDocument(workitem));
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
			logger.fine("... update worklist in " + (System.currentTimeMillis() - ltime) + " ms (" + documents.size()
					+ " worktiems total)");
		}
	}

	/**
	 * This method removes a single Document from the search index.
	 * 
	 * @param uniqueID
	 *            of the workitem to be removed
	 * @throws PluginException
	 */
	public void removeDocument(String uniqueID) {
		IndexWriter awriter = null;
		long ltime = System.currentTimeMillis();
		try {
			awriter = createIndexWriter();
			Term term = new Term("$uniqueid", uniqueID);
			awriter.deleteDocuments(term);
		} catch (CorruptIndexException e) {
			throw new IndexException(IndexException.INVALID_INDEX,
					"Unable to remove workitem '" + uniqueID + "' from search index", e);
		} catch (LockObtainFailedException e) {
			throw new IndexException(IndexException.INVALID_INDEX,
					"Unable to remove workitem '" + uniqueID + "' from search index", e);
		} catch (IOException e) {
			throw new IndexException(IndexException.INVALID_INDEX,
					"Unable to remove workitem '" + uniqueID + "' from search index", e);
		} finally {
			// close writer!
			if (awriter != null) {
				logger.finest("......close IndexWriter...");
				try {
					awriter.close();
				} catch (CorruptIndexException e) {
					throw new IndexException(IndexException.INVALID_INDEX, "Unable to close lucene IndexWriter: ", e);
				} catch (IOException e) {
					throw new IndexException(IndexException.INVALID_INDEX, "Unable to close lucene IndexWriter: ", e);
				}
			}
		}

		logger.fine("...removed Document in " + (System.currentTimeMillis() - ltime) + " ms");

	}

	/**
	 * This method creates a new instance of a lucene IndexWriter.
	 * 
	 * The location of the lucene index in the filesystem is read from the
	 * imixs.properties
	 * 
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
	IndexWriter createIndexWriter() throws IOException {
		// create a IndexWriter Instance
		Directory indexDir = FSDirectory.open(Paths.get(indexDirectoryPath));
		IndexWriterConfig indexWriterConfig;
		indexWriterConfig = new IndexWriterConfig(new ClassicAnalyzer());

		return new IndexWriter(indexDir, indexWriterConfig);
	}

	/**
	 * This method creates a lucene document based on a ItemCollection. The
	 * Method creates for each field specified in the FieldList a separate index
	 * field for the lucene document.
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
				logger.finest("......lucene add SearchField: " + aFieldname + "=" + sValue);
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

}
