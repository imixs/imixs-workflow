package org.imixs.workflow.bpmn;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

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
	 * This method parses a BPMN model from a input stream and returns a
	 * instance of BPMNModel class. The InputStream is converted into a byte
	 * array to be stored into the BPMNModel as rawData. The rawData can be used
	 * to persist the input stream.
	 * 
	 * 
	 * @param bpmnInputStream
	 * @param encoding
	 *            - default encoding use to parse the stream
	 * @return List<ItemCollection> a model definition
	 * @throws ParseException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws ModelException
	 */
	public final static BPMNModel parseModel(InputStream bpmnInputStream, String encoding)
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		long lTime = System.currentTimeMillis();
		if (bpmnInputStream == null) {
			logger.severe("[BPMNParser] parseModel - inputStream is null!");
			throw new ParseException("inputStream is null", -1);
		}

		// copy stream into byte array to store content later in BMPMModel object
		byte[] rawData = streamToByteArray(bpmnInputStream);
		// Parse XML....
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();
		BPMNModelHandler bpmnHandler = new BPMNModelHandler();
		saxParser.parse(new ByteArrayInputStream(rawData), bpmnHandler);
		// build the model
		BPMNModel model = bpmnHandler.buildModel();

		// store file content from input stream into the BPMNmodel
		model.setRawData(rawData);

		logger.fine("...BPMN Model '" + model.getVersion() + "' parsed in " + (System.currentTimeMillis() - lTime) + "ms");
		return model;

	}

	private static byte[] streamToByteArray(InputStream ins) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] byteBuffer = new byte[1024];
		int len;
		while ((len = ins.read(byteBuffer)) > -1) {
			baos.write(byteBuffer, 0, len);
		}
		baos.flush();
		return baos.toByteArray();
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
	public final static BPMNModel parseModel(byte[] bpmnByteArray, String encoding)
			throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

		ByteArrayInputStream input = new ByteArrayInputStream(bpmnByteArray);
		return parseModel(input, encoding);

	}
}
