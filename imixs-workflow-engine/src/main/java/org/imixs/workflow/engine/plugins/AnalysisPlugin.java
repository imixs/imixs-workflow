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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.PluginException;

/**
 * This plugin can be used to measure the time of any phase during a workflow.
 * The plugin can be configured by the activity result :
 * 
 * Example: <code>
 *  <item name='measurepoint' type='start'>M1</item> 
 *  
 * </code>
 * 
 * defines a start point named 'M1'
 * 
 * <code>
 * <item name='measurepoint' type='stop'>M2</item>
 * 
 * </code>
 * 
 * definens a end point named 'M1'
 * 
 * The result will be stored into the txtWorkflowActivityLog (comments) and also
 * the Plugin will create the following fields:
 * 
 * - datMeasurePointStart_M1 : contains the start time points (list latest entry
 * on top!)
 * 
 * - datMeasurePointEnd_M1 : contains the end time points (list)
 * 
 * - numMeasurePoint_M1: contains the total time in milis.
 * 
 * With this logic we can measure any time aspact of a process instance
 * 
 * @author rsoika
 * 
 */
public class AnalysisPlugin extends AbstractPlugin {
    public static final String INVALID_FORMAT = "INVALID_FORMAT";

    private static final Logger logger = Logger.getLogger(AnalysisPlugin.class.getName());

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public ItemCollection run(ItemCollection documentContext, ItemCollection documentActivity) throws PluginException {

        logger.warning("The AnalysisPlugin is deprecated and will be removed in future releases. Please migrate to the new TaxonomyPlugin.");

        // parse for intem name=measurepoint....
        String sActivityResult = documentActivity.getItemValueString("txtActivityResult");
        List<MeasurePoint> measurePoints = evaluate(sActivityResult, documentContext);

        for (MeasurePoint point : measurePoints) {

            if ("start".equals(point.type)) {

                List valuesStart = documentContext.getItemValue("datMeasurePointStart_" + point.name);
                List valuesStop = documentContext.getItemValue("datMeasurePointStop_" + point.name);

                // the length of stop list must be the same as the start list
                if (valuesStart.size() != valuesStop.size()) {
                    logger.log(Level.WARNING, "[AnalysisPlugin] Wrong measure point ''{0}'' starttime without stoptime!"
                            + " - please check model entry {1}.{2} measurepoint will be ignored!",
                            new Object[]{point.name, documentActivity.getItemValueInteger("numProcessID"),
                                documentActivity.getItemValueInteger("numActivityID")});
                    continue;
                }

                // add new start point
                valuesStart.add(0, new Date());
                documentContext.replaceItemValue("datMeasurePointStart_" + point.name, valuesStart);

            }
            if ("stop".equals(point.type)) {
                List valuesStart = documentContext.getItemValue("datMeasurePointStart_" + point.name);
                List valuesStop = documentContext.getItemValue("datMeasurePointStop_" + point.name);

                if (valuesStop.size() != (valuesStart.size() - 1)) {
                    logger.log(Level.WARNING, "[AnalysisPlugin] Wrong measure point ''{0}'' stoptime without starttime!"
                            + " - please check model entry {1}.{2} measurepoint will be ignored!",
                            new Object[]{point.name, documentActivity.getItemValueInteger("numProcessID"),
                                documentActivity.getItemValueInteger("numActivityID")});
                    continue;

                }
                // add new start point
                valuesStop.add(0, new Date());
                documentContext.replaceItemValue("datMeasurePointStop_" + point.name, valuesStop);

                // now we add the new time range....
                int numTotal = documentContext.getItemValueInteger("numMeasurePoint_" + point.name);
                Date start = (Date) valuesStart.get(0);
                Date stop = (Date) valuesStop.get(0);

                long lStart = start.getTime() / 1000;
                long lStop = stop.getTime() / 1000;

                numTotal = (int) (numTotal + (lStop - lStart));
                documentContext.replaceItemValue("numMeasurePoint_" + point.name, numTotal);
            }
        }

        return documentContext;
    }

