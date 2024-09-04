package org.imixs.workflow;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.Color;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.imixs.workflow.xml.XMLDocument;
import org.imixs.workflow.xml.XMLDocumentAdapter;
import org.junit.jupiter.api.Test;

/**
 * Test class for itemCollection object
 * 
 * @author rsoika
 * 
 */
public class TestItemCollection {

    @Test
    public void testItemCollection() {
        ItemCollection itemCollection = new ItemCollection();
        itemCollection.replaceItemValue("txtTitel", "Hello");
        assertEquals(itemCollection.getItemValueString("txttitel"), "Hello");
    }

    @Test
    public void testRemoveItem() {
        ItemCollection itemCollection = new ItemCollection();
        itemCollection.replaceItemValue("txtTitel", "Hello");
        assertEquals(itemCollection.getItemValueString("txttitel"), "Hello");
        assertTrue(itemCollection.hasItem("txtTitel"));
        itemCollection.removeItem("TXTtitel");
        assertFalse(itemCollection.hasItem("txtTitel"));
        assertEquals(itemCollection.getItemValueString("txttitel"), "");
    }

    /**
     * This test verifies if the method getItemValueDouble returns the expected
     * values
     */
    @Test
    public void testGetItemValueDouble() {
        Double d;
        Long l;
        Float f;
        String s;
        ItemCollection itemCollection = new ItemCollection();

        // test double with digits....
        d = 1.234;
        itemCollection.replaceItemValue("double", d);
        assertTrue(itemCollection.isItemValueDouble("double"));
        assertEquals(1.234, itemCollection.getItemValueDouble("double"), 0);

        // test double with no digits
        d = (double) 7;
        itemCollection.replaceItemValue("double", d);
        assertEquals(7, itemCollection.getItemValueDouble("double"), 0);

        // test with long value
        l = (long) 4;
        itemCollection.replaceItemValue("long", l);
        assertEquals(4, itemCollection.getItemValueDouble("long"), 0);

        // test with float value - can not be cast exactly back to double!
        f = (float) 4.777;
        itemCollection.replaceItemValue("float", f);
        assertFalse(itemCollection.isItemValueDouble("float"));

        // test String....
        d = 9.712;
        itemCollection.replaceItemValue("double", d);
        assertEquals("9.712", itemCollection.getItemValueString("double"));

        // test cast back from string....
        s = "9.723";
        itemCollection.replaceItemValue("string", s);
        assertFalse(itemCollection.isItemValueDouble("string"));
        assertEquals(9.723, itemCollection.getItemValueDouble("string"), 0);

    }

    /**
     * This test verifies if the method getItemValueFloat returns the expected
     * values
     */
    @Test
    public void testGetItemValueFloat() {
        String s;
        Float f;
        ItemCollection itemCollection = new ItemCollection();

        // test with float value
        f = (float) 4.777;
        itemCollection.replaceItemValue("float", f);
        assertTrue(itemCollection.isItemValueFloat("float"));
        Float fTest = (float) 4.777;
        assertTrue(fTest.equals(itemCollection.getItemValueFloat("float")));

        // test cast back from string....
        s = "9.723";
        itemCollection.replaceItemValue("string", s);
        assertFalse(itemCollection.isItemValueDouble("string"));
        // not possible to cast back exactly!
        // assertEquals(9.723,
        // itemCollection.getItemValueFloat("string"),
        // 0);

        // test Double to Float...
        double d = 50.777;
        itemCollection.replaceItemValue("double", d);
        assertEquals(d, itemCollection.getItemValueDouble("double"), 0);
        // test float ...
        float f1 = itemCollection.getItemValueFloat("double");
        assertEquals("50.777", "" + f1);

    }

    /**
     * This test verifies if the method getItemValueInteger returns the expected
     * values
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testGetItemValueInteger() {
        Double d;
        Long l;
        Float f;
        Integer i;
        Vector v;
        ItemCollection itemCollection = new ItemCollection();

        // test integer
        i = 7;
        itemCollection.replaceItemValue("integer", i);
        assertTrue(itemCollection.isItemValueInteger("integer"));
        assertEquals(7, itemCollection.getItemValueInteger("integer"), 0);

        // test long
        l = (long) 8;
        itemCollection.replaceItemValue("long", l);
        assertFalse(itemCollection.isItemValueInteger("long"));
        assertTrue(itemCollection.isItemValueLong("long"));
        assertEquals(8, itemCollection.getItemValueInteger("long"), 0);

        // test long in vector
        v = new Vector();
        l = (long) 8;
        v.add(l);
        itemCollection.replaceItemValue("long", v);
        // should return false...
        assertFalse(itemCollection.isItemValueInteger("long"));
        assertTrue(itemCollection.isItemValueLong("long"));
        assertEquals(8, itemCollection.getItemValueInteger("long"), 0);

        // test double
        d = 8.765;
        itemCollection.replaceItemValue("double", d);
        assertFalse(itemCollection.isItemValueInteger("double"));
        assertTrue(itemCollection.isItemValueDouble("double"));
        assertEquals(8, itemCollection.getItemValueInteger("double"), 0);

        // test float
        f = (float) 8.765;
        itemCollection.replaceItemValue("float", f);
        assertFalse(itemCollection.isItemValueInteger("float"));
        assertTrue(itemCollection.isItemValueFloat("float"));
        assertEquals(8, itemCollection.getItemValueInteger("float"), 0);

    }

    /**
     * This test verifies the equals method for a ItemCollection
     */
    @Test
    public void testEquals() {

        ItemCollection itemCollection1 = new ItemCollection();
        ItemCollection itemCollection2 = new ItemCollection();

        itemCollection1.replaceItemValue("txtName", "Manfred");
        itemCollection2.replaceItemValue("txtname", "Manfred");
        itemCollection1.replaceItemValue("numID", Integer.valueOf(20));
        itemCollection2.replaceItemValue("numID", Integer.valueOf(20));

        assertEquals(itemCollection1, itemCollection2);
        assertNotSame(itemCollection1, itemCollection2);

        // compare not equals
        itemCollection1.replaceItemValue("txtName", "Manfred");
        itemCollection2.replaceItemValue("txtname", "Manfred1");

        assertFalse(itemCollection1.equals(itemCollection2));
        itemCollection2.replaceItemValue("txtname", "Manfred");
        itemCollection2.replaceItemValue("numID", Integer.valueOf(21));
        assertFalse(itemCollection1.equals(itemCollection2));
        assertNotSame(itemCollection1, itemCollection2);

    }

