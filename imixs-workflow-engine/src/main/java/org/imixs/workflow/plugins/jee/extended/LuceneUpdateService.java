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

package org.imixs.workflow.plugins.jee.extended;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
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
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.jee.util.PropertyService;

/**
 * The LuceneUpdateService is a singleton EJB providing a service to update the
 * lucene index.
 * 
 * 
 * The service provides a set of public methods which can be used to updated a
 * lucen index.
 * 
 * With the method addWorkitem() a ItemCollection can be added to a lucene
 * search index. The service reads the property file 'imixs.properties' from the
 * current classpath to determine the configuration.
 * 
 * <ul>
 * <li>The property "IndexDir" defines the location of the lucene index
 * <li>The property "FulltextFieldList" lists all fields which should be
 * searchable after a workitem was updated
 * <li>The property "IndexFieldList" lists all fields which should be indexed as
 * keywords by the lucene search engine
 * 
 * If the service is used also by the LucenPlugin
 * 
 * 
 * Updated to version 4.5.1
 * 
 * 
 * The singleton pattern is used to avoid conflicts within multi thread
 * szenarios.
 * 
 * @see http://stackoverflow.com/questions/34880347/why-did-lucene-indexwriter-
 *      did-not-update-the-index-when-called-from-a-web-modul
 * @see LucenePlugin
 * @version 1.0
 * @author rsoika
 */
@Singleton
public class LuceneUpdateService {

	public static final String UNDEFINED_ERROR = "UNDEFINED_ERROR";
	public static final String INVALID_INDEX = "INVALID_INDEX";

	private List<String> searchFieldList = null;
	private List<String> indexFieldListAnalyse = null;
	private List<String> indexFieldListNoAnalyse = null;
	private String indexDirectoryPath = null;
	private String luceneLockFactory = null;
	private Properties properties = null;

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

		// try loading imixs-search properties
		properties = propertyService.getProperties();

		/**
		 * Read configuration
		 */
		// String sLuceneVersion = prop.getProperty("Version", "LUCENE_45");

		indexDirectoryPath = properties.getProperty("lucence.indexDir");
		luceneLockFactory = properties.getProperty("lucence.lockFactory");

		String sFulltextFieldList = properties.getProperty("lucence.fulltextFieldList");
		String sIndexFieldListAnalyse = properties.getProperty("lucence.indexFieldListAnalyze");
		String sIndexFieldListNoAnalyse = properties.getProperty("lucence.indexFieldListNoAnalyze");

		logger.fine("IndexDir:" + indexDirectoryPath);
		logger.fine("FulltextFieldList:" + sFulltextFieldList);
		logger.fine("IndexFieldListAnalyse:" + sIndexFieldListAnalyse);
		logger.fine("IndexFieldListNoAnalyse:" + sIndexFieldListNoAnalyse);
		// compute search field list
		StringTokenizer st = new StringTokenizer(sFulltextFieldList, ",");
		searchFieldList = new ArrayList<String>();
		while (st.hasMoreElements()) {
			String sName = st.nextToken().toLowerCase();
			// do not add internal fields
			if (!"$uniqueid".equals(sName) && !"$readaccess".equals(sName))
				searchFieldList.add(sName);
		}

		// compute Index field list (Analyze)
		st = new StringTokenizer(sIndexFieldListAnalyse, ",");
		indexFieldListAnalyse = new ArrayList<String>();
		while (st.hasMoreElements()) {
			String sName = st.nextToken().toLowerCase();
			// do not add internal fields
			if (!"$uniqueid".equals(sName) && !"$readaccess".equals(sName))
				indexFieldListAnalyse.add(sName);
		}