    /**
     * This method parses the a string for xml tag
     * <item type="start|stop">xxx</item>. Those tags will result in MeasurePoint
     * 
     * 
     * <code>
     *   
     *   <item name="measurepoint" type="start">M1</item>
     *   
     *   
     * </code>
     * 
     * @return - a list of measure points
     * @throws PluginException
     * 
     */
    public List<MeasurePoint> evaluate(String aString, ItemCollection documentContext) throws PluginException {
        int iTagStartPos;
        int iTagEndPos;

        int iContentStartPos;
        int iContentEndPos;

        int iNameStartPos;
        int iNameEndPos;

        int iTypeStartPos;
        int iTypeEndPos;

        String sName = "";
        String sType = " ";
        String sItemValue;

        List<MeasurePoint> result = new ArrayList<AnalysisPlugin.MeasurePoint>();

        if (aString == null || aString.isEmpty())
            return result;

        // test if a <value> tag exists...
        while ((iTagStartPos = aString.toLowerCase().indexOf("<item")) != -1) {

            iTagEndPos = aString.toLowerCase().indexOf("</item>", iTagStartPos);

            // if no end tag found return string unchanged...
            if (iTagEndPos == -1)
                throw new PluginException(ResultPlugin.class.getSimpleName(), INVALID_FORMAT, "</item>  expected!");

            // reset pos vars
            iContentStartPos = 0;
            iContentEndPos = 0;
            iNameStartPos = 0;
            iNameEndPos = 0;
            iTypeStartPos = 0;
            iTypeEndPos = 0;
            sName = "";
            sType = " ";
            sItemValue = "";

            // so we now search the beginning of the tag content
            iContentEndPos = iTagEndPos;
            // start pos is the last > before the iContentEndPos
            String sTestString = aString.substring(0, iContentEndPos);
            iContentStartPos = sTestString.lastIndexOf('>') + 1;

            // if no end tag found return string unchanged...
            if (iContentStartPos >= iContentEndPos)
                return result;

            iTagEndPos = iTagEndPos + "</item>".length();

            // now we have the start and end position of a tag and also the
            // start and end pos of the value

            // next we check if the start tag contains a 'name' attribute
            iNameStartPos = aString.toLowerCase().indexOf("name=", iTagStartPos);
            // extract format string if available
            // ' can be used instead of " chars!
            // e.g.: name='txtName'> or name="txtName">
            if (iNameStartPos > -1 && iNameStartPos < iContentStartPos) {
                // replace ' with " before content start pos
                String sNamePart = aString.substring(0, iContentStartPos);
                sNamePart = sNamePart.replace("'", "\"");
                iNameStartPos = sNamePart.indexOf("\"", iNameStartPos) + 1;
                iNameEndPos = sNamePart.indexOf("\"", iNameStartPos + 1);
                sName = sNamePart.substring(iNameStartPos, iNameEndPos);
                sName = sName.toLowerCase();
            }

            // next we check if the start tag contains a 'type'
            // attribute
            iTypeStartPos = aString.toLowerCase().indexOf("type=", iTagStartPos);
            // extract format string if available
            if (iTypeStartPos > -1 && iTypeStartPos < iContentStartPos) {
                String sTypePart = aString.substring(0, iContentStartPos);
                sTypePart = sTypePart.replace("'", "\"");

                iTypeStartPos = sTypePart.indexOf("\"", iTypeStartPos) + 1;
                iTypeEndPos = sTypePart.indexOf("\"", iTypeStartPos + 1);
                sType = sTypePart.substring(iTypeStartPos, iTypeEndPos);
                sType = sType.toLowerCase();
            }

            // extract Item Value
            sItemValue = aString.substring(iContentStartPos, iContentEndPos);

            // if name= measurepoint and type=start|stop then we can make a
            // measurePoint!

            if ("measurepoint".equals(sName) && ("start".equals(sType) || "stop".equals(sType))) {
                MeasurePoint point = new MeasurePoint(sItemValue.toLowerCase(), sType);
                result.add(point);

            }

            // now cut the tag form the string
            aString = aString.substring(0, iTagStartPos) + "" + aString.substring(iTagEndPos);
        }

        return result;
    }

    /**
     * Measure Point
     * 
     * @author rsoika
     * 
     */
    private class MeasurePoint {

        public String name;
        public String type;// start|stop

        public MeasurePoint(String name, String type) {
            super();
            this.name = name;
            this.type = type;
        }

    }

}