    /**
     * This test verifies the behavior when copy the elements of another
     * ItemCollection
     * 
     */
    @Test
    public void testReplaceAllValues() {

        ItemCollection itemCollection1 = new ItemCollection();
        ItemCollection itemCollection2 = new ItemCollection();

        itemCollection1.replaceItemValue("txtName", "Manfred");
        itemCollection1.replaceItemValue("numID", Integer.valueOf(20));

        // copy values
        itemCollection2.replaceAllItems(itemCollection1.getAllItems());
        assertEquals(itemCollection1, itemCollection2);
        assertNotSame(itemCollection1, itemCollection2);

        // change value of itemcol2
        itemCollection2.replaceItemValue("txtName", "Anna");
        assertEquals("Manfred", itemCollection1.getItemValueString("txtName"));
        assertEquals("Anna", itemCollection2.getItemValueString("txtName"));

        assertFalse(itemCollection1.equals(itemCollection2));
        assertNotSame(itemCollection1, itemCollection2);

    }

    /**
     * Test the copy method
     */
    @Test
    public void testCopyValues() {

        ItemCollection itemCollection1 = new ItemCollection();
        ItemCollection itemCollection2 = new ItemCollection();

        itemCollection1.replaceItemValue("txtName", "Manfred");
        itemCollection1.replaceItemValue("numID", Integer.valueOf(20));

        // copy values
        itemCollection2.copy(itemCollection1);
        assertEquals(itemCollection1, itemCollection2);
        assertNotSame(itemCollection1, itemCollection2);

        // change value of itemcol2
        itemCollection2.replaceItemValue("txtName", "Anna");
        assertEquals("Manfred", itemCollection1.getItemValueString("txtName"));
        assertEquals("Anna", itemCollection2.getItemValueString("txtName"));

        assertFalse(itemCollection1.equals(itemCollection2));
        assertNotSame(itemCollection1, itemCollection2);

    }

    /**
     * This test verifies the behavior when copy the elements of another
     * ItemCollection with embedded collections!
     * 
     */
    @Test
    public void testCopyItemCollection() {

        ItemCollection itemCollection1 = new ItemCollection();
        itemCollection1.replaceItemValue("txtName", "Manfred");
        itemCollection1.replaceItemValue("numID", Integer.valueOf(20));

        ItemCollection itemCollection2 = new ItemCollection(itemCollection1);

        assertEquals(itemCollection1, itemCollection2);
        assertNotSame(itemCollection1, itemCollection2);

        // change valud of itemcol2
        itemCollection2.replaceItemValue("txtName", "Anna");
        assertEquals("Manfred", itemCollection1.getItemValueString("txtName"));
        assertEquals("Anna", itemCollection2.getItemValueString("txtName"));

    }

    /**
     * This test verifies the behavior when copy the elements of another
     * ItemCollection with embedded collections!
     * 
     */
    @Test
    public void testCopyValuesWithEmbeddedCollection() {

        ItemCollection itemCollection1 = new ItemCollection();
        ItemCollection itemCollection2 = new ItemCollection();
        ItemCollection child1 = new ItemCollection();

        itemCollection1.replaceItemValue("txtName", "Manfred");
        itemCollection1.replaceItemValue("numID", Integer.valueOf(20));

        child1.replaceItemValue("txtName", "Thor");
        child1.replaceItemValue("numID", Integer.valueOf(2));

        try {
            itemCollection1.replaceItemValue("child", XMLDocumentAdapter.getDocument(child1));
        } catch (Exception e) {

            fail();
        }

        // copy values
        itemCollection2.replaceAllItems(itemCollection1.getAllItems());
        assertEquals(itemCollection1, itemCollection2);

        assertNotSame(itemCollection1, itemCollection2);

        XMLDocument xmlChild = (XMLDocument) itemCollection1.getItemValue("child").get(0);

        ItemCollection testChild = XMLDocumentAdapter.putDocument(xmlChild);
        assertEquals(2, testChild.getItemValueInteger("numID"));

        // manipulate child1 and repeat the test!
        child1.replaceItemValue("numID", Integer.valueOf(3));
        assertEquals(itemCollection1, itemCollection2);
        assertNotSame(itemCollection1, itemCollection2);

        // Now its the same object !
        assertNotSame(child1, testChild);

        // test child1 form itemcol1
        xmlChild = (XMLDocument) itemCollection1.getItemValue("child").get(0);

        testChild = XMLDocumentAdapter.putDocument(xmlChild);

        // expected id is not 3!
        assertEquals(2, testChild.getItemValueInteger("numID"));

        // change value of itemcol2
        itemCollection2.replaceItemValue("txtName", "Anna");
        assertEquals("Manfred", itemCollection1.getItemValueString("txtName"));
        assertEquals("Anna", itemCollection2.getItemValueString("txtName"));

        assertFalse(itemCollection1.equals(itemCollection2));
        assertNotSame(itemCollection1, itemCollection2);

    }

