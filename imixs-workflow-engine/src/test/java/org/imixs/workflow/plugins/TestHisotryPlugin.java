package org.imixs.workflow.plugins;

import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.plugins.jee.HistoryPlugin;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

/**
 * Test the history convertion
 * 
 * @author rsoika
 * 
 */
public class TestHisotryPlugin extends HistoryPlugin {

	private final static Logger logger = Logger.getLogger(EntityService.class
			.getName());

	@Before
	public void setup() throws PluginException {

	}

	@SuppressWarnings("unchecked")
	@Test
	public void testItemCollection() {
		logger.info("[TestHisotryPlugin] converting old format...");

		// prepare data
		documentContext = new ItemCollection();
		logger.info("[TestHisotryPlugin] setup test data...");
		Vector<String> list = new Vector<String>();
		list.add("14.02.2012 10:35:06 : In Abstimmung mit Ralph Soika.");
		list.add("14.02.2012 10:39:22 : Gespeichert von Ralph Soika.");
		list.add("14.02.2012 15:55:51 : Gespeichert von Ralph Soika.");
		documentContext.replaceItemValue("txtworkflowhistorylog", list);

		convertOldFormat();

		List<List<Object>> newList = documentContext
				.getItemValue("txtworkflowhistory");

		logger.info("[TestHisotryPlugin] txtworkflowhistory after converting: "
				+ newList);
		for (List<Object> aEntry : newList) {
			Object o = aEntry.get(0);
			if (!(o instanceof Date))
				Assert.fail();
		}

		Assert.assertEquals(3, newList.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testItemCollectionAnotherFormat() {
		logger.info("[TestHisotryPlugin] converting old format...");

		// prepare data
		documentContext = new ItemCollection();
		logger.info("[TestHisotryPlugin] setup test data...");
		Vector<String> list = new Vector<String>();
		list.add("");
		list.add("14.02.2012 10:35:06 : In Abstimmung mit Ralph Soika.");
		list.add("14.02.2012 10:39:22 : Gespeichert von Ralph Soika.");
		list.add("14.02.2012 15:55:51 : Gespeichert von Ralph Soika.");
		list.add("06.03.2014 10:29:11 : Info liegt vor von <username>namcurrenteditor</username>.");
		list.add("06.03.2014 10:29:56 : Bestätigung liegt vor von <username>namcurrenteditor</username>.");
		documentContext.replaceItemValue("txtworkflowhistorylog", list);

		convertOldFormat();

		List<List<Object>> newList = documentContext
				.getItemValue("txtworkflowhistory");

		logger.info("[TestHisotryPlugin] txtworkflowhistory after converting: "
				+ newList);
		for (List<Object> aEntry : newList) {
			Object o = aEntry.get(0);
			if (!(o instanceof Date))
				Assert.fail();
		}

		Assert.assertEquals(5, newList.size());
	}

}
