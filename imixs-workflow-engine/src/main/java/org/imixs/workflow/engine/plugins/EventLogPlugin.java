/*  
 *  Imixs-Workflow 
 *  
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
 *      Imixs Software Solutions GmbH - Project Management
 *      Ralph Soika - Software Developer
 */

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

	private static Logger logger = Logger.getLogger(EventLogPlugin.class.getName());

	@Inject
	EventLogService eventLogService;

	@Override
	public ItemCollection run(ItemCollection documentContext, ItemCollection event) throws PluginException {

		// parse for eventlog definition....
		ItemCollection eventLogConfig = this.getWorkflowService().evalWorkflowResult(event, "eventlog", documentContext,
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
