package org.imixs.workflow;

import static org.mockito.Mockito.when;

import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;

import javax.ejb.SessionContext;

import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

/**
 * Test class for Imixs WorkflowKernel using a static default model. The test
 * class verifies basic functionality. See the test class
 * TestWorklfowKernelTestModels for more complex model tests.
 * 
 * @author rsoika
 */
public class TestWorkflowKernel {

	WorkflowKernel kernel = null;
	protected SessionContext ctx;
	protected WorkflowContext workflowContext;
	private static Logger logger = Logger.getLogger(TestWorkflowKernel.class.getName());

	@Before
	public void setup() throws PluginException {

		ctx = Mockito.mock(SessionContext.class);
		// simulate SessionContext ctx.getCallerPrincipal().getName()
		Principal principal = Mockito.mock(Principal.class);
		when(principal.getName()).thenReturn("manfred");
		when(ctx.getCallerPrincipal()).thenReturn(principal);

		workflowContext = Mockito.mock(WorkflowContext.class);

		// provide a mock modelManger class
		when(workflowContext.getModelManager()).thenReturn(new MokModelManager());

		// MokWorkflowContext ctx = new MokWorkflowContext();
		kernel = new WorkflowKernel(workflowContext);

		MokPlugin mokPlugin = new MokPlugin();
		kernel.registerPlugin(mokPlugin);

	}

	@Test
	@Category(org.imixs.workflow.WorkflowKernel.class)
	public void testProcess() {
		ItemCollection itemCollectionProcessed=null; 
		ItemCollection itemCollection = new ItemCollection();
		itemCollection.replaceItemValue("txtTitel", "Hello");
		itemCollection.setTaskID(100);
		itemCollection.setEventID(10);
		itemCollection.replaceItemValue("$modelversion", MokModel.DEFAULT_MODEL_VERSION);

		Assert.assertEquals(itemCollection.getItemValueString("txttitel"), "Hello");

		try {
			itemCollectionProcessed = kernel.process(itemCollection);
		} catch (PluginException e) {
			Assert.fail();
			e.printStackTrace();
		} catch (ProcessingErrorException e) {
			Assert.fail();
			e.printStackTrace();
		} catch (ModelException e) {
			Assert.fail();
			e.printStackTrace();
		}

		Assert.assertEquals(1, itemCollectionProcessed.getItemValueInteger("runs"));
		Assert.assertEquals(100, itemCollectionProcessed.getTaskID());
		
		// initial and processed workitems are not the same and not equals! 
		Assert.assertNotSame(itemCollection, itemCollectionProcessed);
		Assert.assertFalse(itemCollection.equals(itemCollectionProcessed));
	}
	

	/**
	 * This test verifies if the deprecated fileds "$processid" and $activityID are still working.
	 * 
	 * see issue #381
	 */
	@Test
	@Category(org.imixs.workflow.WorkflowKernel.class)
	public void testProcessWithDeprecatedField() {
		ItemCollection itemCollectionProcessed=null; 
		ItemCollection itemCollection = new ItemCollection();
		itemCollection.replaceItemValue("txtTitel", "Hello");
		itemCollection.replaceItemValue("$processid", 100);
		itemCollection.setEventID(10);
		itemCollection.replaceItemValue("$modelversion", MokModel.DEFAULT_MODEL_VERSION);

		Assert.assertEquals(itemCollection.getItemValueString("txttitel"), "Hello");

		try {
			itemCollectionProcessed = kernel.process(itemCollection);
		} catch (PluginException e) {
			Assert.fail();
			e.printStackTrace();
		} catch (ProcessingErrorException e) {
			Assert.fail();
			e.printStackTrace();
		} catch (ModelException e) {
			Assert.fail();
			e.printStackTrace();
		}

		Assert.assertEquals(1, itemCollectionProcessed.getItemValueInteger("runs"));
		Assert.assertEquals(100, itemCollectionProcessed.getItemValueInteger("$processid"));
		
		// initial and processed workitems are not the same and not equals! 
		Assert.assertNotSame(itemCollection, itemCollectionProcessed);
		Assert.assertFalse(itemCollection.equals(itemCollectionProcessed));
	}

