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

package org.imixs.workflow.engine.plugins;

import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
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

    public boolean increase = false;

    private ItemCollection documentContext;
    private static final Logger logger = Logger.getLogger(IntervalPlugin.class.getName());

    /**
     * The method paresed for a fields with the prafix 'keyitnerval'
     */
    public ItemCollection run(ItemCollection adocumentContext, ItemCollection event) throws PluginException {

        LocalDateTime result = null;
        documentContext = adocumentContext;

        // validate deprecated configuration via keyinterval
        Set<String> fieldNames = documentContext.getAllItems().keySet();
        Optional<String> optional = fieldNames.stream().filter(x -> x.toLowerCase().startsWith("keyinterval"))
                .findFirst();
        if (optional.isPresent()) {// Check whether optional has element you are looking for
            logger.warning(
                    "Note: keyinterval is no longer supported by the intervalPlugin. Use instead a cron configuration.");
        }

        // evaluate interval configuration
        ItemCollection evalItemCollection = getWorkflowContext().evalWorkflowResult(event, "item",
                adocumentContext,
                true);
        // We run only if an item 'inteval' is defined in the current event (issue #841)
        if (evalItemCollection != null && evalItemCollection.hasItem(EVAL_INTERVAL)) {
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

            logger.log(Level.INFO, "......cron={0}", cron);
            logger.log(Level.INFO, "......macro={0}", macro);
            logger.log(Level.INFO, "......ref={0}", ref);

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
     * @param cron         - a cron definition * * * * *
     * @param baseDateTime - the base dateTime for the evaluation
     * @return next dateTime
     * @throws PluginException
     */
    public LocalDateTime evalCron(String cron, LocalDateTime baseDateTime) throws PluginException {
        LocalDateTime result = null;

        // split conr
        String[] cronDef = cron.split(" ");
        if (cronDef.length != 5) {
            throw new PluginException(IntervalPlugin.class.getName(), INVALID_FORMAT, "invalid cron format: " + cron);
        }

        if (baseDateTime == null) {
            result = LocalDateTime.now().withSecond(0);
        } else {
            result = baseDateTime;
        }

        increase = true;
        try {
            // adjust minute
            String minute = cronDef[0];
            result = adjustDateTimeByCronUnit(minute, result, ChronoUnit.MINUTES, ChronoField.MINUTE_OF_HOUR);

            // adjust hour
            String hour = cronDef[1];
            result = adjustDateTimeByCronUnit(hour, result, ChronoUnit.HOURS, ChronoField.HOUR_OF_DAY);

            // adjust dayofmonth
            String dayofmonth = cronDef[2];
            result = adjustDateTimeByCronUnit(dayofmonth, result, ChronoUnit.DAYS, ChronoField.DAY_OF_MONTH);

            // adjust month
            String month = cronDef[3];
            result = adjustDateTimeByCronUnit(month, result, ChronoUnit.MONTHS, ChronoField.MONTH_OF_YEAR);

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

    public LocalDateTime evalCron(String cron) throws PluginException {
        return evalCron(cron, null);
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
                // unknown makro return null
                return null;
        }

        return ldt;

    }

    /**
     * Helper method computes the next DateTime based on a given Cron definition
     * based on a TemporaUnit.
     * 
     **/
    private LocalDateTime adjustDateTimeByCronUnit(String cronUnit, LocalDateTime baseDateTime, TemporalUnit tempUnit,
            TemporalField tempField) {
        // adjust time entiy
        if (cronUnit.equals("*")) {
            if (increase) {
                increase = false;
                baseDateTime = baseDateTime.plus(1, tempUnit);
            }
        } else {

            int nowUnit = baseDateTime.get(tempField);
            // test if values (,)
            if (cronUnit.contains(",")) {
                String[] units = cronUnit.split(",");
                // 9,15
                logger.log(Level.INFO, " unit now = {0}", nowUnit);
                boolean found = false;
                for (String singleUnit : units) {
                    if (Integer.parseInt(singleUnit) >= nowUnit) {
                        baseDateTime = baseDateTime.with(tempField, Integer.parseInt(singleUnit));
                        found = true;
                        increase = false;
                        break;
                    }
                }
                if (!found) {
                    // all singleUnits are in the past, take first singleUnit form the list and
                    // increase the next unit
                    baseDateTime = baseDateTime.with(tempField, Integer.parseInt(units[0]));
                    increase = true; // increase next cron unit
                }

            } else {
                baseDateTime = baseDateTime.with(tempField, Integer.parseInt(cronUnit));
                if (nowUnit < Integer.parseInt(cronUnit)) {
                    increase = false;
                }
            }
        }

        return baseDateTime;
    }

}
