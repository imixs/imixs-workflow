/****************************************************************************
 * Copyright (c) 2022-2025 Imixs Software Solutions GmbH and others.
 * https://www.imixs.com
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * This Source Code may also be made available under the terms of the
 * GNU General Public License, version 2 or later (GPL-2.0-or-later),
 * which is available at https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-or-later
 ****************************************************************************/

package org.imixs.workflow;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.imixs.workflow.exceptions.PluginException;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

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

    private static final Logger logger = Logger.getLogger(RuleEngine.class.getName());

    private Context _context = null;
    private String languageId;

    /**
     * This method initializes the default script engine.
     */
    public RuleEngine() {
        super();
        init(DEFAULT_LANGUAGE_ID);
    }

    /**
     * This method initializes the script engine with a specific languageId.
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
        // lazy initialization - see getContext()
        _context = null;
    }

    /**
     * Returns the current polyglot context
     * 
     * This method implements a lazy initialization of the context.
     * See Issue #822
     * 
     * We also set the option 'WarnInterpreterOnly' to false.
     * See also here: https://www.graalvm.org/22.0/reference-manual/js/FAQ/
     * 
     * Issue #821
     * 
     * @return
     */
    public Context getContext() {
        // init context the first time?
        if (_context == null) {
            long l = System.currentTimeMillis();
            _context = Context.newBuilder(languageId) //
                    .option("engine.WarnInterpreterOnly", "false") //
                    .allowAllAccess(true) //
                    .build();
            logger.log(Level.FINEST, "...init RuleEngine took {0}ms", System.currentTimeMillis() - l);
        }
        return _context;
    }

    /**
     * Sets the value of a member using an identifier. The member value is subject
     * to polyglot value mapping rules as described in Context.asValue(Object).
     * 
     * @param identifier
     * @param value
     */
    public void putMember(String identifier, Object value) {
        getContext().getBindings(languageId).putMember(identifier, value);
    }

    public Value eval(String script) {
        Value result = getContext().eval(languageId, script);
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
            logger.log(Level.FINEST, "......SCRIPT:{0}", script);
        }

        // Test if we have a deprecated Script...
        if (RuleEngineNashornConverter.isDeprecatedScript(script)) {
            // here we rewrite the script as best as we can.
            script = RuleEngineNashornConverter.rewrite(script, workitem, null);
        }

        Value result = null;
        try {
            result = eval(script);
        } catch (PolyglotException e) {
            logger.log(Level.WARNING, "Script Error in: {0}", script);
            // script not valid
            throw new PluginException(RuleEngine.class.getSimpleName(), INVALID_SCRIPT,
                    "BusinessRule contains invalid script:" + e.getMessage(), e);
        }

        if (result.isBoolean()) {
            return result.asBoolean();
        } else {
            return false;
        }
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
            logger.log(Level.FINEST, "......SCRIPT: {0}", script);
        }

        // Test if we have a deprecated Script...
        if (RuleEngineNashornConverter.isDeprecatedScript(script)) {
            // here we rewrite the script as best as we can.
            script = RuleEngineNashornConverter.rewrite(script, workitem, event);
        }

        // evaluate the script....
        try {
            eval(script);
            // try to convert the result object, if provided...
            ItemCollection result = convertResult();
            return result;
        } catch (PolyglotException e) {
            logger.log(Level.WARNING, "Script Error: {0} in: {1}", new Object[] { e.getMessage(), script });
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
    public ItemCollection convertResult() {

        // do we have a result object?
        Value resultValue = getContext().getBindings(languageId).getMember("result");
        if (resultValue == null) {
            return null;
        }

        // Now build a new ItemCollection form the provided values...
        ItemCollection result = new ItemCollection();
        // we can not do a deep copy here because of the embedded polyglot value objects
        Set<String> memberKeys = resultValue.getMemberKeys();
        for (String itemName : memberKeys) {
            Value itemObject = resultValue.getMember(itemName);
            if (itemObject == null || itemObject.isNull() || itemName == null || itemName.isEmpty()) {
                continue;
            }

            if (itemObject.hasArrayElements()) {
                // build an object array with all values...
                long arraySize = itemObject.getArraySize();
                List<Object> arrayValues = new ArrayList<Object>();
                for (long i = 0; i < arraySize; i++) {
                    Value arrayValue = itemObject.getArrayElement(i);
                    Object objectValue = arrayValue.as(Object.class);
                    if (isBasicObjectType(objectValue.getClass())) {
                        arrayValues.add(objectValue);
                    }
                }
                // update the value list
                result.replaceItemValue(itemName, arrayValues);
            } else {
                // treat as single basic object type
                Object objectValue = itemObject.as(Object.class);
                if (isBasicObjectType(objectValue.getClass())) {
                    result.replaceItemValue(itemName, objectValue);
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
