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

package org.imixs.workflow.engine.plugins;

import java.util.Calendar;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.EventLogService;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.util.XMLParser;

import jakarta.inject.Inject;

/**
 * The Imixs EventLog plugin can be used to create a EventLog entry during
 * processing an event. The plugin can be configured by the activity
 * result :
 * <p>
 * Example:
 * <p>
 * 
 * <pre>
  {@code
   <eventlog name="snapshot.export">
	<ref>$uniqueid</ref>
	<timeout>60000</timeout>
        <document>
             <amount>500.00</amount>
             <department>Finance</department>
        </document>
 </eventlog>
    }
 * </pre>
 * <p>
 * 
 * An EventLog entry can be processed by internal or external services.
 * See: https://www.imixs.org/doc/engine/eventlogservice.html
 * 
 * @author rsoika
 * 
 */
public class EventLogPlugin extends AbstractPlugin {
	public static final String INVALID_FORMAT = "INVALID_FORMAT";

	private static final Logger logger = Logger.getLogger(EventLogPlugin.class.getName());

	@Inject
	EventLogService eventLogService;

	@Override
	public ItemCollection run(ItemCollection documentContext, ItemCollection event) throws PluginException {

		// parse for eventlog definition....
		ItemCollection eventLogConfig = this.getWorkflowContext().evalWorkflowResult(event, "eventlog",
				documentContext,
				true);
		if (eventLogConfig == null || eventLogConfig.getItemNames().size() == 0) {
			// no op - return
			return documentContext;
		}

		// now iterate over all taxonomy definitions....
		for (String name : eventLogConfig.getItemNames()) {
			String xmlDef = eventLogConfig.getItemValueString(name);
			ItemCollection eventLogData = XMLParser.parseItemStructure(xmlDef);

			if (eventLogData != null) {
				String ref = eventLogData.getItemValueString("ref");
				String documentXML = eventLogData.getItemValueString("document");
				long timeout = eventLogData.getItemValueLong("timeout");
				Calendar cal = null;
				// create event log entry...
				if (timeout > 0) {
					cal = Calendar.getInstance();
					cal.setTimeInMillis(System.currentTimeMillis() + timeout);
				}
				// do we have a document definition?
				if (!documentXML.isEmpty()) {
					ItemCollection docData = XMLParser.parseItemStructure(documentXML);
					// write eventLog entry...
					eventLogService.createEvent(name, ref, docData.getAllItems(), cal);
				} else {
					// write eventLog entry...
					eventLogService.createEvent(name, ref, cal);
				}
			}
		}

		return documentContext;
	}

}
