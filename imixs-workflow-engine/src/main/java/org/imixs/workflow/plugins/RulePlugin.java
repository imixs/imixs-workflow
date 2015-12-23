/*******************************************************************************
 *  Imixs Workflow 
 *  Copyright (C) 2001, 2011 Imixs Software Solutions GmbH,  
 *  http://www.imixs.com
 *  
 *  This program is free software; you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License 
 *  as published by the Free Software Foundation; either version 2 
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 *  General Public License for more details.
 *  
 *  You can receive a copy of the GNU General Public
 *  License at http://www.gnu.org/licenses/gpl.html
 *  
 *  Project: 
 *  	http://www.imixs.org
 *  	http://java.net/projects/imixs-workflow
 *  
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika - Software Developer
 *******************************************************************************/

package org.imixs.workflow.plugins;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.exceptions.PluginException;

/**
 * The Imixs Rule Plugin evaluates a business rule provided by the current
 * ActiviyEntity.
 * 
 * A business rule can be written in any script language supported by the JVM.
 * The Script Language is defined by the property 'txtBusinessRuleEngine' from
 * the current ActivityEntity. The script is defined by the property
 * 'txtBusinessRule'.
 * 
 * The Script can access all basic item values from the current workItem and
 * also the activity entity by the provided map object 'workitem' and
 * 'activity'. But the script may not update any of and workitem.
 * 
 * <code>
 *  // test first value of the workitem attribute 'txtname'
 *  var isValid = ('Anna'==workitem.get('txtname')[0]);
 * </code>
 * 
 * A script may change values of the activity map object. These changes will be
 * reflected back to the current ActivityEntity which can be used for further
 * processing.
 * 
 * <code>
 *  // disable mail 
 *   activity.put('keymailinactive',['0']);
 * </code>
 * 
 * A script can set the variables 'isValid' and 'followUp' to validate a
 * workItem or set a new followUp activity.
 * 
 * If the script set the variable 'isValid' to false then the plugin throws a
 * PluginExcpetion. The Plugin evaluates the variables 'errorCode' and
 * errorMessage. If these variables are set by the Script then the
 * PluginException will be updates with the corresponding errorCode and the
 * 'errorMessage' as params[]. If no errorCode is set then the errorCode of the
 * PluginException will default to 'VALIDATION_ERROR'.
 * 
 * If the script set the variable 'followUp' the follow-up behavior of the
 * current ActivityEntity will be updated.
 * 
 * If a script can not be evaluated by the scriptEngin a PluginExcpetion with
 * the errorCode 'INVALID_SCRIPT' will be thrown.
 * 
 * NOTE: all variable names are case sensitive!
 * 
 * @author Ralph Soika
 * @version 2.0
 * 
 */

public class RulePlugin extends AbstractPlugin {

	public static final String INVALID_SCRIPT = "INVALID_SCRIPT";
	public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
	private static Logger logger = Logger.getLogger(RulePlugin.class.getName());

	@Override
	public void init(WorkflowContext actx) throws PluginException {
		super.init(actx);
	}

	/**
	 * The run method evaluates a script provided by an activityEntity with the
	 * specified scriptEngine.
	 * 
	 * After successful evaluation the method verifies the object value
	 * 'isValid'. If isValid is false then the method throws a PluginException.
	 * In addition the method evaluates the object values 'errorCode' and
	 * 'errorMessage' which will be part of the PluginException.
	 * 
	 * If 'isValid' is true or undefined the method evaluates the object value
	 * 'followUp'. If a followUp value is defined by the script the method
	 * update the model follow up definition.
	 * 
	 * If a script changes properties of the activity entity the method will
	 * evaluate these changes and update the ItemCollection for further
	 * processing.
	 * 
	 */
	public int run(ItemCollection adocumentContext, ItemCollection adocumentActivity) throws PluginException {

		ScriptEngine engine = evaluateBusinessRule(adocumentContext, adocumentActivity);
		if (engine != null) {

			Boolean isValidActivity = (Boolean) engine.get("isValid");

			if (isValidActivity != null && !isValidActivity) {
				// test if a error code is provided!
				String sErrorCode = VALIDATION_ERROR;
				Object oErrorCode = engine.get("errorCode");
				if (oErrorCode != null && oErrorCode instanceof String) {
					sErrorCode = oErrorCode.toString();
				}

				// next test for errorMessage (this can be a string or an array
				// of strings
				Object[] params = null;
				params = this.evaluateScriptObject(engine, "errorMessage");
				// finally throw a Plugin Exception
				throw new PluginException(RulePlugin.class.getName(), sErrorCode,
						"BusinessRule: validation failed - ErrorCode=" + sErrorCode, params);
			}

			// Now update Activity Entity values from script..
			updateActivityEntity(engine, adocumentActivity);

			// now test the followUp variable
			Object o = engine.get("followUp");
			if (o != null) {
				// try to get double value...
				Double d = Double.valueOf(o.toString());
				Long followUpActivity = d.longValue();
				if (followUpActivity != null && followUpActivity > 0) {
					adocumentActivity.replaceItemValue("keyFollowUp", "1");
					adocumentActivity.replaceItemValue("numNextActivityID", followUpActivity);

				}
			}

			// now test the nextTask variable
			o = engine.get("nextTask");
			if (o != null) {
				// try to get double value...
				Double d = Double.valueOf(o.toString());
				Long nextTask = d.longValue();
				if (nextTask != null && nextTask > 0) {
					adocumentActivity.replaceItemValue("numNextProcessID", nextTask);

				}
			}

			return Plugin.PLUGIN_OK;

		} else
			// no business rule is defined
			return Plugin.PLUGIN_OK; // nothing to do

	}

