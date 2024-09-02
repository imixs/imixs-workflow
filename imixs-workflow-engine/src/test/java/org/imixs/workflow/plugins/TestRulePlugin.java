package org.imixs.workflow.plugins;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.ScriptException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.plugins.RulePlugin;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for RulePlugin
 * 
 * @author rsoika
 */
public class TestRulePlugin {
	protected RulePlugin rulePlugin = null;
	private final static Logger logger = Logger.getLogger(TestRulePlugin.class.getName());

	@Before
	public void setUp() throws PluginException {
		rulePlugin = new RulePlugin();
		rulePlugin.init(null);
	}

	/**
	 * This test verifies the evaluation of a simple script.
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Test
	public void testBasicScript() throws PluginException {

		ItemCollection workitem = new ItemCollection();
		workitem.replaceItemValue("txtName", "Anna");
		ItemCollection event = new ItemCollection();

		// set a business rule
		String script = "var result={}; var a=1;var b=2;result.isValid = ((a<b) && 'Anna'==workitem.getItemValueString('txtname'));";

		logger.log(Level.INFO, "Script={0}", script);
		event.replaceItemValue("txtBusinessRUle", script);

		// run plugin
		workitem = rulePlugin.run(workitem, event);
		Assert.assertNotNull(workitem);

		Assert.assertEquals("Anna", workitem.getItemValueString("txtName"));

	}

	/**
	 * Test a deprecated script dialect
	 * 
	 * @throws PluginException
	 */
	@Test
	public void testBasicScriptDeprecated() throws PluginException {

		ItemCollection workitem = new ItemCollection();
		workitem.replaceItemValue("txtName", "Anna");
		ItemCollection event = new ItemCollection();

		// set a business rule
		String script = "var result={}; var a=1;var b=2;result.isValid = ((a<b) && 'Anna'==workitem.get('txtname')[0]);";

		logger.log(Level.INFO, "Script={0}", script);
		event.replaceItemValue("txtBusinessRUle", script);

		// run plugin
		workitem = rulePlugin.run(workitem, event);
		Assert.assertNotNull(workitem);

		Assert.assertEquals("Anna", workitem.getItemValueString("txtName"));

	}

