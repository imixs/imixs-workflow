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

package org.imixs.workflow.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.AdapterException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for the WorkflowService to test conditional and parallel gateways.
 * 
 * For Testcases test model files are loaded. This test verifies specific method
 * implementations of the workflowService by mocking the WorkflowService with
 * the @spy annotation.
 * 
 * 
 * @author rsoika
 */
public class TestWorkflowServiceGateways {

	protected MockWorkflowEnvironment workflowEnvironment;

	@BeforeEach
	public void setUp() throws PluginException, ModelException {
		workflowEnvironment = new MockWorkflowEnvironment();
		workflowEnvironment.setUp();
	}

	/**
	 * This test tests the conditional event gateways....
	 * 
	 * This is just a simple simulation...
	 * 
	 * @throws ProcessingErrorException
	 * @throws AccessDeniedException
	 * @throws ModelException
	 * @throws AdapterException
	 * 
	 */
	@Test
	public void testConditionalEvent1() {

		workflowEnvironment.loadBPMNModelFromFile("/bpmn/conditional_event1.bpmn");
		// load test workitem
		ItemCollection workitem = workflowEnvironment.getDocumentService().load("W0000-00001");
		try {

			workitem.replaceItemValue(WorkflowKernel.MODELVERSION, "1.0.0");

			// test _budget<100
			workitem.setTaskID(1000);
			workitem.replaceItemValue("_budget", 99);
			workitem.setEventID(10);
			workitem = workflowEnvironment.workflowService.processWorkItem(workitem);
			assertEquals(1200, workitem.getTaskID());

			// Next test _budget>100
			workitem.setTaskID(1000);
			workitem.replaceItemValue("_budget", 9999);
			workitem.setEventID(10);
			workitem = workflowEnvironment.workflowService.processWorkItem(workitem);
			assertEquals(1100, workitem.getTaskID());

		} catch (AccessDeniedException | ProcessingErrorException | PluginException | ModelException e) {
			fail(e.getMessage());
		}

		// Test without _budget item. This results into a processing error:
		try {
			workitem.removeItem("_budget");
			workitem.setTaskID(1000);
			workitem.setEventID(10);
			workitem = workflowEnvironment.workflowService.processWorkItem(workitem);
			fail(); // Exception expected!
			assertEquals("1.0.0", workitem.getItemValueString("$ModelVersion"));
			assertEquals(1000, workitem.getTaskID());
		} catch (AccessDeniedException | ProcessingErrorException | PluginException e) {
			fail(e.getMessage());
		} catch (ModelException e) {
			// Expected
			assertEquals(ModelException.INVALID_MODEL_ENTRY, e.getErrorCode());
		}

	}

	/**
	 * This test tests the conditional event gateways with a default condition....
	 * 
	 * @throws ProcessingErrorException
	 * @throws AccessDeniedException
	 * @throws ModelException
	 * @throws AdapterException
	 * 
	 */
	@Test
	public void testConditionalDefaultEvent()
			throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {

		workflowEnvironment.loadBPMNModelFromFile("/bpmn/conditional_default_event.bpmn");

		// load test workitem
		ItemCollection workitem = workflowEnvironment.getDocumentService().load("W0000-00001");
		workitem.replaceItemValue(WorkflowKernel.MODELVERSION, "1.0.0");

		// test _budget<100
		workitem.setTaskID(1000);
		workitem.replaceItemValue("_budget", 99);
		workitem.setEventID(10);
		workitem = workflowEnvironment.workflowService.processWorkItem(workitem);
		assertEquals(1200, workitem.getTaskID());

		// test _budget>100
		workitem.setTaskID(1000);
		workitem.replaceItemValue("_budget", 9999);
		workitem.setEventID(10);
		workitem = workflowEnvironment.workflowService.processWorkItem(workitem);
		assertEquals(1300, workitem.getTaskID());

		// test without any budget
		workitem.setTaskID(1000);
		workitem.removeItem("_budget");
		workitem.setEventID(10);
		workitem = workflowEnvironment.workflowService.processWorkItem(workitem);
		assertEquals(1100, workitem.getTaskID());

	}

	/**
	 * This test tests the conditional event gateways....
	 * 
	 * This is just a simple simulation...
	 * 
	 * @throws ProcessingErrorException
	 * @throws AccessDeniedException
	 * @throws ModelException
	 */
	@Test
	public void testSplitEvent1()
			throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {

		workflowEnvironment.loadBPMNModelFromFile("/bpmn/split_event1.bpmn");

		// load test workitem
		ItemCollection workitem = workflowEnvironment.getDocumentService().load("W0000-00001");
		workitem.replaceItemValue(WorkflowKernel.MODELVERSION, "1.0.0");

		// test none condition ...
		workitem.replaceItemValue("_subject", "Hello");
		workitem.setTaskID(1000);
		workitem.setEventID(10);
		workitem = workflowEnvironment.workflowService.processWorkItem(workitem);
		assertEquals("1.0.0", workitem.getItemValueString("$ModelVersion"));
		assertEquals(1100, workitem.getTaskID());

		// lookup the version.....
		List<ItemCollection> versions = new ArrayList<ItemCollection>();
		for (ItemCollection doc : workflowEnvironment.database.values()) {
			if (workitem.getUniqueID().equals(doc.getItemValueString(WorkflowKernel.UNIQUEIDSOURCE))) {
				versions.add(doc);
			}
		}
		// test new version...
		assertNotNull(versions);
		assertTrue(versions.size() == 1);
		ItemCollection version = versions.get(0);
		assertNotNull(version);

		assertEquals("Hello", version.getItemValueString("_subject"));
		assertEquals(1200, version.getTaskID());
		assertEquals(20, version.getItemValueInteger("$lastevent"));

	}

}
