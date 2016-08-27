/*******************************************************************************
 *  Imixs Workflow Technology
 *  Copyright (C) 2001, 2008 Imixs Software Solutions GmbH,  
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
 *******************************************************************************/
package org.imixs.workflow.engine.adminp;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.engine.lucene.LuceneUpdateService;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.imixs.workflow.jee.ejb.EntityService;

/**
 * The AmdinPService provides a mechanism to start long running jobs. Those jobs
 * can be used to update workitems in a scheduled batch process. This is called
 * a AdminP-Process. The result of a adminp process is documented into an log
 * entity from type='adminp'. The job description is stored in the field
 * 'txtWorkflowSummary'. The current startpos and maxcount are stored in the
 * configuration entity in the properties 'numStart' 'numMaxCount'
 * 
 * The service provides methods to create and start different types of jobs. The
 * job type is stored in the field 'job':
 * 
 * RenameUserJob:
 * 
 * This job is to replace entries in the fields $WriteAccess, $ReadAccess and
 * namOwner. An update request is stored in a adminp entity containing alll
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
@Stateless
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
@LocalBean
public class AdminPService {

	public static final String JOB_RENAME_USER = "RENAME_USER";
	public static final String JOB_REBUILD_LUCENE_INDEX = "REBUILD_LUCENE_INDEX";

	@Resource
	SessionContext ctx;

	@Resource
	javax.ejb.TimerService timerService;

	
	@EJB
	DocumentService documentService;

	
	@EJB
	JobHandlerRebuildIndex jobHandlerRebuildIndex;
	
	@EJB 
	JobHandlerRenameUser jobHandlerRenameUser;

	//private String lastUnqiueID = null;
	//private static int MAX_COUNT = 300;
	private static Logger logger = Logger.getLogger(AdminPService.class.getName());

	/**
	 * This Method starts a new TimerService for a given job.
	 * 
	 * The method loads configuration from a ItemCollection (timerdescription)
	 * with the following informations:
	 * 
	 * datstart - Date Object
	 * 
	 * datstop - Date Object
	 * 
	 * numInterval - Integer Object (interval in seconds)
	 * 
	 * id - String - unique identifier for the schedule Service.
	 * 
	 * The param 'id' should contain a unique identifier (e.g. the EJB Name) as
	 * only one scheduled Workflow should run inside a WorkflowInstance. If a
	 * timer with the id is already running the method stops this timer object
	 * first and reschedules the timer.
	 * 
	 * The method throws an exception if the timerdescription contains invalid
	 * attributes or values.
	 * 
	 * @throws AccessDeniedException
	 */
	public ItemCollection createJob(ItemCollection adminp) throws AccessDeniedException {
		
		
		String jobtype=adminp.getItemValueString("job");
		if (!jobtype.equals(JOB_RENAME_USER) && !jobtype.equals(JOB_REBUILD_LUCENE_INDEX)) {
			throw new InvalidAccessException(ProcessingErrorException.INVALID_WORKITEM, "AdminPService: error - invalid job type");
		}
				
				
		// generate new UniqueID...
		adminp.replaceItemValue(WorkflowKernel.UNIQUEID,WorkflowKernel.generateUniqueID());
	
		// startdatum und enddatum manuell festlegen
		Calendar cal = Calendar.getInstance();
		Date startDate = cal.getTime();
		cal.add(Calendar.YEAR, 10);
		adminp.replaceItemValue("datstart", startDate);
	
		long interval = 60 * 1000;
	
		adminp.replaceItemValue("numInterval", new Long(interval));
	
		adminp.replaceItemValue("txtTimerStatus", "Running");
	
		// save job document
		adminp = documentService.save(adminp);
	
		// start timer...
		Timer timer = timerService.createTimer(startDate, interval, adminp.getItemValueString(WorkflowKernel.UNIQUEID));
	
		logger.info("Job started - ID=" + timer.getInfo().toString() );
		return adminp;
	}



	/**
	 * Stops a running job and deletes the job configuration.
	 * 
	 * @param id
	 * @return
	 * @throws AccessDeniedException
	 */
	public ItemCollection deleteJob(String id) throws AccessDeniedException {
		ItemCollection adminp = stopTimer(id);
	
		ItemCollection entity = documentService.load(id);
		if (entity != null) {
			documentService.remove(entity);
		}
		
		return adminp;
	}



	
	

	/**
	 * This method processes the timeout event. The method loads the
	 * corresponding job description (adminp entity) and delegates the
	 * processing to the corresponding JobHandler.
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

			logger.info("Processing : " + sTimerID);

			String job = adminp.getItemValueString("job");
			if (job.equals(JOB_RENAME_USER)) {
				if (jobHandlerRenameUser.run(adminp)) {
					timer.cancel();
					logger.info("Job "+ adminp.getUniqueID() + " completed - timer stopped");
				}
			}
			if (job.equals(JOB_REBUILD_LUCENE_INDEX)) {
				if (jobHandlerRebuildIndex.run(adminp)) {
					timer.cancel();
					logger.info("Job "+ adminp.getUniqueID() + " completed - timer stopped");
				}
			}

			

		} catch (Exception e) {
			e.printStackTrace();
			// stop timer!
			timer.cancel();
			logger.severe("Timeout sevice stopped: " + sTimerID);
			
			try {
				adminp.replaceItemValue("txtworkflowStatus", "Error");
				adminp.replaceItemValue("errormessage", e.toString());
				// adminp = entityService.save(adminp);
				adminp = documentService.save(adminp);

			} catch (Exception e2) {
				e2.printStackTrace();

			}

		}

		logger.fine("timer call finished successfull after "
				+ ((System.currentTimeMillis()) - lProfiler) + " ms");

	}

	
	
	
	


	private ItemCollection stopTimer(String id) {

		logger.info("[AdminPService] Stopping timer ID=" + id + "....");
		ItemCollection adminp = documentService.load(id);
		if (adminp == null) {
			logger.warning("[AdminPService] anable to load timer data ID=" + id + " ");
			return null;
		}

		// try to cancel an existing timer for this workflowinstance
		Timer timer = this.findTimer(id);
		if (timer != null) {
			timer.cancel();

			adminp.replaceItemValue("txtTimerStatus", "Stopped");

			// adminp = entityService.save(adminp);
			adminp = documentService.save(adminp);

			logger.info("[AdminPService] Timer ID=" + id + " STOPPED.");
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
			Timer timer = (javax.ejb.Timer) obj;
			if (timer.getInfo() instanceof String) {
				String timerid = timer.getInfo().toString();
				if (id.equals(timerid)) {
					return timer;
				}
			}
		}
		return null;
	}
}
