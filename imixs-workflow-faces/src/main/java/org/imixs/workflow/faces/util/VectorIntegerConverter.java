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

package org.imixs.workflow.faces.util;

import java.util.ListIterator;
import java.util.Vector;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;

/**
 * Converts a vector of integer values into a string an vice versa. 
 */

@SuppressWarnings("rawtypes")
public class VectorIntegerConverter implements Converter {

    String separator = "\n";

    @SuppressWarnings({ "unchecked" })
    public Object getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {

        Vector v = new Vector();
        String[] tokens = value.split(separator);
        for (int i = 0; i < tokens.length; i++) {
            String sValue = tokens[i].trim();
            Integer intValue = new Integer(sValue);
            v.addElement(intValue);
        }

        return v;

    }

    public String getAsString(FacesContext context, UIComponent component, Object value) throws ConverterException {

        String s = "";
        Vector vValues = (Vector) value;
        ListIterator li = vValues.listIterator();
        while (li.hasNext()) {
            if (li.hasPrevious()) {
                s += "" + separator;
            }
            s += li.next();
        }

        return s;

    }

}
