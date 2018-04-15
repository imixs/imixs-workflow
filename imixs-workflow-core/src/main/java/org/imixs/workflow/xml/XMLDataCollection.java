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
 * The XMLDataCollection represents a list of XMLItemCollections. This root element
 * is used by JAXB api
 * 
 * @author rsoika
 * @version 0.0.1
 */
@XmlRootElement(name = "data")
public class XMLDataCollection implements java.io.Serializable {

	private static final long serialVersionUID = 1L;
	private XMLDocument[] document;

	public XMLDataCollection() {
		setDocument(new XMLDocument[] {});
	}

	public XMLDocument[] getDocument() {
		return document;
	}

	public void setDocument(XMLDocument[] entity) {
		this.document = entity;
	}

}
