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

package org.imixs.workflow.xml;

import java.util.List;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * The JAXB DocumentTable represents a list of documents in a table format. For
 * each document the same list of items will be added into a separate row. The
 * property labels contans the table headers.
 * 
 * 
 * @author rsoika
 * @version 2.0.0
 */
@XmlRootElement(name = "data")
public class DocumentTable implements java.io.Serializable {

    private static final long serialVersionUID = 1L;
    private XMLDocument[] document;
    private List<String> items;
    private List<String> labels;
    private String encoding;

    public DocumentTable() {
        setDocument(new XMLDocument[] {});
    }

    public DocumentTable(XMLDocument[] documents, List<String> items, List<String> labels, String encoding) {
        setDocument(documents);
        setItems(items);
        setLabels(labels);
        setEncoding(encoding);
    }

    public XMLDocument[] getDocument() {
        return document;
    }

    public void setDocument(XMLDocument[] document) {
        this.document = document;
    }

    public List<String> getItems() {
        return items;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

}
