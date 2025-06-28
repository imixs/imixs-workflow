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

package org.imixs.workflow.engine.plugins;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
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

    private static final Logger logger = Logger.getLogger(ResultPlugin.class.getName());

    public ItemCollection run(ItemCollection documentContext, ItemCollection event) throws PluginException {
        // evaluate new items....
        ItemCollection evalItemCollection = getWorkflowContext().evalWorkflowResult(event, "item",
                documentContext,
                true);

        if (evalItemCollection != null) {
            List<String> itemNameList = evalItemCollection.getItemNames();
            for (String itemName : itemNameList) {
                // do not accept items starting with $
                // allow $file - Issue #644
                if (!isValidItemName(itemName)) {
                    logger.log(Level.WARNING, "<item> tag contains unsupported item name ''{0}''"
                            + " - verify event result definition!", itemName);

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
        // only if name starts with $ we need to check the SUPPORTED_KERNEL_ITEMS
        if (itemName.startsWith("$")) {
            if (!SPPORTED_KERNEL_ITEMS.contains(itemName.toLowerCase())) {
                return false;
            }
        }
        return true;
    }
}
