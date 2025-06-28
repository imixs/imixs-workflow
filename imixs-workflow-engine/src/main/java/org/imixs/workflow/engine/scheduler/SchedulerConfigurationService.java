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

package org.imixs.workflow.engine.scheduler;

import java.util.logging.Logger;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RunAs;
import jakarta.inject.Inject;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentService;

import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * This SchedulerSaveService is used to save configurations in a new
 * transaction. The service is only called by the SchedulerService in case a
 * scheduler throws a SchedulerException or a RuntimeExcepiton.
 * 
 * @see SchedulerService for details
 * @author rsoika
 * @version 1.0
 */
@DeclareRoles({ "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
@Stateless
public class SchedulerConfigurationService {

    @Resource
    SessionContext ctx;

    @Inject
    DocumentService documentService;

    private static final Logger logger = Logger.getLogger(SchedulerConfigurationService.class.getName());

    /**
     * This method saves a configuration in a new transaction. This is needed case
     * of a runtime exception
     * 
     */
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void storeConfigurationInNewTransaction(ItemCollection config) {
        logger.finest(" ....saving scheduler configuration by new transaciton...");
        config.removeItem("$version");
        config = documentService.save(config);
    }
}