	/**
	 * This if a plugin which returns null influences the workitem
	 */
	@Test
	@Category(org.imixs.workflow.WorkflowKernel.class)
	public void testProcessNullPlugin() {
		ItemCollection itemCollection = new ItemCollection();
		itemCollection.replaceItemValue("txtTitel", "Hello");
		itemCollection.setTaskID(100);
		itemCollection.setEventID(10);
		itemCollection.replaceItemValue("$modelversion", MokModel.DEFAULT_MODEL_VERSION);

		Assert.assertEquals(itemCollection.getItemValueString("txttitel"), "Hello");

		try {
			// MokWorkflowContext ctx = new MokWorkflowContext();
			kernel = new WorkflowKernel(workflowContext);

			MokPluginNull mokPlugin = new MokPluginNull();
			kernel.registerPlugin(mokPlugin);
			itemCollection.replaceItemValue("txtname", "test");

			kernel.process(itemCollection);
			// kernel should throw exception...
			Assert.fail();
		} catch (PluginException e) {
			Assert.assertEquals(WorkflowKernel.PLUGIN_ERROR, e.getErrorCode());

		} catch (ProcessingErrorException e) {
			Assert.fail();
			e.printStackTrace();
		} catch (ModelException e) {
			Assert.fail();
			e.printStackTrace();
		}

		Assert.assertEquals("test", itemCollection.getItemValueString("txtname"));
		Assert.assertEquals(100, itemCollection.getItemValueInteger("$processid"));
	}

	@Test
	@Category(org.imixs.workflow.WorkflowKernel.class)
	public void testForward() {
		ItemCollection itemCollection = new ItemCollection();
		itemCollection.replaceItemValue("txtTitel", "Hello");
		itemCollection.replaceItemValue("$processid", 100);
		itemCollection.setEventID(20);
		itemCollection.replaceItemValue("$modelversion", MokModel.DEFAULT_MODEL_VERSION);

		Assert.assertEquals(itemCollection.getItemValueString("txttitel"), "Hello");

		try {
			itemCollection = kernel.process(itemCollection);
		} catch (PluginException e) {
			Assert.fail();
			e.printStackTrace();
		} catch (ProcessingErrorException e) {
			Assert.fail();
			e.printStackTrace();
		} catch (ModelException e) {
			Assert.fail();
			e.printStackTrace();
		}

		Assert.assertEquals(1, itemCollection.getItemValueInteger("runs"));
		// test next state
		Assert.assertEquals(200, itemCollection.getItemValueInteger("$processid"));
	}

	@Test
	@Category(org.imixs.workflow.WorkflowKernel.class)
	public void testFollowup() {
		ItemCollection itemCollection = new ItemCollection();
		itemCollection.replaceItemValue("txtTitel", "Hello");
		itemCollection.setTaskID(100);
		itemCollection.setEventID(11);
		itemCollection.replaceItemValue("$modelversion", MokModel.DEFAULT_MODEL_VERSION);

		Assert.assertEquals(itemCollection.getItemValueString("txttitel"), "Hello");

		try {
			itemCollection = kernel.process(itemCollection);
		} catch (PluginException e) {
			Assert.fail();
			e.printStackTrace();
		} catch (ProcessingErrorException e) {
			Assert.fail();
			e.printStackTrace();
		} catch (ModelException e) {
			Assert.fail();
			e.printStackTrace();
		}

		// runs should be 2
		Assert.assertEquals(2, itemCollection.getItemValueInteger("runs"));
		// test next state
		Assert.assertEquals(200, itemCollection.getItemValueInteger("$processid"));
	}

