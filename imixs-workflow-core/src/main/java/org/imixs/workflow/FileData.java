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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.xml.bind.DatatypeConverter;

/**
 * Helper class to abstract the file content stored in a ItemCollection.
 * <p>
 * A FileData object contains at least the attributes 'name', 'content' and
 * 'contentType'. The optional object custom attributes can be added. It
 * represents a {@code Map<String, List<Object>>}
 * 
 * @see ItemCollection addFile
 * @author rsoika
 * @version 2.0
 */
public class FileData {
    private String name;
    private byte[] content;
    private String contentType;
    private Map<String, List<Object>> attributes;

    public static final String DEFAULT_CONTENT_TYPE = "application/unknown";

    public FileData(String name, byte[] content, String contentType, Map<String, List<Object>> attributes) {
        super();
        this.content = content;
        this.setName(name);
        this.setContentType(contentType);
        this.setAttributes(attributes);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        // validate contentType...
        if (contentType == null || "".equals(contentType)) {
            contentType = DEFAULT_CONTENT_TYPE;
        }
        this.contentType = contentType;
    }

    public Map<String, List<Object>> getAttributes() {
        if (attributes == null) {
            attributes = new LinkedHashMap<String, List<Object>>();
        }
        return attributes;
    }

    public void setAttributes(Map<String, List<Object>> attributes) {
        this.attributes = attributes;
    }

    /**
     * Returns the value of the named custom attribute as an Object, or null if no
     * attribute of the given name exists. A custom attribute can be set by the
     * method setAttribute().
     * 
     * @param name a String specifying the name of the custom attribute
     * @return: an Object containing the value of the attribute, or null if the
     *          attribute does not exist
     **/
    public Object getAttribute(String name) {
        if (attributes != null) {
            return attributes.get(name);
        } else {
            return null;
        }
    }

    /**
     * Set a custom attribute value.
     * 
     * @param name   a String specifying the name of the custom attribute
     * @param values an Object containing the value of the attribute
     */
    public void setAttribute(String name, List<Object> values) {
        if (attributes == null) {
            attributes = new LinkedHashMap<String, List<Object>>();
        }
        attributes.put(name, values);
    }

    /**
     * Generates a MD5 from a current file content
     * 
     * @throws NoSuchAlgorithmException - invalid algorithm
     * @return md5 string
     */
    public String generateMD5() throws NoSuchAlgorithmException {
        byte[] hash_bytes = MessageDigest.getInstance("MD5").digest(content);
        return DatatypeConverter.printHexBinary(hash_bytes);
    }

    /**
     * Validates a given MD5 checksum
     * 
     * @param checksum - checksum to validate
     * @throws NoSuchAlgorithmException - invalid algorithm
     * @return true if equal
     */
    public boolean validateMD5(String checksum) throws NoSuchAlgorithmException {
        String testChecksum = generateMD5();
        return (testChecksum.equals(checksum));
    }
}
