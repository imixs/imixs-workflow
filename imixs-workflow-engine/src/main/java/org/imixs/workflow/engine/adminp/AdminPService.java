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

package org.imixs.workflow.engine.adminp;

import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RunAs;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.exceptions.AccessDeniedException;

import jakarta.ejb.EJBException;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;

/**
 * The AmdinPService provides a mechanism to start long running jobs. Those jobs
 * can be used to update workitems in a scheduled batch process. This is called
 * a AdminP-Process. The result of a adminp process is documented into an log
 * entity from type='adminp'. The job description is stored in the field
 * '$WorkflowSummary'. The current startpos and maxcount are stored in the
 * configuration entity in the properties 'numStart' 'numMaxCount'
 * 
 * The service provides methods to create and start different types of jobs. The
 * job type is stored in the field 'job':
 * 
 * RenameUserJob:
 * 
 * This job is to replace entries in the fields $WriteAccess, $ReadAccess and
 * owner. An update request is stored in a adminp entity containing alll
 * necessary informations. The service starts a timer instances for each update
 * process
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
@DeclareRoles({ "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
@Stateless
public class AdminPService {

    public static final String JOB_RENAME_USER = "RENAME_USER";
    public static final String JOB_REBUILD_INDEX = "JOB_REBUILD_INDEX";
    public static final String JOB_UPGRADE = "UPGRADE";
    public static final String JOB_MIGRATION = "MIGRATION";
    public static final int DEFAULT_INTERVAL = 60;

    @Resource
    SessionContext ctx;

    @Resource
    jakarta.ejb.TimerService timerService;

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

    private static final Logger logger = Logger.getLogger(AdminPService.class.getName());

    /**
     * This Method starts a new TimerService for a given job.
     * 
     * The method loads configuration from a ItemCollection (timerdescription) with
     * the following informations:
     * 
     * datstart - Date Object
     * 
     * datstop - Date Object
     * 
     * numInterval - Integer Object (interval in seconds)
     * 
     * id - String - unique identifier for the schedule Service.
     * 
     * The param 'id' should contain a unique identifier (e.g. the EJB Name) as only
     * one scheduled Workflow should run inside a WorkflowInstance. If a timer with
     * the id is already running the method stops this timer object first and
     * reschedules the timer.
     * 
     * The method throws an exception if the timerdescription contains invalid
     * attributes or values.
     * 
     * @throws AccessDeniedException
     */
    public ItemCollection createJob(ItemCollection adminp) throws AccessDeniedException {

        // set default type
        adminp.replaceItemValue("type", "adminp");
        adminp.replaceItemValue("$snapshot.history", 1);

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
        Timer timer = timerService.createTimer(terminationDate, (interval * 1000),
                adminp.getItemValueString(WorkflowKernel.UNIQUEID));

        logger.log(Level.INFO, "Job {0} ({1}) started... ", new Object[]{jobtype, timer.getInfo().toString()});
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
     * Method to restart an existing job.
     * 
     * The method first tries to cancel a running timer and than creates a new one
     * @param id
     */
    public void restartJobByID(String id) {
        logger.info("...Restart AdminP Job: "+id);
        // try to cancel...
        ItemCollection job = cancelTimer(id);
        if (job!=null) {
            String jobtype = job.getItemValueString("job");
            int interval = job.getItemValueInteger("numInterval");
            Calendar cal = Calendar.getInstance();
            Date terminationDate = cal.getTime();
            // restart timer...
            Timer timer = timerService.createTimer(terminationDate, (interval * 1000),
            job.getUniqueID());
            logger.info("Job " + jobtype + " (" + timer.getInfo().toString() + ") restarted... ");
        } else {
            logger.info("Restart failed - unable to load job with id: "+id);
        }
    }

    /**
     * This method processes the timeout event. The method loads the corresponding
     * job description (adminp entity) and delegates the processing to the
     * corresponding JobHandler.
     * 
     * @param timer
     */
    @Timeout
    public void scheduleTimer(jakarta.ejb.Timer timer) {
        String sTimerID = null;
        boolean debug = logger.isLoggable(Level.FINE);

        // Startzeit ermitteln
        long lProfiler = System.currentTimeMillis();
        sTimerID = timer.getInfo().toString();
        // load adminp configuration from database
        ItemCollection adminp = documentService.load(sTimerID);
        try {

            // verify if admin entity still exists
            if (adminp == null) {
                // configuration was removed - so stop the timer!
                logger.log(Level.INFO, "Process {0} was removed - timer will be canceled", sTimerID);
                // stop timer
                timer.cancel();
                // go out!
                return;
            }

            String job = adminp.getItemValueString("job");
            logger.log(Level.INFO, "Job {0} ({1})  processing...", new Object[]{job, adminp.getUniqueID()});

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
                    logger.log(Level.INFO, "Job {0} ({1}) completed - timer stopped", new Object[]{job, adminp.getUniqueID()});
                } else {
                    adminp.replaceItemValue("$workflowStatus", "WAITING");
                }

            } else {
                logger.log(Level.WARNING, "Unable to start AdminP Job. JobHandler class ''{0}'' not defined!", job);
                timer.cancel();
                adminp.replaceItemValue("$workflowStatus", "FAILED");
                logger.log(Level.INFO, "Job {0} - timer stopped", adminp.getUniqueID());
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
            } catch (AccessDeniedException | EJBException e2) {
                logger.log(Level.WARNING, "Unable to update adminp job status - reason: {0}", e2.getMessage());
                if (debug) {
                    e2.printStackTrace();
                }
            }
        }

        logger.log(Level.FINE, "...timer call finished successfull after {0} ms", (System.currentTimeMillis()) - lProfiler);

    }

    /**
     * This method returns a n injected JobHandler by name or null if no JobHandler
     * with the requested class name is injected.
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
                logger.log(Level.FINEST, "......CDI JobHandler ''{0}'' successful injected", jobHandlerClassName);
                return jobHandler;
            }
        }

        return null;
    }

    /**
     * This method cancels a timer by ID. If a timer configuration exits, the method
     * returns the document entity.
     * 
     * @param id
     * @return
     */
    private ItemCollection cancelTimer(String id) {

        logger.log(Level.FINEST, "......cancelTimer - id:{0} ....", id);
        ItemCollection adminp = documentService.load(id);
        if (adminp == null) {
            logger.log(Level.WARNING, "failed to load timer data ID:{0} ", id);
        }
        // try to cancel an existing timer
        Timer timer = this.findTimer(id);
        if (timer != null) {
            timer.cancel();
            logger.log(Level.INFO, "cancelTimer - id:{0} successful.", id);
        } else {
            logger.log(Level.INFO, "cancelTimer - id:{0} failed - timer does no longer exist.", id);
        }
        if (adminp != null) {
            adminp.replaceItemValue("txtTimerStatus", "Stopped");
        }
        return adminp;
    }

    /**
     * This method returns a timer for a corresponding id if such a timer object
     * exists.
     * 
     * @param id
     * @return Timer
     * @throws Exception
     */
    private Timer findTimer(String id) {
        if (id == null || id.isEmpty())
            return null;

        for (Object obj : timerService.getTimers()) {
            Timer timer = (jakarta.ejb.Timer) obj;
            if (timer.getInfo() instanceof String) {
                String timerid = timer.getInfo().toString();
                if (id.equals(timerid)) {
                    return timer;
                }
            }
        }
        logger.log(Level.WARNING, "findTimer - id:{0} does no longer exist.", id);
        return null;
    }
}
