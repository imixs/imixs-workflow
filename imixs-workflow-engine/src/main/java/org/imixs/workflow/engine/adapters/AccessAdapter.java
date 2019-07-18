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

package org.imixs.workflow.engine.adapters;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;

import org.imixs.workflow.GenericAdapter;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.exceptions.AdapterException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;

/**
 * The AccessAdapter is a generic adapter class responsible to update the
 * ACL of a workitem. The CID Bean updates the following Items
 * <ul>
 * <li>$writeAccess</li>
 * <li>$readAccess</li>
 * <li>$participants</li>
 * </ul>
 * <p>
 * The read and write access for a workitem can be defined by the BPMN model
 * with the ACL Properties of the Imixs-BPMN modeler.
 * <p>
 * The participants is a computed list of all users who edited this workitem.  
 * <p>
 * By defining an CDI alternative an application can overwrite the behavior of
 * this bean.
 * 
 * @author rsoika
 * @version 1.0.0
 */
@Named
public class AccessAdapter implements GenericAdapter, Serializable {

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(AccessAdapter.class.getName());

	// See CDI Constructor
	protected WorkflowService workflowService;

	/**
	 * Default Constructor
	 */
	public AccessAdapter() {
		super();
	}

	/**
	 * CDI Constructor to inject WorkflowService
	 * 
	 * @param workflowService
	 */
	@Inject
	public AccessAdapter(WorkflowService workflowService) {
		super();
		this.workflowService = workflowService;
	}

	@Override
	public ItemCollection execute(ItemCollection document, ItemCollection event) throws AdapterException {
		ItemCollection nextTask = null;
		// get next process entity
		try {
			nextTask = workflowService.evalNextTask(document, event);

			updateACL(document, event, nextTask);
			updateParticipants(document);

		} catch (ModelException | PluginException e) {
			throw new AdapterException(AccessAdapter.class.getSimpleName(), e.getErrorCode(), e.getMessage());
		}
		return null;
	}

	public void setWorkflowService(WorkflowService workflowService) {
		this.workflowService = workflowService;

	}

	/**
	 * Update the $PARTICIPANTS.
	 * 
	 * @param workitem
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public ItemCollection updateParticipants(ItemCollection workitem) {

		List<String> participants = workitem.getItemValue(WorkflowService.PARTICIPANTS);
		String user = workflowService.getUserName();
		if (!participants.contains(user)) {
			participants.add(user);
			workitem.replaceItemValue(WorkflowService.PARTICIPANTS, participants);
		}

		return workitem;
	}

	/**
	 * This method updates the $readAccess and $writeAccess attributes of a WorkItem
	 * depending to the configuration of a Activity Entity.
	 * 
	 * The method evaluates the new model flag keyupdateacl. If 'false' then acl
	 * will not be updated.
	 * 
	 * 
	 */
	@SuppressWarnings({ "rawtypes" })
	public ItemCollection updateACL(ItemCollection workitem, ItemCollection event, ItemCollection nextTask)
			throws PluginException {
		ItemCollection documentContext = workitem;
		ItemCollection documentActivity = event;

		// in case the activity is connected to a followup activity the
		// nextProcess can be null!

		// test update mode of activity and process entity - if true clear the
		// existing values.
		if (documentActivity.getItemValueBoolean("keyupdateacl") == false
				&& (nextTask == null || nextTask.getItemValueBoolean("keyupdateacl") == false)) {
			// no update!
			return documentContext;
		} else {
			// clear existing settings!
			documentContext.replaceItemValue(WorkflowService.READACCESS, new Vector());
			documentContext.replaceItemValue(WorkflowService.WRITEACCESS, new Vector());

			// activity settings will not be merged with process entity settings!
			if (documentActivity.getItemValueBoolean("keyupdateacl") == true) {
				updateACLByItemCollection(documentContext, documentActivity);
			} else {
				updateACLByItemCollection(documentContext, nextTask);
			}
		}

		return documentContext;
	}

	/**
	 * This method updates the read/write access of a workitem depending on a given
	 * model entity The model entity should provide the following attributes:
	 * 
	 * keyupdateacl,
	 * namaddreadaccess,keyaddreadfields,keyaddwritefields,namaddwriteaccess
	 * 
	 * 
	 * The method did not clear the exiting values of $writeAccess and $readAccess
	 * 
	 * @throws PluginException
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void updateACLByItemCollection(ItemCollection documentContext, ItemCollection modelEntity)
			throws PluginException {

		if (modelEntity == null || modelEntity.getItemValueBoolean("keyupdateacl") == false) {
			// no update necessary
			return;
		}

		List vectorAccess;
		vectorAccess = documentContext.getItemValue(WorkflowService.READACCESS);
		// add roles
		mergeRoles(vectorAccess, modelEntity.getItemValue("namaddreadaccess"), documentContext);
		// add Mapped Fields
		mergeFieldList(documentContext, vectorAccess, modelEntity.getItemValue("keyaddreadfields"));
		// clean Vector
		vectorAccess = uniqueList(vectorAccess);

		// update accesslist....
		documentContext.replaceItemValue(WorkflowService.READACCESS, vectorAccess);
		if ((logger.isLoggable(Level.FINE)) && (vectorAccess.size() > 0)) {
			logger.finest("......[AccessPlugin] ReadAccess:");
			for (int j = 0; j < vectorAccess.size(); j++)
				logger.finest("               '" + (String) vectorAccess.get(j) + "'");
		}

		// update WriteAccess
		vectorAccess = documentContext.getItemValue(WorkflowService.WRITEACCESS);
		// add Names
		mergeRoles(vectorAccess, modelEntity.getItemValue("namaddwriteaccess"), documentContext);
		// add Mapped Fields
		mergeFieldList(documentContext, vectorAccess, modelEntity.getItemValue("keyaddwritefields"));
		// clean Vector
		vectorAccess = uniqueList(vectorAccess);

		// update accesslist....
		documentContext.replaceItemValue(WorkflowService.WRITEACCESS, vectorAccess);
		if ((logger.isLoggable(Level.FINE)) && (vectorAccess.size() > 0)) {
			logger.finest("......[AccessPlugin] WriteAccess:");
			for (int j = 0; j < vectorAccess.size(); j++)
				logger.finest("               '" + (String) vectorAccess.get(j) + "'");
		}

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

	/**
	 * This method merges the role names from a SourceList into a valueList and
	 * removes duplicates.
	 * 
	 * The AddaptText event is fired so a client can adapt a role name.
	 * 
	 * @param valueList
	 * @param sourceList
	 * @throws PluginException
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public  void mergeRoles(List valueList, List sourceList, ItemCollection documentContext) throws PluginException {
		if ((sourceList != null) && (sourceList.size() > 0)) {
			for (Object o : sourceList) {
				if (valueList.indexOf(o) == -1) {
					if (o instanceof String) {
						// addapt textList
						List<String> adaptedRoles = workflowService.adaptTextList((String) o, documentContext);
						valueList.addAll(adaptedRoles);// .add(getWorkflowService().adaptText((String)o,
														// documentContext));
					} else {
						valueList.add(o);
					}
				}
			}
		}
	}
}
