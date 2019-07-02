package org.imixs.workflow.plugins;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.plugins.RulePlugin;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Test class for RulePlugin
 * 
 * @author rsoika
 */
public class TestRulePlugin {
	protected RulePlugin rulePlugin = null;

	@Before
	public void setup() throws PluginException {
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
	public void testBasicScript() throws ScriptException, PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("txtName", "Anna");
		ItemCollection adocumentActivity = new ItemCollection();

		// set a business rule
		String script = "var a=1;var b=2;var test = ((a<b) && 'Anna'==workitem.get('txtname')[0]);";

		System.out.println("Script=" + script);
		adocumentActivity.replaceItemValue("txtBusinessRUle", script);

		// run plugin
		adocumentContext = rulePlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		Assert.assertEquals("Anna", adocumentContext.getItemValueString("txtName"));

	}

	/**
	 * This test verifies the evaluation of a simple script unsing the json objects.
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Test
	public void testBasicScriptJson() throws ScriptException, PluginException {

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
	 * This test verifies the isValid cases
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	/*
	 * @Test public void testIsValid() throws ScriptException, PluginException {
	 * 
	 * ItemCollection adocumentContext = new ItemCollection();
	 * adocumentContext.replaceItemValue("txtName", "Anna"); ItemCollection
	 * adocumentActivity = new ItemCollection();
	 * 
	 * // 1) test without any script:
	 * adocumentActivity.replaceItemValue("txtBusinessRUle", null);
	 * Assert.assertTrue(rulePlugin.isValid(adocumentContext, adocumentActivity));
	 * 
	 * // 2) test with an empty script:
	 * adocumentActivity.replaceItemValue("txtBusinessRUle", "");
	 * Assert.assertTrue(rulePlugin.isValid(adocumentContext, adocumentActivity));
	 * 
	 * // 3) test script without isValid variable String script =
	 * "var a=1;var b=2;"; System.out.println("Script=" + script);
	 * adocumentActivity.replaceItemValue("txtBusinessRUle", script);
	 * Assert.assertTrue(rulePlugin.isValid(adocumentContext, adocumentActivity));
	 * 
	 * // 2) test true case script =
	 * "var a=1;var b=2;var isValid = ((a<b) && 'Anna'==txtname[0]);";
	 * System.out.println("Script=" + script);
	 * adocumentActivity.replaceItemValue("txtBusinessRUle", script);
	 * Assert.assertTrue(rulePlugin.isValid(adocumentContext, adocumentActivity));
	 * 
	 * // 2) test false case script =
	 * "var a=1;var b=2;var isValid = ((a>b) && 'Anna'==txtname[0]);";
	 * System.out.println("Script=" + script);
	 * adocumentActivity.replaceItemValue("txtBusinessRUle", script);
	 * Assert.assertFalse(rulePlugin.isValid(adocumentContext, adocumentActivity));
	 * 
	 * }
	 */

