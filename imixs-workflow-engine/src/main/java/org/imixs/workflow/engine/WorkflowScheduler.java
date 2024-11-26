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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.QuerySelector;
import org.imixs.workflow.bpmn.BPMNEntityBuilder;
import org.imixs.workflow.bpmn.BPMNUtil;
import org.imixs.workflow.engine.scheduler.Scheduler;
import org.imixs.workflow.engine.scheduler.SchedulerException;
import org.imixs.workflow.engine.scheduler.SchedulerService;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.QueryException;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.elements.Activity;
import org.openbpmn.bpmn.elements.core.BPMNElementNode;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/**
 * This EJB implements a Imixs Scheduler Interface and scans workitems for
 * scheduled activities.
 * <p>
 * The configuration of the scheduler is based on the Imixs Scheduler API.
 * 
 * @author rsoika
 * @version 1.0
 */
public class WorkflowScheduler implements Scheduler {

    final static public String NAME = "org.imixs.workflow.scheduler";

    final static public int OFFSET_SECONDS = 0;
    final static public int OFFSET_MINUTES = 1;
    final static public int OFFSET_HOURS = 2;
    final static public int OFFSET_DAYS = 3;
    final static public int OFFSET_WORKDAYS = 4;

    final static private int MAX_WORKITEM_COUNT = 1000;

    private static Logger logger = Logger.getLogger(WorkflowScheduler.class.getName());

    @Inject
    private WorkflowService workflowService;

    @Inject
    private DocumentService documentService;

    @Inject
    private ModelService modelService;

    @Inject
    private SchedulerService schedulerService;

    @Inject
    @Any
    protected Instance<QuerySelector> selectors;

    @Resource
    private SessionContext ctx;

