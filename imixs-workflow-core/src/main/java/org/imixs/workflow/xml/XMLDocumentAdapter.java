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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.imixs.workflow.ItemCollection;

/**
 * An XMLDocumentAdapter converts a
 * <code>org.imixs.workflow.xml.XMLDocument</code> into a
 * <code> org.imixs.workflow.ItemCollection</code> and reverse
 * 
 * @author imixs.com - Ralph Soika
 * @version 1.1
 * @see org.imixs.workflow.ItemCollection
 */

public class XMLDocumentAdapter {
	private static Logger logger = Logger.getLogger(XMLDocumentAdapter.class.getName());

	/**
	 * This Method converts a <code>org.imixs.workflow.xml.XMLItemCollection</code>
	 * into a <code> org.imixs.workflow.ItemCollection</code> Returns null if entity
	 * == null
	 * 
	 * @param entity
	 * @return ItemCollection
	 */
	@SuppressWarnings({ "rawtypes" })
	public static ItemCollection putDocument(final XMLDocument xmlDocument) {
		ItemCollection itemCol = new ItemCollection();
		if (xmlDocument == null) {
			return itemCol;
		}

		XMLItem items[] = xmlDocument.getItem();
		if (items != null)
			for (int i = 0; i < items.length; i++) {
				XMLItem it = items[i];
				if (it == null)
					continue;
				String key = it.getName();
				
				
				Object[] valueArray = it.getValue();
				if (valueArray == null || valueArray.length==0) {
					// no value found
					itemCol.replaceItemValue(key, new Vector());
				} else {
					//create a mutable list
					List valueList = new ArrayList<>(Arrays.asList(valueArray));
					itemCol.replaceItemValue(key, valueList);
				}
				
//				if (it.getValue() == null) {
//					// no value found
//					itemCol.replaceItemValue(key, new Vector());
//				} else {
//					// force migration of embedded XMLItem elements!
//					// create a mutable list
//					//List valueList = new ArrayList<>(Arrays.asList(it.getValue(true)));
//					List valueList = new ArrayList<>(Arrays.asList(it.getValue()));
//					
//					
//					// we can not use Arrays.asList() in this content
//					// because list need to be modified
//					itemCol.replaceItemValue(key, valueList);
//				}
			}
		return itemCol;
	}

	/**
	 * This Method converts a <code> org.imixs.workflow.ItemCollection</code> into a
	 * <code>XMLDocument</code>
	 * 
	 * @param document
	 *            instance of a ItemCollection to be converted
	 */
	public static XMLDocument getDocument(final ItemCollection document) {
		return getDocument(document, null);
	}

	/**
	 * This Method converts a <code> org.imixs.workflow.ItemCollection</code> into a
	 * <code>XMLDocument</code>
	 * 
	 * <p>
	 * The method verifies if the values stored are basic java types. If not these
	 * values will not be converted!
	 * 
	 * @param document
	 *            instance of a ItemCollection to be converted
	 * @param itemNames
	 *            - optional list of item names to be converted. If null all items
	 *            will be converted
	 */
	@SuppressWarnings({ "unchecked" })
	public static XMLDocument getDocument(final ItemCollection document, final List<String> itemNames) {

		// create a deep copy of the source
		ItemCollection aItemCollection = (ItemCollection) document.clone();

		String itemName = null;
		XMLDocument entity = new XMLDocument();
		int i = 0;
		XMLItem[] items = null;

		if (aItemCollection != null) {
			// test if only a sublist of items should be converted
			if (itemNames != null && itemNames.size() > 0) {
				items = new XMLItem[itemNames.size()];
				for (String aField : itemNames) {
					// this code block guarantees that the order of items
					// returned
					itemName = aField;
					XMLItem item = new XMLItem();
					// test the ItemValue
					List<?> vOrg = aItemCollection.getItemValue(aField);
					item.setName(itemName);
					item.setValue(vOrg.toArray());

					items[i] = item;
					i++;
				}

			} else {
				// convert all items (no itemname list is provided)
				Iterator<?> it = aItemCollection.getAllItems().entrySet().iterator();
				int max = aItemCollection.getAllItems().entrySet().size();
				items = new XMLItem[max];

				// iterate over all items if no itemNames are provided
				while (it.hasNext()) {
					Map.Entry<String, List<?>> entry = (Entry<String, List<?>>) it.next();
					itemName = entry.getKey();
					XMLItem item = null;
					item = new XMLItem();
					item.setName(itemName);
					if (entry.getValue() != null) {
						item.setValue(entry.getValue().toArray());
						if (item != null) {
							items[i] = item;
							i++;
						}
					} else {
						logger.warning("putItemCollection - itemName=" + itemName + " has null value");
					}
				}
			}

			entity.setItem(items);
		}

		entity = sortItemsByName(entity);

		return entity;
	}

	/**
	 * This method sorts all items of a XMLItemCollection by item name.
	 * 
	 * @param xmlDocument
	 * @return
	 */
	public static XMLDocument sortItemsByName(XMLDocument xmlDocument) {

		XMLItem[] items = xmlDocument.getItem();
		Arrays.sort(items, new XMLItemComparator());

		xmlDocument.setItem(items);

		return xmlDocument;
	}

	/**
	 * This method imports an xml entity data stream containing a singel document
	 * and returns the ItemCollection.
	 * 
	 * @param inputStream
	 *            xml input stream
	 * @throws JAXBException
	 * @throws IOException
	 * @return List of ItemCollection objects
	 */
	public static ItemCollection readItemCollectionFromInputStream(InputStream inputStream)
			throws JAXBException, IOException {
		byte[] byteInput = null;

		if (inputStream == null) {
			return null;
		}
		byteInput = getBytesFromStream(inputStream);
		return readItemCollection(byteInput);

	}

	/**
	 * This method imports a single XMLItemCollection and returns the ItemCollection
	 * object.
	 * 
	 * @param inputStream
	 *            xml input stream
	 * @throws JAXBException
	 * @throws IOException
	 * @return List of ItemCollection objects
	 */
	public static ItemCollection readItemCollection(byte[] byteInput) throws JAXBException, IOException {
		XMLDocument ecol = readXMLDocument(byteInput);
		if (ecol != null) {
			// convert entity....
			ItemCollection itemCol = XMLDocumentAdapter.putDocument(ecol);
			return itemCol;
		}
		return null;
	}

	/**
	 * This method reads a XMLItemCollection from a byte array.
	 * 
	 * @param byteInput
	 *            - xml data
	 * @throws JAXBException
	 * @throws IOException
	 * @return List of ItemCollection objects
	 */
	public static XMLDocument readXMLDocument(byte[] byteInput) throws JAXBException, IOException {

		if (byteInput == null) {
			return null;
		}

		XMLDocument ecol = null;
		logger.finest("......importXmlEntityData - verifing content....");
		JAXBContext context = JAXBContext.newInstance(XMLDocument.class);
		Unmarshaller m = context.createUnmarshaller();

		ByteArrayInputStream input = new ByteArrayInputStream(byteInput);
		Object jaxbObject = m.unmarshal(input);
		if (jaxbObject == null) {
			throw new RuntimeException("readItemCollection error - wrong xml file format - unable to read content!");
		}

		ecol = (XMLDocument) jaxbObject;

		return ecol;

	}

	/**
	 * This method writes a ItemCollection into a Byte array representing a
	 * XMLDocument
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

		XMLDocument ecol = XMLDocumentAdapter.getDocument(document);
		StringWriter writer = new StringWriter();
		JAXBContext context = JAXBContext.newInstance(XMLDocument.class);
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
