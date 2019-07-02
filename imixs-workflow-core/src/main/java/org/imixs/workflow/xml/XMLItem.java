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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Represents a single item inside a XMLItemCollection. An XMLItem has a name
 * and a value. The value can be any Serializable collection of objects.
 * 
 * @author rsoika
 * 
 */
@XmlSeeAlso({ XMLItem[].class }) // important! to support arrays of XMLItem
@XmlRootElement(name = "item")
public class XMLItem implements java.io.Serializable {

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(XMLItem.class.getName());

	private java.lang.String name;

	private java.lang.Object[] value;

	@XmlAttribute
	public java.lang.String getName() {
		return name;
	}

	public void setName(java.lang.String name) {
		this.name = name;
	}

	public java.lang.Object[] getValue() {
		return value;
	}

	/**
	 * This method set the value array of the item. The method verifies if the
	 * values are from basic type, XMLItem or implementing the Map or List
	 * interface.
	 * <p>
	 * Map or List interface will be converted into instantces of XMLItem.
	 * <p>
	 * Null values will be converted into an empty vector.
	 * <p>
	 * In case an value is not convertable the method prints a warning into the log
	 * file.
	 * <p>
	 * issue #52: the method also converts XMLGregorianCalendar into java.util.Date
	 * 
	 * 
	 * @param values
	 *            - array of objects
	 */
	@SuppressWarnings("unchecked")
	public void setValue(Object[] values) {
		// this.value = values;

		if (values == null || values.length == 0) {
			// add empty vector
			Vector<Object> vOrg = new Vector<Object>();
			vOrg.add(null);
			value = vOrg.toArray();
			return;
		}

		// issue #52
		values = convertXMLGregorianCalendar(values);

		// convert values...
		List<Object> listOfObjects = new ArrayList<Object>();
		boolean conversionSuccessfull = true;
		for (Object aSingleObject : values) {
			if (isBasicType(aSingleObject)) {

				if (aSingleObject instanceof String) {
					// issue #502
					// we test the string for NonValidXMLCharacters
					aSingleObject = stripNonValidXMLCharacters((String) aSingleObject);
				}
				// normal type....
				listOfObjects.add(aSingleObject);
			} else {
				// maybe we have a Map?
				if (aSingleObject instanceof Map) {
					@SuppressWarnings("rawtypes")
					XMLItem[] embeddedxmlMap = XMLItem.convertMap((Map) aSingleObject);
					listOfObjects.add(embeddedxmlMap);
				} else {
					// maybe we have a List?
					if (aSingleObject instanceof List) {
						// we create a nameles xmlItem and add the list as the value
						XMLItem xmlVal = new XMLItem();
						// convert into array...
						xmlVal.setValue(((List<?>) aSingleObject).toArray());
						listOfObjects.add(xmlVal);
					} else {
						conversionSuccessfull = false;
						logger.warning("WARNING : XMLItem - property '" + this.name
								+ "' contains unsupported java types: " + aSingleObject.getClass().getName());
						break;
					}
				}
			}
		}
		if (conversionSuccessfull) {
			this.value = listOfObjects.toArray();
		}

	}

	/**
	 * This method ensures that the output String has only valid XML unicode
	 * characters as specified by the XML 1.0 standard. For reference, please see
	 * <a href="http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char">the
	 * standard</a>. This method will return an empty String if the input is null or
	 * empty.
	 *
	 * @param itemValue
	 *            The String whose non-valid characters we want to remove.
	 * @param itemName
	 *            - the item name used just for logging
	 * @return The in String, stripped of non-valid characters.
	 */
	private String stripNonValidXMLCharacters(String itemValue) {
		StringBuffer out = new StringBuffer(); // Used to hold the output.
		char current; // Used to reference the current character.

		if (itemValue == null || ("".equals(itemValue)))
			return ""; // vacancy test.
		for (int i = 0; i < itemValue.length(); i++) {
			current = itemValue.charAt(i); // NOTE: No IndexOutOfBoundsException caught here; it should not happen.
			if ((current == 0x9) || (current == 0xA) || (current == 0xD) || ((current >= 0x20) && (current <= 0xD7FF))
					|| ((current >= 0xE000) && (current <= 0xFFFD))
					|| ((current >= 0x10000) && (current <= 0x10FFFF))) {
				out.append(current);
			} else {
				logger.warning("invalid xml character at position " + i + " in item '" + name + "'");
			}
		}
		return out.toString();
	}

