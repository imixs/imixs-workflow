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

import javax.script.ScriptException;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
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
		//context = Context.create();
		
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

        // test if return value is part of the script
//        if (!script.trim().startsWith("return ")) {
//        	script="return " + script;
//        }
        // set activity properties into engine
        if (documentContext!=null) {
        	putMember("workitem", documentContext);
        }
        if (debug) {
            logger.finest("......SCRIPT:" + script);
        }
        Value result = null;
        try {
        	result=eval(script);
           
        	
        } catch (PolyglotException  e) {
            logger.warning("Script Error in: " + script);
            // script not valid
            throw new PluginException(RuleEngine.class.getSimpleName(), INVALID_SCRIPT,
                    "BusinessRule contains invalid script:" + e.getMessage(), e);
        }
        
        return result.asBoolean();
    }

}
