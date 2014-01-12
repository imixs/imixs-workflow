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

import javax.persistence.*;

/**
 * The EntityIndex is a entity ejb which enables the
 * EntityService to manage different IndexProperties. These IndexProperties
 * are used to brand out attributes form the ItemCollection stored in the
 * data attribute of an Workitem. During the disasembling of an ItemCollection
 * the values of an ItemCollection are spread over different type specific
 * tables. So a Query with SQL and EQL is possible
 * 
 * @see org.imixs.workflow.jee.ejb.EntityPersistenceManagerImplementation
 * @author  Ralph Soika
 * @version 1.0
 */
@javax.persistence.Entity
public class EntityIndex implements java.io.Serializable {

	/**
	 * generated serialVersion ID
	 */
	private static final long serialVersionUID = 1L;
	
	public static final int TYP_TEXT = 0;

	public static final int TYP_INT = 1;

	public static final int TYP_DOUBLE = 2;

	public static final int TYP_CALENDAR = 3;

	
	
	private String name;

	private int typ;


	public EntityIndex() {
		super();		
	}


	public EntityIndex(String sname, int ityp) {
		this();
		setName(sname);
		setTyp(ityp);
	}


	@Id
	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name.toLowerCase();
	}


	public int getTyp() {
		return typ;
	}


	public void setTyp(int typ) {
		this.typ = typ;
	}





}
