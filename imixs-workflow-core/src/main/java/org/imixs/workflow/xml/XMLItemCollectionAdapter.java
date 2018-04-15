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
import java.util.Collection;
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
 * An XMLItemCollectionAdapter converts a
 * <code>org.imixs.workflow.xml.XMLItemCollection</code> into a
 * <code> org.imixs.workflow.ItemCollection</code> and reverse
 * 
 * @author imixs.com - Ralph Soika
 * @version 1.1
 * @see org.imixs.workflow.ItemCollection
 */

public class XMLItemCollectionAdapter {
	private static Logger logger = Logger.getLogger(XMLItemCollectionAdapter.class.getName());

	/**
	 * This Method converts a <code>org.imixs.workflow.xml.XMLItemCollection</code>
	 * into a <code> org.imixs.workflow.ItemCollection</code> Returns null if entity
	 * == null
	 * 
	 * @param entity
	 * @return ItemCollection
	 */
	@SuppressWarnings({ "rawtypes" })
	public static ItemCollection getItemCollection(final XMLItemCollection entity) {
		ItemCollection itemCol = new ItemCollection();
		if (entity == null) {
			return itemCol;
		}

		XMLItem items[] = entity.getItem();
		if (items != null)
			for (int i = 0; i < items.length; i++) {
				XMLItem it = items[i];
				if (it == null)
					continue;
				String key = it.getName();
				if (it.getValue() == null) {
					// no value found
					itemCol.replaceItemValue(key, new Vector());
				} else {
					// force migration of embedded XMLItem elements!
					// create a mutable list
					List valueList = new ArrayList<>(Arrays.asList(it.getValue(true)));
					// we can not use Arrays.asList() in this content
					// because list need to be modified
					itemCol.replaceItemValue(key, valueList);
				}
			}
		return itemCol;
	}

	/**
	 * This Method converts a <code>org.imixs.workflow.xml.DocumentCollection</code>
	 * into a List of <code>org.imixs.workflow.ItemCollection</code>
	 * 
	 * The method returns an empty list if the collection is empty or null
	 * 
	 * @param entity
	 * @return ItemCollection
	 */
	public static List<ItemCollection> getCollection(DocumentCollection doccol) {
		List<ItemCollection> result = new ArrayList<ItemCollection>();

		if (doccol != null && doccol.getDocument() != null) {
			for (int i = 0; i < doccol.getDocument().length; i++) {
				XMLItemCollection xmlItemCol = doccol.getDocument()[i];
				result.add(getItemCollection(xmlItemCol));
			}
		}
		return result;
	}

