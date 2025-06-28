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
import static org.junit.jupiter.api.Assertions.fail;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.MockWorkflowContext;
import org.imixs.workflow.bpmn.BPMNUtil;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openbpmn.bpmn.BPMNModel;

/**
 * Test class for testing the eval method of the workflow kernel
 * The method loads a test model
 * 
 * @author rsoika
 */
public class TestWorkflowKernelEval {

	private final static Logger logger = Logger.getLogger(TestWorkflowKernelEval.class.getName());

	MockWorkflowContext workflowEngine;

	@BeforeEach
	public void setup() {
		try {
			workflowEngine = new MockWorkflowContext();
			workflowEngine.loadBPMNModelFromFile("/bpmn/workflowkernel_eval.bpmn");
			BPMNModel model = workflowEngine.fetchModel("1.0.0");
			assertNotNull(model);
		} catch (PluginException | ModelException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testRuleMatch() {
		long l = System.currentTimeMillis();
		ItemCollection workitem = new ItemCollection();
		workitem.model("1.0.0").task(100).event(10);
		workitem.setItemValue("a", 1);
		workitem.setItemValue("b", "DE");
		try {
			ItemCollection targetTask = workflowEngine.getWorkflowKernel().eval(workitem);
			assertNotNull(targetTask);
			assertEquals("Match", targetTask.getItemValueString("name"));
			logger.log(Level.INFO, "evaluate BPMN Target Task in {0}ms", System.currentTimeMillis() - l);
			// We also expect that the workitem taskID has not changed!
			assertEquals(100, workitem.getTaskID());
		} catch (ModelException | PluginException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testRuleNoMatch() {
		long l = System.currentTimeMillis();
		ItemCollection workitem = new ItemCollection();
		workitem.model("1.0.0").task(100).event(10);
		workitem.setItemValue("a", 1);
		workitem.setItemValue("b", "I");

		try {
			ItemCollection targetTask = workflowEngine.getWorkflowKernel().eval(workitem);
			assertNotNull(targetTask);
			assertEquals("No Match", targetTask.getItemValueString("name"));
			logger.log(Level.INFO, "evaluate BPMN-Rule in {0}ms", System.currentTimeMillis() - l);
			// We also expect that the workitem taskID has not changed!
			assertEquals(100, workitem.getTaskID());
		} catch (ModelException | PluginException e) {
			e.printStackTrace();
			fail();
		}
	}

	/**
	 * In this model we have a parallel gateway. But the flow should only eval the
	 * outcome with the condition 'true'
	 * 
	 */
	@Test
	public void testParallelGateway() {
		workflowEngine.loadBPMNModelFromFile("/bpmn/workflowkernel_eval_parallelgateway.bpmn");

		long l = System.currentTimeMillis();
		ItemCollection workitem = new ItemCollection();
		workitem.model("1.0.0").task(100).event(20);

		try {
			ItemCollection targetTask = workflowEngine.getWorkflowKernel().eval(workitem);
			assertNotNull(targetTask);
			assertEquals("Task 2", targetTask.getItemValueString("name"));
			assertEquals(200, targetTask.getItemValueInteger(BPMNUtil.TASK_ITEM_TASKID));
		} catch (ModelException | PluginException e) {
			e.printStackTrace();
			fail();
		}
	}

}
