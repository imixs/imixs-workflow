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
import org.imixs.workflow.Model;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Before;
import org.junit.Test;

import org.junit.Assert;

/**
 * Test class for WorkflowService
 * 
 * This test verifies specific method implementations of the workflowService by
 * mocking the WorkflowService with the @spy annotation.
 * 
 * 
 * @author rsoika
 */
public class TestModelService extends WorkflowMockEnvironment {
	public static final String DEFAULT_MODEL_VERSION = "1.0.0";

	// @Spy
	// protected ModelService modelService;

	@Before
	public void setUp() throws PluginException, ModelException {

	}

	/**
	 * This test validates the getDataObject method of the modelSerivce.
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
		this.setModelPath("/bpmn/TestWorkflowService.bpmn");
		this.loadModel();

		ItemCollection event = this.getModel().getEvent(100, 20);

		Assert.assertNotNull(event);

		ModelService modelService = new ModelService();
		String data = modelService.getDataObject(event, "MyObject");

		Assert.assertNotNull(data);
		Assert.assertEquals("My data", data);

	}

	/**
	 * This deprecated model version
	 * 
	 * 
	 */
	@Test
	public void testDeprecatedModelVersion() {
		this.setModelPath("/bpmn/TestWorkflowService.bpmn");
		this.loadModel();

		// load test workitem
		ItemCollection workitem = database.get("W0000-00001");
		workitem.setModelVersion("0.9.0");
		workitem.setTaskID(100);
		workitem.setEventID(10);
		workitem.replaceItemValue("txtWorkflowGroup", "Ticket");

		Model amodel = null;
		try {
			amodel = modelService.getModelByWorkitem(workitem);
		} catch (ModelException e) {
			e.printStackTrace();
			Assert.fail();
		}

		Assert.assertNotNull(amodel);
		Assert.assertEquals("1.0.0", amodel.getVersion());

	}

	
	
	/**
	 * This deprecated model version
	 * 
	 * 
	 */
	@Test
	public void testRegexModelVersion() {
		this.setModelPath("/bpmn/TestWorkflowService.bpmn");
		this.loadModel();

		// load test workitem
		ItemCollection workitem = database.get("W0000-00001");
		workitem.setModelVersion("(^1.)");
		workitem.setTaskID(100);
		workitem.setEventID(10);
		

		Model amodel = null;
		try {
			amodel = modelService.getModelByWorkitem(workitem);
			
		} catch (ModelException e) {
			e.printStackTrace();
			Assert.fail();
		}
		Assert.assertNotNull(amodel);
		Assert.assertEquals("1.0.0", amodel.getVersion());
		
	}
	
	
	/**
	 * This deprecated model version
	 * 
	 * 
	 */
	@Test
	public void testNoMatchModelVersion() {
		this.setModelPath("/bpmn/TestWorkflowService.bpmn");
		this.loadModel();

		// load test workitem
		ItemCollection workitem = database.get("W0000-00001");
		workitem.setModelVersion("(^5.)");
		workitem.setTaskID(100);
		workitem.setEventID(10);
		workitem.replaceItemValue("$WorkflowGroup", "Invoice");
		

		Model amodel = null;
		try {
			amodel = modelService.getModelByWorkitem(workitem);
			Assert.fail();
		} catch (ModelException e) {
			// expected
			e.printStackTrace();
		}
		Assert.assertNull(amodel);
	}

}
