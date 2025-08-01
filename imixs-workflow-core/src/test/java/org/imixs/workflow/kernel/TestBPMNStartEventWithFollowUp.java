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
 * Test class test the Imixs BPMNParser with special start event case including
 * a follow up
 * 
 * @author rsoika
 */
public class TestBPMNStartEventWithFollowUp {

	MockWorkflowContext workflowEngine;

	@BeforeEach
	public void setup() {
		try {
			workflowEngine = new MockWorkflowContext();
			workflowEngine.loadBPMNModelFromFile("/bpmn/startevent_followup.bpmn");
			BPMNModel model = workflowEngine.fetchModel("1.0.0");
			assertNotNull(model);
		} catch (PluginException | ModelException e) {
			fail(e.getMessage());
		}
	}

	/**
	 * This test calls a start event with a follow up event
	 */
	@Test
	public void testFollowUpStartEvent() {

		ItemCollection workItem = new ItemCollection();
		workItem.model("1.0.0")
				.task(1000)
				.event(20);
		try {
			workItem = workflowEngine.getWorkflowKernel().process(workItem);
			assertEquals(2, workItem.getItemValueInteger("runs"));
			assertEquals(1000, workItem.getTaskID());

		} catch (ModelException | ProcessingErrorException | PluginException e) {
			e.printStackTrace();
			fail();
		}
	}

	/**
	 * This test calls a init event with a follow up event
	 */
	@Test
	public void testFollowUpInit() {

		ItemCollection workItem = new ItemCollection();
		workItem.model("1.0.0")
				.task(1000)
				.event(40);
		try {
			workItem = workflowEngine.getWorkflowKernel().process(workItem);
			assertEquals(2, workItem.getItemValueInteger("runs"));
			assertEquals(1000, workItem.getTaskID());

		} catch (ModelException | ProcessingErrorException | PluginException e) {
			e.printStackTrace();
			fail();
		}
	}

	/**
	 * This test calls invalid follow up event from a start event
	 * 
	 */
	@Test
	public void testInvalidEventCall() {

		ItemCollection workItem = new ItemCollection();
		workItem.model("1.0.0")
				.task(1000)
				.event(30);
		try {
			// it should not be possible to call event 30!
			workItem = workflowEngine.getWorkflowKernel().process(workItem);
			fail();
		} catch (ProcessingErrorException | PluginException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (ModelException e) {
			e.printStackTrace();
			// expected!
			assertTrue(e.getMessage().contains("1000.30 is not callable in model"));
		}
	}

}
