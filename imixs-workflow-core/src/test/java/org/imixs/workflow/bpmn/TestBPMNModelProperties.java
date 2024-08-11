package org.imixs.workflow.bpmn;

import java.io.IOException;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.ModelException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;
import org.xml.sax.SAXException;

/**
 * Test class test the Imixs BPMNModel properties
 * 
 * See also issue #491
 * 
 * @author rsoika
 */
public class TestBPMNModelProperties {
	BPMNModel model = null;
	OpenBPMNModelManager openBPMNModelManager = null;

	@Before
	public void setup() throws ParseException, ParserConfigurationException, SAXException, IOException {
		openBPMNModelManager = new OpenBPMNModelManager();
		try {
			openBPMNModelManager.addModel(BPMNModelFactory.read("/bpmn/properties.bpmn"));
			model = openBPMNModelManager.getBPMNModel("1.0.0");
		} catch (ModelException | BPMNModelException e) {
			e.printStackTrace();
			Assert.fail();
		}

	}

	/**
	 * Test the adoption of deprecated task properties.
	 * 
	 * See issue #491
	 * 
	 * @throws ModelException
	 */
	@Test
	public void testDeprecatedTaskProperties() throws ModelException {

		Assert.assertNotNull(model);

		// test task 1000
		ItemCollection task = openBPMNModelManager.findTaskByID(model, 1100);
		Assert.assertNotNull(task);
		// Assert.assertEquals("1.0.0", task.getItemValueString("$ModelVersion"));
		// Assert.assertEquals("Simple", task.getItemValueString("txtworkflowgroup"));

		Assert.assertEquals("Task 2", task.getItemValueString(OpenBPMNUtil.TASK_ITEM_NAME));
		Assert.assertEquals("test documentation", task.getItemValueString(OpenBPMNUtil.TASK_ITEM_DOCUMENTATION));

		// new items
		Assert.assertEquals("test summary", task.getItemValueString(OpenBPMNUtil.TASK_ITEM_WORKFLOW_SUMMARY));
		Assert.assertEquals("test abstract", task.getItemValueString(OpenBPMNUtil.TASK_ITEM_WORKFLOW_ABSTRACT));

		// application
		Assert.assertEquals("test form", task.getItemValueString(OpenBPMNUtil.TASK_ITEM_APPLICATION_EDITOR));
		Assert.assertEquals("test icon", task.getItemValueString(OpenBPMNUtil.TASK_ITEM_APPLICATION_ICON));
		Assert.assertEquals("workitemarchive", task.getItemValueString(OpenBPMNUtil.TASK_ITEM_APPLICATION_TYPE));

		// acl
		Assert.assertTrue(task.getItemValueBoolean(OpenBPMNUtil.TASK_ITEM_ACL_UPDATE));
		Assert.assertEquals("test_actor", task.getItemValueString(OpenBPMNUtil.TASK_ITEM_ACL_OWNER_LIST_MAPPING));
		Assert.assertEquals("test_actor",
				task.getItemValueString(OpenBPMNUtil.TASK_ITEM_ACL_READACCESS_LIST_MAPPING));
		Assert.assertEquals("test_actor",
				task.getItemValueString(OpenBPMNUtil.TASK_ITEM_ACL_WRITEACCESS_LIST_MAPPING));

		Assert.assertEquals("testowner", task.getItemValueString(OpenBPMNUtil.TASK_ITEM_ACL_OWNER_LIST));
		Assert.assertEquals("testreadaccess", task.getItemValueString(OpenBPMNUtil.TASK_ITEM_ACL_READACCESS_LIST));
		Assert.assertEquals("testwriteaccess", task.getItemValueString(OpenBPMNUtil.TASK_ITEM_ACL_WRITEACCESS_LIST));

	}

	/**
	 * Test the adoption of deprecated event properties.
	 * 
	 * See issue #491
	 * 
	 * @throws ModelException
	 */
	@Test
	public void testDeprecatedEventProperties() throws ModelException {

		Assert.assertNotNull(model);

		// test event 1000,20
		ItemCollection event = openBPMNModelManager.findEventByID(model, 1000, 20);
		Assert.assertNotNull(event);

		Assert.assertEquals("submit", event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_NAME));
		Assert.assertEquals("test documentation", event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_DOCUMENTATION));

		// acl
		Assert.assertTrue(event.getItemValueBoolean(OpenBPMNUtil.EVENT_ITEM_ACL_UPDATE));
		Assert.assertEquals("test_actor", event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_ACL_OWNER_LIST_MAPPING));
		Assert.assertEquals("test_actor",
				event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_ACL_READACCESS_LIST_MAPPING));
		Assert.assertEquals("test_actor",
				event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_ACL_WRITEACCESS_LIST_MAPPING));
		Assert.assertEquals("testowner", event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_ACL_OWNER_LIST));
		Assert.assertEquals("testreadaccess", event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_ACL_READACCESS_LIST));
		Assert.assertEquals("testwriteaccess", event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_ACL_WRITEACCESS_LIST));

		// workflow
		Assert.assertTrue(event.getItemValueBoolean(OpenBPMNUtil.EVENT_ITEM_WORKFLOW_PUBLIC));
		Assert.assertEquals("my result", event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_WORKFLOW_RESULT));
		Assert.assertEquals("read acces", event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_READACCESS));
		Assert.assertEquals("test_actor", event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_WORKFLOW_PUBLIC_ACTORS));

		// history
		Assert.assertEquals("my history", event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_HISTORY_MESSAGE));

		// mail
		Assert.assertEquals("mail subject", event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_MAIL_SUBJECT));
		Assert.assertEquals("mail body", event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_MAIL_BODY));
		Assert.assertEquals("to", event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_MAIL_TO_LIST));
		Assert.assertEquals("cc", event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_MAIL_CC_LIST));
		Assert.assertEquals("bcc", event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_MAIL_BCC_LIST));
		Assert.assertEquals("test_actor", event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_MAIL_TO_LIST_MAPPING));
		Assert.assertEquals("test_actor", event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_MAIL_CC_LIST_MAPPING));
		Assert.assertEquals("test_actor", event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_MAIL_BCC_LIST_MAPPING));

		// rule
		Assert.assertEquals("my rule", event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_RULE_ENGINE));
		Assert.assertEquals("a=1", event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_RULE_DEFINITION));

		// report
		Assert.assertEquals("my report", event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_REPORT_NAME));
		Assert.assertEquals("my file", event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_REPORT_PATH));
		Assert.assertEquals("my params", event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_REPORT_OPTIONS));
		Assert.assertEquals("2", event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_REPORT_TARGET));

		// version
		Assert.assertEquals("1", event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_VERSION_MODE));
		Assert.assertEquals("55", event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_VERSION_EVENT));

		// timer
		Assert.assertTrue(event.getItemValueBoolean(OpenBPMNUtil.EVENT_ITEM_TIMER_ACTIVE));
		Assert.assertEquals("my timer selection", event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_TIMER_SELECTION));
		Assert.assertEquals("24", event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_TIMER_DELAY));
		Assert.assertEquals("2", event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_TIMER_DELAY_UNIT));
		Assert.assertEquals("4", event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_TIMER_DELAY_BASE));
		Assert.assertEquals("datdate", event.getItemValueString(OpenBPMNUtil.EVENT_ITEM_TIMER_DELAY_BASE_PROPERTY));

	}

}
