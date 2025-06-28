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

import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.PluginException;

/**
 * Deprecated - see PaticipantAdapter.
 * 
 * @author Ralph Soika
 * @version 3.0
 * @see org.imixs.workflow.WorkflowManager
 */
@Deprecated
public class AccessPlugin extends AbstractPlugin {
    private static final Logger logger = Logger.getLogger(AccessPlugin.class.getName());

    @Deprecated
    public ItemCollection run(ItemCollection adocumentContext, ItemCollection documentActivity) throws PluginException {

        logger.warning("The AccessPlugin is deprecated and can be removed from this model!");
        return adocumentContext;
    }

}
