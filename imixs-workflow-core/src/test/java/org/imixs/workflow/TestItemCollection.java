package org.imixs.workflow;

import java.util.Vector;

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
		d=9.712;
		itemCollection.replaceItemValue("double", d);
		Assert.assertEquals("9.712", itemCollection.getItemValueString("double"));
		
		// test cast back from string....
		s="9.723";
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
		s="9.723";
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

}
