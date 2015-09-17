package org.imixs.workflow;

import java.text.Collator;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import javax.faces.context.FacesContext;

import org.imixs.workflow.ItemCollection;

/**
 * The ItemCollectionComparator provides a Comparator for ItemColections. The item to
 * be compared can be provided in the constructor.
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
		itemName = aItemName;
	}

	/**
	 * This method tries to get the locale form the current Faces Context. If no
	 * faces Context exists, then the method get the default locale form the JVM.
	 * 
	 * @param aItemName
	 * @param ascending
	 */
	public ItemCollectionComparator(String aItemName, boolean ascending) {
		Locale locale = null;
		// try to get user locale...
		if (FacesContext.getCurrentInstance() != null) {
			locale = FacesContext.getCurrentInstance().getViewRoot()
					.getLocale();
		} else {
			locale = Locale.getDefault();
		}
		this.collator = Collator.getInstance(locale);
		this.ascending = ascending;
		itemName = aItemName;

	}

	public int compare(ItemCollection a, ItemCollection b) {

		// date compare?
		if (a.isItemValueDate(itemName)) {

			Date dateA = a.getItemValueDate(itemName);
			Date dateB = b.getItemValueDate(itemName);
			int result = dateB.compareTo(dateA);
			if (!this.ascending) {
				result = -result;
			}
			return result;

		}

		// integer compare?
		if (a.isItemValueInteger(itemName)) {
			int result = a.getItemValueInteger(itemName)
					- b.getItemValueInteger(itemName);
			if (!this.ascending) {
				result = -result;
			}
			return result;

		}

		// String compare
		int result = this.collator.compare(a.getItemValueString(itemName),
				b.getItemValueString(itemName));
		if (!this.ascending) {
			result = -result;
		}
		return result;
	}

}
