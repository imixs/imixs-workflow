package org.imixs.workflow.jee.ejb;

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
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;

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
public class WorkflowSchedulerService implements WorkflowSchedulerServiceRemote {

	final static public String TYPE = "configuration";
	final static public String NAME = "org.imixs.workflow.scheduler";

	private static Logger logger = Logger
			.getLogger(WorkflowSchedulerService.class.getName());

	@EJB
	WorkflowService workflowService;

	@EJB
	EntityService entityService;

	@EJB
	ModelService modelService;

	@Resource
	javax.ejb.TimerService timerService;

	@Resource
	SessionContext ctx;

	int iProcessWorkItems = 0;
	List<String> unprocessedIDs = null;

	/**
	 * This method loads the current scheduler configuration. If no
	 * configuration entity yet exists the method returns an empty
	 * ItemCollection. The method updates the timer details netxtTimeout and
	 * timeRemaining of a running timer service.
	 * 
	 * @return configuration ItemCollection
	 */
	public ItemCollection loadConfiguration() {
		ItemCollection configItemCollection = null;
		String sQuery = "SELECT config FROM Entity AS config "
				+ " JOIN config.textItems AS t2" + " WHERE config.type = '"
				+ TYPE + "'" + " AND t2.itemName = 'txtname'"
				+ " AND t2.itemValue = '" + NAME + "'"
				+ " ORDER BY t2.itemValue asc";
		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				0, 1);

