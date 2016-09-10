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
package org.imixs.workflow.jaxrs.v3;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
	 * This Method converts a
	 * <code>org.imixs.workflow.xml.XMLItemCollection</code> into a
	 * <code> org.imixs.workflow.ItemCollection</code> Returns null if entity ==
	 * null
	 * 
	 * @param entity
	 * @return ItemCollection
	 */
	@SuppressWarnings({ "rawtypes" })
	public static ItemCollection getItemCollection(final XMLItemCollection entity) {
		ItemCollection itemCol = new ItemCollection();
		if (entity == null)
			return itemCol;

		try {
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
		} catch (Exception e) {
			logger.warning("[XMLItemCollectionAdapter] getItemCollection - can't convert XMLItem value - error: "
					+ e.toString());
			itemCol = null;
		}

		return itemCol;
	}

	/**
	 * This Method converts a <code> org.imixs.workflow.ItemCollection</code>
	 * into a <code>Entity</code>
	 * 
	 * <p>
	 * The method verifies if the values stored are basic java types. If not
	 * these values will not be converted!
	 * 
	 * @param aItemCollection
	 *            Collection Object to be converted
	 * @param itemNames
	 *            - optional list of item names to be converted. If null all
	 *            items will be converted
	 */
	@SuppressWarnings({ "unchecked" })
	public static XMLItemCollection putItemCollection(final ItemCollection aItemCollection,
			final List<String> itemNames) throws Exception {
		String sName = null;
		XMLItemCollection entity = new XMLItemCollection();
		int i = 0;
		XMLItem[] items = null;
		try {
			if (aItemCollection != null) {
				// test if only a sublist of items should be converted
				if (itemNames != null && itemNames.size() > 0) {
					items = new XMLItem[itemNames.size()];
					for (String aField : itemNames) {
						// this code block guarantees that the order of items
						// returned
						sName = aField;
						XMLItem item = new XMLItem();
						// test the ItemValue
						List<?> vOrg = aItemCollection.getItemValue(aField);
						item.setName(sName);
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
						XMLItem item = null;
						item = new XMLItem();
						item.setName((String) entry.getKey());
						item.setValue(entry.getValue().toArray());
						if (item != null) {
							items[i] = item;
							i++;
						}
					}
				}

				entity.setItem(items);
			}

		} catch (Exception e) {
			System.out.println("[XMLItemCollectionAdapter] Error putItemCollection (" + sName + ")");
			throw e;
		}

		entity=sortItemsByName(entity);
		
		return entity;
	}
	
	
	/**
	 * This method sorts all items of a XMLItemCollection by item name.
	 * @param xmlItemCol
	 * @return
	 */
	public static XMLItemCollection sortItemsByName(XMLItemCollection xmlItemCol) {
		
		XMLItem[] items = xmlItemCol.getItem();
		Arrays.sort(items,new XMLItemComparator());
		
		xmlItemCol.setItem(items);
		
		return xmlItemCol;
	}

	/**
	 * This Method converts a <code> org.imixs.workflow.ItemCollection</code>
	 * into a <code>Entity</code>
	 * 
	 * <p>
	 * The method verifies if the values stored are basic java types. If not
	 * these values will not be converted!
	 * 
	 * @param aItemCollection
	 *            Collection Object to be converted
	 */
	public static XMLItemCollection putItemCollection(final ItemCollection aItemCollection) throws Exception {
		return putItemCollection(aItemCollection, null);
	}

	/**
	 * This method transforms a Collection<ItemCollection> into a
	 * EntityCollection
	 * 
	 * @param col
	 * @return
	 * @throws Exception
	 */
	public static EntityCollection putCollection(final Collection<ItemCollection> col) throws Exception {

		return putCollection(col, null);
	}

	/**
	 * This method transforms a Collection<ItemCollection> into a
	 * EntityCollection
	 * 
	 * If the attribute List is provided only the corresponding properties will
	 * be returned.
	 * 
	 * @param col
	 *            - collection of ItemCollection objects to be converted
	 * @param itemNames
	 *            - optional list of item names to be converted. If null all
	 *            items will be converted
	 * @return
	 * @throws Exception
	 */
	public static EntityCollection putCollection(final Collection<ItemCollection> col, final List<String> itemNames)
			throws Exception {
		EntityCollection entiCol = new EntityCollection();
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
			entiCol.setEntity(entities);
		return entiCol;
	}

	/**
	 * This method imports an xml entity data stream and returns a List of
	 * ItemCollection objects. The method can import any kind of entity data
	 * like model or configuration data an xml export of workitems.
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
	 * ItemCollection objects. The method can import any kind of entity data
	 * like model or configuration data an xml export of workitems.
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

		EntityCollection ecol = null;
		logger.fine("[XMLItemCollectionAdapter] importXmlEntityData - verifing  content....");
		JAXBContext context = JAXBContext.newInstance(EntityCollection.class);
		Unmarshaller m = context.createUnmarshaller();

		ByteArrayInputStream input = new ByteArrayInputStream(byteInput);
		Object jaxbObject = m.unmarshal(input);
		if (jaxbObject == null) {
			throw new RuntimeException(
					"[XMLItemCollectionAdapter] error - wrong xml file format - unable to read content!");
		}

		ecol = (EntityCollection) jaxbObject;

		// convert entities....
		if (ecol.getEntity().length > 0) {
			for (XMLItemCollection aentity : ecol.getEntity()) {
				resultList.add(XMLItemCollectionAdapter.getItemCollection(aentity));
			}
			logger.fine("[XMLItemCollectionAdapter] " + ecol.getEntity().length + " entries sucessfull imported");
		}
		return resultList;

	}

	/**
	 * This method imports a single XMLItemCollection and returns the
	 * ItemCollection object.
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
		logger.fine("[XMLItemCollectionAdapter] importXmlEntityData - verifing content....");
		JAXBContext context = JAXBContext.newInstance(XMLItemCollection.class);
		Unmarshaller m = context.createUnmarshaller();

		ByteArrayInputStream input = new ByteArrayInputStream(byteInput);
		Object jaxbObject = m.unmarshal(input);
		if (jaxbObject == null) {
			throw new RuntimeException(
					"[XMLItemCollectionAdapter] error - wrong xml file format - unable to read content!");
		}

		ecol = (XMLItemCollection) jaxbObject;

		// convert entity....
		ItemCollection itemCol = XMLItemCollectionAdapter.getItemCollection(ecol);

		return itemCol;

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
