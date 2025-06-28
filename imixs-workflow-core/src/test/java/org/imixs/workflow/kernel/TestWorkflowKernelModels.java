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

package org.imixs.workflow.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.MockWorkflowContext;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for Imixs WorkflowKernel using the test models. The test class
 * verifies complex model situations based on the test models.
 * 
 * @author rsoika
 * 
 */
public class TestWorkflowKernelModels {

	private static final Logger logger = Logger.getLogger(TestWorkflowKernelModels.class.getName());

	MockWorkflowContext workflowEngine;

	@BeforeEach
	public void setup() {
		try {
			workflowEngine = new MockWorkflowContext();

		} catch (PluginException e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Simple test
	 * 
	 */
	@Test
	public void testSimpleModel() {
		try {
			workflowEngine.loadBPMNModelFromFile("/bpmn/simple.bpmn");

			ItemCollection itemCollection = new ItemCollection();
			itemCollection.replaceItemValue("txtTitel", "Hello");
			itemCollection.setTaskID(1000);
			itemCollection.setEventID(10);

			itemCollection.replaceItemValue("$modelversion", "1.0.0");

			itemCollection = workflowEngine.processWorkItem(itemCollection);
			assertEquals("Hello", itemCollection.getItemValueString("txttitel"));
			assertEquals("workitem", itemCollection.getItemValueString("type"));
			assertEquals(1000, itemCollection.getTaskID());

			itemCollection.event(20);
			itemCollection = workflowEngine.processWorkItem(itemCollection);
			assertEquals("workitemarchive", itemCollection.getItemValueString("type"));
			assertEquals(1100, itemCollection.getTaskID());

		} catch (Exception e) {
			fail();
			e.printStackTrace();
		}

	}

	/**
	 * ticket.bpmn test
	 * 
	 */
	@Test
	public void testTicketModel() {
		try {
			workflowEngine.loadBPMNModelFromFile("/bpmn/ticket.bpmn");

			ItemCollection itemCollection = new ItemCollection();
			itemCollection.replaceItemValue("txtTitel", "Hello");
			itemCollection.setTaskID(1100);
			itemCollection.setEventID(20);

			itemCollection.replaceItemValue("$modelversion", "1.0.0");
			itemCollection = workflowEngine.getWorkflowKernel().process(itemCollection);
			assertEquals("Hello", itemCollection.getItemValueString("txttitel"));
			assertEquals(1200, itemCollection.getTaskID());
			assertEquals("in Progress", itemCollection.getItemValueString("$workflowstatus"));
			assertEquals("Ticket", itemCollection.getItemValueString("$workflowgroup"));

			// test support for deprecated items
			assertEquals("in Progress", itemCollection.getItemValueString("txtworkflowstatus"));
			assertEquals("Ticket", itemCollection.getItemValueString("txtworkflowgroup"));

		} catch (Exception e) {
			fail();
			e.printStackTrace();
		}

	}

	/**
	 * Test model conditional_event1.bpmn.
	 * 
	 * Here we have two conditions: both to a task.
	 */
	@Test
	public void testConditionalEventModel1() {
		try {
			workflowEngine.loadBPMNModelFromFile("/bpmn/conditional_event1.bpmn");

			// test Condition 1
			ItemCollection itemCollection = new ItemCollection();
			itemCollection.replaceItemValue("txtTitel", "Hello");
			itemCollection.setTaskID(1000);
			itemCollection.setEventID(10);
			itemCollection.replaceItemValue("$modelversion", "1.0.0");

			itemCollection.replaceItemValue("_budget", 99);

			itemCollection = workflowEngine.getWorkflowKernel().process(itemCollection);
			assertEquals("Hello", itemCollection.getItemValueString("txttitel"));
			assertEquals(1200, itemCollection.getTaskID());

			// test Condition 2
			itemCollection = new ItemCollection();
			itemCollection.replaceItemValue("txtTitel", "Hello");
			itemCollection.setTaskID(1000);
			itemCollection.setEventID(10);
			itemCollection.replaceItemValue("$modelversion", "1.0.0");

			itemCollection.replaceItemValue("_budget", 9999);

			itemCollection = workflowEngine.getWorkflowKernel().process(itemCollection);
			assertEquals("Hello", itemCollection.getItemValueString("txttitel"));

			assertEquals(1100, itemCollection.getTaskID());

		} catch (Exception e) {
			fail();
			e.printStackTrace();

		}

	}

	/**
	 * Test model conditional_event2.bpmn.
	 * 
	 * Here we have two conditions: one to a task, the other to a event.
	 * 
	 */
	@Test
	public void testConditionalEventModel2() {
		try {
			workflowEngine.loadBPMNModelFromFile("/bpmn/conditional_event2.bpmn");

			// test Condition 1
			ItemCollection itemCollection = new ItemCollection();
			itemCollection.replaceItemValue("txtTitel", "Hello");
			itemCollection.setTaskID(1000);
			itemCollection.setEventID(10);
			itemCollection.replaceItemValue("$modelversion", "1.0.0");

			itemCollection.replaceItemValue("_budget", 9999);

			itemCollection = workflowEngine.getWorkflowKernel().process(itemCollection);
			assertEquals("Hello", itemCollection.getItemValueString("txttitel"));
			assertEquals(1100, itemCollection.getTaskID());

			// test Condition 2
			itemCollection = new ItemCollection().model("1.0.0").task(1000).event(10);
			itemCollection.replaceItemValue("txtTitel", "Hello");

			itemCollection.replaceItemValue("_budget", 99);

			itemCollection = workflowEngine.getWorkflowKernel().process(itemCollection);
			assertEquals("Hello", itemCollection.getItemValueString("txttitel"));

			assertEquals(1200, itemCollection.getTaskID());

		} catch (Exception e) {
			fail();
			e.printStackTrace();

		}

	}

	/**
	 * Test model split_event1.bpmn.
	 * 
	 * Here we have two conditions: both to a task.
	 */
	@Test
	public void testSplitEventModel1() {
		try {
			workflowEngine.loadBPMNModelFromFile("/bpmn/split_event1.bpmn");

			// test Condition 1
			ItemCollection itemCollection = new ItemCollection();
			itemCollection.replaceItemValue("_subject", "Hello");
			itemCollection.setTaskID(1000);
			itemCollection.setEventID(10);
			itemCollection.replaceItemValue("$modelversion", "1.0.0");

			itemCollection = workflowEngine.getWorkflowKernel().process(itemCollection);
			assertEquals("Hello", itemCollection.getItemValueString("_subject"));
			assertEquals(1100, itemCollection.getTaskID());
			assertEquals(10, itemCollection.getItemValueInteger("$lastEvent"));

			// test new version...
			List<ItemCollection> versions = workflowEngine.getWorkflowKernel().getSplitWorkitems();
			assertNotNull(versions);
			assertTrue(versions.size() == 1);
			ItemCollection version = versions.get(0);

			assertEquals("Hello", version.getItemValueString("_subject"));
			assertEquals(1200, version.getTaskID());
			// $lastEvent should be 20
			assertEquals(20, version.getItemValueInteger("$lastEvent"));

			// Master $uniqueid must not match the version $uniqueid
			assertFalse(itemCollection.getUniqueID().equals(version.getUniqueID()));

			// $uniqueidSource must match $uni1ueid of master
			assertEquals(itemCollection.getUniqueID(),
					version.getItemValueString(WorkflowKernel.UNIQUEIDSOURCE));

			// $uniqueidVirsions must mach $uniqueid of version
			assertEquals(version.getUniqueID(),
					itemCollection.getItemValueString(WorkflowKernel.UNIQUEIDVERSIONS));

		} catch (Exception e) {
			fail(e.getMessage());
			e.printStackTrace();

		}

	}

	/**
	 * Test model split_event1_invalid.bpmn.
	 * 
	 * This model is invalid as a outcome of the split-event is not followed by at
	 * least one Event
	 * 
	 */
	@Test
	public void testSplitEventInvalidModelMissingEvent() {

		workflowEngine.loadBPMNModelFromFile("/bpmn/split_event_invalid_1.bpmn");

		// test Condition 1
		ItemCollection workItem = new ItemCollection();
		workItem.replaceItemValue("_subject", "Hello");
		workItem.model("1.0.0").task(1000).event(10);
		assertNotNull(workItem);
		// model exception expected because the parallel gateway does not provide
		// events!
		try {
			workItem = workflowEngine.getWorkflowKernel().process(workItem);
			fail();

		} catch (PluginException | ModelException e) {
			// not expected
			assertTrue(e.getMessage().contains("only one outcome can be directly linked to a task element!"));
		}

	}

	/**
	 * Test model split_event_invalid_2.bpmn.
	 * 
	 * This model is invalid as no outcome is followed by a Task
	 * 
	 */
	@Test
	public void testSplitEventInvalidModelMissingTask() {

		workflowEngine.loadBPMNModelFromFile("/bpmn/split_event_invalid_2.bpmn");

		// test Condition 1
		ItemCollection workItem = new ItemCollection();
		workItem.replaceItemValue("_subject", "Hello");
		workItem.model("1.0.0").task(1000).event(10);
		assertNotNull(workItem);
		// model exception expected because the parallel gateway does not provide
		// events!
		try {
			workItem = workflowEngine.getWorkflowKernel().process(workItem);
			fail();

		} catch (PluginException | ModelException e) {
			// not expected
			assertTrue(e.getMessage().contains("At least one outcome must be connected directly to a Task Element or"));
		}

	}

	/**
	 * Test model split_event_invalid_3.bpmn.
	 * 
	 * This model shows a situation whe each outcome is connected to a event. In
	 * this case one SequenceFlow has to have a condition to evaluate to true.
	 * 
	 */
	@Test
	public void testSplitEventConditionalFlowsWithEvents() {

		workflowEngine.loadBPMNModelFromFile("/bpmn/split_event_invalid_3.bpmn");

		// test Condition 1
		ItemCollection workItem = new ItemCollection();
		workItem.replaceItemValue("_subject", "Hello");
		workItem.model("1.0.0").task(1000).event(10);
		assertNotNull(workItem);
		// model exception expected because the parallel gateway does not provide
		// events!
		try {
			workItem = workflowEngine.getWorkflowKernel().process(workItem);
			assertEquals("Hello", workItem.getItemValueString("_subject"));
			assertEquals(1100, workItem.getTaskID());
			assertEquals(20, workItem.getItemValueInteger("$lastEvent"));

			// test new version...
			List<ItemCollection> versions = workflowEngine.getWorkflowKernel().getSplitWorkitems();
			assertNotNull(versions);
			assertTrue(versions.size() == 1);
			ItemCollection version = versions.get(0);

			assertEquals("Hello", version.getItemValueString("_subject"));
			assertEquals(1200, version.getTaskID());
			// $lastEvent should be 30
			assertEquals(30, version.getItemValueInteger("$lastEvent"));

			// Master $uniqueid must not match the version $uniqueid
			assertFalse(workItem.getUniqueID().equals(version.getUniqueID()));

			// $uniqueidSource must match $uniqueid of master
			assertEquals(workItem.getUniqueID(),
					version.getItemValueString(WorkflowKernel.UNIQUEIDSOURCE));

			// $uniqueidVirsions must mach $uniqueid of version
			assertEquals(version.getUniqueID(),
					workItem.getItemValueString(WorkflowKernel.UNIQUEIDVERSIONS));
		} catch (PluginException | ModelException e) {
			// not expected
			assertTrue(e.getMessage().contains("At least one outcome must be connected directly to a Task Element!"));
		}

	}

}