    /**
     * Same as testCopyValuesWithEmbeddedCollection but now we add a list of
     * ItemCollections into a ItemCollection!
     * 
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testCopyValuesWithEmbeddedCollectionList() {

        ItemCollection itemCollection1 = new ItemCollection();
        ItemCollection itemCollection2 = new ItemCollection();
        ItemCollection child1 = new ItemCollection();
        ItemCollection child2 = new ItemCollection();

        itemCollection1.replaceItemValue("txtName", "Manfred");
        itemCollection1.replaceItemValue("numID", Integer.valueOf(20));

        child1.replaceItemValue("txtName", "Thor");
        child1.replaceItemValue("numID", Integer.valueOf(2));
        child2.replaceItemValue("txtName", "Ilias");
        child2.replaceItemValue("numID", Integer.valueOf(3));
        List childs = new ArrayList<>();
        try {
            childs.add(XMLDocumentAdapter.getDocument(child1));
            childs.add(XMLDocumentAdapter.getDocument(child2));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        itemCollection1.replaceItemValue("childs", childs);

        // copy values
        itemCollection2.replaceAllItems(itemCollection1.getAllItems());
        assertEquals(itemCollection1, itemCollection2);
        assertNotSame(itemCollection1, itemCollection2);

        // now manipulate child1
        child1.replaceItemValue("numID", 101);

        // repeat test!
        assertEquals(itemCollection1, itemCollection2);
        assertNotSame(itemCollection1, itemCollection2);

        // get child 1 from itemcol2
        List v = itemCollection2.getItemValue("childs");
        XMLDocument xmlct1 = (XMLDocument) v.get(0);
        ItemCollection ct1 = XMLDocumentAdapter.putDocument(xmlct1);
        assertEquals("Thor", ct1.getItemValueString("txtName"));
        assertEquals(2, ct1.getItemValueInteger("numID"));

        assertEquals(101, child1.getItemValueInteger("numID"));

        XMLDocument xmlct2 = (XMLDocument) v.get(1);
        ItemCollection ct2 = XMLDocumentAdapter.putDocument(xmlct2);
        assertEquals("Ilias", ct2.getItemValueString("txtName"));
        assertEquals(3, ct2.getItemValueInteger("numID"));

        // test size
        assertTrue(v.size() == 2);

    }

    /**
     * Same as testCopyValuesWithEmbeddedCollection but now we test the behavior
     * with map interfaces
     * 
     * here we can see that a map is copied by reference!
     * 
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testCopyValuesWithEmbeddedMap() {

        ItemCollection itemCollection1 = new ItemCollection();
        ItemCollection itemCollection2 = new ItemCollection();
        Map child1 = new HashMap();

        itemCollection1.replaceItemValue("txtName", "Manfred");
        itemCollection1.replaceItemValue("numID", Integer.valueOf(20));

        child1.put("txtName", "Thor");
        child1.put("numID", Integer.valueOf(2));

        itemCollection1.replaceItemValue("child", child1);

        // copy values
        itemCollection2.replaceAllItems(itemCollection1.getAllItems());
        assertEquals(itemCollection1, itemCollection2);
        assertNotSame(itemCollection1, itemCollection2);
        Map testChild = (Map) itemCollection1.getItemValue("child").get(0);
        assertEquals(2, testChild.get("numID"));

        // manipulate child1 and repeat the test!
        child1.put("numID", Integer.valueOf(3));
        assertFalse(itemCollection1.equals(itemCollection2));
        assertNotSame(itemCollection1, itemCollection2);

        // its the same object !
        assertSame(child1, testChild);

    }

    /**
     * This test verifies the imixs workflow basic attributes
     * 
     */
    @Test
    public void testBasicAttributes() {

        ItemCollection itemCollection1 = new ItemCollection();

        itemCollection1.replaceItemValue(WorkflowKernel.MODELVERSION, "1.0.0");
        itemCollection1.replaceItemValue(WorkflowKernel.TASKID, 100);
        itemCollection1.replaceItemValue(WorkflowKernel.EVENTID, 10);
        itemCollection1.replaceItemValue(WorkflowKernel.TYPE, "workitem_test");
        itemCollection1.replaceItemValue(WorkflowKernel.UNIQUEID, "ABC-123");

        assertEquals("1.0.0", itemCollection1.getModelVersion());
        assertEquals(10, itemCollection1.getEventID());
        assertEquals(100, itemCollection1.getTaskID());
        assertEquals("workitem_test", itemCollection1.getType());
        assertEquals("ABC-123", itemCollection1.getUniqueID());

    }