		// compute Index field list (NoAnalyze)
		st = new StringTokenizer(sIndexFieldListNoAnalyse, ",");
		indexFieldListNoAnalyse = new ArrayList<String>();
		while (st.hasMoreElements()) {
			String sName = st.nextToken().toLowerCase();
			// do not add internal fields
			if (!"$uniqueid".equals(sName) && !"$readaccess".equals(sName))
				indexFieldListNoAnalyse.add(sName);
		}
	}

	/**
	 * This method adds a single workitem into the search index. The adds the
	 * workitem into a empty Collection and calls teh method addWorklist.
	 * 
	 * @param documentContext
	 * @return
	 * @throws Exception
	 */
	public boolean updateWorkitem(ItemCollection documentContext) throws PluginException {
		List<ItemCollection> workitems = new ArrayList<ItemCollection>();

		workitems.add(documentContext);

		updateWorklist(workitems);

		return true;
	}

	/**
	 * This method updates the search index for a collection of workitems.
	 * 
	 * For each workitem the method test if it did match the conditions to be
	 * added into the search index. If the workitem did not match the conditions
	 * the workitem will be removed from the index.
	 * 
	 * 
	 * @param worklist
	 *            of ItemCollections to be indexed
	 * @return - true if the update was successfull
	 * @throws Exception
	 */
	public boolean updateWorklist(Collection<ItemCollection> worklist) throws PluginException {

		IndexWriter awriter = null;

		try {
			awriter = createIndexWriter();

			// add workitem to search index....

			for (ItemCollection workitem : worklist) {
				// create term
				Term term = new Term("$uniqueid", workitem.getItemValueString("$uniqueid"));
				// test if document should be indexed or not
				if (matchConditions(workitem)) {
					logger.fine(
							"add workitem '" + workitem.getItemValueString(EntityService.UNIQUEID) + "' into index");
					awriter.updateDocument(term, createDocument(workitem));
				} else {
					logger.fine(
							"remove workitem '" + workitem.getItemValueString(EntityService.UNIQUEID) + "' into index");
					awriter.deleteDocuments(term);
				}
			}
		} catch (IOException luceneEx) {
			// close writer!
			logger.warning(" Lucene Exception : " + luceneEx.getMessage());

			throw new PluginException(LucenePlugin.class.getSimpleName(), INVALID_INDEX,
					"Unable to update search index", luceneEx);

		} finally {

			if (awriter != null) {
				logger.fine(" close writer");
				try {
					awriter.close();
				} catch (CorruptIndexException e) {
					throw new PluginException(LucenePlugin.class.getSimpleName(), INVALID_INDEX,
							"Unable to update search index", e);
				} catch (IOException e) {
					throw new PluginException(LucenePlugin.class.getSimpleName(), INVALID_INDEX,
							"Unable to update search index", e);
				}

			}
		}

		logger.fine(" update worklist successfull");
		return true;
	}

	/**
	 * This method removes a single workitem from the search index.
	 * 
	 * @param uniqueID
	 *            of the workitem to be removed
	 * @throws PluginException
	 */
	public void removeWorkitem(String uniqueID) throws PluginException {
		IndexWriter awriter = null;

		try {
			awriter = createIndexWriter();
			Term term = new Term("$uniqueid", uniqueID);
			awriter.deleteDocuments(term);
		} catch (CorruptIndexException e) {
			throw new PluginException(LucenePlugin.class.getSimpleName(), INVALID_INDEX,
					"Unable to remove workitem '" + uniqueID + "' from search index", e);
		} catch (LockObtainFailedException e) {
			throw new PluginException(LucenePlugin.class.getSimpleName(), INVALID_INDEX,
					"Unable to remove workitem '" + uniqueID + "' from search index", e);
		} catch (IOException e) {
			throw new PluginException(LucenePlugin.class.getSimpleName(), INVALID_INDEX,
					"Unable to remove workitem '" + uniqueID + "' from search index", e);
		}

	}

	/**
	 * test if the workitem matches the conditions to be added into the search
	 * index. The Property keys MatchingType and MatchingProcessID can provide
	 * regular expressions
	 * 
	 * @param aworktiem
	 * @return
	 */
	public boolean matchConditions(ItemCollection aworktiem) {

		String typePattern = properties.getProperty("lucence.matchingType");
		String processIDPattern = properties.getProperty("lucence.matchingProcessID");

		String type = aworktiem.getItemValueString("Type");
		String sPid = aworktiem.getItemValueInteger("$Processid") + "";

		// test type pattern
		if (typePattern != null && !"".equals(typePattern) && !type.matches(typePattern)) {
			logger.fine("Lucene type '" + type + "' did not match pattern '" + typePattern + "'");
			return false;
		}

		// test $processid pattern
		if (processIDPattern != null && !"".equals(processIDPattern) && !sPid.matches(processIDPattern)) {
			logger.fine("Lucene $processid '" + sPid + "' did not match pattern '" + processIDPattern + "'");

			return false;
		}
		return true;
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

		/**
		 * Now create a IndexWriter Instance
		 */
		Directory indexDir = createIndexDirectory();

		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LATEST, analyzer);

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

		logger.fine("[LucenePlugin] createIndexDirectory...");
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
			logger.fine("[LuceneUpdateService] set LockFactory=" + luceneLockFactory);
			try {
				Class<?> fsFactoryClass;
				fsFactoryClass = Class.forName(luceneLockFactory);
				LockFactory factoryInstance = (LockFactory) fsFactoryClass.newInstance();
				indexDir.setLockFactory(factoryInstance);
			} catch (ClassNotFoundException e) {
				logger.severe("[LucenePlugin] unable to create Lucene LockFactory!");
				e.printStackTrace();
				return null;
			} catch (InstantiationException e) {
				logger.severe("[LucenePlugin] unable to create Lucene LockFactory!");
				e.printStackTrace();
				return null;
			} catch (IllegalAccessException e) {
				logger.severe("[LucenePlugin] unable to create Lucene LockFactory!");
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
		// ('content')
		// for the lucene document
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
				logger.fine("  add SearchField: " + aFieldname + " = " + sValue);
				sContent += sValue + ",";
			}
		}
		logger.fine("  content = " + sContent);

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

			logger.fine("  add IndexField (analyse=" + analyzeValue + "): " + aFieldname + " = " + sValue);
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
				// do not nalyse content of index fields!
				// doc.add(new Field(aFieldname, sValue,
				// Field.Store.NO,Field.Index.NOT_ANALYZED));
				doc.add(new StringField(aFieldname, sValue, Store.NO));
			}
		}

	}

}
