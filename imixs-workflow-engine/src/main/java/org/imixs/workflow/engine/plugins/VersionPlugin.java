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

import java.util.Collection;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.imixs.workflow.exceptions.QueryException;

/**
 * This plugin handles the creation and management of versions from an existing
 * workitem. inside the Imix JEE Workflow. The creation or modificatin of a
 * version is defined by the workflowmodel. The plugin can generate new versions
 * (e.g. creating a version of a master version) and also converting a existing
 * version into a master version.
 * <p>
 * The Plugin depends on the org.imixs.workflow.jee.ejb.WorkflowManager. So the
 * Plugin can not be used in other implementations.
 * <p>
 * The creation and management of a new version is defined by the workflow model
 * (See Imixs Modeler) There are currently two modes supported (provided by the
 * activity property 'keyVersion')
 * <p>
 * mode=1 indicates that the plugin should create a new version of the current
 * workitem. The two workitems are identically except the attributes $uniqueid
 * and $workitemidRef. The attribute workitemidRef points to the $uniqueid form
 * the source workitem. So the availability of this property indicates that the
 * workitem is a version of source workitem with this $uniqueid. The source
 * workitem has typically no $workitemidRef attribute. The Source Workitem is
 * also named Master Version. After the new version is created the plugin
 * processes the version with the activity provided by the model
 * (numVersionActivityID) if provided by the model.
 * <p>
 * 2=indicates that the plugin should convert a existing version into a Master
 * Version. This means that the $workitemIDRef will be nulled. An existing
 * Master Version will be processed by the activity provided by the model
 * (numVersionActivityID). Also the $workitemidRef will be set to the current
 * $workitemID.
 * <p>
 * If an error occured during the workflow process this plugin will throw a new
 * ejbexception in the close() method to cancel the current transaction. So no
 * changes will be saved by the ejb container
 * 
 * @see org.imixs.workflow.jee.ejb.WorkflowManager
 * @author Ralph Soika
 * @version 1.0
 */

public class VersionPlugin extends AbstractPlugin {

	public static final String INVALID_CONTEXT = "INVALID_CONTEXT";
	public static final String INVALID_WORKITEM = "INVALID_WORKITEM";

	private String versionMode = "";
	private int versionActivityID = -1;
	private ItemCollection version = null;
	private ItemCollection documentContext = null;

	private static final String PROCESSING_VERSION_ATTRIBUTE = "$processingversion";
	private static Logger logger = Logger.getLogger(VersionPlugin.class.getName());

	public ItemCollection getVersion() {
		return version;
	}

	public void setVersion(ItemCollection version) {
		this.version = version;
	}

