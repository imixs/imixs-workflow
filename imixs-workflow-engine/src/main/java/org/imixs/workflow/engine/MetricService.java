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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.annotation.RegistryScope;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.AccessDeniedException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObserverException;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * The Imixs MetricSerivce is a monitoring resource for Imixs-Workflow in the
 * prometheus format. The MetricService is based on Microprofile 2.2 and
 * MP-Metric-API 2.2
 * <p>
 * A metric is created each time when a Imixs ProcessingEvent or Imixs
 * DocumentEvent is fired. The service exports metrics in prometheus text
 * format.
 * <p>
 * The service provides counter metrics for document access and processed
 * workitems. A counter will always increase. To extract the values in
 * prometheus use the rate function - Example:
 * <p>
 * <code>rate(http_requests_total[5m])</code>
 * <p>
 * The service expects MP Metrics v2.0. A warning is logged if corresponding
 * version is missing.
 * <p>
 * To enable the metric service the imixs.property ... must be set to true
 * </p>
 * 
 * @See https://www.robustperception.io/how-does-a-prometheus-counter-work
 * @author rsoika
 * @version 1.0
 */
@ApplicationScoped
public class MetricService {

	public static final String METRIC_DOCUMENTS = "documents";
	public static final String METRIC_WORKITEMS = "workitems";
	public static final String METRIC_TRANSACTIONS = "transactions";

	@Inject
	@ConfigProperty(name = "metrics.enabled", defaultValue = "false")
	private boolean metricsEnabled;

	@Inject
	@ConfigProperty(name = "metrics.anonymised", defaultValue = "true")
	private boolean metricsAnonymised;

	@Inject
	@RegistryScope(scope = MetricRegistry.APPLICATION_SCOPE)
	MetricRegistry metricRegistry;

	boolean mpMetricNoSupport = false;

	private static final Logger logger = Logger.getLogger(MetricService.class.getName());

	/**
	 * ProcessingEvent listener to generate a metric.
	 * 
	 * @param processingEvent
	 * @throws AccessDeniedException
	 */
	public void onProcessingEvent(@Observes ProcessingEvent processingEvent) throws AccessDeniedException {

		if (!metricsEnabled) {
			return;
		}
		if (processingEvent == null) {
			return;
		}
		if (mpMetricNoSupport) {
			// missing MP Metric support!
			return;
		}

		// NOTE: Issue #514 - just uncomment this code!
		try {
			Counter counter = buildWorkitemMetric(processingEvent);
			counter.inc();
		} catch (IncompatibleClassChangeError | ObserverException oe) {
			mpMetricNoSupport = true;
			logger.warning("...Microprofile Metrics not supported!");
		}
	}

	/**
	 * DocumentEvent listener to generate a metric.
	 * 
	 * @param documentEvent
	 * @throws AccessDeniedException
	 */
	public void onDocumentEvent(@Observes DocumentEvent documentEvent) throws AccessDeniedException {

		if (!metricsEnabled) {
			return;
		}
		if (documentEvent == null) {
			return;
		}
		if (mpMetricNoSupport) {
			// missing MP Metric support!
			return;
		}

		// NOTE: Issue #514 - just uncomment this code!
		try {
			Counter counter = buildDocumentMetric(documentEvent);
			counter.inc();

		} catch (IncompatibleClassChangeError | ObserverException oe) {
			mpMetricNoSupport = true;
			logger.warning("...Microprofile Metrics not supported!");
			oe.printStackTrace();
		}
	}

	/**
	 * This method builds a Microprofile Metric for a Counter. The metric contains
	 * the tag 'method'.
	 * 
	 * @return Counter metric
	 */

	// NOTE: Issue #514 - just uncomment this code!

	private Counter buildDocumentMetric(DocumentEvent event) {

		// Constructs a Metadata object from a map with the following keys:
		// - name - The name of the metric
		// - displayName - The display (friendly) name of the metric
		// - description - The description of the metric
		// - type - The type of the metric
		// - tags - The tags of the metric - cannot be null
		// - reusable - If true, this metric name is permitted to be used at multiple

		Metadata metadata = Metadata.builder().withName(METRIC_DOCUMENTS)
				.withDescription("Imixs-Workflow count documents").build();

		String method = null;
		// build tags...
		if (DocumentEvent.ON_DOCUMENT_SAVE == event.getEventType()) {
			method = "save";
		}

		if (DocumentEvent.ON_DOCUMENT_LOAD == event.getEventType()) {
			method = "load";
		}

		if (DocumentEvent.ON_DOCUMENT_DELETE == event.getEventType()) {
			method = "delete";
		}

		Tag[] tags = { new Tag("method", method) };

		Counter counter = metricRegistry.counter(metadata, tags);

		return counter;
	}

	/**
	 * This method builds a Microprofile Metric for a Counter. The metric contains
	 * the tags 'task', 'event', 'type', 'workflowgroup', 'worklowstatus',
	 * 'modelversion'
	 * 
	 * @return Counter metric
	 */
	private Counter buildWorkitemMetric(ProcessingEvent event) {

		// Constructs a Metadata object from a map with the following keys:
		// - name - The name of the metric
		// - displayName - The display (friendly) name of the metric
		// - description - The description of the metric
		// - type - The type of the metric
		// - tags - The tags of the metric - cannot be null
		// - reusable - If true, this metric name is permitted to be used at multiple

		// BEFORE_PROCESS
		if (event.getEventType() == ProcessingEvent.BEFORE_PROCESS) {
			// only transaction count - independent form the result
			Metadata metadata = Metadata.builder().withName(METRIC_TRANSACTIONS)
					.withDescription("Imixs-Workflow transactions").build();
			Counter counter = metricRegistry.counter(metadata);
			return counter;

		} else {
			// AFTER_PROCESS
			// build workflow tags...
			List<Tag> tags = new ArrayList<Tag>();
			tags.add(new Tag("type", event.getDocument().getType()));
			tags.add(new Tag("modelversion", event.getDocument().getModelVersion()));
			tags.add(new Tag("task", event.getDocument().getTaskID() + ""));
			tags.add(new Tag("workflowgroup", event.getDocument().getItemValueString(WorkflowKernel.WORKFLOWGROUP)));
			tags.add(new Tag("workflowstatus", event.getDocument().getItemValueString(WorkflowKernel.WORKFLOWSTATUS)));

			// metricsAnonymised = false?
			if (!metricsAnonymised) {
				// add the user id....
				String user = event.getDocument().getItemValueString(WorkflowKernel.EDITOR);
				tags.add(new Tag("user", user));
			}
			tags.add(new Tag("event", event.getDocument().getItemValueInteger("$lastevent") + ""));
			Metadata metadata = Metadata.builder().withName(METRIC_WORKITEMS)
					.withDescription("Imixs-Workflow count processed workitems").build();
			Tag[] tagArr = tags.toArray(new Tag[tags.size()]);
			Counter counter = metricRegistry.counter(metadata, tagArr);

			return counter;

		}

	}

}
