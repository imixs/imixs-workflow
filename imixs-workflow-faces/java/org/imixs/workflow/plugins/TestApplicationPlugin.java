/****************************************************************************
 * Copyright (c) 2022-2025 Imixs Software Solutions GmbH and others.
 * https://www.imixs.com
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * This Source Code may also be made available under the terms of the
 * GNU General Public License, version 2 or later (GPL-2.0-or-later),
 * which is available at https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-or-later
 ****************************************************************************/

package org.imixs.workflow.plugins;

import java.util.Vector;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.WorkflowMockEnvironment;
import org.imixs.workflow.engine.plugins.ApplicationPlugin;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Before;
import org.junit.Test;

import org.junit.Assert;

/**
 * Test the Application plug-in.
 * 
 * The plug-in evaluates the next process entity
 * 
 * @author rsoika 
 * 
 */
public class TestApplicationPlugin  {

	private final static Logger logger = Logger
			.getLogger(TestApplicationPlugin.class.getName());

	protected ApplicationPlugin applicationPlugin = null;
	protected ItemCollection documentContext;
	protected ItemCollection documentActivity, documentProcess;
	protected WorkflowMockEnvironment workflowMockEnvironment;

	@Before
	public void setUp() throws PluginException, ModelException {

		workflowMockEnvironment=new WorkflowMockEnvironment();
		workflowMockEnvironment.setModelPath("/bpmn/plugin-test.bpmn");
		
		workflowMockEnvironment.setup();

		
	
		applicationPlugin = new ApplicationPlugin();
		try {
			applicationPlugin.init(workflowMockEnvironment.getWorkflowService());
		} catch (PluginException e) {

			e.printStackTrace();
		}

		// prepare data
		documentContext = new ItemCollection().model("1.0.0").task(100);
		logger.info("[TestApplicationPlugin] setup test data...");
		Vector<String> list = new Vector<String>();
		list.add("manfred");
		list.add("anna");
		documentContext.replaceItemValue("namTeam", list);

		documentContext.replaceItemValue("namCreator", "ronny");
	}

	/**
	 * Test if the txtEditorID form the next processEntity is associated to the
	 * current workitem by the ApplicationPlugin
	 * @throws ModelException 
	 */
	@Test
	public void simpleTest() throws ModelException {

		documentActivity = workflowMockEnvironment.getModel().getEvent(100, 10);
		documentContext.setEventID(10);
		try {
			applicationPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		try {
			applicationPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) { 

			e.printStackTrace();
			Assert.fail();
		}

		String newEditor = documentContext
				.getItemValueString("txtWorkflowEditorID");

		Assert.assertEquals("test-data", newEditor);

	}

}
