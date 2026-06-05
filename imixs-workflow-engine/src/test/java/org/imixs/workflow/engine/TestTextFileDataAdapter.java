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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.imixs.workflow.FileData;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.handler.TextFileDataAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for TextFileDataAdapter.
 *
 * The tests use FileData objects with pre-populated "text" metadata to simulate
 * attachments whose content has already been extracted by an OCR or PDF-to-text
 * pipeline.
 */
public class TestTextFileDataAdapter {

    private TextFileDataAdapter adapter;
    private ItemCollection document;

    @BeforeEach
    public void setUp() {
        adapter = new TextFileDataAdapter();
        document = new ItemCollection();
    }

    // =========================================================================
    // Helper
    // =========================================================================

    /**
     * Creates a FileData object with a pre-extracted text content stored in the
     * "text" metadata item — this simulates what an OCR/PDF pipeline produces.
     *
     * @param fileName    the attachment file name
     * @param textContent the pre-extracted plain text content
     * @return FileData ready to be added to an ItemCollection
     */
    private FileData createFileData(String fileName, String textContent) {
        Map<String, List<Object>> attributes = new HashMap<>();
        attributes.put("text", java.util.Arrays.asList(textContent));
        return new FileData(fileName, null, "application/octet-stream", attributes);
    }
    // =========================================================================
    // Basic replacement
    // =========================================================================

    @Test
    void testSimpleFileMatch() {
        document.addFileData(createFileData("invoice.pdf", "Invoice total: 1000 EUR"));
        String text = "Content: <filedata>invoice\\.pdf</filedata>";

        TextEvent event = new TextEvent(text, document);
        adapter.onEvent(event);

        assertEquals("Content: Invoice total: 1000 EUR", event.getText());
    }

    @Test
    void testNoTagLeavesTextUnchanged() {
        document.addFileData(createFileData("invoice.pdf", "Invoice total: 1000 EUR"));
        String text = "No filedata tag here.";

        TextEvent event = new TextEvent(text, document);
        adapter.onEvent(event);

        assertEquals("No filedata tag here.", event.getText());
    }

    @Test
    void testNullTextIsHandled() {
        TextEvent event = new TextEvent(null, document);
        adapter.onEvent(event); // must not throw
    }

    // =========================================================================
    // Pattern matching
    // =========================================================================

    @Test
    void testPatternMatchesMultipleFiles() {
        document.addFileData(createFileData("invoice_2024.pdf", "Invoice 2024"));
        document.addFileData(createFileData("contract.docx", "Contract text"));
        document.addFileData(createFileData("report_2024.pdf", "Report 2024"));

        // Pattern matches all PDFs
        String text = "<filedata>^.+\\.pdf$</filedata>";

        TextEvent event = new TextEvent(text, document);
        adapter.onEvent(event);

        String result = event.getText();
        assertTrue(result.contains("Invoice 2024"));
        assertTrue(result.contains("Report 2024"));
        // The docx file must NOT be included
        assertTrue(!result.contains("Contract text"));
    }

    @Test
    void testPatternMatchAllFiles() {
        document.addFileData(createFileData("invoice.pdf", "Invoice text"));
        document.addFileData(createFileData("contract.docx", "Contract text"));

        String text = "<filedata>.*</filedata>";

        TextEvent event = new TextEvent(text, document);
        adapter.onEvent(event);

        String result = event.getText();
        assertTrue(result.contains("Invoice text"));
        assertTrue(result.contains("Contract text"));
    }

    @Test
    void testNoMatchingFileProducesEmptyString() {
        document.addFileData(createFileData("invoice.pdf", "Invoice text"));

        // Pattern does not match any file
        String text = "Before: <filedata>nonexistent\\.pdf</filedata> After";

        TextEvent event = new TextEvent(text, document);
        adapter.onEvent(event);

        // Tag is replaced with empty string — no exception
        assertEquals("Before:  After", event.getText());
    }

