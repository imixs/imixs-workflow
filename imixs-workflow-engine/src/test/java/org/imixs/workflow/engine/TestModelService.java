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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.bpmn.BPMNUtil;
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
 * Test class for WorkflowService
 * 
 * This test verifies specific method implementations of the workflowService by
 * mocking the WorkflowService with the @spy annotation.
 * 
 * 
 * @author rsoika
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class TestModelService {

	protected MockWorkflowEnvironment workflowEnvironment;
	ItemCollection workitem;

	@BeforeEach
	public void setUp() throws PluginException, ModelException {

		workflowEnvironment = new MockWorkflowEnvironment();
		workflowEnvironment.setUp();
		workflowEnvironment.loadBPMNModelFromFile("/bpmn/TestWorkflowService.bpmn");

		workitem = workflowEnvironment.getDocumentService().load("W0000-00001");
		workitem.model("1.0.0").task(100);

	}

	/**
	 * This test validates the getDataObject method of the modelService.
	 * <p>
	 * A BPMN Task or Event element can be associated with a DataObject. The method
	 * getDataObject extracts the data object value by a given name of a associated
	 * DataObject.
	 * 
	 * @throws ModelException
	 * 
	 */
	@Test
	public void testGetDataObject() throws ModelException {
		workitem.event(20);
		BPMNModel model = this.workflowEnvironment.modelManager.getModelByWorkitem(workitem);
		ItemCollection event = this.workflowEnvironment.getModelManager().loadEvent(workitem, model);

		assertNotNull(event);

		String data = workflowEnvironment.getModelManager().findDataObject(event, "MyObject");

		assertNotNull(data);
		assertEquals("My data", data);

	}

	/**
	 * This deprecated model version
	 * 
	 * 
	 */
	@Test
	public void testDeprecatedModelVersion() {

		// load test workitem
		workitem.setModelVersion("0.9.0");
		workitem.setEventID(10);
		workitem.setWorkflowGroup("Ticket");

		String amodel = null;
		try {
			amodel = workflowEnvironment.workflowService.findModelVersionByWorkitem(workitem);
		} catch (ModelException e) {
			fail(e.getMessage());
		}

		assertNotNull(amodel);
		assertEquals("1.0.0", amodel);
	}

	/**
	 * This deprecated model version
	 * 
	 * 
	 */
	@Test
	public void testRegexModelVersion() {

		// load test workitem

		workitem.setModelVersion("(^1.)");
		workitem.setTaskID(100);
		workitem.setEventID(10);

		BPMNModel amodel = null;
		try {
			amodel = workflowEnvironment.getModelManager().getModelByWorkitem(workitem);

		} catch (ModelException e) {
			e.printStackTrace();
			fail();
		}
		assertNotNull(amodel);
		assertEquals("1.0.0", BPMNUtil.getVersion(amodel));

	}

	/**
	 * This deprecated model version
	 * 
	 * 
	 */
	@Test
	public void testNoMatchModelVersion() {
		workitem.removeItem(WorkflowKernel.MODELVERSION);
		workitem.setEventID(10);
		workitem.setWorkflowGroup("Invoice");

		BPMNModel amodel = null;
		try {
			amodel = workflowEnvironment.getModelManager().getModelByWorkitem(workitem);
			fail();
		} catch (ModelException e) {
			// expected
		}
		assertNull(amodel);
	}

}