	@Test
	@Category(org.imixs.workflow.WorkflowKernel.class)
	public void testRegisterPlugin() {

		try {
			kernel.unregisterPlugin(MokPlugin.class.getName());
		} catch (PluginException e1) {
			Assert.fail();
			e1.printStackTrace();
		}

		// unregister once again - exception expected

		try {
			kernel.unregisterPlugin(MokPlugin.class.getName());
			// exception expected!
			Assert.fail();
		} catch (PluginException e1) {
			Assert.assertEquals(WorkflowKernel.PLUGIN_NOT_REGISTERED, e1.getErrorCode());
		}

		try {
			MokPlugin mokPlugin = new MokPlugin();
			kernel.registerPlugin(mokPlugin);
		} catch (PluginException e) {
			Assert.fail();
			e.printStackTrace();
		}

	}

	/**
	 * This method tests the generation of the $eventlog entries.
	 */
	@SuppressWarnings("rawtypes")
	@Test
	@Category(org.imixs.workflow.WorkflowKernel.class)
	public void testActivityLog() {
		ItemCollection itemCollection = new ItemCollection();
		itemCollection.replaceItemValue("$modelversion", "1.0.0");
		itemCollection.replaceItemValue("txtTitel", "Hello");
		itemCollection.replaceItemValue("$processid", 100);

		Assert.assertEquals(itemCollection.getItemValueString("txttitel"), "Hello");

		try {
			// simulate two steps
			itemCollection.setEventID(10);
			itemCollection = kernel.process(itemCollection);

			itemCollection.setEventID(20);
			// sumulate a Log Comment...
			itemCollection.replaceItemValue("txtworkflowactivitylogComment", "userid|comment");

			itemCollection = kernel.process(itemCollection);

		} catch (PluginException e) {
			Assert.fail();
			e.printStackTrace();
		} catch (ProcessingErrorException e) {
			Assert.fail();
			e.printStackTrace();
		} catch (ModelException e) {
			Assert.fail();
			e.printStackTrace();
		}

		Assert.assertEquals(2, itemCollection.getItemValueInteger("runs"));
		// test next state
		Assert.assertEquals(200, itemCollection.getItemValueInteger("$processid"));

		// test log
		List log = itemCollection.getItemValue("$eventlog");

		Assert.assertNotNull(log);
		Assert.assertEquals(2, log.size());

		logger.info("'$eventlog'=" + log);

		// test log entries
		// Format: timestamp|model-version|1000.10|1000|userid|
		String logEntry = (String) log.get(0);
		StringTokenizer st = new StringTokenizer(logEntry, "|");
		st.nextToken();
		Assert.assertEquals("1.0.0", st.nextToken());
		Assert.assertEquals("100.10", st.nextToken());
		Assert.assertEquals("100", st.nextToken());
		Assert.assertFalse(st.hasMoreTokens());

		logEntry = (String) log.get(1);
		st = new StringTokenizer(logEntry, "|");
		try {
			// check date object
			String sDate = st.nextToken();

			SimpleDateFormat formatter = new SimpleDateFormat(WorkflowKernel.ISO8601_FORMAT);
			Date date = null;
			date = formatter.parse(sDate);

			Calendar cal = Calendar.getInstance();
			Calendar calNow = Calendar.getInstance();
			cal.setTime(date);

			Assert.assertEquals(calNow.get(Calendar.YEAR), cal.get(Calendar.YEAR));
			Assert.assertEquals(calNow.get(Calendar.MONTH), cal.get(Calendar.MONTH));

		} catch (ParseException e) {

			e.printStackTrace();
			Assert.fail();
		}

		Assert.assertEquals("1.0.0", st.nextToken());
		Assert.assertEquals("100.20", st.nextToken());
		Assert.assertEquals("200", st.nextToken());
		// test commment
		Assert.assertTrue(st.hasMoreTokens());
		Assert.assertEquals("userid", st.nextToken());
		Assert.assertEquals("comment", st.nextToken());
		Assert.assertFalse(st.hasMoreTokens());

	}

