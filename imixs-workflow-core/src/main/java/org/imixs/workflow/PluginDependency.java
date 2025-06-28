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

import java.util.List;

/**
 * A plug-in may optionally implement the interface 'PluginDependency' to
 * indicate dependencies on other plug-ins. Plug-in dependencies are validated
 * by the WorkflowKernel during processing a workflow event. If a plug-in
 * defined by the BPMN model signals dependencies which are not reflected by the
 * current model definition, a warning message is logged.
 * 
 * 
 * @author Ralph Soika
 * @version 1.0
 * @see org.imixs.workflow.WorkflowKernel
 */

public interface PluginDependency {

    /**
     * Returns a String list of plugin class names which the currend implementation
     * depends on.
     * 
     */
    public List<String> dependsOn();

}
