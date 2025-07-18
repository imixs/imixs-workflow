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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Vector;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.MockWorkflowEnvironment;
import org.imixs.workflow.engine.plugins.ApplicationPlugin;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openbpmn.bpmn.BPMNModel;

/**
 * Test the Application plug-in.
 * 
 * The plug-in evaluates the next process entity
 * 
 * @author rsoika
 * 
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class TestApplicationPlugin {

	private final static Logger logger = Logger
			.getLogger(TestApplicationPlugin.class.getName());

	protected ApplicationPlugin applicationPlugin = null;
	protected ItemCollection workitem;
	protected ItemCollection event;
	protected MockWorkflowEnvironment workflowEngine;

	@BeforeEach
	public void setUp() throws PluginException, ModelException {

		workflowEngine = new MockWorkflowEnvironment();
		workflowEngine.setUp();
		workflowEngine.loadBPMNModelFromFile("/bpmn/plugin-test.bpmn");

		applicationPlugin = new ApplicationPlugin();
		try {
			applicationPlugin.init(workflowEngine.getWorkflowService());
		} catch (PluginException e) {
			fail(e.getMessage());
		}

		// prepare data
		workitem = workflowEngine.getDocumentService().load("W0000-00001");
		workitem.model("1.0.0").task(100);
		logger.info("[TestApplicationPlugin] setup test data...");
		Vector<String> list = new Vector<String>();
		list.add("manfred");
		list.add("anna");
		workitem.replaceItemValue("namTeam", list);
		workitem.replaceItemValue("namCreator", "ronny");
	}

	/**
	 * Test if the txtEditorID form the next processEntity is associated to the
	 * current workitem by the ApplicationPlugin
	 * 
	 * @throws ModelException
	 */
	@Test
	public void simpleTest() throws ModelException {

		workitem.event(10);
		BPMNModel model = workflowEngine.getModelManager().getModelByWorkitem(workitem);
		event = workflowEngine.getModelManager().loadEvent(workitem, model);

		try {
			applicationPlugin.run(workitem, event);
		} catch (PluginException e) {
			fail(e.getMessage());
		}

		String newEditor = workitem
				.getItemValueString("txtWorkflowEditorID");

		assertEquals("test-data", newEditor);

	}

}
