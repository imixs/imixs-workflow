package org.imixs.workflow.engine;

/*******************************************************************************
 *  Imixs Workflow 
 *  Copyright (C) 2001, 2011 Imixs Software Solutions GmbH,  
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
 *  	http://www.imixs.org
 *  	http://java.net/projects/imixs-workflow
 *  
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika - Software Developer
 *******************************************************************************/

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.ScheduleExpression;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.imixs.workflow.exceptions.QueryException;

/**
 * This EJB implements a TimerService which scans workitems for scheduled
 * activities. The component will be later become part of the imixs workflow
 * engine
 * 
 * 
 * The configuration of the timer is stored by this ejb through the method
 * saveConfiguration(); The configuration is stored as an entity from the type =
 * 'configuration' and the txtName = '"org.imixs.marty.workflow.scheduler'.
 * 
 * 
 * The method processSingleWorkitem() is used to process a workitem in an
 * isolated transaction. See: http://blog.imixs.org/?p=155
 * 
 * 
 * @author rsoika
 * 
 */
@Stateless
@LocalBean
@DeclareRoles({ "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
public class WorkflowSchedulerService {

	final static public String TYPE_CONFIGURATION = "configuration";
	final static public String NAME = "org.imixs.workflow.scheduler";

	final static public int OFFSET_SECONDS = 0;
	final static public int OFFSET_MINUTES = 1;
	final static public int OFFSET_HOURS = 2;
	final static public int OFFSET_DAYS = 3;
	final static public int OFFSET_WORKDAYS = 4;

	private static Logger logger = Logger.getLogger(WorkflowSchedulerService.class.getName());

	@EJB
	WorkflowService workflowService;

	@EJB
	DocumentService documentService;

	@EJB
	ModelService modelService;

	@Resource
	javax.ejb.TimerService timerService;

	@Resource
	SessionContext ctx;

	int iProcessWorkItems = 0;
	List<String> unprocessedIDs = null;

	/**
	 * This method loads the current scheduler configuration. If no configuration
	 * entity yet exists the method returns an empty ItemCollection.
	 * 
	 * The method updates the timer details for a running timer.
	 * 
	 * @return configuration ItemCollection
	 */
	public ItemCollection loadConfiguration() {
		ItemCollection configItemCollection = null;
		String searchTerm = "(type:\"" + TYPE_CONFIGURATION + "\" AND txtname:\"" + NAME + "\")";

		Collection<ItemCollection> col;
		try {
			col = documentService.find(searchTerm, 2, 0);
		} catch (QueryException e) {
			logger.severe("loadConfiguration - invalid param: " + e.getMessage());
			throw new InvalidAccessException(InvalidAccessException.INVALID_ID, e.getMessage(), e);
		}

		if (col.size() > 1) {
			String message = "loadConfiguration - more than on timer configuration found! Check configuration (type:\"configuration\" txtname:\"org.imixs.workflow.scheduler\") ";
			logger.severe(message);
			throw new InvalidAccessException(InvalidAccessException.INVALID_ID, message);
		}

		if (col.size() == 1) {
			logger.fine("loading existing timer configuration...");
			configItemCollection = col.iterator().next();
		} else {
			logger.fine("creating new timer configuration...");
			// create default values
			configItemCollection = new ItemCollection();
			configItemCollection.replaceItemValue("type", TYPE_CONFIGURATION);
			configItemCollection.replaceItemValue("txtname", NAME);
			configItemCollection.replaceItemValue(WorkflowKernel.UNIQUEID, WorkflowKernel.generateUniqueID());
		}
		configItemCollection = updateTimerDetails(configItemCollection);
		return configItemCollection;
	}

	/**
	 * This method saves the timer configuration. The method ensures that the
	 * following properties are set to default.
	 * <ul>
	 * <li>type</li>
	 * <li>txtName</li>
	 * <li>$writeAccess</li>
	 * <li>$readAccess</li>
	 * </ul>
	 * 
	 * The method also updates the timer details of a running timer.
	 * 
	 * @return
	 * @throws AccessDeniedException
	 */
	public ItemCollection saveConfiguration(ItemCollection configItemCollection) throws AccessDeniedException {
		// update write and read access
		configItemCollection.replaceItemValue("type", TYPE_CONFIGURATION);
		configItemCollection.replaceItemValue("txtName", NAME);
		configItemCollection.replaceItemValue("$writeAccess", "org.imixs.ACCESSLEVEL.MANAGERACCESS");
		configItemCollection.replaceItemValue("$readAccess", "org.imixs.ACCESSLEVEL.MANAGERACCESS");

		// configItemCollection.replaceItemValue("$writeAccess", "");
		// configItemCollection.replaceItemValue("$readAccess", "");

		configItemCollection = updateTimerDetails(configItemCollection);
		// save entity
		configItemCollection = documentService.save(configItemCollection);

		return configItemCollection;
	}

	/**
	 * This Method starts the TimerService.
	 * 
	 * The Timer can be started based on a Calendar setting stored in the property
	 * txtConfiguration, or by interval based on the properties datStart, datStop,
	 * numIntervall.
	 * 
	 * 
	 * The method loads the configuration entity and evaluates the timer
	 * configuration. THe $UniqueID of the configuration entity is the id of the
	 * timer to be controlled.
	 * 
	 * $uniqueid - String - identifier for the Timer Service.
	 * 
	 * txtConfiguration - calendarBasedTimer configuration
	 * 
	 * datstart - Date Object
	 * 
	 * datstop - Date Object
	 * 
	 * numInterval - Integer Object (interval in seconds)
	 * 
	 * 
	 * The method throws an exception if the configuration entity contains invalid
	 * attributes or values.
	 * 
	 * After the timer was started the configuration is updated with the latest
	 * statusmessage
	 * 
	 * The method returns the current configuration
	 * 
	 * @throws AccessDeniedException
	 * @throws ParseException
	 */
	public ItemCollection start() throws AccessDeniedException, ParseException {
		ItemCollection configItemCollection = loadConfiguration();
		Timer timer = null;
		if (configItemCollection == null)
			return null;

		String id = configItemCollection.getUniqueID();

		// try to cancel an existing timer for this workflowInstance
		while (this.findTimer(id) != null) {
			this.findTimer(id).cancel();
		}

		String sConfiguation = configItemCollection.getItemValueString("txtConfiguration");

		if (!sConfiguation.isEmpty()) {
			// New timer will be started on calendar confiugration
			timer = createTimerOnCalendar(configItemCollection);
		} else {
			// update the interval based on hour/minute configuration
			int hours = configItemCollection.getItemValueInteger("hours");
			int minutes = configItemCollection.getItemValueInteger("minutes");
			long interval = (hours * 60 + minutes) * 60 * 1000;
			configItemCollection.replaceItemValue("numInterval", new Long(interval));

			timer = createTimerOnInterval(configItemCollection);
		}

		// start the timer and set a status message
		if (timer != null) {

			Calendar calNow = Calendar.getInstance();
			SimpleDateFormat dateFormatDE = new SimpleDateFormat("dd.MM.yy hh:mm:ss");
			String msg = "started at " + dateFormatDE.format(calNow.getTime()) + " by "
					+ ctx.getCallerPrincipal().getName();
			configItemCollection.replaceItemValue("statusmessage", msg);

			if (timer.isCalendarTimer()) {
				configItemCollection.replaceItemValue("Schedule", timer.getSchedule().toString());
			} else {
				configItemCollection.replaceItemValue("Schedule", "");

			}
			logger.info(configItemCollection.getItemValueString("txtName") + " started: " + id);
		}

		configItemCollection = saveConfiguration(configItemCollection);

		return configItemCollection;
	}

	/**
	 * Stops a running timer instance. After the timer was canceled the
	 * configuration will be updated.
	 * 
	 * @throws AccessDeniedException
	 * 
	 */
	public ItemCollection stop() throws AccessDeniedException {
		ItemCollection configItemCollection = loadConfiguration();

		String id = configItemCollection.getUniqueID();
		boolean found = false;
		while (this.findTimer(id) != null) {
			this.findTimer(id).cancel();
			found = true;
		}
		if (found) {
			Calendar calNow = Calendar.getInstance();
			SimpleDateFormat dateFormatDE = new SimpleDateFormat("dd.MM.yy hh:mm:ss");
			String msg = "stopped at " + dateFormatDE.format(calNow.getTime()) + " by "
					+ ctx.getCallerPrincipal().getName();
			configItemCollection.replaceItemValue("statusmessage", msg);
			logger.info(configItemCollection.getItemValueString("txtName") + " stopped: " + id);
		} else {
			configItemCollection.replaceItemValue("statusmessage", "");
		}
		configItemCollection = saveConfiguration(configItemCollection);

		return configItemCollection;
	}

	/**
	 * Returns true if the workflowSchedulerService was started
	 */
	public boolean isRunning() {
		try {
			ItemCollection configItemCollection = loadConfiguration();
			if (configItemCollection == null)
				return false;

			return (findTimer(configItemCollection.getUniqueID()) != null);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * This method checks if a workitem (doc) is in due. There are 4 different cases
	 * which will be compared: The case is determined by the keyScheduledBaseObject
	 * of the activity entity
	 * 
	 * Basis : keyScheduledBaseObject "last process"=1, "last Modification"=2
	 * "Creation"=3 "Field"=4
	 * 
	 * The logic is not the best one but it works. So we are open for any kind of
	 * improvements
	 * 
	 * @return true if workitem is is due
	 */
	public static boolean workItemInDue(ItemCollection doc, ItemCollection docActivity) {
		try {
			int iCompareType = -1;
			int iOffsetUnit = -1;
			int iOffset = 0;
			Date dateTimeCompare = null;
			String suniqueid = doc.getItemValueString("$uniqueid");
			String sDelayUnit = docActivity.getItemValueString("keyActivityDelayUnit");

			try {
				iOffsetUnit = Integer.parseInt(sDelayUnit); // 1= min, 2= hours,
															// 3=day, 4=workdays

				if (iOffsetUnit < 1 || iOffsetUnit > 4) {
					logger.warning(
							"error parsing delay in ActivityEntity " + docActivity.getItemValueInteger("numProcessID")
									+ "." + docActivity.getItemValueInteger("numActivityID")
									+ " : unsuported keyActivityDelayUnit=" + sDelayUnit);
					return false;
				}

			} catch (NumberFormatException nfe) {
				logger.warning(
						"error parsing delay in ActivityEntity " + docActivity.getItemValueInteger("numProcessID") + "."
								+ docActivity.getItemValueInteger("numActivityID") + " :" + nfe.getMessage());
				return false;
			}
			// get activityDelay from Event
			iOffset = docActivity.getItemValueInteger("numActivityDelay");

			if ("1".equals(sDelayUnit))
				sDelayUnit = "minutes";
			if ("2".equals(sDelayUnit))
				sDelayUnit = "hours";
			if ("3".equals(sDelayUnit))
				sDelayUnit = "days";
			if ("4".equals(sDelayUnit))
				sDelayUnit = "workdays";

			logger.finest(suniqueid + " offset =" + iOffset + " " + sDelayUnit);

			iCompareType = docActivity.getItemValueInteger("keyScheduledBaseObject");

			// get current time for compare....
			Date dateTimeNow = Calendar.getInstance().getTime();

			switch (iCompareType) {
			// last process -
			case 1: {
				logger.finest(suniqueid + ": CompareType = last event");

				// support deprecated fields $lastProcessingDate and timWorkflowLastAccess
				if (!doc.hasItem("$lastEventDate")) {
					logger.info("migrating $lasteventdate...");
					if (doc.hasItem("$lastProcessingDate")) {
						doc.replaceItemValue("$lastEventDate", doc.getItemValue("$lastProcessingDate"));
					} else {
						doc.replaceItemValue("$lastEventDate", doc.getItemValue("timWorkflowLastAccess"));
					}
				}
				dateTimeCompare = doc.getItemValueDate("$lastEventDate");
				if (dateTimeCompare == null) {
					logger.warning(suniqueid + ": item '$lastEventDate' is missing!");
					return false;
				}

				// compute scheduled time
				logger.finest(suniqueid + ": $lastEventDate=" + dateTimeCompare);
				dateTimeCompare = adjustBaseDate(dateTimeCompare, iOffsetUnit, iOffset);
				if (dateTimeCompare != null)
					return dateTimeCompare.before(dateTimeNow);
				else
					return false;
			}

			// last modification
			case 2: {
				logger.finest(suniqueid + ": CompareType = last modify");

				dateTimeCompare = doc.getItemValueDate("$modified");

				logger.finest(suniqueid + ": modified=" + dateTimeCompare);

				dateTimeCompare = adjustBaseDate(dateTimeCompare, iOffsetUnit, iOffset);

				if (dateTimeCompare != null)
					return dateTimeCompare.before(dateTimeNow);
				else
					return false;
			}

			// creation
			case 3: {
				logger.finest(suniqueid + ": CompareType = creation");

				dateTimeCompare = doc.getItemValueDate("$created");
				logger.fine(suniqueid + ": doc.getCreated() =" + dateTimeCompare);

				// Nein -> Creation date ist masstab
				dateTimeCompare = adjustBaseDate(dateTimeCompare, iOffsetUnit, iOffset);

				if (dateTimeCompare != null)
					return dateTimeCompare.before(dateTimeNow);
				else
					return false;
			}

			// field
			case 4: {
				String sNameOfField = docActivity.getItemValueString("keyTimeCompareField");
				logger.finest(suniqueid + ": CompareType = field: '" + sNameOfField + "'");

				if (!doc.hasItem(sNameOfField)) {
					logger.finest(suniqueid + ": CompareType =" + sNameOfField
							+ " no value found!");
					return false;
				}

				dateTimeCompare = doc.getItemValueDate(sNameOfField);

				logger.finest(suniqueid + ": " + sNameOfField + "=" + dateTimeCompare);

				dateTimeCompare = adjustBaseDate(dateTimeCompare, iOffsetUnit, iOffset);
				if (dateTimeCompare != null) {
					logger.finest(suniqueid + ": Compare " + dateTimeCompare + " <-> " + dateTimeNow);

					if (dateTimeCompare.before(dateTimeNow)) {
						logger.finest(suniqueid + " isInDue!");
					}
					return dateTimeCompare.before(dateTimeNow);
				} else
					return false;
			}
			default: {
				logger.warning("Time Base is not defined, verify model!");
				return false;
			}
			}

		} catch (Exception e) {

			e.printStackTrace();
			return false;
		}

	}

	/**
	 * This method adds workdays (MONDAY - FRIDAY) to a given calendar object. If
	 * the number of days is negative than this method subtracts the working days
	 * from the calendar object.
	 * 
	 * 
	 * @param cal
	 * @param days
	 * @return new calendar instance
	 */
	public static Calendar addWorkDays(final Calendar baseDate, final int days) {
		Calendar resultDate = null;
		Calendar workCal = Calendar.getInstance();
		workCal.setTime(baseDate.getTime());

		int currentWorkDay = workCal.get(Calendar.DAY_OF_WEEK);

		// test if SATURDAY ?
		if (currentWorkDay == Calendar.SATURDAY) {
			// move to next FRIDAY
			workCal.add(Calendar.DAY_OF_MONTH, (days < 0 ? -1 : +2));
			currentWorkDay = workCal.get(Calendar.DAY_OF_WEEK);
		}
		// test if SUNDAY ?
		if (currentWorkDay == Calendar.SUNDAY) {
			// move to next FRIDAY
			workCal.add(Calendar.DAY_OF_MONTH, (days < 0 ? -2 : +1));
			currentWorkDay = workCal.get(Calendar.DAY_OF_WEEK);
		}

		// test if we are in a working week (should be so!)
		if (currentWorkDay >= Calendar.MONDAY && currentWorkDay <= Calendar.FRIDAY) {
			boolean inCurrentWeek = false;
			if (days > 0)
				inCurrentWeek = (currentWorkDay + days < 7);
			else
				inCurrentWeek = (currentWorkDay + days > 1);

			if (inCurrentWeek) {
				workCal.add(Calendar.DAY_OF_MONTH, days);
				resultDate = workCal;
			} else {
				int totalDays = 0;
				int daysInCurrentWeek = 0;

				// fill up current week.
				if (days > 0) {
					daysInCurrentWeek = Calendar.SATURDAY - currentWorkDay;
					totalDays = daysInCurrentWeek + 2;
				} else {
					daysInCurrentWeek = -(currentWorkDay - Calendar.SUNDAY);
					totalDays = daysInCurrentWeek - 2;
				}

				int restTotalDays = days - daysInCurrentWeek;
				// next working week... add 2 days for each week.
				int x = restTotalDays / 5;
				totalDays += restTotalDays + (x * 2);

				workCal.add(Calendar.DAY_OF_MONTH, totalDays);
				resultDate = workCal;

			}
		}
		if (resultDate != null) {
			logger.finest("addWorkDays (" + baseDate.getTime() + ") + " + days + " = (" + resultDate.getTime() + ")");
		}
		return resultDate;
	}

	/**
	 * This method process scheduled workitems. The method updates the property
	 * 'datLastRun'
	 * 
	 * Because of bug: https://java.net/jira/browse/GLASSFISH-20673 we check the
	 * imixsDayOfWeek
	 * 
	 * @param timer
	 * @throws AccessDeniedException
	 */
	@Timeout
	void runTimer(javax.ejb.Timer timer) throws AccessDeniedException {

		ItemCollection configItemCollection = loadConfiguration();
		logger.info(" started....");

		// test if imixsDayOfWeek is provided
		// https://java.net/jira/browse/GLASSFISH-20673
		if (!isImixsDayOfWeek(configItemCollection)) {
			logger.info("runTimer skipped because today is no imixsDayOfWeek");
			return;
		}

		configItemCollection.replaceItemValue("datLastRun", new Date());

		/*
		 * Now we process all scheduled worktitems for each model
		 */
		iProcessWorkItems = 0;
		unprocessedIDs = new ArrayList<String>();
		try {
			// get all model versions...
			List<String> modelVersions = modelService.getVersions();
			for (String version : modelVersions) {
				logger.info("processing ModelVersion: " + version);
				// find scheduled Activities
				Collection<ItemCollection> colScheduledActivities = findScheduledActivities(version);
				logger.info(" " + colScheduledActivities.size() + " scheduled activityEntities found in ModelVersion: "
						+ version);
				// process all workitems for coresponding activities
				for (ItemCollection aactivityEntity : colScheduledActivities) {
					processWorkListByActivityEntity(aactivityEntity);
				}
			}

		} catch (Exception e) {
			logger.severe(" error processing worklist: " + e.getMessage());
			if (logger.isLoggable(Level.FINE)) {
				e.printStackTrace();
			}
		}

		logger.info("finished successfull");

		logger.info(iProcessWorkItems + " workitems processed");

		if (unprocessedIDs.size() > 0) {
			logger.warning(unprocessedIDs.size() + " workitems could be processed!");
			for (String aid : unprocessedIDs) {
				logger.warning("          " + aid);
			}

		}

		Date endDate = configItemCollection.getItemValueDate("datstop");
		String sTimerID = configItemCollection.getItemValueString("$uniqueid");

		// update statistic of last run
		configItemCollection.replaceItemValue("numWorkItemsProcessed", iProcessWorkItems);
		configItemCollection.replaceItemValue("numWorkItemsUnprocessed", unprocessedIDs.size());

		/*
		 * Check if Timer should be canceled now? - only by interval configuration. In
		 * case of calenderBasedTimer the timer will stop automatically.
		 */
		String sConfiguation = configItemCollection.getItemValueString("txtConfiguration");

		if (sConfiguation.isEmpty()) {

			Calendar calNow = Calendar.getInstance();
			if (endDate != null && calNow.getTime().after(endDate)) {
				timer.cancel();
				System.out.println("Timeout - sevice stopped: " + sTimerID);

				SimpleDateFormat dateFormatDE = new SimpleDateFormat("dd.MM.yy hh:mm:ss");
				String msg = "stopped at " + dateFormatDE.format(calNow.getTime()) + " by datstop="
						+ dateFormatDE.format(endDate);
				configItemCollection.replaceItemValue("statusmessage", msg);

			}
		}

		// save configuration
		configItemCollection = saveConfiguration(configItemCollection);

	}

	/**
	 * Create an interval timer whose first expiration occurs at a given point in
	 * time and whose subsequent expirations occur after a specified interval.
	 **/
	Timer createTimerOnInterval(ItemCollection configItemCollection) {
		// Create an interval timer
		Date startDate = configItemCollection.getItemValueDate("datstart");
		Date endDate = configItemCollection.getItemValueDate("datstop");
		long interval = configItemCollection.getItemValueInteger("numInterval");

		// set default start date?
		if (startDate == null) {
			// set start date to now
			startDate = new Date();
		}

		// check if endDate is before start date, than we do not start the
		// timer!
		if (endDate != null) {
			Calendar calStart = Calendar.getInstance();
			calStart.setTime(startDate);
			Calendar calEnd = Calendar.getInstance();
			calEnd.setTime(endDate);
			if (calStart.after(calEnd)) {
				logger.warning(configItemCollection.getItemValueString("txtName") + " stop-date (" + startDate
						+ ") is before start-date (" + endDate + "). Timer will not be started!");
				return null;
			}
		}
		Timer timer = null;
		// create timer object ($uniqueId)
		timer = timerService.createTimer(startDate, interval, configItemCollection.getUniqueID());
		return timer;

	}

	/**
	 * Create a calendar-based timer based on a input schedule expression. The
	 * expression will be parsed by this method.
	 * 
	 * Example: <code>
	 *   second=0
	 *   minute=0
	 *   hour=*
	 *   dayOfWeek=
	 *   dayOfMonth=25–Last,1–5
	 *   month=
	 *   year=*
	 * </code>
	 * 
	 * @param sConfiguation
	 * @return
	 * @throws ParseException
	 */
	Timer createTimerOnCalendar(ItemCollection configItemCollection) throws ParseException {

		TimerConfig timerConfig = new TimerConfig();

		timerConfig.setInfo(configItemCollection.getUniqueID());
		ScheduleExpression scheduerExpression = new ScheduleExpression();

		@SuppressWarnings("unchecked")
		List<String> calendarConfiguation = (List<String>) configItemCollection.getItemValue("txtConfiguration");
		// try to parse the configuration list....
		for (String confgEntry : calendarConfiguation) {

			if (confgEntry.startsWith("second=")) {
				scheduerExpression.second(confgEntry.substring(confgEntry.indexOf('=') + 1));
			}
			if (confgEntry.startsWith("minute=")) {
				scheduerExpression.minute(confgEntry.substring(confgEntry.indexOf('=') + 1));
			}
			if (confgEntry.startsWith("hour=")) {
				scheduerExpression.hour(confgEntry.substring(confgEntry.indexOf('=') + 1));
			}
			if (confgEntry.startsWith("dayOfWeek=")) {
				scheduerExpression.dayOfWeek(confgEntry.substring(confgEntry.indexOf('=') + 1));
			}
			if (confgEntry.startsWith("dayOfMonth=")) {
				scheduerExpression.dayOfMonth(confgEntry.substring(confgEntry.indexOf('=') + 1));
			}
			if (confgEntry.startsWith("month=")) {
				scheduerExpression.month(confgEntry.substring(confgEntry.indexOf('=') + 1));
			}
			if (confgEntry.startsWith("year=")) {
				scheduerExpression.year(confgEntry.substring(confgEntry.indexOf('=') + 1));
			}
			if (confgEntry.startsWith("timezone=")) {
				scheduerExpression.timezone(confgEntry.substring(confgEntry.indexOf('=') + 1));
			}

			/* Start date */
			if (confgEntry.startsWith("start=")) {
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
				Date convertedDate = dateFormat.parse(confgEntry.substring(confgEntry.indexOf('=') + 1));
				scheduerExpression.start(convertedDate);
			}

			/* End date */
			if (confgEntry.startsWith("end=")) {
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
				Date convertedDate = dateFormat.parse(confgEntry.substring(confgEntry.indexOf('=') + 1));
				scheduerExpression.end(convertedDate);
			}

		}

		Timer timer = timerService.createCalendarTimer(scheduerExpression, timerConfig);

		return timer;

	}

	/**
	 * collects all scheduled workflow activities. An scheduled workflow activity is
	 * identified by the attribute keyScheduledActivity="1"
	 * 
	 * The method goes through the latest or a specific Model Version
	 * 
	 */
	Collection<ItemCollection> findScheduledActivities(String aModelVersion) throws Exception {
		Vector<ItemCollection> vectorActivities = new Vector<ItemCollection>();
		Collection<ItemCollection> colProcessList = null;

		// get a complete list of process entities...
		colProcessList = modelService.getModel(aModelVersion).findAllTasks();
		for (ItemCollection aprocessentity : colProcessList) {
			// select all activities for this process entity...
			int processid = aprocessentity.getItemValueInteger("numprocessid");
			logger.fine("Analyse processentity '" + processid + "'");
			Collection<ItemCollection> aActivityList = modelService.getModel(aModelVersion)
					.findAllEventsByTask(processid);

			for (ItemCollection aactivityEntity : aActivityList) {
				logger.fine("Analyse acitity '" + aactivityEntity.getItemValueString("txtname") + "'");

				// check if activity is scheduled
				if ("1".equals(aactivityEntity.getItemValueString("keyScheduledActivity")))
					vectorActivities.add(aactivityEntity);
			}
		}
		return vectorActivities;
	}

	/**
	 * This method returns a timer for a corresponding id if such a timer object
	 * exists.
	 * 
	 * @param id
	 * @return Timer
	 * @throws Exception
	 */
	Timer findTimer(String id) {
		Timer timer = null;
		for (Object obj : timerService.getTimers()) {
			Timer atimer = (javax.ejb.Timer) obj;
			String timerID = atimer.getInfo().toString();
			if (id.equals(timerID)) {
				if (timer != null) {
					logger.severe("more then one timer with id " + id + " was found!");
				}
				timer = atimer;
			}
		}
		return timer;
	}

	/**
	 * This method processes all workitems for a specific processID. the processID
	 * is identified by the activityEntity Object (numprocessid)
	 * 
	 * If the ActivityEntity has defined a EQL statement (attribute
	 * txtscheduledview) then the method selects the workitems by this query.
	 * Otherwise the method use the standard method getWorklistByProcessID()
	 * 
	 * 
	 * @see http://blog.imixs.org/?p=155
	 * 
	 * @param aProcessID
	 * @throws Exception
	 */
	void processWorkListByActivityEntity(ItemCollection activityEntity) throws Exception {

		// get processID
		int iProcessID = activityEntity.getItemValueInteger("numprocessid");
		int iActivityID = activityEntity.getItemValueInteger("numActivityID");
		// get Modelversion
		String sModelVersion = activityEntity.getItemValueString("$modelversion");

		logger.info("processing " + iProcessID + "." + iActivityID + " (" + sModelVersion + ") ...");

		// now we need to select by type, $ProcessID and by $modelVersion!
		String searchTerm = "($processid:\"" + iProcessID + "\" AND $modelversion:\""
				+ sModelVersion + "\")";

		logger.fine("select: " + searchTerm);

		Collection<ItemCollection> worklist = documentService.find(searchTerm, 1000, 0);

		logger.fine(worklist.size() + " workitems found");
		for (ItemCollection workitem : worklist) {
			
			String type=workitem.getType();
			// skip deleted....
			if (type.endsWith("deleted")) {
				continue;
			}			
			
			// skip $immutable Workitems
			if (workitem.getItemValueBoolean("$immutable")) {
				continue;
			}						
			
			// verify due date
			if (workItemInDue(workitem, activityEntity)) {
				String sID = workitem.getItemValueString(WorkflowKernel.UNIQUEID);
				logger.fine("document " + sID + "is in due");
				workitem.replaceItemValue("$activityid", iActivityID);
				try {
					logger.finest("getBusinessObject.....");
					// call from new instance because of transaction new...
					// see: http://blog.imixs.org/?p=155
					// see: https://www.java.net/node/705304
					ctx.getBusinessObject(WorkflowSchedulerService.class).processSingleWorkitem(workitem);
					iProcessWorkItems++;
				} catch (Exception e) {
					logger.warning("error processing workitem: " + sID);
					if (logger.isLoggable(Level.FINEST)) {
						e.printStackTrace();
					}
					unprocessedIDs.add(sID);
				}
			}

		}
	}

	/**
	 * This method process a single workIten in a new transaction. The method is
	 * called by processWorklist()
	 * 
	 * @param aWorkitem
	 * @throws PluginException
	 * @throws ProcessingErrorException
	 * @throws AccessDeniedException
	 * @throws ModelException
	 */
	@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
	public void processSingleWorkitem(ItemCollection aWorkitem)
			throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {
		workflowService.processWorkItem(aWorkitem);
	}

	/**
	 * Update the timer details of a running timer service. The method updates the
	 * properties netxtTimeout and timeRemaining and store them into the timer
	 * configuration.
	 * 
	 * @param configuration
	 */
	private ItemCollection updateTimerDetails(ItemCollection configuration) {
		if (configuration == null)
			return configuration;
		String id = configuration.getUniqueID();
		Timer timer;
		try {
			timer = this.findTimer(id);

			if (timer != null) {
				// load current timer details
				configuration.replaceItemValue("nextTimeout", timer.getNextTimeout());
				configuration.replaceItemValue("timeRemaining", timer.getTimeRemaining());
			} else {
				configuration.removeItem("nextTimeout");
				configuration.removeItem("timeRemaining");
			}
		} catch (Exception e) {
			logger.warning("unable to updateTimerDetails: " + e.getMessage());
			configuration.removeItem("nextTimeout");
			configuration.removeItem("timeRemaining");
		}
		return configuration;
	}

	/**
	 * Returns true if the param 'imixsDayOfWeek' is provided and the current week
	 * day did not match.
	 * 
	 * @see https://java.net/jira/browse/GLASSFISH-20673
	 * @param configItemCollection
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private boolean isImixsDayOfWeek(ItemCollection configItemCollection) {

		List<String> calendarConfiguation = (List<String>) configItemCollection.getItemValue("txtConfiguration");
		// try to parse the configuration list....
		for (String confgEntry : calendarConfiguation) {
			if (confgEntry.startsWith("imixsDayOfWeek=")) {
				logger.info(confgEntry);
				try {
					String dayValue = confgEntry.substring(confgEntry.indexOf('=') + 1);

					int iStartDay = 0;
					int iEndDay = 0;
					int iSeparator = dayValue.indexOf('-');
					if (iSeparator > -1) {
						iStartDay = Integer.valueOf(dayValue.substring(0, iSeparator));
						iEndDay = Integer.valueOf(dayValue.substring(iSeparator + 1));
					} else {
						iStartDay = Integer.valueOf(dayValue);
						iEndDay = iStartDay;
					}

					// get current weekday
					Calendar now = Calendar.getInstance();
					now.setTime(new Date());

					int iDay = now.get(Calendar.DAY_OF_WEEK);
					// sunday = 1
					// adjust
					iDay--;

					if (iDay < iStartDay || iDay > iEndDay) {
						logger.info("imixsDayOfWeek=false");
						return false; // not a imixsDayOfWeek!
					} else {
						logger.info("imixsDayOfWeek=true");
						return true;
					}
				} catch (Exception e) {
					logger.warning("imixsDayOfWeek not parseable!");
				}

			}
		}

		// return true as default to allow run if now value was defined
		return true;
	}

	/**
	 * This method adjusts a given base date for a amount of delay
	 * 
	 * 
	 * @param baseDate
	 *            date object to be adjusted
	 * 
	 * @param offsetUnit
	 *            - time unit (0=sec, 1=min, 2=hours, 3=days, 4=workdays)
	 * @param offset
	 *            offset for adjustment
	 * @return new date object
	 */
	private static Date adjustBaseDate(Date baseDate, int offsetUnit, int offset) {
		if (baseDate != null) {

			// workdays?
			if (offsetUnit == OFFSET_WORKDAYS) {
				Calendar baseCal = Calendar.getInstance();
				baseCal.setTime(baseDate);
				return addWorkDays(baseCal, offset).getTime();

			} else {

				// compute offset in seconds...
				if (offsetUnit == OFFSET_MINUTES) {
					offset *= 60; // min->sec
				} else {
					if (offsetUnit == OFFSET_HOURS) {
						offset *= 3600; // hour->sec
					} else {
						if (offsetUnit == OFFSET_DAYS) {
							offset *= 3600 * 24; // day->sec
						}
					}
				}
				Calendar calTimeCompare = Calendar.getInstance();
				calTimeCompare.setTime(baseDate);
				calTimeCompare.add(Calendar.SECOND, offset);
				return calTimeCompare.getTime();
			}
		} else
			return null;
	}

}
