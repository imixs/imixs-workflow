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
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.MockWorkflowContext;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;

/**
 * Test class test the Imixs BPMNModel behavior.
 * 
 * @author rsoika
 */
public class TestModelManagerMultipleTaskTypes {

	BPMNModel model = null;
	ModelManager modelManager = null;
	MockWorkflowContext workflowContext;

	@BeforeEach
	public void setup() {
		try {
			workflowContext = new MockWorkflowContext();
			modelManager = new ModelManager(workflowContext);
			workflowContext.loadBPMNModelFromFile("/bpmn/simple-multiple-tasktypes.bpmn");
			model = workflowContext.fetchModel("1.0.0");
			assertNotNull(model);

		} catch (ModelException | PluginException e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Test the startTasks and startEvents
	 * 
	 * @throws ModelException
	 */
	@Test
	public void testStartTasks() throws ModelException {
		try {

			// find start tasks
			List<ItemCollection> startTasks = modelManager.findStartTasks(model, "Simple");
			assertNotNull(startTasks);
			assertEquals(1, startTasks.size());
			ItemCollection startTask = startTasks.get(0);
			assertNotNull(startTask);
			assertEquals("Task 1", startTask.getItemValueString(BPMNUtil.TASK_ITEM_NAME));
		} catch (ModelException e) {
			fail();
		}
	}

	@Test
	public void testStartTasksComplex() throws ModelException {
		try {
			model = BPMNModelFactory.read("/bpmn/simple-startevent.bpmn");
			assertNotNull(model);

			// find start tasks
			List<ItemCollection> startTasks = modelManager.findStartTasks(model, "Simple");
			assertNotNull(startTasks);
			assertEquals(1, startTasks.size());
			ItemCollection startTask = startTasks.get(0);
			assertNotNull(startTask);
			assertEquals("Task 1", startTask.getItemValueString(BPMNUtil.TASK_ITEM_NAME));

			List<ItemCollection> events = modelManager.findEventsByTask(model, 1000);
			assertEquals(3, events.size());
		} catch (BPMNModelException e) {
			fail();
		}
	}

	/**
	 * Test the endTasks
	 * 
	 * @throws ModelException
	 */
	@Test
	public void testEndTasks() throws ModelException {
		try {
			// test End task....
			List<ItemCollection> endTasks = modelManager.findEndTasks(model, "Simple");
			assertNotNull(endTasks);
			assertEquals(1, endTasks.size());

			ItemCollection endTask = endTasks.get(0);
			assertEquals("Task-9", endTask.getItemValueString(BPMNUtil.TASK_ITEM_NAME));
		} catch (ModelException e) {
			fail();
		}
	}

	/**
	 * Test find events by task
	 * 
	 * @throws ModelException
	 */
	@Test
	public void testFindEvetnsByTasks() throws ModelException {

		// test End task....
		List<ItemCollection> events = modelManager.findEventsByTask(model, 1000);
		assertNotNull(events);
		assertEquals(2, events.size());

	}

	/**
	 * Test the behavior of manipulating task objects.
	 * 
	 * A method may changes an attribute of a Event object, but if we reload the
	 * event 'model.getEvent()' than the origin value must be returned!
	 * 
	 * @see http://stackoverflow.com/questions/40480/is-java-pass-by-reference-or-pass-by-value#40523
	 * 
	 * @throws ModelException
	 */
	@Test
	public void testModifyTask() throws ModelException {

		// test task 1000
		ItemCollection task = modelManager.findTaskByID(model, 1000);
		assertNotNull(task);
		assertEquals("Task 1", task.getItemValueString("name"));
		assertEquals("Some documentation...", task.getItemValueString("documentation"));
		// change some attributes of task....
		task.replaceItemValue("txtworkflowgroup", "test");
		assertEquals("test", task.getItemValueString("txtworkflowgroup"));
		// test task 1000 once again
		task = modelManager.findTaskByID(model, 1000);
		assertNotNull(task);
		// changes should not have taken effect.
		assertEquals("", task.getItemValueString("txtworkflowgroup"));

	}

	/**
	 * Test the usage of multiple task types
	 * 
	 * @throws ModelException
	 */
	@Test
	public void testMultipleTaskTypes() throws ModelException {
		try {

			// find start tasks
			List<ItemCollection> allTasks = modelManager.findTasks(model, "Simple");
			assertNotNull(allTasks);
			assertEquals(8, allTasks.size());

		} catch (ModelException e) {
			fail();
		}
	}
}
