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

package org.imixs.workflow.engine.plugins;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.exceptions.PluginException;

/**
 * This abstract class implements different helper methods used by subclasses
 * 
 * @author Ralph Soika
 * @version 1.1
 * @see org.imixs.workflow.WorkflowManager
 * 
 */

public abstract class AbstractPlugin implements Plugin {

	public static final String INVALID_ITEMVALUE_FORMAT = "INVALID_ITEMVALUE_FORMAT";
	public static final String INVALID_PROPERTYVALUE_FORMAT = "INVALID_PROPERTYVALUE_FORMAT";

	private WorkflowContext ctx;
	private WorkflowService workflowService;

	/**
	 * Initialize Plugin and get an instance of the EJB Session Context
	 */
	public void init(WorkflowContext actx) throws PluginException {
		ctx = actx;
		// get WorkflowService by check for an instance of WorkflowService
		if (actx instanceof WorkflowService) {
			// yes we are running in a WorkflowService EJB
			workflowService = (WorkflowService) actx;
		}
	}

	@Override
	public void close(boolean rollbackTransaction) throws PluginException {

	}

	public WorkflowContext getCtx() {
		return ctx;
	}

	/**
	 * Returns an instance of the WorkflowService EJB.
	 * 
	 * @return
	 */
	public WorkflowService getWorkflowService() {
		return workflowService;
	}

	
	/**
	 * This method merges the values of fieldList into valueList and test for
	 * duplicates.
	 * 
	 * If an entry of the fieldList is a single key value, than the values to be
	 * merged are read from the corresponding documentContext property
	 * 
	 * e.g. 'namTeam' -> maps the values of the documentContext property 'namteam'
	 * into the valueList
	 * 
	 * If an entry of the fieldList is in square brackets, than the comma separated
	 * elements are mapped into the valueList
	 * 
	 * e.g. '[user1,user2]' - maps the values 'user1' and 'user2' int the valueList.
	 * Also Curly brackets are allowed '{user1,user2}'
	 * 
	 * 
	 * @param valueList
	 * @param fieldList
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void mergeFieldList(ItemCollection documentContext, List valueList, List<String> fieldList) {
		if (valueList == null || fieldList == null)
			return;
		List<?> values = null;
		if (fieldList.size() > 0) {
			// iterate over the fieldList
			for (String key : fieldList) {
				if (key == null) {
					continue;
				}
				key = key.trim();
				// test if key contains square or curly brackets?
				if ((key.startsWith("[") && key.endsWith("]")) || (key.startsWith("{") && key.endsWith("}"))) {
					// extract the value list with regExpression (\s matches any
					// white space, The * applies the match zero or more times.
					// So \s* means "match any white space zero or more times".
					// We look for this before and after the comma.)
					values = Arrays.asList(key.substring(1, key.length() - 1).split("\\s*,\\s*"));
				} else {
					// extract value list form documentContext
					values = documentContext.getItemValue(key);
				}
				// now append the values into p_VectorDestination
				if ((values != null) && (values.size() > 0)) {
					for (Object o : values) {
						// append only if not used
						if (valueList.indexOf(o) == -1)
							valueList.add(o);
					}
				}
			}
		}

	}

	/**
	 * This method removes duplicates and null values from a vector.
	 * 
	 * @param valueList
	 *            - list of elements
	 */
	public List<?> uniqueList(List<Object> valueList) {
		int iVectorSize = valueList.size();
		Vector<Object> cleanedVector = new Vector<Object>();

		for (int i = 0; i < iVectorSize; i++) {
			Object o = valueList.get(i);
			if (o == null || cleanedVector.indexOf(o) > -1 || "".equals(o.toString()))
				continue;

			// add unique object
			cleanedVector.add(o);
		}
		valueList = cleanedVector;
		// do not work with empty vectors....
		if (valueList.size() == 0)
			valueList.add("");

		return valueList;
	}

	
}