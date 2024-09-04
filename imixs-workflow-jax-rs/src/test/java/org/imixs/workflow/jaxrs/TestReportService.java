package org.imixs.workflow.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.imixs.workflow.engine.ReportService;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for ResportService
 * 
 * @author rsoika
 * 
 */
public class TestReportService {

	protected ReportService reportService = null;

	@BeforeEach
	public void setUp() throws PluginException {
		reportService = new ReportService();
	}

	/**
	 * test computeDynammicDate
	 */
	@Test
	public void testcomputeDynamicDateFirstDayOfMonth() {

		String test = "<date DAY_OF_MONTH=\"1\" MONTH=\"2\" />";

		// construct expected test date....
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MONTH, Calendar.FEBRUARY);
		cal.set(Calendar.DAY_OF_MONTH, 1);

		System.out.println("Test=" + cal.getTime());

		// parse string and compute result

		Calendar result = reportService.computeDynamicDate(test);

		System.out.println("Result=" + result.getTime());

		// compare result with test data
		assertEquals(cal.get(Calendar.MONTH), result.get(Calendar.MONTH));
		assertEquals(cal.get(Calendar.DAY_OF_MONTH), result.get(Calendar.DAY_OF_MONTH));
		assertEquals(cal.get(Calendar.YEAR), result.get(Calendar.YEAR));

	}

	@Test
	public void testcomputeDynamicDateLastDayOfMonth() {

		String test = "<date DAY_OF_MONTH=\"ACTUAL_MAXIMUM\" MONTH=\"12\" />";

		// construct expected test date....
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MONTH, Calendar.DECEMBER);
		cal.set(Calendar.DAY_OF_MONTH, 31);

		System.out.println("Test=" + cal.getTime());

		// parse string and compute result
		Calendar result = reportService.computeDynamicDate(test);

		System.out.println("Result=" + result.getTime());

		// compare result with test data
		assertEquals(cal.get(Calendar.MONTH), result.get(Calendar.MONTH));
		assertEquals(cal.get(Calendar.DAY_OF_MONTH), result.get(Calendar.DAY_OF_MONTH));
		assertEquals(cal.get(Calendar.YEAR), result.get(Calendar.YEAR));

	}

	@Test
	public void testcomputeDynamicDateMoveMonth() {

		String test = "<date DAY_OF_MONTH=\"ACTUAL_MAXIMUM\" MONTH=\"12\" ADD=\"MONTH,-1\" />";

		// construct expected test date....
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MONTH, Calendar.DECEMBER);
		cal.set(Calendar.DAY_OF_MONTH, 31);
		cal.add(Calendar.MONTH, -1);

		System.out.println("Test=" + cal.getTime());

		// parse string and compute result
		Calendar result = reportService.computeDynamicDate(test);

		System.out.println("Result=" + result.getTime());

		// compare result with test data
		assertEquals(cal.get(Calendar.MONTH), result.get(Calendar.MONTH));
		assertEquals(cal.get(Calendar.DAY_OF_MONTH), result.get(Calendar.DAY_OF_MONTH));
		assertEquals(cal.get(Calendar.YEAR), result.get(Calendar.YEAR));

		// test last month of year...

		test = "<date DAY_OF_MONTH=\"ACTUAL_MAXIMUM\" MONTH=\"ACTUAL_MAXIMUM\" ADD=\"MONTH,-1\"  />";

		// construct expected test date....
		cal = Calendar.getInstance();
		cal.set(Calendar.MONTH, Calendar.DECEMBER);
		cal.set(Calendar.DAY_OF_MONTH, 31);
		cal.add(Calendar.MONTH, -1);

		System.out.println("Test=" + cal.getTime());

		// parse string and compute result
		result = reportService.computeDynamicDate(test);

		System.out.println("Result=" + result.getTime());

		// compare result with test data
		assertEquals(cal.get(Calendar.MONTH), result.get(Calendar.MONTH));
		assertEquals(cal.get(Calendar.DAY_OF_MONTH), result.get(Calendar.DAY_OF_MONTH));
		assertEquals(cal.get(Calendar.YEAR), result.get(Calendar.YEAR));

	}

	@Test
	public void testcomputeDynamicDateAbsolutDate() {

		String test = "<date DAY_OF_MONTH=\"7\" MONTH=\"8\" YEAR=\"1969\" />";

		// construct expected test date....
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MONTH, Calendar.AUGUST);
		cal.set(Calendar.DAY_OF_MONTH, 7);
		cal.set(Calendar.YEAR, 1969);

		System.out.println("Test=" + cal.getTime());

		// parse string and compute result
		Calendar result = reportService.computeDynamicDate(test);

		System.out.println("Result=" + result.getTime());

		// compare result with test data
		assertEquals(cal.get(Calendar.MONTH), result.get(Calendar.MONTH));
		assertEquals(cal.get(Calendar.DAY_OF_MONTH), result.get(Calendar.DAY_OF_MONTH));
		assertEquals(cal.get(Calendar.YEAR), result.get(Calendar.YEAR));

		assertEquals(Calendar.THURSDAY, cal.get(Calendar.DAY_OF_WEEK));

	}

	/**
	 * Test the replace date function
	 */
	@Test
	public void testReplaceDateString() {

		String test = "(type:\"workitem\" OR type:\"workitemarchive\") AND (txtworkflowgroup:\"Rechnungsausgang\")"
				+ " AND ($created:[<date DAY_OF_MONTH=\"1\" ADD=\"MONTH,-6\" /> TO <date DAY_OF_MONTH=\"ACTUAL_MAXIMUM\" />])";

		// construct expected test date....
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.set(Calendar.DAY_OF_MONTH, 1);
		cal.add(Calendar.MONTH, -6);

		DateFormat f = new SimpleDateFormat("yyyyMMdd");
		String exprectedDateResult = f.format(cal.getTime());
		;
		System.out.println("expected test-date =" + exprectedDateResult);

		// parse string and compute result
		String result = reportService.replaceDateString(test);

		System.out.println("result=" + result);

		// compare result with test data
		assertTrue(result.contains("$created:[" + exprectedDateResult + " TO "));

	}

}
