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
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.imixs.workflow.util.XMLParser;

/**
 * The Imixs Split&Join Plugin provides functionality to create and update
 * sub-process instances from a workflow event in an origin process. It is also
 * possible to update the origin process from the sub-process instance.
 * 
 * The plugin evaluates the txtactivityResult and the items with the following
 * names:
 * 
 * subprocess_create = create a new subprocess assigned to the current workitem
 * 
 * subprocess_update = update an existing subprocess assigned to the current
 * workitem
 * 
 * origin_update = update the origin process assigned to the current workitem
 * 
 * 
 * A subprocess will contain the $UniqueID of the origin process stored in the
 * property $uniqueidRef. The origin process will contain a link to the
 * subprocess stored in the property txtworkitemRef. So both workitems are
 * linked together.
 * 
 * 
 * @author Ralph Soika
 * @version 1.0
 * @see http://www.imixs.org/doc/engine/plugins/splitandjoinplugin.html
 * 
 */
public class SplitAndJoinPlugin extends AbstractPlugin {
	public static final String LINK_PROPERTY = "txtworkitemref";
	public static final String INVALID_FORMAT = "INVALID_FORMAT";
	public static final String SUBPROCESS_CREATE = "subprocess_create";
	public static final String SUBPROCESS_UPDATE = "subprocess_update";
	public static final String ORIGIN_UPDATE = "origin_update";

	private static Logger logger = Logger.getLogger(SplitAndJoinPlugin.class.getName());

	/**
	 * The method evaluates the workflow activity result for items with name:
	 * 
	 * subprocess_create
	 * 
	 * subprocess_update
	 * 
	 * origin_update
	 * 
	 * For each item a corresponding processing cycle will be started.
	 * 
	 * @throws @throws
	 *             ProcessingErrorException
	 * @throws AccessDeniedException
	 * 
	 */
	@SuppressWarnings("unchecked")
	public ItemCollection run(ItemCollection adocumentContext, ItemCollection adocumentActivity)
			throws PluginException, AccessDeniedException, ProcessingErrorException {

		ItemCollection evalItemCollection = getWorkflowService().evalWorkflowResult(adocumentActivity, adocumentContext,false);

		if (evalItemCollection == null)
			return adocumentContext;

		try {
			// 1.) test for items with name subprocess_create and create the
			// defined suprocesses
			if (evalItemCollection.hasItem(SUBPROCESS_CREATE)) {
				logger.finest("......processing " + SUBPROCESS_CREATE);
				// extract the create subprocess definitions...
				List<String> processValueList = evalItemCollection.getItemValue(SUBPROCESS_CREATE);
				createSubprocesses(processValueList, adocumentContext);
			}

			// 2.) test for items with name subprocess_update and create the
			// defined suprocesses
			if (evalItemCollection.hasItem(SUBPROCESS_UPDATE)) {
				logger.finest("......sprocessing " + SUBPROCESS_UPDATE);
				// extract the create subprocess definitions...
				List<String> processValueList = evalItemCollection.getItemValue(SUBPROCESS_UPDATE);
				updateSubprocesses(processValueList, adocumentContext);
			}

			// 3.) test for items with name origin_update and update the
			// origin workitem
			if (evalItemCollection.hasItem(ORIGIN_UPDATE)) {
				logger.finest("......processing " + ORIGIN_UPDATE);
				// extract the create subprocess definitions...
				String processValue = evalItemCollection.getItemValueString(ORIGIN_UPDATE);
				updateOrigin(processValue, adocumentContext);
			}
		} catch (ModelException e) {
			throw new PluginException(e.getErrorContext(), e.getErrorCode(), e.getMessage(), e);

		}

		return adocumentContext;
	}

