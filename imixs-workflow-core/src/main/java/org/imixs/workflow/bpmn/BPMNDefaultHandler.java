package org.imixs.workflow.bpmn;

import java.util.logging.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class BPMNDefaultHandler extends DefaultHandler {
	
	private static Logger logger = Logger.getLogger(BPMNDefaultHandler.class.getName());

	
	boolean bDefinitions = false;
	boolean bExtensionElements = false;
	boolean bImixsProperty = false;
	boolean bsalary = false;

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {

		logger.info("Start Element :" + qName);

		
		// bpmn2:definitions
		if (qName.equalsIgnoreCase("bpmn2:definitions")) {
			bDefinitions = true;
		}

		if (qName.equalsIgnoreCase("bpmn2:extensionElements")) {
			bExtensionElements = true;
		}

		if (qName.equalsIgnoreCase("imixs:property")) {
			bImixsProperty = true;
		}

		// if a task element is found verify if imixs:processid exists to identify a Imixs Workflow Taks element
		if (qName.equalsIgnoreCase("bpmn2:task")) {
			//attributes.g
			bsalary = true;
		}

	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

		System.out.println("End Element :" + qName);

	}

	@Override
	public void characters(char ch[], int start, int length)
			throws SAXException {

		if (bDefinitions) {
			System.out.println("First Name : " + new String(ch, start, length));
			bDefinitions = false;
		}

		if (bExtensionElements) {
			System.out.println("Last Name : " + new String(ch, start, length));
			bExtensionElements = false;
		}

		if (bImixsProperty) {
			System.out.println("Nick Name : " + new String(ch, start, length));
			bImixsProperty = false;
		}

		if (bsalary) {
			System.out.println("Salary : " + new String(ch, start, length));
			bsalary = false;
		}

	}

}
