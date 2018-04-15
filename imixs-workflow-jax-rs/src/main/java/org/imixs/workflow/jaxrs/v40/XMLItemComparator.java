package org.imixs.workflow.jaxrs.v40;

import java.util.Comparator;

/**
 * The XMLItemComparator provides a Comparator for XMLItems contained by a XMLItemCollection. 
 * <p>
 * Usage:
 * <p>
 * <code>Collections.sort(collection, new XMLItemComparator());</code>
 * 
 * @author rsoika
 * 
 */
public class XMLItemComparator implements Comparator<XMLItem> {

	@Override
	public int compare(XMLItem o1, XMLItem o2) {
		return o1.getName().compareTo(o2.getName());
	}
	

}
