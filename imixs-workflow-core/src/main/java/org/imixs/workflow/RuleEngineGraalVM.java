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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Named;
import javax.script.ScriptException;

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
 * The Script Language can be defined by the property 'txtBusinessRuleEngine'
 * within a BPMN Event element or by a a comment added to the first line of a
 * script in the following format:
 * <p>
 * {@code // graalvm.languageId=js}
 * <p>
 * A Script can access all basic item values from the current workItem and also
 * the event by the provided member variables 'workitem' and 'event'.
 * <p>
 * The CDI bean can be replaced by an alternative CDI implementation to provide
 * an extended functionality.
 * <p>
 * 
 * @author Ralph Soika
 * @version 4.0
 * 
 */
@Named
@RequestScoped
public class RuleEngineGraalVM {
	public static final String DEFAULT_LANGUAGE_ID = "js";
	public static final String INVALID_SCRIPT = "INVALID_SCRIPT";

	private static Logger logger = Logger.getLogger(RuleEngineGraalVM.class.getName());

	private Context context = null;
	private String languageId;

	/**
	 * This method initializes the default script engine.
	 */
	public RuleEngineGraalVM() {
		super();
		init(DEFAULT_LANGUAGE_ID);
	}

	/**
	 * This method initializes the script engine.
	 * 
	 * @param scriptLanguage
	 */
	public RuleEngineGraalVM(final String languageID) {
		super();
		init(languageID);
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
	public boolean evaluateBooleanExpression(String script, ItemCollection documentContext) throws PluginException {
		boolean debug = logger.isLoggable(Level.FINE);
		// test if a business rule is defined
		if ("".equals(script.trim()))
			return false; // nothing to do

		// set activity properties into engine
		if (documentContext != null) {
			putMember("workitem", documentContext);
		}
		if (debug) {
			logger.finest("......SCRIPT:" + script);
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
	 * @param documentContext optional workitem context
	 * @param event           optional bpmn event context
	 * @return evaluated result instance
	 * @throws PluginException
	 */
	public ItemCollection evaluateBusinessRule(String script, ItemCollection documentContext, ItemCollection event)
			throws PluginException {
		boolean debug = logger.isLoggable(Level.FINE);
		// test if a business rule is defined
		if ("".equals(script.trim()))
			return null; // nothing to do

		// set member variables...
		boolean deprecatedScript = isDeprecatedScript(script);
		if (!deprecatedScript) {
			if (documentContext != null) {
				putMember("workitem", documentContext);
			}
			if (event != null) {
				putMember("event", event);
			}
		} else {
			// we have a deprecated script so we need to convert the member variables
			if (documentContext != null) {
				putMember("workitem", RuleEngineNashornConverter.convertItemCollection(documentContext));
			}
			if (event != null) {
				putMember("event", RuleEngineNashornConverter.convertItemCollection(event));
			}
		}

		if (debug) {
			logger.finest("......SCRIPT:" + script);
		}

		Value result = null;
		try {			 
			result = eval(script);
			
			Value testTeil = context.getBindings(languageId).getMember("workitem");
			
			if (testTeil!=null) {
				logger.info("....");
			}
		} catch (PolyglotException e) {
			logger.warning("Script Error: " + e.getMessage() + " in: " + script);
			// script not valid
			throw new PluginException(RuleEngine.class.getSimpleName(), INVALID_SCRIPT,
					"BusinessRule contains invalid script:" + e.getMessage(), e);
		}

		// eval result
		ItemCollection resultItemCol = null;
		if (deprecatedScript) {
			resultItemCol= RuleEngineNashornConverter.convertScriptVariableToItemCollection(context.getBindings(languageId),"result");
		} else {
			resultItemCol = result.as(ItemCollection.class);
		}
		return resultItemCol;
	}

	/**
	 * This method returns true if the script is deprecated and was implemented
	 * Initially for the version 3.0 (Nashorn engine). In this case the member
	 * variables are not added as ItemCollection instances but as object arrays.
	 * 
	 * @param script
	 * @return
	 */
	private boolean isDeprecatedScript(String script) {

		if (script.contains("graalvm.languageId=nashorn")) {
			return true;
		}
		
		// all other languageIs default to graalVM...
		if (script.contains("graalvm.languageId=")) {
			return false;
		}
		
		// test workitem.get( => deprecated
		if (script.contains("workitem.get(") || script.contains("event.get(")) {
			return true;
		}

		// all other getter methods indicate new GraalVM
		if (script.contains("workitem.get") || script.contains("event.get")) {
			return false;
		}

		// hasItem, isItem
		if (script.contains("workitem.hasItem") || script.contains("workitem.isItem")) {
			return false;
		}

		// if we still found something like workitem.***[ it indeicates a deprecated
		// script

		// first test if the ItemCollection getter methods are used in the script
		if (script.contains("workitem.") || script.contains("event.")) {
			return true;
		}

		// default to GaalVM
		return false;
	}
}
