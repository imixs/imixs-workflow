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

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * The XMLDataCollection represents a list of XMLItemCollections. This root
 * element is used by JAXB api
 * 
 * @author rsoika
 * @version 0.0.1
 */
@XmlRootElement(name = "data")
public class XMLDataCollection implements java.io.Serializable {

    private static final long serialVersionUID = 1L;
    private XMLDocument[] document;

    public XMLDataCollection() {
        setDocument(new XMLDocument[] {});
    }

    public XMLDocument[] getDocument() {
        return document;
    }

    public void setDocument(XMLDocument[] entity) {
        this.document = entity;
    }

}
