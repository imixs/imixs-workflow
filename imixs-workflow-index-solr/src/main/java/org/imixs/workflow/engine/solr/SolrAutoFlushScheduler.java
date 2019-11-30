package org.imixs.workflow.engine.solr;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RunAs;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * The SolrAutoFlushScheduler starts a ManagedScheduledExecutorService to flush
 * the index events on a scheduled basis by calling the SolrUpdateService method
 * <code>updateIndex()</code>.
 * <p>
 * The ManagedScheduledExecutorService can be configured with the following
 * properties:
 * <p>
 * <ul>
 * <li>solr.flush.interval - flush interval in milliseconds (default 2sec)</li>
 * <li>solr.flush.disabled - if true the scheduler is disabled (default
 * 'false')</li>
 * <li>solr.autoflush.initialdelay - time in milliseconds to delay the
 * start</li>
 * </ul>
 * <p>
 * 
 * @see SolrUpdateService
 * @version 1.0
 * @author rsoika
 *
 */
@DeclareRoles({ "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
@Startup
@Singleton
@LocalBean
public class SolrAutoFlushScheduler {

	public static final String SOLR_AUTOFLUSH_DISABLED = "solr.autoflush.disabled";
	public static final String SOLR_AUTOFLUSH_INTERVAL = "solr.autoflush.interval";
	public static final String SOLR_AUTOFLUSH_INITIALDELAY = "solr.autoflush.initialdelay";

	@Inject
	@ConfigProperty(name = SOLR_AUTOFLUSH_DISABLED, defaultValue = "false")
	boolean flushDisabled;

	// timeout interval in ms
	@Inject
	@ConfigProperty(name = SOLR_AUTOFLUSH_INTERVAL, defaultValue = "2000")
	long interval;

	// initial delay in ms
	@Inject
	@ConfigProperty(name = SOLR_AUTOFLUSH_INITIALDELAY, defaultValue = "0")
	long initialDelay;

	private static Logger logger = Logger.getLogger(SolrAutoFlushScheduler.class.getName());

	@Resource
	ManagedScheduledExecutorService scheduler;

	@Inject
	SolrUpdateService solrUpdateService;

	/**
	 * This method start the ManagedScheduledExecutorService to flush the index
	 * event log on a schedule base.
	 */
	@PostConstruct
	public void init() {
		if (!flushDisabled) {
			logger.info("Starting Solr auto flush - initalDelay=" + initialDelay + "  inverval=" + interval + " ....");
			this.scheduler.scheduleAtFixedRate(this::run, initialDelay, interval, TimeUnit.MILLISECONDS);
		}
	}

	/**
	 * The method delecates the event processing to the solrUpdateService
	 * 
	 */
	public void run() {
		solrUpdateService.updateIndex();
	}

}