	/**
	 * creates a new version or converts a version into the MasterVersion
	 * depending to the activity attribute "keyVersion" provided by the workflow
	 * model.
	 * 
	 * If a new version is created and be processed the plugin adds the
	 * attribute '_versionprocessing' = true to indicate this situation for
	 * other plugins. Other plugins can determine the $processingversion mode by
	 * calling the method isProcssingVersion(). The attribute will be removed
	 * after a version was processed.
	 * 
	 * @throws PluginException
	 */
	public ItemCollection run(ItemCollection adocumentContext, ItemCollection adocumentActivity)
			throws PluginException {

		documentContext = adocumentContext;

		// determine mode to manage version
		versionMode = adocumentActivity.getItemValueString("keyVersion");
		versionActivityID = adocumentActivity.getItemValueInteger("numVersionActivityID");
		try {

			// handle different version modes
			// 1 = create a new Version from current workitem
			if ("1".equals(versionMode)) {

				// copy workitem

				version = createVersion(documentContext);

				logger.fine("[VersionPlugin] new version created");

				// check if workitem should be processed
				if (versionActivityID > 0) {
					version.replaceItemValue("$ActivityID", versionActivityID);
					version.replaceItemValue(PROCESSING_VERSION_ATTRIBUTE, true);
					version = getWorkflowService().processWorkItem(version);

				} else {
					// no processing - simply save workitem
					version = getWorkflowService().getDocumentService().save(version);
				}
				return documentContext;

			}

			// convert to Master Version
			if ("2".equals(adocumentActivity.getItemValueString("keyVersion"))) {

				/*
				 * this code iterates over all existing workitems with the same
				 * $workitemid and fixes lost parent workitemRefIDs.
				 */
				String sworkitemID = documentContext.getItemValueString("$WorkItemID");

				String searchTerm = "($workitemid:\"" + sworkitemID + "\")";

				Collection<ItemCollection> col = getWorkflowService().getDocumentService().find(searchTerm, 0, -1);
				// now search master version...
				for (ItemCollection aVersion : col) {
					String sWorkitemRef = aVersion.getItemValueString("$workitemIDRef");
					if ("".equals(sWorkitemRef)) {
						// Master version found!
						// convert now this workitem into a Version from the
						// current workitem
						String id = documentContext.getItemValueString("$uniqueID");
						aVersion.replaceItemValue("$WorkItemIDRef", id);
						// process version?
						// check if worktiem should be processed
						if (versionActivityID > 0) {
							aVersion.replaceItemValue("$ActivityID", versionActivityID);
							aVersion = getWorkflowService().processWorkItem(aVersion);
						} else {
							// no processing - simply save workitem
							aVersion = getWorkflowService().getDocumentService().save(aVersion);
						}
						version = aVersion;
					}
				}
				// now remove workitemIDRef from current version
				documentContext.removeItem("$WorkItemIDRef");
			}

		} catch (AccessDeniedException e) {
			throw new PluginException(e.getErrorContext(), e.getErrorCode(), e.getMessage(), e);
		} catch (ProcessingErrorException e) {
			throw new PluginException(e.getErrorContext(), e.getErrorCode(), e.getMessage(), e);
		} catch (QueryException e) {
			throw new PluginException(e.getErrorContext(), e.getErrorCode(), e.getMessage(), e);
		} catch (ModelException e) {
			throw new PluginException(e.getErrorContext(), e.getErrorCode(), e.getMessage(), e);
		}

		// removes the attribute $processingversion which is set to 'true'
		// during the processing of a new version
		documentContext.removeItem(PROCESSING_VERSION_ATTRIBUTE);
		return documentContext;
	}



	/**
	 * returns true in case a new version is created based on the
	 * currentWorkitem
	 * 
	 * @param documentContext
	 * @param documentActivity
	 * @return
	 */
	public static boolean isProcssingVersion(ItemCollection adocumentContext) {
		return adocumentContext.getItemValueBoolean(PROCESSING_VERSION_ATTRIBUTE);
	}

	/**
	 * This method creates a new instance of a exiting workitem. The method did
	 * not save the workitem!. The method can be subclassed to modify the new
	 * created version.
	 * 
	 * The new property $WorkitemIDRef will be added which points to the
	 * $uniqueID of the sourceWorkitem.
	 * 
	 * @param sourceItemCollection
	 *            the ItemCollection which should be versioned
	 * @return new version of the source ItemCollection
	 * 
	 * @throws PluginException
	 * @throws Exception
	 */
	public ItemCollection createVersion(ItemCollection sourceItemCollection) throws PluginException {
		ItemCollection itemColNewVersion = new ItemCollection();

		itemColNewVersion.replaceAllItems(sourceItemCollection.getAllItems());

		String id = sourceItemCollection.getItemValueString("$uniqueid");
		if ("".equals(id))
			throw new PluginException(VersionPlugin.class.getSimpleName(), INVALID_WORKITEM,
					"Error - unable to create a version from a new workitem!");
		// remove $Uniqueid to force the generation of a new Entity Instance.
		itemColNewVersion.getAllItems().remove("$uniqueid");

		// update $WorkItemIDRef to current worktiemID
		itemColNewVersion.replaceItemValue("$WorkItemIDRef", id);

		return itemColNewVersion;

	}

}