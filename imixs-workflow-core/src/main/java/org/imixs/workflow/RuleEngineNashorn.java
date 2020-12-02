/*  
 *  Imixs-Workflow 
 *  
 *  Copyright (C) 2001-2020 Imixs Software Solutions GmbH,  
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
 *      https://www.imixs.org
 *      https://github.com/imixs/imixs-workflow
 *  
 *  Contributors:  
 *      Imixs Software Solutions GmbH - Project Management
 *      Ralph Soika - Software Developer
 */

package org.imixs.workflow;

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
import org.imixs.workflow.exceptions.PluginException;

/**
 * The Imixs RuleEngine evaluates a business rule provided by an Event.
 * 
 * A business rule can be written in any script language supported by the JVM.
 * The Script Language is defined by the property 'txtBusinessRuleEngine' from
 * the current Event element. The script is defined by the property
 * 'txtBusinessRule'.
 * 
 * The Script can access all basic item values from the current workItem and
 * also the event by the provided JSON objects 'workitem' and 'event'.
 * 
 * <code>
 *  // test first value of the workitem attribute 'txtname'
 *  var isValid = ('Anna'==workitem.txtname[0]);
 * </code>
 * 
 * A script can add new values for the current workitem by providing the JSON
 * object 'result'.
 * 
 * <code>
 *     var result={ someitem:'Hello World', somenumber:1};
 * </code>
 * 
 * Also change values of the event object can be made by the script. These
 * changes will be reflected back for further processing.
 * 
 * <code>
 *  // disable mail 
 *   event.keymailenabled='0';
 * </code>
 * 
 * A script can set the variables 'isValid' and 'followUp' to validate a
 * workItem or set a new followUp activity.
 * 
 * <code>
 *   result={ isValid:false };
 * </code>
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
 * NOTE: all variable names are case sensitive! All JSON object elements are
 * lower case!
 * 
 * @author Ralph Soika
 * @version 3.0
 * 
 */
public class RuleEngineNashorn {
    public static final String DEFAULT_SCRIPT_LANGUAGE = "javascript";
    public static final String INVALID_SCRIPT = "INVALID_SCRIPT";
    private static final HashSet<Class<?>> BASIC_OBJECT_TYPES = getBasicObjectTypes();

    private static Logger logger = Logger.getLogger(RuleEngineNashorn.class.getName());

    private ScriptEngineManager scriptEngineManager;
    private ScriptEngine scriptEngine = null;

    /**
     * This method initializes the default script engine.
     */
    public RuleEngineNashorn() {
        super();
        init(DEFAULT_SCRIPT_LANGUAGE);
    }

    /**
     * This method initializes the script engine.
     * 
     * @param scriptLanguage
     */
    public RuleEngineNashorn(final String scriptLanguage) {
        super();
        init(scriptLanguage);
    }

    /**
     * This method initializes the script engine.
     * 
     * @param scriptLanguage
     */
    void init(final String _scriptLanguage) {
        String scriptLanguage = _scriptLanguage;
        // set default engine to javascript if no engine is specified
        if ("".equals(scriptLanguage)) {
            scriptLanguage = DEFAULT_SCRIPT_LANGUAGE;
        }
        // initialize the script engine...
        scriptEngineManager = new ScriptEngineManager();
        scriptEngine = scriptEngineManager.getEngineByName(scriptLanguage);
    }

    /**
     * Returns the instance of the current scriptEngineManager
     * 
     * @return
     */
    public ScriptEngineManager getScriptEngineManager() {
        return scriptEngineManager;
    }

    /**
     * Returns the instance of the current ScriptEngine
     * 
     * @return
     */
    public ScriptEngine getScriptEngine() {
        return scriptEngine;
    }

    /**
     * This method evaluates the business rule defined by the provided event. The
     * method returns the instance of the evaluated result object which can be used
     * to continue evaluation. If a rule evaluation was not successful, the method
     * returns null.
     * 
     * @param adocumentContext
     * @param adocumentActivity
     * @return ScriptEngine instance
     * @throws PluginException
     */
    public ItemCollection evaluateBusinessRule(String script, ItemCollection documentContext, ItemCollection event)
            throws PluginException {
        boolean debug = logger.isLoggable(Level.FINE);
        // test if a business rule is defined
        if ("".equals(script.trim()))
            return null; // nothing to do

        // set activity properties into engine
        scriptEngine.put("event", convertItemCollection(event));
        scriptEngine.put("workitem", convertItemCollection(documentContext));
        if (debug) {
            logger.finest("......SCRIPT:" + script);
        }
        try {
            scriptEngine.eval(script);
        } catch (ScriptException e) {
            logger.warning("Script Error in: " + script);
            // script not valid
            throw new PluginException(RuleEngineNashorn.class.getSimpleName(), INVALID_SCRIPT,
                    "BusinessRule contains invalid script:" + e.getMessage(), e);
        }

        // get the optional result object
        ItemCollection result = convertScriptVariableToItemCollection("result");

        return result;
    }

