package org.imixs.workflow.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import org.imixs.workflow.ItemCollection;

/**
 * The JSONParser is an utility class to parse JSON structures. The parser
 * provides methods to transfer a Imixs JSON structure into a Imixs
 * ItemCollection as well as helper methods to extract single values from a JSON
 * structure.
 * <p>
 * The method parseWorkitem translates a JSON structure containing a Imixs
 * Document into a ItemCollection.
 * 
 * @author rsoika
 */
public class JSONParser {

	private static Logger logger = Logger.getLogger(JSONParser.class.getName());

	/**
	 * This method extracts a single key from a JSON structure. It does not matter
	 * where the key is defined within the JSON structure. The method simply returns
	 * the first match.
	 * <p>
	 * It is also possible to get a JSON object or an JSON array embedded in the
	 * given JSON structure. This object can be parsed again with this method.
	 * 
	 * @param key
	 * @param json
	 * @return - the json value or the json object for the corresponding json key
	 */
	public static String getKey(String key, String json) {
		if (json==null || json.isEmpty()) {
			return null;
		}
		String result = null;
		// now extract the key
		JsonParser parser = Json.createParser(new StringReader(json));
		// {"key":"b38b84614af36f874ba4f08dd4ea40c4e66e0607"}

		Event event = null;
		while (true) {

			try {
				event = parser.next(); // START_OBJECT
				if (event == null) {
					return null;
				}
				if (event.name().equals(Event.KEY_NAME.toString())) {
					String jsonkey = parser.getString();
					if (key.equals(jsonkey)) {
						event = parser.next(); // value
						if (event.name().equals(Event.VALUE_STRING.toString())) {
							result = parser.getString();
							break;
						}
						if (event.name().equals(Event.VALUE_NUMBER.toString())) {
							result = parser.getBigDecimal()+"";
							break;
						}
						if (event.name().equals(Event.VALUE_TRUE.toString())) {
							result = "true";
							break;
						}
						if (event.name().equals(Event.VALUE_FALSE.toString())) {
							result = "false";
							break;
						}
						if (event.name().equals(Event.VALUE_NULL.toString())) {
							result = null;
							break;
						}
						if (event.name().equals(Event.START_OBJECT.toString())) {
							// just return the next json object here
							result = parser.getObject().toString();
							break;
						}
						if (event.name().equals(Event.START_ARRAY.toString())) {
							// just return the next json object here
							result = parser.getArray().toString();
							break;
						}
					}
				}
			} catch (NoSuchElementException e) {
				return null;
			}
		}
		return result;
	}

