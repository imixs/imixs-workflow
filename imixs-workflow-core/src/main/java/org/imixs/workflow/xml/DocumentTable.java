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

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * The JAXB DocumentTable represents a list of documents in a table format. 
 * For each document the same list of items will be added into a separate row. 
 * The property labels contans the table headers.
 * 

 * @author rsoika
 * @version 2.0.0
 */
@XmlRootElement(name = "data")
public class DocumentTable implements java.io.Serializable {

	private static final long serialVersionUID = 1L;
	private XMLItemCollection[] document;
	private List<String> items;
	private List<String> labels;

	public DocumentTable() {
		setDocument(new XMLItemCollection[] {});
	}
	
	public DocumentTable(XMLItemCollection[] documents,List<String> items,List<String> labels) {
		setDocument(documents);
		setItems(items);
		setLabels(labels);
	}

	public XMLItemCollection[] getDocument() {
		return document;
	}

	public void setDocument(XMLItemCollection[] document) {
		this.document = document;
	}

	public List<String> getItems() {
		return items;
	}

	public void setItems(List<String> items) {
		this.items = items;
	}

	public List<String> getLabels() {
		return labels;
	}

	public void setLabels(List<String> labels) {
		this.labels = labels;
	}


	
	

}