    private int iProcessWorkItems = 0;
    private List<String> unprocessedIDs = null;

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
    public boolean workItemInDue(ItemCollection doc, ItemCollection docActivity) {
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
                    logger.log(Level.WARNING, "error parsing delay in ActivityEntity {0}.{1} :"
                            + " unsuported keyActivityDelayUnit={2}",
                            new Object[] { docActivity.getItemValueInteger("numProcessID"),
                                    docActivity.getItemValueInteger("numActivityID"), sDelayUnit });
                    return false;
                }

            } catch (NumberFormatException nfe) {
                logger.log(Level.WARNING, "error parsing delay in ActivityEntity {0}.{1} :{2}",
                        new Object[] { docActivity.getItemValueInteger("numProcessID"),
                                docActivity.getItemValueInteger("numActivityID"), nfe.getMessage() });
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

            logger.log(Level.FINEST, "......{0} offset ={1} {2}", new Object[] { suniqueid, iOffset, sDelayUnit });

            iCompareType = docActivity.getItemValueInteger("keyScheduledBaseObject");

            // get current time for compare....
            Date dateTimeNow = Calendar.getInstance().getTime();

            switch (iCompareType) {
                // last process -
                case 1: {
                    logger.log(Level.FINEST, "......{0}: CompareType = last event", suniqueid);

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
                        logger.log(Level.WARNING, "{0}: item ''$lastEventDate'' is missing!", suniqueid);
                        return false;
                    }

                    // compute scheduled time
                    logger.log(Level.FINEST, "......{0}: $lastEventDate={1}",
                            new Object[] { suniqueid, dateTimeCompare });
                    dateTimeCompare = adjustBaseDate(dateTimeCompare, iOffsetUnit, iOffset);
                    if (dateTimeCompare != null)
                        return dateTimeCompare.before(dateTimeNow);
                    else
                        return false;
                }

                // last modification
                case 2: {
                    logger.log(Level.FINEST, "......{0}: CompareType = last modify", suniqueid);

                    dateTimeCompare = doc.getItemValueDate("$modified");

                    logger.log(Level.FINEST, "......{0}: modified={1}", new Object[] { suniqueid, dateTimeCompare });

                    dateTimeCompare = adjustBaseDate(dateTimeCompare, iOffsetUnit, iOffset);

                    if (dateTimeCompare != null)
                        return dateTimeCompare.before(dateTimeNow);
                    else
                        return false;
                }

                // creation
                case 3: {
                    logger.log(Level.FINEST, "......{0}: CompareType = creation", suniqueid);

                    dateTimeCompare = doc.getItemValueDate("$created");
                    logger.log(Level.FINEST, "......{0}: doc.getCreated() ={1}",
                            new Object[] { suniqueid, dateTimeCompare });

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
                    logger.log(Level.FINEST, "......{0}: CompareType = field: ''{1}''",
                            new Object[] { suniqueid, sNameOfField });

                    if (!doc.hasItem(sNameOfField)) {
                        logger.log(Level.FINEST, "......{0}: CompareType ={1} no value found!",
                                new Object[] { suniqueid, sNameOfField });
                        return false;
                    }

                    dateTimeCompare = doc.getItemValueDate(sNameOfField);

                    logger.log(Level.FINEST, "......{0}: {1}={2}",
                            new Object[] { suniqueid, sNameOfField, dateTimeCompare });

                    dateTimeCompare = adjustBaseDate(dateTimeCompare, iOffsetUnit, iOffset);
                    if (dateTimeCompare != null) {
                        logger.log(Level.FINEST, "......{0}: Compare {1} <-> {2}",
                                new Object[] { suniqueid, dateTimeCompare, dateTimeNow });

                        if (dateTimeCompare.before(dateTimeNow)) {
                            logger.log(Level.FINEST, "......{0} isInDue!", suniqueid);
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
    public Calendar addWorkDays(final Calendar baseDate, final int days) {
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
            logger.log(Level.FINEST, "......addWorkDays ({0}) + {1} = ({2})",
                    new Object[] { baseDate.getTime(), days, resultDate.getTime() });
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
    @Override
    public ItemCollection run(ItemCollection configItemCollection) throws SchedulerException {

        /*
         * Now we process all scheduled worktitems for each model
         */
        iProcessWorkItems = 0;
        unprocessedIDs = new ArrayList<String>();
        try {
            // get all model versions...
            List<String> modelVersions = modelService.getModelManager().getVersions();

            // sort versions in descending order (issue #482)
            Collections.sort(modelVersions, Collections.reverseOrder());

            for (String version : modelVersions) {
                // find scheduled Events

                // Erst müss ma aller tasks finden und dann die gültigen Trigger Events.
                // Damti kann man dann eien Such estarten und das zeug processen....

                // processWorkListByEvent(version, taskID, EventID, Group)

                BPMNModel model = modelService.getModelManager().getModel(version);
                if (model != null) {
                    // find all tasks
                    Set<Activity> activities = model.findAllActivities();
                    for (Activity task : activities) {
                        if (BPMNUtil.isImixsTaskElement(task)) {
                            ItemCollection taskEntity = BPMNEntityBuilder.build(task);
                            int taskID = taskEntity.getItemValueInteger(BPMNUtil.TASK_ITEM_TASKID);
                            // iterate through all scheduled events
                            List<ItemCollection> events = modelService.getModelManager().findEventsByTask(model,
                                    taskID);
                            for (ItemCollection eventEntity : events) {
                                // test if this is a scheduled event...
                                if (eventEntity.getItemValueBoolean(BPMNUtil.EVENT_ITEM_TIMER_ACTIVE)) {
                                    // eventID = eventEntity.getItemValueInteger(BPMNUtil.EVENT_ITEM_EVENTID);
                                    processWorkListByEvent(model, taskEntity, eventEntity, configItemCollection);
                                }
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing worklist: {0}", e.getMessage());
            if (logger.isLoggable(Level.FINE)) {
                e.printStackTrace();
            }
        }

        schedulerService.logMessage("================================", configItemCollection, null);
        schedulerService.logMessage(iProcessWorkItems + " workitems processed in total", configItemCollection, null);

        if (unprocessedIDs.size() > 0) {
            schedulerService.logWarning(unprocessedIDs.size() + " workitems could not be processed:",
                    configItemCollection, null);
            for (String aid : unprocessedIDs) {
                schedulerService.logWarning("          " + aid, configItemCollection, null);
            }
        }

        // update statistic of last run
        configItemCollection.replaceItemValue("numWorkItemsProcessed", iProcessWorkItems);
        configItemCollection.replaceItemValue("numWorkItemsUnprocessed", unprocessedIDs.size());

        return configItemCollection;
    }

    /**
     * This method processes all workitems for a specific scheduled event element of
     * a workflow model. A scheduled event element can define a selector
     * (txtscheduledview). If no selector is defined, the default selector is used:
     * <p>
     * {@code
     * ($taskid:"[TASKID]" AND $modelversion:"[MODELVERSION]")
     * }
     * 
     * @param event - a event model element
     * @throws ModelException
     * @throws QueryException
     * @throws Exception
     */
    protected void processWorkListByEvent(BPMNModel model, ItemCollection taskEntity, ItemCollection eventEntity,
            ItemCollection configItemCollection)
            throws ModelException, QueryException {

        // get taskID and workflowGroup
        String modelVersion = BPMNUtil.getVersion(model);
        BPMNElementNode task = model.findElementNodeById(taskEntity.getItemValueString("id"));
        int taskID = taskEntity.getItemValueInteger(BPMNUtil.TASK_ITEM_TASKID);
        int eventID = eventEntity.getItemValueInteger(BPMNUtil.EVENT_ITEM_EVENTID);
        String workflowGroup = task.getBpmnProcess().getName();

        // create selector....
        String searchTerm = null;
        searchTerm = eventEntity.getItemValueString(BPMNUtil.EVENT_ITEM_TIMER_SELECTION);
        if (searchTerm.isEmpty()) {
            // build the default selector....
            searchTerm = "($taskid:\"" + taskID + "\" AND $workflowgroup:\"" + workflowGroup + "\")";
        }

        // In the following code we use a pagination to iterate over all workitems
        // defined by the selector. This is necessary because in some cases the
        // workitems in selection can be more then the MAX_WORKITEM_COUNT
        int currentPageIndex = 0;
        List<ItemCollection> worklistCollector = new ArrayList<ItemCollection>();
        while (true) {
            List<ItemCollection> worklist = null;
            // test if selector is a CDI Bean
            String classPattern = "^[a-z][a-z0-9_]*(\\.[A-Za-z0-9_]+)+$";
            if (Pattern.compile(classPattern).matcher(searchTerm).find()) {
                QuerySelector selector = findSelectorByName(searchTerm);
                if (selector != null) {
                    schedulerService.logMessage("......CDI selector = " + searchTerm, configItemCollection, null);
                    worklist = selector.find(MAX_WORKITEM_COUNT, currentPageIndex);
                }
            } else {
                schedulerService.logMessage("......selector = " + searchTerm, configItemCollection, null);
                worklist = documentService.find(searchTerm, MAX_WORKITEM_COUNT, currentPageIndex);
            }

            // if we do not found any workitems we can break here
            if (worklist.size() == 0) {
                break;
            } else {
                logger.log(Level.FINEST, "......{0} workitems found in total, collect due date...", worklist.size());
                // update collector.....
                collectWorkitemsInDue(eventEntity, modelVersion, worklist, worklistCollector);
                if (worklist.size() < MAX_WORKITEM_COUNT) {
                    break;
                } else {
                    // increase current page index
                    currentPageIndex++;
                }
            }

            // if the worklistCollector size is > than the MAX_WOKITEM_COUNT we break
            if (worklistCollector.size() >= MAX_WORKITEM_COUNT) {
                schedulerService.logMessage(
                        "...more than " + MAX_WORKITEM_COUNT + " workitems in due found in current selector!",
                        configItemCollection, null);
                break;
            }
            schedulerService.logMessage("...verify next " + MAX_WORKITEM_COUNT + " workitems for current selector...",
                    configItemCollection, null);
        }

        // Now we iterate all workitems in the collector
        if (worklistCollector.size() > 0) {
            schedulerService.logMessage("......processing " + worklistCollector.size() + " workitems in due...",
                    configItemCollection, null);
            for (ItemCollection workitem : worklistCollector) {
                workitem.setEventID(eventID);
                try {
                    logger.finest("......getBusinessObject.....");
                    // call from new instance because of transaction new...
                    // see: http://blog.imixs.org/?p=155
                    // see: https://www.java.net/node/705304
                    workitem = workflowService.processWorkItemByNewTransaction(workitem);
                    iProcessWorkItems++;
                } catch (Exception e) {
                    logger.log(Level.WARNING, "error processing workitem: {0} Error={1}",
                            new Object[] { workitem.getUniqueID(), e.getMessage() });
                    if (logger.isLoggable(Level.FINEST)) {
                        e.printStackTrace();
                    }
                    unprocessedIDs.add(workitem.getUniqueID());
                }
            }
        }
    }

    /**
     * This helper method iterates over a collection of workitems and tests for each
     * workitem if its dueDate matches a given BPMN event. If so the workitem is
     * added to the given workitemCollector.
     * <p>
     * The method is called form processWorklistByEvent which iterates over a all
     * workItems selected by specific selector.
     * <p>
     * In case an old $modelVersion was detected, the method tries to migrate to the
     * latest model version. (issue #482)
     * 
     * @param event
     * @param eventID
     * @param modelVersion
     * @param worklist
     */
    private void collectWorkitemsInDue(ItemCollection event, String modelVersion, List<ItemCollection> worklist,
            List<ItemCollection> collector) {

        for (ItemCollection workitem : worklist) {
            String type = workitem.getType();
            // skip deleted....
            if (type.endsWith("deleted")) {
                continue;
            }

            // skip $immutable Workitems
            if (workitem.getItemValueBoolean("$immutable")) {
                continue;
            }

            // issue #482
            // If the modelversion did not match the eventModelVersion, than migrate the
            // model version...
            if (!modelVersion.equals(workitem.getModelVersion())) {
                // test if the old model version still exists.
                try {
                    modelService.getModelManager().getModel(workitem.getModelVersion());
                    logger.finest("......skip because model version is older than current version...");
                    // will be processed in the following loops..
                    continue;
                } catch (ModelException me) {
                    // ModelException - we migrate the model ...
                    logger.log(Level.FINE, "...deprecated model version ''{0}'' no longer exists ->"
                            + " migrating to new model version ''{1}''",
                            new Object[] { workitem.getModelVersion(), modelVersion });
                    workitem.model(modelVersion);
                }
            }

            // verify due date
            if (workItemInDue(workitem, event)) {
                logger.log(Level.FINEST, "......document {0}is in due", workitem.getUniqueID());
                collector.add(workitem);
            }
        }
    }

    /**
     * This method adjusts a given base date for a amount of delay
     * 
     * 
     * @param baseDate   date object to be adjusted
     * 
     * @param offsetUnit - time unit (0=sec, 1=min, 2=hours, 3=days, 4=workdays)
     * @param offset     offset for adjustment
     * @return new date object
     */
    private Date adjustBaseDate(Date baseDate, int offsetUnit, int offset) {
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

    /**
     * This method returns an injected Plugin by name or null if no plugin with the
     * requested class name is injected.
     * 
     * @param selectorClassName
     * @return plugin class or null if not found
     */
    private QuerySelector findSelectorByName(String selectorClassName) {
        if (selectorClassName == null || selectorClassName.isEmpty())
            return null;
        boolean debug = logger.isLoggable(Level.FINE);

        if (selectors == null || !selectors.iterator().hasNext()) {
            if (debug) {
                logger.finest("......no CDI selectors injected");
            }
            return null;
        }
        // iterate over all injected selectors....
        for (QuerySelector selector : this.selectors) {
            if (selector.getClass().getName().equals(selectorClassName)) {
                if (debug) {
                    logger.log(Level.FINEST, "......CDI selector ''{0}'' successful injected", selectorClassName);
                }
                return selector;
            }
        }

        return null;
    }
}
