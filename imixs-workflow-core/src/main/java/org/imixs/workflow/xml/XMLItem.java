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
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

import javax.xml.bind.annotation.XmlRootElement;

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
	 * This method set the value list of the item. The method verifies if the
	 * values are from basic type or implementing a Map interface. Otherwise the
	 * method prints a warning into the log file.
	 * 
	 * Null values will be converted into an empty vector.
	 * 
	 * @param values
	 */
	public void setValue(java.lang.Object[] values) {
		// this.value = values;

		if (values == null || values.length == 0) {
			// add empty vector
			Vector<Object> vOrg = new Vector<Object>();
			vOrg.add(null);
			value = vOrg.toArray();
			return;
		}

		// convert basic types into array
		if (isBasicType(values)) {
			this.value = values;
		} else {
			// now test if the values are from type Map?
			logger.fine("[XMLItem] test values for map intefaces");
			for (Object singleValue : values) {
				if (singleValue instanceof Map) {
					// map - convert to XMLItem
					List<XMLItem> mapData = new ArrayList<XMLItem>();
					@SuppressWarnings("unchecked")
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
					this.value = mapData.toArray();

				} else {
					// unable to convert object!
					logger.warning("WARNING : XMLItem - unsupported java type = " + singleValue.getClass().getName());

				}
			}
		}

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
			// test raw array types first
			if (o instanceof byte[] || o instanceof boolean[] || o instanceof short[] || o instanceof char[]
					|| o instanceof int[] || o instanceof long[] || o instanceof float[] || o instanceof double[]) {
				continue;
			}

			// test package name
			Class c = o.getClass();
			String name = c.getName();
			if (!name.startsWith("java.lang.") && !name.startsWith("java.math.") && !"java.util.Date".equals(name)) {
				return false;
			}
		}
		return true;
	}

}