	/**
	 * This method returns a transformed version of the XMLItem value array.
	 * <p>
	 * In case an object value is an instance of a XMLItem, the method converts the
	 * object into the corresponding Map or List interface.
	 * <p>
	 * 
	 * @see XMLDocumentAdapter#putDocument(XMLDocument)
	 * @return
	 */
	public java.lang.Object[] transformValue() {

		if (value == null) {
			return null;
		}

		Object[] result = new Object[value.length];
		int j = 0;
		for (Object aSingleObject : value) {

			if (aSingleObject instanceof XMLItem) {
				XMLItem embeddedXMLItem = (XMLItem) aSingleObject;
				Object[] embeddedValueArray = embeddedXMLItem.transformValue();
				// convert to list...
				result[j] = Arrays.asList(embeddedValueArray);
			} else {

				// is value object of type XMLItem >> Map
				if (aSingleObject instanceof XMLItem[]) {
					XMLItem[] innerlist = (XMLItem[]) aSingleObject;

					// create map
					HashMap<String, List<Object>> map = new HashMap<String, List<Object>>();
					for (XMLItem x : innerlist) {
						map.put(x.getName(), Arrays.asList(x.transformValue()));
					}
					// return resultMap;

					// Map<String, List<Object>> map = XMLItem.convertXMLItemArray(innerlist);
					result[j] = map;
				}

				else {
					// raw type...
					result[j] = aSingleObject;
				}
			}

			j++;

		}

		return result;
	}

	/**
	 * This method compares the item name and value array
	 */
	public boolean equals(Object o) {
		if (!(o instanceof XMLItem))
			return false;
		XMLItem _xmlItem = (XMLItem) o;
		return (name != null && name.equals(_xmlItem.name) && value != null && Arrays.equals(value, _xmlItem.value));
	}

	/**
	 * Converts a Map interface into a Array of XMLItem objects
	 * 
	 * @return
	 */
	private static XMLItem[] convertMap(Map<String, Object> map) {
		Set<Entry<String, Object>> entrySet = map.entrySet();
		XMLItem[] result = new XMLItem[entrySet.size()];
		int i = 0;
		for (Entry<String, Object> mapentry : entrySet) {
			XMLItem singleXMLItem = new XMLItem();
			singleXMLItem.setName(mapentry.getKey());
			if (mapentry.getValue() instanceof List) {
				singleXMLItem.setValue(((List<?>) mapentry.getValue()).toArray());
			} else {
				// create single list entry
				ArrayList<String> aList = new ArrayList<String>();
				aList.add(mapentry.getValue().toString());
				singleXMLItem.setValue(aList.toArray());
			}
			result[i] = singleXMLItem;
			i++;
		}

		return result;
	}

	/**
	 * This helper method test if a value if of a basic java type including check
	 * for raw arrays, java.lang.*, java.math.*
	 * 
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private static boolean isBasicType(java.lang.Object o) {

		if (o == null) {
			return true;
		}

		// test raw types first
		if (o instanceof byte[] || o instanceof String[] || o instanceof boolean[] || o instanceof short[]
				|| o instanceof char[] || o instanceof int[] || o instanceof long[] || o instanceof float[]
				|| o instanceof double[] || o instanceof Long[] || o instanceof Integer[] || o instanceof Double[]
				|| o instanceof Float[] || o instanceof Short[] || o instanceof XMLItem[]) {
			return true;
		}

		// text mixed object arrays...
		if (o instanceof Object[]) {
			Object[] objects = (Object[]) o;
			for (Object oneObject : objects) {
				if (!isBasicType(oneObject)) {
					return false;
				}
			}
			// all elements are supported
			return true;
		}

		// finaly test package name
		Class c = o.getClass();
		String name = c.getName();
		if (name.startsWith("java.lang.") || name.startsWith("java.math.") || "java.util.Date".equals(name)
				|| "org.imixs.workflow.xml.XMLItem".equals(name)) {
			return true;
		}

		// unsupported object type
		return false;
	}

	/**
	 * This helper method converts instances of XMLGregorianCalendar into
	 * java.util.Date objects.
	 * 
	 * See issue #52
	 *
	 */
	private static Object[] convertXMLGregorianCalendar(final Object[] objectArray) {
		// test the content for GregorianCalendar... (issue #52)
		for (int j = 0; j < objectArray.length; j++) {
			if (objectArray[j] instanceof XMLGregorianCalendar) {
				XMLGregorianCalendar xmlCal = (XMLGregorianCalendar) objectArray[j];
				// convert into Date object
				objectArray[j] = xmlCal.toGregorianCalendar().getTime();
			}
		}
		return objectArray;
	}

}
