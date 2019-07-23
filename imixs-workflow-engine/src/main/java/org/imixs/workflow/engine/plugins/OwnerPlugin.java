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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;

/**
 * This plugin implements a ownership control by evaluating the configuration of
 * an BPMN Event element. The Plugin updates the WorkItem attribute '$owner'
 * depending on the provided information.
 * 
 * <p>
 * These attributes defined in Activity Entity are evaluated by the plugin:
 * <ul>
 * <li>keyupdateacl (Boolean): if false no changes are necessary
 * <li>keyOwnershipFields (Vector): Properties of the current WorkItem
 * <li>namOwnershipNames (Vector): Names & Groups to be added /replaced
 * 
 * 
 * 
 * NOTE: Models generated with the first version of the Imixs-Workflow Modeler
 * provide a different set of attributes. Therefore the plugin implements a
 * fallback method to support deprecated models. The fallback method evaluate
 * the following list of attributes defined in Activity Entity:
 * <p>
 * 
 * <ul>
 * <li>keyOwnershipMode (Vector): '1'=modify access '0'=renew access
 * <li>keyOwnershipFields (Vector): Properties of the current WorkItem
 * <li>namOwnershipNames (Vector): Names & Groups to be added /replaced
 * 
 * 
 * 
 * 
 * #Issue 133: Extend access plug-in to resolve owner settings in process entity
 * 
 * The AccessPlugin also evaluates the ACL settings in the next ProcessEntity
 * which is supported by newer versions of the imixs-bpmn modeler.
 * 
 * 
 * 
 * @author Ralph Soika
 * @version 1.0
 * @see org.imixs.workflow.WorkflowManager
 */

public class OwnerPlugin extends AbstractPlugin {
	
	public final static String OWNER="$owner";
	
	private ItemCollection documentContext;
	private ItemCollection documentActivity;
	private ItemCollection documentNextProcessEntity;

	private static Logger logger = Logger.getLogger(OwnerPlugin.class.getName());

	/**
	 * changes the '$owner' item depending to the activityentity or
	 * processEntity
	 * 
	 */
	public ItemCollection run(ItemCollection adocumentContext, ItemCollection adocumentActivity)
			throws PluginException {

		documentContext = adocumentContext;
		documentActivity = adocumentActivity;

		// get next process entity
		try {
			documentNextProcessEntity = this.getWorkflowService().evalNextTask(adocumentContext, adocumentActivity);
		} catch (ModelException e) {
			throw new PluginException(OwnerPlugin.class.getSimpleName(), e.getErrorCode(), e.getMessage());
		}

		// in case the activity is connected to a followup activity the
		// nextProcess can be null!

		// test update mode of activity and process entity - if true clear the
		// existing values.
		if (documentActivity.getItemValueBoolean("keyupdateacl") == false && (documentNextProcessEntity == null
				|| documentNextProcessEntity.getItemValueBoolean("keyupdateacl") == false)) {
			// no update!
			return documentContext;
		} else {
			// activity settings will not be merged with process entity
			// settings!
			if (documentActivity.getItemValueBoolean("keyupdateacl") == true) {
				updateOwnerByItemCollection(documentActivity);
			} else {
				updateOwnerByItemCollection(documentNextProcessEntity);
			}
		}

		return documentContext;
	}

	/**
	 * This method updates the owner of a workitem depending on a given model entity
	 * The model entity should provide the following attributes:
	 * 
	 * keyupdateacl, namOwnershipNames,keyOwnershipFields
	 * 
	 * 
	 * The method did not clear the exiting values of namowner
	 * @throws PluginException 
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void updateOwnerByItemCollection(ItemCollection modelEntity) throws PluginException {

		if (modelEntity == null || modelEntity.getItemValueBoolean("keyupdateacl") == false) {
			// no update necessary
			return;
		}

		List newOwnerList;
		newOwnerList = new ArrayList<String>();

		// add names
		mergeRoles(newOwnerList, modelEntity.getItemValue("namOwnershipNames"),documentContext);
		// add Mapped Fields
		mergeFieldList(documentContext, newOwnerList, modelEntity.getItemValue("keyOwnershipFields"));
		// clean Vector
		newOwnerList = uniqueList(newOwnerList);

		// update ownerlist....
		documentContext.replaceItemValue(OWNER, newOwnerList);
		if ((logger.isLoggable(Level.FINE)) && (newOwnerList.size() > 0)) {
			logger.finest("......Owners:");
			for (int j = 0; j < newOwnerList.size(); j++)
				logger.finest("               '" + (String) newOwnerList.get(j) + "'");
		}
		
		
		// we also need to support the deprecated iten name "namOwner" which was replaced since version 5.0.2 by "owner"
		documentContext.replaceItemValue("namOwner", newOwnerList);

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
						List<String> adaptedRoles = this.getWorkflowService().adaptTextList((String) o, documentContext);
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
