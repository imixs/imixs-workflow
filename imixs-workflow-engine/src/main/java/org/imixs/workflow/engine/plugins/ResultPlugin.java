/*******************************************************************************
 * <pre>
 *  Imixs Workflow 
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
 *      Imixs Software Solutions GmbH - initial API and implementation
 *      Ralph Soika - Software Developer
 * </pre>
 *******************************************************************************/

package org.imixs.workflow.engine.plugins;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.PluginException;

/**
 * This Plug-In evaluates the result message provided by the Activity property 'txtActivityResult'.
 * The value will be parsed for the xml tag 'item'
 * 
 * <code>
 * 		<item name="fieldname">value</item> 
 * </code>
 * 
 * The provided value will be assigned to the named property. The value can also be evaluated with
 * the tag 'itemValue'
 * 
 * <code>
 *   <item name="fieldname"><itemvalue>namCreator</itemvalue></item> 
 * </code>
 * 
 * 
 * 
 * @author Ralph Soika
 * @version 1.0
 * @see org.imixs.workflow.WorkflowManager
 * 
 */
public class ResultPlugin extends AbstractPlugin {

  public ItemCollection run(ItemCollection documentContext, ItemCollection adocumentActivity)
      throws PluginException {
    // evaluate new items....
    ItemCollection evalItemCollection =
        getWorkflowService().evalWorkflowResult(adocumentActivity, documentContext, true);
    // copy values
    if (evalItemCollection != null) {
      documentContext.replaceAllItems(evalItemCollection.getAllItems());
    }
    return documentContext;
  }



}
