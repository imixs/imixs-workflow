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
