package org.imixs.workflow;

import static org.mockito.Mockito.when;

import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.ejb.SessionContext;

import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

/**
 * Test class for workflow kernel
 * 
 * @author rsoika
 * 
 */
public class KernelTest {

	WorkflowKernel kernel = null;
	protected SessionContext ctx;
	protected WorkflowContext workflowContext;
	private static Logger logger = Logger.getLogger(KernelTest.class.getName());

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
		ItemCollection itemCollection = new ItemCollection();
		itemCollection.replaceItemValue("txtTitel", "Hello");
		itemCollection.replaceItemValue("$processid", 100);
		itemCollection.replaceItemValue("$activityid", 10);
		itemCollection.replaceItemValue("$modelversion", MokModel.DEFAULT_MODEL_VERSION);

		Assert.assertEquals(itemCollection.getItemValueString("txttitel"), "Hello");

		try {
			kernel.process(itemCollection);
		} catch (PluginException e) {
			Assert.fail();
			e.printStackTrace();
		} catch (ProcessingErrorException e) {
			Assert.fail();
			e.printStackTrace();
		}

		Assert.assertEquals(1, itemCollection.getItemValueInteger("runs"));
		Assert.assertEquals(100, itemCollection.getItemValueInteger("$processid"));
	}

	@Test
	@Category(org.imixs.workflow.WorkflowKernel.class)
	public void testForward() {
		ItemCollection itemCollection = new ItemCollection();
		itemCollection.replaceItemValue("txtTitel", "Hello");
		itemCollection.replaceItemValue("$processid", 100);
		itemCollection.replaceItemValue("$activityid", 20);
		itemCollection.replaceItemValue("$modelversion", MokModel.DEFAULT_MODEL_VERSION);

		Assert.assertEquals(itemCollection.getItemValueString("txttitel"), "Hello");

		try {
			kernel.process(itemCollection);
		} catch (PluginException e) {
			Assert.fail();
			e.printStackTrace();
		} catch (ProcessingErrorException e) {
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
		itemCollection.replaceItemValue("$processid", 100);
		itemCollection.replaceItemValue("$activityid", 11);
		itemCollection.replaceItemValue("$modelversion", MokModel.DEFAULT_MODEL_VERSION);

		Assert.assertEquals(itemCollection.getItemValueString("txttitel"), "Hello");

		try {
			kernel.process(itemCollection);
		} catch (PluginException e) {
			Assert.fail();
			e.printStackTrace();
		} catch (ProcessingErrorException e) {
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
	 * This method tests the generation of the txtworkflowactivitylog entries.
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
			itemCollection.replaceItemValue("$activityid", 10);
			kernel.process(itemCollection);

			itemCollection.replaceItemValue("$activityid", 20);
			// sumulate a Log Comment...
			itemCollection.replaceItemValue("txtworkflowactivitylogComment", "userid|comment");

			kernel.process(itemCollection);

		} catch (PluginException e) {
			Assert.fail();
			e.printStackTrace();
		} catch (ProcessingErrorException e) {
			Assert.fail();
			e.printStackTrace();
		}

		Assert.assertEquals(2, itemCollection.getItemValueInteger("runs"));
		// test next state
		Assert.assertEquals(200, itemCollection.getItemValueInteger("$processid"));

		// test log
		List log = itemCollection.getItemValue("txtworkflowactivitylog");

		Assert.assertNotNull(log);
		Assert.assertTrue(log.size() == 2);

		logger.info("'txtworkflowactivitylog'=" + log);

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
}
