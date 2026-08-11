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

package org.imixs.workflow.engine.handler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.engine.TextEvent;
import org.imixs.workflow.util.XMLParser;

import jakarta.ejb.Stateless;
import jakarta.enterprise.event.Observes;

/**
 * The TextTimestampAdapter replaces text fragments with a specific timestamp.
 * 
 * The adapter parses a text for the tag `<timestamp />` and replaces the tag
 * with the current system time.
 * The output can be formatted with the attribute `format` adapting the Java
 * Time Format.
 * The timestamp can further be adjusted by days or minutes using the optional
 * attributes `adjustDays`, `adjustMonths`, `adjustMinutes` or `adjustSeconds` .
 * 
 * 
 * @author rsoika
 *
 */
@Stateless
public class TextTimestampAdapter {

    private static final Logger logger = Logger.getLogger(TextTimestampAdapter.class.getName());

    // Default format pattern used if no 'format' attribute is provided
    private static final String DEFAULT_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    /**
     * This method reacts on CDI events of the type TextEvent and parses a string
     * for xml tag <timestamp>. Those tags will be replaced with the current
     * system time.
     * 
     */
    public void onEvent(@Observes TextEvent event) {
        String text = event.getText();
        boolean debug = logger.isLoggable(Level.FINE);

        List<String> timestampTags = XMLParser.findTags(text, "timestamp");

        // Replace each tag with the computed timestamp
        for (String tag : timestampTags) {
            String timestamp = computeTimestamp(tag);

            if (debug) {
                logger.log(Level.FINE, "replacing tag ''{0}'' with ''{1}''", new Object[] { tag, timestamp });
            }

            text = text.replace(tag, timestamp);
        }

        event.setText(text);
    }

    /**
     * Computes the timestamp string for a given <timestamp .../> tag.
     * 
     * Supports the optional attributes:
     * - format: a java.time.format.DateTimeFormatter pattern
     * - adjustDays: number of days to add (negative values subtract)
     * - adjustMinutes: number of minutes to add (negative values subtract)
     * 
     * @param tag - the full xml tag string, e.g. <timestamp format="dd.MM.yyyy" />
     * @return the formatted timestamp
     */
    /**
     * Computes the timestamp string for a given <timestamp .../> tag.
     * 
     * Supports the optional attributes:
     * - format: a java.time.format.DateTimeFormatter pattern
     * - adjustMonths: number of months to add (negative values subtract)
     * - adjustDays: number of days to add (negative values subtract)
     * - adjustMinutes: number of minutes to add (negative values subtract)
     * - adjustSeconds: number of seconds to add (negative values subtract)
     * 
     * @param tag - the full xml tag string, e.g. <timestamp format="dd.MM.yyyy" />
     * @return the formatted timestamp
     */
    private String computeTimestamp(String tag) {
        LocalDateTime now = LocalDateTime.now();

        now = applyAdjustment(now, tag, "adjustMonths", ChronoUnit.MONTHS);
        now = applyAdjustment(now, tag, "adjustDays", ChronoUnit.DAYS);
        now = applyAdjustment(now, tag, "adjustMinutes", ChronoUnit.MINUTES);
        now = applyAdjustment(now, tag, "adjustSeconds", ChronoUnit.SECONDS);

        // Resolve the format pattern, fallback to default if not provided or invalid
        String format = XMLParser.findAttribute(tag, "format");
        String pattern = (format != null && !format.isEmpty()) ? format : DEFAULT_FORMAT;

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return now.format(formatter);
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "invalid format pattern ''{0}'' - using default format", pattern);
            return now.format(DateTimeFormatter.ofPattern(DEFAULT_FORMAT));
        }
    }

    /**
     * Reads the given attribute from the tag and, if present, adjusts the
     * given LocalDateTime by that amount using the specified ChronoUnit.
     * 
     * @param dateTime      - the date/time to adjust
     * @param tag           - the full xml tag string
     * @param attributeName - the attribute name to look up (e.g. "adjustDays")
     * @param unit          - the ChronoUnit to apply the value with
     * @return the adjusted LocalDateTime, or the unchanged input if the
     *         attribute is missing or invalid
     */
    private LocalDateTime applyAdjustment(LocalDateTime dateTime, String tag, String attributeName, ChronoUnit unit) {
        String value = XMLParser.findAttribute(tag, attributeName);
        if (value != null && !value.isEmpty()) {
            try {
                long amount = Long.parseLong(value.trim());
                return dateTime.plus(amount, unit);
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, "invalid {0} value ''{1}'' - ignoring",
                        new Object[] { attributeName, value });
            }
        }
        return dateTime;
    }
}