	/**
	 * This method expects a list of Subprocess definitions and create for each
	 * definition a new subprocess. The reference of the created subprocess will
	 * be stored in the property txtworkitemRef of the origin workitem
	 * 
	 * 
	 * The definition is expected in the following format
	 * 
	 * <code>
	 *    <modelversion>1.0.0</modelversion>
	 *    <processid>100</processid>
	 *    <activityid>20</activityid>
	 *    <items>namTeam,_sub_data</items>
	 *    <action>home</action>
	 * </code>
	 * 
	 *
	 * Both workitems are connected to each other. The subprocess will contain
	 * the $UniqueID of the origin process stored in the property $uniqueidRef.
	 * The origin process will contain a link to the subprocess stored in the
	 * property txtworkitemRef.
	 *
	 * The tag 'action' is optional and allows to overwrite the action result evaluated by the ResultPlugin.
	 * 
	 * @param subProcessDefinitions
	 * @param originWorkitem
	 * @throws AccessDeniedException
	 * @throws ProcessingErrorException
	 * @throws PluginException
	 * @throws ModelException
	 */
	protected void createSubprocesses(final List<String> subProcessDefinitions, final ItemCollection originWorkitem)
			throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {

		if (subProcessDefinitions == null || subProcessDefinitions.size() == 0) {
			// no definition found
			return;
		}
		// we iterate over each declaration of a SUBPROCESS_CREATE item....
		for (String processValue : subProcessDefinitions) {

			if (processValue.trim().isEmpty()) {
				// no definition
				continue;
			}
			// evaluate the item content (XML format expected here!)
			ItemCollection processData = XMLParser.parseItemStructure(processValue);

			if (processData != null) {
				// create new process instance
				ItemCollection workitemSubProcess = new ItemCollection();

				// now clone the field list...
				copyItemList(processData.getItemValueString("items"), originWorkitem, workitemSubProcess);

				// check model version
				String sModelVersion=processData.getItemValueString("modelversion");
				if (sModelVersion.isEmpty()) {
					sModelVersion=originWorkitem.getModelVersion();
				}
				workitemSubProcess.replaceItemValue(WorkflowKernel.MODELVERSION,sModelVersion);
				workitemSubProcess.setTaskID(
						Integer.valueOf(processData.getItemValueString("processid")));
				workitemSubProcess.setEventID(
						Integer.valueOf(processData.getItemValueString("activityid")));

				
				
				// add the origin reference
				workitemSubProcess.replaceItemValue(WorkflowService.UNIQUEIDREF, originWorkitem.getUniqueID());

				// process the new subprocess...
				workitemSubProcess = getWorkflowService().processWorkItem(workitemSubProcess);

				logger.finest("...... successful created new subprocess.");
				// finally add the new workitemRef into the origin
				// documentContext
				addWorkitemRef(workitemSubProcess.getUniqueID(), originWorkitem);

				// test for optional action result..
				if (processData.hasItem("action")) {
					String workflowResult=processData.getItemValueString("action");
					if (!workflowResult.isEmpty()) {
						workflowResult = getWorkflowService().adaptText(workflowResult, workitemSubProcess);
						originWorkitem.replaceItemValue("action", workflowResult);
					}
					
				}
			}

		}
	}

	/**
	 * This method expects a list of Subprocess definitions and updates each
	 * matching existing subprocess.
	 * 
	 * The definition is expected in the following format (were regular
	 * expressions are allowed)
	 * 
	 * <code>
	 *    <modelversion>1.0.0</modelversion>
	 *    <processid>100</processid>
	 *    <activityid>20</activityid>
	 *    <items>namTeam,_sub_data</items>
	 * </code>
	 * 
	 * Subprocesses and the origin process are connected to each other. The
	 * subprocess will contain the $UniqueID of the origin process stored in the
	 * property $uniqueidRef. The origin process will contain a link to the
	 * subprocess stored in the property txtworkitemRef.
	 * 
	 * @param subProcessDefinitions
	 * @param originWorkitem
	 * @throws AccessDeniedException
	 * @throws ProcessingErrorException
	 * @throws PluginException
	 * @throws ModelException
	 */
	protected void updateSubprocesses(final List<String> subProcessDefinitions, final ItemCollection originWorkitem)
			throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {

		if (subProcessDefinitions == null || subProcessDefinitions.size() == 0) {
			// no definition found
			return;
		}
		// we iterate over each declaration of a SUBPROCESS_CREATE item....
		for (String processValue : subProcessDefinitions) {

			if (processValue.trim().isEmpty()) {
				// no definition
				continue;
			}
			// evaluate the item content (XML format expected here!)
			ItemCollection processData = XMLParser.parseItemStructure(processValue);

			if (processData != null) {
				// we need to lookup all subprocess instances which are matching
				// the process definition

				String model_pattern = processData.getItemValueString("modelversion");
				String process_pattern = processData.getItemValueString("processid");

				List<ItemCollection> subprocessList = getWorkflowService()
						.getWorkListByRef(originWorkitem.getUniqueID());
				// process all subprcess matching...
				for (ItemCollection workitemSubProcess : subprocessList) {

					// test if process matches
					String subModelVersion = workitemSubProcess.getModelVersion();
					String subProcessID = "" + workitemSubProcess.getTaskID();

					if (Pattern.compile(model_pattern).matcher(subModelVersion).find()
							&& Pattern.compile(process_pattern).matcher(subProcessID).find()) {

						logger.finest("...... subprocess matches criteria.");
						// now clone the field list...
						copyItemList(processData.getItemValueString("items"), originWorkitem, workitemSubProcess);

						workitemSubProcess.setEventID(
								Integer.valueOf(processData.getItemValueString("activityid")));
						// process the exisitng subprocess...
						workitemSubProcess = getWorkflowService().processWorkItem(workitemSubProcess);

						
						// test for optional action result..
						if (processData.hasItem("action")) {
							String workflowResult=processData.getItemValueString("action");
							if (!workflowResult.isEmpty()) {
								workflowResult = getWorkflowService().adaptText(workflowResult, workitemSubProcess);
								originWorkitem.replaceItemValue("action", workflowResult);
							}
						}

						logger.finest("...... successful updated subprocess.");
					}
					
					
					// test for optional action result..
					if (processData.hasItem("action")) {
						String workflowResult=processData.getItemValueString("action");
						if (!workflowResult.isEmpty()) {
							workflowResult = getWorkflowService().adaptText(workflowResult, workitemSubProcess);
							originWorkitem.replaceItemValue("action", workflowResult);
						}
						
					}
				}
				

				
			}

		}
	}