    /**
     * This method verifies if null values will be removed from lists.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testNullValues() {

        Vector v1 = new Vector();
        v1.add(null);
        v1.add("");
        v1.add(null);
        v1.add("anna");

        Vector v2 = new Vector();
        v1.add(null);

        ItemCollection itemCol = new ItemCollection();
        itemCol.replaceItemValue("a", v1);
        itemCol.replaceItemValue("b", v2);

        List l1 = itemCol.getItemValue("a");
        List l2 = itemCol.getItemValue("b");

        // test if the null values are removed...
        assertEquals(2, l1.size());
        assertEquals("", l1.get(0));
        assertEquals("anna", l1.get(1));

        assertEquals(0, l2.size());

    }

    @Test
    public void testItemCollectionItemNameList() {
        ItemCollection itemCollection = new ItemCollection();
        itemCollection.replaceItemValue("_subject", "Hello");
        itemCollection.replaceItemValue("_dummy", "Hello");
        itemCollection.replaceItemValue("_title", "Hello");

        List<String> itemNames = itemCollection.getItemNames();
        assertEquals(3, itemNames.size());

        itemCollection.removeItem("_dummy");
        itemNames = itemCollection.getItemNames();
        assertEquals(2, itemNames.size());

    }

    /**
     * This method verifies the clone interface
     */
    @Test
    public void testCloning() {

        ItemCollection itemCol1 = new ItemCollection();
        itemCol1.replaceItemValue("a", 1);
        itemCol1.replaceItemValue("$a", true);
        itemCol1.replaceItemValue("b", "hello");
        itemCol1.replaceItemValue("c", "world");
        itemCol1.replaceItemValue("d", "cats & dogs");

        // clone only some attributes
        List<String> attributes = new ArrayList<String>();
        attributes.add("a");
        attributes.add("b");
        attributes.add("d | animals");
        ItemCollection itemCol2 = itemCol1.clone(attributes);

        assertNotNull(itemCol2);

        // test values of clone
        assertEquals(1, itemCol2.getItemValueInteger("a"));
        assertEquals("hello", itemCol2.getItemValueString("b"));
        assertEquals("", itemCol2.getItemValueString("c"));
        assertEquals("cats & dogs", itemCol2.getItemValueString("animals"));
        assertFalse(itemCol2.hasItem("d"));

        // test full clone
        ItemCollection itemCol3 = null;

        if (itemCol1 instanceof Cloneable) {
            itemCol3 = (ItemCollection) itemCol1.clone();
        }
        assertNotNull(itemCol3);
        assertEquals(1, itemCol3.getItemValueInteger("a"));
        assertEquals("hello", itemCol3.getItemValueString("b"));
        assertEquals("world", itemCol3.getItemValueString("c"));

        // itemCol2 should be equals to itemCol1
        assertNotSame(itemCol2, itemCol1);
        // itemCol3 should be equals to itemCol1
        assertNotSame(itemCol3, itemCol1);
        // itemCol3 should be equals to itemCol1
        assertEquals(itemCol1, itemCol3);
        // itemCol2 should be equals to itemCol1
        assertFalse(itemCol1.equals(itemCol2));

        // now we change some values of itemCol1 to see if this affects itemCol2
        // and itemCol3....
        itemCol1.replaceItemValue("c", "Imixs");
        assertEquals("", itemCol2.getItemValueString("c"));
        assertEquals("world", itemCol3.getItemValueString("c"));

    }

    /**
     * This method verifies the clone interface in conjunction with byte arrays as
     * used in the $file field
     */
    @Test
    @Deprecated
    public void testCloningByteArraysDeprecated() {

        ItemCollection itemCol1 = new ItemCollection();
        itemCol1.replaceItemValue("a", 1);
        itemCol1.replaceItemValue("b", "hello");
        itemCol1.replaceItemValue("c", "world");
        byte[] empty = { 0 };
        // add a dummy file
        itemCol1.addFile(empty, "test1.txt", "application/xml");

        ItemCollection itemCol2 = (ItemCollection) itemCol1.clone();

        assertNotNull(itemCol2);

        // test values of clone
        assertEquals(1, itemCol2.getItemValueInteger("a"));
        assertEquals("hello", itemCol2.getItemValueString("b"));
        // test the byte content of itemcol2
        Map<String, List<Object>> conedFilesTest = itemCol2.getFiles();
        List<Object> fileContentTest = conedFilesTest.get("test1.txt");
        byte[] file1DataTest = (byte[]) fileContentTest.get(1);

        assertArrayEquals(empty, file1DataTest);

        // -------------------
        // Now we change file content in itemcol1
        byte[] dummy = { 1, 2, 3 };
        itemCol1.removeFile("test1.txt");
        itemCol1.addFile(dummy, "test1.txt", "application/xml");

        // test the byte content of itemCol1
        Map<String, List<Object>> conedFiles1 = itemCol1.getFiles();
        List<Object> fileContent1 = conedFiles1.get("test1.txt");
        byte[] file1Data1 = (byte[]) fileContent1.get(1);
        // we expect the new dummy array { 1, 2, 3 }
        assertArrayEquals(dummy, file1Data1);

        // test the clone
        Map<String, List<Object>> conedFiles2 = itemCol2.getFiles();
        List<Object> fileContent2 = conedFiles2.get("test1.txt");
        byte[] file1Data2 = (byte[]) fileContent2.get(1);
        // we expect still the empty array
        assertArrayEquals(empty, file1Data2);

    }

