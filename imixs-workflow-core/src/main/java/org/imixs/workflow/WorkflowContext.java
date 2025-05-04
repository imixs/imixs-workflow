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

import org.imixs.workflow.exceptions.ModelException;
import org.openbpmn.bpmn.BPMNModel;

/**
 * This WorkflowContext provides the {@link WorkflowKernel} methods to load a
 * BPMNModel instance.
 * 
 * @author imixs.com
 * @version 1.0
 * @see org.imixs.workflow.WorkflowKernel
 */

public interface WorkflowContext {

    // /**
    // * This method loads a BPMNModel instance.
    // *
    // * @param modelRegex - valid model version
    // * @return an instance of a BPMNModel
    // * @throws ModelException
    // */
    // public BPMNModel loadModel(String version) throws ModelException;

    /**
     * This method returns the highest version matching a given regular expression.
     * 
     * @param modelRegex - regular expression
     * @return version matching the regex
     * @throws ModelException
     */
    public String findVersionByRegEx(String modelRegex) throws ModelException;

    /**
     * Returns returns the hightest version matching a given workflow group.
     * 
     * @param group - name of the workflow group
     * @return version matching the group
     * @throws ModelException
     */
    public String findVersionByGroup(String group) throws ModelException;

    /**
     * Returns a Model matching the $modelVersion of a given workitem.
     * 
     * @param group - name of the workflow group
     * @return version matching the params of the given workitem
     * @throws ModelException
     **/
    public BPMNModel findModelByWorkitem(ItemCollection workitem) throws ModelException;

}
