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

import org.imixs.workflow.exceptions.PluginException;

/**
 * Mokup Plugin which returns null after run but changes the field "txtname"!
 * 
 * @author rsoika
 * 
 */
public class MockPluginNull implements Plugin {

	@Override
	public void init(WorkflowContext actx) throws PluginException {
		// no op
	}

	@Override
	public ItemCollection run(final ItemCollection documentContext, ItemCollection documentActivity)
			throws PluginException {
		documentContext.replaceItemValue("txtName", "should not be null");
		return null;
	}

	@Override
	public void close(boolean rollbackTransaction) throws PluginException {
		// no op
	}

}
