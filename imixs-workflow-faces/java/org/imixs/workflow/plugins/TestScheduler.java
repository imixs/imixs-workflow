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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test some date operations
 * 
 * @author rsoika
 * 
 */
public class TestScheduler  {
	

	
	@Test 
	public void testDateFormating() throws ParseException { 
		String input_date="2013/06/30";
		  SimpleDateFormat format1=new SimpleDateFormat("yyyy/MM/dd");
		  Date dt1=format1.parse(input_date);
		  
		Calendar c = Calendar.getInstance();
		c.setTime(dt1);

		// sunday = 1
		Assert.assertEquals(1,c.get(Calendar.DAY_OF_WEEK));
		
		
	//	input_date="2013/07/11";
		
//		Date convertedDate = dateFormat.parse(confgEntry
//				.substring(confgEntry.indexOf('=') + 1));
//		scheduerExpression.start(convertedDate);
		
		
		
		
	}

	
}
