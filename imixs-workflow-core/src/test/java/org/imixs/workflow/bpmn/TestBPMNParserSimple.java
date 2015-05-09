package org.imixs.workflow.bpmn;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Collection;
import java.util.List;

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
public class TestBPMNParserSimple {

	
	@Before
	public void setup() {

	}

	@After
	public void teardown() {

	}

	// @Ignore
	@SuppressWarnings("rawtypes")
	@Test
	public void testSimple() throws ParseException,
			ParserConfigurationException, SAXException, IOException {

		String VERSION="1.0.0";
		
		InputStream inputStream = getClass().getResourceAsStream(
				"/bpmn/simple.bpmn");

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
		Assert.assertEquals(2, model.getProcessEntityList(VERSION).size());

		// test task 1000
		ItemCollection task = model.getProcessEntity(1000,VERSION);
		Assert.assertNotNull(task);
		Assert.assertEquals("1.0.0",
				task.getItemValueString("$ModelVersion"));
		Assert.assertEquals("Simple",
				task.getItemValueString("txtworkflowgroup"));
		
		
		
		
		// test activity 1000.10 submit
		ItemCollection activity = model.getActivityEntity(1000, 10,VERSION);
		Assert.assertNotNull(activity);
	
		
		// test activity for task 1100
		List<ItemCollection> activities = model.getActivityEntityList(1000,VERSION);
		Assert.assertNotNull(activities);
		Assert.assertEquals(2, activities.size());
	
	
	}

}
