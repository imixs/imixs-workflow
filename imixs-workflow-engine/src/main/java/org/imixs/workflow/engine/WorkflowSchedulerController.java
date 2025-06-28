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

package org.imixs.workflow.engine;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import org.imixs.workflow.engine.scheduler.SchedulerController;

/**
 * The WorkflowSchedulerController is used to start and stop the standard workflow scheduler.
 * <p>
 * The Controller creates a configuration entity "type=scheduler;
 * txtname=org.imixs.workflow.scheduler".
 * <p>
 * 
 * @see SchedulerController
 * @author rsoika
 * 
 */
@Named
@RequestScoped
public class WorkflowSchedulerController extends SchedulerController {

    private static final long serialVersionUID = 1L;

    @Override
    public String getName() {
        return WorkflowScheduler.NAME;
    }

    /**
     * Returns the workflow scheduler class name. 
     * 
     */
    @Override
    public String getSchedulerClass() {
        return WorkflowScheduler.class.getName();
    }

}
