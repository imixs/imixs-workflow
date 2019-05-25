package org.imixs.workflow.engine;

import org.imixs.workflow.exceptions.PluginException;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Test class for ReportService
 * 
 * This test verifies specific method implementations of the reportService.
 * 
 * @author rsoika
 */
public class TestReportService {

	@Before
	public void setup() throws PluginException {

	}

	/**
	 * Test the customNumberFormat method of the report service.
	 */
	@Test
	public void testFormatNumber() {
		ReportService reportService=new ReportService();
		Assert.assertEquals("123,456.789", reportService.customNumberFormat("###,###.###", "en_UK", 123456.789));
		Assert.assertEquals("123,456.789", reportService.customNumberFormat("###,###.###", "en_US", 123456.789));
		Assert.assertEquals("123,456.79", reportService.customNumberFormat("###,##0.00", "en_US", 123456.789));
		Assert.assertEquals("1.456,78", reportService.customNumberFormat("#,###,##0.00", "de_DE", 1456.781));
		Assert.assertEquals("1.456,78 €", reportService.customNumberFormat("#,###,##0.00 €", "de_DE", 1456.781));
		Assert.assertEquals("EUR 1.456,78", reportService.customNumberFormat("EUR #,###,##0.00", "de_DE", 1456.781));
	}

}
