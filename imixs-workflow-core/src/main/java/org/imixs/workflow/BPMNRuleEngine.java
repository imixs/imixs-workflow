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
import org.imixs.workflow.exceptions.PluginException;

/**
 * The BPMN Rule Engine can be used to evaluate a business Rule based on a BPMN
 * model with conditional events.
 * <p>
 * The rules are evaluated as a chain of conditional events
 * 
 * @author rsoika
 *
 */
public class BPMNRuleEngine {

    private Model model = null;

    public BPMNRuleEngine(Model model) {
        super();
        this.model = model;
    }

    /**
     * Evaluates a BPMN Business Rule based on the data provided by a workitem.
     * 
     * @param workitem
     * @return evaluated task id
     * @throws ModelException
     */
    public int eval(ItemCollection workitem) throws ModelException {
        // setup the workflow rule context
        RuleContext ruleContext = new RuleContext();
        WorkflowKernel workflowKernel = new WorkflowKernel(ruleContext);
        ruleContext.getModelManager().addModel(model);
        int result;
        try {
            result = workflowKernel.eval(workitem);
        } catch (PluginException e) {
            throw new ModelException(e.getErrorCode(), e.getMessage(), e);
        }

        return result;
    }

    /**
     * Helper Class to mock a workflow kernel
     * 
     * @author rsoika
     *
     */
    class RuleContext implements WorkflowContext, ModelManager {

        private Model model = null;

        @Override
        public Object getSessionContext() {
            return null;
        }

        @Override
        public ModelManager getModelManager() {
            return this;
        }

        @Override
        public Model getModel(String version) throws ModelException {
            return model;
        }

        @Override
        public void addModel(Model model) throws ModelException {
            this.model = model;
        }

        @Override
        public void removeModel(String version) {
        }

        @Override
        public Model getModelByWorkitem(ItemCollection workitem) throws ModelException {
            return model;
        }

    }

}
