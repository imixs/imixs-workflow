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

package org.imixs.workflow.engine;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Liveness;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
@Liveness
// @Readiness
@ApplicationScoped
public class HealthCheckService implements HealthCheck {

    private String workflowVersion = null;
    private static final Logger logger = Logger.getLogger(HealthCheckService.class.getName());

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
        int modelCount = 0;
        boolean failure = false;
        boolean databaseFailure = false;
        boolean indexFailure = false;
        try {
            modelCount = setupService.getModelVersionCount();

        } catch (Exception e) {
            // failed!
            modelCount = 0;
            failure = true;
        }

        // check database and index....
        databaseFailure = !setupService.checkDatabase();
        indexFailure = !setupService.checkIndex();

        if (databaseFailure || indexFailure) {
            failure = true;
        }

        if (!failure) {
            builder = HealthCheckResponse.named("imixs-workflow").withData("engine.version", getWorkflowVersion())
                    .withData("model.versions", modelCount)
                    .withData("database.status", "ok").withData("index.status", "ok").up();
        } else {
            builder = HealthCheckResponse.named("imixs-workflow");
            // add details
            if (databaseFailure) {
                builder.withData("database.status", "failure");
            } else {
                builder.withData("database.status", "ok");
            }
            if (indexFailure) {
                builder.withData("index.status", "failure");
            } else {
                builder.withData("index.status", "ok");
            }
            builder.down();
        }

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
