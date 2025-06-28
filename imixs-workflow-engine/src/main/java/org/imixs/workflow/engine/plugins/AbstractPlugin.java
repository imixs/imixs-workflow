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
import java.util.Vector;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.exceptions.PluginException;

/**
 * This abstract class implements different helper methods used by subclasses
 * 
 * @author Ralph Soika
 * @version 1.1
 * @see org.imixs.workflow.WorkflowManager
 * 
 */
public abstract class AbstractPlugin implements Plugin {

    public static final String INVALID_ITEMVALUE_FORMAT = "INVALID_ITEMVALUE_FORMAT";
    public static final String INVALID_PROPERTYVALUE_FORMAT = "INVALID_PROPERTYVALUE_FORMAT";

    private WorkflowContext ctx;

    private WorkflowService workflowService;

    /**
     * Initialize Plugin and get an instance of the EJB Session Context
     */
    public void init(WorkflowContext _ctx) throws PluginException {
        ctx = _ctx;
        // get WorkflowService by check for an instance of WorkflowService
        if (_ctx instanceof WorkflowService) {
            // yes we are running in a WorkflowService EJB
            workflowService = (WorkflowService) _ctx;
        }
    }

    public WorkflowContext getWorkflowContext() {
        return ctx;
    }

    @Override
    public void close(boolean rollbackTransaction) throws PluginException {

    }

    /**
     * Returns an instance of the WorkflowService EJB.
     *
     *
     * @return
     */
    public WorkflowService getWorkflowService() {
        return workflowService;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    /**
     * This method merges the values of fieldList into valueList and test for
     * duplicates.
     * 
     * If an entry of the fieldList is a single key value, than the values to be
     * merged are read from the corresponding documentContext property
     * 
     * e.g. 'namTeam' -> maps the values of the documentContext property 'namteam'
     * into the valueList
     * 
     * If an entry of the fieldList is in square brackets, than the comma separated
     * elements are mapped into the valueList
     * 
     * e.g. '[user1,user2]' - maps the values 'user1' and 'user2' int the valueList.
     * Also Curly brackets are allowed '{user1,user2}'
     * 
     * 
     * @param valueList
     * @param fieldList
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void mergeFieldList(ItemCollection documentContext, List valueList, List<String> fieldList) {
        if (valueList == null || fieldList == null)
            return;
        List<?> values = null;
        if (fieldList.size() > 0) {
            // iterate over the fieldList
            for (String key : fieldList) {
                if (key == null) {
                    continue;
                }
                key = key.trim();
                // test if key contains square or curly brackets?
                if ((key.startsWith("[") && key.endsWith("]")) || (key.startsWith("{") && key.endsWith("}"))) {
                    // extract the value list with regExpression (\s matches any
                    // white space, The * applies the match zero or more times.
                    // So \s* means "match any white space zero or more times".
                    // We look for this before and after the comma.)
                    values = Arrays.asList(key.substring(1, key.length() - 1).split("\\s*,\\s*"));
                } else {
                    // extract value list form documentContext
                    values = documentContext.getItemValue(key);
                }
                // now append the values into p_VectorDestination
                if ((values != null) && (values.size() > 0)) {
                    for (Object o : values) {
                        // append only if not used
                        if (valueList.indexOf(o) == -1)
                            valueList.add(o);
                    }
                }
            }
        }

    }

    /**
     * This method removes duplicates and null values from a vector.
     * 
     * @param valueList - list of elements
     */
    public List<?> uniqueList(List<Object> valueList) {
        int iVectorSize = valueList.size();
        Vector<Object> cleanedVector = new Vector<Object>();

        for (int i = 0; i < iVectorSize; i++) {
            Object o = valueList.get(i);
            if (o == null || cleanedVector.indexOf(o) > -1 || "".equals(o.toString()))
                continue;

            // add unique object
            cleanedVector.add(o);
        }
        valueList = cleanedVector;
        // do not work with empty vectors....
        if (valueList.size() == 0)
            valueList.add("");

        return valueList;
    }

}
