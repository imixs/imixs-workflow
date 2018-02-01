package org.imixs.workflow.plugins;

import java.util.logging.Logger;

import junit.framework.Assert;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.WorkflowMockEnvironment;
import org.imixs.workflow.engine.plugins.MailPlugin;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the MailPlugin getter methods
 * 
 * @author rsoika
 * 
 */
public class TestMailPlugin {

	MailPlugin mailPlugin = null;
	private final static Logger logger = Logger.getLogger(TestMailPlugin.class.getName());

	
	
	WorkflowMockEnvironment workflowMockEnvironment;

	@Before
	public void setup() throws PluginException, ModelException {
		
		workflowMockEnvironment=new WorkflowMockEnvironment();
		workflowMockEnvironment.setModelPath("/bpmn/TestAccessPlugin.bpmn");
		
		workflowMockEnvironment.setup();

		mailPlugin = new MailPlugin();
		try {
			mailPlugin.init(workflowMockEnvironment.getWorkflowService());
		} catch (PluginException e) {

			e.printStackTrace();
		}

		
	}
	

	/**
	 * Test getBody, getSubject replace dynamic values
	 * 
	 */
	@Test
	public void testItemCollection() {
		logger.info("[TestMailPlugin] getBody...");

		// prepare data
		ItemCollection documentContext = new ItemCollection();

		ItemCollection documentActivity = new ItemCollection();

		documentActivity.replaceItemValue("rtfMailBody", "Hello <itemValue>namcreator</itemValue>!");
		documentContext.replaceItemValue("namcreator", "Anna");

		String sBody = null;
		try {
			sBody = mailPlugin.getBody(documentContext, documentActivity);
		} catch (PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}

		Assert.assertEquals("Hello Anna!", sBody);
	}

	/**
	 * Test xsl transformation
	 * 
	 */
	@Test
	public void testXSLTransformation() {
		logger.info("[TestMailPlugin] getBody...");

		// prepare data
		ItemCollection documentContext = new ItemCollection();

		documentContext.replaceItemValue("txtname", "Anna");
		documentContext.replaceItemValue("_subject", "This is an example");
		documentContext.replaceItemValue("htmlDescription", "<strong>Important text message</strong>");

		ItemCollection documentActivity = new ItemCollection();

		// create XSL template...
		String xsl = "";
		xsl = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + "<xsl:stylesheet xmlns=\"http://www.w3.org/1999/xhtml\""
				+ "	xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">"
				+ " <xsl:output method=\"html\" media-type=\"text/html\" indent=\"no\" encoding=\"ISO-8859-1\"" + " />";
		xsl += "<xsl:template match=\"/\">";

		xsl +="<html>  <body> ";
		xsl +=" <h1>Welcome</h1>";
		
		xsl +=" <h2><xsl:value-of select=\"document/item[@name='txtname']/value\" /></h2>";
		
		xsl +="</body></html>";
		
		xsl +="</xsl:template></xsl:stylesheet>";

		
		documentActivity.replaceItemValue("rtfMailBody", xsl);

		String sBody = null;
		try {
			sBody = mailPlugin.getBody(documentContext, documentActivity);
		} catch (PluginException e) {
			e.printStackTrace();
			Assert.fail();
		}

		logger.info(sBody);
		Assert.assertTrue( sBody.contains("<h2>Anna</h2>"));
	}

}
