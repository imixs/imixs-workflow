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

/**
 * A SignalAdapter extends the Adapter Interface. This Adapter can be associated
 * with a BPMN Signal Event. A SignalAdapter is called by the WorkfklowKernel
 * during the processing life-cycle before the plugin life-cycle.
 * <p>
 * A SignalAdapter can be a CDI implementation.
 * <p>
 * SignalAdapters are called after the execution of GenericAdapters but before
 * any plugin was executed.
 * <p>
 * 
 * @author Ralph Soika
 * @version 1.0
 * @see org.imixs.workflow.engine.WorkflowKernel
 */

public interface SignalAdapter extends Adapter {

}
