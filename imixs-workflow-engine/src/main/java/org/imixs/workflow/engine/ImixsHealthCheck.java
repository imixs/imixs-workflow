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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;

/**
 * The ImixsHealthCheck implements the Microservice HealthCheck interface.
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
public class ImixsHealthCheck implements HealthCheck {

	@Inject
	private SetupService setupService;

	/**
	 * This is the implementation for the health check call back method.
	 * <p>
	 * The method returns the status 'UP' in case the count of workflow models > 0
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
		int modelCount=0;
		try {
			 modelCount = setupService.getModelCount();
		} catch (Exception e) {
			// failed!
			modelCount=0;
		}
		HealthCheckResponseBuilder builder = HealthCheckResponse.named("imixs-workflow").withData("model.count",
				modelCount);

		if (modelCount > 1) {
			builder.up();
		} else {
			builder.down();

		}

		return builder.build();
	}

}