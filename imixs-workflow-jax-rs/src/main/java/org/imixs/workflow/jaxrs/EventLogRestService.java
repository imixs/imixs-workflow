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

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.EventLogService;
import org.imixs.workflow.engine.index.SearchService;
import org.imixs.workflow.engine.jpa.EventLog;
import org.imixs.workflow.xml.XMLDataCollection;
import org.imixs.workflow.xml.XMLDataCollectionAdapter;

import jakarta.ejb.Stateless;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;

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
     * Returns all eventLog entries.
     * 
     * @param pageSize  - page size
     * @param pageIndex - page index (default = 0)
     * @param items     - optional list of items
     * @return result set.
     * 
     * @param maxCount - max count of returned eventLogEntries (default 99)
     * @return - xmlDataCollection containing all matching eventLog entries
     */
    @GET
    @Path("/")
    public XMLDataCollection getAllEventLogEntries(
            @DefaultValue("" + SearchService.DEFAULT_PAGE_SIZE) @QueryParam("pageSize") int pageSize,
            @DefaultValue("0") @QueryParam("pageIndex") int pageIndex) {

        logger.finest("......get all eventLogEntries");
        int firstResult = pageIndex * pageSize;
        // we split the topic by swung dash if multiple topics are provided
        List<EventLog> eventLogEntries = eventLogService.findAllEvents(firstResult, pageSize);

        List<ItemCollection> result = new ArrayList<ItemCollection>();
        for (EventLog eventLog : eventLogEntries) {
            // Build a ItemCollection for each EventLog
            result.add(buildItemCollection(eventLog));
        }
        return XMLDataCollectionAdapter.getDataCollection(result);
    }

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
            result.add(buildItemCollection(eventLog));
        }
        return XMLDataCollectionAdapter.getDataCollection(result);
    }

    /**
     * This method locks an eventLog entry for processing. The topic will be
     * suffixed with '.lock' to indicate that this topic is locked by a process. If
     * a lock is successful a client can exclusive process this eventLog entry.
     * 
     * @param id - id of the event log entry
     * @return the method returns a Response OK in case of a successful lock.
     */
    @POST
    @Path("/lock/{id}")
    public Response lockEventLogEntry(@PathParam("id") String id) {
        EventLog _eventLogEntry = eventLogService.getEvent(id);
        if (_eventLogEntry != null) {
            // lock eventLogEntry....
            try {
                if (eventLogService.lock(_eventLogEntry)) {
                    return Response.status(Response.Status.OK).build();
                } else {
                    return Response.status(Response.Status.CONFLICT).build();
                }

            } catch (OptimisticLockException e) {
                // lock was not possible - continue....
                logger.info("...unable to lock EventLock: " + e.getMessage());

            }
        }
        return Response.status(Response.Status.CONFLICT).build();
    }

    /**
     * This method unlocks an eventLog entry. The topic suffix '.lock' will be
     * removed.
     * 
     * @param id - id of the event log entry
     */
    @POST
    @Path("/unlock/{id}")
    public Response unlockEventLogEntry(@PathParam("id") String id) {
        EventLog _eventLogEntry = eventLogService.getEvent(id);
        if (_eventLogEntry != null) {
            try {
                // unlock eventLogEntry....
                if (eventLogService.unlock(_eventLogEntry)) {
                    return Response.status(Response.Status.OK).build();
                } else {
                    return Response.status(Response.Status.CONFLICT).build();
                }
            } catch (OptimisticLockException e) {
                // lock was not possible - continue....
                logger.info("...unable to lock EventLock: " + e.getMessage());
            }
        }
        return Response.status(Response.Status.CONFLICT).build();
    }

    /**
     * This method unlocks eventlog entries which are older than 1 minute. We assume
     * that these events are deadlocks.
     *
     * @param interval - interval in millis
     * @param topic    - topic to search event log entries.
     */
    @POST
    @Path("/release/{interval}/{topic}")
    public void releaseDeadLocks(@PathParam("interval") long deadLockInterval, @PathParam("topic") String topic) {
        logger.finest("......releaseDeadLocks: " + topic);
        // we split the topic by swung dash if multiple topics are provided
        String[] topicList = topic.split("~");
        eventLogService.releaseDeadLocks(deadLockInterval, topicList);
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

    /**
     * This helper method converts a EventLog entity into a ItemCollection.
     * 
     * @param eventLog - event log entity
     * @return - ItemCollection
     */
    private ItemCollection buildItemCollection(EventLog eventLog) {
        if (eventLog == null) {
            return null;
        }
        ItemCollection itemColEvent = new ItemCollection();
        itemColEvent.setItemValue("id", eventLog.getId());
        itemColEvent.setItemValue("ref", eventLog.getRef());
        itemColEvent.setItemValue("created", eventLog.getCreated());
        itemColEvent.setItemValue("topic", eventLog.getTopic());
        itemColEvent.setItemValue("data", eventLog.getData());
        return itemColEvent;
    }

}