	/**
	 * This test verifies if in case of isValid==false a PluginExeption is thrown
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Test(expected = PluginException.class)
	public void testSimplePluginException() throws ScriptException, PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		ItemCollection adocumentActivity = new ItemCollection();

		// set a business rule
		String script = "var a=1;var b=2;var isValid = (a>b);";

		System.out.println("Script=" + script);
		adocumentActivity.replaceItemValue("txtBusinessRUle", script);
		rulePlugin.run(adocumentContext, adocumentActivity);
		Assert.fail();

	}

	@Test(expected = PluginException.class)
	public void testResultObjectPluginException() throws ScriptException, PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		ItemCollection adocumentActivity = new ItemCollection();

		// set a business rule
		String script = "var result={ isValid:false };";

		System.out.println("Script=" + script);
		adocumentActivity.replaceItemValue("txtBusinessRUle", script);
		rulePlugin.run(adocumentContext, adocumentActivity);
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
	public void testComplexPluginException() throws ScriptException {

		ItemCollection adocumentContext = new ItemCollection();
		ItemCollection adocumentActivity = new ItemCollection();

		// 1) invalid returning one messsage
		String script = "var a=1;var b=2;var isValid = (a>b);" + " var errorCode='MY_ERROR';"
				+ " var errorMessage='Somehing go wrong!';";

		System.out.println("Script=" + script);
		adocumentActivity.replaceItemValue("txtBusinessRUle", script);
		try {
			rulePlugin.run(adocumentContext, adocumentActivity);
			Assert.fail();
		} catch (PluginException e) {
			// test excption
			Assert.assertEquals("MY_ERROR", e.getErrorCode());
			Object[] params = e.getErrorParameters();
			Assert.assertEquals(1, params.length);
			Assert.assertEquals("Somehing go wrong!", params[0].toString());
		}

		// 2) invalid returning 2 messages in an array
		script = "var a=1;var b=2;var isValid = (a>b);" + " var errorMessage = new Array();"
				+ " errorMessage[0]='Somehing go wrong!';" + " errorMessage[1]='Somehingelse go wrong!';";

		System.out.println("Script=" + script);
		adocumentActivity.replaceItemValue("txtBusinessRUle", script);
		try {
			rulePlugin.run(adocumentContext, adocumentActivity);
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
	 * This test verifies the follUp behavior. If set then keyFollowUp and
	 * numNextActivity should be overwritten by the RulePlugin
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Test
	public void testFollowUpActivity() throws ScriptException, PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		ItemCollection adocumentActivity = new ItemCollection();

		// set a business rule
		String script = "var a=1.0;var b=2;var followUp =a+b;";

		System.out.println("Script=" + script);
		adocumentActivity.replaceItemValue("txtBusinessRUle", script);

		// run plugin
		adocumentContext = rulePlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		String sFllowUp = adocumentActivity.getItemValueString("keyFollowUp");
		int followUp = adocumentActivity.getItemValueInteger("numNextActivityID");

		Assert.assertEquals("1", sFllowUp);

		Assert.assertEquals(followUp, 3);

	}

	



	/**
	 * only to evaluate some behavior
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Test
	public void simpleApprovalTest() throws ScriptException, PluginException {

		// set a business rule
		// workitem.get(refField1)[0])
		String script = " var followUp=null;" + " if (workitem._amount_brutto[0]>5000)" + "    followUp=90;";
		System.out.println("Script=" + script);

		ItemCollection adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("_amount_brutto", new Double(6000));
		ItemCollection adocumentActivity = new ItemCollection();

		adocumentActivity.replaceItemValue("txtBusinessRUle", script);

		// run plugin
		adocumentContext = rulePlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);
		String sFllowUp = adocumentActivity.getItemValueString("keyFollowUp");
		int followUp = adocumentActivity.getItemValueInteger("numNextActivityID");

		Assert.assertEquals("1", sFllowUp);

		Assert.assertEquals(90, followUp);

		/*
		 * Case 2
		 */

		adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("_amount_brutto", new Double(3000));
		adocumentActivity = new ItemCollection();

		adocumentActivity.replaceItemValue("txtBusinessRUle", script);

		// run plugin
		adocumentContext = rulePlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		sFllowUp = adocumentActivity.getItemValueString("keyFollowUp");
		followUp = adocumentActivity.getItemValueInteger("numNextActivityID");

		Assert.assertEquals("", sFllowUp);