	/**
	 * nothing to do
	 */
	@Override
	public void close(int status) {
	}

	/**
	 * This method evaluates a script and test the object value 'isValid'. The
	 * documentContext is valid for the current activity if 'isValid' is not
	 * 'false'
	 * 
	 * @param adocumentContext
	 * @param adocumentActivity
	 * @return
	 * @throws PluginException
	 */
	public boolean isValid(ItemCollection documentContext, ItemCollection activity) throws PluginException {

		ScriptEngine engine = evaluateBusinessRule(documentContext, activity);
		if (engine != null) {
			Boolean isValidActivity = (Boolean) engine.get("isValid");
			if (isValidActivity != null)
				return isValidActivity;

		}
		// no rule defined
		return true;
	}

	/**
	 * This method evaluates the business rule defined by the provided activity.
	 * The method returns the instance of the script engine which can be used to
	 * continue evaluation. If a rule evaluation was not successful, the method
	 * returns null.
	 * 
	 * @param adocumentContext
	 * @param adocumentActivity
	 * @return ScriptEngine instance
	 * @throws PluginException
	 */
	public ScriptEngine evaluateBusinessRule(ItemCollection documentContext, ItemCollection activity)
			throws PluginException {

		// test if a business rule is defined
		String script = activity.getItemValueString("txtBusinessRule");
		if ("".equals(script.trim()))
			return null; // nothing to do

		// initialize the script engine...
		ScriptEngineManager manager = new ScriptEngineManager();
		String sEngineType = activity.getItemValueString("txtBusinessRuleEngine");
		// set default engine to javascript if no engine is specified
		if ("".equals(sEngineType))
			sEngineType = "javascript";

		ScriptEngine engine = manager.getEngineByName(sEngineType);

		// set activity properties into engine
		engine.put("activity", convertItemCollection(activity));
		engine.put("workitem", convertItemCollection(documentContext));

		// The following code is only for backward compatibility since version
		// 3.1.9
		// setup document data...
		Map<String, List<Object>> itemList = documentContext.getAllItems();
		for (Map.Entry<String, List<Object>> entry : itemList.entrySet()) {
			String key = entry.getKey().toLowerCase();
			List<?> value = (List<?>) entry.getValue();
			// do only put basic values and not values starting the $
			if (!key.startsWith("$") && value.size() > 0) {
				if (isBasicObjectType(value.get(0).getClass())) {
					engine.put(key, value.toArray());
				}
			}
		}

		logger.fine("SCRIPT:" + script);
		try {
			engine.eval(script);
		} catch (ScriptException e) {
			// script not valid
			throw new PluginException(RulePlugin.class.getSimpleName(), INVALID_SCRIPT,
					"BusinessRule contains invalid script:" + e.getMessage(), e);
		}

		return engine;
	}

