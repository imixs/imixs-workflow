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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.text.ParseException;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.MockWorkflowContext;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openbpmn.bpmn.BPMNModel;
import org.xml.sax.SAXException;

/**
 * Test class test the Imixs BPMNParser
 * 
 * Special cases with collaboration diagrams
 * 
 * @author rsoika
 */
public class TestModelManagerCollaboration {
	BPMNModel model = null;
	ModelManager modelManager = null;
	MockWorkflowContext workflowContext;

	@BeforeEach
	public void setup() {
		try {
			workflowContext = new MockWorkflowContext();
			modelManager = new ModelManager(workflowContext);
			workflowContext.loadBPMNModelFromFile("/bpmn/collaboration.bpmn");
			model = workflowContext.fetchModel("1.0.0");
			assertNotNull(model);

		} catch (ModelException | PluginException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testSimple()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		// Test Environment
		ItemCollection profile = modelManager.loadDefinition(model);
		assertNotNull(profile);
		assertEquals("environment.profile", profile.getItemValueString("txtname"));
		assertEquals("WorkflowEnvironmentEntity", profile.getItemValueString("type"));
		assertEquals("1.0.0", profile.getItemValueString("$ModelVersion"));

		Set<String> groups = modelManager.findAllGroupsByModel(model);
		// List<String> groups = model.getGroups();
		// Test Groups
		assertFalse(groups.contains("Collaboration"));
		assertTrue(groups.contains("WorkflowGroup1"));
		assertTrue(groups.contains("WorkflowGroup2"));

		// test count of elements
		// assertEquals(2, model.findAllTasks().size());

		// test task 1000
		ItemCollection task = modelManager.findTaskByID(model, 1000);
		assertNotNull(task);

		// test activity 1000.10 submit
		ItemCollection activity = modelManager.findEventByID(model, 1000, 10);
		assertNotNull(activity);
		assertEquals("submit", activity.getItemValueString("txtname"));

		// test task 1100
		task = modelManager.findTaskByID(model, 2000);
		assertNotNull(task);

	}

}
