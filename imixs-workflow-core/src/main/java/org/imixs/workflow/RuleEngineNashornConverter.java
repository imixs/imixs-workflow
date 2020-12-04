/*  
 *  Imixs-Workflow 
 *  
 *  Copyright (C) 2001-2020 Imixs Software Solutions GmbH,  
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
 *      https://www.imixs.org
 *      https://github.com/imixs/imixs-workflow
 *  
 *  Contributors:  
 *      Imixs Software Solutions GmbH - Project Management
 *      Ralph Soika - Software Developer
 */

package org.imixs.workflow;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.imixs.workflow.exceptions.PluginException;

/**
 * This is a helper class to convert ItemCollection objects into map objects
 * used by the deprecated scripts (nashorn). The converter is called by the
 * RuleEngine only in case a deprecated script need to be evaluated.
 * 
 * @author Ralph Soika
 * @version 1.0
 * 
 */
public class RuleEngineNashornConverter {
	   private static final HashSet<Class<?>> BASIC_OBJECT_TYPES = getBasicObjectTypes();

	    private static Logger logger = Logger.getLogger(RuleEngineNashornConverter.class.getName());

	/**
	 * This method converts the values of an ItemCollection into a Map Object with
	 * Arrays of Objects for each value
	 * 
	 * @param itemCol
	 * @return
	 */
	public static Map<String, Object[]> convertItemCollection(ItemCollection itemCol) {
		Map<String, Object[]> result = new HashMap<String, Object[]>();
		Map<String, List<Object>> itemList = itemCol.getAllItems();
		for (Map.Entry<String, List<Object>> entry : itemList.entrySet()) {
			String key = entry.getKey().toLowerCase();
			List<?> value = (List<?>) entry.getValue();
			// do only put basic values
			if (value.size() > 0) {
				if (isBasicObjectType(value.get(0).getClass())) {
					result.put(key, value.toArray());
				}

			}
		}
		return result;
	}

	/**
	 * This method converts a JSON variable by name into a ItemCollection. The
	 * variable is expected as a JSON object holding single values or arrays in the
	 * following format
	 * 
	 * <code>
	 * 
	 * {'single_item':'Hello World', 'multi_item':[ 'Hello World', 'Hello Imixs' ]
	 * };
	 * 
	 * <code>
	 * 
	 * The converted object is expected as an Map interface.
	 * 
	 * @param engine
	 * @return ItemCollection holding the item values of the variable or null if no
	 *         variable with the given name exists or the variable has not
	 *         properties.
	 * @throws ScriptException
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static ItemCollection convertScriptVariableToItemCollection(Value bindings , String variable) {
		ItemCollection result = null;
		boolean debug = logger.isLoggable(Level.FINE);
		// get result object from engine
		Value dingens = bindings.getMember(variable);
		
		Map<String, Object> scriptResult = (Map) bindings.getMember(variable).as(Map.class);
		// test if the json object exists and has child objects...
		if (scriptResult != null) {
			result = new ItemCollection();
			// evaluate values if available...
			if (scriptResult.entrySet().size() > 0) {
				// iterate over all entries
				for (Map.Entry<String, Object> entry : scriptResult.entrySet()) {

					// test if the entry value is a single object or an array....
					if (isBasicObjectType(entry.getValue().getClass())) {
						// single value - build array....
						if (debug) {
							logger.finest("......adding " + variable + " property " + entry.getKey());
						}
						List<Object> list = new ArrayList();
						list.add(entry.getValue());
						result.replaceItemValue(entry.getKey(), list);
					} else {
						// test if array...
						String expression = "result['" + entry.getKey() + "']";
						Object[] oScript = null;// evaluateNativeScriptArray(expression);
						logger.warning("eval script array not yet implemented!");
						if (oScript == null) {
							continue;
						}
						if (debug) {
							logger.finest("......adding " + variable + " property " + entry.getKey());
						}
						List<?> list = new ArrayList(Arrays.asList(oScript));
						result.replaceItemValue(entry.getKey(), list);
					}
				}
			}
		}
		return result;
	}

	
	  private static boolean isBasicObjectType(Class<?> clazz) {
		return BASIC_OBJECT_TYPES.contains(clazz);
	}

	private static HashSet<Class<?>> getBasicObjectTypes() {
		HashSet<Class<?>> ret = new HashSet<Class<?>>();
		ret.add(Boolean.class);
		ret.add(Character.class);
		ret.add(Byte.class);
		ret.add(Short.class);
		ret.add(Integer.class);
		ret.add(Long.class);
		ret.add(Float.class);
		ret.add(Double.class);
		ret.add(Void.class);

		ret.add(BigDecimal.class);
		ret.add(BigInteger.class);

		ret.add(String.class);
		ret.add(Object.class);
		ret.add(Date.class);
		ret.add(Calendar.class);
		ret.add(GregorianCalendar.class);

		return ret;
	}

}
