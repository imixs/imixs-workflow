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
 * This plug-in implements a generic access management control (ACL) by
 * evaluating the configuration of a Activity Entity. The plug-in updates the
 * WorkItem attributes $ReadAccess and $WriteAccess depending on the provided
 * information.
 * 
 * <p>
 * These attributes defined in Activity Entity are evaluated by the plugin:
 * <ul>
 * <li>keyupdateacl (Boolean): if false no changes are necessary
 * <li>keyaddreadfields (Vector): Properties of the current WorkItem
 * <li>keyaddwritefields (Vector): Properties of the current WorkItem
 * <li>namaddreadaccess (Vector): Names & Groups to be added /replaced
 * <li>namaddwriteaccess (Vector): Names & Groups to be added/replaced
 * 
 * 
 * 
 * #Issue 90: Extend access plugin to resolve ACL settings in process entity
 * 
 * The AccessPlugin also evaluates the ACL settings in the next ProcessEntity
 * which is supported by newer versions of the imixs-bpmn modeler.
 * 
 * 
 * 
 * 
 * 
 * 
 * Fallback Mode:
 * 
 * NOTE: Models generated with the first version of the Imixs-Workflow Modeler
 * provide a different set of attributes. Therefore the plugin implements a
 * fallback method to support deprecated models. The fallback method evaluate
 * the following list of attributes defined in Activity Entity:
 * <p>
 * These attributes are:
 * <ul>
 * <li>keyaccessmode (Vector): '1'=update '0'=renew
 * <li>namaddreadaccess (Vector): Names & Groups to be added /replaced
 * <li>namaddwriteaccess (Vector): Names & Groups to be added/replaced
 * <li>keyaddreadfields (Vector): Attributes of the processd workitem to add
 * there values
 * <li>keyaddwritefields (Vector): Attributes of the processd workitem to add
 * therevalues
 * 
 * 
 * 
 * @author Ralph Soika
 * @version 3.0
 * @see org.imixs.workflow.WorkflowManager
 */

public class AccessPlugin extends AbstractPlugin {
	ItemCollection documentContext;
	ItemCollection documentActivity, documentNextProcessEntity;
	List<?> itemReadRollback, itemWriteRollback;

	private static Logger logger = Logger.getLogger(AccessPlugin.class.getName());

	/**
	 * This method updates the $readAccess and $writeAccess attributes of a
	 * WorkItem depending to the configuration of a Activity Entity.
	 * 
	 * The method evaluates the new model flag keyupdateacl. If 'false' then acl
	 * will not be updated.
	 * 
	 * 
	 */
	@SuppressWarnings({ "rawtypes" })
	public int run(ItemCollection adocumentContext, ItemCollection adocumentActivity) throws PluginException {
		documentContext = adocumentContext;
		documentActivity = adocumentActivity;

		// save Attributes for roleback
		itemReadRollback = (Vector) documentContext.getItemValue("$readAccess");
		itemWriteRollback = documentContext.getItemValue("$writeAccess");

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
			documentContext.replaceItemValue("$readAccess", new Vector());
			documentContext.replaceItemValue("$writeAccess", new Vector());

			// activity settings will not be merged with process entity settings!
			if (documentActivity.getItemValueBoolean("keyupdateacl") == true) {
				updateACLByItemCollection(documentActivity);
			} else {
				updateACLByItemCollection(documentNextProcessEntity);
			}
		}

