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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.MockWorkflowContext;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openbpmn.bpmn.BPMNModel;

/**
 * Test class to test Loop Situations.
 * 
 * The WorklfowKernel should detect infinite event loops
 * 
 * 
 * @see issue #299
 * @author rsoika
 */
public class TestWorkflowKernelLoop {

	MockWorkflowContext workflowEngine;

	@BeforeEach
	public void setup() {
		try {
			workflowEngine = new MockWorkflowContext();
			workflowEngine.loadBPMNModelFromFile("/bpmn/loop-event.bpmn");
			BPMNModel model = workflowEngine.fetchModel("1.0.0");
			assertNotNull(model);
		} catch (PluginException | ModelException e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Here we test _capacity <= 1.00
	 * No Loop expected
	 * 
	 */
	@Test
	public void testLoopCase1() {

		ItemCollection workItem = new ItemCollection();
		workItem.setItemValue("budget", 1.0);
		workItem.model("1.0.0")
				.task(1000)
				.event(10);

		try {
			workflowEngine.getWorkflowKernel().process(workItem);
			// We expect 1200 because budget is <= 1.00
			assertEquals(1200, workItem.getTaskID());
			assertEquals(2, workItem.getItemValueInteger("runs"));
		} catch (ModelException | PluginException e) {
			e.printStackTrace();
			fail();
		}

	}

	/**
	 * Here we test _capacity <= 1.00 with the eval method
	 * No Loop expected
	 * 
	 */
	@Test
	public void testLoopCase1Eval() {

		ItemCollection workItem = new ItemCollection();
		workItem.setItemValue("budget", 1.0);
		workItem.model("1.0.0")
				.task(1000)
				.event(10);

		try {
			ItemCollection task = workflowEngine.getWorkflowKernel().eval(workItem);
			// We expect 1200 because budget is <= 1.00
			assertEquals(1200, task.getItemValueInteger("taskID"));
			assertEquals(0, workItem.getItemValueInteger("runs"));
		} catch (ModelException | PluginException e) {
			e.printStackTrace();
			fail();
		}

	}

	/**
	 * Here we test _capacity > 1.00
	 * Loop expected
	 * 
	 */
	@Test
	public void testLoopCase2() {
		ItemCollection workItem = new ItemCollection();
		workItem.setItemValue("budget", 100.50);
		workItem.model("1.0.0")
				.task(1000)
				.event(10);
		try {
			workflowEngine.getWorkflowKernel().process(workItem);
			// We expect a ProcessingErrorException! budget is <= 1.00
			fail();
		} catch (ModelException | PluginException e) {
			e.printStackTrace();
			fail();

		} catch (ProcessingErrorException e) {
			// Expected situation
			System.out.println(e.getMessage());
			assertTrue(e.getMessage().contains("loop detected"));
		}

	}

	/**
	 * Here we test _capacity > 1.00 with the eval() method
	 * Loop expected
	 * 
	 */
	@Test
	public void testLoopCase2Eval() {
		ItemCollection workItem = new ItemCollection();
		workItem.setItemValue("budget", 100.50);
		workItem.model("1.0.0")
				.task(1000)
				.event(10);
		try {
			workflowEngine.getWorkflowKernel().eval(workItem);
			// We expect a ProcessingErrorException! budget is <= 1.00
			fail();
		} catch (ModelException | PluginException e) {
			e.printStackTrace();
			fail();

		} catch (ProcessingErrorException e) {
			// Expected situation
			System.out.println(e.getMessage());
			assertTrue(e.getMessage().contains("loop detected"));
		}

	}

}