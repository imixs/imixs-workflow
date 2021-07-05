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

import java.util.List;
import java.util.logging.Logger;

/**
 * This is a helper class to convert a deprecated script into the new format.
 * The RuleEngineNashornConverter is called by the RuleEngine.
 * 
 * @author Ralph Soika
 * @version 1.0
 * 
 */
public class RuleEngineNashornConverter {

    private static Logger logger = Logger.getLogger(RuleEngineNashornConverter.class.getName());

    /**
     * This method returns true if the script is detected as deprecated. A
     * deprecated script was implemented Initially for the version 3.0 (Nashorn
     * engine).
     * 
     * @param script
     * @return
     */
    public static boolean isDeprecatedScript(String script) {

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

    /**
     * This method tries to convert a deprecated Nashorn script into a new script
     * dialect.
     * 
     * @param script
     * @param documentContext
     * @param event
     * @return
     */
    public static String rewrite(String script, ItemCollection workitem, ItemCollection event) {
        logger.fine("rewrite scipt: " + script);
        script = convertByItemCollection(script, workitem, "workitem");
        script = convertByItemCollection(script, event, "event");
        // here it may happen the something like
        // workitem.getItemValueString(refField)[0]
        // is the result. We need to remove the [0] here!
        script = script.replace(")[0]", ")");
        return script;

    }

    /**
     * This is a helper method to convert a ItemCollection into a java script object
     * according to the deprecated JavaScript engine Nashorn
     * 
     * @param script
     * @param documentContext
     * @param contextName
     * @return
     */
    private static String convertByItemCollection(String script, ItemCollection documentContext, String contextName) {

        if (documentContext == null || contextName == null || contextName.isEmpty()) {
            return script;
        }
        List<String> itemNames = documentContext.getItemNames();
        for (String itemName : itemNames) {

            String phrase;
            String newPhrase;

            // replace : workitem.txtname[0] => workitem.getItemValueString('txtname')
            phrase = contextName + "." + itemName + "[0]";
            newPhrase = contextName + ".getItemValueString('" + itemName + "')";
            script = script.replace(phrase, newPhrase);

            // replace : workitem.txtname => workitem.hasItem('txtname')
            phrase = contextName + "." + itemName;
            newPhrase = contextName + ".hasItem('" + itemName + "')";
            script = script.replace(phrase, newPhrase);

            phrase = contextName + ".get(";
            newPhrase = contextName + ".getItemValueString(";
            script = script.replace(phrase, newPhrase);

        }
        return script;

    }

}
