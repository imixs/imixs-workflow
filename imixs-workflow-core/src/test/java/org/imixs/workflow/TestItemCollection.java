package org.imixs.workflow;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.imixs.workflow.xml.XMLItemCollection;
import org.imixs.workflow.xml.XMLItemCollectionAdapter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test class for itemCollection object
 * 
 * @author rsoika
 * 
 */
public class TestItemCollection {

	@Test
	@Category(org.imixs.workflow.ItemCollection.class)
	public void testItemCollection() {
		ItemCollection itemCollection = new ItemCollection();
		itemCollection.replaceItemValue("txtTitel", "Hello");
		Assert.assertEquals(itemCollection.getItemValueString("txttitel"),
				"Hello");
	}

	@Test
	@Category(org.imixs.workflow.ItemCollection.class)
	public void testRemoveItem() {
		ItemCollection itemCollection = new ItemCollection();
		itemCollection.replaceItemValue("txtTitel", "Hello");
		Assert.assertEquals(itemCollection.getItemValueString("txttitel"),
				"Hello");
		Assert.assertTrue(itemCollection.hasItem("txtTitel"));
		itemCollection.removeItem("TXTtitel");
		Assert.assertFalse(itemCollection.hasItem("txtTitel"));
		Assert.assertEquals(itemCollection.getItemValueString("txttitel"), "");
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
		Assert.assertTrue(itemCollection.isItemValueDouble("double"));
		Assert.assertEquals(1.234, itemCollection.getItemValueDouble("double"),
				0);

		// test double with no digits
		d = (double) 7;
		itemCollection.replaceItemValue("double", d);
		Assert.assertEquals(7, itemCollection.getItemValueDouble("double"), 0);

		// test with long value
		l = (long) 4;
		itemCollection.replaceItemValue("long", l);
		Assert.assertEquals(4, itemCollection.getItemValueDouble("long"), 0);

		// test with float value - can not be cast exactly back to double!
		f = (float) 4.777;
		itemCollection.replaceItemValue("float", f);
		Assert.assertFalse(itemCollection.isItemValueDouble("float"));

		// test String....
		d = 9.712;
		itemCollection.replaceItemValue("double", d);
		Assert.assertEquals("9.712",
				itemCollection.getItemValueString("double"));

		// test cast back from string....
		s = "9.723";
		itemCollection.replaceItemValue("string", s);
		Assert.assertFalse(itemCollection.isItemValueDouble("string"));
		Assert.assertEquals(9.723, itemCollection.getItemValueDouble("string"),
				0);

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
		Assert.assertTrue(itemCollection.isItemValueFloat("float"));
		Float fTest = (float) 4.777;
		Assert.assertTrue(fTest.equals(itemCollection
				.getItemValueFloat("float")));

		// test cast back from string....
		s = "9.723";
		itemCollection.replaceItemValue("string", s);
		Assert.assertFalse(itemCollection.isItemValueDouble("string"));
		// not possible to cast back exactly!
		// Assert.assertEquals(9.723,
		// itemCollection.getItemValueFloat("string"),
		// 0);

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
		Assert.assertTrue(itemCollection.isItemValueInteger("integer"));
		Assert.assertEquals(7, itemCollection.getItemValueInteger("integer"), 0);

		// test long
		l = (long) 8;
		itemCollection.replaceItemValue("long", l);
		Assert.assertFalse(itemCollection.isItemValueInteger("long"));
		Assert.assertTrue(itemCollection.isItemValueLong("long"));
		Assert.assertEquals(8, itemCollection.getItemValueInteger("long"), 0);

		// test long in vector
		v = new Vector();
		l = (long) 8;
		v.add(l);
		itemCollection.replaceItemValue("long", v);
		// should return false...
		Assert.assertFalse(itemCollection.isItemValueInteger("long"));
		Assert.assertTrue(itemCollection.isItemValueLong("long"));
		Assert.assertEquals(8, itemCollection.getItemValueInteger("long"), 0);

		// test double
		d = 8.765;
		itemCollection.replaceItemValue("double", d);
		Assert.assertFalse(itemCollection.isItemValueInteger("double"));
		Assert.assertTrue(itemCollection.isItemValueDouble("double"));
		Assert.assertEquals(8, itemCollection.getItemValueInteger("double"), 0);

		// test float
		f = (float) 8.765;
		itemCollection.replaceItemValue("float", f);
		Assert.assertFalse(itemCollection.isItemValueInteger("float"));
		Assert.assertTrue(itemCollection.isItemValueFloat("float"));
		Assert.assertEquals(8, itemCollection.getItemValueInteger("float"), 0);

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
		itemCollection1.replaceItemValue("numID", new Integer(20));
		itemCollection2.replaceItemValue("numID", new Integer(20));

		Assert.assertEquals(itemCollection1, itemCollection2);
		Assert.assertNotSame(itemCollection1, itemCollection2);

		// compare not equals
		itemCollection1.replaceItemValue("txtName", "Manfred");
		itemCollection2.replaceItemValue("txtname", "Manfred1");

		Assert.assertFalse(itemCollection1.equals(itemCollection2));
		itemCollection2.replaceItemValue("txtname", "Manfred");
		itemCollection2.replaceItemValue("numID", new Integer(21));
		Assert.assertFalse(itemCollection1.equals(itemCollection2));
		Assert.assertNotSame(itemCollection1, itemCollection2);

	}

