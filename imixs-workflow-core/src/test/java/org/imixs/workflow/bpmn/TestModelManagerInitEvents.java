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

package org.imixs.workflow.bpmn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.MockWorkflowContext;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openbpmn.bpmn.BPMNModel;

/**
 * Test class tests complex situations of a Task with several init events.
 * 
 * @author rsoika
 */
public class TestModelManagerInitEvents {
	BPMNModel model = null;
	ModelManager modelManager = null;
	MockWorkflowContext workflowContext;

	@BeforeEach
	public void setup() {
		try {
			workflowContext = new MockWorkflowContext();
			modelManager = new ModelManager(workflowContext);
			workflowContext.loadBPMNModelFromFile("/bpmn/startevent_followup.bpmn");
			model = workflowContext.fetchModel("1.0.0");
			assertNotNull(model);

		} catch (ModelException | PluginException e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Test the startEvents. We expect 3 Events for Task 1000 : 'init', 'import',
	 * 'save'
	 * 
	 */
	@Test
	public void testStartEvents() {
		try {
			// test start task....
			modelManager.findStartTasks(model, "Simple");
			List<ItemCollection> startEvents = modelManager.findEventsByTask(model, 1000);
			assertNotNull(startEvents);
			assertEquals(3, startEvents.size());

			// we expect event 20 and 10 but not event 30 as a possible Start events
			List<String> names = new ArrayList<String>();
			for (ItemCollection event : startEvents) {
				names.add(event.getItemValueString(BPMNUtil.EVENT_ITEM_NAME));
			}
			assertTrue(names.contains("import"));
			assertTrue(names.contains("init"));
			assertTrue(names.contains("save"));
		} catch (ModelException e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Test the startTasks
	 * 
	 */
	@Test
	public void testStartTasks() {
		// test start task....
		List<ItemCollection> startTasks;
		try {
			startTasks = modelManager.findStartTasks(model, "Simple");
			assertNotNull(startTasks);
			assertEquals(1, startTasks.size());
			ItemCollection startTask = startTasks.get(0);
			assertEquals("Task 1", startTask.getItemValueString(BPMNUtil.TASK_ITEM_NAME));
		} catch (ModelException e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Test the endTasks
	 * 
	 */
	@Test
	public void testEndTasks() {
		// test start task....
		try {
			List<ItemCollection> endTasks;
			endTasks = modelManager.findEndTasks(model, "Simple");
			assertNotNull(endTasks);
			assertEquals(1, endTasks.size());
			ItemCollection endTask = endTasks.get(0);
			assertEquals("Task 1", endTask.getItemValueString(BPMNUtil.TASK_ITEM_NAME));
		} catch (ModelException e) {
			fail(e.getMessage());
		}
	}
}