	/**
	 * This method tests the generation of the $eventlog entries and
	 * the restriction to a maximum length of 30 entries.
	 * 
	 * Issue https://github.com/imixs/imixs-workflow/issues/179
	 * 
	 */
	@SuppressWarnings("rawtypes")
	@Test
	@Category(org.imixs.workflow.WorkflowKernel.class)
	public void testActivityLogMaxLength() {
		ItemCollection itemCollection = new ItemCollection();
		itemCollection.replaceItemValue("$modelversion", "1.0.0");
		itemCollection.replaceItemValue("txtTitel", "Hello");
		itemCollection.replaceItemValue("$processid", 100);

		// we create 40 dummy entries
		String dummyEntry = "" + new Date() + "|1.0.0|100.10|100";
		Vector<String> v = new Vector<String>();
		for (int i = 1; i <= 40; i++) {
			v.add(dummyEntry);
		}
		itemCollection.replaceItemValue("$eventlog", v);

		try {
			// simulate two steps
			itemCollection.setEventID(10);
			itemCollection = kernel.process(itemCollection);

			itemCollection.setEventID(20);
			// sumulate a Log Comment...
			itemCollection.replaceItemValue("txtworkflowactivitylogComment", "userid|comment");

			itemCollection = kernel.process(itemCollection);

		} catch (PluginException e) {
			Assert.fail();
			e.printStackTrace();
		} catch (ProcessingErrorException e) {
			Assert.fail();
			e.printStackTrace();
		} catch (ModelException e) {
			Assert.fail();
			e.printStackTrace();
		}

		Assert.assertEquals(2, itemCollection.getItemValueInteger("runs"));
		// test next state
		Assert.assertEquals(200, itemCollection.getItemValueInteger("$processid"));

		// test log
		List log = itemCollection.getItemValue("$eventlog");

		Assert.assertNotNull(log);
		Assert.assertEquals(30, log.size());

		logger.info("'$eventlog'=" + log);

		// test log entries
		// Format: timestamp|model-version|1000.10|1000|userid|
		String logEntry = (String) log.get(log.size() - 2);
		StringTokenizer st = new StringTokenizer(logEntry, "|");
		st.nextToken();
		Assert.assertEquals("1.0.0", st.nextToken());
		Assert.assertEquals("100.10", st.nextToken());
		Assert.assertEquals("100", st.nextToken());
		Assert.assertFalse(st.hasMoreTokens());

		// test last entry
		logEntry = (String) log.get(log.size() - 1);
		st = new StringTokenizer(logEntry, "|");
		try {
			// check date object
			String sDate = st.nextToken();

			SimpleDateFormat formatter = new SimpleDateFormat(WorkflowKernel.ISO8601_FORMAT);
			Date date = null;
			date = formatter.parse(sDate);

			Calendar cal = Calendar.getInstance();
			Calendar calNow = Calendar.getInstance();
			cal.setTime(date);

			Assert.assertEquals(calNow.get(Calendar.YEAR), cal.get(Calendar.YEAR));
			Assert.assertEquals(calNow.get(Calendar.MONTH), cal.get(Calendar.MONTH));

		} catch (ParseException e) {

			e.printStackTrace();
			Assert.fail();
		}

		Assert.assertEquals("1.0.0", st.nextToken());
		Assert.assertEquals("100.20", st.nextToken());
		Assert.assertEquals("200", st.nextToken());
		// test commment
		Assert.assertTrue(st.hasMoreTokens());
		Assert.assertEquals("userid", st.nextToken());
		Assert.assertEquals("comment", st.nextToken());
		Assert.assertFalse(st.hasMoreTokens());

	}

	/**
	 * test generated UUID
	 * 
	 * @see https://docs.oracle.com/javase/8/docs/api/java/util/UUID.html
	 */
	@Test
	@Category(org.imixs.workflow.WorkflowKernel.class)
	public void testUUID() {
		String uid = WorkflowKernel.generateUniqueID();
		// expected length is 36
		Assert.assertEquals(36, uid.length());
	}
}
