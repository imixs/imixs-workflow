/*******************************************************************************
 * <pre>
 *  Imixs Workflow 
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
 *      Imixs Software Solutions GmbH - initial API and implementation
 *      Ralph Soika - Software Developer
 * </pre>
 *******************************************************************************/

package org.imixs.workflow.engine.adminp;

import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RunAs;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.exceptions.AccessDeniedException;

/**
 * The AmdinPService provides a mechanism to start long running jobs. Those jobs can be used to
 * update workitems in a scheduled batch process. This is called a AdminP-Process. The result of a
 * adminp process is documented into an log entity from type='adminp'. The job description is stored
 * in the field '$WorkflowSummary'. The current startpos and maxcount are stored in the
 * configuration entity in the properties 'numStart' 'numMaxCount'
 * 
 * The service provides methods to create and start different types of jobs. The job type is stored
 * in the field 'job':
 * 
 * RenameUserJob:
 * 
 * This job is to replace entries in the fields $WriteAccess, $ReadAccess and owner. An update
 * request is stored in a adminp entity containing alll necessary informations. The service starts a
 * timer instances for each update process
 * 
 * 
 * LuceneRebuildIndexJob:
 * 
 * This job is to update the lucene index.
 * 
 * 
 * @see AdminPController
 * 
 * @author rsoika
 * 
 */
@DeclareRoles({"org.imixs.ACCESSLEVEL.MANAGERACCESS"})
@Stateless
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
@LocalBean
public class AdminPService {

  public static final String JOB_RENAME_USER = "RENAME_USER";
  public static final String JOB_REBUILD_INDEX = "JOB_REBUILD_INDEX";
  public static final String JOB_UPGRADE = "UPGRADE";
  public static final String JOB_MIGRATION = "MIGRATION";
  private static final int DEFAULT_INTERVAL = 1;

  @Resource
  SessionContext ctx;

  @Resource
  javax.ejb.TimerService timerService;

  @Inject
  DocumentService documentService;

  @Inject
  JobHandlerUpgradeWorkitems jobHandlerUpgradeWorkitems;

  @Inject
  JobHandlerRenameUser jobHandlerRenameUser;

  @Inject
  JobHandlerRebuildIndex jobHandlerRebuildIndex;

  @Inject
  @Any
  private Instance<JobHandler> jobHandlers;

  @Inject
  @Any
  private Instance<Plugin> plugins;

  private static Logger logger = Logger.getLogger(AdminPService.class.getName());

  /**
   * This Method starts a new TimerService for a given job.
   * 
   * The method loads configuration from a ItemCollection (timerdescription) with the following
   * informations:
   * 
   * datstart - Date Object
   * 
   * datstop - Date Object
   * 
   * numInterval - Integer Object (interval in seconds)
   * 
   * id - String - unique identifier for the schedule Service.
   * 
   * The param 'id' should contain a unique identifier (e.g. the EJB Name) as only one scheduled
   * Workflow should run inside a WorkflowInstance. If a timer with the id is already running the
   * method stops this timer object first and reschedules the timer.
   * 
   * The method throws an exception if the timerdescription contains invalid attributes or values.
   * 
   * @throws AccessDeniedException
   */
  public ItemCollection createJob(ItemCollection adminp) throws AccessDeniedException {

    // set default type
    adminp.replaceItemValue("type", "adminp");

    String jobtype = adminp.getItemValueString("job");

    // generate new UniqueID...
    adminp.replaceItemValue(WorkflowKernel.UNIQUEID, WorkflowKernel.generateUniqueID());

    // Test interval - in minutes
    int interval = adminp.getItemValueInteger("numInterval");
    if (interval <= 0) {
      interval = DEFAULT_INTERVAL;
      adminp.replaceItemValue("numInterval", Long.valueOf(interval));
    }

    // startdatum und enddatum manuell festlegen
    Calendar cal = Calendar.getInstance();
    Date terminationDate = cal.getTime();
    cal.add(Calendar.HOUR, 24);
    adminp.replaceItemValue("datTerminate", terminationDate);

    // save job document
    adminp = documentService.save(adminp);

    // start timer...
    Timer timer = timerService.createTimer(terminationDate, (60 * interval * 1000),
        adminp.getItemValueString(WorkflowKernel.UNIQUEID));

    logger.info("Job " + jobtype + " (" + timer.getInfo().toString() + ") started... ");
    return adminp;
  }

  /**
   * Stops a running job and deletes the job configuration.
   * 
   * @param id
   * @return
   * @throws AccessDeniedException
   */
  public void deleteJob(String id) throws AccessDeniedException {
    ItemCollection adminp = cancelTimer(id);
    if (adminp != null) {
      documentService.remove(adminp);
    }
  }

