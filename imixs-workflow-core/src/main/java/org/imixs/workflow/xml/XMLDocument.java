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

import java.io.Serializable;
import java.util.Arrays;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * The XMLitemCollection is a basic serializable representation of a pojo to map
 * a org.imixs.workflow.ItemCollection into a xml representation using JAXB api
 * 
 * @author rsoika
 * @version 0.0.1
 */
@XmlRootElement(name = "document")
public class XMLDocument implements Serializable {

    private static final long serialVersionUID = 1L;
    private XMLItem[] item;

    public XMLDocument() {
        this.setItem(new XMLItem[] {});
    }

    public XMLItem[] getItem() {
        return item;
    }

    public void setItem(XMLItem[] item) {
        this.item = item;
    }

    /**
     * This method compares the item array
     */
    public boolean equals(Object o) {
        if (!(o instanceof XMLDocument))
            return false;
        return Arrays.equals(item, ((XMLDocument) o).item);
    }

}
