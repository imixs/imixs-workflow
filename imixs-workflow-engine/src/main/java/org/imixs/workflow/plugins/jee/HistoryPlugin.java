/*******************************************************************************
 *  Imixs Workflow 
 *  Copyright (C) 2001, 2011 Imixs Software Solutions GmbH,  
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
 *  	http://www.imixs.org
 *  	http://java.net/projects/imixs-workflow
 *  
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika - Software Developer
 *******************************************************************************/

package org.imixs.workflow.plugins.jee;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.exceptions.PluginException;

/**
 * This Plugin creates a history log in the property txtWorkflowHistoryLog. The
 * history log contains a list for each history entry with the following
 * information:
 * <ul>
 * <li>date of creation (Date)</li>
 * <li>comment (String)</li>
 * <li>userID (String)</li>
 * </ul>
 * 
 * 
 * Note: In early versions of this plugin the history entries were stored in a
 * simple string list. The date was separated by the char sequence ' : ' from
 * the comment entry. The userId was not stored explicit. This plugin converts
 * the old format automatically (see method convertOldFormat)
 * 
 * 
 * 
 * @author Ralph Soika
 * @version 2.0
 * @see org.imixs.workflow.WorkflowManager
 * 
 */

public class HistoryPlugin extends AbstractPlugin {
	public ItemCollection documentContext;
	public ItemCollection documentActivity;
	List<List<Object>> historyList = null;
	private static Logger logger = Logger.getLogger(HistoryPlugin.class
			.getName());

	/**
	 * Update the Log entry.
	 * 
	 * The method tests if the property 'txtworkflowhistorylogrev' exists. In
	 * this case the old log format will be transformed into the new format see
	 * method convertOldFormat
	 */
	@SuppressWarnings("unchecked")
	public int run(ItemCollection adocumentContext,
			ItemCollection adocumentActivity) throws PluginException {
		String rtfItemLog;

		documentContext = adocumentContext;
		documentActivity = adocumentActivity;

		// convert old format if exists (backward compatibility (< 3.1.1)
		if (documentContext.hasItem("txtworkflowhistorylogrev")) {
			convertOldFormat();
			documentContext.removeItem("txtworkflowhistorylogrev");
		}

		// add logtext into history log
		rtfItemLog = documentActivity.getItemValueString("rtfresultlog");
		if (rtfItemLog.isEmpty())
			return Plugin.PLUGIN_OK;

		rtfItemLog = replaceDynamicValues(rtfItemLog, documentContext);

		List<?> temp = documentContext.getItemValue("txtworkflowhistory");
		historyList=new Vector();
		// clear null and empty values
		Iterator<?> i = temp.iterator();
		while (i.hasNext()) {
			Object o = i.next();
			if (o instanceof List)
				historyList.add((List<Object>) o);
		}

		List<Object> newEntry = new ArrayList<Object>();
		newEntry.add(new Date());
		newEntry.add(rtfItemLog);
		newEntry.add(this.getUserName());
		historyList.add(newEntry);

		// check if maximum length of log is defined
		int iMaxLogLength = documentContext
				.getItemValueInteger("numworkflowhistoryLength");
		if (iMaxLogLength > 0) {
			while (historyList.size() > iMaxLogLength)
				historyList.remove(0);
		}

		return Plugin.PLUGIN_OK;
	}

	/**
	 * store history only if no error has occurred
	 * 
	 * @throws
	 */
	public void close(int status) throws PluginException {

		// restore changes if OK or WARNING
		if (status < Plugin.PLUGIN_ERROR) {
			if (historyList != null) {
				documentContext.replaceItemValue("txtworkflowhistory",
						historyList);
			}

			// set timWorkflowLastAccess
			documentContext.replaceItemValue("timworkflowlastaccess",
					new Date());

		}

	}

	/**
	 * This method converts the old StringList format in the new fomrat with a
	 * list of separated values:
	 * 
	 * <ul>
	 * <li>date of creation (Date)</li>
	 * <li>comment (String)</li>
	 * <li>userID (String)</li>
	 * </ul>
	 * 
	 */
	protected void convertOldFormat() {
		List<List<Object>> newList = new ArrayList<List<Object>>();

		try {
			List<String> oldList = documentContext
					.getItemValue("txtworkflowhistorylog");

			for (String oldEntry : oldList) {
				String sDate = oldEntry.substring(0, oldEntry.indexOf(" : "));
				String sComment = oldEntry
						.substring(oldEntry.indexOf(" : ") + 3);
				String sUser = "";
				List<Object> newEntry = new ArrayList<Object>();
				newEntry.add(convertDate(sDate));
				newEntry.add(sComment);
				newEntry.add(sUser);
				newList.add(newEntry);
			}

		} catch (ClassCastException cce) {
			logger.warning("[HistoryPlugin] can not convert txtworkflowhistorylog into new format!");
			logger.warning(cce.getMessage());
		}

		documentContext.replaceItemValue("txtworkflowhistory", newList);
	}

	/**
	 * This methd is only used to convert old date formats....
	 * 
	 * @param aDateString
	 * @return
	 */
	private Date convertDate(String aDateString) {
		Date result;
		DateFormat df = null;

		try {
			df = DateFormat.getDateTimeInstance(DateFormat.LONG,
					DateFormat.LONG,new Locale("de","DE"));
			
			result = df.parse(aDateString);
			return result;
		} catch (ParseException e) {
			// no opp
		}

		try {
			df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
					DateFormat.MEDIUM,new Locale("de","DE"));
			result = df.parse(aDateString);
			return result;
		} catch (ParseException e) {
			// no opp
		}

		try {
			df = DateFormat.getDateTimeInstance(DateFormat.SHORT,
					DateFormat.SHORT,new Locale("de","DE"));
			result = df.parse(aDateString);
			return result;
		} catch (ParseException e) {
			// no opp
		}

		try {
			df = DateFormat.getDateTimeInstance(DateFormat.LONG,
					DateFormat.MEDIUM,new Locale("de","DE"));
			result = df.parse(aDateString);
			return result;
		} catch (ParseException e) {
			// no opp
		}

		try {
			df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
					DateFormat.SHORT,new Locale("de","DE"));
			result = df.parse(aDateString);
			return result;
		} catch (ParseException e) {
			// no opp
		}

		try {
			df = DateFormat.getDateTimeInstance(DateFormat.SHORT,
					DateFormat.MEDIUM,new Locale("de","DE"));
			result = df.parse(aDateString);
			return result;
		} catch (ParseException e) {
			// no opp
		}

		try {
			df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
					DateFormat.LONG,new Locale("de","DE"));
			result = df.parse(aDateString);
			return result;
		} catch (ParseException e) {
			// no opp
		}

		return null;
	}

}
