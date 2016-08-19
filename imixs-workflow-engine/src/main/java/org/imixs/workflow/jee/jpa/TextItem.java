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

import java.math.BigInteger;

import javax.persistence.*;

/**
 * A TextItem is a subdata item of the data object class Entity. These subdata
 * types are used to extract single attributes of an ItemCollection from the
 * data Attribute of an Entity outside to separate entity ejbs. As both - the
 * Entity and the SubDataItem are Entity EJBs this facilitate the possibility
 * of a object-relational data mapping into different table.
 * 
 * @see org.imixs.workflow.jee.jpa.Entity
 * @author  Ralph Soika
 * @version 1.0
 * 
 */
@javax.persistence.Entity
public class TextItem implements java.io.Serializable {

	/**
	 *  default serial id
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	private BigInteger id;

	public String itemValue;

	public String itemName;

	@SuppressWarnings("unused")
	private TextItem() {
	}

	public TextItem(String name, String value) {
		itemValue = value;
		itemName = name;
	}

	
	
	
	@Override
	public int hashCode() {
		return ((itemValue+itemName).hashCode());
		//return super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof TextItem))
			return false;

		TextItem aItem = (TextItem) obj;
		// test if null objects
		if (aItem.itemName==null || aItem.itemValue==null)
			return false;
		
		return (aItem.itemName.equals(itemName) && aItem.itemValue
				.equals(itemValue));

	}

}
