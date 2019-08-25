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

package org.imixs.workflow.engine.index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.workflow.ItemCollection;

/**
 * The IndexSchemaService provides the index Schema.
 * 
 * @version 1.0
 * @author rsoika
 */
@Singleton
public class SchemaService  {

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

	private List<String> fieldList = null;
	private List<String> fieldListAnalyse = null;
	private List<String> fieldListNoAnalyse = null;
	private List<String> fieldListStore = null;

	// default field lists
	private static List<String> DEFAULT_SEARCH_FIELD_LIST = Arrays.asList("$workflowsummary", "$workflowabstract");
	private static List<String> DEFAULT_NOANALYSE_FIELD_LIST = Arrays.asList("$modelversion", "$taskid", "$processid",
			"$workitemid", "$uniqueidref", "type", "$writeaccess", "$modified", "$created", "namcreator", "$creator",
			"$editor", "$lasteditor", "$workflowgroup", "$workflowstatus", "txtworkflowgroup", "name", "txtname",
			"$owner", "namowner", "txtworkitemref", "$uniqueidsource", "$uniqueidversions", "$lasttask", "$lastevent",
			"$lasteventdate");
	private static List<String> DEFAULT_STORE_FIELD_LIST = Arrays.asList("type", "$taskid", "$writeaccess",
			"$workflowsummary", "$workflowabstract", "$workflowgroup", "$workflowstatus", "$modified", "$created",
			"$lasteventdate", "$creator", "$editor", "$lasteditor", "$owner", "namowner");

	

	private static Logger logger = Logger.getLogger(SchemaService.class.getName());

	/**
	 * PostContruct event - The method loads the lucene index properties from the
	 * imixs.properties file from the classpath. If no properties are defined the
	 * method terminates.
	 * 
	 */
	@PostConstruct
	void init() {
		
		logger.finest("......lucene FulltextFieldList=" + luceneFulltextFieldList);
		logger.finest("......lucene IndexFieldListAnalyse=" + luceneIndexFieldListAnalyse);
		logger.finest("......lucene IndexFieldListNoAnalyse=" + luceneIndexFieldListNoAnalyse);

		// compute search field list
		fieldList = new ArrayList<String>();
		// add all static default field list
		fieldList.addAll(DEFAULT_SEARCH_FIELD_LIST);
		if (luceneFulltextFieldList != null && !luceneFulltextFieldList.isEmpty()) {
			StringTokenizer st = new StringTokenizer(luceneFulltextFieldList, ",");
			while (st.hasMoreElements()) {
				String sName = st.nextToken().toLowerCase().trim();
				// do not add internal fields
				if (!"$uniqueid".equals(sName) && !"$readaccess".equals(sName) && !fieldList.contains(sName))
					fieldList.add(sName);
			}
		}

		// compute Index field list (Analyze)
		fieldListAnalyse = new ArrayList<String>();
		if (luceneIndexFieldListAnalyse != null && !luceneIndexFieldListAnalyse.isEmpty()) {
			StringTokenizer st = new StringTokenizer(luceneIndexFieldListAnalyse, ",");
			while (st.hasMoreElements()) {
				String sName = st.nextToken().toLowerCase().trim();
				// do not add internal fields
				if (!"$uniqueid".equals(sName) && !"$readaccess".equals(sName))
					fieldListAnalyse.add(sName);
			}
		}

		// compute Index field list (NoAnalyze)
		fieldListNoAnalyse = new ArrayList<String>();
		// add all static default field list
		fieldListNoAnalyse.addAll(DEFAULT_NOANALYSE_FIELD_LIST);
		if (luceneIndexFieldListNoAnalyse != null && !luceneIndexFieldListNoAnalyse.isEmpty()) {
			// add additional field list from imixs.properties
			StringTokenizer st = new StringTokenizer(luceneIndexFieldListNoAnalyse, ",");
			while (st.hasMoreElements()) {
				String sName = st.nextToken().toLowerCase().trim();
				if (!fieldListNoAnalyse.contains(sName))
					fieldListNoAnalyse.add(sName);
			}
		}

		// compute Index field list (Store)
		fieldListStore = new ArrayList<String>();
		// add all static default field list
		fieldListStore.addAll(DEFAULT_STORE_FIELD_LIST);
		if (luceneIndexFieldListStore != null && !luceneIndexFieldListStore.isEmpty()) {
			// add additional field list from imixs.properties
			StringTokenizer st = new StringTokenizer(luceneIndexFieldListStore, ",");
			while (st.hasMoreElements()) {
				String sName = st.nextToken().toLowerCase().trim();
				if (!fieldListStore.contains(sName))
					fieldListStore.add(sName);
			}
		}

		// Issue #518:
		// if a field of the indexFieldListStore is not part of the
		// indexFieldListAnalyse , than we add these fields to the indexFieldListAnalyse
		// This is to guaranty that we store the field value in any case.
		for (String fieldName : fieldListStore) {
			if (!fieldListAnalyse.contains(fieldName)) {
				// add this field into he indexFieldListAnalyse
				fieldListAnalyse.add(fieldName);
			}
		}

	}
	
	
	
	

	public List<String> getFieldList() {
		return fieldList;
	}







	public List<String> getFieldListAnalyse() {
		return fieldListAnalyse;
	}





	public List<String> getFieldListNoAnalyse() {
		return fieldListNoAnalyse;
	}





	public List<String> getFieldListStore() {
		return fieldListStore;
	}





	/**
	 * Returns the Lucene schema configuration
	 * 
	 * @return
	 */
	public ItemCollection getConfiguration() {
		ItemCollection config = new ItemCollection();

		config.replaceItemValue("lucence.fulltextFieldList", fieldList);
		config.replaceItemValue("lucence.indexFieldListAnalyze", fieldListAnalyse);
		config.replaceItemValue("lucence.indexFieldListNoAnalyze", fieldListNoAnalyse);
		config.replaceItemValue("lucence.indexFieldListStore", fieldListStore);

		return config;
	}

	
	
}
