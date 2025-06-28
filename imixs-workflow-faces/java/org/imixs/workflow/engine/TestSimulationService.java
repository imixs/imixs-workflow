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

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.junit.Test;

import org.junit.Assert;

/**
 * Test class for the SimulationService
 * 
 * This test simulates the processing life cycle of a workitem using the
 * SimulationService.
 * 
 * @author rsoika
 */
public class TestSimulationService {

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
	public void testConditionalEvent1()
			throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {

		WorkflowSimulationEnvironment wse = new WorkflowSimulationEnvironment();
		wse.setUp();
		wse.loadModel("/bpmn/conditional_event1.bpmn");

		// load test workitem
		ItemCollection workitem = new ItemCollection();
		workitem.replaceItemValue(WorkflowKernel.MODELVERSION, WorkflowSimulationEnvironment.DEFAULT_MODEL_VERSION);

		// test _budget<100
		workitem.setTaskID(1000);
		workitem.replaceItemValue("_budget", 99);
		workitem.setEventID(10);
		workitem = wse.simulationService.processWorkItem(workitem, null);
		Assert.assertEquals(1200, workitem.getTaskID());

		// test _budget>100
		workitem.setTaskID(1000);
		workitem.replaceItemValue("_budget", 9999);
		workitem.setEventID(10);
		workitem = wse.simulationService.processWorkItem(workitem, null);
		Assert.assertEquals(1100, workitem.getTaskID());

	}

}
