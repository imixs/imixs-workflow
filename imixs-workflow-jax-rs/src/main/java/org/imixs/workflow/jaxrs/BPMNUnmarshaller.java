/*******************************************************************************
 * <pre>
 *  Imixs Workflow 
 *  Copyright (C) 2001-2020 Imixs Software Solutions GmbH,  
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
 *      https://www.imixs.org
 *      https://github.com/imixs/imixs-workflow
 *  
 *  Contributors:  
 *      Imixs Software Solutions GmbH - initial API and implementation
 *      Ralph Soika - Software Developer
 * </pre>
 *******************************************************************************/

package org.imixs.workflow.jaxrs;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.logging.Logger;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import javax.xml.parsers.ParserConfigurationException;
import org.imixs.workflow.bpmn.BPMNModel;
import org.imixs.workflow.bpmn.BPMNParser;
import org.imixs.workflow.exceptions.ModelException;
import org.xml.sax.SAXException;

/**
 * The BPMNUnmarshaller converts a bpmn input stream into a BPMNModel instance.
 * 
 * @see ModelRestService putBPMNModel
 * @author rsoika
 */
@Provider
@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_OCTET_STREAM, MediaType.TEXT_PLAIN})
public class BPMNUnmarshaller implements MessageBodyReader<BPMNModel> {

  private static Logger logger = Logger.getLogger(BPMNUnmarshaller.class.getName());

  @SuppressWarnings("rawtypes")
  @Override
  public boolean isReadable(Class aClass, Type type, Annotation[] annotations,
      MediaType mediaType) {
    if (aClass == BPMNModel.class)
      return true;

    return false;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public BPMNModel readFrom(Class aClass, Type type, Annotation[] annotations, MediaType mediaType,
      MultivaluedMap multivaluedMap, InputStream bpmnInputStream)
      throws IOException, WebApplicationException {

    try {
      return BPMNParser.parseModel(bpmnInputStream, "UTF-8");
    } catch (ModelException e) {
      logger.warning("Invalid Model: " + e.getMessage());
      e.printStackTrace();
    } catch (ParseException e) {
      logger.warning("Invalid Model: " + e.getMessage());
      e.printStackTrace();
    } catch (ParserConfigurationException e) {
      logger.warning("Invalid Model: " + e.getMessage());
      e.printStackTrace();
    } catch (SAXException e) {
      logger.warning("Invalid Model: " + e.getMessage());
      e.printStackTrace();
    }

    return null;

  }
}
