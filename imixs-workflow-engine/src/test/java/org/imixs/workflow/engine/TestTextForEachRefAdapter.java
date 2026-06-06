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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.handler.TextForEachRefAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for TextForEachRefAdapter.
 *
 * The adapter iterates over all uniqueIds stored in a specified item of the
 * current workitem, loads each referenced workitem via the DocumentService, and
 * expands the for-each-ref block once per loaded workitem.
 *
 * Because the adapter depends on DocumentService (a CDI/EJB bean), tests use
 * Mockito to stub the service — no real database or container is required.
 *
 * Test coverage: - Single referenced workitem produces one expanded block -
 * Multiple references produce one block each, concatenated - A reference whose
 * uniqueId cannot be loaded is silently skipped - An empty reference list
 * produces no output - Fields of the referenced workitem are accessible inside
 * the block - Fields of the original workitem are NOT visible inside the block
 * (the block runs in the context of the referenced workitem) - Text without
 * for-each-ref tags is left unchanged - Null text is handled without throwing -
 * Multiple for-each-ref blocks in one text are all expanded - CDATA sections
 * must not be matched
 */
public class TestTextForEachRefAdapter {

    private TextForEachRefAdapter adapter;
    private DocumentService documentService;
    private ItemCollection mainDocument;

    @BeforeEach
    public void setUp() {
        documentService = mock(DocumentService.class);

        adapter = new TextForEachRefAdapter();
        // Inject the mocked DocumentService via reflection (field injection)
        try {
            java.lang.reflect.Field f = TextForEachRefAdapter.class.getDeclaredField("documentService");
            f.setAccessible(true);
            f.set(adapter, documentService);
        } catch (Exception e) {
            throw new RuntimeException("Could not inject DocumentService mock", e);
        }

        mainDocument = new ItemCollection();
    }

    // =========================================================================
    // Basic iteration over referenced workitems
    // =========================================================================

    /**
     * A single reference must produce exactly one expanded block containing the
     * field values of the referenced workitem.
     */
    @Test
    void testSingleReference() {
        ItemCollection ref = new ItemCollection();
        ref.setItemValue("_orderid", "A123");
        ref.setItemValue("_price", "50.55");
        when(documentService.load("uid-001")).thenReturn(ref);

        mainDocument.setItemValue("_orderitems", "uid-001");
        String text = "<for-each-ref item=\"_orderitems\">"
                + "<itemvalue>_orderid</itemvalue>: <itemvalue>_price</itemvalue>"
                + "</for-each-ref>";

        TextEvent event = new TextEvent(text, mainDocument);
        adapter.onEvent(event);

        assertEquals("A123: 50.55", event.getText());
    }

    /**
     * Multiple references must each produce one block, concatenated in order.
     */
    @Test
    void testMultipleReferences() {
        ItemCollection ref1 = new ItemCollection();
        ref1.setItemValue("_orderid", "A123");
        ref1.setItemValue("_price", "50.55");

        ItemCollection ref2 = new ItemCollection();
        ref2.setItemValue("_orderid", "B456");
        ref2.setItemValue("_price", "150.10");

        when(documentService.load("uid-001")).thenReturn(ref1);
        when(documentService.load("uid-002")).thenReturn(ref2);

        mainDocument.setItemValue("_orderitems", Arrays.asList("uid-001", "uid-002"));
        String text = "<for-each-ref item=\"_orderitems\">"
                + "<itemvalue>_orderid</itemvalue>: <itemvalue>_price</itemvalue>\n"
                + "</for-each-ref>";

        TextEvent event = new TextEvent(text, mainDocument);
        adapter.onEvent(event);

        assertEquals("A123: 50.55\nB456: 150.10\n", event.getText());
    }

    // =========================================================================
    // Context isolation — the block runs in the referenced workitem's context
    // =========================================================================

    /**
     * Items that exist only in the main document must NOT be visible inside the
     * block, because the block is evaluated against the referenced workitem.
     */
    @Test
    void testBlockContextIsReferencedWorkitem() {
        ItemCollection ref = new ItemCollection();
        ref.setItemValue("_refitem", "REF_VALUE");
        when(documentService.load("uid-ref")).thenReturn(ref);

        // This item exists only on the main document, not on the referenced one
        mainDocument.setItemValue("_mainonly", "MAIN_VALUE");
        mainDocument.setItemValue("_orderitems", "uid-ref");

        // Request an item that only lives on the main document
        String text = "<for-each-ref item=\"_orderitems\">"
                + "<itemvalue>_mainonly</itemvalue>"
                + "</for-each-ref>";

        TextEvent event = new TextEvent(text, mainDocument);
        adapter.onEvent(event);

        // The referenced workitem does not have _mainonly — result must be empty
        assertEquals("", event.getText());
    }

