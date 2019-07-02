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
