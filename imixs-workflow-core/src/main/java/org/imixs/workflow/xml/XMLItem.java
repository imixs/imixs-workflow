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
package org.imixs.workflow.xml;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents a single item inside a XMLItemCollection. An XMLItem has a name and a
 * value. The value can be any serializable collection of objects.
 * 
 * @author rsoika
 * 
 */
@XmlRootElement(name="item")
public class XMLItem implements java.io.Serializable {
	
	private static final long serialVersionUID = 1L;

	private java.lang.String name;

	private java.lang.Object[] value;

	public java.lang.String getName() {
		return name;
	}

	public void setName(java.lang.String name) {
		this.name = name;
	}

	public java.lang.Object[] getValue() {
		return value;
	}

	public void setValue(java.lang.Object[] values) {
		this.value = values;
	}
	
	
	
}