	/**
	 * This method evaluates script variable from the script engine. The method
	 * returns a Object array with the variable value. If the javaScript var is
	 * a String a new Array will be created. If the javaScript var is a
	 * NativeArray the method tries to create a java List object.
	 * 
	 * See the following examples used by the Rhino JavaScript engine bundled
	 * with Java 6. http://www.rgagnon.com/javadetails/java-0640.html
	 * 
	 * @return
	 */
	public Object[] evaluateScriptObject(ScriptEngine engine, String expression) {
		Object[] params = null;

		if (engine == null) {
			logger.severe("RulePlugin evaluateScritpObject error: no script engine! - call run()");
			return null;
		}

		// first test if expression is a basic string var
		Object objectResult = engine.get(expression);
		if (objectResult != null && objectResult instanceof String) {
			// just return a simple array with one value
			params = new String[1];
			params[0] = objectResult.toString();
			return params;
		}

		// now try to pass the object to engine and convert it into a
		// ArryList....
		try {
			// Nashorn: check for importClass function and then load if missing
			// See: issue #124
			String jsNashorn = " if (typeof importClass != 'function') { load('nashorn:mozilla_compat.js');}";

			String jsCode = "importPackage(java.util);" + "var _evaluateScriptParam = Arrays.asList(" + expression
					+ "); ";
			// pass a collection from javascript to java;
			engine.eval(jsNashorn + jsCode);

			@SuppressWarnings("unchecked")
			List<Object> resultList = (List<Object>) engine.get("_evaluateScriptParam");
			if (resultList==null) {
				return null;
			}
			if ("[undefined]".equals(resultList.toString())) {
				return null;
			}
			// logging
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("evalueateScript object to Java");
				for (Object val : resultList) {
					logger.fine(val.toString());
				}
			}

			return resultList.toArray();
		} catch (ScriptException se) {
			// not convertable!
			// se.printStackTrace();
			logger.fine("[RulePlugin] error evaluating " + expression + " - " + se.getMessage());
			return null;
		}

	}

	/**
	 * This method compares the properties of the script element 'activity' with
	 * the values of the current ActivityEntity. If a value has changed, then
	 * the method will update the Activity ItemCollection which can be used for
	 * further processing.
	 * 
	 * @param engine
	 * @param adocumentActivity
	 * @throws ScriptException
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void updateActivityEntity(ScriptEngine engine, ItemCollection adocumentActivity) {

		Map<String, Object[]> orginalActivity = convertItemCollection(adocumentActivity);
		// get activity from engine
		Map<String, Object[]> scriptActivity = (Map) engine.get("activity");

		// iterate over all entries
		for (Map.Entry<String, Object[]> entry : scriptActivity.entrySet()) {

			String expression = "activity.get('" + entry.getKey() + "')";

			Object[] oScript = evaluateScriptObject(engine, expression);
			if (oScript == null) {
				continue;
			}
			Object[] oActivity = orginalActivity.get(entry.getKey());
			if (oActivity == null) {
				continue;
			}

			// compare object arrays with deepEquals....
			if (!Arrays.deepEquals(oScript, oActivity)) {
				logger.fine("[RulePlugin] update activity proeperty " + entry.getKey());
				List<?> list = new ArrayList(Arrays.asList(oScript));
				adocumentActivity.replaceItemValue(entry.getKey(), list);
			}

		}

	}

	/**
	 * This method converts the values of an ItemCollection into a Map Object
	 * with Arrays of Objects for each value
	 * 
	 * @param itemCol
	 * @return
	 */
	private Map<String, Object[]> convertItemCollection(ItemCollection itemCol) {
		Map<String, Object[]> result = new HashMap<String, Object[]>();
		Map<String, List<Object>> itemList = itemCol.getAllItems();
		for (Map.Entry<String, List<Object>> entry : itemList.entrySet()) {
			String key = entry.getKey().toLowerCase();
			List<?> value = (List<?>) entry.getValue();
			// do only put basic values
			if (value.size() > 0) {
				if (isBasicObjectType(value.get(0).getClass())) {
					result.put(key, value.toArray());
				}

			}
		}
		return result;
	}

	private static final HashSet<Class<?>> BASIC_OBJECT_TYPES = getBasicObjectTypes();

	private static boolean isBasicObjectType(Class<?> clazz) {
		return BASIC_OBJECT_TYPES.contains(clazz);
	}

	private static HashSet<Class<?>> getBasicObjectTypes() {
		HashSet<Class<?>> ret = new HashSet<Class<?>>();
		ret.add(Boolean.class);
		ret.add(Character.class);
		ret.add(Byte.class);
		ret.add(Short.class);
		ret.add(Integer.class);
		ret.add(Long.class);
		ret.add(Float.class);
		ret.add(Double.class);
		ret.add(Void.class);

		ret.add(BigDecimal.class);
		ret.add(BigInteger.class);

		ret.add(String.class);
		ret.add(Object.class);
		ret.add(Date.class);
		ret.add(Calendar.class);
		ret.add(GregorianCalendar.class);

		return ret;
	}
}
