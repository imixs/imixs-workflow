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

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.TextEvent;
import org.imixs.workflow.util.XMLParser;
import org.imixs.workflow.util.XMLTag;

import jakarta.ejb.Stateless;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * The TextItemValueAdapter replaces text fragments with the values of a named
 * Item.
 * 
 * @author rsoika
 *
 */
@Stateless
public class TextItemValueAdapter {

    private static final Logger logger = Logger.getLogger(TextItemValueAdapter.class.getName());

    @Inject
    DocumentService documentService;

    /**
     * This method reacts on CDI events of the type TextEvent and parses a string
     * for xml tag [{@code<itemvalue>}. Those tags will be replaced with the
     * corresponding item value:
     * <p>
     * {@code hello <itemvalue>$Creator</itemvalue>}
     * <p>
     * Item values can also be formated. e.g. for date/time values:
     * <p>
     * {@code  Last access Time= <itemvalue format="mm:ss">$created</itemvalue>}
     * <p>
     * If the itemValue is a multiValue object the single values can be spearated by
     * a separator:
     * <p>
     * {@code Phone List: <itemvalue separator="<br />">txtPhones</itemvalue>}
     * 
     * 
     */
    public void onEvent(@Observes TextEvent event) {
        boolean debug = logger.isLoggable(Level.FINE);

        // Cache for referenced workitems — avoids repeated DocumentService lookups
        // within one adaptText() call that may contain multiple <itemvalue ref="...">
        // tags
        Map<String, ItemCollection> refCache = new HashMap<>();

        String text = event.getText();

        if (text == null)
            return;

        // lower case <itemValue> into <itemvalue>
        if (text.contains("<itemValue") || text.contains("</itemValue>")) {
            logger.warning("Deprecated <itemValue> tag should be lowercase <itemvalue> !");
            text = text.replace("<itemValue", "<itemvalue");
            text = text.replace("</itemValue>", "</itemvalue>");
        }

        // Use parseTagMatches() to get exact tag positions.
        // This fixes the indexOf() bug where duplicate tag content caused wrong
        // replacements, and correctly handles CDATA sections in prompt templates.
        List<XMLTag> tagList = XMLParser.parseTagMatches(text, "itemvalue");
        if (debug) {
            logger.log(Level.FINEST, "......{0} tags found", tagList.size());
        }

        // Iterate in reverse order so that replacing by position does not shift
        // the positions of tags that have not yet been processed.
        // Cache for referenced workitems — avoids repeated DocumentService lookups
        for (int i = tagList.size() - 1; i >= 0; i--) {
            XMLTag tag = tagList.get(i);

            String sFormat = tag.getAttribute("format");
            String sSeparator = tag.getAttribute("separator");
            String sPosition = tag.getAttribute("position");

            // Extract locale
            Locale locale = null;
            String sLocale = tag.getAttribute("locale");
            if (sLocale != null && !sLocale.isEmpty()) {
                StringTokenizer stLocale = new StringTokenizer(sLocale, "_");
                if (stLocale.countTokens() == 1) {
                    String sLang = stLocale.nextToken();
                    locale = new Locale(sLang, sLang.toUpperCase());
                } else {
                    String sLang = stLocale.nextToken();
                    String sCount = stLocale.nextToken();
                    locale = new Locale(sLang, sCount);
                }
            }

            // Resolve the document context — either the current workitem or a
            // referenced workitem loaded via the ref= attribute.
            // References are cached to avoid repeated DocumentService lookups.
            ItemCollection documentContext = resolveRef(tag.getAttribute("ref"),
                    event.getDocument(), refCache);

            if (documentContext == null) {
                // Referenced workitem not found — skip this tag
                logger.log(Level.WARNING,
                        "TextItemValueAdapter: ref ''{0}'' could not be resolved — tag skipped",
                        tag.getAttribute("ref"));
                continue;
            }

            List<?> vValue = documentContext.getItemValue(tag.getContent());
            String sResult = formatItemValues(vValue, sSeparator, sFormat, locale, sPosition);

            text = text.substring(0, tag.getStartPos()) + sResult + text.substring(tag.getEndPos());
        }

        event.setText(text);
    }

    /**
     * This method returns a formated a string object.
     * 
     * In case a Separator is provided, multiValues will be separated by the
     * provided separator.
     * 
     * If no separator is provide, only the first value will returned.
     * 
     * The format and locale attributes can be used to format number and date
     * values.
     * 
     */
    public String formatItemValues(List<?> aItem, String aSeparator, String sFormat, Locale locale, String sPosition) {

        StringBuffer sBuffer = new StringBuffer();

        if (aItem == null || aItem.size() == 0)
            return "";

        // test if a position was defined?
        if (sPosition == null || sPosition.isEmpty()) {
            // no - we iterate over all...
            for (Object aSingleValue : aItem) {
                String aValue = formatObjectValue(aSingleValue, sFormat, locale);
                sBuffer.append(aValue);
                // append delimiter only if a separator is defined
                if (aSeparator != null) {
                    sBuffer.append(aSeparator);
                } else {
                    // no separator, so we can exit with the first value
                    break;
                }
            }
        } else {
            // evaluate position
            if ("last".equalsIgnoreCase(sPosition)) {
                sBuffer.append(aItem.get(aItem.size() - 1));
            } else {
                // default first poistion
                sBuffer.append(aItem.get(0));
            }

        }

        String sString = sBuffer.toString();

        // cut last separator
        if (aSeparator != null && sString.endsWith(aSeparator)) {
            sString = sString.substring(0, sString.lastIndexOf(aSeparator));
        }

        return sString;

    }

