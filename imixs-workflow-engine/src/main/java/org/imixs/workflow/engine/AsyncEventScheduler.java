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

import java.util.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RunAs;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;
import java.util.logging.Level;

import static org.imixs.workflow.engine.AsyncEventSchedulerConfig.*;

/**
 * The AsyncEventScheduler starts a scheduler service to process async events in
 * an asynchronous way by calling the AsyncEventService.
 * <p>
 * The AsyncEventScheduler runs on a non-persistent ejb timer with the interval
 * 'ASYNCEVENT_PROCESSOR_INTERVAL' and an optional delay defined by
 * 'ASYNCEVENT_PROCESSOR_INITIALDELAY'. To enable the processor
 * 'ASYNCEVENT_PROCESSOR_ENABLED' must be set to true (default=false).
 * 'ASYNCEVENT_PROCESSOR_DEADLOCK' deadlock timeout
 * <p>
 * In a clustered environment this timer runs in each cluster member that
 * contains the EJB. So this means the non-persistent EJB Timer scales
 * horizontal within a clustered environment – e.g. a Kubernetes cluster.
 *
 * @see AsyncEventService
 * @version 1.1
 * @author rsoika
 *
 */
@DeclareRoles({ "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
@Startup
@Singleton
public class AsyncEventScheduler {



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
    @ConfigProperty(name = ASYNCEVENT_PROCESSOR_DEADLOCK, defaultValue = "60000")
    long deadLockInterval;

    private static final Logger logger = Logger.getLogger(AsyncEventScheduler.class.getName());

    @Resource
    TimerService timerService;

    @Inject
    AsyncEventService asyncEventService;

    @Inject
    EventLogService eventLogService;

    @PostConstruct
    public void init() {
        if (enabled) {
            logger.log(Level.INFO, "Starting AsyncEventScheduler - initalDelay={0}  inverval={1} ....",
                    new Object[]{initialDelay, interval});

            // Registering a non-persistent Timer Service.
            final TimerConfig timerConfig = new TimerConfig();
            timerConfig.setInfo("Imixs-Workflow AsyncEventScheduler");
            timerConfig.setPersistent(false);
            timerService.createIntervalTimer(initialDelay, interval, timerConfig);
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
    @Timeout
    public void run(Timer timer) {
        eventLogService.releaseDeadLocks(deadLockInterval, EVENTLOG_TOPIC_ASYNC_EVENT);
        asyncEventService.processEventLog();
    }

}