    @Test
    void testFileWithNoTextMetadataIsSkipped() {
        // File exists but has no "text" metadata item
        document.addFileData(new FileData("image.png", null, "image/png", null));
        String text = "<filedata>image\\.png</filedata>";

        TextEvent event = new TextEvent(text, document);
        adapter.onEvent(event);

        // Replaced with empty string — no exception
        assertEquals("", event.getText());
    }

    @Test
    void testInvalidPatternProducesEmptyString() {
        document.addFileData(createFileData("invoice.pdf", "Invoice text"));

        // Invalid regex pattern
        String text = "<filedata>[invalid</filedata>";

        TextEvent event = new TextEvent(text, document);
        adapter.onEvent(event); // must not throw

        assertEquals("", event.getText());
    }

    // =========================================================================
    // Separator
    // =========================================================================

    @Test
    void testDefaultSeparatorBetweenMultipleFiles() {
        document.addFileData(createFileData("doc1.pdf", "Content A"));
        document.addFileData(createFileData("doc2.pdf", "Content B"));

        String text = "<filedata>^.+\\.pdf$</filedata>";

        TextEvent event = new TextEvent(text, document);
        adapter.onEvent(event);

        // Default separator is \n\n
        assertEquals("Content A\n\nContent B", event.getText());
    }

    @Test
    void testCustomSeparator() {
        document.addFileData(createFileData("doc1.pdf", "Content A"));
        document.addFileData(createFileData("doc2.pdf", "Content B"));

        String text = "<filedata separator=\"\\n---\\n\">^.+\\.pdf$</filedata>";

        TextEvent event = new TextEvent(text, document);
        adapter.onEvent(event);

        assertEquals("Content A\n---\nContent B", event.getText());
    }

    @Test
    void testSingleFileHasNoSeparator() {
        document.addFileData(createFileData("invoice.pdf", "Invoice text"));
        document.addFileData(createFileData("contract.docx", "Contract text"));

        // Only the PDF matches — no separator needed
        String text = "<filedata separator=\"---\">invoice\\.pdf</filedata>";

        TextEvent event = new TextEvent(text, document);
        adapter.onEvent(event);

        assertEquals("Invoice text", event.getText());
    }

    // =========================================================================
    // Multiple tags in one template
    // =========================================================================

    @Test
    void testMultipleTagsInTemplate() {
        document.addFileData(createFileData("invoice.pdf", "Invoice text"));
        document.addFileData(createFileData("contract.pdf", "Contract text"));

        String text = "Invoice: <filedata>invoice\\.pdf</filedata>\n"
                + "Contract: <filedata>contract\\.pdf</filedata>";

        TextEvent event = new TextEvent(text, document);
        adapter.onEvent(event);

        assertEquals("Invoice: Invoice text\nContract: Contract text", event.getText());
    }

    @Test
    void testDuplicateTagsWithSamePattern() {
        // The classic indexOf() bug scenario — same pattern twice
        document.addFileData(createFileData("invoice.pdf", "Invoice text"));

        String text = "<filedata>invoice\\.pdf</filedata> and <filedata>invoice\\.pdf</filedata>";

        TextEvent event = new TextEvent(text, document);
        adapter.onEvent(event);

        assertEquals("Invoice text and Invoice text", event.getText());
    }

    // =========================================================================
    // CDATA — tags inside CDATA must not be processed
    // =========================================================================

    @Test
    void testFileDataInsideCdataIsIgnored() {
        document.addFileData(createFileData("invoice.pdf", "Invoice text"));

        String text = "<![CDATA[<filedata>invoice\\.pdf</filedata>]]>"
                + " <filedata>invoice\\.pdf</filedata>";

        TextEvent event = new TextEvent(text, document);
        adapter.onEvent(event);

        assertEquals("<![CDATA[<filedata>invoice\\.pdf</filedata>]]> Invoice text",
                event.getText());
    }

    // =========================================================================
    // No attachments
    // =========================================================================

    @Test
    void testNoAttachmentsInWorkitem() {
        // Workitem has no file attachments at all
        String text = "Content: <filedata>.*</filedata>";

        TextEvent event = new TextEvent(text, document);
        adapter.onEvent(event);

        // Replaced with empty string — no exception
        assertEquals("Content: ", event.getText());
    }
}