		return Plugin.PLUGIN_OK;
	}

	/**
	 * This method updates the read/write access of a workitem depending on a
	 * given model entity The model entity should provide the following
	 * attributes:
	 * 
	 * keyupdateacl,
	 * namaddreadaccess,keyaddreadfields,keyaddwritefields,namaddwriteaccess
	 * 
	 * 
	 * The method did not clear the exiting values of $writeAccess and
	 * $readAccess
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void updateACLByItemCollection(ItemCollection modelEntity) {

		if (modelEntity == null || modelEntity.getItemValueBoolean("keyupdateacl") == false) {
			// no update necessary
			return;
		}

		List vectorAccess;
		vectorAccess = documentContext.getItemValue("$readAccess");
		// add names
		mergeValueList(vectorAccess, modelEntity.getItemValue("namaddreadaccess"));
		// add Mapped Fields
		mergeFieldList(documentContext, vectorAccess, modelEntity.getItemValue("keyaddreadfields"));
		// clean Vector
		vectorAccess = uniqueList(vectorAccess);

		// update accesslist....
		documentContext.replaceItemValue("$readAccess", vectorAccess);
		if ((ctx.getLogLevel() == WorkflowKernel.LOG_LEVEL_FINE) && (vectorAccess.size() > 0)) {
			logger.info("[AccessPlugin] ReadAccess:");
			for (int j = 0; j < vectorAccess.size(); j++)
				logger.info("               '" + (String) vectorAccess.get(j) + "'");
		}

		// update WriteAccess
		vectorAccess = documentContext.getItemValue("$writeAccess");
		// add Names
		mergeValueList(vectorAccess, modelEntity.getItemValue("namaddwriteaccess"));
		// add Mapped Fields
		mergeFieldList(documentContext, vectorAccess, modelEntity.getItemValue("keyaddwritefields"));
		// clean Vector
		vectorAccess = uniqueList(vectorAccess);

		// update accesslist....
		documentContext.replaceItemValue("$writeAccess", vectorAccess);
		if ((ctx.getLogLevel() == WorkflowKernel.LOG_LEVEL_FINE) && (vectorAccess.size() > 0)) {
			logger.info("[AccessPlugin] WriteAccess:");
			for (int j = 0; j < vectorAccess.size(); j++)
				logger.info("               '" + (String) vectorAccess.get(j) + "'");
		}

	}

	public void close(int status) {
		// restore changes?
		if (status == Plugin.PLUGIN_ERROR) {
			documentContext.replaceItemValue("$writeAccess", itemWriteRollback);
			documentContext.replaceItemValue("$readAccess", itemReadRollback);
		}
	}

	/**
	 * Returns true if a old workflow model need to be evaluated
	 * 
	 * @return
	 */
	@Deprecated
	private boolean isFallBackMode() {
		// if the new keyupdateacl exists no fallback mode!
		if (documentActivity.hasItem("keyupdateacl")) {
			return false;
		}

		// fallback mode if no keyupdateacl exists and keyaccessmode exits
		if (documentActivity.hasItem("keyaccessmode")) {
			return true;
		}

		return false;

	}

	@Deprecated
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void processFallBack() {
		List itemRead;
		List itemWrite;
		List vectorAccess;

		itemRead = (Vector) documentContext.getItemValue("$readAccess");

		// test mode (1=update)
		if ("1".equals(documentActivity.getItemValueString("keyaccessmode")))
			vectorAccess = itemRead;
		else
			vectorAccess = new Vector();
		if (ctx.getLogLevel() == WorkflowKernel.LOG_LEVEL_FINE)
			logger.info("[AccessPlugin] AccessMode: '" + documentActivity.getItemValueString("keyaccessmode") + "'");

		if (vectorAccess == null)
			vectorAccess = new Vector();

		// **1** AllowAccess add names
		mergeValueList(vectorAccess, documentActivity.getItemValue("namaddreadaccess"));
		// **3** AllowAccess add Mapped Fields
		mergeFieldList(documentContext, vectorAccess, documentActivity.getItemValue("keyaddreadfields"));

		// clean Vector
		vectorAccess = uniqueList(vectorAccess);

		// save Vector
		documentContext.replaceItemValue("$readAccess", vectorAccess);
		if ((ctx.getLogLevel() == WorkflowKernel.LOG_LEVEL_FINE) && (vectorAccess.size() > 0)) {
			logger.info("[AccessPlugin] ReadAccess:");
			for (int j = 0; j < vectorAccess.size(); j++)
				logger.info("               '" + (String) vectorAccess.get(j) + "'");
		}

		/**** now process write access ***/

		// check for $writeAccess
		itemWrite = documentContext.getItemValue("$writeAccess");

		// add new WriteAccess

		if ("1".equals(documentActivity.getItemValueString("keyaccessmode")))
			vectorAccess = itemWrite;
		else
			vectorAccess = new Vector();

		if (vectorAccess == null)
			vectorAccess = new Vector();

		// **1** AllowAccess add Names
		mergeValueList(vectorAccess, documentActivity.getItemValue("namaddwriteaccess"));
		// **3** AllowAccess add Mapped Fields
		mergeFieldList(documentContext, vectorAccess, documentActivity.getItemValue("keyaddwritefields"));

		// clean Vector
		vectorAccess = uniqueList(vectorAccess);

		// save Vector
		documentContext.replaceItemValue("$writeAccess", vectorAccess);
		if ((ctx.getLogLevel() == WorkflowKernel.LOG_LEVEL_FINE) && (vectorAccess.size() > 0)) {
			logger.info("[AccessPlugin] WriteAccess:");
			for (int j = 0; j < vectorAccess.size(); j++)
				logger.info("               '" + (String) vectorAccess.get(j) + "'");
		}

	}

}
