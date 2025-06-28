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

import org.imixs.workflow.exceptions.AdapterException;
import org.imixs.workflow.exceptions.PluginException;

/**
 * An Adapter defines an adapter pattern used by the WorkflowKernel to call
 * adapter implementations defined by the BPMN model.
 * <p>
 * 
 * 
 * @author Ralph Soika
 * @version 1.0
 * @see org.imixs.workflow.WorkflowKernel
 */

public abstract interface Adapter {

    /**
     * @param document the workitem to be processed
     * @param event    the workflow event containing the processing instructions
     * @return updated workitem for further processing
     * @throws AdapterException interrupt processing
     */
    public ItemCollection execute(ItemCollection document, ItemCollection event) throws AdapterException, PluginException;

}
