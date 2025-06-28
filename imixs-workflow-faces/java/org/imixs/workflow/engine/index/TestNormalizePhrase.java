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

package org.imixs.workflow.engine.index;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Before;
import org.junit.Test;

import org.junit.Assert;

/**
 * Test normalization of a search prase
 * 
 * @author rsoika
 * 
 */
public class TestNormalizePhrase {

	SchemaService schemaService = null;
	private static final Logger logger = Logger.getLogger(TestNormalizePhrase.class.getName());

	@Before
	public void setUp() throws PluginException, ModelException {

		schemaService = new SchemaService();

	}

	/**
	 * Test "Lukas Podolski"
	 */
	@Test
	public void test1() throws PluginException {
		String searchTerm = "Lukas Podolski";
		String result = null;
		
			result = schemaService.normalizeSearchTerm(searchTerm);
			logger.log(Level.INFO, "{0}  -->  {1}", new Object[]{searchTerm, result});
	

		Assert.assertEquals("lukas podolski", result);

	}

	
	
	
	/**
	 * Test "Europe/Berlin"
	 */
	@Test
	public void test2() throws PluginException {
		String searchTerm = "Europe/Berlin";
		String result = null;
		
			result = schemaService.normalizeSearchTerm(searchTerm);
			logger.log(Level.INFO, "{0}  -->  {1}", new Object[]{searchTerm, result});
		

		Assert.assertEquals("europe berlin", result);

	}

	
	/**
	 * Test "rs/82550/201618"
	 */
	@Test
	public void test3() throws PluginException {
		String searchTerm = "rs/82550/201618";
		String result = null;
		
			result = schemaService.normalizeSearchTerm(searchTerm);
			logger.log(Level.INFO, "{0}  -->  {1}", new Object[]{searchTerm, result});
		

		Assert.assertEquals("rs\\/82550\\/201618", result);

	}
	
	
	/**
	 * Test "rs-82550/201618"
	 */
	@Test
	public void test4() throws PluginException {
		String searchTerm = "rs-82550/201618";
		String result = null;
		
			result = schemaService.normalizeSearchTerm(searchTerm);
			logger.log(Level.INFO, "{0}  -->  {1}", new Object[]{searchTerm, result});
		
		Assert.assertEquals("rs\\-82550\\/201618", result);

	}

	
	
	
	
}
