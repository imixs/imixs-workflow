/*  
 *  Imixs-Workflow 
 *  
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
 *      Imixs Software Solutions GmbH - Project Management
 *      Ralph Soika - Software Developer
 */

package org.imixs.workflow.engine.plugins;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.util.XMLParser;

/**
 * The Imixs Interval Plugin implements an mechanism to adjust a date field of a
 * workitem based on a interval description. The interval description is stored
 * in a field with the prafix 'keyinterval' followed by the name of an existing
 * date field. See the following example:
 * 
 * <pre>
 * {@code
<item name="interval">
    <ref>reminder</ref>
    <cron>5 15 * * 1-5</cron>
</item>}
 * </pre>
 * 
 * 
 * @author Ralph Soika
 * @version 1.0
 * @see org.imixs.workflow.WorkflowManager
 * 
 */
public class IntervalPlugin extends AbstractPlugin {

	public static final String EVAL_INTERVAL = "interval";

	public static final String INVALID_FORMAT = "INVALID_FORMAT";

	private ItemCollection documentContext;
	private static Logger logger = Logger.getLogger(IntervalPlugin.class.getName());

	/**
	 * The method paresed for a fields with the prafix 'keyitnerval'
	 */
	public ItemCollection run(ItemCollection adocumentContext, ItemCollection adocumentActivity)
			throws PluginException {

		LocalDateTime result = null;

		documentContext = adocumentContext;
		// test if activity is a schedule activity...
		// check if activity is scheduled
		if (!"1".equals(adocumentActivity.getItemValueString("keyScheduledActivity"))) {
			return documentContext;
		}

		// validate deprecated configuration via keyinterval
		Set<String> fieldNames = documentContext.getAllItems().keySet();
		Optional<String> optional = fieldNames.stream().filter(x -> x.toLowerCase().startsWith("keyinterval"))
				.findFirst();
		if (optional.isPresent()) {// Check whether optional has element you are looking for
			throw new PluginException(IntervalPlugin.class.getName(), INVALID_FORMAT,
					"Note: keyinterval is no longer supported by the intervalPlugin. Use instead a cron configuration.");
		}

		// evaluate interval configuration
		long l = System.currentTimeMillis();
		ItemCollection evalItemCollection = getWorkflowService().evalWorkflowResult(adocumentActivity, adocumentContext,
				false);
		logger.warning("evaluation takes " + (System.currentTimeMillis() - l) + "ms");

		if (evalItemCollection == null) {
			return adocumentContext;
		}

		if (evalItemCollection.hasItem(EVAL_INTERVAL)) {

			String invervalDef = evalItemCollection.getItemValueString(EVAL_INTERVAL);

			if (invervalDef.trim().isEmpty()) {
				// no definition - skip
				return adocumentContext;
			}
			// evaluate the item content (XML format expected here!)
			ItemCollection processData = XMLParser.parseItemStructure(invervalDef);

			String cron = processData.getItemValueString("cron").trim().toLowerCase();
			String macro = processData.getItemValueString("macro").trim().toLowerCase();
			String ref = processData.getItemValueString("ref").trim().toLowerCase();

			if (!cron.isEmpty() && !macro.isEmpty()) {
				throw new PluginException(IntervalPlugin.class.getName(), INVALID_FORMAT,
						"invalid interval configuration: cron and macro can not be combined!");
			}

			LocalDateTime refDate = documentContext.getItemValueLocalDateTime(ref);
			if (!macro.isEmpty() && refDate == null) {
				throw new PluginException(IntervalPlugin.class.getName(), INVALID_FORMAT,
						"invalid interval configuration: ref item is missing for " + "macro!");
			}

			logger.info("......cron=" + cron);
			logger.info("......macro=" + macro);
			logger.info("......ref=" + ref);

			// compute cron or macro interval
			if (!cron.isEmpty()) {
				result = evalCron(cron);
			} else {
				result = evalMacro(macro, refDate);
			}

			// update ref item
			if (result != null) {
				documentContext.replaceItemValue(ref, result);
			}
		}
		return documentContext;
	}

