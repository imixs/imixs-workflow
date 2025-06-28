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

import java.util.Set;

import org.imixs.workflow.MockWorkflowContext;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openbpmn.bpmn.BPMNModel;

/**
 * Test class to test the behavior of the ModelManager.
 * 
 * Special cases with collaboration diagram containing two workflow groups
 * (participants) with different workflow models.
 * 
 * 
 * @author rsoika
 */
public class TestBPMNParserCollaborationMinutes {

	BPMNModel model = null;
	ModelManager modelManager = null;
	MockWorkflowContext workflowContext;

	@BeforeEach
	public void setup() {
		try {
			workflowContext = new MockWorkflowContext();
			modelManager = new ModelManager(workflowContext);
			workflowContext.loadBPMNModelFromFile("/bpmn/minutes.bpmn");
			model = workflowContext.fetchModel("1.0.0");
			assertNotNull(model);

		} catch (ModelException | PluginException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testSimple() throws ModelException {

		Set<String> groups = modelManager.findAllGroupsByModel(model);
		// Test Groups
		assertFalse(groups.contains("Collaboration"));
		assertTrue(groups.contains("Protokoll"));
		assertTrue(groups.contains("Protokollpunkt"));

		// test count of elements
		assertEquals(8, model.findAllActivities().size());
	}

}
