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

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;

/**
 * Sorts a ArrayList of SelectItems by label
 * 
 * @author rsoika
 *
 */
public class SelectItemComparator implements Comparator<SelectItem> {
    private final Collator collator;

    private final boolean ascending;

    public SelectItemComparator(boolean ascending) {
        Locale locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
        this.collator = Collator.getInstance(locale);
        this.ascending = ascending;
    }

    public SelectItemComparator(Locale locale, boolean ascending) {
        this.collator = Collator.getInstance(locale);
        this.ascending = ascending;
    }

    public int compare(SelectItem a, SelectItem b) {
        int result = this.collator.compare(a.getLabel(), b.getLabel());
        if (!this.ascending) {
            result = -result;
        }
        return result;
    }

}