    /**
     * This method verifies the clone interface in conjunction with byte arrays as
     * used in the $file field
     */
    @Test
    public void testCloningByteArraysWithFileData() {

        ItemCollection itemCol1 = new ItemCollection();
        itemCol1.replaceItemValue("a", 1);
        itemCol1.replaceItemValue("b", "hello");
        itemCol1.replaceItemValue("c", "world");
        byte[] empty = { 0 };
        // add a dummy file
        itemCol1.addFileData(new FileData("test1.txt", empty, "application/xml", null));

        ItemCollection itemCol2 = (ItemCollection) itemCol1.clone();

        assertNotNull(itemCol2);

        // test values of clone
        assertEquals(1, itemCol2.getItemValueInteger("a"));
        assertEquals("hello", itemCol2.getItemValueString("b"));
        // test the byte content of itemcol2
        // Map<String, List<Object>> conedFilesTest = itemCol2.getFiles();

        byte[] file1DataTest = itemCol2.getFileData("test1.txt").getContent();

        assertArrayEquals(empty, file1DataTest);

        // -------------------
        // Now we change file content in itemcol1
        byte[] dummy = { 1, 2, 3 };
        itemCol1.removeFile("test1.txt");
        itemCol1.addFileData(new FileData("test1.txt", dummy, "application/xml", null));

        // test the byte content of itemCol1
        byte[] file1Data1 = itemCol1.getFileData("test1.txt").getContent();
        // we expect the new dummy array { 1, 2, 3 }
        assertArrayEquals(dummy, file1Data1);

        // test the clone
        byte[] file1Data2 = itemCol2.getFileData("test1.txt").getContent();
        // we expect still the empty array
        assertArrayEquals(empty, file1Data2);

    }

    /**
     * This method just test the time difference between clone and copy a
     * ItemCollection hashmap
     * 
     */
    @Test
    public void testPerformanceCloning() {

        ItemCollection itemCol1 = new ItemCollection();
        itemCol1.replaceItemValue("a", 1);
        itemCol1.replaceItemValue("b", "hello");
        itemCol1.replaceItemValue("c", "world");

        // clone
        long l = System.currentTimeMillis();
        ItemCollection itemCol2 = (ItemCollection) itemCol1.clone();
        System.out.println("Performancetest ItemCollecton clone: " + (System.currentTimeMillis() - l) + "ms");
        assertNotNull(itemCol2);

        // copy
        l = System.currentTimeMillis();
        ItemCollection itemCol3 = new ItemCollection();
        itemCol3.setAllItems(itemCol1.getAllItems());
        System.out.println("Performancetest ItemCollecton clone: " + (System.currentTimeMillis() - l) + "ms");
        assertNotNull(itemCol3);

    }

    /**
     * This getItemValue hasItem with invalid string patterns
     * 
     */
    @Test
    public void testGetItemByName() {

        ItemCollection itemCol1 = new ItemCollection();
        itemCol1.replaceItemValue(" a", "hello");
        itemCol1.replaceItemValue(" b ", "world");
        itemCol1.replaceItemValue("c", "of");
        itemCol1.replaceItemValue("d", "Imixs-Workflow");

        assertEquals("hello", itemCol1.getItemValueString("a"));
        assertEquals("hello", itemCol1.getItemValueString(" a"));
        assertEquals("world", itemCol1.getItemValueString(" b"));
        assertEquals("of", itemCol1.getItemValueString(" c "));

        assertTrue(itemCol1.hasItem(" a  "));
        assertTrue(itemCol1.hasItem(" a"));
        assertTrue(itemCol1.hasItem("a"));

        assertFalse(itemCol1.hasItem(null));
    }