	/**
	 * evaluates a cron definition
	 * 
	 * @param cron
	 * @return new schedule date
	 * @throws PluginException
	 */
	public LocalDateTime evalCron(String cron) throws PluginException {

		// split conr
		String[] cronDef = cron.split(" ");
		if (cronDef.length != 5) {
			throw new PluginException(IntervalPlugin.class.getName(), INVALID_FORMAT, "invalid cron format: " + cron);
		}

		LocalDateTime result = LocalDateTime.now().withSecond(0);
		boolean increase = true;
		try {
			// adjust minute
			String minute = cronDef[0];
			if (minute.equals("*")) {
				if (increase) {
					increase = false;
					result = result.plusMinutes(1);
				}
			} else {
				result = result.withMinute(Integer.parseInt(minute));
			}

			// adjust hour
			String hour = cronDef[1];
			if (hour.equals("*")) {
				if (increase) {
					increase = false;
					result = result.plusHours(1);
				}
			} else {
				result = result.withHour(Integer.parseInt(hour));
			}

			// adjust dayofmonth
			String dayofmonth = cronDef[2];
			if (dayofmonth.equals("*")) {
				if (increase) {
					increase = false;
					result = result.plusDays(1);
				}
			} else {
				result = result.withDayOfMonth(Integer.parseInt(dayofmonth));
			}

			// adjust month
			String month = cronDef[3];
			if (month.equals("*")) {
				if (increase) {
					increase = false;
					result = result.plusMonths(1);
				}
			} else {
				result = result.withMonth(Integer.parseInt(month));
			}
		} catch (NumberFormatException e) {
			// we do not support all kind of patterns
			throw new PluginException(IntervalPlugin.class.getName(), INVALID_FORMAT,
					"invalid cron format: " + cron + " Note: we do not yet support all kind of patterns.");
		}

		// adjust day of week by regex or Year
		String dayofweek = cronDef[4];
		if ("*".equals(dayofweek)) {
			if (increase) {
				increase = false;
				result = result.plusYears(1);
			}
		} else {
			// we assume that this is a regex like [1-5]
			// try to evaluate....
			if (!dayofweek.startsWith("[")) {
				dayofweek = "[" + dayofweek + "]";
			}
			int count = 0;
			while (true) {
				int dow = result.getDayOfWeek().getValue();
				if (Pattern.compile(dayofweek).matcher("" + dow).find()) {
					break;
				}
				result = result.plusDays(1);
				count++;
				if (count > 7) {
					// seems to be a wrong regex!
					throw new PluginException(IntervalPlugin.class.getName(), INVALID_FORMAT,
							"invalid cron format 'DayOfWeek' : " + cron);
				}
			}

		}

		return result;
	}

	/**
	 * The method evaluates a macro. Possible values:
	 * <ul>
	 * <li>@yearly</li>
	 * <li>@monthly</li>
	 * <li>@weekly</li>
	 * <li>@daily</li>
	 * <li>@hourly</li>
	 *
	 * @param macro
	 * @param date
	 * @return
	 * @throws PluginException
	 */
	public LocalDateTime evalMacro(String macro, LocalDateTime ldt) throws PluginException {

		switch (macro) {
		case "@yearly":
			ldt = ldt.plusYears(1);
			break;
		case "@monthly":
			ldt = ldt.plusMonths(1);
			break;
		case "@weekly":
			ldt = ldt.plusWeeks(1);
			break;
		case "@daily":
			ldt = ldt.plusDays(1);
			break;
		case "@hourly":
			ldt = ldt.plusHours(1);
			break;
		default:
			throw new PluginException(IntervalPlugin.class.getName(), INVALID_FORMAT, "invalid macro format: " + macro);
		}

		return ldt;

	}

}