    /**
     * this method formats a string object depending of an attribute type.
     * MultiValues will be separated by the provided separator
     */
    public String formatItemValues(List<?> aItem, String aSeparator, String sFormat) {
        return formatItemValues(aItem, aSeparator, sFormat, null, null);
    }

    /**
     * this method formats a string object depending of an attribute type.
     * MultiValues will be separated by the provided separator
     */
    public String formatItemValues(List<?> aItem, String aSeparator, String sFormat, Locale alocale) {
        return formatItemValues(aItem, aSeparator, sFormat, alocale, null);
    }

    /**
     * This method converts a double value into a custom number format including an
     * optional locale.
     * 
     * <pre>
     * {@code
     * 
     * "###,###.###", "en_UK", 123456.789
     * 
     * "EUR #,###,##0.00", "de_DE", 1456.781
     * 
     * }
     * </pre>
     * 
     * @param pattern
     * @param value
     * @return
     */
    private String customNumberFormat(String pattern, Locale _locale, double value) {
        DecimalFormat formatter = null;

        // test if we have a locale
        if (_locale != null) {
            formatter = (DecimalFormat) DecimalFormat.getInstance(_locale);
        } else {
            formatter = (DecimalFormat) DecimalFormat.getInstance();
        }
        formatter.applyPattern(pattern);
        String output = formatter.format(value);

        return output;
    }

    /**
     * This helper method test the type of an object provided by a itemcollection
     * and formats the object into a string value.
     * 
     * Only Date Objects will be formated into a modified representation. other
     * objects will be returned using the toString() method.
     * 
     * If an optional format is provided this will be used to format date objects.
     * 
     * @param o
     * @return
     */
    private String formatObjectValue(Object o, String format, Locale locale) {
        String singleValue = "";
        Date dateValue = null;

        // now test the objct type to date
        if (o instanceof Date) {
            dateValue = (Date) o;
        }

        if (o instanceof Calendar) {
            Calendar cal = (Calendar) o;
            dateValue = cal.getTime();
        }

        // format date string?
        if (dateValue != null) {
            if (format != null && !"".equals(format)) {
                // format date with provided formater
                try {
                    SimpleDateFormat formatter = null;
                    if (locale != null) {
                        formatter = new SimpleDateFormat(format, locale);
                    } else {
                        formatter = new SimpleDateFormat(format);
                    }
                    singleValue = formatter.format(dateValue);
                } catch (Exception ef) {
                    logger.log(Level.WARNING, "TextItemValueAdapter: Invalid format String ''{0}''", format);
                    logger.log(Level.WARNING, "TextItemValueAdapter: Can not format value - error: {0}",
                            ef.getMessage());
                    return "" + dateValue;
                }
            } else {
                // use standard formate short/short
                singleValue = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(dateValue);
            }

        } else {
            // test if number formater is provided....
            if (format != null && format.contains("#")) {
                try {
                    double d = Double.parseDouble(o.toString());
                    singleValue = customNumberFormat(format, locale, d);
                } catch (IllegalArgumentException e) {
                    logger.log(Level.WARNING, "Format Error ({0}) = {1}", new Object[] { format, e.getMessage() });
                    singleValue = "0";
                }

            } else {
                // return object as string
                singleValue = o.toString();
            }
        }

        return singleValue;

    }

    /**
     * Resolves the document context for a given ref attribute value.
     *
     * If ref is null or empty, the current workitem is returned directly. Otherwise
     * the ref is treated as an item name in the current workitem whose value holds
     * the uniqueId of the referenced workitem. Loaded workitems are cached in
     * refCache to avoid repeated DB lookups within one adaptText() call.
     *
     * @param ref             the ref attribute value, may be null
     * @param currentWorkitem the current workitem from the TextEvent
     * @param refCache        cache map for already loaded referenced workitems
     * @return the resolved ItemCollection, or null if the ref could not be resolved
     */
    private ItemCollection resolveRef(String ref, ItemCollection currentWorkitem,
            Map<String, ItemCollection> refCache) {
        // No ref attribute — use the current workitem directly
        if (ref == null || ref.isEmpty()) {
            return currentWorkitem;
        }

        // Return from cache if already loaded
        if (refCache.containsKey(ref)) {
            return refCache.get(ref);
        }

        // The ref attribute value is the name of an item in the current workitem
        // that holds the uniqueId of the referenced workitem.
        if (documentService == null) {
            // No CDI context available (e.g. unit tests) — ref cannot be resolved
            logger.warning("TextItemValueAdapter: documentService not available — ref attribute ignored");
            return currentWorkitem;
        }
        String uniqueId = currentWorkitem.getItemValueString(ref);
        if (uniqueId == null || uniqueId.isEmpty()) {
            logger.log(Level.WARNING,
                    "TextItemValueAdapter: ref item ''{0}'' is empty or not found in current workitem", ref);
            return null;
        }

        // Load the referenced workitem and cache it
        ItemCollection referencedWorkitem = documentService.load(uniqueId);
        if (referencedWorkitem != null) {
            refCache.put(ref, referencedWorkitem);
        }
        return referencedWorkitem;
    }
}
