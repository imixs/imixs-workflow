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

package org.imixs.workflow.engine.solr;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RunAs;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import java.util.logging.Level;

/**
 * The SolrAutoFlushScheduler starts a ManagedScheduledExecutorService to flush
 * the index events on a scheduled basis by calling the SolrUpdateService method
 * <code>updateIndex()</code>.
 * <p>
 * The ManagedScheduledExecutorService can be configured with the following
 * properties:
 * <p>
 * <ul>
 * <li>solr.flush.interval - flush interval in milliseconds (default 2sec)</li>
 * <li>solr.flush.disabled - if true the scheduler is disabled (default
 * 'false')</li>
 * <li>solr.autoflush.initialdelay - time in milliseconds to delay the
 * start</li>
 * </ul>
 * <p>
 * 
 * @see SolrUpdateService
 * @version 1.0
 * @author rsoika
 *
 */
@DeclareRoles({ "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
@Startup
@Singleton
public class SolrAutoFlushScheduler {

    public static final String SOLR_AUTOFLUSH_DISABLED = "solr.autoflush.disabled";
    public static final String SOLR_AUTOFLUSH_INTERVAL = "solr.autoflush.interval";
    public static final String SOLR_AUTOFLUSH_INITIALDELAY = "solr.autoflush.initialdelay";

    @Inject
    @ConfigProperty(name = SOLR_AUTOFLUSH_DISABLED, defaultValue = "false")
    boolean flushDisabled;

    // timeout interval in ms
    @Inject
    @ConfigProperty(name = SOLR_AUTOFLUSH_INTERVAL, defaultValue = "2000")
    long interval;

    // initial delay in ms
    @Inject
    @ConfigProperty(name = SOLR_AUTOFLUSH_INITIALDELAY, defaultValue = "0")
    long initialDelay;

    private static final Logger logger = Logger.getLogger(SolrAutoFlushScheduler.class.getName());

    @Resource
    ManagedScheduledExecutorService scheduler;

    @Inject
    SolrUpdateService solrUpdateService;

    /**
     * This method start the ManagedScheduledExecutorService to flush the index
     * event log on a schedule base.
     */
    @PostConstruct
    public void init() {
        if (!flushDisabled) {
            logger.log(Level.INFO, "Starting Solr auto flush - initalDelay={0}  inverval={1} ....",
                    new Object[]{initialDelay, interval});
            this.scheduler.scheduleAtFixedRate(this::run, initialDelay, interval, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * The method delecates the event processing to the solrUpdateService
     * 
     */
    public void run() {
        solrUpdateService.updateIndex();
    }

}
