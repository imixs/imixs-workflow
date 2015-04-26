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
@Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_OCTET_STREAM,
		MediaType.TEXT_PLAIN })
public class BPMNUnmarshaller implements MessageBodyReader<BPMNModel> {

	private static Logger logger = Logger.getLogger(BPMNUnmarshaller.class
			.getName());

	@SuppressWarnings("rawtypes")
	@Override
	public boolean isReadable(Class aClass, Type type,
			Annotation[] annotations, MediaType mediaType) {
		if (aClass==BPMNModel.class)
			return true;
		
		return false;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public BPMNModel readFrom(Class aClass, Type type,
			Annotation[] annotations, MediaType mediaType,
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