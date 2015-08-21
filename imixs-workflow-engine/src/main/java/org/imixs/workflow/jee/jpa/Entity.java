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

package org.imixs.workflow.jee.jpa;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.persistence.Basic;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import org.imixs.workflow.WorkflowKernel;

/**
 * This Entity ejb is a wrapper class for the org.imixs.workflow.ItemCollection
 * which is used in all Imixs Workflow Interfaces. The Entity is used by the
 * EntityServiceBean to store ItemCollections into a database using the JEE Java
 * Persistence API. Each Entity contains a universal unique ID to identify the
 * Entity. Also the Entiy supports the additional properties
 * <ul>
 * <li>type
 * <li>created
 * <li>modified
 * <li>readAccess
 * <li>writeAccess
 * <li>textItems
 * <li>calendarItems
 * <li>integerItems
 * <li>
 * </ul>
 * 
 * The creation time represents the point of time where the ItemCollection was
 * first saved by the EntityService to the Database. The modify property
 * represents the point of time where the ItemCollection was last saved by the
 * EntityService. The type property is used to categorize entities in a
 * database. This property is used by the save() method of the
 * EntityServiceBean. So if an ItemCollection contains the attriubte 'type' the
 * value will be automatically mapped to the type property. The properties read-
 * and writeAccess containing the current access restrictions to an
 * ItemCollection managed by the EntityService.
 * <p>
 * The data attribute is used to hold the ItemCollection data. It is mapped by a
 * OR-Mapper to a large object (Lob). There is no way to query single attributes
 * using the EJB Query Language. To support the powerful EJB Query language the
 * Entity contains additional index properties to branch properties of an
 * ItemCollection into onToMany relationships. These are the supported Index
 * properties:
 * <ul>
 * <li>TextItem
 * <li>IntegerItem
 * <li>DoubleItem
 * <li>CalendarItem
 * </ul>
 * A Client should not work directly with an instance of the Entity EJB or its
 * index Properties. Its recommended to use the EntityService which acts as a
 * session facade to manage instances of ItemCollection in a database system.
 * <p>
 * Notice: All relationships are marked as FetchType=EAGER. This is because the
 * load and find Methods of the EntiyServiceBean do a clear() call to the
 * PersitenceContext because the implodeEntity() method of the EntityServiceBean
 * will modifies the values of an entity. So the Entity needs to be detached
 * Immediately.
 * <p>
 * Why did the Entity Table use Join tables to link to OneToMany relationships ?
 * <p>
 * The default schema-level mapping for unidirectional one-to-many relationships
 * uses a join table, as described in JSR 220 - Section 2.1.8.5. Unidirectional
 * one-to-many relationships may be implemented using one-to-many foreign key
 * mappings, however, such support is not required in this release. Applications
 * that want to use a foreign key mapping strategy for one-to-many relationships
 * should make these relationships bidirectional to ensure portability.
 * <p>
 * To store the Text-, Integer-, Double- and Calendar- Lists we can not use
 * HashSet because the order in which elements are returned by a HashSet's
 * iterator is not specified. This is the reason why we use Vectors to store the
 * values and check for duplicates manually. So we make sure that values added
 * to the list are not recorded
 * 
 * 
 * @see org.imixs.workflow.jee.ejb.EntityService
 * @see org.imixs.workflow.jee.ejb.EntityService
 * @author rsoika
 * @version 1.0
 */

@javax.persistence.Entity
public class Entity implements java.io.Serializable {

	private static final long serialVersionUID = 1L;
	private String id;
	private Integer version;
	private String type;
	private Calendar created;
	private Calendar modified;
	private Map<String,List<Object>> data;
	private List<ReadAccess> readAccessList;
	private List<WriteAccess> writeAccessList;

	private List<TextItem> textItems;
	private List<IntegerItem> integerItems;
	private List<DoubleItem> doubleItems;
	private List<CalendarItem> calendarItems;

	
	/**
	 * A Entity will be automatically initialized with a unique id and a
	 * creation date.
	 */
	public Entity() {
		/*
		 * Generate a new uniqueId
		 */
		id=WorkflowKernel.generateUniqueID();
		/*
		String sIDPart1 = Long.toHexString(System.currentTimeMillis());
		Double d = Math.random() * 900000000;
		int i = d.intValue();
		String sIDPart2 = Integer.toHexString(i);
		id = sIDPart1 + "-" + sIDPart2;
		*/

		// Initialize objects
		Calendar cal = Calendar.getInstance();
		created = cal;
	}

