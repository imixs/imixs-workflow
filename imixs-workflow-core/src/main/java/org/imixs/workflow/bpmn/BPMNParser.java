package org.imixs.workflow.bpmn;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.ModelException;
import org.xml.sax.SAXException;

/**
 * This class parses an BPMN model and transform the content into a Imixs
 * Workflow Model definition.
 * 
 * 
 * 
 * 
 * @see: http://www.mkyong.com/java/how-to-read-xml-file-in-java-sax-parser/
 *       http://tutorials.jenkov.com/java-xml/sax-example.html
 * 
 * 
 * @author rsoika
 *
 */
public class BPMNParser {

	private static Logger logger = Logger.getLogger(BPMNParser.class.getName());

	/**
	 * This method parses a BPMN model from a input stream.
	 * 
	 * @param requestBodyStream
	 * @param encoding
	 *            - default encoding use to parse the stream
	 * @return List<ItemCollection> a model definition
	 * @throws ParseException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws ModelException
	 */
	public final static BPMNModel parseModel(InputStream bpmnInputStream,
			String encoding) throws ParseException,
			ParserConfigurationException, SAXException, IOException,
			ModelException {

		long lTime = System.currentTimeMillis();
		if (bpmnInputStream == null) {
			logger.severe("[BPMNParser] parseModel - inputStream is null!");
			throw new java.text.ParseException("inputStream is null", -1);
		}

		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();

		BPMNModelHandler bpmnHandler = new BPMNModelHandler();

		saxParser.parse(bpmnInputStream, bpmnHandler);

		BPMNModel model = bpmnHandler.buildModel();
		
		ItemCollection definition=model.getDefinition();
		String version="";
		if (definition!=null) {
			version=definition.getModelVersion();
		}
		logger.info("BPMN Model ["+version+"] parsed in "
				+ (System.currentTimeMillis() - lTime) + "ms");
		return model;

	}

	/**
	 * This method parses a BPMN model from a byte array.
	 * 
	 * @param requestBodyStream
	 * @param encoding
	 *            - default encoding use to parse the stream
	 * @return List<ItemCollection> a model definition
	 * @throws ParseException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws ModelException
	 */
	public final static BPMNModel parseModel(byte[] bpmnByteArray,
			String encoding) throws ParseException,
			ParserConfigurationException, SAXException, IOException,
			ModelException {

		ByteArrayInputStream input = new ByteArrayInputStream(bpmnByteArray);
		return parseModel(input, encoding);

	}
}