	/**
	 * This method expects a single process definitions to update the origin
	 * process for a subprocess. The origin workitem will be loaded by the
	 * $uniqueidRef stored in the subprocess
	 * 
	 * The processing definition for the origin process is expected in the
	 * following format
	 * 
	 * <code>
	 * 	  <activityid>20</activityid>
	 *    <items>namTeam,_sub_data</items>
	 * </code>
	 * 
	 * 
	 * @param originProcessDefinition
	 * @param subprocessWorkitem
	 * @throws AccessDeniedException
	 * @throws ProcessingErrorException
	 * @throws PluginException
	 * @throws ModelException
	 */
	@SuppressWarnings("unchecked")
	protected void updateOrigin(final String originProcessDefinition, final ItemCollection subprocessWorkitem)
			throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {

		ItemCollection originWorkitem = null;

		if (originProcessDefinition == null || originProcessDefinition.isEmpty()) {
			// no definition
			return;
		}

		// evaluate the item content (XML format expected here!)
		ItemCollection processData = XMLParser.parseItemStructure(originProcessDefinition);

		String model_pattern = processData.getItemValueString("modelversion");
		String process_pattern = processData.getItemValueString("processid");

		// first we need to lookup the corresponding origin process instance
		List<String> refs = subprocessWorkitem.getItemValue(WorkflowService.UNIQUEIDREF);
		// iterate over all refs and identify the origin workItem
		for (String ref : refs) {
			originWorkitem = getWorkflowService().getWorkItem(ref);
			if (originWorkitem != null) {

				// test if process matches
				String subModelVersion = originWorkitem.getModelVersion();
				String subProcessID = "" + originWorkitem.getTaskID();

				if (Pattern.compile(model_pattern).matcher(subModelVersion).find()
						&& Pattern.compile(process_pattern).matcher(subProcessID).find()) {

					logger.finest("...... origin matches criteria.");

					// process the origin workitem
					originWorkitem.setEventID(
							Integer.valueOf(processData.getItemValueString("activityid")));

					// now clone the field list...
					copyItemList(processData.getItemValueString("items"), subprocessWorkitem, originWorkitem);

					// finally we process the new subprocess...
					originWorkitem = getWorkflowService().processWorkItem(originWorkitem);

					// test for optional action result..
					if (processData.hasItem("action")) {
						String workflowResult=processData.getItemValueString("action");
						if (!workflowResult.isEmpty()) {
							workflowResult = getWorkflowService().adaptText(workflowResult, originWorkitem);
							subprocessWorkitem.replaceItemValue("action", workflowResult);
						}
						
					}
					logger.finest("...... successful processed originprocess.");

				}

			}

		}

	}

	/**
	 * This Method copies the fields defined in 'items' into the targetWorkitem.
	 * Multiple values are separated with comma ','.
	 * 
	 * In case a item name contains '|' the target field name will become the
	 * right part of the item name.
	 */
	protected void copyItemList(String items, ItemCollection source, ItemCollection target) {
		// clone the field list...
		StringTokenizer st = new StringTokenizer(items, ",");
		while (st.hasMoreTokens()) {
			String field = st.nextToken().trim();

			int pos = field.indexOf('|');
			if (pos > -1) {
				target.replaceItemValue(field.substring(pos + 1).trim(),
						source.getItemValue(field.substring(0, pos).trim()));
			} else {
				target.replaceItemValue(field, source.getItemValue(field));
			}
		}
	}

	/**
	 * This methods adds a new workItem reference into a workitem
	 */
	protected void addWorkitemRef(String aUniqueID, ItemCollection workitem) {

		logger.fine("LinkController add workitem reference: " + aUniqueID);

		@SuppressWarnings("unchecked")
		List<String> refList = workitem.getItemValue(LINK_PROPERTY);

		// clear empty entry if set
		if (refList.size() == 1 && "".equals(refList.get(0)))
			refList.remove(0);

		// test if not yet a member of
		if (refList.indexOf(aUniqueID) == -1) {
			refList.add(aUniqueID);
			workitem.replaceItemValue(LINK_PROPERTY, refList);
		}

	}
}
