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

package org.imixs.workflow.lucene;

import java.io.File;
import java.io.IOException;
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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.ClassicAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ejb.PropertyService;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.jee.ejb.EntityService;

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

	public static final String UNDEFINED_ERROR = "UNDEFINED_ERROR";
	public static final String INVALID_INDEX = "INVALID_INDEX";
	protected static final String DEFAULT_ANALYSER = "org.apache.lucene.analysis.standard.ClassicAnalyzer";
	protected static final String DEFAULT_INDEX_DIRECTORY = "imixs-workflow-index";

	private List<String> searchFieldList = null;
	private List<String> indexFieldListAnalyse = null;
	private List<String> indexFieldListNoAnalyse = null;
	private String indexDirectoryPath = null;
	private String luceneLockFactory = null;
	private String analyserClass = null;
	private Properties properties = null;

	private static List<String> NOANALYSE_FIELD_LIST = Arrays.asList("$modelversion", "$processid", "$workitemid",
			"$uniqueidref", "type", "$writeaccess", "namcreator", "txtworkflowgroup", "txtname", "namowner",
			"txtworkitemref");

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
		luceneLockFactory = properties.getProperty("lucence.lockFactory");
		// get Analyzer Class -
		// default=org.apache.lucene.analysis.standard.ClassicAnalyzer
		analyserClass = properties.getProperty("lucence.analyzerClass", DEFAULT_ANALYSER);

		String sFulltextFieldList = properties.getProperty("lucence.fulltextFieldList");
		String sIndexFieldListAnalyse = properties.getProperty("lucence.indexFieldListAnalyze");
		String sIndexFieldListNoAnalyse = properties.getProperty("lucence.indexFieldListNoAnalyze");

		logger.fine("lucene IndexDir=" + indexDirectoryPath);
		logger.fine("lucene FulltextFieldList=" + sFulltextFieldList);
		logger.fine("lucene IndexFieldListAnalyse=" + sIndexFieldListAnalyse);
		logger.fine("lucene IndexFieldListNoAnalyse=" + sIndexFieldListNoAnalyse);

		// compute search field list
		searchFieldList = new ArrayList<String>();
		if (sFulltextFieldList != null && !sFulltextFieldList.isEmpty()) {
			StringTokenizer st = new StringTokenizer(sFulltextFieldList, ",");
			while (st.hasMoreElements()) {
				String sName = st.nextToken().toLowerCase();
				// do not add internal fields
				if (!"$uniqueid".equals(sName) && !"$readaccess".equals(sName))
					searchFieldList.add(sName);
			}
		}

		// compute Index field list (Analyze)
		indexFieldListAnalyse = new ArrayList<String>();
		if (sIndexFieldListAnalyse != null && !sIndexFieldListAnalyse.isEmpty()) {
			StringTokenizer st = new StringTokenizer(sIndexFieldListAnalyse, ",");
			while (st.hasMoreElements()) {
				String sName = st.nextToken().toLowerCase();
				// do not add internal fields
				if (!"$uniqueid".equals(sName) && !"$readaccess".equals(sName))
					indexFieldListAnalyse.add(sName);
			}
		}

		// compute Index field list (NoAnalyze)

		indexFieldListNoAnalyse = new ArrayList<String>();
		// add all static fields from the WorkflowService
		indexFieldListNoAnalyse.addAll(NOANALYSE_FIELD_LIST);
		if (sIndexFieldListNoAnalyse != null && !sIndexFieldListNoAnalyse.isEmpty()) {
			// add additional field list from imixs.properties
			StringTokenizer st = new StringTokenizer(sIndexFieldListNoAnalyse, ",");
			while (st.hasMoreElements()) {
				String sName = st.nextToken().toLowerCase();
				if (!indexFieldListNoAnalyse.contains(sName))
					indexFieldListNoAnalyse.add(sName);
			}
		}
	}

	
	
	/**
	 * Returns the Lucene configuration 
	 * @return
	 */
	public ItemCollection getConfiguration() {
		ItemCollection config=new ItemCollection();
		
		config.replaceItemValue("lucence.indexDir", indexDirectoryPath);
		config.replaceItemValue("lucence.lockFactory", luceneLockFactory);
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
	 * @return
	 * @throws Exception
	 */
	public boolean updateDocument(ItemCollection documentContext) {
		// adds the document into a empty Collection and call the method
		// updateDocuments.
		List<ItemCollection> documents = new ArrayList<ItemCollection>();
		documents.add(documentContext);
		updateDocuments(documents);
		return true;
	}

	/**
	 * This method updates the search index for a collection of Documents.
	 * 
	 * @param documents
	 *            of ItemCollections to be indexed
	 * @return - true if the update was successful
	 * @throws Exception
	 */
	public boolean updateDocuments(Collection<ItemCollection> documents) throws LuceneException {

		IndexWriter awriter = null;
		long ltime = System.currentTimeMillis();
		try {
			awriter = createIndexWriter();
			// add workitem to search index....
			for (ItemCollection workitem : documents) {
				// create term
				Term term = new Term("$uniqueid", workitem.getItemValueString("$uniqueid"));

				logger.fine("lucene add/update workitem '" + workitem.getItemValueString(EntityService.UNIQUEID)
						+ "' to index...");
				awriter.updateDocument(term, createDocument(workitem));
			}
		} catch (IOException luceneEx) {
			logger.warning("lucene error: " + luceneEx.getMessage());
			throw new LuceneException(INVALID_INDEX, "Unable to update lucene search index", luceneEx);
		} finally {
			// close writer!
			if (awriter != null) {
				logger.fine("lucene close writer");
				try {
					awriter.close();
				} catch (CorruptIndexException e) {
					throw new LuceneException(INVALID_INDEX, "Unable to update lucene search index", e);
				} catch (IOException e) {
					throw new LuceneException(INVALID_INDEX, "Unable to update lucene search index", e);
				}
			}
		}

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("lucene update worklist in " + (System.currentTimeMillis() - ltime) + " ms (" + documents.size()
					+ " worktiems total)");
		}
		return true;
	}

	/**
	 * This method removes a single Document from the search index.
	 * 
	 * @param uniqueID
	 *            of the workitem to be removed
	 * @throws PluginException
	 */
	public void removeDocument(String uniqueID) throws LuceneException {
		IndexWriter awriter = null;
		try {
			awriter = createIndexWriter();
			Term term = new Term("$uniqueid", uniqueID);
			awriter.deleteDocuments(term);
		} catch (CorruptIndexException e) {
			throw new LuceneException(INVALID_INDEX, "Unable to remove workitem '" + uniqueID + "' from search index",
					e);
		} catch (LockObtainFailedException e) {
			throw new LuceneException(INVALID_INDEX, "Unable to remove workitem '" + uniqueID + "' from search index",
					e);
		} catch (IOException e) {
			throw new LuceneException(INVALID_INDEX, "Unable to remove workitem '" + uniqueID + "' from search index",
					e);
		}

	}

	/**
	 * This method creates a new instance of a lucene IndexWriter.
	 * 
	 * The configuration how to index a workiem is read from the properties
	 * param.
	 * 
	 * The timeout to wait for a write lock is set to 10 seconds.
	 * 
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
	IndexWriter createIndexWriter() throws IOException {
		// create a IndexWriter Instance
		Directory indexDir = createIndexDirectory();

		// IndexWriterConfig indexWriterConfig = new
		// IndexWriterConfig(Version.LATEST, new ClassicAnalyzer());
		// we build the analyzer form the configuration - default =
		// org.apache.lucene.analysis.standard.ClassicAnalyzer

		IndexWriterConfig indexWriterConfig;
		try {
			indexWriterConfig = new IndexWriterConfig(Version.LATEST,
					(Analyzer) Class.forName(analyserClass).newInstance());
			logger.fine("Analyzer Class: " + analyserClass);
		} catch (Exception e) {
			logger.warning("Unable to instanciate Analyzer Class '" + analyserClass + "' - verify imixs.properties");
			logger.warning("Create default analyzer: " + DEFAULT_ANALYSER);
			indexWriterConfig = new IndexWriterConfig(Version.LATEST, new ClassicAnalyzer());
		}
		// set the WriteLockTimeout to wait for a write lock (in milliseconds)
		// for this instance. 10 seconds!
		indexWriterConfig.setWriteLockTimeout(10000);

		return new IndexWriter(indexDir, indexWriterConfig);
	}

	/**
	 * Creates a Lucene FSDirectory Instance. The method uses the proeprty
	 * LockFactory to set a custom LockFactory.
	 * 
	 * For example: org.apache.lucene.store.SimpleFSLockFactory
	 * 
	 * @return
	 * @throws IOException
	 */
	Directory createIndexDirectory() throws IOException {

		logger.fine("lucene createIndexDirectory...");
		/**
		 * Read configuration
		 */

		Directory indexDir = FSDirectory.open(new File(indexDirectoryPath));

		// set lockFactory
		// NativeFSLockFactory: using native OS file locks
		// SimpleFSLockFactory: recommended for NFS based access to an index,
		if (luceneLockFactory != null && !"".equals(luceneLockFactory)) {
			// indexDir.setLockFactory(new SimpleFSLockFactory());
			// set factory by class name
			logger.fine("lucene set LockFactory=" + luceneLockFactory);
			try {
				Class<?> fsFactoryClass;
				fsFactoryClass = Class.forName(luceneLockFactory);
				LockFactory factoryInstance = (LockFactory) fsFactoryClass.newInstance();
				indexDir.setLockFactory(factoryInstance);
			} catch (ClassNotFoundException e) {
				logger.severe("lucene error - unable to create Lucene LockFactory!");
				e.printStackTrace();
				return null;
			} catch (InstantiationException e) {
				logger.severe("lucene error - unable to create Lucene LockFactory!");
				e.printStackTrace();
				return null;
			} catch (IllegalAccessException e) {
				logger.severe("lucene error - unable to create Lucene LockFactory!");
				e.printStackTrace();
				return null;
			}
		}
		return indexDir;
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
					SimpleDateFormat dateformat = new SimpleDateFormat("yyyyMMddHHmm");
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
				logger.finest("lucene add SearchField: " + aFieldname + "=" + sValue);
				sContent += sValue + ",";
			}
		}
		logger.fine("lucene document content=" + sContent);

		// Migration guide
		// http://lucene.apache.org/core/4_0_0/MIGRATE.html

		// If instead the value was stored:
		//
		// new Field("field", value, Field.Store.YES, Field.Indexed.ANALYZED)
		// you can now do this:
		//
		// new Field("field", value, TextField.TYPE_STORED)
		//

		// doc.add(new Field("content", sContent, Field.Store.NO,
		// Field.Index.ANALYZED));
		doc.add(new TextField("content", sContent, Store.NO));

		// add each field from the indexFieldList into the lucene document
		for (String aFieldname : indexFieldListAnalyse) {
			addFieldValue(doc, aworkitem, aFieldname, true);
		}

		for (String aFieldname : indexFieldListNoAnalyse) {
			addFieldValue(doc, aworkitem, aFieldname, false);
		}

		// add default value $uniqueid
		// doc.add(new Field("$uniqueid",
		// aworkitem.getItemValueString("$uniqueid"),
		// Field.Store.YES,Field.Index.NOT_ANALYZED));
		doc.add(new StringField("$uniqueid", aworkitem.getItemValueString("$uniqueid"), Store.YES));

		// add default values $readAccess
		List<String> vReadAccess = (List<String>) aworkitem.getItemValue("$readAccess");
		if (vReadAccess.size() == 0 || (vReadAccess.size() == 1 && "".equals(vReadAccess.get(0).toString()))) {
			sValue = "ANONYMOUS";

			// migration
			// new Field("field", "value", Field.Store.NO,
			// Field.Indexed.NOT_ANALYZED_NO_NORMS)
			// you can now do this:
			// new StringField("field", "value")

			// doc.add(new Field("$readaccess", sValue,
			// Field.Store.NO,Field.Index.NOT_ANALYZED_NO_NORMS));
			doc.add(new StringField("$readaccess", sValue, Store.NO));
		} else {
			sValue = "";
			// add each role / username as a single field value
			for (String sReader : vReadAccess) {
				// doc.add(new Field("$readaccess", sReader,
				// Field.Store.NO,Field.Index.NOT_ANALYZED_NO_NORMS));
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
	 * @param aworkitem
	 *            the workitem containg the values
	 * @param aFieldname
	 *            the Fieldname inside the workitem
	 * @param analyzeValue
	 *            indicates if the value should be parsed by the analyzer
	 */
	void addFieldValue(Document doc, ItemCollection aworkitem, String aFieldname, boolean analyzeValue) {
		String sValue = null;
		List<?> vValues = aworkitem.getItemValue(aFieldname);
		if (vValues.size() == 0)
			return;
		if (vValues.get(0) == null)
			return;

		for (Object singleValue : vValues) {

			// Object o = vValues.firstElement();
			if (singleValue instanceof Calendar || singleValue instanceof Date) {
				SimpleDateFormat dateformat = new SimpleDateFormat("yyyyMMddHHmm");

				// convert calendar to string
				String sDateValue;
				if (singleValue instanceof Calendar)
					sDateValue = dateformat.format(((Calendar) singleValue).getTime());
				else
					sDateValue = dateformat.format((Date) singleValue);
				sValue = sDateValue;

			} else
				// simple string representation
				sValue = singleValue.toString();

			logger.fine("lucene add IndexField (analyse=" + analyzeValue + "): " + aFieldname + "=" + sValue);
			if (analyzeValue) {
				// If you did this before (value can be String or Reader):
				// new Field("field", value, Field.Store.NO,
				// Field.Indexed.ANALYZED)
				// you can now do this:
				// new TextField("field", value)

				// doc.add(new Field(aFieldname, sValue,
				// Field.Store.NO,Field.Index.ANALYZED));
				doc.add(new TextField(aFieldname, sValue, Store.NO));
			} else {
				// do not analyse content of index fields!
				// doc.add(new Field(aFieldname, sValue,
				// Field.Store.NO,Field.Index.NOT_ANALYZED));
				doc.add(new StringField(aFieldname, sValue, Store.NO));
			}
		}

	}

}
