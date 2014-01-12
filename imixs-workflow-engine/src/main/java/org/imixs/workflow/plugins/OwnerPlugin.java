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

import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.PluginException;

/**
 * This Pluginmodul implements a Ownership Control for Workflowactivitys. The
 * Plugin manages the modifications of the field namOwner of a Workitem inside a
 * activity by setting this Attribute. A Workflowmanager can use this attributes
 * to reflect there settings to the ownership inside a certain databasesystem.
 * The Plugin checks a set of activity attributes to manage the new settings of
 * the ownership defined inside the activity entity These attributes are:
 * 
 * o keyOwnershipMode (Vector): '1'=modify access '0'=renew access
 * 
 * o namOwnershipNames (Vector): Names & Groups to add to the namOwner (Vector):
 * 
 * o keyaddwriteroles (Vector): Roles to add to the namOwner attribute
 * 
 * @author Ralph Soika
 * @version 1.0
 * @see org.imixs.workflow.WorkflowManager
 */

public class OwnerPlugin extends AbstractPlugin {
	ItemCollection documentContext;
	ItemCollection documentActivity;
	Vector itemOwnerRollback;
	WorkflowContext workflowContext;

	public void init(WorkflowContext actx) throws PluginException {
		workflowContext = actx;
	}

	/**
	 * changes the namworkflowreadaccess and namworkflowwriteaccess attribues
	 * depending to the activityentity
	 */
	public int run(ItemCollection adocumentContext,
			ItemCollection adocumentActivity) throws PluginException {
		List itemOwner;
		List vectorAccess;

		documentContext = adocumentContext;
		documentActivity = adocumentActivity;

		// Validate Activity and Workitem
		validate();

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
		mergeVectors(vectorAccess,
				documentActivity.getItemValue("namOwnershipNames"));

		// **3** AllowAccess add Mapped Fields
		mergeMappedFieldValues(documentContext, vectorAccess,
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

		return Plugin.PLUGIN_OK;
	}

	public void close(int status) {
		// restore changes?
		if (status == Plugin.PLUGIN_ERROR) {
			documentContext.replaceItemValue("namOwner", itemOwnerRollback);
		}
	}

	/**
	 * Ensures that the workitem and activityentity has a valid set of
	 * attributes to be process by this plugin.
	 */
	private void validate() {

		// validate activity
		if (!documentActivity.hasItem("keyOwnershipMode"))
			documentActivity.replaceItemValue("keyOwnershipMode", "");

		if (!documentActivity.hasItem("namOwnershipNames"))
			documentActivity.replaceItemValue("namOwnershipNames", "");

		if (!documentActivity.hasItem("keyOwnershipFields"))
			documentActivity.replaceItemValue("keyOwnershipFields", "");

		// validate document
		if (!documentContext.hasItem("namOwner"))
			documentContext.replaceItemValue("namOwner", "");

	}

}
