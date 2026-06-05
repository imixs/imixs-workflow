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

package org.imixs.workflow.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.handler.TextItemValueAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for TextItemValueAdapter.
 *
 * These tests focus on the cases that required the refactoring from
 * indexOf()-based replacement to position-based replacement via XMLTag:
 *
 * - Simple tag replacement - Duplicate tag content (the classic indexOf() bug)
 * - Multiple different tags in one text - Attributes: format, separator,
 * position, locale - CDATA sections must not be matched
 */
public class TestTextItemValueAdapter {

    private TextItemValueAdapter adapter;
    private ItemCollection document;

    @BeforeEach
    public void setUp() {
        adapter = new TextItemValueAdapter();
        document = new ItemCollection();
    }

    // =========================================================================
    // Basic replacement
    // =========================================================================

    @Test
    void testSimpleReplacement() {
        document.setItemValue("customer.name", "John Doe");
        String text = "Hello <itemvalue>customer.name</itemvalue>!";

        TextEvent event = new TextEvent(text, document);
        adapter.onEvent(event);

        assertEquals("Hello John Doe!", event.getText());
    }

    @Test
    void testUnknownItemReturnsEmpty() {
        // An item that does not exist in the document returns an empty string
        String text = "Hello <itemvalue>unknown.item</itemvalue>!";

        TextEvent event = new TextEvent(text, document);
        adapter.onEvent(event);

        assertEquals("Hello !", event.getText());
    }

    @Test
    void testMultipleDifferentTags() {
        document.setItemValue("firstname", "John");
        document.setItemValue("lastname", "Doe");
        String text = "<itemvalue>firstname</itemvalue> <itemvalue>lastname</itemvalue>";

        TextEvent event = new TextEvent(text, document);
        adapter.onEvent(event);

        assertEquals("John Doe", event.getText());
    }

    // =========================================================================
    // The indexOf() bug — duplicate tag content
    // =========================================================================

    @Test
    void testDuplicateItemNameInText() {
        // Both tags reference the same item — this was the classic indexOf() bug.
        // The old implementation always replaced the first occurrence twice.
        document.setItemValue("order.id", "A123");
        String text = "Order: <itemvalue>order.id</itemvalue> - Ref: <itemvalue>order.id</itemvalue>";

        TextEvent event = new TextEvent(text, document);
        adapter.onEvent(event);

        assertEquals("Order: A123 - Ref: A123", event.getText());
    }

    @Test
    void testDuplicateTagsWithDifferentItems() {
        document.setItemValue("item.a", "AAA");
        document.setItemValue("item.b", "BBB");
        String text = "<itemvalue>item.a</itemvalue>|<itemvalue>item.b</itemvalue>|<itemvalue>item.a</itemvalue>";

        TextEvent event = new TextEvent(text, document);
        adapter.onEvent(event);

        assertEquals("AAA|BBB|AAA", event.getText());
    }

    // =========================================================================
    // Attributes
    // =========================================================================

    @Test
    void testSeparatorAttribute() {
        document.setItemValue("phones", java.util.Arrays.asList("111", "222", "333"));
        String text = "<itemvalue separator=\", \">phones</itemvalue>";

        TextEvent event = new TextEvent(text, document);
        adapter.onEvent(event);

        assertEquals("111, 222, 333", event.getText());
    }

    @Test
    void testPositionLastAttribute() {
        document.setItemValue("values", java.util.Arrays.asList("first", "middle", "last"));
        String text = "<itemvalue position=\"last\">values</itemvalue>";

        TextEvent event = new TextEvent(text, document);
        adapter.onEvent(event);

        assertEquals("last", event.getText());
    }

    // =========================================================================
    // Deprecated uppercase tag — must still work
    // =========================================================================

    @Test
    void testDeprecatedUppercaseTag() {
        document.setItemValue("customer.name", "John Doe");
        String text = "Hello <itemValue>customer.name</itemValue>!";

        TextEvent event = new TextEvent(text, document);
        adapter.onEvent(event);

        assertEquals("Hello John Doe!", event.getText());
    }

    // =========================================================================
    // CDATA — tags inside CDATA must not be replaced
    // =========================================================================

    @Test
    void testItemValueInsideCdataIsIgnored() {
        document.setItemValue("customer.name", "John Doe");
        // The <itemvalue> inside CDATA must NOT be replaced —
        // only the one outside CDATA should be processed.
        String text = "<![CDATA[<itemvalue>customer.name</itemvalue>]]>"
                + " <itemvalue>customer.name</itemvalue>";

        TextEvent event = new TextEvent(text, document);
        adapter.onEvent(event);

        assertEquals("<![CDATA[<itemvalue>customer.name</itemvalue>]]> John Doe",
                event.getText());
    }

    // =========================================================================
    // Null / empty input
    // =========================================================================

    @Test
    void testNullTextIsHandled() {
        TextEvent event = new TextEvent(null, document);
        adapter.onEvent(event); // must not throw
    }

    @Test
    void testTextWithoutTagsIsUnchanged() {
        String text = "No tags here.";
        TextEvent event = new TextEvent(text, document);
        adapter.onEvent(event);

        assertEquals("No tags here.", event.getText());
    }
}