    /**
     * This method converts a JSON String into a JavaScript JSON Object and
     * evaluates a script.
     * <p>
     * The JSON Object is set as a input variable named 'data' so that the script
     * can access the json structure in an easy way.
     * <p>
     * Example: <code>
     *   var result={}; result.name=data.name;
     * </code>
     * <p>
     * The method returns an ItemCollection with the result object.
     * 
     * @param json   - a JSON data string
     * @param script - a Script to be evaluated
     * @return an ItemCollection returning the Result Object.
     * @throws ScriptException
     */
    public ItemCollection evaluateJsonByScript(String json, String script) throws ScriptException {

        // create a data object
        scriptEngine.put("data", json);
        Object jsonDataObject = scriptEngine.eval("JSON.parse(data);");
        // set the parsed JSON object again as 'data'.
        scriptEngine.put("data", jsonDataObject);
        // evaluate the script
        scriptEngine.eval(script);
        // get the result object
        ItemCollection result = convertScriptVariableToItemCollection("result");
        return result;
    }

    /**
     * This method evaluates a boolean expression. The method takes a
     * documentContext as argument.
     * 
     * @param adocumentContext
     * @return ScriptEngine instance
     * @throws PluginException
     */
    public boolean evaluateBooleanExpression(String script, ItemCollection documentContext) throws PluginException {
        boolean debug = logger.isLoggable(Level.FINE);
        // test if a business rule is defined
        if ("".equals(script.trim()))
            return false; // nothing to do

        // set activity properties into engine
        scriptEngine.put("workitem", convertItemCollection(documentContext));

        if (debug) {
            logger.finest("......SCRIPT:" + script);
        }
        Object result = null;
        try {
            result = scriptEngine.eval(script);
        } catch (ScriptException e) {
            logger.warning("Script Error in: " + script);
            // script not valid
            throw new PluginException(RuleEngineNashorn.class.getSimpleName(), INVALID_SCRIPT,
                    "BusinessRule contains invalid script:" + e.getMessage(), e);
        }
        if (result instanceof Boolean) {
            return (boolean) result;
        } else {
            return false;
        }
    }

    /**
     * This method evaluates a script variable as an native Script array from the
     * script engine. The method returns a Object array with the variable values. If
     * the javaScript var is a String a new Array will be created. If the javaScript
     * var is a NativeArray the method tries to create a java List object.
     * 
     * See the following examples used by the Rhino JavaScript engine bundled with
     * Java 6. http://www.rgagnon.com/javadetails/java-0640.html
     * 
     * @return
     */
    public Object[] evaluateNativeScriptArray(String expression) {
        Object[] params = null;
        boolean debug = logger.isLoggable(Level.FINE);
        if (scriptEngine == null) {
            logger.severe("evaluateScritpObject error: no script engine! - call run()");
            return null;
        }

        // first test if expression is a basic string var
        Object objectResult = scriptEngine.get(expression);
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
            scriptEngine.eval(jsNashorn + jsCode);

            @SuppressWarnings("unchecked")
            List<Object> resultList = (List<Object>) scriptEngine.get("_evaluateScriptParam");
            if (resultList == null) {
                return null;
            }
            if ("[undefined]".equals(resultList.toString())) {
                return null;
            }
            // logging
            if (debug) {
                logger.finest("......evalueateScript object to Java");
                for (Object val : resultList) {
                    logger.finest("        " + val.toString());
                }
            }

            return resultList.toArray();
        } catch (ScriptException se) {
            // not convertable!
            // se.printStackTrace();
            if (debug) {
                logger.finest("......error evaluating " + expression + " - " + se.getMessage());
            }
            return null;
        }

    }

    /**
     * This method converts the values of an ItemCollection into a Map Object with
     * Arrays of Objects for each value
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

    /**
     * This method converts a JSON variable by name into a ItemCollection. The
     * variable is expected as a JSON object holding single values or arrays in the
     * following format
     * 
     * <code>
     * 
     * {'single_item':'Hello World', 'multi_item':[ 'Hello World', 'Hello Imixs' ]
     * };
     * 
     * <code>
     * 
     * The converted object is expected as an Map interface.
     * 
     * @param engine
     * @return ItemCollection holding the item values of the variable or null if no
     *         variable with the given name exists or the variable has not
     *         properties.
     * @throws ScriptException
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ItemCollection convertScriptVariableToItemCollection(String variable) {
        ItemCollection result = null;
        boolean debug = logger.isLoggable(Level.FINE);
        // get result object from engine
        Map<String, Object> scriptResult = (Map) scriptEngine.get(variable);
        // test if the json object exists and has child objects...
        if (scriptResult != null) {
            result = new ItemCollection();
            // evaluate values if available...
            if (scriptResult.entrySet().size() > 0) {
                // iterate over all entries
                for (Map.Entry<String, Object> entry : scriptResult.entrySet()) {

                    // test if the entry value is a single object or an array....
                    if (isBasicObjectType(entry.getValue().getClass())) {
                        // single value - build array....
                        if (debug) {
                            logger.finest("......adding " + variable + " property " + entry.getKey());
                        }
                        List<Object> list = new ArrayList();
                        list.add(entry.getValue());
                        result.replaceItemValue(entry.getKey(), list);
                    } else {
                        // test if array...
                        String expression = "result['" + entry.getKey() + "']";
                        Object[] oScript = evaluateNativeScriptArray(expression);
                        if (oScript == null) {
                            continue;
                        }
                        if (debug) {
                            logger.finest("......adding " + variable + " property " + entry.getKey());
                        }
                        List<?> list = new ArrayList(Arrays.asList(oScript));
                        result.replaceItemValue(entry.getKey(), list);
                    }
                }
            }
        }
        return result;
    }

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
