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

package org.imixs.workflow.util;

import java.util.Collections;
import java.util.Map;

/**
 * Immutable value object representing a single XML tag occurrence found by
 * {@link XMLParser#parseTagMatches(String, String)}.
 *
 * <p>
 * An {@code XMLTag} carries everything a caller needs to inspect and replace a
 * tag in the original text:
 * </p>
 * <ul>
 * <li>the full tag string including open tag, inner content and close tag</li>
 * <li>the tag name (lowercase)</li>
 * <li>all attributes declared in the open tag</li>
 * <li>the inner content between the open and close tag</li>
 * <li>the exact start and end position in the original text</li>
 * </ul>
 *
 * <h2>Position-based replacement</h2>
 * <p>
 * Using {@link #getStartPos()} and {@link #getEndPos()} for replacement is
 * safer than calling {@code text.indexOf(tag.getFullMatch())}, which breaks
 * when the same tag content appears more than once in a text. The recommended
 * pattern is to iterate the result list of
 * {@link XMLParser#parseTagMatches(String, String)} in <em>reverse</em> order
 * so that replacing a tag by position does not shift the positions of tags that
 * have not yet been processed:
 * </p>
 *
 * <pre>
 * {@code
 * List<XMLTag> tags = XMLParser.parseTagMatches(text, "itemvalue");
 * for (int i = tags.size() - 1; i >= 0; i--) {
 *     XMLTag tag = tags.get(i);
 *     String replacement = resolve(tag.getContent());
 *     text = text.substring(0, tag.getStartPos())
 *             + replacement
 *             + text.substring(tag.getEndPos());
 * }
 * }
 * </pre>
 *
 * <h2>Attribute access</h2>
 * <p>
 * Attribute lookup via {@link #getAttribute(String)} is case-insensitive, so
 * {@code tag.getAttribute("ref")} and {@code tag.getAttribute("REF")} return
 * the same value.
 * </p>
 *
 * @see XMLParser#parseTagMatches(String, String)
 * @author rsoika
 */
public final class XMLTag {

    /** The complete tag string: open tag + inner content + close tag. */
    private final String outerXML;

    /** The tag name in lowercase (e.g. {@code "itemvalue"}). */
    private final String name;

    /**
     * All attributes declared in the open tag, keyed by their original name. Lookup
     * is case-insensitive via {@link #getAttribute(String)}.
     */
    private final Map<String, String> attributes;

    /** The raw inner content between the open tag and its matching close tag. */
    private final String content;

    /** Inclusive start position of {@link #outerXML} in the original text. */
    private final int startPos;

    /** Exclusive end position of {@link #outerXML} in the original text. */
    private final int endPos;

    /**
     * Creates a new {@code XMLTag}. Called exclusively by {@link XMLParser}.
     *
     * @param fullMatch  the complete tag string
     * @param name       the tag name (lowercase)
     * @param attributes all attributes parsed from the open tag
     * @param content    the inner content between open and close tag
     * @param startPos   inclusive start position in the original text
     * @param endPos     exclusive end position in the original text
     */
    XMLTag(String fullMatch, String name, Map<String, String> attributes,
            String content, int startPos, int endPos) {
        this.outerXML = fullMatch;
        this.name = name;
        // Wrap in unmodifiable map to preserve immutability
        this.attributes = Collections.unmodifiableMap(attributes);
        this.content = content;
        this.startPos = startPos;
        this.endPos = endPos;
    }

    /**
     * Returns the complete tag string including the open tag, inner content and
     * close tag, exactly as it appeared in the original text.
     *
     * @return full tag string, never {@code null}
     */
    public String getOuterXML() {
        return outerXML;
    }

    /**
     * Returns the tag name in lowercase.
     *
     * @return tag name, never {@code null}
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the raw inner content between the open tag and its matching close
     * tag. For self-closing tags ({@code <tag/>}) and explicit empty tags
     * ({@code <tag></tag>}) this is an empty string.
     *
     * @return inner content, never {@code null}
     */
    public String getContent() {
        return content;
    }

    /**
     * Returns the inclusive start position of this tag in the original text. Use
     * together with {@link #getEndPos()} for position-based replacement.
     *
     * @return start position (inclusive)
     */
    public int getStartPos() {
        return startPos;
    }

    /**
     * Returns the exclusive end position of this tag in the original text. Use
     * together with {@link #getStartPos()} for position-based replacement.
     *
     * @return end position (exclusive)
     */
    public int getEndPos() {
        return endPos;
    }

    /**
     * Returns the value of a named attribute, or {@code null} if the attribute is
     * not present in the open tag.
     *
     * <p>
     * The lookup is <em>case-insensitive</em>: {@code getAttribute("ref")},
     * {@code getAttribute("REF")} and {@code getAttribute("Ref")} all return the
     * same value.
     * </p>
     *
     * @param name the attribute name to look up
     * @return the attribute value, or {@code null} if absent
     */
    public String getAttribute(String name) {
        if (name == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Returns an unmodifiable view of all attributes declared in the open tag. Keys
     * preserve their original capitalisation as written in the source text. Use
     * {@link #getAttribute(String)} for case-insensitive lookup.
     *
     * @return unmodifiable map of attribute name to attribute value
     */
    public Map<String, String> getAttributes() {
        return attributes;
    }

    /**
     * Returns {@code true} if this tag has no inner content, i.e. it is either a
     * self-closing tag ({@code <tag/>}) or an explicit empty tag
     * ({@code <tag></tag>}).
     *
     * @return {@code true} if the content is empty
     */
    public boolean isEmpty() {
        return content.isEmpty();
    }

    @Override
    public String toString() {
        return "XMLTag{name='" + name + "', content='" + content
                + "', startPos=" + startPos + ", endPos=" + endPos + "}";
    }
}