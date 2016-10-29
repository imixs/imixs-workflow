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
import java.util.Vector;
import java.util.logging.Logger;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Represents a single item inside a XMLItemCollection. An XMLItem has a name
 * and a value. The value can be any Serializable collection of objects.
 * 
 * @author rsoika
 * 
 */
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
		return value;
	}

	/**
	 * This method returns the value. In case forceConversion=true and the value is
	 * an array of XMLItem elements, the method converts the elements into a Map
	 * or List interface.
	 * 
	 * @see XMLItemCollectionAdapter.getItemCollection()
	 * @return
	 */
	public java.lang.Object[] getValue(boolean forceConversion) {

		if (forceConversion && isArrayOfXMLItem(value)) {
			ArrayList<Object> convertedValue = new ArrayList<Object>();
			for (Object object : value) {
				XMLItem singleValue = (XMLItem) object;
				String key = singleValue.getName();
				if (key != null && !key.isEmpty()) {
					// create map
					HashMap<String, List<Object>> map = new HashMap<String, List<Object>>();
					// convert object array into list
				//	List<Object> lili=Arrays.asList(singleValue.getValue());
					
					map.put(key, Arrays.asList(singleValue.getValue()));
					convertedValue.add(map);
				} else {
					//convertedValue.add(singleValue.getValue());
					convertedValue.add(Arrays.asList(singleValue.getValue()));
				}
			}
			return convertedValue.toArray();
		} else {
			return value;
		}
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
			logger.fine("[XMLItem] test values for map intefaces");
			if (isListOfMap(values)) {
				// map - convert to XMLItem
				List<XMLItem> mapData = new ArrayList<XMLItem>();
				for (Object singleValue : values) {

					Map<String, Object> map = (Map<String, Object>) singleValue;

					for (Map.Entry<String, Object> mapentry : map.entrySet()) {
						String key = mapentry.getKey();
						Object value = mapentry.getValue();
						XMLItem mapValueItem = new XMLItem();
						mapValueItem.setName(key);
						if (value instanceof List) {
							mapValueItem.setValue(((List<?>) value).toArray());
						} else {
							// create single list entry
							ArrayList<String> simpleValueList = new ArrayList<String>();
							simpleValueList.add(value.toString());
							mapValueItem.setValue(simpleValueList.toArray());
						}
						mapData.add(mapValueItem);
					}

				}
				this.value = mapData.toArray();
				// customValue.add(mapData.toArray());

			} else

			if (isListOfList(values)) {
				logger.fine("[XMLItem] convert List intefaces into list of XMLItem elements");
				List<XMLItem> mapData = new ArrayList<XMLItem>();
				for (Object singleValue : values) {
					XMLItem mapValueItem = new XMLItem();
					mapValueItem.setValue(((List<Object>) singleValue).toArray());
					mapData.add(mapValueItem);
				}
				this.value = mapData.toArray();

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

	/**
	 * This method compares the item name and value array
	 */
	public boolean equals(Object o) {
		if (!(o instanceof XMLItem))
			return false;
		XMLItem _xmlItem=(XMLItem)o;
		return  (name!=null && name.equals(_xmlItem.name)
			&& value!=null && Arrays.equals(value,_xmlItem.value));
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
			
			if (o==null) {
				continue;
			}
			// test raw array types first
			if (o instanceof byte[] || o instanceof boolean[] || o instanceof short[] || o instanceof char[]
					|| o instanceof int[] || o instanceof long[] || o instanceof float[] || o instanceof double[]) {
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
