/*******************************************************************************
 *  Imixs Workflow Technology
 *  Copyright (C) 2003, 2008 Imixs Software Solutions GmbH,  
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
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika
 *  
 *******************************************************************************/
package org.imixs.workflow.engine.scheduler;

import java.io.Serializable;
import java.text.ParseException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.faces.event.ActionEvent;
import javax.inject.Named;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;

/**
 * The SchedulerController is a front-end controller to start and stop
 * schedulers. A scheduler configuration is defined by the item type="scheduler"
 * and the item txtname.
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

	@EJB
	private SchedulerService schedulerService;

	private static final long serialVersionUID = 1L;

	private static Logger logger = Logger.getLogger(SchedulerController.class.getName());

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
			configuration.setItemValue(Scheduler.ITEM_SCHEDULER_NAME, getName());
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
		getSchedulerService().saveConfiguration(getConfiguration());
	}

	/**
	 * This method updates the scheduler configuration with the current timer
	 * information
	 * 
	 */
	public void refresh() {
		getSchedulerService().updateTimerDetails(getConfiguration());
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
		configuration=schedulerService.start(getConfiguration());
		schedulerService.saveConfiguration(configuration);
	}

	public void stopScheduler() {
		configuration=schedulerService.stop(getConfiguration());
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
		logger.finest("......confert ms " + duration);
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
