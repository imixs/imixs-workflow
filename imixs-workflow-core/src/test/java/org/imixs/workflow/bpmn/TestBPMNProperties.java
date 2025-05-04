package org.imixs.workflow.bpmn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.exceptions.ModelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
public class TestBPMNProperties {
	BPMNModel model = null;
	ModelManager openBPMNModelManager = null;

	@BeforeEach
	public void setup() throws ParseException, ParserConfigurationException, SAXException, IOException {
		openBPMNModelManager = new ModelManager();
		try {
			model = BPMNModelFactory.read("/bpmn/properties.bpmn");
		} catch (BPMNModelException e) {
			e.printStackTrace();
			fail();
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

		assertNotNull(model);

		// test task 1000
		ItemCollection task = openBPMNModelManager.findTaskByID(model, 1100);
		assertNotNull(task);
		// assertEquals("1.0.0", task.getItemValueString("$ModelVersion"));
		// assertEquals("Simple", task.getItemValueString("txtworkflowgroup"));

		assertEquals("Task 2", task.getItemValueString(BPMNUtil.TASK_ITEM_NAME));
		assertEquals("test documentation", task.getItemValueString(BPMNUtil.TASK_ITEM_DOCUMENTATION));

		// new items
		assertEquals("test summary", task.getItemValueString(BPMNUtil.TASK_ITEM_WORKFLOW_SUMMARY));
		assertEquals("test abstract", task.getItemValueString(BPMNUtil.TASK_ITEM_WORKFLOW_ABSTRACT));

		// application
		assertEquals("test form", task.getItemValueString(BPMNUtil.TASK_ITEM_APPLICATION_EDITOR));
		assertEquals("test icon", task.getItemValueString(BPMNUtil.TASK_ITEM_APPLICATION_ICON));
		assertEquals("workitemarchive", task.getItemValueString(BPMNUtil.TASK_ITEM_APPLICATION_TYPE));

		// acl
		assertTrue(task.getItemValueBoolean(BPMNUtil.TASK_ITEM_ACL_UPDATE));
		assertEquals("test_actor", task.getItemValueString(BPMNUtil.TASK_ITEM_ACL_OWNER_LIST_MAPPING));
		assertEquals("test_actor",
				task.getItemValueString(BPMNUtil.TASK_ITEM_ACL_READACCESS_LIST_MAPPING));
		assertEquals("test_actor",
				task.getItemValueString(BPMNUtil.TASK_ITEM_ACL_WRITEACCESS_LIST_MAPPING));

		assertEquals("testowner", task.getItemValueString(BPMNUtil.TASK_ITEM_ACL_OWNER_LIST));
		assertEquals("testreadaccess", task.getItemValueString(BPMNUtil.TASK_ITEM_ACL_READACCESS_LIST));
		assertEquals("testwriteaccess", task.getItemValueString(BPMNUtil.TASK_ITEM_ACL_WRITEACCESS_LIST));

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

		assertNotNull(model);

		// test event 1000,20
		ItemCollection event = openBPMNModelManager.findEventByID(model, 1000, 20);
		assertNotNull(event);

		assertEquals("submit", event.getItemValueString(BPMNUtil.EVENT_ITEM_NAME));
		assertEquals("test documentation", event.getItemValueString(BPMNUtil.EVENT_ITEM_DOCUMENTATION));

		// acl
		assertTrue(event.getItemValueBoolean(BPMNUtil.EVENT_ITEM_ACL_UPDATE));
		assertEquals("test_actor", event.getItemValueString(BPMNUtil.EVENT_ITEM_ACL_OWNER_LIST_MAPPING));
		assertEquals("test_actor",
				event.getItemValueString(BPMNUtil.EVENT_ITEM_ACL_READACCESS_LIST_MAPPING));
		assertEquals("test_actor",
				event.getItemValueString(BPMNUtil.EVENT_ITEM_ACL_WRITEACCESS_LIST_MAPPING));
		assertEquals("testowner", event.getItemValueString(BPMNUtil.EVENT_ITEM_ACL_OWNER_LIST));
		assertEquals("testreadaccess", event.getItemValueString(BPMNUtil.EVENT_ITEM_ACL_READACCESS_LIST));
		assertEquals("testwriteaccess", event.getItemValueString(BPMNUtil.EVENT_ITEM_ACL_WRITEACCESS_LIST));

		// workflow
		assertTrue(event.getItemValueBoolean(BPMNUtil.EVENT_ITEM_WORKFLOW_PUBLIC));
		assertEquals("my result", event.getItemValueString(BPMNUtil.EVENT_ITEM_WORKFLOW_RESULT));
		assertEquals("read acces", event.getItemValueString(BPMNUtil.EVENT_ITEM_READACCESS));
		assertEquals("test_actor", event.getItemValueString(BPMNUtil.EVENT_ITEM_WORKFLOW_PUBLIC_ACTORS));

		// history
		assertEquals("my history", event.getItemValueString(BPMNUtil.EVENT_ITEM_HISTORY_MESSAGE));

		// mail
		assertEquals("mail subject", event.getItemValueString(BPMNUtil.EVENT_ITEM_MAIL_SUBJECT));
		assertEquals("mail body", event.getItemValueString(BPMNUtil.EVENT_ITEM_MAIL_BODY));
		assertEquals("to", event.getItemValueString(BPMNUtil.EVENT_ITEM_MAIL_TO_LIST));
		assertEquals("cc", event.getItemValueString(BPMNUtil.EVENT_ITEM_MAIL_CC_LIST));
		assertEquals("bcc", event.getItemValueString(BPMNUtil.EVENT_ITEM_MAIL_BCC_LIST));
		assertEquals("test_actor", event.getItemValueString(BPMNUtil.EVENT_ITEM_MAIL_TO_LIST_MAPPING));
		assertEquals("test_actor", event.getItemValueString(BPMNUtil.EVENT_ITEM_MAIL_CC_LIST_MAPPING));
		assertEquals("test_actor", event.getItemValueString(BPMNUtil.EVENT_ITEM_MAIL_BCC_LIST_MAPPING));

		// rule
		assertEquals("my rule", event.getItemValueString(BPMNUtil.EVENT_ITEM_RULE_ENGINE));
		assertEquals("a=1", event.getItemValueString(BPMNUtil.EVENT_ITEM_RULE_DEFINITION));

		// report
		assertEquals("my report", event.getItemValueString(BPMNUtil.EVENT_ITEM_REPORT_NAME));
		assertEquals("my file", event.getItemValueString(BPMNUtil.EVENT_ITEM_REPORT_PATH));
		assertEquals("my params", event.getItemValueString(BPMNUtil.EVENT_ITEM_REPORT_OPTIONS));
		assertEquals("2", event.getItemValueString(BPMNUtil.EVENT_ITEM_REPORT_TARGET));

		// version
		assertEquals("1", event.getItemValueString(BPMNUtil.EVENT_ITEM_VERSION_MODE));
		assertEquals("55", event.getItemValueString(BPMNUtil.EVENT_ITEM_VERSION_EVENT));

		// timer
		assertTrue(event.getItemValueBoolean(BPMNUtil.EVENT_ITEM_TIMER_ACTIVE));
		assertEquals("my timer selection", event.getItemValueString(BPMNUtil.EVENT_ITEM_TIMER_SELECTION));
		assertEquals("24", event.getItemValueString(BPMNUtil.EVENT_ITEM_TIMER_DELAY));
		assertEquals("2", event.getItemValueString(BPMNUtil.EVENT_ITEM_TIMER_DELAY_UNIT));
		assertEquals("4", event.getItemValueString(BPMNUtil.EVENT_ITEM_TIMER_DELAY_BASE));
		assertEquals("datdate", event.getItemValueString(BPMNUtil.EVENT_ITEM_TIMER_DELAY_BASE_PROPERTY));

	}

}
