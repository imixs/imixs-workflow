package org.imixs.workflow.util;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.imixs.workflow.ItemCollection;

/**
 * The ImixsJSONBuilder is an utility class to generate a typed json string from an
 * Imixs ItemCollection.
 * <p>
 * The result can be converted back into a ItemCollection by using the
 * ImixsJSONParser class.
 * 
 * @See ImixsJSONParser
 * @author rsoika
 */
public class ImixsJSONBuilder {

	public static final String ISO8601DATEFORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

	/**
	 * This method builds a typed JSON output stream from a Imixs ItemCollection.
	 * 
	 * Example Output: <code>
	 *  {
		"item":[
				{"name":"$isauthor","value":{"@type":"xs:boolean","$":"true"}},
				{"name":"$readaccess","value":{"@type":"xs:string","$":"Anna"}},
				{"name":"txtmessage","value":{"@type":"xs:string","$":"worklist"}},
				{"name":"txtlog","value":[
					{"@type":"xs:string","$":"A"},
					{"@type":"xs:string","$":"B"},
					{"@type":"xs:string","$":"C"}]
				},
				{"name":"$eventid","value":{"@type":"xs:int","$":"0"}}
			]
		}
	 * </code>
	 * 
	 * @param worktiem
	 *            - ItemCollection to be translated into JSON
	 * @return a JSON string
	 * @throws ParseException
	 * @throws UnsupportedEncodingException
	 */
	@SuppressWarnings("unchecked")
	public final static String build(final ItemCollection workitem)
			throws ParseException, UnsupportedEncodingException {

		StringBuffer out = new StringBuffer();
		out.append("{\"item\":[");

		// iterate over all items...
		List<String> itemNames = workitem.getItemNames();

		for (int i = 0; i < itemNames.size(); i++) {

			String itemName = itemNames.get(i);
			out.append("{\"name\":\"" + itemName + "\",\"value\":");
			List<Object> values = workitem.getItemValue(itemName);
			buildValues(values, out);

			// add comma?
			if (i < (itemNames.size() - 1)) {
				out.append(",");
			}
		}
		out.append("]}");

		return out.toString();
	}

	/**
	 * This helper method converts a value list into a json string
	 * <p>
	 * In case values contains more than one item the values are ordered into a json
	 * array.
	 * <p>
	 * e.g.
	 * <p>
	 * <code>
	 * {"name":"$isauthor","value":{"@type":"xs:boolean","$":true}},
	 * {"name":"$readaccess","value":{"@type":"xs:string","$":"Anna"}},
	 * {"name":"txtmessage","value":{"@type":"xs:string","$":"worklist"}},
	 * {"name":"$activityid","value":{"@type":"xs:int","$":10}},
	 * {"name":"$processid","value":{"@type":"xs:int","$":100}}
	 *  </code>
	 * 
	 * @param token
	 * @throws ParseException
	 */
	private static void buildValues(List<Object> values, StringBuffer out) {
		if (values == null || values.size() == 0) {
			out.append("{}");
			return;
		}
		if (values.size() > 1) {
			out.append("[");
		}

		// print each output...
		for (int i = 0; i < values.size(); i++) {
			out.append("{");
			// {"@type":"xs:string","$":"worklist"}
			Object valueObject = values.get(i);
			String type = "";
			// test raw types first
			if (valueObject instanceof String) {
				type = "\"@type\":\"xs:string\"";
			}
			if (valueObject instanceof Boolean) {
				type = "\"@type\":\"xs:boolean\"";
			}
			if (valueObject instanceof Short) {
				type = "\"@type\":\"xs:short\"";
			}
			if (valueObject instanceof Integer) {
				type = "\"@type\":\"xs:int\"";
			}
			if (valueObject instanceof Long) {
				type = "\"@type\":\"xs:long\"";
			}
			if (valueObject instanceof Float) {
				type = "\"@type\":\"xs:floatt\"";
			}
			if (valueObject instanceof Double) {
				type = "\"@type\":\"xs:double\"";
			}
			if (valueObject instanceof Date || valueObject instanceof Calendar) {
				type = "\"@type\":\"xs:dateTime\"";
			}
			if (valueObject instanceof BigInteger) {
				type = "\"@type\":\"xs:integer\"";
			}
			if (valueObject instanceof BigDecimal) {
				type = "\"@type\":\"xs:decimal\"";
			}

			out.append(type+",");

			// print the value...
			// "$":"worklist"}
			String valueString = null;
			if (valueObject instanceof Date || valueObject instanceof Calendar) {
				// convert 2013-10-07T22:18:55.476+02:00
				Date date = null;
				if (valueObject instanceof Calendar) {
					date = ((Calendar) valueObject).getTime();
				} else {
					date = (Date) valueObject;
				}
				SimpleDateFormat sdf = new SimpleDateFormat(ISO8601DATEFORMAT);
				// sdf.setTimeZone(TimeZone.getTimeZone("CET"));
				valueString = sdf.format(date);
			} else {
				// simple convert to string
				valueString = valueObject.toString();
			}

			out.append("\"$\":\"" + valueString + "\"");

			out.append("}");
			// add comma?
			if ((i) < (values.size() - 1)) {
				out.append(",");
			}
		}

		if (values.size() > 1) {
			out.append("]");

		}

		out.append("}");
	}

}
