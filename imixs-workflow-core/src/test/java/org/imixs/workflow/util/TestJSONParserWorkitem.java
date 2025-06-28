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

package org.imixs.workflow.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.List;

import org.imixs.workflow.ItemCollection;
import org.junit.jupiter.api.Test;

/**
 * Test class test the parsing of a Imixs JSON file used by the
 * workflowRestService
 * method postWorkitemJSON(InputStream requestBodyStream)
 * 
 * 
 * @author rsoika
 */
public class TestJSONParserWorkitem {

	@Test
	public void testSimpleDeprecated() throws ParseException {

		InputStream inputStream = getClass().getResourceAsStream("/json/simple.json");

		ItemCollection itemCol = null;
		try {
			itemCol = ImixsJSONParser.parse(inputStream).get(0);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			fail();
		}

		assertNotNull(itemCol);

		assertEquals("Anna", itemCol.getItemValueString("$readaccess"));

		List<?> list = itemCol.getItemValue("txtLog");
		assertEquals(3, list.size());

		assertEquals("C", list.get(2));

		assertEquals(10, itemCol.getItemValueInteger("$ActivityID"));
	}

	/**
	 * test parsing of json number fields
	 * 
	 * e.g. {"name":"$processid", "value":{"@type":"xs:int","$":1100}},
	 * 
	 * 
	 * @throws ParseException
	 */
	@Test
	public void testSimpleNumbersDeprecated() throws ParseException {

		InputStream inputStream = getClass().getResourceAsStream("/json/simple_numbers.json");

		ItemCollection itemCol = null;
		try {
			itemCol = ImixsJSONParser.parse(inputStream).get(0);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			fail();
		}

		assertNotNull(itemCol);

		assertEquals(10, itemCol.getEventID());
		assertEquals(100, itemCol.getTaskID());
	}

	@Test()
	public void testCorrupted() throws ParseException {

		InputStream inputStream = getClass().getResourceAsStream("/json/corrupted.json");

		ItemCollection itemCol = null;
		try {
			itemCol = ImixsJSONParser.parse(inputStream).get(0);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			fail();
		}
		assertNotNull(itemCol);

		assertFalse(itemCol.hasItem("worklist"));
	}

	@Test
	public void testComplexWorkitem() throws ParseException {

		InputStream inputStream = getClass().getResourceAsStream("/json/workitem.json");

		ItemCollection itemCol = null;
		try {
			itemCol = ImixsJSONParser.parse(inputStream).get(0);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			fail();
		}

		assertNotNull(itemCol);

		assertEquals(20, itemCol.getTaskID());

		assertEquals("worklist", itemCol.getItemValueString("txtworkflowresultmessage"));
		assertEquals("true", itemCol.getItemValueString("$isAuthor"));
		assertEquals(7, itemCol.getItemValueInteger("$activityID"));

		assertEquals("14194929161-1003e42a", itemCol.getItemValueString("$UniqueID"));

		List<?> list = itemCol.getItemValue("txtworkflowpluginlog");
		assertEquals(7, list.size());

	}
}
