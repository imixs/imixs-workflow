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

package org.imixs.workflow.jaxrs.v40;

import java.io.IOException;
import java.io.OutputStream;

import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

/**
 * The RootService provides the api description
 * 
 * @author rsoika
 * 
 */
@Path("/v40/")
@Produces({ MediaType.TEXT_HTML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.TEXT_XML })
@Stateless
public class RootRestServiceV40 {

	
	@GET
	@Produces(MediaType.APPLICATION_XHTML_XML)
	//@Path("/") generates jersey warning
	public StreamingOutput getRoot() {

		return new StreamingOutput() {
			public void write(OutputStream out) throws IOException, WebApplicationException {

				out.write("<div class=\"root\">".getBytes());
				out.write("<a href=\"/documents\" type=\"application/xml\" rel=\"documents\"/>".getBytes());
				out.write("<a href=\"/workflow\" type=\"application/xml\" rel=\"workflow\"/>".getBytes());
				out.write("<a href=\"/model\" type=\"application/xml\" rel=\"model\"/>".getBytes());
				out.write("<a href=\"/report\" type=\"application/xml\" rel=\"report\"/>".getBytes());
				out.write("<a href=\"/adminp\" type=\"application/xml\" rel=\"adminp\"/>".getBytes());
				out.write("</div>".getBytes());
			}
		};

	}
}
