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

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.PluginException;

/**
 * This Plugin creates a history log in the property txtWorkflowHistory. The
 * history log contains a list of history entires. Each entry provides the
 * following information:
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
 * 
 */

public class HistoryPlugin extends AbstractPlugin {
    private ItemCollection documentContext;
    // private List<List<Object>> historyList = null;
    private static final Logger logger = Logger.getLogger(HistoryPlugin.class.getName());

    public static final String ITEM_HISTORY_LOG = "workflow.history";

    /**
     * Update the Log entry.
     * 
     * The method tests if the deprecated property 'txtworkflowhistorylogrev'
     * exists. In this case the old log format will be transformed into the new
     * format see method convertOldFormat
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ItemCollection run(ItemCollection adocumentContext, ItemCollection adocumentActivity)
            throws PluginException {
        String rtfItemLog;

        documentContext = adocumentContext;
        ItemCollection documentActivity = adocumentActivity;

        // convert old format if exists (backward compatibility (< 3.1.1)
        if (documentContext.hasItem("txtworkflowhistorylogrev")) {
            convertOldFormat();
            documentContext.removeItem("txtworkflowhistorylogrev");
        }

        // add logtext into history log
        rtfItemLog = documentActivity.getItemValueString("rtfresultlog");
        if (rtfItemLog.isEmpty())
            return documentContext;

        rtfItemLog = getWorkflowService().adaptText(rtfItemLog, documentContext);

        List<List<?>> temp = null;
        // test deprecated field
        if (!documentContext.hasItem(ITEM_HISTORY_LOG) && documentContext.hasItem("txtworkflowhistory")) {
            // migrate deprecated field name
            temp = documentContext.getItemValue("txtworkflowhistory");
        } else {
            temp = documentContext.getItemValue(ITEM_HISTORY_LOG);
        }

        // insert new entry
        List<Object> newEntry = new ArrayList<Object>();
        newEntry.add(documentContext.getItemValueDate(WorkflowKernel.LASTEVENTDATE));
        newEntry.add(rtfItemLog);
        newEntry.add(this.getWorkflowService().getUserName());
        temp.add(newEntry);
        // Sort the list by date in descending order
        Collections.sort(temp, new Comparator<List<?>>() {
            @Override
            public int compare(List<?> entry1, List<?> entry2) {
                Date date1 = (Date) entry1.get(0);
                Date date2 = (Date) entry2.get(0);
                // Compare in descending order
                return date1.compareTo(date2);
            }
        });

        documentContext.replaceItemValue(ITEM_HISTORY_LOG, temp);
        // we still support the deprecated item name
        documentContext.replaceItemValue("txtworkflowhistory", temp);

        return documentContext;
    }

    /**
     * This method converts the old StringList format in the new format with a list
     * of separated values:
     * 
     * <ul>
     * <li>date of creation (Date)</li>
     * <li>comment (String)</li>
     * <li>userID (String)</li>
     * </ul>
     * 
     */
    @SuppressWarnings("unchecked")
    protected void convertOldFormat() {
        List<List<Object>> newList = new ArrayList<List<Object>>();

        try {
            List<String> oldList = (List<String>) documentContext.getItemValue("txtworkflowhistorylog");

            for (String oldEntry : oldList) {
                if (oldEntry != null && !oldEntry.isEmpty() && oldEntry.indexOf(" : ") > -1) {
                    String sDate = oldEntry.substring(0, oldEntry.indexOf(" : "));
                    String sComment = oldEntry.substring(oldEntry.indexOf(" : ") + 3);
                    String sUser = "";
                    List<Object> newEntry = new ArrayList<Object>();
                    newEntry.add(convertDate(sDate));
                    newEntry.add(sComment);
                    newEntry.add(sUser);
                    newList.add(newEntry);
                }
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
            df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, new Locale("de", "DE"));

            result = df.parse(aDateString);
            return result;
        } catch (ParseException e) {
            // no opp
        }

        try {
            df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, new Locale("de", "DE"));
            result = df.parse(aDateString);
            return result;
        } catch (ParseException e) {
            // no opp
        }

        try {
            df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, new Locale("de", "DE"));
            result = df.parse(aDateString);
            return result;
        } catch (ParseException e) {
            // no opp
        }

        try {
            df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM, new Locale("de", "DE"));
            result = df.parse(aDateString);
            return result;
        } catch (ParseException e) {
            // no opp
        }

        try {
            df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, new Locale("de", "DE"));
            result = df.parse(aDateString);
            return result;
        } catch (ParseException e) {
            // no opp
        }

        try {
            df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, new Locale("de", "DE"));
            result = df.parse(aDateString);
            return result;
        } catch (ParseException e) {
            // no opp
        }

        try {
            df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG, new Locale("de", "DE"));
            result = df.parse(aDateString);
            return result;
        } catch (ParseException e) {
            // no opp
        }

        return null;
    }

}