    /**
     * Test the append method
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testItemCollectionAppend() {
        ItemCollection itemCollection = new ItemCollection();
        itemCollection.replaceItemValue("txtTitel", "Hello");
        assertEquals(itemCollection.getItemValueString("txttitel"), "Hello");

        itemCollection.appendItemValue("txttitel", "World");
        assertEquals(itemCollection.getItemValueString("txttitel"), "Hello");
        List values = itemCollection.getItemValue("txtTitel");
        assertEquals(2, values.size());

        itemCollection.appendItemValue("txttitel", "World");
        assertEquals(itemCollection.getItemValueString("txttitel"), "Hello");
        values = itemCollection.getItemValue("txtTitel");
        assertEquals(3, values.size());

    }

    /**
     * Test no basic type
     */
    @Test
    public void testItemCollectionNoBasictype() {
        ItemCollection itemCollection = new ItemCollection();
        itemCollection.replaceItemValue("txtTitel", "Hello");
        try {
            itemCollection.replaceItemValue("color", new Color(1, 1, 1));
            fail();
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                // expected result
            } else {
                fail();
            }
        }

    }

    /**
     * Test raw type int, double, boolean
     */
    @Test
    public void testItemCollectionRawtype() {
        ItemCollection itemCollection = new ItemCollection();
        itemCollection.replaceItemValue("txtTitel", "Hello");
        double d = 5.20;
        itemCollection.replaceItemValue("price", d);

        byte[] bytes = "Some Data".getBytes();
        itemCollection.replaceItemValue("data", bytes);

        assertNotNull(itemCollection);

    }

    /**
     * Test the fileData methods
     */
    @Test
    @Deprecated
    public void testFileDataDeprecated() {
        ItemCollection itemColSource = new ItemCollection();

        // add a dummy file
        byte[] empty = { 0 };
        itemColSource.addFile(empty, "test1.txt", "application/xml");

        ItemCollection itemColTarget = new ItemCollection();

        itemColTarget.addFileData(itemColSource.getFileData("test1.txt"));

        FileData filedata = itemColTarget.getFileData("test1.txt");
        assertNotNull(filedata);
        assertEquals("test1.txt", filedata.getName());
        assertEquals("application/xml", filedata.getContentType());

        // test the byte content of itemColSource
        Map<String, List<Object>> conedFiles1 = itemColSource.getFiles();
        List<Object> fileContent1 = conedFiles1.get("test1.txt");
        byte[] file1Data1 = (byte[]) fileContent1.get(1);
        // we expect the new dummy array { 1, 2, 3 }
        assertArrayEquals(empty, file1Data1);

        // test the byte content of itemColTarget
        conedFiles1 = itemColTarget.getFiles();
        fileContent1 = conedFiles1.get("test1.txt");
        file1Data1 = (byte[]) fileContent1.get(1);
        // we expect the new dummy array { 1, 2, 3 }
        assertArrayEquals(empty, file1Data1);

    }

    @Test
    public void testFileData() {
        ItemCollection itemColSource = new ItemCollection();

        // add a dummy file
        byte[] empty = { 0 };
        itemColSource.addFileData(new FileData("test1.txt", empty, "application/xml", null));

        ItemCollection itemColTarget = new ItemCollection();

        itemColTarget.addFileData(itemColSource.getFileData("test1.txt"));

        FileData filedata = itemColTarget.getFileData("test1.txt");
        assertNotNull(filedata);
        assertEquals("test1.txt", filedata.getName());
        assertEquals("application/xml", filedata.getContentType());

        // test the byte content of itemColSource
        byte[] file1Data1 = itemColSource.getFileData("test1.txt").getContent();
        // we expect the new dummy array { 1, 2, 3 }
        assertArrayEquals(empty, file1Data1);

        // test the byte content of itemColTarget
        file1Data1 = itemColTarget.getFileData("test1.txt").getContent();
        // we expect the new dummy array { 1, 2, 3 }
        assertArrayEquals(empty, file1Data1);

        // test getFileNames
        assertEquals(1, itemColSource.getItemValueInteger("$file.count"));
        List<String> fileNames = itemColSource.getFileNames();
        assertEquals(1, fileNames.size());
        assertEquals("test1.txt", fileNames.get(0));

        // remove file....
        itemColSource.removeFile("test1.txt");
        assertEquals(0, itemColSource.getItemValueInteger("$file.count"));
        fileNames = itemColSource.getFileNames();
        assertEquals(0, fileNames.size());

    }

    /*
     * Test the fluent code interface.
     */
    @Test
    public void testFluentInterface() {
        ItemCollection itemCollection = new ItemCollection().model("1.0.0").task(1).event(10);

        assertEquals("1.0.0", itemCollection.getModelVersion());
        assertEquals(1, itemCollection.getTaskID());
        assertEquals(10, itemCollection.getEventID());

        // set group
        itemCollection = new ItemCollection().workflowGroup("Ticket").task(1).event(10);
        assertEquals("Ticket", itemCollection.getWorkflowGroup());
        assertEquals(1, itemCollection.getTaskID());
        assertEquals(10, itemCollection.getEventID());

    }

    /**
     * Test issue #383, #384
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testDeprecatedFieldProcessID() {

        assertEquals("$processid", WorkflowKernel.PROCESSID);
        assertEquals("$taskid", WorkflowKernel.TASKID);

        ItemCollection itemColSource = new ItemCollection();

        itemColSource.replaceItemValue("$processid", 42);
        assertEquals(42, itemColSource.getTaskID());
        assertEquals(42, itemColSource.getProcessID());
        assertEquals(42, itemColSource.getItemValueInteger("$processid"));

        // test setter
        itemColSource = new ItemCollection();
        itemColSource.setTaskID(43);
        assertEquals(43, itemColSource.getTaskID());
        assertEquals(43, itemColSource.getProcessID());
        assertEquals(43, itemColSource.getItemValueInteger("$processid"));

        // test direct setting
        itemColSource = new ItemCollection();
        itemColSource.replaceItemValue("$taskid", 44);
        assertEquals(44, itemColSource.getTaskID());
        assertEquals(44, itemColSource.getProcessID());
        // !!!
        assertEquals(0, itemColSource.getItemValueInteger("$processid"));
    }

    /**
     * Test the methods setItemValue and getItemValue with type String
     */
    @Test
    public void testGetItemValueTypeString() {

        ItemCollection itemCol = new ItemCollection();
        itemCol.setItemValue("txtname", "hello");
        itemCol.setItemValue("numage", 7);

        // test String values
        String s = itemCol.getItemValue("txtname", String.class);
        assertEquals("hello", s);

        // test non existing value
        assertNull(itemCol.getItemValue("txtname2", String.class));

    }

    /**
     * Test the methods setItemValue and getItemValue with type Integer/int
     */
    @Test
    public void testGetItemValueTypeInt() {

        ItemCollection itemCol = new ItemCollection();
        itemCol.setItemValue("txtname", "hello");
        itemCol.setItemValue("numage", 7);

        // test int values....
        int i1 = itemCol.getItemValue("numage", Integer.class);
        assertEquals(7, i1);

        int i2 = itemCol.getItemValue("numage", int.class);
        assertEquals(7, i2);

        Integer i3 = itemCol.getItemValue("numage", Integer.class);
        assertEquals(7, i3.intValue());

        // test non existing value
        assertNull(itemCol.getItemValue("txtname2", String.class));

    }

    /**
     * Test the methods setItemValue and getItemValue with type Long/long
     */
    @Test
    public void testGetItemValueTypeLong() {

        ItemCollection itemCol = new ItemCollection();
        itemCol.setItemValue("txtname", "hello");
        itemCol.setItemValue("numage", Long.valueOf(7));

        // test int values....
        long l1 = itemCol.getItemValue("numage", Long.class);
        assertEquals(7, l1);

        long l2 = itemCol.getItemValue("numage", long.class);
        assertEquals(7, l2);

        Long l3 = itemCol.getItemValue("numage", long.class);
        assertEquals(7, l3.longValue());

        // test non existing value
        assertNull(itemCol.getItemValue("txtname2", String.class));

    }

    /**
     * Test the fluent code interface for the method setItemValue.
     */
    @Test
    public void testSetItemValueFluent() {

        ItemCollection itemCol = new ItemCollection();
        itemCol.setItemValue("team", "Anna").appendItemValue("team", "Mark").appendItemValue("team", "Jo");

        String s = itemCol.getItemValue("team", String.class);
        assertEquals("Anna", s);

        @SuppressWarnings("unchecked")
        List<String> vaules = itemCol.getItemValue("team");

        assertEquals(3, vaules.size());
        assertEquals("Mark", vaules.get(1));
        assertEquals("Jo", vaules.get(2));

    }

    /**
     * Test setting an item value null within an unmutable list See:
     * https://www.codebyamir.com/blog/calling-remove-on-arraylist-throws-exception-java-lang-unsupportedoperationexception-at-java-util-abstractlist-remove
     * 
     * @see Issue #593
     */
    @Test
    public void testSetItemValueNull() {

        ItemCollection itemCol = new ItemCollection();

        // create a mutable list with null value
        List<String> values = new ArrayList<>(Arrays.asList("Amir", null, "Beth", "Lucy"));

        itemCol.setItemValue("team", values);
        assertTrue(itemCol.hasItem("team"));

        // create a unmutable list with null value
        values = Arrays.asList("Amir", null, "Beth", "Lucy");
        // this will cause a java.lang.UnsupportedOperationExeption because the
        // setItemValue method tries to remove null values. This particular
        // implementation has a fixed size. And a remove is not allowed.
        // It seems to make no sense to care about this case
        try {
            itemCol.setItemValue("team", values);
            fail(); // exception expected
        } catch (UnsupportedOperationException e) {
            // exception expected
        }
        assertTrue(itemCol.hasItem("team"));

    }

    /**
     * Test the methods getItemValue with convertign type
     */
    @Test
    public void testGetItemValueTypeMixedValues() {

        ItemCollection itemCol = new ItemCollection();
        // number as string
        itemCol.setItemValue("_a", "5");
        itemCol.setItemValue("_b", 7); // int

        // test string
        String s = itemCol.getItemValue("_a", String.class);
        assertEquals("5", s);

        // test int
        int i = itemCol.getItemValue("_a", int.class);
        assertEquals(5, i);

        // test long
        long l = itemCol.getItemValue("_a", long.class);
        assertEquals(5, l);

        // test string of _b
        s = itemCol.getItemValue("_b", String.class);
        assertEquals("7", s);
    }

    /**
     * Test the method getItemValueList. Test mixed types and the convertion into
     * the expected object type.
     */
    @Test
    public void testGetItemValueListType() {
        ItemCollection itemCol = new ItemCollection();
        itemCol.setItemValue("txtname", "hello");
        itemCol.appendItemValue("txtname", "world");
        itemCol.appendItemValue("txtname", Integer.valueOf(47));

        // test String list...
        List<String> slist = itemCol.getItemValueList("txtname", String.class);
        assertEquals(3, slist.size());
        assertEquals("hello", slist.get(0));
        assertEquals("world", slist.get(1));
        assertEquals("47", slist.get(2));

        // test int list
        List<Integer> integerlist = itemCol.getItemValueList("txtname", Integer.class);
        assertEquals(3, integerlist.size());
        assertEquals(Integer.valueOf(0), integerlist.get(0));
        assertEquals(Integer.valueOf(0), integerlist.get(1));
        assertEquals(Integer.valueOf(47), integerlist.get(2));

        // test int list
        List<Integer> intlist = itemCol.getItemValueList("txtname", int.class);
        assertEquals(3, intlist.size());
        assertEquals(Integer.valueOf(0), intlist.get(0));
        assertEquals(Integer.valueOf(0), intlist.get(1));
        assertEquals(Integer.valueOf(47), intlist.get(2));

    }

    /**
     * Test the method getItemValueList with an empty list
     */
    @Test
    public void testGetItemValueListEmpty() {
        ItemCollection itemCol = new ItemCollection();

        List<String> slist = itemCol.getItemValueList("txtname", String.class);
        // we expect an empty list
        assertNotNull(slist);
        assertEquals(0, slist.size());
    }

    /**
     * Test the method getItemValueList with a non convertible type.
     */
    @Test
    public void testGetItemValueListNonConvertable() {
        ItemCollection itemCol = new ItemCollection();
        itemCol.setItemValue("txtname", "hello");
        itemCol.appendItemValue("txtname", Integer.valueOf(47));
        Map<String, String> map = new HashMap<String, String>();
        map.put("_subject", "some data");
        itemCol.appendItemValue("txtname", map);

        // test String list...
        List<String> slist = itemCol.getItemValueList("txtname", String.class);
        assertEquals(3, slist.size());
        assertEquals("hello", slist.get(0));
        assertEquals("47", slist.get(1));
        assertEquals("{_subject=some data}", slist.get(2)); // map toString

        // test int list
        List<Integer> integerlist = itemCol.getItemValueList("txtname", Integer.class);
        assertEquals(3, integerlist.size());
        assertEquals(Integer.valueOf(0), integerlist.get(0));
        assertEquals(Integer.valueOf(47), integerlist.get(1));
        assertEquals(Integer.valueOf(0), integerlist.get(2));

        // test int list
        List<Integer> intlist = itemCol.getItemValueList("txtname", int.class);
        assertEquals(3, intlist.size());
        assertEquals(Integer.valueOf(0), intlist.get(0));
        assertEquals(Integer.valueOf(47), intlist.get(1));
        assertEquals(Integer.valueOf(0), intlist.get(2));

    }

    /**
     * Test the method getItemValue for number types null
     */
    @Test
    public void testGetItemValueNumberNull() {
        ItemCollection itemCol = new ItemCollection();

        // integer
        Integer i = itemCol.getItemValue("unkonw", Integer.class);
        assertNotNull(i);
        assertEquals(0, i.intValue());

        // int
        int ii = itemCol.getItemValue("unkonw", int.class);
        assertNotNull(ii);
        assertEquals(0, ii);

        // Float
        Float f = itemCol.getItemValue("unkonw", Float.class);
        assertNotNull(f);
        assertEquals(0, f.intValue());

        // float
        float ff = itemCol.getItemValue("unkonw", float.class);
        assertNotNull(ff);
        assertEquals(0, ff, 0);

        // Long
        Long l = itemCol.getItemValue("unkonw", Long.class);
        assertNotNull(l);
        assertEquals(0, l.intValue());

        // long
        long ll = itemCol.getItemValue("unkonw", long.class);
        assertNotNull(ll);
        assertEquals(0, ll);

        // Double
        Double d = itemCol.getItemValue("unkonw", Double.class);
        assertNotNull(d);
        assertEquals(0, d.intValue());

        // double
        double dd = itemCol.getItemValue("unkonw", double.class);
        assertNotNull(dd);
        assertEquals(0, dd, 0);

    }

    /**
     * Test the handling ob Calendar objects. CalendarObjects will be converted into
     * Date.
     * 
     */
    @Test
    public void testCalendarValues() {
        ItemCollection itemCol = new ItemCollection();

        Date nowDate = new Date();

        Calendar nowCalendar = Calendar.getInstance();
        nowCalendar.setTime(nowDate);
        itemCol.setItemValue("calendar", nowCalendar);

        Date testDateCal = itemCol.getItemValueDate("calendar");
        assertNotNull(testDateCal);
        assertEquals(nowDate, testDateCal);

    }

    /**
     * Test the method getItemValueDate and getItemValueLocalDateTime and the
     * corresponding setter method
     * 
     */
    @Test
    public void testLocalDateTimeValues() {
        ItemCollection itemCol = new ItemCollection();

        Date nowDate = new Date();
        itemCol.setItemValue("date", nowDate);
        Date testDate = itemCol.getItemValueDate("date");
        assertEquals(nowDate, testDate);

        // test with LocalDateTime
        LocalDateTime localDateTime = itemCol.getItemValueLocalDateTime("date");

        // test if equal....
        Date out = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        assertEquals(nowDate, out);

        // now set a LocalDateTime Value...
        LocalDateTime ldt = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault());
        itemCol.setItemValue("localdate", ldt);

        LocalDateTime localDateTimeTest = itemCol.getItemValueLocalDateTime("localdate");
        assertNotNull(localDateTimeTest);
        assertEquals(ldt, localDateTimeTest);

        // test date convertion....
        Date dateTest2 = itemCol.getItemValueDate("localDAte");

        assertEquals(nowDate, dateTest2);

    }

    /**
     * Test the method getItemValueDate and getItemValueLocalDate and the
     * corresponding setter method
     * 
     */
    @Test
    public void testLocalDateValues() {
        ItemCollection itemCol = new ItemCollection();

        Date nowDate = new Date();
        itemCol.setItemValue("date", nowDate);
        Date testDate = itemCol.getItemValueDate("date");
        assertEquals(nowDate, testDate);

        // test with LocalDateTime
        LocalDate localDate = itemCol.getItemValueLocalDate("date");

        // test if equal....
        Date out = Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        assertEquals(dateFormat.format(nowDate), dateFormat.format(out));

        // now set a LocalDateTime Value...
        LocalDate ldt = nowDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        itemCol.setItemValue("localdate", ldt);

        LocalDate localDateTest = itemCol.getItemValueLocalDate("localdate");
        assertNotNull(localDateTest);
        assertEquals(ldt, localDateTest);

        // test date convertion....
        Date dateTest2 = itemCol.getItemValueDate("localDAte");

        assertEquals(dateFormat.format(nowDate), dateFormat.format(dateTest2));

    }

    /**
     * Test the isItemEmpty method
     */
    @Test
    public void testIsItemEmpty() {

        ItemCollection workitem = new ItemCollection();
        workitem.setItemValue("a", null);
        workitem.setItemValue("b", "");

        List<String> list = new ArrayList<String>();
        list.add(null);
        list.add("");
        list.add("test");
        workitem.setItemValue("c", list);

        assertTrue(workitem.isItemEmpty("a"));
        assertTrue(workitem.isItemEmpty("b"));
        assertFalse(workitem.isItemEmpty("c"));
        // we expect 2 values because the null value should be removed
        assertEquals(2, workitem.getItemValue("c").size());

    }
}
