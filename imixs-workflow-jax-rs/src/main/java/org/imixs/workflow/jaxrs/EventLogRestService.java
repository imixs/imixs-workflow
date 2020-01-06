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

package org.imixs.workflow.jaxrs;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.EventLogService;
import org.imixs.workflow.engine.jpa.EventLog;
import org.imixs.workflow.xml.XMLDataCollection;
import org.imixs.workflow.xml.XMLDataCollectionAdapter;

/**
 * The EventLogRestService supports methods to access the event log entries by
 * different kind of request URIs
 * 
 * @author rsoika
 * 
 */
@Path("/eventlog")
@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.TEXT_XML })
@Stateless
public class EventLogRestService {

    @Inject
    private EventLogService eventLogService;

    @Context
    private HttpServletRequest servletRequest;

    private static Logger logger = Logger.getLogger(EventLogRestService.class.getName());

    /**
     * Returns a set of eventLog entries for a given topic. Multiple topics can be
     * separated by a swung dash (~).
     * 
     * @param topic    - topic to search event log entries.
     * @param maxCount - max count of returned eventLogEntries (default 99)
     * @return - xmlDataCollection containing all matching eventLog entries
     */
    @GET
    @Path("/{topic}")
    public XMLDataCollection getEventLogEntriesByTopic(@PathParam("topic") String topic,
            @DefaultValue("99") @QueryParam("maxCount") int maxCount) {

        logger.finest("......get eventLogEntry by topic: " + topic);
        // we split the topic by swung dash if multiple topics are provided
        String[] topicList = topic.split("~");
        List<EventLog> eventLogEntries = eventLogService.findEventsByTopic(maxCount, topicList);

        List<ItemCollection> result = new ArrayList<ItemCollection>();
        for (EventLog eventLog : eventLogEntries) {
            // Build a ItemCollection for each EventLog

            ItemCollection itemColEvent = new ItemCollection();
            itemColEvent.setItemValue("id", eventLog.getId());
            itemColEvent.setItemValue("ref", eventLog.getRef());
            itemColEvent.setItemValue("created", eventLog.getCreated());
            itemColEvent.setItemValue("topic", eventLog.getTopic());
            itemColEvent.setItemValue("data", eventLog.getData());

            result.add(itemColEvent);
        }
        return XMLDataCollectionAdapter.getDataCollection(result);
    }

    /**
     * Deletes a eventLog entry by its $uniqueID
     * 
     * @param name of report or uniqueid
     */
    @DELETE
    @Path("/{id}")
    public void deleteEventLogEntry(@PathParam("id") String id) {
        // remove eventLogEntry....
        eventLogService.removeEvent(id);
    }

}
