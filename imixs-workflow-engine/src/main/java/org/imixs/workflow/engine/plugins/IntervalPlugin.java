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

package org.imixs.workflow.engine.plugins;

import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.PluginException;

/**
 * The Imixs Interval Plugin implements an mechanism to adjust a date field of a
 * workitem based on a interval description. The interval description is stored
 * in a field with the prafix 'keyinterval' followed by the name of an existing
 * date field. See the following example:
 * 
 * <code>
 *  keyIntervalDatDate=monthly
 *  datDate=01.01.2014 
 * </code>
 * 
 * @author Ralph Soika
 * @version 1.0
 * @see org.imixs.workflow.WorkflowManager
 * 
 */
public class IntervalPlugin extends AbstractPlugin {

	public static final String INVALID_FORMAT = "INVALID_FORMAT";

	private ItemCollection documentContext;
	private static Logger logger = Logger.getLogger(IntervalPlugin.class.getName());

	/**
	 * The method paresed for a fields with the prafix 'keyitnerval'
	 */
	public ItemCollection run(ItemCollection adocumentContext, ItemCollection adocumentActivity) throws PluginException {
		documentContext = adocumentContext;
		// test if activity is a schedule activity...
		// check if activity is scheduled
		if (!"1".equals(adocumentActivity.getItemValueString("keyScheduledActivity"))) {
			return documentContext;
		}

		
		Calendar calNow = Calendar.getInstance();

		logger.finest("......compute next interval dates for workitem "
				+ documentContext.getItemValueString(WorkflowKernel.UNIQUEID));

		Set<String> fieldNames = documentContext.getAllItems().keySet();
		for (String fieldName : fieldNames) {
			if (fieldName.toLowerCase().startsWith("keyinterval")) {
				String sInterval = documentContext.getItemValueString(fieldName);

				if (sInterval.isEmpty())
					continue;

				sInterval = sInterval.toLowerCase();
				// lookup for a date value
				String sDateField = fieldName.substring(11);
				if (!sDateField.isEmpty() && documentContext.hasItem(sDateField)) {
					Date date = documentContext.getItemValueDate(sDateField);
					if (date != null) {

						// verify if date is in the past....
						Calendar calDate = Calendar.getInstance();
						calDate.setTime(date);
						if (calNow.after(calDate)) {
							logger.finest("......compute next interval for " + sDateField);

							// first set day month and year to now
							calDate.set(Calendar.YEAR, calNow.get(Calendar.YEAR));
							calDate.set(Calendar.MONTH, calNow.get(Calendar.MONTH));
							calDate.set(Calendar.DAY_OF_MONTH, calNow.get(Calendar.DAY_OF_MONTH));
							
							
							// test if interval is a number. In this case
							// increase the date of the number of days
							try {
								int iDays = Integer.parseInt(sInterval);
								calDate.add(Calendar.DAY_OF_MONTH, iDays);
							} catch (NumberFormatException nfe) {
								// check for daily, monthly, yerarliy

								if (sInterval.contains("daily")) {
									calDate.add(Calendar.DAY_OF_MONTH, 1);
								}

								if (sInterval.contains("weekly")) {
									calDate.add(Calendar.DAY_OF_MONTH, 7);
								}

								if (sInterval.contains("monthly")) {
									calDate.add(Calendar.MONTH, 1);
								}

								if (sInterval.contains("yearly")) {
									calDate.add(Calendar.YEAR, 1);
								}
							}

							documentContext.replaceItemValue(sDateField, calDate.getTime());

						}
					}
				}

			}
		}

		return documentContext;
	}

	

}
