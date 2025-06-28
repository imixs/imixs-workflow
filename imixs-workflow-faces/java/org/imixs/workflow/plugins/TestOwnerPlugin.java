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

package org.imixs.workflow.plugins;

import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.WorkflowMockEnvironment;
import org.imixs.workflow.engine.plugins.OwnerPlugin;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Before;
import org.junit.Test;

import org.junit.Assert;

/**
 * Test the ACL plugin.
 * 
 * Also test the fallback mode
 * 
 * @author rsoika
 * 
 */
public class TestOwnerPlugin  {

	private final static Logger logger = Logger.getLogger(TestOwnerPlugin.class.getName());

	OwnerPlugin ownerPlugin = null;
	ItemCollection documentContext;
	ItemCollection documentActivity;

	WorkflowMockEnvironment workflowMockEnvironment;

	@Before
	public void setup() throws PluginException, ModelException {

		workflowMockEnvironment=new WorkflowMockEnvironment();
		workflowMockEnvironment.setModelPath("/bpmn/TestOwnerPlugin.bpmn");
		workflowMockEnvironment.setup();
		
		ownerPlugin = new OwnerPlugin();
		try {
			ownerPlugin.init(workflowMockEnvironment.getWorkflowService());
		} catch (PluginException e) {

			e.printStackTrace();
		}

		// prepare data
		documentContext = new ItemCollection().model( WorkflowMockEnvironment.DEFAULT_MODEL_VERSION).task(100).event(10);
		logger.info("[TestOwnerPlugin] setup test data...");
		Vector<String> list = new Vector<String>();
		list.add("manfred");
		list.add("anna");
		documentContext.replaceItemValue("namTeam", list);
		documentContext.replaceItemValue("namCreator", "ronny");
	}

	@SuppressWarnings({ "rawtypes" })
	@Test
	public void simpleTest() {

		documentActivity = new ItemCollection();
		documentActivity.replaceItemValue("keyupdateAcl", true);
		documentActivity.replaceItemValue("numNextProcessID", 100);
		
		

		Vector<String> list = new Vector<String>();
		list.add("sam");
		list.add("joe");
		documentActivity.replaceItemValue("namOwnershipNames", list);

		try {
			ownerPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		List writeAccess = documentContext.getItemValue(OwnerPlugin.OWNER);

		Assert.assertEquals(2, writeAccess.size());
		Assert.assertTrue(writeAccess.contains("joe"));
		Assert.assertTrue(writeAccess.contains("sam"));
	}

	/**
	 * Test if the current value of namowner can be set as the new value
	 */
	@SuppressWarnings({ "rawtypes" })
	@Test
	public void testUpdateOfnamOwner() {

		documentActivity = new ItemCollection();
		documentActivity.replaceItemValue("keyupdateAcl", true);
		documentActivity.replaceItemValue("numNextProcessID", 100);
		
		Vector<String> list = new Vector<String>();
		list.add("sam");
		list.add("joe");
		documentActivity.replaceItemValue("namOwnershipNames", list);

		// set a current owner
		documentContext.replaceItemValue(OwnerPlugin.OWNER, "ralph");
		documentActivity.replaceItemValue("keyOwnershipFields", OwnerPlugin.OWNER);

		try {
			ownerPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}

		List ownerList = documentContext.getItemValue(OwnerPlugin.OWNER);

		Assert.assertEquals(3, ownerList.size());
		Assert.assertTrue(ownerList.contains("joe"));
		Assert.assertTrue(ownerList.contains("sam"));
		Assert.assertTrue(ownerList.contains("ralph"));
	}

	/**
	 * This test verifies if a list of users provided by the fieldMapping is
	 * mapped correctly into the workItem
	 * @throws ModelException 
	 */
	@SuppressWarnings({ "rawtypes" })
	@Test
	public void staticUserGroupMappingTest() throws ModelException {

		documentActivity = workflowMockEnvironment.getModel().getEvent(100, 10);
		documentActivity.replaceItemValue("keyupdateAcl", true);
		documentActivity.replaceItemValue("keyOwnershipFields", "[sam, tom,  anna ,]"); // 3
																						// values
																						// expected!
		//this.setActivityEntity(documentActivity);
		try {
			ownerPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		List writeAccess = documentContext.getItemValue(OwnerPlugin.OWNER);

		Assert.assertEquals(3, writeAccess.size());
		Assert.assertTrue(writeAccess.contains("tom"));
		Assert.assertTrue(writeAccess.contains("sam"));
		Assert.assertTrue(writeAccess.contains("anna"));
	}

	@SuppressWarnings({ "rawtypes" })
	@Test
	public void testNoUpdate() throws ModelException {

		documentActivity = workflowMockEnvironment.getModel().getEvent(100, 20);
		
		try {
			ownerPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		List writeAccess = documentContext.getItemValue(OwnerPlugin.OWNER);

		Assert.assertEquals(0, writeAccess.size());
	}

}
