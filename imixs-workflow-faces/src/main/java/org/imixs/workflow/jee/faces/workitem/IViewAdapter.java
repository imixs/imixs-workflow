package org.imixs.workflow.jee.faces.workitem;

import java.util.List;

import org.imixs.workflow.ItemCollection;

/**
 * The IViewAdapter addapts the result of a get
 * @author rsoika
 *
 */
public interface IViewAdapter {
	List<ItemCollection> getViewEntries(final ViewController controller) ;
	
}
