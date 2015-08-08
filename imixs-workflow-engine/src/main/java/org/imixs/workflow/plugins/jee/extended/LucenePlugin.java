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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParser.Operator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.jee.ejb.WorkflowService;
import org.imixs.workflow.plugins.AbstractPlugin;

/**
 * This Plugin add workitems to a lucene search index. The Plugin provides a set
 * of static methods which can be used also outside the workflowManager to index
 * single workitems or collections of workitems.
 * 
 * With the method addWorkitem() a ItemCollection can be added to a lucene
 * search index. The Plugin reads the property file 'imixs.properties' from the
 * current classpath to determine the configuration.
 * 
 * <ul>
 * <li>The property "IndexDir" defines the location of the lucene index
 * <li>The property "FulltextFieldList" lists all fields which should be
 * searchable after a workitem was updated
 * <li>The property "IndexFieldList" lists all fields which should be indexed as
 * keywords by the lucene search engine
 * 
 * If the plugin is used as worflow pugin in the model definition, the plugin
 * should be run last to be sure that newly computed values like the workflow
 * status or the wokflowSummary are indexed correctly
 * 
 * 
 * Updated to version 4.5.1
 * 
 * @author rsoika
 * @version 4.5.1 (Lucene)
 * 
 */
public class LucenePlugin extends AbstractPlugin {
	public static final String UNDEFINED_ERROR = "UNDEFINED_ERROR";
	public static final String INVALID_INDEX = "INVALID_INDEX";

	// Properties properties = null;
	IndexWriter writer = null;
	static List<String> searchFieldList = null;
	static List<String> indexFieldListAnalyse = null;
	static List<String> indexFieldListNoAnalyse = null;
	private static Logger logger = Logger.getLogger(LucenePlugin.class
			.getName());

	private static int maxResult = 100;

	/**
	 * returns the maximum size of a search result
	 * 
	 * @return
	 */
	public int getMaxResult() {
		return maxResult;
	}

	/**
	 * set the maximum size of a search result
	 * 
	 * @param searchCount
	 */
	public void setMaxResult(int searchCount) {
		LucenePlugin.maxResult = searchCount;
	}

	@Override
	public void init(WorkflowContext actx) throws PluginException {
		super.init(actx);
	}

	/**
	 * This method adds the current workitem to the search index by calling the
	 * method addWorkitem. The method computes temporarily the field $processid
	 * based on the numnextprocessid from thE activty entity. This will ensure
	 * that the workitem is indexed correctly on the $processid the workitem
	 * will hold after the process step is completed.
	 * 
	 * Also the $modified and Created date will be set temporarily for the case
	 * that we process a new WorkItem which was not yet saved by the
	 * entityService.
	 * 
	 * If and how the workitem will be added to the search index is fully
	 * controlled by the method addWorkitem.
	 */
	public int run(ItemCollection documentContext, ItemCollection activity)
			throws PluginException {

		//logger.info("Lucene ImplementationVersion="+LucenePackage.get().getImplementationVersion());
		// compute next $processid to be added correctly into the search index
		int nextProcessID = activity.getItemValueInteger("numnextprocessid");
		int currentProcessID = documentContext
				.getItemValueInteger("$processid");

		// temporarily set a $Created and $Modified Date (used to search for
		// $modified)
		if (!documentContext.hasItem("$Created")) {
			documentContext.replaceItemValue("$Created", Calendar.getInstance()
					.getTime());
		}
		documentContext.replaceItemValue("$modified", Calendar.getInstance()
				.getTime());

		// temporarily replace the $processid
		documentContext.replaceItemValue("$processid", nextProcessID);
		// update the search index for the current Worktitem
		updateWorkitem(documentContext);
		// restore $processid
		documentContext.replaceItemValue("$processid", currentProcessID);

		return Plugin.PLUGIN_OK;
	}

	public void close(int status) throws PluginException {

	}

