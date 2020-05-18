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

package org.imixs.workflow.engine;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RunAs;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * The AsyncEventScheduler starts a ManagedScheduledExecutorService to process
 * async events in an asynchronous way by calling the AsyncEventService.
 * <p>
 * The AsyncEventScheduler runs on a ManagedScheduledExecutorService with the
 * interval 'ASYNCEVENT_PROCESSOR_INTERVAL' and an optional delay defined by
 * 'ASYNCEVENT_PROCESSOR_INITIALDELAY'. To enable the processor
 * 'ASYNCEVENT_PROCESSOR_ENABLED' must be set to true (default=false).
 * 'ASYNCEVENT_PROCESSOR_DEADLOCK' deadlock timeout
 * <p>
 *
 * @see AsyncEventService
 * @version 1.0
 * @author rsoika
 *
 */
@DeclareRoles({ "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
@Startup
@Singleton
@LocalBean
public class AsyncEventScheduler {

    public static final String ASYNCEVENT_PROCESSOR_ENABLED = "asyncevent.processor.enabled";
    public static final String ASYNCEVENT_PROCESSOR_INTERVAL = "asyncevent.processor.interval";
    public static final String ASYNCEVENT_PROCESSOR_INITIALDELAY = "asyncevent.processor.initialdelay";
    public static final String ASYNCEVENT_PROCESSOR_DEADLOCK = "asyncevent.processor.deadlock";

    public static final String EVENTLOG_TOPIC_ASYNC_EVENT = "async.event";

    // enabled
    @Inject
    @ConfigProperty(name = ASYNCEVENT_PROCESSOR_ENABLED, defaultValue = "false")
    boolean enabled;

    // timeout interval in ms
    @Inject
    @ConfigProperty(name = ASYNCEVENT_PROCESSOR_INTERVAL, defaultValue = "1000")
    long interval;

    // initial delay in ms
    @Inject
    @ConfigProperty(name = ASYNCEVENT_PROCESSOR_INITIALDELAY, defaultValue = "0")
    long initialDelay;

    // deadlock timeout interval in ms
    @Inject
    @ConfigProperty(name = AsyncEventScheduler.ASYNCEVENT_PROCESSOR_DEADLOCK, defaultValue = "60000")
    long deadLockInterval;

    private static Logger logger = Logger.getLogger(AsyncEventScheduler.class.getName());

    @Resource
    ManagedScheduledExecutorService scheduler;

    @Inject
    AsyncEventService asyncEventService;

    @Inject
    EventLogService eventLogService;

    @PostConstruct
    public void init() {
        if (enabled) {
            logger.info(
                    "Starting AsyncEventScheduler - initalDelay=" + initialDelay + "  inverval=" + interval + " ....");
            this.scheduler.scheduleAtFixedRate(this::run, initialDelay, interval, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * The method delegates the event processing to the stateless ejb
     * AsyncEventProcessor.
     * <p>
     * Before processing the eventLog the method releases possible dead locks first.
     * Both methods are running in separate transactions
     * 
     */
    public void run() {
        eventLogService.releaseDeadLocks(deadLockInterval, EVENTLOG_TOPIC_ASYNC_EVENT);
        asyncEventService.processEventLog();
    }

}