		if (col.size() > 0) {
			configItemCollection = col.iterator().next();

		} else {
			// create default values
			configItemCollection = new ItemCollection();
			try {
				configItemCollection.replaceItemValue("type", TYPE);
				configItemCollection.replaceItemValue("txtname", NAME);

			} catch (Exception e) {
				e.printStackTrace();
			}

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
	public ItemCollection saveConfiguration(ItemCollection configItemCollection)
			throws AccessDeniedException {
		// update write and read access
		configItemCollection.replaceItemValue("type", TYPE);
		configItemCollection.replaceItemValue("txtName", NAME);
		configItemCollection.replaceItemValue("$writeAccess",
				"org.imixs.ACCESSLEVEL.MANAGERACCESS");
		configItemCollection.replaceItemValue("$readAccess",
				"org.imixs.ACCESSLEVEL.MANAGERACCESS");

		// configItemCollection.replaceItemValue("$writeAccess", "");
		// configItemCollection.replaceItemValue("$readAccess", "");

		configItemCollection = updateTimerDetails(configItemCollection);
		// save entity
		configItemCollection = entityService.save(configItemCollection);

		return configItemCollection;
	}

	/**
	 * This Method starts the TimerService.
	 * 
	 * The Timer can be started based on a Calendar setting stored in the
	 * property txtConfiguration, or by interval based on the properties
	 * datStart, datStop, numIntervall.
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
	 * The method throws an exception if the configuration entity contains
	 * invalid attributes or values.
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

		String id = configItemCollection.getItemValueString("$uniqueid");

		// try to cancel an existing timer for this workflowinstance
		while (this.findTimer(id) != null) {
			this.findTimer(id).cancel();
		}

		String sConfiguation = configItemCollection
				.getItemValueString("txtConfiguration");

		if (!sConfiguation.isEmpty()) {
			// New timer will be started on calendar confiugration
			timer = createTimerOnCalendar(configItemCollection);
		} else {
			// update the interval based on hour/minute configuration
			int hours = configItemCollection.getItemValueInteger("hours");
			int minutes = configItemCollection.getItemValueInteger("minutes");
			long interval = (hours * 60 + minutes) * 60 * 1000;
			configItemCollection.replaceItemValue("numInterval", new Long(
					interval));

			timer = createTimerOnInterval(configItemCollection);
		}

		// start and set statusmessage
		if (timer != null) {

			Calendar calNow = Calendar.getInstance();
			SimpleDateFormat dateFormatDE = new SimpleDateFormat(
					"dd.MM.yy hh:mm:ss");
			String msg = "started at " + dateFormatDE.format(calNow.getTime())
					+ " by " + ctx.getCallerPrincipal().getName();
			configItemCollection.replaceItemValue("statusmessage", msg);

			if (timer.isCalendarTimer()) {
				configItemCollection.replaceItemValue("Schedule", timer
						.getSchedule().toString());
			} else {
				configItemCollection.replaceItemValue("Schedule", "");

			}
			logger.info("[WorkflowSchedulerService] "
					+ configItemCollection.getItemValueString("txtName")
					+ " started: " + id);
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

		String id = configItemCollection.getItemValueString("$uniqueid");
		boolean found = false;
		while (this.findTimer(id) != null) {
			this.findTimer(id).cancel();
			found = true;
		}
		if (found) {

			Calendar calNow = Calendar.getInstance();
			SimpleDateFormat dateFormatDE = new SimpleDateFormat(
					"dd.MM.yy hh:mm:ss");

			String msg = "stopped at " + dateFormatDE.format(calNow.getTime())
					+ " by " + ctx.getCallerPrincipal().getName();
			configItemCollection.replaceItemValue("statusmessage", msg);

			logger.info("[WorkflowSchedulerService] "
					+ configItemCollection.getItemValueString("txtName")
					+ " stopped: " + id);
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

			return (findTimer(configItemCollection
					.getItemValueString("$uniqueid")) != null);
		} catch (Exception e) {

			e.printStackTrace();
			return false;
		}
	}

	/**
	 * This method checks if a workitem (doc) is in due. There are 4 different
	 * cases which will be compared: The case is determined by the
	 * keyScheduledBaseObject of the activity entity
	 * 
	 * Basis : keyScheduledBaseObject "last process"=1, "last Modification"=2
	 * "Creation"=3 "Field"=4
	 * 
	 * The logic is not the best one but it works. So we are open for any kind
	 * of improvements
	 * 
	 * @return true if workitem is is due
	 */
	public static boolean workItemInDue(ItemCollection doc,
			ItemCollection docActivity) {
		try {
			int iCompareType = -1;
			int iDelayUnit = -1;

			Date dateTimeCompare = null;
			// int iRepeatTime = 0,
			int iActivityDelay = 0;

			String suniqueid = doc.getItemValueString("$uniqueid");

			String sDelayUnit = docActivity
					.getItemValueString("keyActivityDelayUnit");
			try {
				iDelayUnit = Integer.parseInt(sDelayUnit); // min | 1; hours |
															// 2;

			} catch (NumberFormatException nfe) {
				logger.warning("[WorkflowSchedulerService] error parsing delay in ActivityEntity "
						+ docActivity.getItemValueInteger("numProcessID")
						+ "."
						+ docActivity.getItemValueInteger("numActivityID")
						+ " :" + nfe.getMessage());
				return false;
			}
			// days | 3
			// iRepeatTime =
			// docActivity.getItemValueInteger("numActivityMinOffset");
			iActivityDelay = docActivity
					.getItemValueInteger("numActivityDelay");
			if (true) {
				if ("1".equals(sDelayUnit))
					sDelayUnit = "minutes";
				if ("2".equals(sDelayUnit))
					sDelayUnit = "hours";
				if ("3".equals(sDelayUnit))
					sDelayUnit = "days";

				logger.finest("[WorkflowSchedulerService] " + suniqueid
						+ " delay =" + iActivityDelay + " " + sDelayUnit);

			}
			// Delay in sekunden umrechnen
			if (iDelayUnit == 1) {
				iActivityDelay *= 60; // min->sec
			} else {
				if (iDelayUnit == 2) {
					iActivityDelay *= 3600; // hour->sec
				} else {
					if (iDelayUnit == 3) {
						iActivityDelay *= 3600 * 24; // day->sec
					}
				}
			}

			iCompareType = Integer.parseInt(docActivity
					.getItemValueString("keyScheduledBaseObject"));

			// get current time for compare....
			Date dateTimeNow = Calendar.getInstance().getTime();

			switch (iCompareType) {
			// last process -
			case 1: {
				logger.finest("[WorkflowSchedulerService] " + suniqueid
						+ ": CompareType = last process");

				if (!doc.hasItem("timWorkflowLastAccess"))
					return false;

				dateTimeCompare = doc.getItemValueDate("timWorkflowLastAccess");
				logger.finest("[WorkflowSchedulerService] " + suniqueid
						+ ": timWorkflowLastAccess=" + dateTimeCompare);

				// scheduled time
				dateTimeCompare = adjustSecond(dateTimeCompare, iActivityDelay);

				return dateTimeCompare.before(dateTimeNow);
			}

			// last modification - es erfolgt kein Vergleich mit last
			// Event, da dieses ja selbst der auslöser der Zeit ist
			case 2: {
				logger.finest("[WorkflowSchedulerService] " + suniqueid
						+ ": CompareType = last modify");

				dateTimeCompare = doc.getItemValueDate("$modified");

				logger.finest("[WorkflowSchedulerService] " + suniqueid
						+ ": modified=" + dateTimeCompare);

				dateTimeCompare = adjustSecond(dateTimeCompare, iActivityDelay);

				return dateTimeCompare.before(dateTimeNow);
			}

			// creation
			case 3: {
				logger.finest("[WorkflowSchedulerService] " + suniqueid
						+ ": CompareType = creation");

				dateTimeCompare = doc.getItemValueDate("$created");
				logger.fine("[WorkflowSchedulerService] " + suniqueid
						+ ": doc.getCreated() =" + dateTimeCompare);

				// Nein -> Creation date ist masstab
				dateTimeCompare = adjustSecond(dateTimeCompare, iActivityDelay);

				return dateTimeCompare.before(dateTimeNow);
			}

			// field
			case 4: {
				String sNameOfField = docActivity
						.getItemValueString("keyTimeCompareField");
				logger.finest("[WorkflowSchedulerService] " + suniqueid
						+ ": CompareType = field: '" + sNameOfField + "'");

				if (!doc.hasItem(sNameOfField)) {
					logger.finest("[WorkflowSchedulerService] " + suniqueid
							+ ": CompareType =" + sNameOfField
							+ " no value found!");
					return false;
				}

				dateTimeCompare = doc.getItemValueDate(sNameOfField);

				logger.finest("[WorkflowSchedulerService] " + suniqueid + ": "
						+ sNameOfField + "=" + dateTimeCompare);

				dateTimeCompare = adjustSecond(dateTimeCompare, iActivityDelay);

				logger.finest("[WorkflowSchedulerService] " + suniqueid
						+ ": Compare " + dateTimeCompare + " <-> "
						+ dateTimeNow);

				if (dateTimeCompare.before(dateTimeNow)) {
					logger.finest("[WorkflowSchedulerService] " + suniqueid
							+ " isInDue!");
				}
				return dateTimeCompare.before(dateTimeNow);
			}
			default:
				return false;
			}

		} catch (Exception e) {

			e.printStackTrace();
			return false;
		}

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
		logger.info("[WorkflowSchedulerService] started....");

		// test if imixsDayOfWeek is provided
		// https://java.net/jira/browse/GLASSFISH-20673
		if (!isImixsDayOfWeek(configItemCollection)) {
			logger.info("[WorkflowSchedulerService] runTimer skipped because today is no imixsDayOfWeek");
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
			List<String> modelVersions = modelService.getAllModelVersions();
			for (String version : modelVersions) {
				logger.info("[WorkflowSchedulerService] processing ModelVersion: "
						+ version);
				// find scheduled Activities
				Collection<ItemCollection> colScheduledActivities = findScheduledActivities(version);
				logger.info("[WorkflowSchedulerService] "
						+ colScheduledActivities.size()
						+ " scheduled activityEntities found in ModelVersion: "
						+ version);
				// process all workitems for coresponding activities
				for (ItemCollection aactivityEntity : colScheduledActivities) {
					processWorkListByActivityEntity(aactivityEntity);
				}
			}

		} catch (Exception e) {
			logger.severe("[WorkflowSchedulerService] error processing worklist: "
					+ e.getMessage());
			if (logger.isLoggable(Level.FINE)) {
				e.printStackTrace();
			}
		}

		logger.info("[WorkflowSchedulerService] finished successfull");

		logger.info("[WorkflowSchedulerService] " + iProcessWorkItems
				+ " workitems processed");

		if (unprocessedIDs.size() > 0) {
			logger.warning("[WorkflowSchedulerService] "
					+ unprocessedIDs.size() + " workitems could be processed!");
			for (String aid : unprocessedIDs) {
				logger.warning("[WorkflowSchedulerService]          " + aid);
			}

		}

		Date endDate = configItemCollection.getItemValueDate("datstop");
		String sTimerID = configItemCollection.getItemValueString("$uniqueid");

		// update statistic of last run

		configItemCollection.replaceItemValue("numWorkItemsProcessed",
				iProcessWorkItems);
		configItemCollection.replaceItemValue("numWorkItemsUnprocessed",
				unprocessedIDs.size());

		/*
		 * Check if Timer should be canceld now? - only by interval
		 * configuration. In case of calenderBasedTimer the timer will stop
		 * automatically.
		 */
		String sConfiguation = configItemCollection
				.getItemValueString("txtConfiguration");

		if (sConfiguation.isEmpty()) {

			Calendar calNow = Calendar.getInstance();
			if (endDate != null && calNow.getTime().after(endDate)) {
				timer.cancel();
				System.out
						.println("[WorkflowSchedulerService] Timeout sevice stopped: "
								+ sTimerID);

				SimpleDateFormat dateFormatDE = new SimpleDateFormat(
						"dd.MM.yy hh:mm:ss");
				String msg = "stopped at "
						+ dateFormatDE.format(calNow.getTime())
						+ " by datstop=" + dateFormatDE.format(endDate);
				configItemCollection.replaceItemValue("statusmessage", msg);

			}
		}

		// save configuration
		configItemCollection = saveConfiguration(configItemCollection);

	}

	/**
	 * Create an interval timer whose first expiration occurs at a given point
	 * in time and whose subsequent expirations occur after a specified
	 * interval.
	 **/
	Timer createTimerOnInterval(ItemCollection configItemCollection) {

		// Create an interval timer
		Date startDate = configItemCollection.getItemValueDate("datstart");
		Date endDate = configItemCollection.getItemValueDate("datstop");
		long interval = configItemCollection.getItemValueInteger("numInterval");
		// if endDate is in the past we do not start the timer!
		Calendar calNow = Calendar.getInstance();
		Calendar calEnd = Calendar.getInstance();

		if (endDate != null)
			calEnd.setTime(endDate);
		if (calNow.after(calEnd)) {
			logger.warning("[WorkflowSchedulerService] "
					+ configItemCollection.getItemValueString("txtName")
					+ " stop-date is in the past");

			endDate = startDate;
		}
		Timer timer = timerService.createTimer(startDate, interval,
				configItemCollection);

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
	Timer createTimerOnCalendar(ItemCollection configItemCollection)
			throws ParseException {

		TimerConfig timerConfig = new TimerConfig();
		timerConfig.setInfo(configItemCollection);
		ScheduleExpression scheduerExpression = new ScheduleExpression();

		@SuppressWarnings("unchecked")
		List<String> calendarConfiguation = configItemCollection
				.getItemValue("txtConfiguration");
		// try to parse the configuration list....
		for (String confgEntry : calendarConfiguation) {

			if (confgEntry.startsWith("second=")) {
				scheduerExpression.second(confgEntry.substring(confgEntry
						.indexOf('=') + 1));
			}
			if (confgEntry.startsWith("minute=")) {
				scheduerExpression.minute(confgEntry.substring(confgEntry
						.indexOf('=') + 1));
			}
			if (confgEntry.startsWith("hour=")) {
				scheduerExpression.hour(confgEntry.substring(confgEntry
						.indexOf('=') + 1));
			}
			if (confgEntry.startsWith("dayOfWeek=")) {
				scheduerExpression.dayOfWeek(confgEntry.substring(confgEntry
						.indexOf('=') + 1));
			}
			if (confgEntry.startsWith("dayOfMonth=")) {
				scheduerExpression.dayOfMonth(confgEntry.substring(confgEntry
						.indexOf('=') + 1));
			}
			if (confgEntry.startsWith("month=")) {
				scheduerExpression.month(confgEntry.substring(confgEntry
						.indexOf('=') + 1));
			}
			if (confgEntry.startsWith("year=")) {
				scheduerExpression.year(confgEntry.substring(confgEntry
						.indexOf('=') + 1));
			}
			if (confgEntry.startsWith("timezone=")) {
				scheduerExpression.timezone(confgEntry.substring(confgEntry
						.indexOf('=') + 1));
			}

			/* Start date */
			if (confgEntry.startsWith("start=")) {
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
				Date convertedDate = dateFormat.parse(confgEntry
						.substring(confgEntry.indexOf('=') + 1));
				scheduerExpression.start(convertedDate);
			}

			/* End date */
			if (confgEntry.startsWith("end=")) {
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
				Date convertedDate = dateFormat.parse(confgEntry
						.substring(confgEntry.indexOf('=') + 1));
				scheduerExpression.end(convertedDate);
			}

		}

		Timer timer = timerService.createCalendarTimer(scheduerExpression,
				timerConfig);

		return timer;

	}

	/**
	 * collects all scheduled workflow activities. An scheduled workflow
	 * activity is identified by the attribute keyScheduledActivity="1"
	 * 
	 * The method goes through the latest or a specific Model Version
	 * 
	 */
	Collection<ItemCollection> findScheduledActivities(String aModelVersion)
			throws Exception {
		Vector<ItemCollection> vectorActivities = new Vector<ItemCollection>();
		Collection<ItemCollection> colProcessList = null;

		// get a complete list of process entities...
		if (aModelVersion != null)
			colProcessList = modelService
					.getProcessEntityListByVersion(aModelVersion);
		else
			colProcessList = modelService.getProcessEntityList();
		for (ItemCollection aprocessentity : colProcessList) {
			// select all activities for this process entity...
			int processid = aprocessentity.getItemValueInteger("numprocessid");
			// System.out.println("Analyse processentity '" + processid+ "'");
			Collection<ItemCollection> aActivityList = modelService
					.getActivityEntityListByVersion(processid, aModelVersion);

			for (ItemCollection aactivityEntity : aActivityList) {
				// System.out.println("Analyse acitity '" + aactivityEntity
				// .getItemValueString("txtname") + "'");

				// check if activity is scheduled
				if ("1".equals(aactivityEntity
						.getItemValueString("keyScheduledActivity")))
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
			if (atimer.getInfo() instanceof ItemCollection) {
				ItemCollection adescription = (ItemCollection) atimer.getInfo();
				if (id.equals(adescription.getItemValueString("$uniqueid"))) {
					if (timer != null)
						logger.severe("[WorkflowScheduelrService] - more then one timer with id "
								+ id + " was found!");
					timer = atimer;
				}
			}
		}
		return timer;
	}

	/**
	 * This method processes all workitems for a specific processID. the
	 * processID is identified by the activityEntity Object (numprocessid)
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
	void processWorkListByActivityEntity(ItemCollection activityEntity)
			throws Exception {

		// get processID
		int iProcessID = activityEntity.getItemValueInteger("numprocessid");
		int iActivityID = activityEntity.getItemValueInteger("numActivityID");
		// get Modelversion
		String sModelVersion = activityEntity
				.getItemValueString("$modelversion");

		logger.info("[WorkflowSchedulerService] processing " + iProcessID + "."
				+ iActivityID + " (" + sModelVersion + ") ...");

		// now we need to select by type, $ProcessID and by $modelVersion!
		String sQuery = "SELECT wi FROM Entity as wi "
				+ " JOIN wi.integerItems AS i " + " JOIN wi.textItems as t "
				+ " WHERE wi.type='workitem' ";
		sQuery += " AND i.itemName = '$processid' AND i.itemValue = '"
				+ iProcessID + "'"
				+ " AND t.itemName = '$modelversion' AND t.itemValue = '"
				+ sModelVersion + "'";

		logger.fine("[WorkflowSchedulerService] select: " + sQuery);

		Collection<ItemCollection> worklist = entityService.findAllEntities(
				sQuery, 0, -1);

		logger.fine("[WorkflowSchedulerService] " + worklist.size()
				+ " workitems found");
		for (ItemCollection workitem : worklist) {
			// verify due date
			if (workItemInDue(workitem, activityEntity)) {
				String sID = workitem
						.getItemValueString(EntityService.UNIQUEID);
				logger.fine("[WorkflowSchedulerService] workitem " + sID
						+ "is in due");
				workitem.replaceItemValue("$activityid", iActivityID);
				try {
					logger.finest("[WorkflowSchedulerService] getBusinessObject.....");
					// call from new instance because of transaction new...
					// see: http://blog.imixs.org/?p=155
					// see: https://www.java.net/node/705304
					ctx.getBusinessObject(WorkflowSchedulerService.class)
							.processSingleWorkitem(workitem);
					iProcessWorkItems++;
				} catch (Exception e) {
					logger.warning("[WorkflowSchedulerService] error processing workitem: "
							+ sID);
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
	 */
	@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
	public void processSingleWorkitem(ItemCollection aWorkitem)
			throws AccessDeniedException, ProcessingErrorException,
			PluginException {
		workflowService.processWorkItem(aWorkitem);
	}

	/**
	 * Update the timer details of a running timer service. The method updates
	 * the properties netxtTimeout and timeRemaining and store them into the
	 * timer configuration.
	 * 
	 * @param configuration
	 */
	private ItemCollection updateTimerDetails(ItemCollection configuration) {
		if (configuration == null)
			return configuration;
		String id = configuration.getItemValueString("$uniqueid");
		Timer timer;
		try {
			timer = this.findTimer(id);

			if (timer != null) {
				// load current timer details
				configuration.replaceItemValue("nextTimeout",
						timer.getNextTimeout());
				configuration.replaceItemValue("timeRemaining",
						timer.getTimeRemaining());
			} else {
				configuration.removeItem("nextTimeout");
				configuration.removeItem("timeRemaining");

			}
		} catch (Exception e) {
			logger.warning("[WorkflowSchedulerService] unable to updateTimerDetails: "
					+ e.getMessage());
			configuration.removeItem("nextTimeout");
			configuration.removeItem("timeRemaining");

		}
		return configuration;
	}

	/**
	 * Returns true if the param 'imixsDayOfWeek' is provided and the current
	 * week day did not match.
	 * 
	 * @see https://java.net/jira/browse/GLASSFISH-20673
	 * @param configItemCollection
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private boolean isImixsDayOfWeek(ItemCollection configItemCollection) {

		List<String> calendarConfiguation = configItemCollection
				.getItemValue("txtConfiguration");
		// try to parse the configuration list....
		for (String confgEntry : calendarConfiguation) {
			if (confgEntry.startsWith("imixsDayOfWeek=")) {
				logger.info("[WorkflowSchedulerService] " + confgEntry);
				try {
					String dayValue = confgEntry.substring(confgEntry
							.indexOf('=') + 1);

					int iStartDay = 0;
					int iEndDay = 0;
					int iSeparator = dayValue.indexOf('-');
					if (iSeparator > -1) {
						iStartDay = Integer.valueOf(dayValue.substring(0,
								iSeparator));
						iEndDay = Integer.valueOf(dayValue
								.substring(iSeparator + 1));
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
						logger.info("[WorkflowSchedulerService] imixsDayOfWeek=false");
						return false; // not a imixsDayOfWeek!
					} else {
						logger.info("[WorkflowSchedulerService] imixsDayOfWeek=true");
						return true;
					}
				} catch (Exception e) {
					logger.warning("[WorkflowSchedulerService] imixsDayOfWeek not parseable!");
				}

			}
		}

		// return true as default to allow run if now value was defined
		return true;
	}

	/**
	 * This method add seconds to a given date object
	 * 
	 * @param adate
	 *            to be adjusted
	 * @param seconds
	 *            to be added (can be <0)
	 * @return new date object
	 */
	private static Date adjustSecond(Date adate, int seconds) {
		Calendar calTimeCompare = Calendar.getInstance();
		calTimeCompare.setTime(adate);
		calTimeCompare.add(Calendar.SECOND, seconds);
		return calTimeCompare.getTime();
	}

}