	/**
	 * This Method converts a <code> org.imixs.workflow.ItemCollection</code> into a
	 * <code>XMLItemCollection</code>
	 * 
	 * <p>
	 * The method verifies if the values stored are basic java types. If not these
	 * values will not be converted!
	 * 
	 * @param sourceItemCollection
	 *            ItemCollection Object to be converted
	 * @param itemNames
	 *            - optional list of item names to be converted. If null all items
	 *            will be converted
	 */
	@SuppressWarnings({ "unchecked" })
	public static XMLItemCollection putItemCollection(final ItemCollection sourceItemCollection,
			final List<String> itemNames) {

		// create a deep copy of the source
		ItemCollection aItemCollection = (ItemCollection) sourceItemCollection.clone();

		String itemName = null;
		XMLItemCollection entity = new XMLItemCollection();
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
	 * @param xmlItemCol
	 * @return
	 */
	public static XMLItemCollection sortItemsByName(XMLItemCollection xmlItemCol) {

		XMLItem[] items = xmlItemCol.getItem();
		Arrays.sort(items, new XMLItemComparator());

		xmlItemCol.setItem(items);

		return xmlItemCol;
	}

	/**
	 * This Method converts a <code> org.imixs.workflow.ItemCollection</code> into a
	 * <code>Entity</code>
	 * 
	 * <p>
	 * The method verifies if the values stored are basic java types. If not these
	 * values will not be converted!
	 * 
	 * @param aItemCollection
	 *            Collection Object to be converted
	 */
	public static XMLItemCollection putItemCollection(final ItemCollection aItemCollection) {
		return putItemCollection(aItemCollection, null);
	}

	/**
	 * This method transforms a Collection<ItemCollection> into a DocumentCollection
	 * 
	 * @param col
	 * @return
	 */
	public static DocumentCollection putDocuments(final Collection<ItemCollection> col) {

		return putDocuments(col, null);
	}

	/**
	 * This method transforms a Collection<ItemCollection> into a DocumentCollection
	 * 
	 * If the attribute List is provided only the corresponding properties will be
	 * returned.
	 * 
	 * @param col
	 *            - collection of ItemCollection objects to be converted
	 * @param itemNames
	 *            - optional list of item names to be converted. If null all items
	 *            will be converted
	 * @return
	 */
	public static DocumentCollection putDocuments(final Collection<ItemCollection> col, final List<String> itemNames) {
		DocumentCollection entiCol = new DocumentCollection();
		Iterator<ItemCollection> it = col.iterator();
		int max = col.size();
		int i = 0;
		XMLItemCollection[] entities = new XMLItemCollection[max];
		while (it.hasNext()) {
			ItemCollection icw = (ItemCollection) it.next();
			XMLItemCollection entity = putItemCollection(icw, itemNames);
			entities[i] = entity;
			i++;
		}
		if (max > 0)
			entiCol.setDocument(entities);
		return entiCol;
	}

	
	
	/**
	 * This method transforms a single ItemCollection into a DocumentCollection with one element. 
	 * 
	 * If the attribute List is provided only the corresponding properties will be
	 * returned.
	 * 
	 * @param col
	 *            - collection of ItemCollection objects to be converted
	 * @param itemNames
	 *            - optional list of item names to be converted. If null all items
	 *            will be converted
	 * @return
	 */
	public static DocumentCollection putDocuments(final ItemCollection doc, final List<String> itemNames) {
		DocumentCollection entiCol = new DocumentCollection();
		List<ItemCollection> col=new ArrayList<ItemCollection>();
		col.add(doc);
		
		Iterator<ItemCollection> it = col.iterator();
		int max = col.size();
		int i = 0;
		XMLItemCollection[] entities = new XMLItemCollection[max];
		while (it.hasNext()) {
			ItemCollection icw = (ItemCollection) it.next();
			XMLItemCollection entity = putItemCollection(icw, itemNames);
			entities[i] = entity;
			i++;
		}
		if (max > 0)
			entiCol.setDocument(entities);
		return entiCol;
	}

	/**
	 * This method transforms single ItemCollection into a DocumentCollection with one element. 
	 * 
	 * @param doc
	 * @return
	 */
	public static DocumentCollection putDocuments(final ItemCollection doc) {
		return putDocuments(doc, null);
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
	 * This method imports an xml entity data stream containing a singel document and returns the
	 * ItemCollection. 
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

		DocumentCollection ecol = null;
		logger.finest("......readCollection importXmlEntityData - verifing  content....");
		JAXBContext context = JAXBContext.newInstance(DocumentCollection.class);
		Unmarshaller m = context.createUnmarshaller();

		ByteArrayInputStream input = new ByteArrayInputStream(byteInput);
		Object jaxbObject = m.unmarshal(input);
		if (jaxbObject == null) {
			throw new RuntimeException("readCollection error - wrong xml file format - unable to read content!");
		}

		ecol = (DocumentCollection) jaxbObject;

		// convert entities....
		if (ecol.getDocument().length > 0) {
			for (XMLItemCollection aentity : ecol.getDocument()) {
				resultList.add(XMLItemCollectionAdapter.getItemCollection(aentity));
			}
			logger.fine("readCollection" + ecol.getDocument().length + " entries sucessfull imported");
		}
		return resultList;

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

		if (byteInput == null) {
			return null;
		}

		XMLItemCollection ecol = null;
		logger.finest("......importXmlEntityData - verifing content....");
		JAXBContext context = JAXBContext.newInstance(XMLItemCollection.class);
		Unmarshaller m = context.createUnmarshaller();

		ByteArrayInputStream input = new ByteArrayInputStream(byteInput);
		Object jaxbObject = m.unmarshal(input);
		if (jaxbObject == null) {
			throw new RuntimeException("readItemCollection error - wrong xml file format - unable to read content!");
		}

		ecol = (XMLItemCollection) jaxbObject;

		// convert entity....
		ItemCollection itemCol = XMLItemCollectionAdapter.getItemCollection(ecol);

		return itemCol;

	}

	
	
	/**
	 * This method writes a ItemCollection into a Byte array
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

		XMLItemCollection ecol = putItemCollection(document);
		StringWriter writer = new StringWriter();
		JAXBContext context = JAXBContext.newInstance(XMLItemCollection.class);
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
