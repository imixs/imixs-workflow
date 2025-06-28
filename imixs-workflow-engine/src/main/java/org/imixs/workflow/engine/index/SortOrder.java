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

package org.imixs.workflow.engine.index;

/**
 * Stores information about how to sort documents by terms by an individual
 * field. Fields must be indexed in order to sort by them.
 *
 * @version 1.0
 */
public class SortOrder {

    private boolean reverse;
    private String field;

    /**
     * Creates a sort, possibly in reverse, with a custom comparison function.
     * 
     * @param field      Name of field to sort by; cannot be <code>null</code>.
     * @param comparator Returns a comparator for sorting hits.
     * @param reverse    True if natural order should be reversed.
     */
    public SortOrder(String field, boolean reverse) {
        this.reverse = reverse;
        this.field = field;
    }

    public boolean isReverse() {
        return reverse;
    }

    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((field == null) ? 0 : field.hashCode());
        result = prime * result + (reverse ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SortOrder other = (SortOrder) obj;
        if (field == null) {
            if (other.field != null)
                return false;
        } else if (!field.equals(other.field))
            return false;
        if (reverse != other.reverse)
            return false;
        return true;
    }
}
