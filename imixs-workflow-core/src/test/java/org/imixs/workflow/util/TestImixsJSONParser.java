package org.imixs.workflow.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.List;

import org.imixs.workflow.ItemCollection;
import org.junit.jupiter.api.Test;

/**
 * Test class test the parsing of a Imixs JSON file used by the
 * workflowRestService method
 * postWorkitemJSON(InputStream requestBodyStream)
 * 
 * 
 * @author rsoika
 */
public class TestImixsJSONParser {

  @Test
  public void testSimple() throws ParseException {

    InputStream inputStream = getClass().getResourceAsStream("/json/simple.json");

    List<ItemCollection> result = null;
    try {
      result = ImixsJSONParser.parse(inputStream);
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      fail();
    }

    assertNotNull(result);
    assertEquals(1, result.size());
    ItemCollection itemCol = result.get(0);

    assertEquals("Anna", itemCol.getItemValueString("$readaccess"));

    List<?> list = itemCol.getItemValue("txtLog");
    assertEquals(3, list.size());

    assertEquals("C", list.get(2));

    assertEquals(10, itemCol.getItemValueInteger("$ActivityID"));
  }

  @Test
  public void testMultiDocuments() throws ParseException {

    InputStream inputStream = getClass().getResourceAsStream("/json/multidocuments.json");

    List<ItemCollection> result = null;
    try {
      result = ImixsJSONParser.parse(inputStream);
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      fail();
    }

    assertNotNull(result);
    assertEquals(2, result.size());

    ItemCollection itemCol = result.get(0);
    assertEquals("Anna", itemCol.getItemValueString("$readaccess"));
    List<?> list = itemCol.getItemValue("txtLog");
    assertEquals(3, list.size());
    assertEquals("C", list.get(2));
    assertEquals(10, itemCol.getItemValueInteger("$ActivityID"));

    itemCol = result.get(1);
    assertEquals("Tom", itemCol.getItemValueString("$readaccess"));
    list = itemCol.getItemValue("txtLog");
    assertEquals(3, list.size());
    assertEquals("F", list.get(2));
    assertEquals(20, itemCol.getItemValueInteger("$ActivityID"));

  }

  /**
   * test parsing of json number fields
   * 
   * e.g. {"name":"$processid", "value":{"@type":"xs:int","$":1100}},
   * 
   * 
   * @throws ParseException
   */
  @Test
  public void testSimpleNumbers() throws ParseException {

    InputStream inputStream = getClass().getResourceAsStream("/json/simple_numbers.json");

    List<ItemCollection> result = null;
    try {
      result = ImixsJSONParser.parse(inputStream);
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      fail();
    }
    assertNotNull(result);
    ItemCollection itemCol = result.get(0);

    assertNotNull(itemCol);

    assertEquals(10, itemCol.getEventID());
    assertEquals(100, itemCol.getTaskID());
  }

  /**
   * test parsing a staic json workitem structure
   * 
   * 
   * @throws ParseException
   */
  @Test
  public void testParseStaticWorkitem() throws ParseException {

    String json = "{\"item\":["
        + "     {\"name\":\"type\",\"value\":{\"@type\":\"xs:string\",\"$\":\"workitem\"}},"
        + "     {\"name\":\"$modelversion\",\"value\":{\"@type\":\"xs:string\",\"$\":\"" + "1.0.0"
        + "\"}}," + "     {\"name\":\"$taskid\",\"value\":{\"@type\":\"xs:int\",\"$\":\"1000\"}},"
        + "     {\"name\":\"$eventid\",\"value\":{\"@type\":\"xs:int\",\"$\":\"10\"}},"
        + "     {\"name\":\"vehicle.modeltype\",\"value\":{\"@type\":\"xs:string\",\"$\":\"M5\"}},"
        + "     {\"name\":\"poi.category\",\"value\":{\"@type\":\"xs:string\",\"$\":\"RT\"}},"
        + "     {\"name\":\"customer.language\",\"value\":{\"@type\":\"xs:string\",\"$\":\"DE\"}}"

        + " ]}";

    InputStream targetStream = new ByteArrayInputStream(json.getBytes());
    List<ItemCollection> result;

    try {
      result = ImixsJSONParser.parse(targetStream);

      assertNotNull(result);
      assertTrue(result.size() > 0);

    } catch (UnsupportedEncodingException e) {
      fail();
    }

  }

}
