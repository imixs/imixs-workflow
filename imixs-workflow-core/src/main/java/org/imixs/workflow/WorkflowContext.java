/*  
 *  Imixs-Workflow 
 *  
 *  Copyright (C) 2001-2025 Imixs Software Solutions GmbH,  
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

import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.openbpmn.bpmn.BPMNModel;

/**
 * This WorkflowContext provides methods to resolve a valid model version and
 * fetch a thread save instance of a load a {@code org.openbpmn.bpmn.BPMNModel}.
 * 
 * @author imixs.com
 * @version 1.0
 * @see org.imixs.workflow.WorkflowKernel
 */
public interface WorkflowContext {

        /**
         * This method returns a exclusive thread save BPMNModel instance. The method
         * must not return a shared model instance. The method must throw a
         * ModelException in case no model matching the requested version exists. A
         * client can call {@code findModelVersionByWorkitem} to resolve a valid model
         * version for a workitem.
         *
         * @param version - valid model version
         * @return an instance of a BPMNModel to be used in a thread save way
         * @throws ModelException
         */
        public BPMNModel fetchModel(String version) throws ModelException;

        /**
         * Returns a valid model version for a given workitem. A model version can also
         * be specified as a regular expression or can be resolved only by a given
         * workflow group. The method must throw a ModelException in case no matching
         * model version exists.
         * 
         * @param group - name of the workflow group
         * @return version matching the params of the given workitem
         * @throws ModelException
         **/
        public String findModelVersionByWorkitem(ItemCollection workitem) throws ModelException;

        /**
         * Returns the highest version matching a given regular expression. The method
         * must throw a ModelException in case no matching model version exists.
         * 
         * @param modelRegex - regular expression
         * @return version matching the regex
         * @throws ModelException
         */
        public String findModelVersionByRegEx(String modelRegex) throws ModelException;

        /**
         * Returns returns the hightest version matching a given workflow group. The
         * method must throw a ModelException in case no matching model version exists.
         * 
         * @param group - name of the workflow group
         * @return version matching the group
         * @throws ModelException
         */
        public String findModelVersionByGroup(String group) throws ModelException;

        /**
         * This method loads a Workitem with the corresponding uniqueid.
         * 
         */
        public ItemCollection getWorkItem(String uniqueid);

        /**
         * This method processes a workItem. A workitem have to provide at
         * least the properties '$modelversion', '$taskid' and '$eventid'
         * 
         * @param workitem - the workItem to be processed
         * @return updated version of the processed workItem
         * @throws AccessDeniedException    - thrown if the user has insufficient access
         *                                  to update the workItem
         * @throws ProcessingErrorException - thrown if the workitem could not be
         *                                  processed by the workflowKernel
         * @throws PluginException          - thrown if processing by a plugin fails
         * @throws ModelException
         */
        public ItemCollection processWorkItem(ItemCollection workitem)
                        throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException;

        /**
         * The method evaluates the next task for a process instance (workitem) based on
         * the current model definition. A Workitem must at least provide the properties
         * $TASKID and $EVENTID.
         * 
         * @return Task entity
         * @throws PluginException
         * @throws ModelException
         */
        public ItemCollection evalNextTask(ItemCollection workitem) throws PluginException, ModelException;

        /**
         * The method evaluates the WorkflowResult for a given BPMN event and returns a
         * ItemCollection containing all item values of a specified xml tag. Each tag
         * definition must contain at least a name attribute and may contain an optional
         * list of additional attributes.
         * 
         * @param event
         * @param xmlTag            - XML tag to be evaluated
         * @param documentContext
         * @param resolveItemValues - if true, itemValue tags will be resolved.
         * @return eval itemCollection or null if no tags are contained in the workflow
         *         result.
         * @throws PluginException if the xml structure is invalid
         */
        public ItemCollection evalWorkflowResult(ItemCollection event, String xmlTag, ItemCollection documentContext,
                        boolean resolveItemValues) throws PluginException;

        /**
         * The method evaluates the WorkflowResult for a given BPMN event and returns a
         * ItemCollection containing all item values of a specified tag name. Each tag
         * definition of a WorkflowResult contains a name and a optional list of
         * additional attributes. The method generates a item for each content element
         * and attribute value. <br>
         * e.g. <item name="comment" ignore="true">text</item> <br>
         * will result in the attributes 'comment' with value 'text' and
         * 'comment.ignore' with the value 'true'
         * <p>
         * 
         * @param event
         * @param tag             - tag to be evaluated
         * @param documentContext
         * @return
         * @throws PluginException
         */
        public ItemCollection evalWorkflowResult(ItemCollection event, String tag, ItemCollection documentContext)
                        throws PluginException;

}
