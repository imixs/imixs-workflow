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

import java.io.Serializable;
import java.util.Arrays;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * The XMLitemCollection is a basic serializable representation of a pojo to map
 * a org.imixs.workflow.ItemCollection into a xml representation using JAXB api
 * 
 * @author rsoika
 * @version 0.0.1
 */
@XmlRootElement(name = "document")
public class XMLDocument implements Serializable {

	private static final long serialVersionUID = 1L;
	private XMLItem[] item;

	public XMLDocument() {
		this.setItem(new XMLItem[] {});
	}

	public XMLItem[] getItem() {
		return item;
	}

	public void setItem(XMLItem[] item) {
		this.item = item;
	}
	
	/**
	 * This method compares the item array
	 */
	public boolean equals(Object o) {
		if (!(o instanceof XMLDocument))
			return false;
		return Arrays.equals(item,((XMLDocument)o).item);
	}
	
}
