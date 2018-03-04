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

import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;

/**
 * This plug-in implements a generic access management control (ACL) by
 * evaluating the configuration of a BPMN Event or BPMN Task. The plug-in
 * updates the WorkItem attributes $ReadAccess and $WriteAccess depending on the
 * provided information.
 * 
 * <p>
 * The following attributes defined in the model element are evaluated by the
 * plugin:
 * <ul>
 * <li>keyupdateacl (Boolean): if false the ACL will not be changed
 * <li>keyaddreadfields (Vector): a list of items of the current WorkItem to be
 * applied to the read access
 * <li>keyaddwritefields (Vector): a list of items of the current WorkItem to be
 * applied to the write access
 * <li>namaddreadaccess (Vector): Names & Groups to be applied to the read
 * access
 * <li>namaddwriteaccess (Vector): Names & Groups to be applied to the write
 * access
 * </ul>
 * 
 * The AccessPlugin evaluates the ACL settings of the current Event element as
 * also the ACL settings of the next Task element. If the current Event Element
 * provides a ACL setting, the next Task element will be ignored.
 * 
 * 
 * <p>
 * Fallback Mode:
 * <p>
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
 * </ul>
 * 
 * 
 * @author Ralph Soika
 * @version 3.0
 * @see org.imixs.workflow.WorkflowManager
 */

public class AccessPlugin extends AbstractPlugin {
	ItemCollection documentContext;
	ItemCollection documentActivity, documentNextProcessEntity;

	private static Logger logger = Logger.getLogger(AccessPlugin.class.getName());

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
	public ItemCollection run(ItemCollection adocumentContext, ItemCollection adocumentActivity)
			throws PluginException {
		documentContext = adocumentContext;
		documentActivity = adocumentActivity;

		// get next process entity
		try {
			documentNextProcessEntity = this.getWorkflowService().evalNextTask(adocumentContext, adocumentActivity);
		} catch (ModelException e) {
			throw new PluginException(AccessPlugin.class.getSimpleName(), e.getErrorCode(), e.getMessage());
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
	 * @throws PluginException 
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void updateACLByItemCollection(ItemCollection modelEntity) throws PluginException {

		if (modelEntity == null || modelEntity.getItemValueBoolean("keyupdateacl") == false) {
			// no update necessary
			return;
		}

		List vectorAccess;
		vectorAccess = documentContext.getItemValue("$readAccess");
		// add roles
		mergeRoles(vectorAccess, modelEntity.getItemValue("namaddreadaccess"),documentContext);
		// add Mapped Fields
		mergeFieldList(documentContext, vectorAccess, modelEntity.getItemValue("keyaddreadfields"));
		// clean Vector
		vectorAccess = uniqueList(vectorAccess);

		// update accesslist....
		documentContext.replaceItemValue("$readAccess", vectorAccess);
		if ((logger.isLoggable(Level.FINE)) && (vectorAccess.size() > 0)) {
			logger.finest("......[AccessPlugin] ReadAccess:");
			for (int j = 0; j < vectorAccess.size(); j++)
				logger.finest("               '" + (String) vectorAccess.get(j) + "'");
		}

		// update WriteAccess
		vectorAccess = documentContext.getItemValue("$writeAccess");
		// add Names
		mergeRoles(vectorAccess, modelEntity.getItemValue("namaddwriteaccess"),documentContext);
		// add Mapped Fields
		mergeFieldList(documentContext, vectorAccess, modelEntity.getItemValue("keyaddwritefields"));
		// clean Vector
		vectorAccess = uniqueList(vectorAccess);

		// update accesslist....
		documentContext.replaceItemValue("$writeAccess", vectorAccess);
		if ((logger.isLoggable(Level.FINE)) && (vectorAccess.size() > 0)) {
			logger.finest("......[AccessPlugin] WriteAccess:");
			for (int j = 0; j < vectorAccess.size(); j++)
				logger.finest("               '" + (String) vectorAccess.get(j) + "'");
		}

	}

}
