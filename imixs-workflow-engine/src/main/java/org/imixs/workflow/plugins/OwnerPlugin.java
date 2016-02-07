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

package org.imixs.workflow.plugins;

import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.PluginException;

/**
 * This plugin implements a ownership control by evaluating the configuration of
 * a Activity Entity. The Plugin updates the WorkItem attribute namOwner
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
	ItemCollection documentContext;
	ItemCollection documentActivity, documentNextProcessEntity;
	Vector<?> itemOwnerRollback;

	private static Logger logger = Logger.getLogger(AccessPlugin.class.getName());

	/**
	 * changes the namworkflowreadaccess and namworkflowwriteaccess attribues
	 * depending to the activityentity
	 */
	@SuppressWarnings({ "rawtypes" })
	public int run(ItemCollection adocumentContext, ItemCollection adocumentActivity) throws PluginException {

		documentContext = adocumentContext;
		documentActivity = adocumentActivity;

		// save Attributes for roleback
		itemOwnerRollback = (Vector) documentContext.getItemValue("namowner");

		// test if fallback mode?
		if (isFallBackMode()) {
			// run the deprecated model evaluation...
			processFallBack();
			return Plugin.PLUGIN_OK;
		}

		// get next process entity
		int iNextProcessID = adocumentActivity.getItemValueInteger("numNextProcessID");
		String aModelVersion = adocumentActivity.getItemValueString("$modelVersion");
		documentNextProcessEntity = ctx.getModel().getProcessEntity(iNextProcessID, aModelVersion);
		// in case the activity is connected to a followup activity the
		// nextProcess can be null!

		// test update mode of activity and process entity - if true clear the
		// existing values.
		if (documentActivity.getItemValueBoolean("keyupdateacl") == false && (documentNextProcessEntity == null
				|| documentNextProcessEntity.getItemValueBoolean("keyupdateacl") == false)) {
			// no update!
			return Plugin.PLUGIN_OK;
		} else {
			// clear existing settings!
			documentContext.replaceItemValue("namOwner", new Vector());
			// activity settings will not be merged with process entity
			// settings!
			if (documentActivity.getItemValueBoolean("keyupdateacl") == true) {
				updateOwnerByItemCollection(documentActivity);
			} else {
				updateOwnerByItemCollection(documentNextProcessEntity);
			}
		}

		return Plugin.PLUGIN_OK;
	}

	/**
	 * This method updates the owner of a workitem depending on a given model
	 * entity The model entity should provide the following attributes:
	 * 
	 * keyupdateacl, namOwnershipNames,keyOwnershipFields
	 * 
	 * 
	 * The method did not clear the exiting values of namowner
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void updateOwnerByItemCollection(ItemCollection modelEntity) {

		if (modelEntity == null || modelEntity.getItemValueBoolean("keyupdateacl") == false) {
			// no update necessary
			return;
		}

		List vectorAccess;
		vectorAccess = documentContext.getItemValue("namowner");
		// add names
		mergeValueList(vectorAccess, modelEntity.getItemValue("namOwnershipNames"));
		// add Mapped Fields
		mergeFieldList(documentContext, vectorAccess, modelEntity.getItemValue("keyOwnershipFields"));
		// clean Vector
		vectorAccess = uniqueList(vectorAccess);

		// update ownerlist....
		documentContext.replaceItemValue("namowner", vectorAccess);
		if ((ctx.getLogLevel() == WorkflowKernel.LOG_LEVEL_FINE) && (vectorAccess.size() > 0)) {
			logger.info("[OwnerPlugin] Owners:");
			for (int j = 0; j < vectorAccess.size(); j++)
				logger.info("               '" + (String) vectorAccess.get(j) + "'");
		}

	}

	public void close(int status) {
		// restore changes?
		if (status == Plugin.PLUGIN_ERROR) {
			documentContext.replaceItemValue("namOwner", itemOwnerRollback);
		}
	}

	/**
	 * Returns true if a old workflow model need to be evaluated
	 * 
	 * @return
	 */
	private boolean isFallBackMode() {
		// if the new keyupdateacl exists no fallback mode!
		if (documentActivity.hasItem("keyupdateacl")) {
			return false;
		}

		// fallback mode if no keyupdateacl exists and keyaccessmode exits
		if (documentActivity.hasItem("keyOwnershipMode")) {
			return true;
		}

		return false;

	}

	@Deprecated
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void processFallBack() {
		List itemOwner;
		List vectorAccess;

		itemOwner = (Vector) documentContext.getItemValue("namowner");

		// save Attribute for roleback
		itemOwnerRollback = (Vector) documentContext.getItemValue("namOwners");

		// add new ownership
		if ("1".equals(documentActivity.getItemValueString("keyOwnershipMode")))
			vectorAccess = itemOwner;
		else
			vectorAccess = new Vector();
		if (ctx.getLogLevel() == WorkflowKernel.LOG_LEVEL_FINE)
			System.out.println(
					"[OwnerPlugin] AccessMode: '" + documentActivity.getItemValueString("keyOwnershipMode") + "'");

		if (vectorAccess == null)
			vectorAccess = new Vector();

		// **1** AllowAccess add names
		mergeValueList(vectorAccess, documentActivity.getItemValue("namOwnershipNames"));

		// **3** AllowAccess add Mapped Fields
		mergeFieldList(documentContext, vectorAccess, documentActivity.getItemValue("keyOwnershipFields"));

		// clean Vector
		vectorAccess = uniqueList(vectorAccess);

		// save Vector
		documentContext.replaceItemValue("namOwner", vectorAccess);
		if ((ctx.getLogLevel() == WorkflowKernel.LOG_LEVEL_FINE) && (vectorAccess.size() > 0)) {
			System.out.println("[OwnerPlugin] Owner:");
			for (int j = 0; j < vectorAccess.size(); j++)
				System.out.println("              " + (String) vectorAccess.get(j));
		}

	}
}