  /**
   * This method processes the timeout event. The method loads the corresponding job description
   * (adminp entity) and delegates the processing to the corresponding JobHandler.
   * 
   * @param timer
   */
  @Timeout
  public void scheduleTimer(javax.ejb.Timer timer) {
    String sTimerID = null;

    // Startzeit ermitteln
    long lProfiler = System.currentTimeMillis();
    sTimerID = timer.getInfo().toString();
    // load adminp configuration from database
    ItemCollection adminp = documentService.load(sTimerID);
    try {

      // verify if admin entity still exists
      if (adminp == null) {
        // configuration was removed - so stop the timer!
        logger.info("Process " + sTimerID + " was removed - timer will be canceled");
        // stop timer
        timer.cancel();
        // go out!
        return;
      }

      String job = adminp.getItemValueString("job");
      logger.info("Job " + job + " (" + adminp.getUniqueID() + ")  processing...");

      // boolean jobfound = false;

      // find the corresponding job handler....
      JobHandler jobHandler = null;
      if (job.equals(JOB_RENAME_USER)) {
        jobHandler = jobHandlerRenameUser;
      }

      if (job.equals(JOB_UPGRADE)) {
        jobHandler = jobHandlerUpgradeWorkitems;
      }

      if (job.equals(JOB_REBUILD_INDEX) || job.equals("REBUILD_LUCENE_INDEX")) {
        jobHandler = jobHandlerRebuildIndex;
      }

      if (jobHandler == null) {
        // try to find the jobHandler by CDI .....
        jobHandler = findJobHandlerByName(job);
      }

      // run the job handler...
      if (jobHandler != null) {
        // update status
        adminp.replaceItemValue("$workflowStatus", "PROCESSING");
        adminp = documentService.save(adminp);

        adminp = jobHandler.run(adminp);
        if (adminp.getItemValueBoolean("iscompleted")) {
          timer.cancel();
          adminp.replaceItemValue("$workflowStatus", "COMPLETED");
          logger.info("Job " + job + " (" + adminp.getUniqueID() + ") completed - timer stopped");
        } else {
          adminp.replaceItemValue("$workflowStatus", "WAITING");
        }

      } else {
        logger.warning("Unable to start AdminP Job. JobHandler class '" + job + "' not defined!");
        timer.cancel();
        adminp.replaceItemValue("$workflowStatus", "FAILED");
        logger.info("Job " + adminp.getUniqueID() + " - timer stopped");
      }

    } catch (AdminPException e) {
      e.printStackTrace();
      // stop timer!
      timer.cancel();
      logger.severe("AdminP job '" + sTimerID + "' failed - " + e.getMessage());
      if (adminp != null) {
        adminp.replaceItemValue("$workflowStatus", "FAILED");
        adminp.replaceItemValue("errormessage", e.getMessage());
      }
    } finally {
      // try to update the amdinp document...
      try {
        if (adminp != null) {
          adminp = documentService.save(adminp);
        } else {
          logger.warning("Unable to update adminp job status - adminp document is null!");
        }
      } catch (Exception e2) {
        logger.warning("Unable to update adminp job status: " + e2.getMessage());
        e2.printStackTrace();
      }
    }

    logger.fine("...timer call finished successfull after "
        + ((System.currentTimeMillis()) - lProfiler) + " ms");

  }

  /**
   * This method returns a n injected JobHandler by name or null if no JobHandler with the requested
   * class name is injected.
   * 
   * @param jobHandlerClassName
   * @return jobHandler class or null if not found
   */
  private JobHandler findJobHandlerByName(String jobHandlerClassName) {
    if (jobHandlerClassName == null || jobHandlerClassName.isEmpty())
      return null;

    if (jobHandlers == null || !jobHandlers.iterator().hasNext()) {
      logger.finest("......no CDI jobHandlers injected");
      return null;
    }
    // iterate over all injected JobHandlers....
    for (JobHandler jobHandler : this.jobHandlers) {
      if (jobHandler.getClass().getName().equals(jobHandlerClassName)) {
        logger.finest("......CDI JobHandler '" + jobHandlerClassName + "' successful injected");
        return jobHandler;
      }
    }

    return null;
  }

  /**
   * This method cancels a timer by ID. If a timer configuration exits, the method returns the
   * document entity.
   * 
   * @param id
   * @return
   */
  private ItemCollection cancelTimer(String id) {

    logger.finest("......cancelTimer - id:" + id + " ....");
    ItemCollection adminp = documentService.load(id);
    if (adminp == null) {
      logger.warning("failed to load timer data ID:" + id + " ");
    }
    // try to cancel an existing timer
    Timer timer = this.findTimer(id);
    if (timer != null) {
      timer.cancel();
      logger.info("cancelTimer - id:" + id + " successful.");
    } else {
      logger.info("cancelTimer - id:" + id + " failed - timer does no longer exist.");
    }
    if (adminp != null) {
      adminp.replaceItemValue("txtTimerStatus", "Stopped");
    }
    return adminp;
  }

  /**
   * This method returns a timer for a corresponding id if such a timer object exists.
   * 
   * @param id
   * @return Timer
   * @throws Exception
   */
  private Timer findTimer(String id) {
    if (id == null || id.isEmpty())
      return null;

    for (Object obj : timerService.getTimers()) {
      Timer timer = (javax.ejb.Timer) obj;
      if (timer.getInfo() instanceof String) {
        String timerid = timer.getInfo().toString();
        if (id.equals(timerid)) {
          return timer;
        }
      }
    }
    logger.warning("findTimer - id:" + id + " does no longer exist.");
    return null;
  }
}
