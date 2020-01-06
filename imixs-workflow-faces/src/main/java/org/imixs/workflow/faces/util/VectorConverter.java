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

import java.util.ListIterator;
import java.util.Vector;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

/**
 * für ConfigItem benutzter Converter, der einen Komma-separierten String in
 * einen Vektor umwandelt und umgekehrt.
 * 
 * Noch dringend zu tun: - Dem Converter im Fehlerfall noch eine eigene
 * Fehlermeldung mitgeben - müssen da nicht noch eine Menge try-catch blöcke und
 * Typ-Prüfungen rein? Derzeit geht das alles sehr optimistisch davon aus, dass
 * in dem Vektor wirklich auch Strings drin sind; was eigentlich auch der Fall
 * ist. Interessant wird es, wenn man bestehende Felder umbiegt.
 * 
 * Schön wäre noch folgendes: - Den Separator im converter-tag der JSP Seite
 * definieren. Das wird allerdings ein Act (Vorgehen beschrieben in Kap. 20.4 in
 * "Kito Mann - JSF in Action")
 */
@SuppressWarnings("rawtypes")
@FacesConverter(value = "org.imixs.VectorConverter")
public class VectorConverter implements Converter {

    private String separator = "\n";

    @SuppressWarnings({ "unchecked" })
    public Object getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {

        Vector v = new Vector();
        String[] tokens = value.split(separator);
        for (int i = 0; i < tokens.length; i++) {
            v.addElement(tokens[i].trim());
        }

        return v;

    }

    public String getAsString(FacesContext context, UIComponent component, Object value) throws ConverterException {

        String s = "";
        Vector vValues = null;

        if (value instanceof Vector)
            vValues = (Vector) value;
        else
            vValues = new Vector();
        ListIterator li = vValues.listIterator();
        while (li.hasNext()) {
            if (li.hasPrevious()) {
                s += separator;
            }
            s += li.next();
        }

        return s;

    }

}
