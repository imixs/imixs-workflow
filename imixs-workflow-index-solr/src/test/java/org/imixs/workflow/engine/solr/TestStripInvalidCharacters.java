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

package org.imixs.workflow.engine.solr;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test striping invalid characters
 * 
 * @author rsoika
 * 
 */
public class TestStripInvalidCharacters {
	SolrIndexService solrIndexService;

	@BeforeEach
	public void setUp() throws PluginException, ModelException {
		solrIndexService = new SolrIndexService();
	}

	/**
	 * Test
	 * 
	 */
	@Test
	public void testCDATA() {

		String testString = "Hello <![CDATA[<XX>....</XX>]]> Data!";

		String result = solrIndexService.stripCDATA(testString);

		assertEquals("Hello <XX>....</XX> Data!", result);

	}

}