	/**
	 * This test verifies the behavior when copy the elements of another
	 * ItemCollection
	 * 
	 */
	@Test
	public void testCopyValues() {

		ItemCollection itemCollection1 = new ItemCollection();
		ItemCollection itemCollection2 = new ItemCollection();

		itemCollection1.replaceItemValue("txtName", "Manfred");
		itemCollection1.replaceItemValue("numID", new Integer(20));

		// copy values
		itemCollection2.replaceAllItems(itemCollection1.getAllItems());
		Assert.assertEquals(itemCollection1, itemCollection2);
		Assert.assertNotSame(itemCollection1, itemCollection2);

		// change value of itemcol2
		itemCollection2.replaceItemValue("txtName", "Anna");
		Assert.assertEquals("Manfred",
				itemCollection1.getItemValueString("txtName"));
		Assert.assertEquals("Anna",
				itemCollection2.getItemValueString("txtName"));

		Assert.assertFalse(itemCollection1.equals(itemCollection2));
		Assert.assertNotSame(itemCollection1, itemCollection2);

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
		itemCollection1.replaceItemValue("numID", new Integer(20));

		ItemCollection itemCollection2 = new ItemCollection(itemCollection1);

		Assert.assertEquals(itemCollection1, itemCollection2);
		Assert.assertNotSame(itemCollection1, itemCollection2);

		// change valud of itemcol2
		itemCollection2.replaceItemValue("txtName", "Anna");
		Assert.assertEquals("Manfred",
				itemCollection1.getItemValueString("txtName"));
		Assert.assertEquals("Anna",
				itemCollection2.getItemValueString("txtName"));

	}

	/**
	 * This test verifies the behavior when copy the elements of another
	 * ItemCollection with embedded collections!
	 * 
	 */
	@SuppressWarnings("unused")
	@Test
	public void testCopyValuesWithEmbeddedCollection() {

		ItemCollection itemCollection1 = new ItemCollection();
		ItemCollection itemCollection2 = new ItemCollection();
		ItemCollection child1 = new ItemCollection();
		ItemCollection child2 = new ItemCollection();

		itemCollection1.replaceItemValue("txtName", "Manfred");
		itemCollection1.replaceItemValue("numID", new Integer(20));

		child1.replaceItemValue("txtName", "Thor");
		child1.replaceItemValue("numID", new Integer(2));

		try {
			itemCollection1.replaceItemValue("child",
					XMLItemCollectionAdapter.putItemCollection(child1));
		} catch (Exception e) {

			Assert.fail();
		}

		// copy values
		itemCollection2.replaceAllItems(itemCollection1.getAllItems());
		Assert.assertEquals(itemCollection1, itemCollection2);
		Assert.assertNotSame(itemCollection1, itemCollection2);

		XMLItemCollection xmlChild = (XMLItemCollection) itemCollection1
				.getItemValue("child").get(0);

		ItemCollection testChild = XMLItemCollectionAdapter
				.getItemCollection(xmlChild);
		Assert.assertEquals(2, testChild.getItemValueInteger("numID"));

		// manipulate child1 and repeat the test!
		child1.replaceItemValue("numID", new Integer(3));
		Assert.assertEquals(itemCollection1, itemCollection2);
		Assert.assertNotSame(itemCollection1, itemCollection2);

		// Now its the same object !
		Assert.assertNotSame(child1, testChild);

		// test child1 form itemcol1
		 xmlChild = (XMLItemCollection) itemCollection1
				.getItemValue("child").get(0);

		 testChild = XMLItemCollectionAdapter
				.getItemCollection(xmlChild);
		
		// expected id is not 3!
		Assert.assertEquals(2, testChild.getItemValueInteger("numID"));

		// change value of itemcol2
		itemCollection2.replaceItemValue("txtName", "Anna");
		Assert.assertEquals("Manfred",
				itemCollection1.getItemValueString("txtName"));
		Assert.assertEquals("Anna",
				itemCollection2.getItemValueString("txtName"));

		Assert.assertFalse(itemCollection1.equals(itemCollection2));
		Assert.assertNotSame(itemCollection1, itemCollection2);

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
		itemCollection1.replaceItemValue("numID", new Integer(20));

		child1.replaceItemValue("txtName", "Thor");
		child1.replaceItemValue("numID", new Integer(2));
		child2.replaceItemValue("txtName", "Ilias");
		child2.replaceItemValue("numID", new Integer(3));
		Vector childs = new Vector();
		childs.add(child1);
		childs.add(child2);
		itemCollection1.replaceItemValue("childs", childs);

		// copy values
		itemCollection2.replaceAllItems(itemCollection1.getAllItems());
		Assert.assertEquals(itemCollection1, itemCollection2);
		Assert.assertNotSame(itemCollection1, itemCollection2);

		// now manipulate child1
		child1.replaceItemValue("numID", 101);

		// repeat test!
		Assert.assertEquals(itemCollection1, itemCollection2);
		Assert.assertNotSame(itemCollection1, itemCollection2);

		// get child 1 from itemcol2
		Vector v = (Vector) itemCollection2.getItemValue("childs");
		ItemCollection ct1 = (ItemCollection) v.elementAt(0);
		Assert.assertEquals("Thor", ct1.getItemValueString("txtName"));
		Assert.assertEquals(101, ct1.getItemValueInteger("numID"));

		Assert.assertEquals(101, child1.getItemValueInteger("numID"));

		ct1 = (ItemCollection) v.elementAt(1);
		Assert.assertEquals("Ilias", ct1.getItemValueString("txtName"));
		Assert.assertEquals(3, ct1.getItemValueInteger("numID"));

		// test size
		Assert.assertTrue(v.size() == 2);

	}

