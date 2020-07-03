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

package org.imixs.workflow.engine.plugins;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.PluginException;

/**
 * This Plug-In evaluates the result message provided by the Activity property
 * 'txtActivityResult'. The value will be parsed for the xml tag 'item'
 * 
 * <code>
 * 		<item name="fieldname">value</item> 
 * </code>
 * 
 * The provided value will be assigned to the named property. The value can also
 * be evaluated with the tag 'itemValue'
 * 
 * <code>
 *   <item name="fieldname"><itemvalue>namCreator</itemvalue></item> 
 * </code>
 * 
 * 
 * 
 * @author Ralph Soika
 * @version 1.1
 * @see org.imixs.workflow.WorkflowManager
 * 
 */
public class ResultPlugin extends AbstractPlugin {

    public static List<String> SPPORTED_KERNEL_ITEMS = Arrays.asList("$file", "$snapshot.history");

    private static Logger logger = Logger.getLogger(ResultPlugin.class.getName());

    public ItemCollection run(ItemCollection documentContext, ItemCollection event) throws PluginException {
        // evaluate new items....
        ItemCollection evalItemCollection = getWorkflowService().evalWorkflowResult(event, "item", documentContext,
                true);

        if (evalItemCollection != null) {
            List<String> itemNameList = evalItemCollection.getItemNames();
            for (String itemName : itemNameList) {
                // do not accept items starting with $
                // allow $file - Issue #644
                if (!isValidItemName(itemName)) {
                    logger.warning("<item> tag contains unsupported item name '" + itemName
                            + "' - verify event result definition!");

                    evalItemCollection.removeItem(itemName);
                }
            }
            // copy values (invalid items are already removed)
            documentContext.replaceAllItems(evalItemCollection.getAllItems());
        }
        return documentContext;
    }

    /**
     * Returns true if the given itemName is valid to be set by this plugin.
     * 
     * @param itemname
     * @return
     */
    public boolean isValidItemName(String itemName) {
        // only if name starts with $ we need to check the SPPORTED_KERNEL_ITEMS
        if (itemName.startsWith("$")) {
            if (!SPPORTED_KERNEL_ITEMS.contains(itemName.toLowerCase())) {
                return false;
            }
        }
        return true;
    }
}
