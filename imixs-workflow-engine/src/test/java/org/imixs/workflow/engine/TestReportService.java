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

package org.imixs.workflow.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

import javax.xml.transform.TransformerException;

import org.imixs.workflow.plugins.TestMailPlugin;
import org.imixs.workflow.xml.XSLHandler;
import org.junit.jupiter.api.Test;

/**
 * Test class for ReportService
 * 
 * This test verifies specific method implementations of the reportService.
 * 
 * @author rsoika
 */
public class TestReportService {

	private final static Logger logger = Logger.getLogger(TestMailPlugin.class.getName());

	/**
	 * Test the customNumberFormat method of the report service.
	 */
	@Test
	public void testFormatNumber() {
		ReportService reportService = new ReportService();
		assertEquals("123,456.789", reportService.customNumberFormat("###,###.###", "en_UK", 123456.789));
		assertEquals("123,456.789", reportService.customNumberFormat("###,###.###", "en_US", 123456.789));
		assertEquals("123,456.79", reportService.customNumberFormat("###,##0.00", "en_US", 123456.789));
		assertEquals("1.456,78", reportService.customNumberFormat("#,###,##0.00", "de_DE", 1456.781));
		assertEquals("1.456,78 €", reportService.customNumberFormat("#,###,##0.00 €", "de_DE", 1456.781));
		assertEquals("EUR 1.456,78", reportService.customNumberFormat("EUR #,###,##0.00", "de_DE", 1456.781));
	}

	/**
	 * This test verifies the FEATURE_SECURE_PROCESSING.
	 * This feature will set limits on XML constructs to avoid conditions such as
	 * denial of service attacks.
	 * 
	 * See discussion: https://github.com/imixs/imixs-workflow/issues/852
	 * 
	 * 
	 */
	@Test
	// @Ignore
	public void testSecureProcessing() {
		logger.info("[TestMailPlugin] getBody...");

		// prepare data
		String insecureCode = "xxxxx";
		// insecureCode = "<xml:stylesheet version=\"1.0\"
		// xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"
		// xmlns:rt=\"http://xml.apache.org/xalan/java/javax.xml.transform\"
		// xmlns:ob=\"http://xml.apache.org/xalan/java/org.apache.xalan.xsltc\"
		// xmlns:val=\"rt:getRuntime()\" xmlns:proc=\"rt:exec('$rt:load(\\'touch
		// ~/insecure_success\\')')\" xmlns:parameter=\"ob:toString('$processString')\"
		// xmlns:value-of=\"rt:eval('$value-ofString')\" />\n";

		String xmlDoc = "<document>" +
				"   <item name=\"txtname\"><value>Anna</value></item>" +
				"	<item name='insecure'>" +
				"      <value xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				"	          xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xsi:type=\"xs:string\">" + insecureCode +
				"	   </value>" +
				"   </item>" +
				"</document>";

		// create XSL template...
		String xsl = "";
		xsl = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + "<xsl:stylesheet xmlns=\"http://www.w3.org/1999/xhtml\""
				+ "	xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">"
				+ " <xsl:output method=\"html\" media-type=\"text/html\" indent=\"no\" encoding=\"ISO-8859-1\"" + " />";
		xsl += "<xsl:template match=\"/\">";
		xsl += "<html>  <body> ";
		xsl += " <h1>Welcome</h1>";
		xsl += " <h2><xsl:value-of select=\"document/item[@name='txtname']/value\" /></h2>";
		xsl += " <h3><xsl:value-of select=\"document/item[@name='insecure']/value\" /></h3>";
		xsl += "</body></html>";
		xsl += "</xsl:template></xsl:stylesheet>";

		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			logger.info("xmlDoc=" + xmlDoc);
			XSLHandler.transform(xmlDoc, xsl, "UTF-8", outputStream);
			String outputContent = outputStream.toString("UTF-8");
			logger.info("result=" + outputContent);
			assertTrue(outputContent.contains("<h2>Anna</h2>"));

		} catch (UnsupportedEncodingException | TransformerException e) {
			e.printStackTrace();
			fail();
		}

	}

}