    // =========================================================================
    // Missing / unresolvable references
    // =========================================================================

    /**
     * A reference whose uniqueId cannot be loaded (documentService returns null)
     * must be silently skipped — no exception, no placeholder in the output.
     */
    @Test
    void testMissingReferenceIsSkipped() {
        when(documentService.load(anyString())).thenReturn(null);

        mainDocument.setItemValue("_orderitems", Arrays.asList("uid-missing-1", "uid-missing-2"));
        String text = "<for-each-ref item=\"_orderitems\">BLOCK</for-each-ref>";

        TextEvent event = new TextEvent(text, mainDocument);
        adapter.onEvent(event);

        assertEquals("", event.getText());
    }

    /**
     * If only one of two references can be loaded, only that one must appear in the
     * output; the missing one is skipped.
     */
    @Test
    void testPartiallyMissingReferences() {
        ItemCollection ref = new ItemCollection();
        ref.setItemValue("_orderid", "FOUND");
        when(documentService.load("uid-ok")).thenReturn(ref);
        when(documentService.load("uid-missing")).thenReturn(null);

        mainDocument.setItemValue("_orderitems", Arrays.asList("uid-ok", "uid-missing"));
        String text = "<for-each-ref item=\"_orderitems\">"
                + "<itemvalue>_orderid</itemvalue>"
                + "</for-each-ref>";

        TextEvent event = new TextEvent(text, mainDocument);
        adapter.onEvent(event);

        assertEquals("FOUND", event.getText());
    }

    /**
     * An empty reference list (item not set) must produce no output.
     */
    @Test
    void testEmptyReferenceListProducesNoOutput() {
        // _orderitems is not set on mainDocument → empty list
        String text = "<for-each-ref item=\"_orderitems\">BLOCK</for-each-ref>";

        TextEvent event = new TextEvent(text, mainDocument);
        adapter.onEvent(event);

        assertEquals("", event.getText());
    }

    // =========================================================================
    // Multiple blocks in one text
    // =========================================================================

    /**
     * Two independent for-each-ref blocks in one text must both be expanded, even
     * when they reference the same item (covers the duplicate-tag-position scenario
     * analogous to the indexOf() bug in TextItemValueAdapter).
     */
    @Test
    void testMultipleBlocksInOneText() {
        ItemCollection ref = new ItemCollection();
        ref.setItemValue("_label", "ITEM");
        when(documentService.load("uid-x")).thenReturn(ref);

        mainDocument.setItemValue("_refs", "uid-x");
        String text = "A: <for-each-ref item=\"_refs\"><itemvalue>_label</itemvalue></for-each-ref>"
                + " B: <for-each-ref item=\"_refs\"><itemvalue>_label</itemvalue></for-each-ref>";

        TextEvent event = new TextEvent(text, mainDocument);
        adapter.onEvent(event);

        assertEquals("A: ITEM B: ITEM", event.getText());
    }

    // =========================================================================
    // Edge cases — null / no tags
    // =========================================================================

    /**
     * Text that contains no for-each-ref tags must be returned unchanged.
     */
    @Test
    void testTextWithoutTagsIsUnchanged() {
        String text = "No tags here.";
        TextEvent event = new TextEvent(text, mainDocument);
        adapter.onEvent(event);

        assertEquals("No tags here.", event.getText());
    }

    /**
     * A null text must be handled gracefully without throwing an exception.
     */
    @Test
    void testNullTextIsHandled() {
        TextEvent event = new TextEvent(null, mainDocument);
        adapter.onEvent(event); // must not throw
    }

    // =========================================================================
    // CDATA protection
    // =========================================================================

    /**
     * A for-each-ref tag inside a CDATA section must not be processed; only the tag
     * outside CDATA must be expanded.
     */
    @Test
    void testForEachRefInsideCdataIsIgnored() {
        ItemCollection ref = new ItemCollection();
        ref.setItemValue("_orderid", "A1");
        when(documentService.load("uid-001")).thenReturn(ref);

        mainDocument.setItemValue("_orderitems", "uid-001");
        String text = "<![CDATA[<for-each-ref item=\"_orderitems\"><itemvalue>_orderid</itemvalue></for-each-ref>]]>"
                + "<for-each-ref item=\"_orderitems\"><itemvalue>_orderid</itemvalue></for-each-ref>";

        TextEvent event = new TextEvent(text, mainDocument);
        adapter.onEvent(event);

        assertTrue(event.getText().startsWith("<![CDATA[<for-each-ref"),
                "CDATA content must not be modified");
        assertTrue(event.getText().endsWith("A1"),
                "Block outside CDATA must be expanded");
    }
}