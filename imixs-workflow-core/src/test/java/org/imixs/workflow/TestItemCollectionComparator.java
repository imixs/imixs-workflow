/****************************************************************************
 * Copyright (c) 2022-2025 Imixs Software Solutions GmbH and others.
 * https://www.imixs.com
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * This Source Code may also be made available under the terms of the
 * GNU General Public License, version 2 or later (GPL-2.0-or-later),
 * which is available at https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-or-later
 ****************************************************************************/

package org.imixs.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Test class for itemCollectionComparator object
 * 
 * @author rsoika
 * 
 */
public class TestItemCollectionComparator {

    /**
     * Sorts a list of ItemCollections by a String item ascending.
     */
    @Test
    public void testItemCollectionStrings() {
        ItemCollection itemCollectionA = new ItemCollection();
        itemCollectionA.setItemValue("name", "Charlie");

        ItemCollection itemCollectionB = new ItemCollection();
        itemCollectionB.setItemValue("name", "Alice");

        ItemCollection itemCollectionC = new ItemCollection();
        itemCollectionC.setItemValue("name", "Bob");

        List<ItemCollection> itemCollections = new ArrayList<>();
        itemCollections.add(itemCollectionA);
        itemCollections.add(itemCollectionB);
        itemCollections.add(itemCollectionC);

        Collections.sort(itemCollections, new ItemCollectionComparator("name"));

        assertEquals("Alice", itemCollections.get(0).getItemValueString("name"));
        assertEquals("Bob", itemCollections.get(1).getItemValueString("name"));
        assertEquals("Charlie", itemCollections.get(2).getItemValueString("name"));
    }

    /**
     * Sorts a list of ItemCollections by a String item descending.
     */
    @Test
    public void testItemCollectionStringsDescending() {
        ItemCollection itemCollectionA = new ItemCollection();
        itemCollectionA.setItemValue("name", "Charlie");

        ItemCollection itemCollectionB = new ItemCollection();
        itemCollectionB.setItemValue("name", "Alice");

        ItemCollection itemCollectionC = new ItemCollection();
        itemCollectionC.setItemValue("name", "Bob");

        List<ItemCollection> itemCollections = new ArrayList<>();
        itemCollections.add(itemCollectionA);
        itemCollections.add(itemCollectionB);
        itemCollections.add(itemCollectionC);

        ItemCollectionComparator comparator = new ItemCollectionComparator("name");
        Collections.sort(itemCollections, comparator.reversed());

        assertEquals("Charlie", itemCollections.get(0).getItemValueString("name"));
        assertEquals("Bob", itemCollections.get(1).getItemValueString("name"));
        assertEquals("Alice", itemCollections.get(2).getItemValueString("name"));
    }

    /**
     * Sorts a list of ItemCollections by a Date item ascending.
     */
    @Test
    public void testLocalDateValues() {
        Date nowDate = new Date();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(nowDate);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        Date yesterdayDate = calendar.getTime();

        calendar.setTime(nowDate);
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        Date tomorrowDate = calendar.getTime();

        ItemCollection itemCollectionA = new ItemCollection();
        itemCollectionA.setItemValue("date", nowDate);

        ItemCollection itemCollectionB = new ItemCollection();
        itemCollectionB.setItemValue("date", yesterdayDate);

        ItemCollection itemCollectionC = new ItemCollection();
        itemCollectionC.setItemValue("date", tomorrowDate);

        List<ItemCollection> itemCollections = new ArrayList<>();
        itemCollections.add(itemCollectionA);
        itemCollections.add(itemCollectionB);
        itemCollections.add(itemCollectionC);

        Collections.sort(itemCollections, new ItemCollectionComparator("date"));

        assertNotNull(itemCollections);
        assertEquals(tomorrowDate, itemCollections.get(0).getItemValueDate("date"));
        assertEquals(nowDate, itemCollections.get(1).getItemValueDate("date"));
        assertEquals(yesterdayDate, itemCollections.get(2).getItemValueDate("date"));
    }

    /**
     * Verifies that null Date values are sorted to the end of the list.
     */
    @Test
    public void testDateValuesWithNull() {
        Date nowDate = new Date();

        ItemCollection itemCollectionA = new ItemCollection();
        itemCollectionA.setItemValue("date", nowDate);

        // ItemCollection without a value for 'date' -> getItemValueDate(...) returns
        // null
        ItemCollection itemCollectionB = new ItemCollection();

        List<ItemCollection> itemCollections = new ArrayList<>();
        itemCollections.add(itemCollectionB);
        itemCollections.add(itemCollectionA);

        Collections.sort(itemCollections, new ItemCollectionComparator("date"));

        assertEquals(nowDate, itemCollections.get(0).getItemValueDate("date"));
    }

    /**
     * Sorts a list of ItemCollections by an Integer item ascending.
     */
    @Test
    public void testIntegerValues() {
        ItemCollection itemCollectionA = new ItemCollection();
        itemCollectionA.setItemValue("count", 30);

        ItemCollection itemCollectionB = new ItemCollection();
        itemCollectionB.setItemValue("count", 10);

        ItemCollection itemCollectionC = new ItemCollection();
        itemCollectionC.setItemValue("count", 20);

        List<ItemCollection> itemCollections = new ArrayList<>();
        itemCollections.add(itemCollectionA);
        itemCollections.add(itemCollectionB);
        itemCollections.add(itemCollectionC);

        Collections.sort(itemCollections, new ItemCollectionComparator("count"));

        assertEquals(10, itemCollections.get(0).getItemValueInteger("count"));
        assertEquals(20, itemCollections.get(1).getItemValueInteger("count"));
        assertEquals(30, itemCollections.get(2).getItemValueInteger("count"));
    }

    /**
     * Sorts a list of ItemCollections by an Integer item descending.
     */
    @Test
    public void testIntegerValuesDescending() {
        ItemCollection itemCollectionA = new ItemCollection();
        itemCollectionA.setItemValue("count", 30);

        ItemCollection itemCollectionB = new ItemCollection();
        itemCollectionB.setItemValue("count", 10);

        ItemCollection itemCollectionC = new ItemCollection();
        itemCollectionC.setItemValue("count", 20);

        List<ItemCollection> itemCollections = new ArrayList<>();
        itemCollections.add(itemCollectionA);
        itemCollections.add(itemCollectionB);
        itemCollections.add(itemCollectionC);

        ItemCollectionComparator comparator = new ItemCollectionComparator("count");
        Collections.sort(itemCollections, comparator.reversed());

        assertEquals(30, itemCollections.get(0).getItemValueInteger("count"));
        assertEquals(20, itemCollections.get(1).getItemValueInteger("count"));
        assertEquals(10, itemCollections.get(2).getItemValueInteger("count"));
    }

}