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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.MockWorkflowEnvironment;
import org.imixs.workflow.engine.plugins.AnalysisPlugin;
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
 * Test class for AnalysisPlugin
 * 
 * @author rsoika
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class TestAnalysisPlugin {
	protected AnalysisPlugin analysisPlugin = null;
	private static final Logger logger = Logger.getLogger(TestAnalysisPlugin.class.getName());

	ItemCollection workitem;

	protected MockWorkflowEnvironment workflowEngine;

	@BeforeEach
	public void setUp() throws PluginException, ModelException {

		workflowEngine = new MockWorkflowEnvironment();
		workflowEngine.setUp();
		workflowEngine.loadBPMNModelFromFile("/bpmn/plugin-test.bpmn");

		analysisPlugin = new AnalysisPlugin();
		try {
			analysisPlugin.init(workflowEngine.getWorkflowService());
		} catch (PluginException e) {
			fail(e.getMessage());
		}

		workitem = workflowEngine.getDocumentService().load("W0000-00001");
		workitem.model("1.0.0").task(100);

	}

	/**
	 * Verify the start mechanism
	 * 
	 * @throws PluginException
	 */
	@Test
	public void testBasicTest() throws PluginException {

		workitem.replaceItemValue("txtName", "Anna");
		workitem.event(10);
		ItemCollection event;
		try {
			BPMNModel model = workflowEngine.getModelManager().getModelByWorkitem(workitem);
			event = workflowEngine.getModelManager().loadEvent(workitem, model);

			String sResult = "<item name='measurepoint' type='start'>M1</item>";
			logger.log(Level.INFO, "txtActivityResult={0}", sResult);
			event.replaceItemValue("txtActivityResult", sResult);

			workitem = analysisPlugin.run(workitem, event);
			assertNotNull(workitem);

			assertTrue(workitem.hasItem("datMeasurePointStart_M1"));

			logger.log(Level.INFO, "datMeasurePointStart_M1= {0}",
					workitem.getItemValueDate("datMeasurePointStart_M1"));
		} catch (ModelException e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Verify the start mechanism
	 * 
	 * @throws PluginException
	 */
	@Test
	public void testWrongStartTest() throws PluginException {

		workitem = new ItemCollection();
		workitem.replaceItemValue("txtName", "Anna");

		// Activity Entity Dummy
		ItemCollection event = new ItemCollection();

		String sResult = "<item name='measurepoint' type='start'>M1</item>";
		logger.log(Level.INFO, "txtActivityResult={0}", sResult);
		event.replaceItemValue("txtActivityResult", sResult);

		workitem = analysisPlugin.run(workitem, event);
		assertNotNull(workitem);

		assertTrue(workitem.hasItem("datMeasurePointStart_M1"));

		logger.log(Level.INFO, "datMeasurePointStart_M1= {0}",
				workitem.getItemValueDate("datMeasurePointStart_M1"));

		sResult = "<item name='measurepoint' type='start'>M1</item>";
		logger.log(Level.INFO, "txtActivityResult={0}", sResult);
		event.replaceItemValue("txtActivityResult", sResult);

		workitem = analysisPlugin.run(workitem, event);
		assertNotNull(workitem);

		assertTrue(workitem.hasItem("datMeasurePointStart_M1"));
	}

	/**
	 * Verify the start numMeasurePoint_
	 * 
	 * @throws PluginException
	 */
	@Test
	public void testTotalTime() throws PluginException {

		workitem = new ItemCollection();
		workitem.replaceItemValue("txtName", "Anna");

		// Activity Entity Dummy
		ItemCollection event = new ItemCollection();

		String sResult = "<item name='measurepoint' type='start'>M1</item>";
		logger.log(Level.INFO, "txtActivityResult={0}", sResult);
		event.replaceItemValue("txtActivityResult", sResult);

		workitem = analysisPlugin.run(workitem, event);
		assertNotNull(workitem);

		assertTrue(workitem.hasItem("datMeasurePointStart_M1"));

		logger.log(Level.INFO, "datMeasurePointStart_M1= {0}",
				workitem.getItemValueDate("datMeasurePointStart_M1"));

		try {
			Thread.sleep(1000); // 1000 milliseconds is one second.
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}

		sResult = "<item name='measurepoint' type='stop'>M1</item>";
		logger.log(Level.INFO, "txtActivityResult={0}", sResult);
		event.replaceItemValue("txtActivityResult", sResult);

		workitem = analysisPlugin.run(workitem, event);
		assertNotNull(workitem);

		assertTrue(workitem.hasItem("datMeasurePointStart_M1"));

		int time = workitem.getItemValueInteger("numMeasurePoint_M1");

		System.out.println("Time=" + time);
		assertTrue(time > 0);
	}

}
