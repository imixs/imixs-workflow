package org.imixs.workflow.bpmn;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;

import junit.framework.Assert;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.ModelException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * Test class test the Imixs BPMNParser
 * 
 * @author rsoika
 */
public class TestBPMNParser {

	@Before
	public void setup() {

	}

	@After
	public void teardown() {

	}

	// @Ignore
	@Test
	public void testSimple() throws ParseException,
			ParserConfigurationException, SAXException, IOException {

		InputStream inputStream = getClass().getResourceAsStream(
				"/bpmn/ticket.bpmn");

		BPMNModel model = null;
		try {
			model = BPMNParser.parseModel(inputStream, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ModelException e) {
			e.printStackTrace();
			Assert.fail();
		}
		Assert.assertNotNull(model);

		// test count of elements
		Assert.assertEquals(4, model.getProcessEntityList().size());

		// test task 1000
		ItemCollection task = model.getProcessEntity(1000);
		Assert.assertNotNull(task);

		// test activity for task 1000
		Collection<ItemCollection> activities = model
				.getActivityEntityList(1000);
		Assert.assertNotNull(activities);
		Assert.assertEquals(1, activities.size());

		// test activity for task 1100
		activities = model.getActivityEntityList(1100);
		Assert.assertNotNull(activities);
		Assert.assertEquals(3, activities.size());
		
		
		// test activity for task 1200
		activities = model.getActivityEntityList(1200);
		Assert.assertNotNull(activities);
		Assert.assertEquals(3, activities.size());

	}

	@Ignore
	@Test(expected = ParseException.class)
	public void testCorrupted() throws ParseException,
			ParserConfigurationException, SAXException, IOException {

		InputStream inputStream = getClass().getResourceAsStream(
				"/bpmn/corrupted.bpmn");

		BPMNModel model = null;
		try {
			model = BPMNParser.parseModel(inputStream, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ModelException e) {
			e.printStackTrace();
			Assert.fail();
		}

		Assert.assertNull(model);
	}

}
