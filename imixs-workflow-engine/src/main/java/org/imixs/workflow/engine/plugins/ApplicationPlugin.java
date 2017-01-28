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

import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;

/**
 * This Plugin updates application specific settings.
 * 
 * <ul>
 * <li>txtWorkflowEditorID - optional EditorID to be used by an application
 * <li>txtWorkflowImageURL - visual image can be displayed by an application
 * <li>txtWorkflowAbstract - Abstract text
 * <li>txtWorkflowSummary - Summary
 * 
 * 
 * These settings can be configured by the imixs modeler on the Application
 * Property Tab on a ProcessEntity.
 * 
 * The Plugin determines the new settings by fetching the next ProcessEntity.
 * The Next ProcessEntity is defined by the ActivityEntity attribute
 * 'numNextProcessID'
 * 
 * 
 * Version 1.1
 * 
 * The Plugin will test if the provided Model supports ExtendedModels. If so the
 * Plugin will fetch the next ProcessEntity by the current used modelVersion of
 * the workitem.
 * 
 * Version 1.2 The plugin submits the new settings directly in the run() method,
 * so other plugins can access the new properties for further operations
 * http://java.net/jira/browse/IMIXS_WORKFLOW-81
 * 
 * Version 1.3: type, workflowgroup and workfowstatus are handled by the WorkflowKernel
 * 
 * @author Ralph Soika
 * @version 1.3
 * @see org.imixs.workflow.WorkflowManager
 * 
 */
public class ApplicationPlugin extends AbstractPlugin {

	public static final String PROCESS_UNDEFINED = "PROCESS_UNDEFINED";

	ItemCollection documentContext;

	private String sEditorID;
	private String sImageURL;
	private String sAbstract;
	private String sSummary;
	private static Logger logger = Logger.getLogger(ApplicationPlugin.class.getName());

	public ItemCollection run(ItemCollection adocumentContext, ItemCollection adocumentActivity) throws PluginException {

		documentContext = adocumentContext;

		sEditorID = null;
		sImageURL = null;
		sAbstract = null;
		sSummary = null;

		// try to get next ProcessEntity

		// get numNextProcessID and modelVersion
		int iNextProcessID = adocumentActivity.getItemValueInteger("numNextProcessID");

		// now get the next ProcessEntity from ctx
		ItemCollection itemColNextProcess = null;
		// get from model version
		String aModelVersion = adocumentActivity.getItemValueString("$modelVersion");
		try {
			itemColNextProcess = getCtx().getModelManager().getModel(aModelVersion).getTask(iNextProcessID);
		} catch (ModelException e) {
			logger.warning(
					"Warning - Task '" + iNextProcessID + "' is not defined by model version '" + aModelVersion + "' : "+e.getMessage());
			return documentContext;
		}


		// fetch Editor and Image
		sEditorID = itemColNextProcess.getItemValueString("txtEditorID");
		sImageURL = itemColNextProcess.getItemValueString("txtImageURL");

		// fetch workflow Abstract
		sAbstract = itemColNextProcess.getItemValueString("txtworkflowabstract");
		if (!"".equals(sAbstract))
			sAbstract = this.replaceDynamicValues(sAbstract, documentContext);

		// fetch workflow Abstract
		sSummary = itemColNextProcess.getItemValueString("txtworkflowsummary");
		if (!"".equals(sSummary))
			sSummary = this.replaceDynamicValues(sSummary, documentContext);

		// submit data now into documentcontext
	
		// set Editor if value is defined
		if (sEditorID != null && !"".equals(sEditorID))
			documentContext.replaceItemValue("txtWorkflowEditorID", sEditorID);

		// set ImageURl if one is defined
		if (sImageURL != null && !"".equals(sImageURL))
			documentContext.replaceItemValue("txtWorkflowImageURL", sImageURL);

	
		// set Abstract
		if (sAbstract != null)
			documentContext.replaceItemValue("txtworkflowabstract", sAbstract);

		// set Summary
		if (sSummary != null)
			documentContext.replaceItemValue("txtworkflowsummary", sSummary);

		return documentContext;
	}

	

}