	/**
	 * This test verifies the evaluation of a simple script unsing the json objects.
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Test
	public void testBasicScriptJson() throws PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("txtName", "Anna");
		ItemCollection adocumentActivity = new ItemCollection();

		// access single value
		String script = "var result={}; if (workitem.txtname && workitem.txtname[0]==='Anna') result.numage=50;";
		adocumentActivity.replaceItemValue("txtBusinessRUle", script);

		// run plugin
		adocumentContext = rulePlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		Assert.assertEquals("Anna", adocumentContext.getItemValueString("txtName"));
		Assert.assertEquals(50, adocumentContext.getItemValueInteger("numage"));

	}

	/**
	 * This test verifies if in case of isValid==false a PluginExeption is thrown
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Test(expected = PluginException.class)
	public void testSimplePluginException() throws PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		ItemCollection adocumentActivity = new ItemCollection();
		// set a business rule
		String script = "var a=1;var b=2;var result={ isValid : (a>b)};";
		logger.log(Level.INFO, "Script={0}", script);
		adocumentActivity.replaceItemValue("txtBusinessRUle", script);
		rulePlugin.run(adocumentContext, adocumentActivity);
		Assert.fail();

	}

	/**
	 * This test verifies if a script with only comments and not returning a result
	 * object is allowed.
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Test
	public void testEmptyScript() throws PluginException {

		ItemCollection workitem = new ItemCollection();
		ItemCollection event = new ItemCollection();
		// set a business rule
		String script = "// var a=1;var b=2;var isValid = (a>b);";
		logger.log(Level.INFO, "Script={0}", script);
		event.replaceItemValue("txtBusinessRUle", script);
		// should be accepted!
		rulePlugin.run(workitem, event);
		Assert.assertNotNull(workitem);
	}

	@Test(expected = PluginException.class)
	public void testResultObjectPluginException() throws PluginException {

		ItemCollection workitem = new ItemCollection();
		ItemCollection event = new ItemCollection();

		// set a business rule
		String script = "var result={ isValid:false };";

		logger.log(Level.INFO, "Script={0}", script);
		event.replaceItemValue("txtBusinessRUle", script);
		rulePlugin.run(workitem, event);
		Assert.fail();

	}

	/**
	 * This test verifies if in case of isValid==false a PluginExeption is thrown
	 * and evalues the data contained in the Exception. There for the script adds an
	 * errorCode and a errorMessage
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Test
	public void testComplexPluginException() {

		ItemCollection workitem = new ItemCollection();
		ItemCollection event = new ItemCollection();

		// 1) invalid returning one messsage
		String script = "var result={};var a=1;var b=2;result.isValid = (a>b);" + " result.errorCode='MY_ERROR';"
				+ " result.errorMessage='Somehing go wrong!';";

		logger.log(Level.INFO, "Script={0}", script);
		event.replaceItemValue("txtBusinessRUle", script);
		try {
			rulePlugin.run(workitem, event);
			Assert.fail();
		} catch (PluginException e) {
			logger.severe(e.getMessage());
			// test excption
			Assert.assertEquals("MY_ERROR", e.getErrorCode());
			Object[] params = e.getErrorParameters();
			Assert.assertEquals(1, params.length);
			Assert.assertEquals("Somehing go wrong!", params[0].toString());
		}

		// 2) invalid returning 2 messages in an array
		script = "var result={};var a=1;var b=2;result.isValid = (a>b);" + " result.errorMessage = new Array();"
				+ " result.errorMessage[0]='Somehing go wrong!';" + " result.errorMessage[1]='Somehingelse go wrong!';";

		logger.log(Level.INFO, "Script={0}", script);
		event.replaceItemValue("txtBusinessRUle", script);
		try {
			rulePlugin.run(workitem, event);
			Assert.fail();
		} catch (PluginException e) {
			// e.printStackTrace();
			// test exception
			Assert.assertEquals(RulePlugin.VALIDATION_ERROR, e.getErrorCode());
			Object[] params = e.getErrorParameters();
			Assert.assertEquals(2, params.length);
			Assert.assertEquals("Somehing go wrong!", params[0].toString());
			Assert.assertEquals("Somehingelse go wrong!", params[1].toString());
		}

	}

	/**
	 * only to evaluate some behavior
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Test
	public void simpleApprovalTest() throws PluginException {

		// set a business rule
		// workitem.get(refField1)[0])
		String script = " var result={};" + " if (workitem._amount_brutto[0]>5000)"
				+ "    result.followUp=90;";
		logger.log(Level.INFO, "Script={0}", script);

		ItemCollection workitem = new ItemCollection();
		workitem.replaceItemValue("_amount_brutto", Double.valueOf(6000));
		ItemCollection event = new ItemCollection();

		event.replaceItemValue("txtBusinessRUle", script);

		// run plugin
		workitem = rulePlugin.run(workitem, event);
		Assert.assertNotNull(workitem);

		/*
		 * Case 2
		 */

		workitem = new ItemCollection();
		workitem.replaceItemValue("_amount_brutto", Double.valueOf(3000));
		event = new ItemCollection();

		event.replaceItemValue("txtBusinessRUle", script);

