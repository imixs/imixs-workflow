package org.imixs.workflow.bpmn;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Model;
import org.imixs.workflow.exceptions.ModelException;
import org.junit.Test;
import org.xml.sax.SAXException;

import junit.framework.Assert;

/**
 * Test class test the Imixs BPMNParser
 * 
 * Special cases with collaboration diagrams
 * 
 * @author rsoika
 */
public class TestBPMNParserCollaborationMessageFlow {

	@Test
	public void testSimple()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		String VERSION = "1.0.0";

		InputStream inputStream = getClass().getResourceAsStream("/bpmn/collaboration_messageflow.bpmn");

		Model model = null;
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
		Assert.assertEquals(2, model.findAllTasks().size());

		// test task 1000
		ItemCollection task = model.getTask(1000);
		Assert.assertNotNull(task);
		Assert.assertEquals(VERSION, task.getItemValueString("$ModelVersion"));
		Assert.assertEquals("WorkflowGroup1", task.getItemValueString("txtworkflowgroup"));

		// test event for task 1000
		Collection<ItemCollection> activities = model.findAllEventsByTask(1000);
		Assert.assertNotNull(activities);
		Assert.assertEquals(1, activities.size());

		// test activity 1000.10 submit
		ItemCollection activity = model.getEvent(1000, 10);
		Assert.assertNotNull(activity);
		Assert.assertEquals("submit", activity.getItemValueString("txtname"));
		Assert.assertEquals(1000, activity.getItemValueInteger("numNextProcessID"));

	}

	@Test
	public void testComplex()
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		String VERSION = "1.0.0";

		InputStream inputStream = getClass().getResourceAsStream("/bpmn/collaboration_messageflow_complex.bpmn");

		Model model = null;
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

		// test task 1000
		ItemCollection task = model.getTask(1000);
		Assert.assertNotNull(task);
		Assert.assertEquals(VERSION, task.getItemValueString("$ModelVersion"));
		Assert.assertEquals("Sales", task.getItemValueString("txtworkflowgroup"));

		// test event for task 1000
		Collection<ItemCollection> activities = model.findAllEventsByTask(1000);
		Assert.assertNotNull(activities);
		Assert.assertEquals(2, activities.size());

		// test activity 1000.10 submit
		ItemCollection activity = model.getEvent(1000, 20);
		Assert.assertNotNull(activity);
		Assert.assertEquals("submit", activity.getItemValueString("txtname"));
		Assert.assertEquals(1010, activity.getItemValueInteger("numNextProcessID"));

		// test task 2000
		task = model.getTask(2000);
		Assert.assertNotNull(task);
		Assert.assertEquals(VERSION, task.getItemValueString("$ModelVersion"));
		Assert.assertEquals("Order", task.getItemValueString("txtworkflowgroup"));

		// test event for task 2000
		activities = model.findAllEventsByTask(2000);
		Assert.assertNotNull(activities);
		Assert.assertEquals(3, activities.size());

	}

}
