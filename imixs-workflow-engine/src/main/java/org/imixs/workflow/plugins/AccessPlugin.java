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
 * This Plugin module implements a generic Access management controled throught
 * the configuration in a Workflowactivity. The Plugin updates the attriubtes
 * $ReadAccess and $WriteAccess depending on the configuration in a
 * ActivityEntiy.
 * <p>
 * These attributes are:
 * <ul>
 * <li>keyaccessmode (Vector): '1'=update '0'=renew
 * <li>namaddreadaccess (Vector): Names & Groups to be added /replaced
 * <li>namaddwriteaccess (Vector): Names & Groups to be added/replaced
 * <li>keyaddreadroles (Vector): Roles to be added/replaced
 * <li>keyaddwriteroles (Vector): Roles to added/replaced
 * <li>keyaddreadfields (Vector): Attributes of the processd workitem to add
 * there values
 * <li>keyaddwritefields (Vector): Attributes of the processd workitem to add
 * therevalues
 * 
 * @author Ralph Soika
 * @version 2.0
 * @see org.imixs.workflow.WorkflowManager
 */

public class AccessPlugin extends AbstractPlugin {
	ItemCollection documentContext;
	ItemCollection documentActivity;
	List itemReadRollback, itemWriteRollback;
	WorkflowContext workflowContext;

	private static Logger logger = Logger.getLogger("org.imixs.workflow");

	public void init(WorkflowContext actx) throws PluginException {
		workflowContext = actx;
	}

	/**
	 * changes the $readAccess and $writeAccess attribues depending to the
	 * activityentity
	 */
	public int run(ItemCollection adocumentContext,
			ItemCollection adocumentActivity) throws PluginException {
		List itemRead;
		List itemWrite;
		List vectorAccess;

		documentContext = adocumentContext;
		documentActivity = adocumentActivity;

		// Validate Activity and Workitem
		validate();

		itemRead = (Vector) documentContext.getItemValue("$readAccess");

		// save Attribute for roleback
		itemReadRollback = (Vector) documentContext.getItemValue("$readAccess");

		// neuen ReadAccess hinzuf�gen
		if ("1".equals(documentActivity.getItemValueString("keyaccessmode")))
			vectorAccess = itemRead;
		else
			vectorAccess = new Vector();
		if (workflowContext.getLogLevel() == WorkflowKernel.LOG_LEVEL_FINE)
			logger.info("[AccessPlugin] AccessMode: '"
					+ documentActivity.getItemValueString("keyaccessmode")
					+ "'");

		if (vectorAccess == null)
			vectorAccess = new Vector();

		// **1** AllowAccess add names
		mergeVectors(vectorAccess,
				documentActivity.getItemValue("namaddreadaccess"));
		// **2** AllowAccess add roles
		mergeVectors(vectorAccess,
				documentActivity.getItemValue("keyaddreadroles"));
		// **3** AllowAccess add Mapped Fields
		mergeMappedFieldValues(documentContext, vectorAccess,
				documentActivity.getItemValue("keyaddreadfields"));

		// clean Vector
		vectorAccess = uniqueList(vectorAccess);

		// save Vector
		documentContext.replaceItemValue("$readAccess", vectorAccess);
		if ((workflowContext.getLogLevel() == WorkflowKernel.LOG_LEVEL_FINE)
				&& (vectorAccess.size() > 0)) {
			logger.info("[AccessPlugin] ReadAccess:");
			for (int j = 0; j < vectorAccess.size(); j++)
				logger.info("               '" + (String) vectorAccess.get(j)+"'");
		}

		/**** now process write access ***/

		// check for $writeAccess
		itemWrite = documentContext.getItemValue("$writeAccess");

		// save Attribute for roleback
		itemWriteRollback = documentContext.getItemValue("$writeAccess");

		// add new WriteAccess

		if ("1".equals(documentActivity.getItemValueString("keyaccessmode")))
			vectorAccess = itemWrite;
		else
			vectorAccess = new Vector();

		if (vectorAccess == null)
			vectorAccess = new Vector();

		// **1** AllowAccess add Names
		mergeVectors(vectorAccess,
				documentActivity.getItemValue("namaddwriteaccess"));
		// **2** AllowAccess add Rolles
		mergeVectors(vectorAccess,
				documentActivity.getItemValue("keyaddwriteroles"));
		// **3** AllowAccess add Mapped Fields �gen
		mergeMappedFieldValues(documentContext, vectorAccess,
				documentActivity.getItemValue("keyaddwritefields"));

		// clean Vector
		vectorAccess = uniqueList(vectorAccess);

		// save Vector
		documentContext.replaceItemValue("$writeAccess", vectorAccess);
		if ((workflowContext.getLogLevel() == WorkflowKernel.LOG_LEVEL_FINE)
				&& (vectorAccess.size() > 0)) {
			logger.info("[AccessPlugin] WriteAccess:");
			for (int j = 0; j < vectorAccess.size(); j++)
				logger.info("               '" + (String) vectorAccess.get(j)+"'");
		}

		return Plugin.PLUGIN_OK;
	}

	public void close(int status) {
		// restore changes?
		if (status == Plugin.PLUGIN_ERROR) {
			documentContext.replaceItemValue("$writeAccess", itemWriteRollback);
			documentContext.replaceItemValue("$readAccess", itemReadRollback);
		}
	}

	/**
	 * Ensures that the workitem and activityentity has a valid set of
	 * attributes to be process by this plugin.
	 */
	private void validate() {

		// validate activity
		if (!documentActivity.hasItem("keyaccessmode"))
			documentActivity.replaceItemValue("keyaccessmode", "");

		if (!documentActivity.hasItem("namaddreadaccess"))
			documentActivity.replaceItemValue("namaddreadaccess", "");

		if (!documentActivity.hasItem("keyaddreadroles"))
			documentActivity.replaceItemValue("keyaddreadroles", "");

		if (!documentActivity.hasItem("keyaddreadfields"))
			documentActivity.replaceItemValue("keyaddreadfields", "");

		if (!documentActivity.hasItem("namaddwriteaccess"))
			documentActivity.replaceItemValue("namaddwriteaccess", "");

		if (!documentActivity.hasItem("keyaddwriteroles"))
			documentActivity.replaceItemValue("keyaddwriteroles", "");

		if (!documentActivity.hasItem("keyaddreadfields"))
			documentActivity.replaceItemValue("keyaddreadfields", "");

		// validate document
		if (!documentContext.hasItem("$readAccess"))
			documentContext.replaceItemValue("$readAccess", "");

		if (!documentContext.hasItem("$writeAccess"))
			documentContext.replaceItemValue("$writeAccess", "");

	}

}
