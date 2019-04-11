/*******************************************************************************
 *  Imixs Workflow 
 *  Copyright (C) 2001, 2011 Imixs Software Solutions GmbH,  
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
 *  	http://www.imixs.org
 *  	http://java.net/projects/imixs-workflow
 *  
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika - Software Developer
 *******************************************************************************/

package org.imixs.workflow.engine.jpa;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javax.persistence.Basic;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.InvalidAccessException;

/**
 * This Document entity bean is a wrapper class for the
 * org.imixs.workflow.ItemCollection which is used in all Imixs-Workflow
 * Interfaces. The Document is used by the DocumentService to store
 * ItemCollections into a database, using the Java Persistence API. Each
 * Document is added into the Lucene index.
 * 
 * A Document contains a universal unique ID to identify the Entity. Also the
 * Document contains the following additional properties
 * <ul>
 * <li>type
 * <li>created
 * <li>modified
 * <li>
 * </ul>
 * 
 * The creation time represents the point of time where the Document object was
 * created. The modify property represents the point of time when the Document
 * was last modified by the DocumentService. The type property is used to
 * categorize documents in a database. If an ItemCollection contains the
 * attribute 'type' the value will be automatically mapped to the type property.
 * <p>
 * The data attribute is used to hold the ItemCollection data. It is mapped by a
 * OR-Mapper to a large object (Lob).
 * <p>
 * A Client should not work directly with an instance of the Document entity.
 * It's recommended to use the DocumentService which acts as a session facade to
 * manage instances of ItemCollection persisted in a database system.
 * <p>
 * 
 * 
 * @see org.imixs.workflow.jee.ejb.EntityService
 * @see org.imixs.workflow.jee.ejb.EntityService
 * @author rsoika
 * @version 1.0
 */

@javax.persistence.Entity
public class Document implements java.io.Serializable {

	private static final long serialVersionUID = 1L;
	private String id;
	private Integer version;
	private String type;
	private Calendar created;
	private Calendar modified;
	private Map<String, List<Object>> data;
	private boolean pending;

	/**
	 * A Document will be automatically initialized with a unique id and a creation
	 * date.
	 */
	public Document() {
		// Generate a new uniqueId
		id = WorkflowKernel.generateUniqueID();
		// Initialize objects
		Calendar cal = Calendar.getInstance();
		created = cal;
		modified = cal;
	}

	/**
	 * This constructor allows the creation of an Document Instance with a default
	 * uniqueID
	 * 
	 * @param aID
	 */
	public Document(String aID) {
		this();
		if (aID != null && !aID.isEmpty()) {
			// overwrite $UNIQUEID
			id = aID;
		}
	}

	/**
	 * This transient flag indicates if the document was just saved and is still
	 * managed by the entityManager. In this case the entity may not be detached by
	 * other methods during the same transaction. See issue #230.
	 * 
	 * @return save status
	 */
	@Transient
	public boolean isPending() {
		return pending;
	}

	public void setPending(boolean pandingState) {
		pending = pandingState;
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
	 * returns the type property of the entity instance. This property can be
	 * provided by an itemColleciton in the attribute 'type'. Values will be case
	 * sensitive!
	 * 
	 * @see org.imixs.workflow.jee.ejb.EntityService
	 * @return
	 */
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
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
	 * Returns the time of last modification. This attribute is synchronized by the
	 * DocumetnService with the item '$modified'.
	 * 
	 * @see setData()
	 * @return time of modification
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Calendar getModified() {
		return modified;
	}

	/**
	 * Set the time of last modification. This attribute is automatically
	 * synchronized with the item '$modified'.
	 */
	public void setModified(Calendar modified) {
		this.modified = modified;
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
	 * @throws InvalidAccessException
	 *             if $modified is missing
	 */
	public void setData(Map<String, List<Object>> itemCol) {
		this.data = itemCol;
	}

}