	/**
	 * This method adds a single workitem into the search index. The adds the
	 * workitem into a empty Collection and calls teh method addWorklist.
	 * 
	 * @param documentContext
	 * @return
	 * @throws Exception
	 */
	public static boolean updateWorkitem(ItemCollection documentContext)
			throws PluginException {
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
	 * The method loads the lucene index properties from the imixs.properties
	 * file from the classpath. If no properties are defined the method
	 * terminates.
	 * 
	 * 
	 * @param worklist
	 *            of ItemCollections to be indexed
	 * @return - true if the update was successfull
	 * @throws Exception
	 */
	public static boolean updateWorklist(Collection<ItemCollection> worklist)
			throws PluginException {

		IndexWriter awriter = null;
		// try loading imixs-search properties
		Properties prop = loadProperties();
		if (prop.isEmpty())
			return false;

		try {
			awriter = createIndexWriter(prop);

			// add workitem to search index....

			for (ItemCollection workitem : worklist) {
				// create term
				Term term = new Term("$uniqueid",
						workitem.getItemValueString("$uniqueid"));
				// test if document should be indexed or not
				if (matchConditions(prop, workitem)) {
					logger.fine("add workitem '"
							+ workitem
									.getItemValueString(EntityService.UNIQUEID)
							+ "' into index");
					awriter.updateDocument(term, createDocument(workitem));
				} else {
					logger.fine("remove workitem '"
							+ workitem
									.getItemValueString(EntityService.UNIQUEID)
							+ "' into index");
					awriter.deleteDocuments(term);
				}
			}
		} catch (IOException luceneEx) {
			// close writer!
			logger.warning(" Lucene Exception : " + luceneEx.getMessage());

			throw new PluginException(LucenePlugin.class.getSimpleName(),
					INVALID_INDEX, "Unable to update search index", luceneEx);

		} finally {

			if (awriter != null) {
				logger.fine(" close writer");
				try {
					awriter.close();					
				} catch (CorruptIndexException e) {
					throw new PluginException(
							LucenePlugin.class.getSimpleName(), INVALID_INDEX,
							"Unable to update search index", e);
				} catch (IOException e) {
					throw new PluginException(
							LucenePlugin.class.getSimpleName(), INVALID_INDEX,
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
	public static void removeWorkitem(String uniqueID) throws PluginException {
		IndexWriter awriter = null;
		Properties prop = loadProperties();
		if (!prop.isEmpty()) {
			try {
				awriter = createIndexWriter(prop);
				Term term = new Term("$uniqueid", uniqueID);
				awriter.deleteDocuments(term);
			} catch (CorruptIndexException e) {
				throw new PluginException(LucenePlugin.class.getSimpleName(),
						INVALID_INDEX, "Unable to remove workitem '" + uniqueID
								+ "' from search index", e);
			} catch (LockObtainFailedException e) {
				throw new PluginException(LucenePlugin.class.getSimpleName(),
						INVALID_INDEX, "Unable to remove workitem '" + uniqueID
								+ "' from search index", e);
			} catch (IOException e) {
				throw new PluginException(LucenePlugin.class.getSimpleName(),
						INVALID_INDEX, "Unable to remove workitem '" + uniqueID
								+ "' from search index", e);
			}
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
	public static boolean matchConditions(Properties prop,
			ItemCollection aworktiem) {

		String typePattern = prop.getProperty("lucence.matchingType");
		String processIDPattern = prop.getProperty("lucence.matchingProcessID");

		String type = aworktiem.getItemValueString("Type");
		String sPid = aworktiem.getItemValueInteger("$Processid") + "";

		// test type pattern
		if (typePattern != null && !"".equals(typePattern)
				&& !type.matches(typePattern)) {
			logger.fine("Lucene type '" + type + "' did not match pattern '"
					+ typePattern + "'");
			return false;
		}

		// test $processid pattern
		if (processIDPattern != null && !"".equals(processIDPattern)
				&& !sPid.matches(processIDPattern)) {
			logger.fine("Lucene $processid '" + sPid
					+ "' did not match pattern '" + processIDPattern + "'");

			return false;
		}
		return true;
	}

	/**
	 * Returns a ItemCollection List matching the provided search term. The
	 * provided search team will we extended with a users roles to test the read
	 * access level of each workitem matching the search term. The usernames and
	 * user roles will be search lowercase!
	 * 
	 * @param sSearchTerm
	 * @param workflowService
	 * @return collection of search result
	 */
	public static List<ItemCollection> search(String sSearchTerm,
			WorkflowService workflowService) {
		// no sort order
		return search(sSearchTerm, workflowService, null, null);
	}

	/**
	 * Returns a ItemCollection List matching the provided search term. The
	 * provided search team will we extended with a users roles to test the read
	 * access level of each workitem matching the search term. The usernames and
	 * user roles will be search lowercase!
	 * 
	 * The optional param 'searchOrder' can be set to force lucene to sort the
	 * search result by any search order.
	 * 
	 * @param sSearchTerm
	 * @param workflowService
	 * @param sortOrder
	 *            - optional to sort the result
	 * @return collection of search result
	 */
	public static List<ItemCollection> search(String sSearchTerm,
			WorkflowService workflowService, Sort sortOrder) {
		// no default operator
		return search(sSearchTerm, workflowService, sortOrder, null);
	}

	/**
	 * Returns a ItemCollection List matching the provided search term. The
	 * provided search team will we extended with a users roles to test the read
	 * access level of each workitem matching the search term. The usernames and
	 * user roles will be search lowercase!
	 * 
	 * The optional param 'searchOrder' can be set to force lucene to sort the
	 * search result by any search order.
	 * 
	 * The optional param 'defaultOperator' can be set to Operator.AND
	 * 
	 * @param sSearchTerm
	 * @param workflowService
	 * @param sortOrder
	 *            - optional to sort the result
	 * @param defaultOperator
	 *            - optional to change the default search operator
	 * @return collection of search result
	 */
	public static List<ItemCollection> search(String sSearchTerm,
			WorkflowService workflowService, Sort sortOrder,
			Operator defaultOperator) {

		ArrayList<ItemCollection> workitems = new ArrayList<ItemCollection>();

		// test if searchtem is provided
		if (sSearchTerm == null || "".equals(sSearchTerm))
			return workitems;

		long ltime = System.currentTimeMillis();
		Properties prop = loadProperties();
		if (prop.isEmpty())
			return workitems;

		try {
			IndexSearcher searcher = createIndexSearcher(prop);
			QueryParser parser = createQueryParser(prop);

			// extend the Search Term
			if (!workflowService
					.isUserInRole(EntityService.ACCESSLEVEL_MANAGERACCESS)) {
				// get user names list
				List<String> userNameList = workflowService.getUserNameList();
				// create search term
				String sAccessTerm = "($readaccess:ANONYMOUS";
				for (String aRole : userNameList) {
					if (!"".equals(aRole))
						sAccessTerm += " $readaccess:\"" + aRole + "\"";
				}
				sAccessTerm += ") AND ";
				sSearchTerm = sAccessTerm + sSearchTerm;
			}
			logger.fine("  lucene search:" + sSearchTerm);

			if (!"".equals(sSearchTerm)) {
				parser.setAllowLeadingWildcard(true);

				// set default operator?
				if (defaultOperator != null)
					parser.setDefaultOperator(defaultOperator);

				TopDocs topDocs = null;
				if (sortOrder != null) {
					logger.fine(" sortOrder= '" + sortOrder+"' ");

					topDocs = searcher.search(parser.parse(sSearchTerm),
							maxResult, sortOrder);
				} else {
					topDocs = searcher.search(parser.parse(sSearchTerm),
							maxResult);
				}

				logger.fine("  total hits=" + topDocs.totalHits);

				// Get an array of references to matched documents
				ScoreDoc[] scoreDosArray = topDocs.scoreDocs;
				for (ScoreDoc scoredoc : scoreDosArray) {
					// Retrieve the matched document and show relevant details
					Document doc = searcher.doc(scoredoc.doc);

					String sID = doc.get("$uniqueid");
					logger.fine("  lucene lookup $uniqueid=" + sID);
					ItemCollection itemCol = workflowService.getEntityService()
							.load(sID);
					if (itemCol != null) {
						workitems.add(itemCol);
					} else {
						logger.warning("[LucenePlugin] index returned un unreadable workitem : "
								+ sID);
						// this situation happens if the search index returned
						// documents
						// the current user has no read access.
						// this should normally avoided with the $readaccess
						// search phrase!
						// So if this happens we need to check the
						// createDocument method!
					}
				}

			}

			searcher.getIndexReader().close();

			logger.fine(" lucene serach: "
					+ (System.currentTimeMillis() - ltime) + " ms");
		} catch (Exception e) {
			logger.warning("  lucene error!");
			e.printStackTrace();
		}

		return workitems;
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
	static Document createDocument(ItemCollection aworkitem) {
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
					SimpleDateFormat dateformat = new SimpleDateFormat(
							"yyyyMMddHHmm");
					// convert calendar to string
					String sDateValue;
					if (o instanceof Calendar)
						sDateValue = dateformat
								.format(((Calendar) o).getTime());
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
		doc.add(new StringField("$uniqueid", aworkitem
				.getItemValueString("$uniqueid"), Store.YES));

		// add default values $readAccess
		List<String> vReadAccess = aworkitem.getItemValue("$readAccess");
		if (vReadAccess.size() == 0
				|| (vReadAccess.size() == 1 && "".equals(vReadAccess.get(0)
						.toString()))) {
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
	static void addFieldValue(Document doc, ItemCollection aworkitem,
			String aFieldname, boolean analyzeValue) {
		String sValue = null;
		List<?> vValues = aworkitem.getItemValue(aFieldname);
		if (vValues.size() == 0)
			return;
		if (vValues.get(0) == null)
			return;

		for (Object singleValue : vValues) {

			// Object o = vValues.firstElement();
			if (singleValue instanceof Calendar || singleValue instanceof Date) {
				SimpleDateFormat dateformat = new SimpleDateFormat(
						"yyyyMMddHHmm");

				// convert calendar to string
				String sDateValue;
				if (singleValue instanceof Calendar)
					sDateValue = dateformat.format(((Calendar) singleValue)
							.getTime());
				else
					sDateValue = dateformat.format((Date) singleValue);
				sValue = sDateValue;

			} else
				// simple string representation
				sValue = singleValue.toString();

			logger.fine("  add IndexField (analyse=" + analyzeValue + "): "
					+ aFieldname + " = " + sValue);
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
	public static IndexWriter createIndexWriter(Properties prop)
			throws IOException {

		/**
		 * Read configuration
		 */
		// String sLuceneVersion = prop.getProperty("Version", "LUCENE_45");

		String sIndexDir = prop.getProperty("lucence.indexDir");
		String sFulltextFieldList = prop
				.getProperty("lucence.fulltextFieldList");
		String sIndexFieldListAnalyse = prop
				.getProperty("lucence.indexFieldListAnalyze");
		String sIndexFieldListNoAnalyse = prop
				.getProperty("lucence.indexFieldListNoAnalyze");

		logger.fine("IndexDir:" + sIndexDir);
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

		// compute Index field list (Analyze)
		st = new StringTokenizer(sIndexFieldListNoAnalyse, ",");
		indexFieldListNoAnalyse = new ArrayList<String>();
		while (st.hasMoreElements()) {
			String sName = st.nextToken().toLowerCase();
			// do not add internal fields
			if (!"$uniqueid".equals(sName) && !"$readaccess".equals(sName))
				indexFieldListNoAnalyse.add(sName);
		}

		/**
		 * Now create a IndexWriter Instance
		 */
		Directory indexDir = createIndexDirectory(prop);

		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig indexWriterConfig = new IndexWriterConfig(
				Version.LATEST, analyzer);

		// set the WriteLockTimeout to wait for a write lock (in milliseconds)
		// for this instance.
		// 10 seconds!
		indexWriterConfig.setWriteLockTimeout(10000);

		// there is no need to unlock the index if we set the timeout to 10
		// seconds
		// if (IndexWriter.isLocked(indexDir)) {
		// logger.warning("Lucene IndexWriter was locked! - try to unlock....");
		// IndexWriter.unlock(indexDir);
		// }
		return new IndexWriter(indexDir, indexWriterConfig);

	}

	/**
	 * returns a IndexSearcher instance
	 * 
	 * @param prop
	 * @return
	 * @throws Exception
	 */
	public static IndexSearcher createIndexSearcher(Properties prop)
			throws Exception {
		logger.fine("[LucenePlugin] createIndexSearcher...");

		Directory indexDir = createIndexDirectory(prop);
		IndexReader reader = DirectoryReader.open(indexDir);
		IndexSearcher searcher = new IndexSearcher(reader);

		return searcher;
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
	public static Directory createIndexDirectory(Properties prop)
			throws IOException {

		logger.fine("[LucenePlugin] createIndexDirectory...");
		/**
		 * Read configuration
		 */
		String sLuceneLockFactory = prop.getProperty("lucence.lockFactory");
		String sIndexDir = prop.getProperty("lucence.indexDir");

		Directory indexDir = FSDirectory.open(new File(sIndexDir));

		// set lockFactory
		// NativeFSLockFactory: using native OS file locks
		// SimpleFSLockFactory: recommended for NFS based access to an index,
		if (sLuceneLockFactory != null && !"".equals(sLuceneLockFactory)) {
			// indexDir.setLockFactory(new SimpleFSLockFactory());
			// set factory by class name
			logger.fine("[LucenePlugin] set LockFactory=" + sLuceneLockFactory);
			try {
				Class<?> fsFactoryClass;
				fsFactoryClass = Class.forName(sLuceneLockFactory);
				LockFactory factoryInstance = (LockFactory) fsFactoryClass
						.newInstance();
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
	 * Returns in instance of a QueyParser based on a KeywordAnalyser
	 * 
	 * @param prop
	 * @return
	 */
	public static QueryParser createQueryParser(Properties prop) {
		// String sLuceneVersion = prop.getProperty("Version", "LUCENE_45");
		Analyzer analyzer = new KeywordAnalyzer();
		QueryParser parser = new QueryParser("content",
				analyzer);

		// check the default operator
		String defaultOperator = prop.getProperty("lucene.defaultOperator");
		if (defaultOperator != null
				&& "AND".equals(defaultOperator.toUpperCase())) {
			parser.setDefaultOperator(Operator.AND);
		}

		return parser;
	}

	/**
	 * loads a imixs-search.property file
	 * 
	 * @return
	 * @throws Exception
	 */
	static Properties loadProperties() {
		// try loading imixs-search properties
		Properties prop = new Properties();
		try {
			prop.load(Thread.currentThread().getContextClassLoader()
					.getResource("imixs.properties").openStream());
		} catch (Exception ep) {
			// no properties found
		}
		return prop;
	}

}
