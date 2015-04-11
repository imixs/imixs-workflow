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
import org.imixs.workflow.WorkflowContext;
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
 * @author Ralph Soika
 * @version 1.0
 * @see org.imixs.workflow.WorkflowManager
 */

public class OwnerPlugin extends AbstractPlugin {
	ItemCollection documentContext;
	ItemCollection documentActivity;
	Vector<?> itemOwnerRollback;
	WorkflowContext workflowContext;

	private static Logger logger = Logger.getLogger(AccessPlugin.class
			.getName());

	public void init(WorkflowContext actx) throws PluginException {
		workflowContext = actx;
	}

	/**
	 * changes the namworkflowreadaccess and namworkflowwriteaccess attribues
	 * depending to the activityentity
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public int run(ItemCollection adocumentContext,
			ItemCollection adocumentActivity) throws PluginException {
		List vectorAccess;

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

		// test update mode..
		if (documentActivity.getItemValueBoolean("keyupdateacl") == false) {
			// no update!
			return Plugin.PLUGIN_OK;
		}

		vectorAccess = new Vector();
		// add names
		mergeValueList(vectorAccess,
				documentActivity.getItemValue("namOwnershipNames"));
		// add Mapped Fields
		mergeFieldList(documentContext, vectorAccess,
				documentActivity.getItemValue("keyOwnershipFields"));
		// clean Vector
		vectorAccess = uniqueList(vectorAccess);

		// update accesslist....
		documentContext.replaceItemValue("namowner", vectorAccess);
		if ((workflowContext.getLogLevel() == WorkflowKernel.LOG_LEVEL_FINE)
				&& (vectorAccess.size() > 0)) {
			logger.info("[OwnerPlugin] Owners:");
			for (int j = 0; j < vectorAccess.size(); j++)
				logger.info("               '" + (String) vectorAccess.get(j)
						+ "'");
		}

		return Plugin.PLUGIN_OK;
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
		if (workflowContext.getLogLevel() == WorkflowKernel.LOG_LEVEL_FINE)
			System.out.println("[OwnerPlugin] AccessMode: '"
					+ documentActivity.getItemValueString("keyOwnershipMode")
					+ "'");

		if (vectorAccess == null)
			vectorAccess = new Vector();

		// **1** AllowAccess add names
		mergeValueList(vectorAccess,
				documentActivity.getItemValue("namOwnershipNames"));

		// **3** AllowAccess add Mapped Fields
		mergeFieldList(documentContext, vectorAccess,
				documentActivity.getItemValue("keyOwnershipFields"));

		// clean Vector
		vectorAccess = uniqueList(vectorAccess);

		// save Vector
		documentContext.replaceItemValue("namOwner", vectorAccess);
		if ((workflowContext.getLogLevel() == WorkflowKernel.LOG_LEVEL_FINE)
				&& (vectorAccess.size() > 0)) {
			System.out.println("[OwnerPlugin] Owner:");
			for (int j = 0; j < vectorAccess.size(); j++)
				System.out.println("              "
						+ (String) vectorAccess.get(j));
		}

	}
}
