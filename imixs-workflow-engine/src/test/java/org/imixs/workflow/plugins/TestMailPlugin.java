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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.MockWorkflowEnvironment;
import org.imixs.workflow.engine.plugins.MailPlugin;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test the MailPlugin getter methods
 * 
 * @author rsoika
 * 
 */
public class TestMailPlugin {

	protected MailPlugin mailPlugin = null;
	private final static Logger logger = Logger.getLogger(TestMailPlugin.class.getName());

	ItemCollection workitem;
	ItemCollection event;

	protected MockWorkflowEnvironment workflowEnvironment;

	@BeforeEach
	public void setUp() throws PluginException, ModelException {

		workflowEnvironment = new MockWorkflowEnvironment();
		workflowEnvironment.setUp();
		workflowEnvironment.loadBPMNModelFromFile("/bpmn/TestOwnerPlugin.bpmn");

		mailPlugin = new MailPlugin();
		try {
			mailPlugin.init(workflowEnvironment.getWorkflowService());
		} catch (PluginException e) {
			fail(e.getMessage());
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
			fail();
		}
		assertEquals("Hello Anna!", sBody);
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
		xsl += "<html>  <body> ";
		xsl += " <h1>Welcome</h1>";
		xsl += " <h2><xsl:value-of select=\"document/item[@name='txtname']/value\" /></h2>";
		xsl += "</body></html>";
		xsl += "</xsl:template></xsl:stylesheet>";

		documentActivity.replaceItemValue("rtfMailBody", xsl);
		String sBody = null;
		try {
			sBody = mailPlugin.getBody(documentContext, documentActivity);
		} catch (PluginException e) {
			e.printStackTrace();
			fail();
		}
		logger.info(sBody);
		assertTrue(sBody.contains("<h2>Anna</h2>"));
	}
}
