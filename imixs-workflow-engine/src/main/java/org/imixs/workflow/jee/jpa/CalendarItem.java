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

import java.math.BigInteger;
import java.util.*;

/**
 * A CalendarItem is a subdata item of the data object class WorkItem. These
 * subdata types are used to extract single attributes of an ItemCollection from
 * the data Attribute of a WorkItem outside to seperate entity ejbs. As both -
 * the WorkItem and the SubDataItem are Entity EJBs this facilitate the
 * possibility of a object-relational data mapping into different table.
 * 
 * @see org.imixs.workflow.jee.jpa.Entity
 * @see org.imixs.workflow.jee.ejb.EntityService
 * @author Ralph Soika
 * @version 1.0
 * 
 */

@javax.persistence.Entity
public class CalendarItem implements java.io.Serializable {

	/**
	 *  default serial id
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	private BigInteger id;

	@Temporal(TemporalType.TIMESTAMP)
	public Calendar itemValue;

	public String itemName;

	@SuppressWarnings("unused")
	private CalendarItem() {
	}

	public CalendarItem(String name, Calendar value) {
		itemValue = value;
		itemName = name;
	}
}
