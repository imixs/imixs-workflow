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

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a helper class to convert a deprecated script into the new format.
 * The RuleEngineNashornConverter is called by the RuleEngine.
 * 
 * @author Ralph Soika
 * @version 1.0
 * 
 */
public class RuleEngineNashornConverter {

    private static final Logger logger = Logger.getLogger(RuleEngineNashornConverter.class.getName());

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

        // test for things like  workitem['space.team']  or  workitem['space.team'][0]
        // workitem\['\w+'\]
        String regex = "workitem\\['[._\\w]+'\\]";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(script);
        if (matcher.find()) {
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

        StringBuilder converterLog = new StringBuilder()
        .append("\n***************************************************")
        .append("\n*** DEPRECATED NASHORN SCRIPT FOUND:            ***")
        .append("\n***************************************************\n")

        .append("\n").append(script).append("\n\n");

        script = convertByItemCollection(script, workitem, "workitem");
        script = convertByItemCollection(script, event, "event");
        // here it may happen the something like
        // workitem.getItemValueString(refField)[0]
        // is the result. We need to remove the [0] here!
        script = script.replace(")[0]", ")");

        converterLog.append("\n***************************************************")
        .append("\n*** PLEASE REPLACE YOUR SCRIPT WITH:            ***")
        .append("\n***************************************************\n")
        .append("\n").append(script).append("\n")
        .append("\n***************************************************\n");
        logger.warning(converterLog.toString());
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

        // resort the name list by length.
        // it may happen that two item names start with the name of another item
        // (e.g. 'team', 'team$approvers') in that case it is important that the longer
        // item name is checked first!

        Collections.sort(itemNames, (a, b) -> Integer.compare(b.length(), a.length()));

        for (String itemName : itemNames) {

            String phrase;
            String newPhrase;

            // CASE-1
            // replace : workitem.txtname[0] => workitem.getItemValueString('txtname')
            phrase = contextName + "." + itemName + "[0]";
            // is it a number?
            if (documentContext.isItemValueNumeric(itemName)) {
                newPhrase = contextName + ".getItemValueDouble('" + itemName + "')";
            } else {
                newPhrase = contextName + ".getItemValueString('" + itemName + "')";
            }
            script = script.replace(phrase, newPhrase);




            // CASE-2
            // replace :workitem['txtname'][0] => workitem.getItemValueString('txtname')
            phrase = contextName + "['" + itemName +"'][0]";
            // is it a number?
            if (documentContext.isItemValueNumeric(itemName)) {
                newPhrase = contextName + ".getItemValueDouble('" + itemName + "')";
            } else {
                newPhrase = contextName + ".getItemValueString('" + itemName + "')";
            }
            script = script.replace(phrase, newPhrase);


            // CASE-3
            // replace : workitem.txtname => workitem.hasItem('txtname')
            phrase = contextName + "." + itemName;
            newPhrase = contextName + ".hasItem('" + itemName + "')";
            script = script.replace(phrase, newPhrase);


             // CASE-4
            // replace : workitem['txtname'] => workitem.hasItem('txtname')
            phrase = contextName + "['" + itemName +"']";
            newPhrase = contextName + ".hasItem('" + itemName + "')";
            script = script.replace(phrase, newPhrase);

            
            // CASE-5
            // replace : workitem.txtname 
            phrase = contextName + ".get(";
            // is it a number?
            if (documentContext.isItemValueNumeric(itemName)) {
                newPhrase = contextName + ".getItemValueDouble(";
            } else {
                newPhrase = contextName + ".getItemValueString(";
            }
            script = script.replace(phrase, newPhrase);

        }
        return script;

    }

}
