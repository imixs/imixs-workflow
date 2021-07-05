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
 *      Volker Probst, Ralph Soika - Software Developer
 */

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
