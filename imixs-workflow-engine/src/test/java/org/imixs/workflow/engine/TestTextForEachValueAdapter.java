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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.handler.TextForEachValueAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for TextForEachValueAdapter.
 *
 * The adapter iterates over the value list of a specified item and replaces the
 * for-each-value block for each single value. Within the block, the current
 * iteration value is accessible via the item name, while all other items of the
 * original document remain accessible as well.
 *
 * Test coverage: - Single value iteration - Multi-value iteration (the primary
 * use case) - Access to other document items within the loop body - Current
 * iteration value overrides the multi-value item temporarily - Empty value list
 * produces an empty result - Unknown item name (empty list) produces no output
 * - Text without for-each-value tags is left unchanged - Null text is handled
 * without throwing - Nested itemvalue tags within the loop body - Multiple
 * for-each-value blocks in one text - CDATA sections must not be matched
 */
public class TestTextForEachValueAdapter {

    private TextForEachValueAdapter adapter;
    private ItemCollection document;

    @BeforeEach
    public void setUp() {
        // Wire the adapter manually — no CDI container available in unit tests.
        // The TextItemValueAdapter is instantiated directly inside the fallback
        // branch of TextForEachValueAdapter when textEvents is null.
        adapter = new TextForEachValueAdapter();
        document = new ItemCollection();
    }

    // =========================================================================
    // Basic iteration
    // =========================================================================

    /**
     * A single-value list must produce exactly one block in the output.
     */
    @Test
    void testSingleValueIteration() {
        document.setItemValue("_partid", "A123");
        String text = "<for-each-value item=\"_partid\">Part: <itemvalue>_partid</itemvalue></for-each-value>";

        TextEvent event = new TextEvent(text, document);
        adapter.onEvent(event);

        assertEquals("Part: A123", event.getText());
    }

    /**
     * A multi-value list must produce one block per value, concatenated.
     */
    @Test
    void testMultiValueIteration() {
        document.setItemValue("_partid", Arrays.asList("A123", "B456", "C789"));
        String text = "<for-each-value item=\"_partid\"><itemvalue>_partid</itemvalue>\n</for-each-value>";

        TextEvent event = new TextEvent(text, document);
        adapter.onEvent(event);

        assertEquals("A123\nB456\nC789\n", event.getText());
    }

    /**
     * Within the loop body, all items of the original document are accessible. The
     * iteration item is temporarily set to the current single value.
     */
    @Test
    void testAccessToOtherDocumentItems() {
        document.setItemValue("_orderid", "111222");
        document.setItemValue("_partid", Arrays.asList("A123", "B456"));
        String text = "<for-each-value item=\"_partid\">"
                + "Order: <itemvalue>_orderid</itemvalue> - Part: <itemvalue>_partid</itemvalue>\n"
                + "</for-each-value>";

        TextEvent event = new TextEvent(text, document);
        adapter.onEvent(event);

        assertEquals(
                "Order: 111222 - Part: A123\n"
                        + "Order: 111222 - Part: B456\n",
                event.getText());
    }

    /**
     * The iteration value for the current pass must shadow the full multi-value
     * list — i.e. within each block, the item returns exactly the current value,
     * not the whole list.
     */
    @Test
    void testCurrentValueShadowsMultiValueList() {
        document.setItemValue("_partid", Arrays.asList("X1", "X2"));
        // If the adapter did NOT set the current value on the temp doc, both
        // iterations would still return the first value of the full list ("X1").
        String text = "<for-each-value item=\"_partid\"><itemvalue>_partid</itemvalue>|</for-each-value>";

        TextEvent event = new TextEvent(text, document);
        adapter.onEvent(event);

        assertEquals("X1|X2|", event.getText());
    }

    // =========================================================================
    // Edge cases
    // =========================================================================

    /**
     * An item with an empty list must produce no output for the block.
     */
    @Test
    void testEmptyValueListProducesNoOutput() {
        // Do not set any value — getItemValue returns an empty list
        String text = "<for-each-value item=\"_missing\">BLOCK</for-each-value>";

        TextEvent event = new TextEvent(text, document);
        adapter.onEvent(event);

        assertEquals("", event.getText());
    }

    /**
     * Text that contains no for-each-value tags must be returned unchanged.
     */
    @Test
    void testTextWithoutTagsIsUnchanged() {
        String text = "No tags here.";
        TextEvent event = new TextEvent(text, document);
        adapter.onEvent(event);

        assertEquals("No tags here.", event.getText());
    }

    /**
     * A null text must be handled gracefully without throwing an exception.
     */
    @Test
    void testNullTextIsHandled() {
        TextEvent event = new TextEvent(null, document);
        adapter.onEvent(event); // must not throw
    }

    // =========================================================================
    // Multiple blocks in one text
    // =========================================================================

    /**
     * Two independent for-each-value blocks in one text must both be expanded
     * correctly, even when they reference different items.
     */
    @Test
    void testMultipleBlocksInOneText() {
        document.setItemValue("_colors", Arrays.asList("red", "blue"));
        document.setItemValue("_sizes", Arrays.asList("S", "L"));
        String text = "Colors: <for-each-value item=\"_colors\"><itemvalue>_colors</itemvalue> </for-each-value>"
                + "Sizes: <for-each-value item=\"_sizes\"><itemvalue>_sizes</itemvalue> </for-each-value>";

        TextEvent event = new TextEvent(text, document);
        adapter.onEvent(event);

        assertEquals("Colors: red blue Sizes: S L ", event.getText());
    }

    /**
     * Two for-each-value blocks referencing the same item must both be expanded
     * independently (analogous to the classic indexOf() duplicate-tag bug in
     * TextItemValueAdapter).
     */
    @Test
    void testDuplicateBlocksForSameItem() {
        document.setItemValue("_tags", Arrays.asList("foo", "bar"));
        String text = "<for-each-value item=\"_tags\"><itemvalue>_tags</itemvalue>,</for-each-value>"
                + "|"
                + "<for-each-value item=\"_tags\"><itemvalue>_tags</itemvalue>,</for-each-value>";

        TextEvent event = new TextEvent(text, document);
        adapter.onEvent(event);

        assertEquals("foo,bar,|foo,bar,", event.getText());
    }

    // =========================================================================
    // CDATA protection
    // =========================================================================

    /**
     * A for-each-value tag that appears inside a CDATA section must not be
     * processed; only the tag outside CDATA must be expanded.
     */
    @Test
    void testForEachValueInsideCdataIsIgnored() {
        document.setItemValue("_partid", Arrays.asList("A1", "B2"));
        String text = "<![CDATA[<for-each-value item=\"_partid\"><itemvalue>_partid</itemvalue></for-each-value>]]>"
                + "<for-each-value item=\"_partid\"><itemvalue>_partid</itemvalue> </for-each-value>";

        TextEvent event = new TextEvent(text, document);
        adapter.onEvent(event);

        assertTrue(event.getText().startsWith("<![CDATA[<for-each-value"),
                "CDATA content must not be modified");
        assertTrue(event.getText().endsWith("A1 B2 "),
                "Block outside CDATA must be expanded");
    }
}