package org.imixs.workflow.bpmn;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import junit.framework.Assert;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.ModelException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * Test class test the Imixs BPMNParser
 * 
 * @author rsoika
 */
public class TestBPMNParserStartEvent {

	
	@Before
	public void setup() {

	}

	@After
	public void teardown() {

	}

	@Test
	public void testSimple() throws ParseException,
			ParserConfigurationException, SAXException, IOException {

		String VERSION="1.0.0";
		
		InputStream inputStream = getClass().getResourceAsStream(
				"/bpmn/startevent.bpmn");

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
		ItemCollection profile = model.getProfile();
		Assert.assertNotNull(profile);
		Assert.assertEquals("environment.profile",
				profile.getItemValueString("txtname"));
		Assert.assertEquals("WorkflowEnvironmentEntity",
				profile.getItemValueString("type"));
		Assert.assertEquals(VERSION,
				profile.getItemValueString("$ModelVersion"));
		
		Assert.assertTrue(model.workflowGroups.contains("Simple"));
		
		// test count of elements
		Assert.assertEquals(1, model.getProcessEntityList(VERSION).size());

		// test task 1000
		ItemCollection task = model.getProcessEntity(1000,VERSION);
		Assert.assertNotNull(task);
		Assert.assertEquals("1.0.0",
				task.getItemValueString("$ModelVersion"));
		Assert.assertEquals("Simple",
				task.getItemValueString("txtworkflowgroup"));
		
		
		
		
		// test activity 1000.10 save
		ItemCollection activity = model.getActivityEntity(1000, 10,VERSION);
		Assert.assertNotNull(activity);
		Assert.assertEquals("save",
				activity.getItemValueString("txtname"));
		Assert.assertEquals(1000,
				activity.getItemValueInteger("numNextProcessID"));
		
		// test activity for task 1000
		List<ItemCollection> activities = model.getActivityEntityList(1000,VERSION);
		Assert.assertNotNull(activities);
		Assert.assertEquals(2, activities.size());
	
		
		// test import activity for task 1000.20
		 activity = model.getActivityEntity(1000, 20,VERSION);
		Assert.assertNotNull(activity);
		Assert.assertEquals("import",
				activity.getItemValueString("txtname"));
		Assert.assertEquals(1000,
				activity.getItemValueInteger("numNextProcessID"));
	}

}
