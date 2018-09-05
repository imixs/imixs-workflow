package org.imixs.workflow.engine.scheduler;

import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

/**
 * This servlet checks als Imixs-Schedulers on startup. The servlet is configured with
 * the option load-on-startup=1 which means that the servlet init() method is
 * triggered after deployment.
 * 
 * The scheduler will only be started automatically if the item "_scheduler_enabled" is set to "true".
 * 
 * @author rsoika
 * 
 */
@WebServlet(loadOnStartup = 1)
public class SchedulerStartupServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static Logger logger = Logger.getLogger(SchedulerStartupServlet.class.getName());

	@EJB
	private SchedulerService schedulerService;

	@Override
	public void init() throws ServletException {

		super.init();
		logger.info("...starting Imixs-Schedulers...");		
		schedulerService.startAllSchedulers();
	}

}