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

import org.imixs.workflow.exceptions.ModelException;
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

}
