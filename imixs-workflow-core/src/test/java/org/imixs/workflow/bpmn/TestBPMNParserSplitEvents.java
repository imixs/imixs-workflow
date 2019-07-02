package org.imixs.workflow.bpmn;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.ModelException;
import org.junit.Test;
import org.xml.sax.SAXException;

import junit.framework.Assert;

/**
 * Test class test the Imixs BPMNParser.
 * 
 * Special case: Conditional-Events
 * 
 * @see issue #299
 * @author rsoika
 */
public class TestBPMNParserSplitEvents {

	@SuppressWarnings("unchecked")
	@Test
	public void testSimple()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		InputStream inputStream = getClass().getResourceAsStream("/bpmn/split_event1.bpmn");

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

		// Test Environment
		ItemCollection profile = model.getDefinition();
		Assert.assertNotNull(profile);

		// test count of elements
		Assert.assertEquals(3, model.findAllTasks().size());

		// test task 1000
		ItemCollection task = model.getTask(1000);
		Assert.assertNotNull(task);

		// test events for task 1000
		List<ItemCollection> events = model.findAllEventsByTask(1000);
		Assert.assertNotNull(events);
		Assert.assertEquals(2, events.size());

		// test activity 1000.10 submit
		ItemCollection activity = model.getEvent(1000, 10);
		Assert.assertNotNull(activity);
		Assert.assertEquals("split event", activity.getItemValueString("txtname"));

		Assert.assertEquals(1000, activity.getItemValueInteger("numNextProcessID"));

		// Now we need to evaluate if the Event is marked as an conditional Event with
		// the condition list copied from the gateway.
		Assert.assertTrue(activity.hasItem("keySplitConditions"));
		Map<String, String> conditions = (Map<String, String>) activity.getItemValue("keySplitConditions").get(0);
		Assert.assertNotNull(conditions);
		Assert.assertEquals("true", conditions.get("task=1100"));
	}



}