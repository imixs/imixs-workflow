package org.imixs.workflow.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.Test;

/**
 * Test some date operations
 * 
 * @author rsoika
 * 
 */
public class TestScheduler {

	@Test
	public void testDateFormating() throws ParseException {
		String input_date = "2013/06/30";
		SimpleDateFormat format1 = new SimpleDateFormat("yyyy/MM/dd");
		Date dt1 = format1.parse(input_date);

		Calendar c = Calendar.getInstance();
		c.setTime(dt1);

		// sunday = 1
		assertEquals(1, c.get(Calendar.DAY_OF_WEEK));

	}

}
