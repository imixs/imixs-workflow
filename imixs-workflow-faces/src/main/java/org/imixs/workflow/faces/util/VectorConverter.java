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

import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.convert.FacesConverter;

/**
 * The VectorConverter can be used to convert a new-line separated list into a
 * value list and vice versa.
 * <p>
 * usage:
 * 
 * <pre>{@code
 * &lt;h:inputTextarea value="#{value}" converter="org.imixs.VectorConverter" /&gt;
 * }</pre>
 * 
 */
@SuppressWarnings("rawtypes")
@FacesConverter(value = "org.imixs.VectorConverter")
public class VectorConverter implements Converter {

    private String separator = "\n";

    @SuppressWarnings({ "unchecked" })
    public Object getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {
        // Return null for empty values to allow required validation to work
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        // for backward compatibility we leave it a Vector even if a ArrayList would
        // make more sense here.
        Vector v = new Vector();
        String[] tokens = value.split(separator);
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i].trim();
            // Optional: skip empty lines to avoid empty elements in the vector
            if (!token.isEmpty()) {
                v.addElement(token);
            }
        }
        // Return null if vector is empty after processing
        return v.isEmpty() ? null : v;
    }

    /**
     * Converts a List of objects into a comma separated String
     */
    public String getAsString(FacesContext context, UIComponent component, Object value) throws ConverterException {
        String result = "";
        // we only support List objects
        if (value instanceof List) {
            ListIterator interator = ((List) value).listIterator();
            while (interator.hasNext()) {
                result = result + interator.next();
                // append separator?
                if (interator.hasNext()) {
                    result = result + separator;
                }
            }
        }
        return result;
    }

}
