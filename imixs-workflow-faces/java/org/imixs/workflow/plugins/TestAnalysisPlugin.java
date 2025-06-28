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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.plugins.AnalysisPlugin;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Before;
import org.junit.Test;

import org.junit.Assert;

/**
 * Test class for AnalysisPlugin
 * 
 * @author rsoika
 */
public class TestAnalysisPlugin {
	protected AnalysisPlugin analysisPlugin = null;
	private static final Logger logger = Logger.getLogger(TestAnalysisPlugin.class.getName());

	@Before
	public void setUp() throws PluginException {
		analysisPlugin = new AnalysisPlugin();
		analysisPlugin.init(null);
	}


	
	
	
	
	/**
	 * Verify the start mechanism
	 * @throws PluginException
	 */
	@Test
	public void testBasicTest() throws PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("txtName", "Anna");
		
		// Activity Entity Dummy
		ItemCollection adocumentActivity = new ItemCollection();

		String sResult = "<item name='measurepoint' type='start'>M1</item>";
		logger.log(Level.INFO, "txtActivityResult={0}", sResult);
		adocumentActivity.replaceItemValue("txtActivityResult", sResult);

		adocumentContext = analysisPlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		Assert.assertTrue(adocumentContext.hasItem("datMeasurePointStart_M1"));
		
		logger.log(Level.INFO, "datMeasurePointStart_M1= {0}", adocumentContext.getItemValueDate("datMeasurePointStart_M1"));
		

	}




	/**
	 * Verify the start mechanism
	 * @throws PluginException
	 */
	@Test
	public void testWrongStartTest() throws PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("txtName", "Anna");
		
		// Activity Entity Dummy
		ItemCollection adocumentActivity = new ItemCollection();

		String sResult = "<item name='measurepoint' type='start'>M1</item>";
		logger.log(Level.INFO, "txtActivityResult={0}", sResult);
		adocumentActivity.replaceItemValue("txtActivityResult", sResult);

		adocumentContext = analysisPlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		Assert.assertTrue(adocumentContext.hasItem("datMeasurePointStart_M1"));
		
		logger.log(Level.INFO, "datMeasurePointStart_M1= {0}", adocumentContext.getItemValueDate("datMeasurePointStart_M1"));
		

		
		
		sResult = "<item name='measurepoint' type='start'>M1</item>";
		logger.log(Level.INFO, "txtActivityResult={0}", sResult);
		adocumentActivity.replaceItemValue("txtActivityResult", sResult);

		adocumentContext = analysisPlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		Assert.assertTrue(adocumentContext.hasItem("datMeasurePointStart_M1"));
	}


	
	
	
	


	/**
	 * Verify the start  numMeasurePoint_
	 * 
	 * @throws PluginException
	 */
	@Test
	public void testTotalTime() throws PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("txtName", "Anna");
		
		// Activity Entity Dummy
		ItemCollection adocumentActivity = new ItemCollection();

		String sResult = "<item name='measurepoint' type='start'>M1</item>";
		logger.log(Level.INFO, "txtActivityResult={0}", sResult);
		adocumentActivity.replaceItemValue("txtActivityResult", sResult);

		adocumentContext = analysisPlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		Assert.assertTrue(adocumentContext.hasItem("datMeasurePointStart_M1"));
		
		logger.log(Level.INFO, "datMeasurePointStart_M1= {0}", adocumentContext.getItemValueDate("datMeasurePointStart_M1"));
		

		
		
		try {
		    Thread.sleep(1000);                 //1000 milliseconds is one second.
		} catch(InterruptedException ex) {
		    Thread.currentThread().interrupt();
		}
		
		
		sResult = "<item name='measurepoint' type='stop'>M1</item>";
		logger.log(Level.INFO, "txtActivityResult={0}", sResult);
		adocumentActivity.replaceItemValue("txtActivityResult", sResult);

		adocumentContext = analysisPlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		Assert.assertTrue(adocumentContext.hasItem("datMeasurePointStart_M1"));
		
		
		int time=adocumentContext.getItemValueInteger("numMeasurePoint_M1");
		
		System.out.println("Time=" + time);
		Assert.assertTrue(time>0);
	}


	
	


}
