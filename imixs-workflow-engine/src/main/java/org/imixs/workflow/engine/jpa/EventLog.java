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

package org.imixs.workflow.engine.jpa;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.InvalidAccessException;

import jakarta.persistence.Basic;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Version;

/**
 * The EventLog entity bean is used by the EventLogService to create and access
 * event log entries. An EventLog defines a unique event created during the
 * processing life-cycle of a workitem or the update life-cycle of a Document
 * entity.
 * <p>
 * An EventLog is an immutable entity. The object contains the following
 * additional properties
 * <ul>
 * <li>id - identifier for the event log entry
 * <li>ref - the reference id of the corresponding workitem or document entity
 * <li>topic - the topic of the eventlog
 * <li>created - the creation timestamp
 * <li>data - an optional data field
 * <li>timeout - an optional timestamp indicated the earliest processing time.
 * </ul>
 * <p>
 * The 'data' attribute of an eventLog is optional and can hold any kind of
 * event specific data (e.g. a Mail Message).
 * <p>
 * EventLog entities can be created and accessed by the EventLogService.
 * Typically a new EventLog entity is created within the same transaction of the
 * main processing or update life cycle. With this mechanism a client can be
 * sure that eventLogEntries returned by the EventLogService are created during
 * a committed Transaction.
 * <p>
 * Note: for the same document reference ($uniqueid) there can exist different
 * eventlog entries. Eventlog entries are unique over there internal ID.
 * 
 * @see org.imixs.workflow.engine.EventLogService
 * @author rsoika
 * @version 1.0
 */

@jakarta.persistence.Entity
public class EventLog implements java.io.Serializable {

    private static final long serialVersionUID = 1L;
    private String id;
    private String topic;
    private String ref;
    private Integer version;
    private Calendar created;
    private Map<String, List<Object>> data;
    private Calendar timeout;

    /**
     * default constructor for JPA
     */
    public EventLog() {
        super();
    }

    /**
     * Creates a new EventLog entity.
     * 
     * @param topic - the event topic
     * @param ref   - the reference to the associated document entity
     * @param data  - a optional data list
     */
    public EventLog(String topic, String ref, Map<String, List<Object>> data) {
        // Generate a new uniqueId
        this.id = WorkflowKernel.generateUniqueID();
        // Initialize objects
        Calendar cal = Calendar.getInstance();
        this.created = cal;
        this.topic = topic;
        this.ref = ref;
        this.data = data;
        this.timeout= cal; // default timeout = now
    }

    /**
     * returns the unique identifier for the Entity.
     * 
     * @return universal id
     */
    @Id
    public String getId() {
        return id;
    }

    protected void setId(String aID) {
        id = aID;
    }

    @Version
    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    /**
     * returns the topic property of the entity instance.
     * 
     * @return
     */
    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    /**
     * returns the reference ID ($uniqueid) of the associated document or workitem
     * instance.
     * 
     * @return
     */
    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    /**
     * returns the creation point of time.
     * 
     * @return time of creation
     */
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar getCreated() {
        return created;
    }

    public void setCreated(Calendar created) {
        this.created = created;
    }

    /**
     * returns an optional timeout information indicated the earliest processing
     * time.
     * 
     * @return time of creation
     */
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar getTimeout() {
        return timeout;
    }

    public void setTimeout(Calendar timeout) {
        this.timeout = timeout;
    }

    /**
     * returns the data object part of the Entity represented by a java.util.Map
     * <p>
     * Data is loaded eager because it is read in any case by the DocumentService.
     *
     * @return Map
     */
    @Lob
    @Basic(fetch = FetchType.EAGER)
    public Map<String, List<Object>> getData() {
        return data;
    }

    /**
     * sets a data object for this Entity.
     * <p>
     * Note: the modified timestamp will be updated automatically to the current
     * point of time (see setModified) independent from the value of the item
     * $modified. The item $modified will be updated by the DocumentService on read.
     * 
     * @param data
     * @throws InvalidAccessException if $modified is missing
     */
    public void setData(Map<String, List<Object>> itemCol) {
        this.data = itemCol;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((ref == null) ? 0 : ref.hashCode());
        result = prime * result + ((topic == null) ? 0 : topic.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EventLog other = (EventLog) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (ref == null) {
            if (other.ref != null)
                return false;
        } else if (!ref.equals(other.ref))
            return false;
        if (topic == null) {
            if (other.topic != null)
                return false;
        } else if (!topic.equals(other.topic))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return this.getTopic() + ":" + this.getId();
    }

}