	/**
	 * Same as testCopyValuesWithEmbeddedCollection but now we test the behavior
	 * with map interfaces
	 * 
	 * here we can see that a map is copied by reference!
	 * 
	 */
	@SuppressWarnings({ "unused", "unchecked", "rawtypes" })
	@Test
	public void testCopyValuesWithEmbeddedMap() {

		ItemCollection itemCollection1 = new ItemCollection();
		ItemCollection itemCollection2 = new ItemCollection();
		Map child1 = new HashMap();
		Map child2 = new HashMap();

		itemCollection1.replaceItemValue("txtName", "Manfred");
		itemCollection1.replaceItemValue("numID", new Integer(20));

		child1.put("txtName", "Thor");
		child1.put("numID", new Integer(2));

		itemCollection1.replaceItemValue("child", child1);

		// copy values
		itemCollection2.replaceAllItems(itemCollection1.getAllItems());
		Assert.assertEquals(itemCollection1, itemCollection2);
		Assert.assertNotSame(itemCollection1, itemCollection2);
		Map testChild = (Map) itemCollection1.getItemValue("child").get(0);
		Assert.assertEquals(2, testChild.get("numID"));

		// manipulate child1 and repeat the test!
		child1.put("numID", new Integer(3));
		Assert.assertEquals(itemCollection1, itemCollection2);
		Assert.assertNotSame(itemCollection1, itemCollection2);

		// its the same object !
		Assert.assertSame(child1, testChild);

	}
	
	
	
	
	
	/**
	 * This test verifies the imixs workflow basic attributes
	 * 
	 */
	@Test
	public void testBasicAttributes() {

		ItemCollection itemCollection1 = new ItemCollection();
	
		itemCollection1.replaceItemValue(WorkflowKernel.MODELVERSION, "1.0.0");
		itemCollection1.replaceItemValue(WorkflowKernel.PROCESSID, 100);
		itemCollection1.replaceItemValue(WorkflowKernel.ACTIVITYID, 10);
		itemCollection1.replaceItemValue(WorkflowKernel.TYPE,"workitem_test");
		itemCollection1.replaceItemValue(WorkflowKernel.UNIQUEID, "ABC-123");

		
		Assert.assertEquals("1.0.0",itemCollection1.getModelVersion());
		Assert.assertEquals(10,itemCollection1.getActivityID());
		Assert.assertEquals(100,itemCollection1.getProcessID());
		Assert.assertEquals("workitem_test",itemCollection1.getType());
		Assert.assertEquals("ABC-123",itemCollection1.getUniqueID());

	}


}