	/**
	 * This constructor allows the creation of an Entity Instance with a default
	 * uniqueID
	 * 
	 * @param aID
	 */
	public Entity(String aID) {
		this();
		if (aID != null && !"".equals(aID))
			id = aID;
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
	 * provided by an itemColleciton in the attribute 'type'. Values will be
	 * case sensitive!
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
	 * returns the last modification point of time
	 * 
	 * @return time of modification
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Calendar getModified() {
		return modified;
	}

	public void setModified(Calendar modified) {
		this.modified = modified;
	}

	/**
	 * updates the modification time before a update by a persistence manager is
	 * performed.
	 */
	@PrePersist
	@PreUpdate
	private void updateModified() {
		Calendar cal = Calendar.getInstance();
		modified = cal;
	}

	/**
	 * returns the data object part of the Entity represented by a
	 * java.util.Map
	 * 
	 * @return Map
	 */
	@Lob
	@Basic(fetch = FetchType.LAZY)
	public Map<String, List<Object>> getData() {
		return data;
	}

	/**
	 * sets a data object for this Entity.
	 * 
	 * @param data
	 */
	public void setData(Map<String,List<Object>> itemCol) {
		this.data = itemCol;
	}

	/**
	 * ReadAccess list is loaded eager as this need to be check on every access
	 * 
	 * @return ReadAccess list for the entity
	 */
	@OneToMany(fetch = FetchType.EAGER)
	public List<ReadAccess> getReadAccessList() {
		if (readAccessList==null)
			readAccessList=new ArrayList<ReadAccess>();
		return readAccessList;
	}

	public void setReadAccessList(List<ReadAccess> readAccessList) {
		this.readAccessList = readAccessList;
	}
	
	

	/**
	 * WrateAccess list is loaded lazy as this a check is only on update method
	 * needed
	 * 
	 * @return WriteAccess list for the entity
	 */
	@OneToMany(fetch = FetchType.LAZY)
	public List<WriteAccess> getWriteAccessList() {
		if (writeAccessList==null)
			writeAccessList=new ArrayList<WriteAccess>();
		return writeAccessList;
	}

	public void setWriteAccessList(List<WriteAccess> writeAccessList) {
		this.writeAccessList = writeAccessList;
	}
	
	
	/**
	 * returns a list of all textItems joined to this Entity.
	 * 
	 * @return a collection of TextItem objects
	 */
	@OneToMany(fetch = FetchType.LAZY)
	public List<TextItem> getTextItems() {
		if (textItems == null)
			textItems = new Vector<TextItem>();
		return textItems;
	}

	public void setTextItems(List<TextItem> textItems) {
		this.textItems = textItems;
	}
	
	
	/**
	 * returns a list of all integerItems joined to this Entity
	 * 
	 * @return a collection of IntegerItem objects
	 */
	@OneToMany(fetch = FetchType.LAZY)
	public List<IntegerItem> getIntegerItems() {
		if (integerItems == null)
			integerItems = new Vector<IntegerItem>();
		return integerItems;
	}

	public void setIntegerItems(List<IntegerItem> integerItems) {
		this.integerItems = integerItems;
	}

	/**
	 * returns a list of all doubleItems joined to this Entity
	 * 
	 * @return a collection of DoubleItem objects
	 */
	@OneToMany(fetch = FetchType.LAZY)
	public List<DoubleItem> getDoubleItems() {
		if (doubleItems == null)
			doubleItems = new Vector<DoubleItem>();
		return doubleItems;
	}

	public void setDoubleItems(List<DoubleItem> doubleItems) {
		this.doubleItems = doubleItems;
	}

	/**
	 * returns a list of all calendarItems joined to this Entity
	 * 
	 * @return a collection of CalendarItem objects
	 */
	@OneToMany(fetch = FetchType.LAZY)
	public List<CalendarItem> getCalendarItems() {
		if (calendarItems == null)
			calendarItems = new Vector<CalendarItem>();
		return calendarItems;
	}

	public void setCalendarItems(List<CalendarItem> calendarItems) {
		this.calendarItems = calendarItems;
	}

	
	
	
	
	/*
	@ElementCollection(fetch = FetchType.LAZY)
	public List<TextItem> getTextItems() {
		return textItems;
	}

	public void setTextItems(List<TextItem> textItems) {
		this.textItems = textItems;
	}
	*/

	
	/*
	@ElementCollection(fetch = FetchType.LAZY)
	public Map<String, List<String>> getTextItems() {
		return textItems;
	}

	public void setTextItems(Map<String,List<String>> textItems) {
		this.textItems = textItems;
	}

	@ElementCollection(fetch = FetchType.LAZY)
	public Map<String, List<Integer>> getIntegerItems() {
		return integerItems;
	}

	public void setIntegerItems(Map<String, List<Integer>> integerItems) {
		this.integerItems = integerItems;
	}
	*/
}
