package org.imixs.workflow.bpmn;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.imixs.workflow.ItemCollection;
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
	 * This method parses a BPMN model from a input stream
	 * 
	 * 
	 * 
	 * @param requestBodyStream
	 * @param encoding
	 *            - default encoding use to parse the stream
	 * @return List<ItemCollection> a model definition
	 * @throws ParseException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 */
	public final static List<ItemCollection> parseModel(
			InputStream bpmnInputStream, String encoding)
			throws ParseException, ParserConfigurationException, SAXException,
			IOException {

		if (bpmnInputStream == null) {
			logger.severe("[BPMNParser] parseModel - inputStream is null!");
			throw new java.text.ParseException("inputStream is null", -1);
		}

		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();

		BPMNDefaultHandler bpmnHandler = new BPMNDefaultHandler();

		saxParser.parse(bpmnInputStream, bpmnHandler);

		
		List<ItemCollection> model = new ArrayList<ItemCollection>();

		return model;
	}

}
