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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Named;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.imixs.workflow.exceptions.PluginException;

import jakarta.enterprise.context.RequestScoped;

/**
 * The Imixs RuleEngine is a CDI bean called by the WorkflowKernel to evaluate
 * business rules part of an BPMN model.
 * <p>
 * The engine is based on the GraalVM script engine which provides an advanced
 * polyglot language feature. This allows to evaluate scripts in different
 * programming languages (e.g. Java, JavaScript, Ruby, Python, R, LLVM,
 * WebAssembly, etc.).
 * <p>
 * From a BPMN Event element, the Script Language can be defined by the property
 * 'txtBusinessRuleEngine' or by a a comment added to the first line of a script
 * in the following format:
 * <p>
 * {@code // graalvm.languageId=js}
 * <p>
 * A Script can access all basic item values from the current workItem and also
 * the event by the provided member variables 'workitem' and 'event'.
 * <p>
 * The CDI bean can be replaced by an alternative CDI implementation to provide
 * an extended functionality.
 * <p>
 * NOTE: The implementation replaces the old RuleEngien which was based on the
 * Nashorn Script Engine. The engine to detect deprecated scripts and convert
 * them automatically into the new format. It is recommended to replace
 * deprecated scripts.
 * 
 * @author Ralph Soika
 * @version 4.0
 * 
 */
@Named
@RequestScoped
public class RuleEngine {
    public static final String DEFAULT_LANGUAGE_ID = "js";
    public static final String INVALID_SCRIPT = "INVALID_SCRIPT";
    private static final HashSet<Class<?>> BASIC_OBJECT_TYPES = getBasicObjectTypes();

    private static Logger logger = Logger.getLogger(RuleEngine.class.getName());

    private Context context = null;
    private String languageId;

    /**
     * This method initializes the default script engine.
     */
    public RuleEngine() {
        super();
        init(DEFAULT_LANGUAGE_ID);
    }

    /**
     * This method initializes the script engine.
     * 
     * @param scriptLanguage
     */
    public RuleEngine(final String languageID) {
        super();
        if (languageID == null || languageID.isEmpty()) {
            init(DEFAULT_LANGUAGE_ID);
        } else {
            init(languageID);
        }
    }

    /**
     * This method initializes a context with default configuration.
     * 
     * @param languageId
     */
    void init(final String languageId) {
        this.languageId = languageId;
        context = Context.newBuilder(languageId).allowAllAccess(true).build();
    }

    /**
     * Returns the current polyglot context
     * 
     * @return
     */
    public Context getContext() {
        return context;
    }

    /**
     * Sets the value of a member using an identifier. The member value is subject
     * to polyglot value mapping rules as described in Context.asValue(Object).
     * 
     * @param identifier
     * @param value
     */
    public void putMember(String identifier, Object value) {
        context.getBindings(languageId).putMember(identifier, value);
    }

    public Value eval(String script) {
        Value result = context.eval(languageId, script);
        return result;
    }

    /**
     * This method evaluates a boolean expression. An optional documentContext can
     * be provided as member Variables to be used by the script
     * 
     * @param documentContext optional workitem context
     * @return boolean
     * @throws PluginException
     */
    public boolean evaluateBooleanExpression(String script, ItemCollection workitem) throws PluginException {
        boolean debug = logger.isLoggable(Level.FINE);
        // test if a business rule is defined
        if ("".equals(script.trim()))
            return false; // nothing to do

        // set member variables...
        if (workitem != null) {
            putMember("workitem", workitem);
        }

        if (debug) {
            logger.finest("......SCRIPT:" + script);
        }

        // Test if we have a deprecated Script...
        if (RuleEngineNashornConverter.isDeprecatedScript(script)) {
            logger.warning("evaluate deprecated nashorn script");
            // here we rewrite the script as best as we can.
            script = RuleEngineNashornConverter.rewrite(script, workitem, null);
            logger.info("New Script: \n=========================\n" + script + "\n=========================");
        }

        Value result = null;
        try {
            result = eval(script);
        } catch (PolyglotException e) {
            logger.warning("Script Error in: " + script);
            // script not valid
            throw new PluginException(RuleEngine.class.getSimpleName(), INVALID_SCRIPT,
                    "BusinessRule contains invalid script:" + e.getMessage(), e);
        }

        return result.asBoolean();
    }

    /**
     * This method evaluates the business rule. The method returns the instance of
     * the evaluated result object which can be used to continue evaluation. If a
     * rule evaluation was not successful, the method returns null.
     * <p>
     * An optional documentContext and a event object can be provided as member
     * Variables to be used by the script
     * 
     * @param workitem optional document context
     * @param event    optional bpmn event context
     * @return evaluated result instance
     * @throws PluginException
     */
    public ItemCollection evaluateBusinessRule(String script, ItemCollection workitem, ItemCollection event)
            throws PluginException {
        boolean debug = logger.isLoggable(Level.FINE);
        // test if a business rule is defined
        if ("".equals(script.trim()))
            return null; // nothing to do

        // set member variables...
        if (workitem != null) {
            putMember("workitem", workitem);
        }
        if (event != null) {
            putMember("event", event);
        }

        if (debug) {
            logger.finest("......SCRIPT: " + script);
        }

        // Test if we have a deprecated Script...
        if (RuleEngineNashornConverter.isDeprecatedScript(script)) {
            logger.warning("evaluate deprecated nashorn script");
            // here we rewrite the script as best as we can.
            script = RuleEngineNashornConverter.rewrite(script, workitem, event);
            logger.info("New Script: \n=========================\n" + script + "\n=========================");
        }

        // evaluate the script....
        try {
            eval(script);
            // try to convert the result object, if provided...
            ItemCollection result = convertResult();
            return result;
        } catch (PolyglotException e) {
            logger.warning("Script Error: " + e.getMessage() + " in: " + script);
            // script not valid
            throw new PluginException(RuleEngine.class.getSimpleName(), INVALID_SCRIPT,
                    "BusinessRule contains invalid script:" + e.getMessage(), e);
        }

    }

    /**
     * This helper method converts the member variable 'result' of the current
     * context into a Map object and returns a new instance of a ItemCollection
     * holding the values of the map.
     * 
     * <code> var result={};result.name='xxx';result.count=42; <code>
     * 
     * @param engine
     * @return ItemCollection holding the item values of the variable or null if no
     *         variable with the given name exists or the variable has not
     *         properties.
     */
    @SuppressWarnings({ "rawtypes" })
    public ItemCollection convertResult() {
        Map mapResult = null;

        // do we have a result object?
        Value resultValue = context.getBindings(languageId).getMember("result");
        if (resultValue == null) {
            return null;
        }

        // try to convert the result object into a Map
        try {
            mapResult = resultValue.as(Map.class);
        } catch (ClassCastException | IllegalStateException | PolyglotException e) {
            logger.warning("Unable to convert result object to an ItemCollection");
            return null;
        }

        // Now build a new ItemCollection form the provided values...
        // we can not do a deep copy here because of the embedded polyglot value objects
        ItemCollection result = new ItemCollection();
        Iterator it = mapResult.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            String itemName = pair.getKey().toString();
            Object itemObject = pair.getValue();
            if (isBasicObjectType(itemObject.getClass())) {
                result.replaceItemValue(itemName, itemObject);
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