		// run plugin
		workitem = rulePlugin.run(workitem, event);
		Assert.assertNotNull(workitem);

	}

	/**
	 * This test tests if a the properties of an activity entity can be evaluated by
	 * a script
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Test
	public void testEventObjectByScript() throws PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("txtName", "Anna");

		// simulate an activity
		ItemCollection adocumentActivity = new ItemCollection();
		adocumentActivity.replaceItemValue("keyMailEnabled", "1");

		// set a business rule
		String script = "var result={}; result.isValid =  '1'==event.keymailenabled[0];";

		logger.log(Level.INFO, "Script={0}", script);
		adocumentActivity.replaceItemValue("txtBusinessRUle", script);

		// run plugin
		adocumentContext = rulePlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

	}

	/**
	 * This test tests if a a scipt can inject new properties into the current
	 * activity entity
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Test
	public void testInjectItemIntoEventObjectByScript() throws PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("txtName", "Anna");

		// simulate an activity
		ItemCollection event = new ItemCollection();

		// set a business rule
		String script = "var result={}; event.setItemValue('nammailreplytouser','test@me.com');";

		logger.log(Level.INFO, "Script={0}", script);
		event.replaceItemValue("txtBusinessRUle", script);

		// run plugin
		adocumentContext = rulePlugin.run(adocumentContext, event);
		Assert.assertNotNull(adocumentContext);

		Assert.assertEquals("test@me.com", event.getItemValueString("nammailReplytoUser"));

	}

	/**
	 * This test test if a the properties of an activity entity can be evaluated by
	 * a script
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Test
	public void testChangeEventObejctByScript() throws PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("txtName", "Anna");

		// simulate an activity
		ItemCollection adocumentActivity = new ItemCollection();
		adocumentActivity.replaceItemValue("keyMailEnabled", "1");

		// set a business rule
		// String script = "var result={}; result.isValid =event.keymailenabled[0]=='1';
		// event.keymailenabled='0';";
		String script = "var result={}; result.isValid =event.keymailenabled[0]=='1'; event.setItemValue('keymailenabled','0');";

		logger.log(Level.INFO, "Script={0}", script);
		adocumentActivity.replaceItemValue("txtBusinessRUle", script);

		// run plugin
		adocumentContext = rulePlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		Assert.assertEquals("0", adocumentActivity.getItemValueString("keyMailEnabled"));

	}

	/**
	 * This test test if a the properties of an workitem entity can be evaluated by
	 * a script
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Test
	public void testSimpleWorkitemScript() throws PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("txtName", "Anna");
		adocumentContext.setTaskID(1000);
		// simulate an activity
		ItemCollection adocumentActivity = new ItemCollection();
		adocumentActivity.replaceItemValue("keyMailEnabled", "1");

		// set a business rule
		String script = "var result={}; result.isValid =  1000==workitem.get('$taskid')[0];";

		logger.log(Level.INFO, "Script={0}", script);
		adocumentActivity.replaceItemValue("txtBusinessRUle", script);

		// run plugin
		adocumentContext = rulePlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

	}

	/**
	 * Same test as before but using the deprected item $processid
	 */
	@Test
	public void testSimpleWorkitemScriptWithDeprecatedField() throws PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("txtName", "Anna");
		adocumentContext.setTaskID(1000);
		// simulate an activity
		ItemCollection adocumentActivity = new ItemCollection();
		adocumentActivity.replaceItemValue("keyMailEnabled", "1");

		// set a business rule
		// String script = "var isValid = 1000==workitem.get('$processid')[0];";
		String script = "var result={}; result.isValid =  1000==workitem.get('$processid')[0];";

		logger.log(Level.INFO, "Script={0}", script);
		adocumentActivity.replaceItemValue("txtBusinessRUle", script);

		// run plugin
		adocumentContext = rulePlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

	}

	/**
	 * This test verifies the isValid case for date fields
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Test
	public void testIsValidDate() throws PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("datDate", new Date());
		ItemCollection adocumentActivity = new ItemCollection();

		// 2) test true case
		String script = "var result={}; var refField1=\"_contact\";" + " var refField2=\"datdate\";"
				+ "  result.isValid=true;"
				+ " if (   ( workitem.get(refField2) == null)   ) {" + "    result.isValid=false;"
				+ "      result.errorMessage='1) Bitte geben Sie ein Datum fuer das Zahlungsziel an!';" + " }  ";
		logger.log(Level.INFO, "Script={0}", script);
		adocumentActivity.replaceItemValue("txtBusinessRUle", script);

		// run plugin
		adocumentContext = rulePlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		// Assert.assertTrue(rulePlugin.isValid(adocumentContext,
		// adocumentActivity));

		// 2) test false case
		adocumentContext = new ItemCollection();
		adocumentActivity = new ItemCollection();
		script = "var result={}; var refField1=\"_contact\";" + " var refField2=\"datdate\";" + " result.isValid=true;"
				+ " if (   ( workitem.get(refField2) == null)   ) {" + "     result.isValid=false;"
				+ "      result.errorMessage='2) Bitte geben Sie ein Datum fuer das Zahlungsziel an!';" + " }  ";
		logger.log(Level.INFO, "Script={0}", script);
		adocumentActivity.replaceItemValue("txtBusinessRUle", script);

		try {
			// run plugin
			adocumentContext = rulePlugin.run(adocumentContext, adocumentActivity);

			Assert.fail();
		} catch (PluginException pe) {
			// ok!
		}

		// Assert.assertFalse(rulePlugin.isValid(adocumentContext,
		// adocumentActivity));

		// calendar test
		adocumentContext = new ItemCollection();
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		adocumentContext.replaceItemValue("datDate", cal);
		adocumentActivity = new ItemCollection();

		// 2a) test true case
		script = "var result={}; var refField1=\"_contact\";" + " var refField2=\"datdate\";" + " result.isValid=true;"
				+ " if (   ( workitem.get(refField2) == null)   ) {" + "     result.isValid=false;"
				+ "     result.errorMessage='3) Bitte geben Sie ein Datum fuer das Zahlungsziel an!';" + " }  ";
		logger.log(Level.INFO, "Script={0}", script);
		adocumentActivity.replaceItemValue("txtBusinessRUle", script);

		// run plugin
		adocumentContext = rulePlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

	}

	/**
	 * 
	 * <code>
	 *   var refField1="txtbetrag"; 
		 var refField2="txtgutschift"; 
		 var isValid=true;
		 if ( ( workitem.get(refField1) == null || ''==workitem.get(refField1)[0]) || ( workitem.get(refField2) == null || ''==workitem.get(refField2)[0])) {
		     isValid=false;
		     var errorMessage='Bitte geben Sie den Betrag an! ';
		  }
	 * </code>
	 */
	@Test
	public void testIsValidTwoFields() throws PluginException {
		ItemCollection adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("txtbetrag", "5,55");
		adocumentContext.replaceItemValue("txtgutschift", "5,55");
		ItemCollection adocumentActivity = new ItemCollection();

		// 2) test true case
		String script = "var result={}; var refField1=\"txtbetrag\";" + "		 var refField2=\"txtgutschift\";"
				+ "		 result.isValid=true;"
				+ "		 if ( ( workitem.get(refField1) == null || ''==workitem.get(refField1)[0]) || ( workitem.get(refField2) == null || ''==workitem.get(refField2)[0])) {"
				+ "		     result.isValid=false;" + "		     result.errorMessage='Bitte geben Sie den Betrag an! ';"
				+ "		  } ";

		adocumentActivity.replaceItemValue("txtBusinessRUle", script);

		// run plugin
		adocumentContext = rulePlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("txtbetrag", "");
		adocumentContext.replaceItemValue("txtgutschift", "5,55");
		adocumentActivity = new ItemCollection();

		adocumentActivity.replaceItemValue("txtBusinessRUle", script);
		try {
			// run plugin
			adocumentContext = rulePlugin.run(adocumentContext, adocumentActivity);

			Assert.fail();
		} catch (PluginException e) {
			// ok
		}

		adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("txtbetrag", "55");
		adocumentContext.replaceItemValue("txtgutschift", "");
		adocumentActivity = new ItemCollection();

		adocumentActivity.replaceItemValue("txtBusinessRUle", script);
		try {
			// run plugin
			adocumentContext = rulePlugin.run(adocumentContext, adocumentActivity);

			Assert.fail();
		} catch (PluginException e) {
			// ok
		}

		adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("txtbetrag", "");
		adocumentContext.replaceItemValue("txtgutschift", "");
		adocumentActivity = new ItemCollection();

		adocumentActivity.replaceItemValue("txtBusinessRUle", script);
		try {
			// run plugin
			adocumentContext = rulePlugin.run(adocumentContext, adocumentActivity);

			Assert.fail();
		} catch (PluginException e) {
			// ok
		}

		adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("txtbetrag", "1,5");
		adocumentContext.replaceItemValue("txtgutschift", "4");
		adocumentActivity = new ItemCollection();

		adocumentActivity.replaceItemValue("txtBusinessRUle", script);
		// run plugin
		adocumentContext = rulePlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

	}

	/**
	 * following script should not throw an exception because of the fact that the
	 * errorCode is undefined.
	 * 
	 * @see issue #108
	 * 
	 */
	@Test
	public void testUndefinedErrorCode() throws PluginException {
		ItemCollection adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("_subject", "test");
		ItemCollection adocumentActivity = new ItemCollection();

		logger.info("testUndefineErroCode - test case 1:");
		// 2) test undefined case
		String script = " var result={}; result.isValid=true;" + " var errorCode,errorMessage;" + "refField='_contact';"
				+ "if ( workitem.get(refField) == null || workitem.get(refField)[0] == ''  ) {"
				+ "     result.isValid=false;"
				+ "     result.errorMessage='Please enter subject';" + " }";

		adocumentActivity.replaceItemValue("txtBusinessRule", script);

		try {
			rulePlugin.run(adocumentContext, adocumentActivity);

			Assert.fail();
		} catch (PluginException pe) {
			// PluginException expected
			logger.info(pe.getMessage());
			Assert.assertEquals(pe.getErrorCode(), RulePlugin.VALIDATION_ERROR);

			Object[] errorParams = pe.getErrorParameters();

			Assert.assertEquals(1, errorParams.length);
			Assert.assertEquals("Please enter subject", errorParams[0]);

		}

		logger.info("testUndefineErroCode - test case 2:");

		// test the same case if errorCode is defined
		// 2) test true case
		script = " var result={}; result.isValid=true;" + " var errorCode,errorMessage;" + "refField='_contact';"
				+ "if ( workitem.get(refField) == null || workitem.get(refField)[0] == ''  ) {"
				+ "     result.isValid=false;"
				+ "     result.errorCode='SOME_ERROR';" + " }";

		adocumentActivity.replaceItemValue("txtBusinessRule", script);

		try {
			rulePlugin.run(adocumentContext, adocumentActivity);

			Assert.fail();
		} catch (PluginException pe) {
			// PluginException expected
			logger.info(pe.getMessage());
			Assert.assertEquals(pe.getErrorCode(), "SOME_ERROR");
			Object[] errorParams = pe.getErrorParameters();
			Assert.assertTrue(errorParams == null);

		}

	}

	/*
	 * 
	 * New Version 3.0 supporting result object
	 * 
	 */

	/**
	 * This test verifies a json result object
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Test
	public void testResultObjectJSON() throws PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		ItemCollection adocumentActivity = new ItemCollection();

		// set a business rule
		String script = "var result={ someitem:'Hello World', somenumber:1};";

		logger.log(Level.INFO, "Script={0}", script);
		adocumentActivity.replaceItemValue("txtBusinessRUle", script);

		// run plugin
		adocumentContext = rulePlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		Assert.assertEquals("Hello World", adocumentContext.getItemValueString("someitem"));

		Assert.assertEquals(1, adocumentContext.getItemValueInteger("somenumber"));

	}

	/**
	 * This test verifies setting a new value via the result object
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Test
	public void testResultObjectNewValue() throws PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		ItemCollection adocumentActivity = new ItemCollection();

		// set a business rule
		String script = "var a=1.0;var b=2;var result={}; result.someitem='Hello World'; result.somenumber=1";

		logger.log(Level.INFO, "Script={0}", script);
		adocumentActivity.replaceItemValue("txtBusinessRUle", script);

		// run plugin
		adocumentContext = rulePlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		String newdata = adocumentContext.getItemValueString("someitem");

		Assert.assertEquals("Hello World", newdata);

	}

	/**
	 * This test verifies setting a new value list via the result object
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testResultObjectNewValueList() throws PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		ItemCollection adocumentActivity = new ItemCollection();

		// set a business rule
		String script = "var a=1.0;var b=2;var result={}; result.some_item=[]; result.some_item[0]='Hello World'; result.some_item[1]='Hello Imixs';";

		logger.log(Level.INFO, "Script={0}", script);
		adocumentActivity.replaceItemValue("txtBusinessRUle", script);

		// run plugin
		adocumentContext = rulePlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		List<String> newdata = adocumentContext.getItemValue("some_item");

		Assert.assertEquals(2, newdata.size());
		Assert.assertEquals("Hello World", newdata.get(0));
		Assert.assertEquals("Hello Imixs", newdata.get(1));

	}

	/**
	 * This test verifies setting a new value list via the result object as an JSON
	 * object
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testResultObjectNewValueListAsJSON() throws PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		ItemCollection adocumentActivity = new ItemCollection();

		// set a business rule
		String script = "var a=1.0;var b=2;var result={'single_item':'Hello World', 'multi_item':['Hello World','Hello Imixs']};";

		logger.log(Level.INFO, "Script={0}", script);
		adocumentActivity.replaceItemValue("txtBusinessRUle", script);

		// run plugin
		adocumentContext = rulePlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		// test single value
		String singelItem = adocumentContext.getItemValueString("single_item");
		Assert.assertEquals("Hello World", singelItem);

		// test multivalue
		List<String> multiItem = adocumentContext.getItemValue("multi_item");
		Assert.assertEquals(2, multiItem.size());
		Assert.assertEquals("Hello World", multiItem.get(0));
		Assert.assertEquals("Hello Imixs", multiItem.get(1));

	}

	/**
	 * This test tests if an activity ItemCollection can be updated by the script
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Test
	public void testUpdateActivityByScript() throws PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("txtName", "Anna");
		adocumentContext.setTaskID(1000);
		// simulate an activity
		ItemCollection event = new ItemCollection();
		event.replaceItemValue("keyMailEnabled", "1");

		// set a business rule
		String script = "";
		script += "var result={}; result.isValid =  1000==workitem.getTaskID();";
		// now add a manipulation!
		script += " event.setItemValue('keymailenabled','0');";

		logger.log(Level.INFO, "Script={0}", script);
		event.replaceItemValue("txtBusinessRUle", script);

		event.replaceItemValue("txtBusinessRuleEngine", "js");

		// run plugin
		adocumentContext = rulePlugin.run(adocumentContext, event);
		Assert.assertNotNull(adocumentContext);

		Assert.assertEquals(1000, adocumentContext.getTaskID());

		// test manipulation of activity
		Assert.assertEquals("0", event.getItemValueString("keyMailEnabled"));
		// test for integer value
		Assert.assertEquals(0, event.getItemValueInteger("keyMailEnabled"));

	}
}
