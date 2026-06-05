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

package org.imixs.workflow.engine.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.imixs.workflow.FileData;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.TextEvent;
import org.imixs.workflow.util.XMLParser;
import org.imixs.workflow.util.XMLTag;

import jakarta.ejb.Stateless;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * The TextFileDataAdapter resolves the text content of file attachments stored
 * in a workitem and inserts it into a text template.
 *
 * <p>
 * The adapter reacts on the {@code <filedata>} tag. The tag content is treated
 * as a regular expression that is matched against the file names of all
 * attachments in the workitem. For every matching file the adapter reads the
 * item {@code "text"} from the file metadata — this item is expected to hold
 * the pre-extracted plain text content of the file (e.g. produced by an OCR or
 * PDF-to-text pipeline).
 * </p>
 *
 * <h2>Basic usage</h2>
 *
 * <pre>
 * {@code
 * <!-- match a specific file -->
 * <filedata>invoice\.pdf</filedata>
 *
 * <!-- match all PDFs -->
 * <filedata>^.+\.pdf$</filedata>
 *
 * <!-- match all attachments -->
 * <filedata>.*</filedata>
 * }
 * </pre>
 *
 * <h2>Separator</h2>
 * <p>
 * When multiple files match the pattern, their text content is concatenated. An
 * optional {@code separator} attribute controls the string inserted between
 * individual file contents. If no separator is defined a double newline
 * ({@code \n\n}) is used as default.
 * </p>
 *
 * <pre>
 * {@code
 * <filedata separator="\n---\n">^.+\.pdf$</filedata>
 * }
 * </pre>
 *
 * <h2>Cross-workitem reference</h2>
 * <p>
 * The optional {@code ref} attribute allows reading file attachments from a
 * different workitem. The attribute value is the name of an item in the current
 * workitem whose value holds the uniqueId of the referenced workitem.
 * </p>
 *
 * <pre>
 * {@code
 * <filedata ref="$workitemref">invoice\.pdf</filedata>
 * }
 * </pre>
 *
 * <h2>Behaviour when no file matches</h2>
 * <p>
 * If no attachment matches the pattern the tag is replaced with an empty string
 * and a warning is written to the log. No exception is thrown so that the rest
 * of the text template is still processed normally.
 * </p>
 *
 * @see TextItemValueAdapter
 * @author rsoika
 */
@Stateless
public class TextFileDataAdapter {

    private static final Logger logger = Logger.getLogger(TextFileDataAdapter.class.getName());

    /**
     * Default separator used between multiple file contents when none is specified.
     */
    private static final String DEFAULT_SEPARATOR = "\n\n";

    @Inject
    DocumentService documentService;

    /**
     * Reacts on CDI events of type {@link TextEvent} and replaces all
     * {@code <filedata>} tags with the text content of matching file attachments.
     */
    public void onEvent(@Observes TextEvent event) {
        boolean debug = logger.isLoggable(Level.FINE);
        String text = event.getText();

        if (text == null) {
            return;
        }

        List<XMLTag> tagList = XMLParser.parseTagMatches(text, "filedata");
        if (debug) {
            logger.log(Level.FINEST, "......{0} <filedata> tags found", tagList.size());
        }

        if (tagList.isEmpty()) {
            return;
        }

        // Cache for referenced workitems — avoids repeated DocumentService lookups
        // when the same ref appears in multiple <filedata> tags within one template.
        Map<String, ItemCollection> refCache = new HashMap<>();

        // Iterate in reverse order so that position-based replacement does not shift
        // the start/end positions of tags that have not yet been processed.
        for (int i = tagList.size() - 1; i >= 0; i--) {
            XMLTag tag = tagList.get(i);

            // The tag content is the regex pattern to match file names
            String fileNamePattern = tag.getContent().trim();

            // The separator to insert between multiple matched file contents.
            // Default is a double newline if the attribute is absent.
            String separator = tag.getAttribute("separator");
            if (separator == null) {
                separator = DEFAULT_SEPARATOR;
            }
            // Resolve escape sequences in the separator attribute value
            separator = separator.replace("\\n", "\n").replace("\\t", "\t");

            // Resolve the workitem to read attachments from —
            // either the current workitem or a referenced one via ref=
            ItemCollection documentContext = resolveRef(
                    tag.getAttribute("ref"), event.getDocument(), refCache);

            if (documentContext == null) {
                logger.log(Level.WARNING,
                        "TextFileDataAdapter: ref ''{0}'' could not be resolved — tag skipped",
                        tag.getAttribute("ref"));
                continue;
            }

            // Collect text content from all matching file attachments
            String fileContent = collectFileContent(documentContext, fileNamePattern, separator);

            if (fileContent.isEmpty()) {
                logger.log(Level.WARNING,
                        "TextFileDataAdapter: no file matching pattern ''{0}'' found in workitem ''{1}''",
                        new Object[] { fileNamePattern,
                                documentContext.getItemValueString("$uniqueid") });
            }

            // Replace tag by exact position — safe even with duplicate tag content
            text = text.substring(0, tag.getStartPos()) + fileContent + text.substring(tag.getEndPos());
        }

        event.setText(text);
    }

