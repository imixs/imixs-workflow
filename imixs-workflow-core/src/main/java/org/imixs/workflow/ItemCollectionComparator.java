/*******************************************************************************
 * <pre>
 *  Imixs Workflow 
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
 *      Imixs Software Solutions GmbH - initial API and implementation
 *      Ralph Soika - Software Developer
 * </pre>
 *******************************************************************************/

package org.imixs.workflow;

import java.text.Collator;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

/**
 * The ItemCollectionComparator provides a Comparator for ItemColections. The item to be compared
 * can be provided in the constructor.
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
    int result =
        this.collator.compare(a.getItemValueString(itemName), b.getItemValueString(itemName));
    if (!this.ascending) {
      result = -result;
    }
    return result;
  }

}
