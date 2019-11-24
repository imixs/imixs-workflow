/*******************************************************************************
 *  Imixs IX Workflow Technology
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
package org.imixs.workflow.engine;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;

/**
 * The Imixs HealthCheckService implements the Microservice HealthCheck
 * interface.
 * <p>
 * The service returns the count of workflow models
 * <p>
 * Example:
 * <code>{"data":{"model.count":1},"name":"imixs-workflow","state":"UP"}</code>
 * <p>
 * This check indicates the overall status of the workflow engine. If models are
 * available also database access and security works.
 * 
 * @author rsoika
 * @version 1.0
 */
@Health
@ApplicationScoped
public class HealthCheckService implements HealthCheck {

	private String workflowVersion = null;
	private static Logger logger = Logger.getLogger(HealthCheckService.class.getName());

	@Inject
	private SetupService setupService;

	/**
	 * This is the implementation for the health check call back method.
	 * <p>
	 * The method returns the status 'UP' together with the count of workflow models
	 * <p>
	 * Example:
	 * <code>{"data":{"model.count":1},"name":"imixs-workflow","state":"UP"}</code>
	 * <p>
	 * This check indicates the overall status of the workflow engine. If models are
	 * available also database access and security works.
	 * 
	 */
	@Override
	public HealthCheckResponse call() {
		HealthCheckResponseBuilder builder = null;
		long l=System.currentTimeMillis();
		int modelCount = 0;
		int groupCount = 0;
		boolean failure = false;
		try {
			modelCount = setupService.getModelVersionCount();
			groupCount = setupService.getModelGroupCount();
		} catch (Exception e) {
			// failed!
			modelCount = 0;
			failure = true;
		}

		if (!failure) {
			builder = HealthCheckResponse.named("imixs-workflow").withData("engine.version", getWorkflowVersion())
					.withData("model.versions", modelCount)
					.withData("model.groups", groupCount)
					.up();
		} else {
			builder = HealthCheckResponse.named("imixs-workflow").down();
		}

		logger.info(" berechnet in " + (System.currentTimeMillis() -l) + "ms");
		
		return builder.build();
	}

	/**
	 * This method extracts the workflow version form the maven pom.properties
	 * 
	 * META-INF/maven/${groupId}/${artifactId}/pom.properties
	 * 
	 */
	private String getWorkflowVersion() {
		if (workflowVersion == null) {
			logger.info("...loading pom.properies");
			try {
				InputStream resourceAsStream = this.getClass()
						.getResourceAsStream("/META-INF/maven/org.imixs.workflow/imixs-workflow-engine/pom.properties");
				if (resourceAsStream != null) {
					Properties prop = new Properties();
					prop.load(resourceAsStream);
					workflowVersion = prop.getProperty("version");
				}
			} catch (IOException e1) {
				logger.warning("failed to load pom.properties");
			}
		}
		// if not found -> 'unknown'
		if (workflowVersion == null || workflowVersion.isEmpty()) {
			workflowVersion = "unknown";
		}

		return workflowVersion;
	}

}