    /**
     * Iterates over all file attachments of {@code workitem}, matches each file
     * name against {@code fileNamePattern}, and concatenates the {@code "text"}
     * metadata item of all matching files using {@code separator}.
     *
     * <p>
     * The {@code "text"} item is expected to contain the pre-extracted plain text
     * content of the attachment (e.g. produced by an OCR or PDF-to-text pipeline).
     * Files that match the name pattern but carry no {@code "text"} metadata are
     * silently skipped.
     * </p>
     *
     * @param workitem        the workitem whose attachments are scanned
     * @param fileNamePattern regex pattern matched against file names
     * @param separator       string inserted between individual file contents
     * @return concatenated text content of all matching files, or empty string
     */
    private String collectFileContent(ItemCollection workitem, String fileNamePattern, String separator) {
        StringBuilder result = new StringBuilder();

        // Compile the filename regex — log a warning and bail out on invalid patterns
        Pattern pattern;
        try {
            pattern = Pattern.compile(fileNamePattern);
        } catch (PatternSyntaxException e) {
            logger.log(Level.WARNING,
                    "TextFileDataAdapter: invalid filename pattern ''{0}'': {1}",
                    new Object[] { fileNamePattern, e.getMessage() });
            return "";
        }

        List<FileData> files = workitem.getFileData();
        if (files == null || files.isEmpty()) {
            return "";
        }

        for (FileData file : files) {
            // Skip files whose name does not match the pattern
            if (!pattern.matcher(file.getName()).find()) {
                continue;
            }

            // Read the pre-extracted text content from the file metadata
            ItemCollection metadata = new ItemCollection(file.getAttributes());
            String textContent = metadata.getItemValueString("text");

            if (textContent == null || textContent.isEmpty()) {
                logger.log(Level.FINE,
                        "......TextFileDataAdapter: file ''{0}'' matched but has no text content",
                        file.getName());
                continue;
            }

            // Append separator before each file except the first
            if (result.length() > 0) {
                result.append(separator);
            }
            result.append(textContent);
            logger.log(Level.FINEST, "......TextFileDataAdapter: added content of ''{0}''", file.getName());
        }

        return result.toString();
    }

    /**
     * Resolves the document context for a given {@code ref} attribute value.
     *
     * <p>
     * If {@code ref} is {@code null} or empty the current workitem is returned
     * directly. Otherwise {@code ref} is treated as the name of an item in the
     * current workitem whose value holds the uniqueId of the referenced workitem.
     * Loaded workitems are cached in {@code refCache} to avoid repeated DB lookups
     * within one {@code adaptText()} call.
     * </p>
     *
     * @param ref             the ref attribute value, may be {@code null}
     * @param currentWorkitem the current workitem from the {@link TextEvent}
     * @param refCache        cache map for already loaded referenced workitems
     * @return the resolved {@link ItemCollection}, or {@code null} if the ref could
     *         not be resolved
     */
    private ItemCollection resolveRef(String ref, ItemCollection currentWorkitem,
            Map<String, ItemCollection> refCache) {
        // No ref attribute — use the current workitem directly
        if (ref == null || ref.isEmpty()) {
            return currentWorkitem;
        }

        // Return from cache if already loaded during this adaptText() call
        if (refCache.containsKey(ref)) {
            return refCache.get(ref);
        }

        // documentService may be null in unit test environments without CDI
        if (documentService == null) {
            logger.warning(
                    "TextFileDataAdapter: documentService not available — ref attribute ignored");
            return currentWorkitem;
        }

        // The ref value is the name of an item in the current workitem that holds
        // the uniqueId of the referenced workitem
        String uniqueId = currentWorkitem.getItemValueString(ref);
        if (uniqueId == null || uniqueId.isEmpty()) {
            logger.log(Level.WARNING,
                    "TextFileDataAdapter: ref item ''{0}'' is empty or not found in current workitem",
                    ref);
            return null;
        }

        // Load the referenced workitem and put it into the cache
        ItemCollection referencedWorkitem = documentService.load(uniqueId);
        if (referencedWorkitem != null) {
            refCache.put(ref, referencedWorkitem);
        }
        return referencedWorkitem;
    }
}