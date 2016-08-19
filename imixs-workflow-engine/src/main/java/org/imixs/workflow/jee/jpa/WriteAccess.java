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

import javax.persistence.GeneratedValue;
import javax.persistence.Id;


/**
 * A AccessEntity defines a single Entry in the Access List of the Entity Class.
 * An AccessEntity is defined by its entityID which is a unique primary key and
 * its entry which is a String value containing the entryValue. This is typical
 * a UserName or a Role Name defined in the ejb-jar.xml
 * 
 * @see org.imixs.workflow.jee.jpa.Entity
 * @author Ralph Soika
 * @version 1.0
 * 
 */
@javax.persistence.Entity
public class WriteAccess implements java.io.Serializable {

	/**
	 *  default serial id
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	private int id;
	
	private String value;

	@SuppressWarnings("unused")
	private WriteAccess() {
	}

	public WriteAccess(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
	
	
}
