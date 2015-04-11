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

package org.imixs.workflow.plugins;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.PluginException;

/**
 * This Pluginmodul implements a Historyfunktion for Workflowactivitys. The
 * Plugin generates a History Entry which will be appended to an existing
 * workflowprotokoll. The Protocol will be stored in the Attributes
 * 
 * o txtworkflowhistorylog (descending sort)
 * 
 * o txtworkflowhistorylogrev (ascending sort)
 * 
 * The Plugin generate a History Entry by a set of activityEntity attributes
 * 
 * These attributes are:
 * 
 * o rtfresultlog (String): HistoryLog text
 * 
 * o keylogdateformat (String): Dateformat for Historyentry
 * 
 * o keylogtimeformat (String): Timeformat for Historyentry
 * 
 * Each entry will be Präfixed by a DateTime String representing the time the
 * Entry was generated.
 * 
 * The HistoryLog function pares the rtfresultlog for "<field>attribute</field>"
 * tags and replaces these tags with the values of the corresponding attributes
 * of the workitem.
 * 
 * 
 * the Attribute numworkflowhistoryLength indicates the maximum number of
 * entries. if <= 0 no limit is set. Otherwise older entries will be cut off.
 * 
 * 
 * @author Ralph Soika
 * @version 1.2
 * @see org.imixs.workflow.WorkflowManager
 * 
 */

public class HistoryPlugin extends AbstractPlugin {
	public ItemCollection documentContext;
	public ItemCollection documentActivity;
	@SuppressWarnings("rawtypes")
	List vOldProt, vOldProtRev;
	private static Logger logger = Logger.getLogger("org.imixs.workflow");

	@SuppressWarnings("unchecked")
	public int run(ItemCollection adocumentContext,
			ItemCollection adocumentActivity) throws PluginException {
		String rtfItemResult;

		documentContext = adocumentContext;
		documentActivity = adocumentActivity;

		// Logtext aus Resultdocument lesen
		rtfItemResult = documentActivity.getItemValueString("rtfresultlog");
		String aProtokoll = rtfItemResult;

		String sDatumsFormat = documentActivity
				.getItemValueString("keyLogDateFormat");

		if (sDatumsFormat == null || "".equals(sDatumsFormat))
			sDatumsFormat = "1";
		String sZeitFormat = documentActivity
				.getItemValueString("keylogtimeformat");
		if (sZeitFormat == null || "".equals(sZeitFormat))
			sZeitFormat = "2";

		if (ctx.getLogLevel() == WorkflowKernel.LOG_LEVEL_FINE)
			logger.info("[HistoryPlugin] logtimeformat=" + sZeitFormat);
		if (ctx.getLogLevel() == WorkflowKernel.LOG_LEVEL_FINE)
			logger.info("[HistoryPlugin] logdateformat=" + sDatumsFormat);

		// get Time Date format...
		int iDatumsFormat = -1;
		int iZeitFormat = -1;
		try {
			iDatumsFormat = Integer.parseInt(sDatumsFormat);
			iZeitFormat = Integer.parseInt(sZeitFormat);
		} catch (NumberFormatException nfe) {
			// invalid DateTime format found
			if (ctx.getLogLevel() >= WorkflowKernel.LOG_LEVEL_WARNING)
				logger.severe("[HistoryPlugin] error logtimeformat "
						+ nfe.toString());
		}

		String sTim = "";
		if ((iZeitFormat > -1) && (iDatumsFormat > -1)) {
			sTim = DateFormat.getDateTimeInstance(iDatumsFormat, iZeitFormat)
					.format(new Date());
		} else {
			if ((iZeitFormat == -1) && (iDatumsFormat > -1))
				sTim = DateFormat.getDateInstance(iDatumsFormat).format(
						new Date());
			else if ((iZeitFormat > -1) && (iDatumsFormat == -1))
				sTim = DateFormat.getTimeInstance(iZeitFormat).format(
						new Date());
		}

		// Check if a text was found. Protocol will only be added if text
		// was defined
		if (!"".equals(aProtokoll)) {
			String sDoppelpunkt = "";
			if ((!"".equals(sTim)) && (!"".equals(aProtokoll)))
				sDoppelpunkt = " : ";
			aProtokoll = sTim + sDoppelpunkt + aProtokoll;

			aProtokoll = replaceDynamicValues(aProtokoll, documentContext);

			vOldProt = documentContext.getItemValue("txtworkflowhistorylog");
			vOldProtRev = documentContext
					.getItemValue("txtworkflowhistorylogrev");

			vOldProt.add(aProtokoll);
			vOldProtRev.add(0, aProtokoll);

			// check if maximum length of log is defined
			int iMaxLogLength = documentContext
					.getItemValueInteger("numworkflowhistoryLength");
			if (iMaxLogLength > 0) {
				while (vOldProt.size() > iMaxLogLength)
					vOldProt.remove(0);

				while (vOldProtRev.size() > iMaxLogLength)
					vOldProtRev.remove(vOldProtRev.size() - 1);
			}

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
			if (vOldProt != null)

				documentContext.replaceItemValue("txtworkflowhistorylog",
						vOldProt);

			if (vOldProtRev != null)
				documentContext.replaceItemValue("txtworkflowhistorylogrev",
						vOldProtRev);
			Date date = new Date();
			// set timWorkflowLastAccess
			documentContext.replaceItemValue("timworkflowlastaccess", date);

		}

	}

}
