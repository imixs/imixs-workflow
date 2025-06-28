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

package org.imixs.workflow.jee.faces.workitem;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Date;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.faces.data.DocumentController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for the DataController
 * 
 * @author rsoika
 * 
 */
public class DataControllerTest {

	DocumentController dataController = null;

	@BeforeEach
	public void before() {
		dataController = new DocumentController();
	}

	@Test
	public void testBasic() {
		ItemCollection workitem = new ItemCollection();
		// test is new

		dataController.setDocument(workitem);
		assertTrue(dataController.isNewWorkitem());

		Date someDate = new Date();
		workitem.replaceItemValue("$Modified", someDate);
		workitem.replaceItemValue("$Created", someDate);
		dataController.setDocument(workitem);
		assertFalse(dataController.isNewWorkitem());

		someDate = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(someDate);
		cal.add(Calendar.SECOND, +1);
		workitem.replaceItemValue("$Modified", cal.getTime());
		dataController.setDocument(workitem);
		assertFalse(dataController.isNewWorkitem());

	}

}
