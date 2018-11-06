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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.imixs.workflow.ItemCollection;

/**
 * An XMLItemCollectionAdapter converts a
 * <code>org.imixs.workflow.xml.XMLItemCollection</code> into a
 * <code> org.imixs.workflow.ItemCollection</code> and reverse
 * 
 * @author imixs.com - Ralph Soika
 * @version 1.1
 * @see org.imixs.workflow.ItemCollection
 */

public class XMLDataCollectionAdapter {
	private static Logger logger = Logger.getLogger(XMLDataCollectionAdapter.class.getName());

	/**
	 * This Method converts a <code>org.imixs.workflow.xml.DocumentCollection</code>
	 * into a List of <code>org.imixs.workflow.ItemCollection</code>
	 * 
	 * The method returns an empty list if the collection is empty or null
	 * 
	 * @param entity
	 * @return ItemCollection
	 */
	public static List<ItemCollection> putDataCollection(XMLDataCollection xmlDocuments) {
		List<ItemCollection> result = new ArrayList<ItemCollection>();

		if (xmlDocuments != null && xmlDocuments.getDocument() != null) {
			for (int i = 0; i < xmlDocuments.getDocument().length; i++) {
				XMLDocument xmlItemCol = xmlDocuments.getDocument()[i];
				result.add(XMLDocumentAdapter.putDocument(xmlItemCol));
			}
		}
		return result;
	}

	/**
	 * This method transforms a Collection<ItemCollection> into a DocumentCollection
	 * 
	 * @param documents
	 * @return
	 */
	public static XMLDataCollection getDataCollection(final Collection<ItemCollection> documents) {
		return getDataCollection(documents, null);
	}

	/**
	 * This method transforms a Collection<ItemCollection> into a
	 * XMLDocumentCollection
	 * 
	 * If the attribute List is provided only the corresponding properties will be
	 * returned.
	 * 
	 * @param documents
	 *            - collection of ItemCollection objects to be converted
	 * @param itemNames
	 *            - optional list of item names to be converted. If null all items
	 *            will be converted
	 * @return
	 */
	public static XMLDataCollection getDataCollection(final Collection<ItemCollection> documents,
			final List<String> itemNames) {
		XMLDataCollection entiCol = new XMLDataCollection();
		Iterator<ItemCollection> it = documents.iterator();
		int max = documents.size();
		int i = 0;
		XMLDocument[] entities = new XMLDocument[max];
		while (it.hasNext()) {
			ItemCollection icw = (ItemCollection) it.next();
			if (icw != null) {
				XMLDocument entity = XMLDocumentAdapter.getDocument(icw, itemNames);
				entities[i] = entity;
				i++;
			}
		}
		if (max > 0)
			entiCol.setDocument(entities);
		return entiCol;
	}

	/**
	 * This method transforms a single ItemCollection into a XMLDocumentCollection
	 * 
	 */
	public static XMLDataCollection getDataCollection(final ItemCollection document) {
		List<ItemCollection> col = new ArrayList<ItemCollection>();
		col.add(document);
		return getDataCollection(col, null);
	}

	/**
	 * This method transforms a single ItemCollection into a XMLDocumentCollection
	 * 
	 */
	public static XMLDataCollection getDataCollection(final ItemCollection document, final List<String> itemNames) {
		List<ItemCollection> col = new ArrayList<ItemCollection>();
		col.add(document);
		return getDataCollection(col, itemNames);
	}

	/**
	 * This method imports an xml entity data stream and returns a List of
	 * ItemCollection objects. The method can import any kind of entity data like
	 * model or configuration data an xml export of workitems.
	 * 
	 * @param inputStream
	 *            xml input stream
	 * @throws JAXBException
	 * @throws IOException
	 * @return List of ItemCollection objects
	 */
	public static List<ItemCollection> readCollectionFromInputStream(InputStream inputStream)
			throws JAXBException, IOException {
		byte[] byteInput = null;

		if (inputStream == null) {
			return null;
		}
		byteInput = getBytesFromStream(inputStream);
		return readCollection(byteInput);

	}

	/**
	 * This method imports an xml entity data byte array and returns a List of
	 * ItemCollection objects. The method can import any kind of entity data like
	 * model or configuration data an xml export of workitems.
	 * 
	 * @param inputStream
	 *            xml input stream
	 * @throws JAXBException
	 * @throws IOException
	 * @return List of ItemCollection objects
	 */
	public static List<ItemCollection> readCollection(byte[] byteInput) throws JAXBException, IOException {

		List<ItemCollection> resultList = new ArrayList<ItemCollection>();

		if (byteInput == null) {
			return null;
		}

		XMLDataCollection ecol = null;
		logger.finest("......readCollection importXmlEntityData - verifing  content....");
		JAXBContext context = JAXBContext.newInstance(XMLDataCollection.class);
		Unmarshaller m = context.createUnmarshaller();

		ByteArrayInputStream input = new ByteArrayInputStream(byteInput);
		Object jaxbObject = m.unmarshal(input);
		if (jaxbObject == null) {
			throw new RuntimeException("readCollection error - wrong xml file format - unable to read content!");
		}

		ecol = (XMLDataCollection) jaxbObject;

		// convert entities....
		if (ecol.getDocument().length > 0) {
			for (XMLDocument aentity : ecol.getDocument()) {
				resultList.add(XMLDocumentAdapter.putDocument(aentity));
			}
			logger.fine("readCollection" + ecol.getDocument().length + " entries sucessfull imported");
		}
		return resultList;

	}

	/**
	 * This method writes a collection of ItemCollection into a Byte array representing a XMLDataCollection
	 * 
	 * @param inputStream
	 *            xml input stream
	 * @throws JAXBException
	 * @throws IOException
	 * @return List of ItemCollection objects
	 */
	public static byte[] writeItemCollection(final Collection<ItemCollection> documents)
			throws JAXBException, IOException {
		if (documents == null || documents.size() == 0) {
			return null;
		}
		XMLDataCollection ecol = XMLDataCollectionAdapter.getDataCollection(documents);
		StringWriter writer = new StringWriter();
		JAXBContext context = JAXBContext.newInstance(XMLDataCollection.class);
		Marshaller m = context.createMarshaller();
		m.marshal(ecol, writer);
		return writer.toString().getBytes();
	}

	/**
	 * This method writes a ItemCollection into a Byte array representing a
	 * XMLDataCollection
	 * 
	 * @param inputStream
	 *            xml input stream
	 * @throws JAXBException
	 * @throws IOException
	 * @return List of ItemCollection objects
	 */
	public static byte[] writeItemCollection(ItemCollection document) throws JAXBException, IOException {
		if (document == null) {
			return null;
		}
		XMLDataCollection ecol = XMLDataCollectionAdapter.getDataCollection(document);
		StringWriter writer = new StringWriter();
		JAXBContext context = JAXBContext.newInstance(XMLDataCollection.class);
		Marshaller m = context.createMarshaller();
		m.marshal(ecol, writer);
		return writer.toString().getBytes();
	}

	private static byte[] getBytesFromStream(InputStream is) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int nRead;
		byte[] data = new byte[0x4000];
		while ((nRead = is.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}
		buffer.flush();
		is.close();
		return buffer.toByteArray();
	}

}