	/**
	 * This method parses an Imixs JSON input stream and returns a Imixs
	 * ItemCollection.
	 * 
	 * Example: <code>
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
				{"name":"$activityid","value":{"@type":"xs:int","$":"0"}}
			]
		}
	 * </code>
	 * 
	 * @param requestBodyStream
	 * @param encoding
	 *            - default encoding use to parse the stream
	 * @return a workitem
	 * @throws ParseException
	 * @throws UnsupportedEncodingException
	 */
	public final static ItemCollection parseWorkitem(final InputStream requestBodyStream, final String _encoding)
			throws ParseException, UnsupportedEncodingException {
		boolean debug = logger.isLoggable(Level.FINE);
		String encoding = _encoding;

		if (requestBodyStream == null) {
			logger.severe("parseWorkitem - inputStream is null!");
			throw new ParseException("inputStream is null", -1);
		}

		// default encoding?
		if (encoding == null || encoding.isEmpty()) {
			if (debug) {
			logger.finest("......parseWorkitem - switch to default encoding 'UTF-8'");
			}
			encoding = "UTF-8";
		}

		// Vector<String> vMultiValueFieldNames = new Vector<String>();
		BufferedReader in = new BufferedReader(new InputStreamReader(requestBodyStream, encoding));

		String inputLine;
		ItemCollection workitem = new ItemCollection();

		String content = null;
		String token = null;
		String name = null;
		StringBuffer stringBuffer = new StringBuffer();
		int iPos = -1;
		int iStart = -1;
		int iEnd = -1;
		try {
			// first we concat all lines
			while ((inputLine = in.readLine()) != null) {
				stringBuffer.append(inputLine);
				if (debug) {
					logger.finest("......parseWorkitem - read line:" + inputLine + "");
				}
			}
			content = stringBuffer.toString();

			// find start ...."item":[...
			content = content.substring(content.indexOf('[') + 0);
			if (debug) {
				logger.finest("......parseWorkitem - start parsing...");
			}
			while (content != null) {

				// find name => "name" : "$isauthor" ,
				iPos = content.indexOf(':');
				content = content.substring(iPos);

				token = content.substring(0, content.indexOf(','));
				iStart = token.indexOf('"') + 1;
				iEnd = token.lastIndexOf('"');
				if (iEnd < iStart)
					throw new java.text.ParseException("Unexpected position of '}", iEnd);

				name = token.substring(iStart, iEnd);

				content = content.substring(token.length());
				if (!isValueArray(content)) {
					// now find the value token =>
					// "value":{"@type":"xs:boolean","$":"true"}},
					iStart = findNextChar(content, '{') + 1;
					iEnd = findNextChar(content, '}');
					if (iEnd < iStart)
						throw new java.text.ParseException("Unexpected position of '}", iEnd);
					token = content.substring(iStart, iEnd);
					content = content.substring(iEnd + 1);
					storeValue(name, token, workitem);
				} else {
					// get content of array
					iStart = findNextChar(content, '[') + 1;
					iEnd = findNextChar(content, ']');
					if (iEnd < iStart)
						throw new java.text.ParseException("Unexpected position of '}", iEnd);

					String arrayContent = content.substring(iStart, iEnd);
					content = content.substring(iEnd + 1);
					// parse array values....
					while (arrayContent != null) {
						// now find the value token =>
						// "value":{"@type":"xs:boolean","$":"true"}},
						iStart = findNextChar(arrayContent, '{') + 1;
						iEnd = findNextChar(arrayContent, '}');
						if (iEnd < iStart)
							throw new java.text.ParseException("Unexpected position of '}", iEnd);

						token = arrayContent.substring(iStart, iEnd);
						arrayContent = arrayContent.substring(iEnd + 1);
						storeValue(name, token, workitem);

						if (!arrayContent.contains("{"))
							break;
					}

				}

				if (!content.contains("{"))
					break;

			}
		} catch (IOException e1) {
			// logger.severe("Unable to parse workitem data!");
			e1.printStackTrace();
			return null;
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		return workitem;
	}

	/**
	 * This helper method extracts the type and value of a token and stores the
	 * value into the workitem
	 * 
	 * e.g.
	 * 
	 * {"name":"$isauthor","value":{"@type":"xs:boolean","$":true}},
	 * {"name":"$readaccess","value":{"@type":"xs:string","$":"Anna"}},
	 * {"name":"txtmessage","value":{"@type":"xs:string","$":"worklist"}},
	 * {"name":"$activityid","value":{"@type":"xs:int","$":10}},
	 * {"name":"$processid","value":{"@type":"xs:int","$":100}}
	 * 
	 * @param token
	 * @throws ParseException
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void storeValue(String name, String token, ItemCollection workitem) throws ParseException {
		boolean debug = logger.isLoggable(Level.FINE);
		int iPos, iStart, iEnd;
		Object value;
		String type = null;

		// check if "@type" exists
		iPos = token.indexOf("\"@type\"");
		if (iPos > -1) {
			iStart = token.indexOf('"', iPos + "\"@type\"".length() + 1) + 1;
			iEnd = token.indexOf('"', iStart);
			if (iEnd < iStart)
				throw new ParseException("Unexpected position of '}", iEnd);

			type = token.substring(iStart, iEnd);
			token = token.substring(iEnd + 1);
		}

		// store value - the value can be surrounded by " or not
		iPos = token.indexOf(":") + 1;
		if (token.indexOf('"', iPos) > -1) {
			iStart = token.indexOf('"', iPos) + 1;
			iEnd = token.indexOf('"', iStart);
		} else {
			iStart = iPos;
			iEnd = token.length();
		}
		if (iEnd < iStart)
			throw new ParseException("Unexpected position of '}", iEnd);

		String stringValue = token.substring(iStart, iEnd);
		value = stringValue;

		// convert value to Object Type
		if ("xs:boolean".equalsIgnoreCase(type)) {
			value=	Boolean.parseBoolean(stringValue);
			if (debug) {
				logger.finest("......storeValue - datatype=xs:boolean");
			}
		}
		if ("xs:integer".equalsIgnoreCase(type) || "xs:int".equalsIgnoreCase(type)) {
			value = Integer.parseInt(stringValue);
			if (debug) {
				logger.finest("......storeValue - datatype=xs:integer");
			}
		}
		if ("xs:long".equalsIgnoreCase(type)) {
			value = Long.parseLong(stringValue);
			if (debug) {
				logger.finest("......storeValue - datatype=xs:long");
			}
		}
		if ("xs:float".equalsIgnoreCase(type)) {
			value = new Float(stringValue);
			if (debug) {
				logger.finest("......storeValue - datatype=xs:float");
			}
		}
		if ("xs:double".equalsIgnoreCase(type)) {
			value = new Double(stringValue);
			if (debug) {
				logger.finest("......storeValue - datatype=xs:double");
			}
		}

		// store value
		if (!workitem.hasItem(name)) {
			// frist value
			workitem.replaceItemValue(name, value);
			if (debug) {
				logger.finest("......storeValue: '" + name + "' = '" + value + "'");
			}
		} else {
			// add value
			List valueList = workitem.getItemValue(name);
			valueList.add(value);
			workitem.replaceItemValue(name, valueList);
			if (debug) {
				logger.finest("......store multivalue: '" + name + "' = '" + value + "'");
			}
		}

	}

	/**
	 * Checks if the value is an array of values
	 * 
	 * ,"value":[ {"@type":"xs:string","$":"A"},
	 * 
	 * @param token
	 * @return
	 */
	private static boolean isValueArray(String token) {
		int b1 = findNextChar(token, '[');
		int b2 = findNextChar(token, '{');
		if (b1 > -1 && b1 < b2)
			return true;
		else
			return false;
	}

	/**
	 * This method finds the next position of a char. The method scips excapte
	 * characters like '\"' or '\['
	 * 
	 * @param token
	 * @param c
	 * @return
	 */
	private static int findNextChar(String token, char c) {
		int iPos = token.indexOf(c);

		if (iPos <= 0)
			return iPos;

		// check if the char before is a \
		while ((token.charAt(iPos - 1)) == '\\') {
			iPos = token.indexOf(c, iPos + 2);
			if (iPos == -1)
				break;
		}

		return iPos;

	}

}
