package org.imixs.workflow.engine.lucene;

import java.util.logging.Logger;

import org.imixs.workflow.exceptions.PluginException;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Test the WorkflowService method 'adaptText'
 * 
 * @author rsoika
 * 
 */
public class TestRandomEventUID {
	private final static Logger logger = Logger.getLogger(TestRandomEventUID.class.getName());

	/**
	 * 
	 */
	@Test
	public void testEventUID() throws PluginException {
		String result = null;

		for (int i = 0; i < 20; i++) {
			result = LuceneUpdateService.generateEventUID();
			logger.info("random event uid=" + result);
			Assert.assertTrue(result.length() >= 4);
		}

	}

}
