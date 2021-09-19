/*  
 *  Imixs-Workflow 
 *  
 *  Copyright (C) 2001-2020 Imixs Software Solutions GmbH,  
 *  http://www.imixs.com
 *  
 *  This program is free software; you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License 
 *  as published by the Free Software Foundation; either version 2 
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 *  General Public License for more details.
 *  
 *  You can receive a copy of the GNU General Public
 *  License at http://www.gnu.org/licenses/gpl.html
 *  
 *  Project: 
 *      https://www.imixs.org
 *      https://github.com/imixs/imixs-workflow
 *  
 *  Contributors:  
 *      Imixs Software Solutions GmbH - Project Management
 *      Ralph Soika - Software Developer
 */

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
 * vecotr and vice versa.
 * <p>
 * usage:
 * <p>
 * <code><h:inputTextarea value="#{value}" converter="org.imixs.VectorConverter" /></code>
 * 
 *
 */
@SuppressWarnings("rawtypes")
@FacesConverter(value = "org.imixs.VectorConverter")
public class VectorConverter implements Converter {

    private String separator = "\n";

    @SuppressWarnings({ "unchecked" })
    public Object getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {
        // for backward compatibility we leave it a Vector even if a ArrayList would
        // make more sense here.
        Vector v = new Vector();
        String[] tokens = value.split(separator);
        for (int i = 0; i < tokens.length; i++) {
            v.addElement(tokens[i].trim());
        }
        return v;
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
