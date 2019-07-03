package org.imixs.workflow.plugins;

import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.plugins.AnalysisPlugin;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Test class for AnalysisPlugin
 * 
 * @author rsoika
 */
public class TestAnalysisPlugin {
	protected AnalysisPlugin analysisPlugin = null;
	private static Logger logger = Logger.getLogger(TestAnalysisPlugin.class.getName());

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
		logger.info("txtActivityResult=" + sResult);
		adocumentActivity.replaceItemValue("txtActivityResult", sResult);

		adocumentContext = analysisPlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		Assert.assertTrue(adocumentContext.hasItem("datMeasurePointStart_M1"));
		
		logger.info("datMeasurePointStart_M1= " + adocumentContext.getItemValueDate("datMeasurePointStart_M1"));
		

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
		logger.info("txtActivityResult=" + sResult);
		adocumentActivity.replaceItemValue("txtActivityResult", sResult);

		adocumentContext = analysisPlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		Assert.assertTrue(adocumentContext.hasItem("datMeasurePointStart_M1"));
		
		logger.info("datMeasurePointStart_M1= " + adocumentContext.getItemValueDate("datMeasurePointStart_M1"));
		

		
		
		sResult = "<item name='measurepoint' type='start'>M1</item>";
		logger.info("txtActivityResult=" + sResult);
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
		logger.info("txtActivityResult=" + sResult);
		adocumentActivity.replaceItemValue("txtActivityResult", sResult);

		adocumentContext = analysisPlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		Assert.assertTrue(adocumentContext.hasItem("datMeasurePointStart_M1"));
		
		logger.info("datMeasurePointStart_M1= " + adocumentContext.getItemValueDate("datMeasurePointStart_M1"));
		

		
		
		try {
		    Thread.sleep(1000);                 //1000 milliseconds is one second.
		} catch(InterruptedException ex) {
		    Thread.currentThread().interrupt();
		}
		
		
		sResult = "<item name='measurepoint' type='stop'>M1</item>";
		logger.info("txtActivityResult=" + sResult);
		adocumentActivity.replaceItemValue("txtActivityResult", sResult);

		adocumentContext = analysisPlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		Assert.assertTrue(adocumentContext.hasItem("datMeasurePointStart_M1"));
		
		
		int time=adocumentContext.getItemValueInteger("numMeasurePoint_M1");
		
		System.out.println("Time=" + time);
		Assert.assertTrue(time>0);
	}


	
	


}
