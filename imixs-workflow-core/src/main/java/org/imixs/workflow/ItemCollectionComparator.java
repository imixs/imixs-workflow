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

package org.imixs.workflow;

import java.text.Collator;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

/**
 * The ItemCollectionComparator provides a Comparator for ItemColections. The
 * item to be compared can be provided in the constructor.
 * <p>
 * Usage:
 * <p>
 * <code>Collections.sort(collection, new ItemCollectionComparator("txtname", true));</code>
 * 
 * @author rsoika
 * 
 */
public class ItemCollectionComparator implements Comparator<ItemCollection> {
    private final Collator collator;
    private final boolean ascending;
    private final String itemName;

    public ItemCollectionComparator(String aItemName, boolean ascending, Locale locale) {
        this.collator = Collator.getInstance(locale);
        this.ascending = ascending;
        this.itemName = aItemName;
    }

    /**
     * This method sorts by the default locale
     * 
     * @param aItemName
     * @param ascending
     */
    public ItemCollectionComparator(String aItemName, boolean ascending) {
        this.collator = Collator.getInstance(Locale.getDefault());
        this.ascending = ascending;
        this.itemName = aItemName;

    }

    /**
     * This method sorts by the default locale ascending
     * 
     * @param aItemName
     * @param ascending
     */
    public ItemCollectionComparator(String aItemName) {
        this.collator = Collator.getInstance(Locale.getDefault());
        this.ascending = true;
        this.itemName = aItemName;

    }

    public int compare(ItemCollection a, ItemCollection b) {

        // date compare?
        if (a.isItemValueDate(itemName)) {

            Date dateA = a.getItemValueDate(itemName);
            Date dateB = b.getItemValueDate(itemName);
            if (dateA == null && dateB != null) {
                return 1;
            }
            if (dateB == null && dateA != null) {
                return -1;
            }
            if (dateB == null && dateA == null) {
                return 0;
            }

            int result = dateB.compareTo(dateA);
            if (!this.ascending) {
                result = -result;
            }
            return result;

        }

        // integer compare?
        if (a.isItemValueInteger(itemName)) {
            int result = a.getItemValueInteger(itemName) - b.getItemValueInteger(itemName);
            if (!this.ascending) {
                result = -result;
            }
            return result;

        }

        // String compare
        int result = this.collator.compare(a.getItemValueString(itemName), b.getItemValueString(itemName));
        if (!this.ascending) {
            result = -result;
        }
        return result;
    }

}
