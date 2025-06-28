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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.MockWorkflowContext;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;

/**
 * Test class test the Imixs BPMNParser with an Adapter definition in a Event
 * (Signal Event)
 * 
 * @author rsoika
 */
public class TestModelManagerParserAdapter {

	BPMNModel model = null;
	ModelManager modelManager = null;
	MockWorkflowContext workflowContext;

	@BeforeEach
	public void setup() {
		try {
			workflowContext = new MockWorkflowContext();
			modelManager = new ModelManager(workflowContext);
		} catch (PluginException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testEventAdapter() {
		try {
			model = BPMNModelFactory.read("/bpmn/adapter.bpmn");
			assertNotNull(model);
		} catch (BPMNModelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertNotNull(model);
		// test activity 1000.20 submit
		ItemCollection event = modelManager.findEventByID(model, 1000, 20);
		assertNotNull(event);
		assertEquals("submit", event.getItemValueString("name"));

		// test adapter class.....
		assertEquals("org.imixs.workflow.adapter.Example", event.getItemValueString("adapter.id"));

	}

	@Test
	public void testEventMulitAdapter() {
		try {
			model = BPMNModelFactory.read("/bpmn/adapter_multi.bpmn");
			assertNotNull(model);
		} catch (BPMNModelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertNotNull(model);

		// test activity 1000.20 submit
		ItemCollection event = modelManager.findEventByID(model, 1000, 20);
		assertNotNull(event);
		assertEquals("adapter A", event.getItemValueString("txtname"));
		// test adapter class.....
		assertEquals("org.imixs.workflow.adapter.Example", event.getItemValueString("adapter.id"));

		// test activity 1100.10 submit
		event = modelManager.findEventByID(model, 1100, 10);
		assertNotNull(event);
		assertEquals("adapter A", event.getItemValueString("txtname"));
		// test adapter class.....
		assertEquals("org.imixs.workflow.adapter.Example", event.getItemValueString("adapter.id"));

		// test activity 1000.20 submit
		event = modelManager.findEventByID(model, 1100, 20);
		assertNotNull(event);
		assertEquals("adapter B", event.getItemValueString("txtname"));
		// test adapter class.....
		assertEquals("com.imixs.test.AdapterB", event.getItemValueString("adapter.id"));

	}

}
