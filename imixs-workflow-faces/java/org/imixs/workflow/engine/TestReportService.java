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

import org.junit.Test;

import org.junit.Assert;

/**
 * Test class for ReportService
 * 
 * This test verifies specific method implementations of the reportService.
 * 
 * @author rsoika
 */
public class TestReportService {

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
