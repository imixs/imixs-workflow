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

package org.imixs.workflow.plugins.jee;

import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.jee.ejb.WorkflowService;
import org.imixs.workflow.plugins.jee.AbstractPlugin;
import org.imixs.workflow.plugins.ResultPlugin;

/**
 * The Imixs Split&Join Plugin provides functionality to create and update
 * sub-process instances from a workflow event in an origin process. It is also
 * possible to update the origin process from the sub-process instance.
 * 
 * @author Ralph Soika
 * @version 1.0
 * @see http://www.imixs.org/doc/engine/plugins/splitandjoinplugin.html
 * 
 */
public class SplitAndJoinPlugin extends AbstractPlugin {

	public static final String INVALID_FORMAT = "INVALID_FORMAT";
	public static final String SUBPROCESS_CREATE = "subprocess_create";
	public static final String SUBPROCESS_UPDATE = "subprocess_update";
	public static final String ORIGIN_UPDATE = "origin_update";

	private WorkflowService workflowService = null;
	ItemCollection documentContext;
	String sActivityResult;
	private static Logger logger = Logger.getLogger(SplitAndJoinPlugin.class.getName());

	/**
	 * Overwrite init to get the instance of the WorkflowService
	 */
	@Override
	public void init(WorkflowContext actx) throws PluginException {
		super.init(actx);
		// check for an instance of WorkflowService
		if (actx instanceof WorkflowService) {
			// yes we are running in a WorkflowService EJB
			workflowService = (WorkflowService) actx;
		}
	}

	/**
	 * The method evaluates the workflow activity result
	 */
	public int run(ItemCollection adocumentContext, ItemCollection adocumentActivity) throws PluginException {

		ItemCollection evalItemCollection = ResultPlugin.evaluate(adocumentActivity, adocumentContext);

		if (evalItemCollection.hasItem(SUBPROCESS_CREATE)) {
			logger.fine("Evaluate Subprocess Item...");

			// evaluate the item content (XML format expected here!)
			ItemCollection processData = ResultPlugin
					.parseItemStructure(evalItemCollection.getItemValueString(SUBPROCESS_CREATE).trim());

			// create new process instance
			ItemCollection workitemSubProcess = new ItemCollection();
			workitemSubProcess.replaceItemValue(WorkflowKernel.MODELVERSION,
					processData.getItemValueString("modelversion"));
			workitemSubProcess.replaceItemValue(WorkflowKernel.PROCESSID,
					new Integer(processData.getItemValueString("processid")));
			workitemSubProcess.replaceItemValue(WorkflowKernel.ACTIVITYID,
					processData.getItemValueString("activityid"));

			// now clone the field list...
			String items = processData.getItemValueString("items");
			StringTokenizer st = new StringTokenizer(items, ",");
			while (st.hasMoreTokens()) {
				String field = st.nextToken().trim();
				workitemSubProcess.replaceItemValue(field, adocumentContext.getItemValue(field));
			}

			// finally we try to process the new subprocess...
			workitemSubProcess = workflowService.processWorkItem(workitemSubProcess);
			logger.fine("[GeNesysPlugin] successful processed subprocess.");

		}

		return Plugin.PLUGIN_OK;
	}

	@Override
	public void close(int status) throws PluginException {
		// no op

	}

}