		Assert.assertEquals(0, followUp);

	}

	/**
	 * This test verifies the BigDecimal support of the RulePlugin
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Test
	public void bigDecimalTest() throws ScriptException, PluginException {

		// set a business rule
		String script = " var followUp=null;" + " if (workitem._amount_brutto[0]>5000.50)" + "    followUp=90;";
		System.out.println("Script=" + script);

		ItemCollection adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("_amount_brutto", BigDecimal.valueOf(5000.51d));
		ItemCollection adocumentActivity = new ItemCollection();

		adocumentActivity.replaceItemValue("txtBusinessRUle", script);

		// run plugin
		adocumentContext = rulePlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		String sFllowUp = adocumentActivity.getItemValueString("keyFollowUp");
		int followUp = adocumentActivity.getItemValueInteger("numNextActivityID");

		Assert.assertEquals("1", sFllowUp);

		Assert.assertEquals(90, followUp);

	}

	/**
	 * See: http://www.rgagnon.com/javadetails/java-0640.html
	 */
	@Test
	public void testArray() {

		ScriptEngineManager mgr = new ScriptEngineManager();
		// we are using the rhino javascript engine
		ScriptEngine engine = mgr.getEngineByName("javascript");

		// pass a Java collection to javascript
		List<String> list1 = Arrays.asList("Homer", "Bart", "Marge", "Maggie", "Lisa");
		engine.put("list1", list1);

		// Nashorn: check for importClass function and then load if missing ...
		String jsNashorn = " if (typeof importClass != 'function') { load('nashorn:mozilla_compat.js');}";

		String jsCode = "var index; " + "var values =list1.toArray();" + "print('*** Java object to Javascript');"
				+ "for(index in values) {" + "  print(values[index]);" + "}";
		try {
			engine.eval(jsNashorn + jsCode);
		} catch (ScriptException se) {
			se.printStackTrace();
		}

		// pass a collection from javascript to java
		jsCode = "importPackage(java.util);" + "var list2 = Arrays.asList(['Moe', 'Barney', 'Ned']); ";
		try {
			engine.eval(jsNashorn + jsCode);
		} catch (ScriptException se) {
			se.printStackTrace();
		}

		@SuppressWarnings("unchecked")
		List<String> list2 = (List<String>) engine.get("list2");
		System.out.println("*** Javascript object to Java");
		for (String val : list2) {
			System.out.println(val);
		}

	}

	/**
	 * This test tests if a the properties of an activity entity can be evaluated by
	 * a script
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Test
	public void testEventObjectByScript() throws ScriptException, PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("txtName", "Anna");

		// simulate an activity
		ItemCollection adocumentActivity = new ItemCollection();
		adocumentActivity.replaceItemValue("keyMailEnabled", "1");

		// set a business rule
		String script = "var isValid =  '1'==event.keymailenabled[0];";

		System.out.println("Script=" + script);
		adocumentActivity.replaceItemValue("txtBusinessRUle", script);

		// run plugin
		adocumentContext = rulePlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

	}
	
	
	/**
	 * This test tests if a a scipt can inject new properties into the current activity entity 
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Test
	public void testInjectItemIntoEventObjectByScript() throws ScriptException, PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("txtName", "Anna");

		// simulate an activity
		ItemCollection event = new ItemCollection();
		
		// set a business rule
		String script = "var result={}; event.nammailreplytouser='test@me.com';";

		System.out.println("Script=" + script);
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
	public void testChangeEventObejctByScript() throws ScriptException, PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("txtName", "Anna");

		// simulate an activity
		ItemCollection adocumentActivity = new ItemCollection();
		adocumentActivity.replaceItemValue("keyMailEnabled", "1");

		// set a business rule
		String script = "var isValid =event.keymailenabled[0]=='1'; event.keymailenabled='0';";

		System.out.println("Script=" + script);
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
	public void testSimpleWorkitemScript() throws ScriptException, PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("txtName", "Anna");
		adocumentContext.setTaskID(1000);
		// simulate an activity
		ItemCollection adocumentActivity = new ItemCollection();
		adocumentActivity.replaceItemValue("keyMailEnabled", "1");

		// set a business rule
		String script = "var isValid =  1000==workitem.get('$taskid')[0];";

		System.out.println("Script=" + script);
		adocumentActivity.replaceItemValue("txtBusinessRUle", script);

		// run plugin
		adocumentContext = rulePlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

	}
	
	/**
	 * Same test as before but using the deprected item $processid
	 */
	@Test
	public void testSimpleWorkitemScriptWithDeprecatedField() throws ScriptException, PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("txtName", "Anna");
		adocumentContext.setTaskID(1000);
		// simulate an activity
		ItemCollection adocumentActivity = new ItemCollection();
		adocumentActivity.replaceItemValue("keyMailEnabled", "1");

		// set a business rule
		// String script = "var isValid = 1000==workitem.get('$processid')[0];";
		String script = "var isValid =  1000==workitem.get('$processid')[0];";

		System.out.println("Script=" + script);
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
	public void testIsValidDate() throws ScriptException, PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("datDate", new Date());
		ItemCollection adocumentActivity = new ItemCollection();

		// 2) test true case
		String script = " var refField1=\"_contact\";" + " var refField2=\"datdate\";" + " var isValid=true;"
				+ " if (   ( workitem.get(refField2) == null)   ) {" + "     isValid=false;"
				+ "     var errorMessage='Bitte geben Sie ein Datum fuer das Zahlungsziel an!';" + " }  ";
		System.out.println("Script=" + script);
		adocumentActivity.replaceItemValue("txtBusinessRUle", script);

		// run plugin
		adocumentContext = rulePlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		// Assert.assertTrue(rulePlugin.isValid(adocumentContext,
		// adocumentActivity));

		// 2) test false case
		adocumentContext = new ItemCollection();
		adocumentActivity = new ItemCollection();
		script = " var refField1=\"_contact\";" + " var refField2=\"datdate\";" + " var isValid=true;"
				+ " if (   ( workitem.get(refField2) == null)   ) {" + "     isValid=false;"
				+ "     var errorMessage='Bitte geben Sie ein Datum fuer das Zahlungsziel an!';" + " }  ";
		System.out.println("Script=" + script);
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
		script = " var refField1=\"_contact\";" + " var refField2=\"datdate\";" + " var isValid=true;"
				+ " if (   ( workitem.get(refField2) == null)   ) {" + "     isValid=false;"
				+ "     var errorMessage='Bitte geben Sie ein Datum fuer das Zahlungsziel an!';" + " }  ";
		System.out.println("Script=" + script);
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
	public void testIsValidTwoFields() throws ScriptException, PluginException {
		ItemCollection adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("txtbetrag", "5,55");
		adocumentContext.replaceItemValue("txtgutschift", "5,55");
		ItemCollection adocumentActivity = new ItemCollection();

		// 2) test true case
		String script = " var refField1=\"txtbetrag\";" + "		 var refField2=\"txtgutschift\";"
				+ "		 var isValid=true;"
				+ "		 if ( ( workitem.get(refField1) == null || ''==workitem.get(refField1)[0]) || ( workitem.get(refField2) == null || ''==workitem.get(refField2)[0])) {"
				+ "		     isValid=false;" + "		     var errorMessage='Bitte geben Sie den Betrag an! ';"
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
	public void testUndefinedErrorCode() throws ScriptException, PluginException {
		ItemCollection adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("_subject", "test");
		ItemCollection adocumentActivity = new ItemCollection();

		System.out.println("testUndefineErroCode - test case 1:");
		// 2) test undefined case
		String script = " var isValid=true;" + " var errorCode,errorMessage;" + "refField='_contact';"
				+ "if ( workitem.get(refField) == null || workitem.get(refField)[0] == ''  ) {" + "     isValid=false;"
				+ "     errorMessage='Please enter subject';" + " }";

		adocumentActivity.replaceItemValue("txtBusinessRule", script);

		try {
			rulePlugin.run(adocumentContext, adocumentActivity);

			Assert.fail();
		} catch (PluginException pe) {
			// PluginException expected
			System.out.println(pe.getMessage());
			Assert.assertEquals(pe.getErrorCode(), RulePlugin.VALIDATION_ERROR);

			Object[] errorParams = pe.getErrorParameters();

			Assert.assertEquals(1, errorParams.length);
			Assert.assertEquals("Please enter subject", errorParams[0]);

		}

		System.out.println("testUndefineErroCode - test case 2:");

		// test the same case if errorCode is defined
		// 2) test true case
		script = " var isValid=true;" + " var errorCode,errorMessage;" + "refField='_contact';"
				+ "if ( workitem.get(refField) == null || workitem.get(refField)[0] == ''  ) {" + "     isValid=false;"
				+ "     errorCode='SOME_ERROR';" + " }";

		adocumentActivity.replaceItemValue("txtBusinessRule", script);

		try {
			rulePlugin.run(adocumentContext, adocumentActivity);

			Assert.fail();
		} catch (PluginException pe) {
			// PluginException expected
			System.out.println(pe.getMessage());
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
	public void testResultObjectJSON() throws ScriptException, PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		ItemCollection adocumentActivity = new ItemCollection();

		// set a business rule
		String script = "var result={ someitem:'Hello World', somenumber:1};";

		System.out.println("Script=" + script);
		adocumentActivity.replaceItemValue("txtBusinessRUle", script);

		// run plugin
		adocumentContext = rulePlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		Assert.assertEquals("Hello World", adocumentContext.getItemValueString("someitem"));

		Assert.assertEquals(1, adocumentContext.getItemValueInteger("somenumber"));

	}

	
	/**
	 * This test verifies the follUp behavior. If set then keyFollowUp and
	 * numNextActivity should be overwritten by the RulePlugin
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Test
	public void testResultObjectFollowUpActivity() throws ScriptException, PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		ItemCollection adocumentActivity = new ItemCollection();

		// set a business rule
		String script = "var a=1.0;var b=2;var result={}; result.followUp =a+b;";

		System.out.println("Script=" + script);
		adocumentActivity.replaceItemValue("txtBusinessRUle", script);

		// run plugin
		adocumentContext = rulePlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		String sFllowUp = adocumentActivity.getItemValueString("keyFollowUp");
		int followUp = adocumentActivity.getItemValueInteger("numNextActivityID");

		Assert.assertEquals("1", sFllowUp);

		Assert.assertEquals(followUp, 3);

	}

	

	/**
	 * This test verifies setting a new value via the result object
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Test
	public void testResultObjectNewValue() throws ScriptException, PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		ItemCollection adocumentActivity = new ItemCollection();

		// set a business rule
		String script = "var a=1.0;var b=2;var result={}; result.someitem='Hello World'; result.somenumber=1";

		System.out.println("Script=" + script);
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
	public void testResultObjectNewValueList() throws ScriptException, PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		ItemCollection adocumentActivity = new ItemCollection();

		// set a business rule
		String script = "var a=1.0;var b=2;var result={}; result.some_item=[]; result.some_item[0]='Hello World'; result.some_item[1]='Hello Imixs';";

		System.out.println("Script=" + script);
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
	public void testResultObjectNewValueListAsJSON() throws ScriptException, PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		ItemCollection adocumentActivity = new ItemCollection();

		// set a business rule
		String script = "var a=1.0;var b=2;var result={'single_item':'Hello World', 'multi_item':['Hello World','Hello Imixs']};";

		System.out.println("Script=" + script);
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
	 * This test tests if the workitem object can be used as an JSON object - using
	 * the nahorn javascript engine
	 * 
	 * This test is only runable on JDK-8
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Ignore
	@Test
	public void testAccessWorkitembyJSON() throws ScriptException, PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("txtName", "Anna");
		adocumentContext.replaceItemValue("$ProcessID", 1000);
		// simulate an activity
		ItemCollection adocumentActivity = new ItemCollection();
		adocumentActivity.replaceItemValue("keyMailEnabled", "1");

		// set a business rule
		String script = "";
		script += "var isValid =  'Anna'==workitem.txtname[0];";

		System.out.println("Script=" + script);
		adocumentActivity.replaceItemValue("txtBusinessRUle", script);

		adocumentActivity.replaceItemValue("txtBusinessRuleEngine", "rhino");

		// run plugin
		adocumentContext = rulePlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

	}

	/**
	 * This test tests if an activity ItemCollection can be updated by the script
	 * 
	 * @throws ScriptException
	 * @throws PluginException
	 */
	@Test
	public void testUpdateActivityByScript() throws ScriptException, PluginException {

		ItemCollection adocumentContext = new ItemCollection();
		adocumentContext.replaceItemValue("txtName", "Anna");
		adocumentContext.setTaskID(1000);
		// simulate an activity
		ItemCollection adocumentActivity = new ItemCollection();
		adocumentActivity.replaceItemValue("keyMailEnabled", "1");

		// set a business rule
		String script = "";
		script += "var isValid =  1000==workitem['$taskid'][0];";
		// now add a manipulation!
		script += " event.keymailenabled='0';";

		System.out.println("Script=" + script);
		adocumentActivity.replaceItemValue("txtBusinessRUle", script);

		adocumentActivity.replaceItemValue("txtBusinessRuleEngine", "nashorn");

		// run plugin
		adocumentContext = rulePlugin.run(adocumentContext, adocumentActivity);
		Assert.assertNotNull(adocumentContext);

		Assert.assertEquals(1000, adocumentContext.getTaskID());

		// test manipulation of activity
		Assert.assertEquals("0", adocumentActivity.getItemValueString("keyMailEnabled"));
		// test for integer value
		Assert.assertEquals(0, adocumentActivity.getItemValueInteger("keyMailEnabled"));

	}
}
