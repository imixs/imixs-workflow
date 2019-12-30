package org.imixs.workflow.util;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import javax.json.Json;
import javax.json.stream.JsonLocation;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
import org.imixs.workflow.ItemCollection;

/**
 * The ImixsJSONParser is an utility class to parse JSON structures of Imixs Documents. The parser
 * supports single document structures as also collections of documents (data element).
 * <p>
 * The method 'parse()' returns in any case a collection of ItemCollection.
 * <p>
 * 
 * @author rsoika
 */
public class ImixsJSONParser {

  public static final String DATA_ELEMENT = "data";
  public static final String ITEM_ELEMENT = "item";
  public static final String NAME_ELEMENT = "name";
  public static final String VALUE_ELEMENT = "value";

  /**
   * This method parses an Imixs JSON input stream and returns a List of Imixs ItemCollection
   * instances.
   * <p>
   * The method supports both - a single document (item element) or a collection of documents (data
   * element). In both cases the method returns a collection of ItemCollections
   * 
   * Example-1: <code>
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
   * 
   * Example-2: <code>
   * {
   *  "data": [
   *  {
  	"item":[
  			{"name":"$isauthor","value":{"@type":"xs:boolean","$":"true"}},
  			{"name":"$readaccess","value":{"@type":"xs:string","$":"Anna"}}
  		]
  	},
  	{
  	"item":[
  			{"name":"$isauthor","value":{"@type":"xs:boolean","$":"true"}},
  			{"name":"$readaccess","value":{"@type":"xs:string","$":"Anna"}}
  		]
  	}
  	]
     }
   * </code>
   * 
   * 
   * @param requestBodyStream
   * @param encoding          - default encoding use to parse the stream
   * @return a workitem
   * @throws ParseException
   * @throws UnsupportedEncodingException
   */
  public final static List<ItemCollection> parse(final InputStream jsonDataStream)
      throws ParseException, UnsupportedEncodingException {
    boolean isarray = false;
    List<ItemCollection> result = null;

    if (jsonDataStream == null) {
      return null;
    }

    JsonParser parser = Json.createParser(jsonDataStream);
    Event event = null;
    while (true) {

      try {
        event = parser.next(); // START_OBJECT
        if (event == null) {
          return null;
        }

        if (event.name().equals(Event.START_ARRAY.toString())) {
          isarray = true;
          continue;
        }

        if (event.name().equals(Event.KEY_NAME.toString())) {
          String jsonkey = parser.getString();
          // data element?
          if (DATA_ELEMENT.equals(jsonkey)) {
            if (result != null) {
              // we do not expect a second data element!
              JsonLocation location = parser.getLocation();
              throw new ParseException(
                  "Invalid JSON Data Structure - element 'data' not expected (line: "
                      + location.getLineNumber() + " column: " + location.getColumnNumber() + ")",
                  (int) location.getStreamOffset());
            } else {
              // create result List
              result = new ArrayList<ItemCollection>();
            }
          }

          // item element?
          if (ITEM_ELEMENT.equals(jsonkey)) {
            ItemCollection document = new ItemCollection();
            parseDocument(parser, document);
            // late init of collection?
            if (result == null) {
              result = new ArrayList<ItemCollection>();
            }
            result.add(document);
          }
        }

        if (isarray && event.name().equals(Event.END_ARRAY.toString())) {
          break;
        }
        if (!isarray && event.name().equals(Event.END_OBJECT.toString())) {
          break;
        }

      } catch (NoSuchElementException e) {
        return null;
      }
    }

    return result;
  }

  /**
   * parsing an item array (document) fragment: <code>
  	"item":[
  			{"name":"$isauthor","value":{"@type":"xs:boolean","$":"true"}},
  			{"name":"$readaccess","value":{"@type":"xs:string","$":"Anna"}}
  		]
   * </code>
   **/
  private static void parseDocument(JsonParser parser, ItemCollection document) {
    while (true) {
      try {
        Event event = parser.next(); // START_OBJECT
        if (event.name().equals(Event.START_OBJECT.toString())) {
          parseItem(parser, document);
        }
        // if end of object or array we can break here
        if (event.name().equals(Event.END_OBJECT.toString())
            || event.name().equals(Event.END_ARRAY.toString())) {
          break;
        }
      } catch (NoSuchElementException e) {
        break;
      }
    }
  }

  /**
   * parsing an item object fragment: <code>
  			{"name":"$isauthor","value":{"@type":"xs:boolean","$":"true"}},
   * </code>
   **/
  private static void parseItem(JsonParser parser, ItemCollection document) {
    Object itemValue = null;
    String itemName = null;
    boolean isItem = true;
    while (isItem) {
      try {
        Event event = parser.next(); // START_OBJECT
        if (event.name().equals(Event.KEY_NAME.toString())) {
          String jsonkey = parser.getString();
          // data element?
          if (NAME_ELEMENT.equals(jsonkey)) {
            parser.next();
            itemName = parser.getString();
            continue;
          }
          if (VALUE_ELEMENT.equals(jsonkey)) {
            // parser.next();
            itemValue = parseValue(parser, document);
            continue;
          }
        }
        // END of Object?
        if (event.name().equals(Event.END_OBJECT.toString())) {
          isItem = false;
          // add item...?
          if (itemName != null && itemValue != null) {
            document.setItemValue(itemName, itemValue);
          }
          break;
        }
      } catch (NoSuchElementException e) {
        break;
      }
    }
  }

  /**
   * parsing an item value fragment: <code>
  			"value":{"@type":"xs:boolean","$":"true"},
   * </code>
   **/
  private static List<Object> parseValue(JsonParser parser, ItemCollection document) {
    String type = null;
    String stringValue = null;
    Object value = null;
    Boolean isarray = false;
    List<Object> valueList = new ArrayList<Object>();
    while (true) {
      try {
        Event event = parser.next(); // START_OBJECT
        if (event.name().equals(Event.START_ARRAY.toString())) {
          isarray = true;
          continue;
        }

        if (event.name().equals(Event.KEY_NAME.toString())) {
          String jsonkey = parser.getString();
          // data element?
          if ("@type".equals(jsonkey)) {
            parser.next();
            type = parser.getString();
            continue;
          }
          if ("$".equals(jsonkey)) {
            parser.next();
            stringValue = parser.getString();
            continue;
          }
        }

        // END of Object?
        if (event.name().equals(Event.END_OBJECT.toString())) {

          // convert value to Object Type
          if ("xs:boolean".equalsIgnoreCase(type)) {
            value = Boolean.parseBoolean(stringValue);
          }
          if ("xs:integer".equalsIgnoreCase(type) || "xs:int".equalsIgnoreCase(type)) {
            value = Integer.parseInt(stringValue);
          }
          if ("xs:long".equalsIgnoreCase(type)) {
            value = Long.parseLong(stringValue);
          }
          if ("xs:float".equalsIgnoreCase(type)) {
            value = new Float(stringValue);
          }
          if ("xs:double".equalsIgnoreCase(type)) {
            value = new Double(stringValue);
          }
          // default to string
          if (value == null) {
            value = stringValue;
          }

          valueList.add(value);
          value = null;
          if (!isarray) {
            return valueList;
          }
          // return value;
        }

        if (event.name().equals(Event.END_ARRAY.toString())) {
          return valueList;
        }

      } catch (NoSuchElementException e) {
        break;
      }
    }
    return null;
  }

}
