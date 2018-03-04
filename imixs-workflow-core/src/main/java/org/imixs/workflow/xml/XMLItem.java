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

	/**
	 * This method returns the value.
	 * 
	 * @return
	 */
	public java.lang.Object[] getValue() {
		return getValue(false);
		// return value;
	}

	/**
	 * This method returns the value. In case forceConversion=true and the value
	 * is an array of XMLItem elements, the method converts the elements into a
	 * Map or List interface.
	 * 
	 * @see XMLItemCollectionAdapter.getItemCollection()
	 * @return
	 */
	public java.lang.Object[] getValue(boolean forceConversion) {

		// check array of map
		if (forceConversion && isArrayOfXMLItemArray(value)) {
			ArrayList<Map<String, List<Object>>> convertedValue = new ArrayList<Map<String, List<Object>>>();
			for (Object object : value) {
				XMLItem[] innerlist = (XMLItem[]) object;
				Map<String, List<Object>> map = XMLItem.convertXMLItemArray(innerlist);
				convertedValue.add(map);
			}
			return convertedValue.toArray();
		}

		// check array of list
		if (forceConversion && isArrayOfXMLItem(value)) {
			ArrayList<List<Object>> convertedValue = new ArrayList<List<Object>>();
			for (Object object : value) {
				convertedValue.add(convertXMLItemValues((XMLItem) object));
			}
			return convertedValue.toArray();
		}

		// simple value
		return value;

	}

	/**
	 * This method set the value list of the item. The method verifies if the
	 * values are from basic type or implementing a Map or List interface.
	 * Otherwise the method prints a warning into the log file.
	 * 
	 * Null values will be converted into an empty vector.
	 * 
	 * issue #52:
	 * 
	 * We convert XMLGregorianCalendar into java.util.Date objects
	 * 
	 * 
	 * @param values
	 */
	@SuppressWarnings("unchecked")
	public void setValue(java.lang.Object[] values) {
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

		// convert basic types into array
		if (isBasicType(values)) {
			this.value = values;
		} else {

			// now test if the values are from type Map?
			logger.finest("......test values for map intefaces");
			if (isListOfMap(values)) {
				// map - convert to XMLItem
				List<XMLItem[]> listOfXMLItems = new ArrayList<XMLItem[]>();
				for (Object singleMapEntry : values) {
					Map<String, Object> map = (Map<String, Object>) singleMapEntry;
					XMLItem[] xmlVal = XMLItem.convertMap(map);
					listOfXMLItems.add(xmlVal);
				}
				this.value = listOfXMLItems.toArray();

			} else {
				if (isListOfList(values)) {
					// list - convert to XMLItem elements
					XMLItem[] result = new XMLItem[values.length];
					int j = 0;
					for (Object aSingleValueList : values) {
						XMLItem xmlVal = new XMLItem();
						List<?> aList = (List<?>) aSingleValueList;
						xmlVal.setValue(aList.toArray());
						result[j] = xmlVal;
						j++;
					}
					this.value = result;

				} else {
					// unable to convert object!
					String classNames = "";
					for (Object singleValue : values) {
						classNames = classNames + singleValue.getClass().getName() + "; ";
					}
					logger.warning("WARNING : XMLItem - property '" + this.name + "' contains unsupported java types: "
							+ classNames);
				}
			}

		}
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
	 * Converts an Array of XMLItem objects into a Map interface
	 * 
	 * @param xmlItems
	 * @return
	 */
	private static Map<String, List<Object>> convertXMLItemArray(XMLItem[] xmlItems) {
		// create map
		HashMap<String, List<Object>> resultMap = new HashMap<String, List<Object>>();
		for (XMLItem x : xmlItems) {
			resultMap.put(x.getName(), Arrays.asList(x.getValue()));
		}
		return resultMap;
	}

	/**
	 * Converts the values of a XMLItem objects into a List interface
	 * 
	 * @param xmlItems
	 * @return
	 */
	private static List<Object> convertXMLItemValues(XMLItem axmlItem) {
		List<Object> resultList = new ArrayList<Object>();
		for (Object o : axmlItem.getValue()) {
			resultList.add(o);
		}
		return resultList;

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
	 * returns true if all elements of values are from type Map
	 * 
	 * @param values
	 * @return
	 */
	private boolean isListOfMap(java.lang.Object[] values) {
		for (Object singleValue : values) {
			if (!(singleValue instanceof Map)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * returns true if all elements of values are from type List
	 * 
	 * @param values
	 * @return
	 */
	private boolean isListOfList(java.lang.Object[] values) {
		for (Object singleValue : values) {
			if (!(singleValue instanceof List)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * returns true if all elements of values are an array from type XMLItem
	 * arrays
	 * 
	 * @param values
	 * @return
	 */
	private boolean isArrayOfXMLItemArray(java.lang.Object[] values) {
		for (Object singleValue : values) {
			if (!(singleValue instanceof Object[])) {
				return false;
			} else {
				// verfiy embedded xmlItem array...
				Object[] embededList = (Object[]) singleValue;
				for (Object embeddedValue : embededList) {
					if (!(embeddedValue instanceof XMLItem)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * returns true if all elements of values are from type XMLItem
	 * 
	 * @param values
	 * @return
	 */
	private boolean isArrayOfXMLItem(java.lang.Object[] values) {
		for (Object singleValue : values) {
			if (!(singleValue instanceof XMLItem)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * This helper method test if the values of a List are basic types which can
	 * be converted into a XML element
	 * 
	 * check for raw arrays, java.lang.*, java.math.*
	 * 
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private static boolean isBasicType(java.lang.Object[] v) {
		for (Object o : v) {

			if (o == null) {
				continue;
			}
			// test raw array types first
			if (o instanceof byte[] || o instanceof boolean[] || o instanceof short[] || o instanceof char[]
					|| o instanceof int[] || o instanceof long[] || o instanceof float[] || o instanceof double[]
				    || o instanceof XMLItem[]		
					) {
				continue;
			}

			// test package name
			Class c = o.getClass();
			String name = c.getName();
			if (!name.startsWith("java.lang.") && !name.startsWith("java.math.") && !"java.util.Date".equals(name)
					&& !"org.imixs.workflow.xml.XMLItem".equals(name)) {
				return false;
			}
		}
		return true;
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
