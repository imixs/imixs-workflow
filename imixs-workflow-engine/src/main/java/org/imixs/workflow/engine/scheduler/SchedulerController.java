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

import java.io.Serializable;
import java.text.ParseException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.event.ActionEvent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;

/**
 * The SchedulerController is a front-end controller to start and stop
 * schedulers. A scheduler configuration is defined by the item type="scheduler"
 * and the item name.
 * <p>
 * The class can be subclassed to add specific data to the scheduler
 * configuration.
 * 
 * @author rsoika
 * @version 1.0
 */

@Named
@RequestScoped
public class SchedulerController implements Serializable {

    private ItemCollection configuration = null;
    private String name;
    private String schedulerClass;

    @Inject
    private SchedulerService schedulerService;

    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(SchedulerController.class.getName());

    /**
     * This method load the config entity after postContstruct. If no Entity exists
     * than the ConfigService EJB creates a new config entity.
     * 
     */
    @PostConstruct
    public void init() {
        configuration = schedulerService.loadConfiguration(getName());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSchedulerClass() {
        return schedulerClass;
    }

    public void setSchedulerClass(String schedulerClass) {
        this.schedulerClass = schedulerClass;
    }

    public ItemCollection getConfiguration() {
        if (configuration == null) {
            configuration = new ItemCollection();
            configuration.setItemValue("$workflowsummary", getName());
            configuration.setItemValue(Scheduler.ITEM_SCHEDULER_NAME, getName());
            configuration.setItemValue(Scheduler.ITEM_SCHEDULER_CLASS, getSchedulerClass());
        }
        return configuration;
    }

    public void setConfiguration(ItemCollection configuration) {
        this.configuration = configuration;
    }

    /**
     * Saves the current scheduler configuration.
     */
    public void saveConfiguration() {
        configuration.setItemValue(Scheduler.ITEM_SCHEDULER_CLASS, getSchedulerClass());
        schedulerService.saveConfiguration(getConfiguration());
    }

    /**
     * This method updates the scheduler configuration with the current timer
     * information
     * 
     */
    public void refresh() {
        configuration = schedulerService.loadConfiguration(getName());
    }

    public SchedulerService getSchedulerService() {
        return schedulerService;
    }

    /**
     * starts the timer service
     * 
     * @return
     * @throws ParseException
     * @throws AccessDeniedException
     * @throws Exception
     */
    public void startScheduler() throws AccessDeniedException, ParseException {
        configuration = schedulerService.start(getConfiguration());
        schedulerService.saveConfiguration(configuration);
    }

    public void stopScheduler() {
        configuration = schedulerService.stop(getConfiguration());
        schedulerService.saveConfiguration(configuration);
    }

    public void restartScheduler(ActionEvent event) throws Exception {
        stopScheduler();
        startScheduler();
    }

    /**
     * 
     * converts time (in milliseconds) to human-readable format "<dd:>hh:mm:ss"
     * 
     * @return
     */
    public String millisToShortDHMS(int duration) {
        boolean debug = logger.isLoggable(Level.FINE);
        if (debug) {
            logger.log(Level.FINEST, "......confert ms {0}", duration);
        }
        String res = "";
        long days = TimeUnit.MILLISECONDS.toDays(duration);
        long hours = TimeUnit.MILLISECONDS.toHours(duration)
                - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(duration));
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
                - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(duration));
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration)
                - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration));
        if (days == 0) {
            res = String.format("%d hours, %d minutes, %d seconds", hours, minutes, seconds);
        } else {
            res = String.format("%d days, %d hours, %d minutes, %d seconds", days, hours, minutes, seconds);
        }
        return res;

    }

}
