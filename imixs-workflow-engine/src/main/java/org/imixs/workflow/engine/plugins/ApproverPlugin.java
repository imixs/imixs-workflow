/*******************************************************************************
 * <pre>
 *  Imixs Workflow 
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
 *      Imixs Software Solutions GmbH - initial API and implementation
 *      Ralph Soika - Software Developer
 * </pre>
 *******************************************************************************/

package org.imixs.workflow.engine.plugins;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.plugins.AbstractPlugin;
import org.imixs.workflow.exceptions.PluginException;

/**
 * This plug-in is used to manage multiple users involved in a approver procedure. The list of
 * approvers can be declared within the workflow result:
 * <p>
 * Example:
 * 
 * <pre>
 * {@code
 *  <item name='approvedby'>ReviewTeam</item>
 * }
 * </pre>
 * 
 * The tag value (e.g. 'ReviewTeam') declares the source item holding the users involved in the
 * approver procedure. The plugin creates the following items to monitor the approver procedure:
 * 
 * <pre>
 * {@code
 *  [SOURCEITEMNAME]$Approvers 
 *  [SOURCEITEMNAME]$ApprovedBy
 * }
 * </pre>
 * 
 * If the source item is updated during the approving process, the plugin will add new userIDs if
 * these new UserIDs are not yet listed in the item [SOURCEITEMNAME]$ApprovedBy.
 * <p>
 * If the attribute 'refresh' is set to true, the list [SOURCEITEMNAME]$Approvers will be updated
 * (default is true).
 * <p>
 * If the attribute 'reset' is set to true, the list [SOURCEITEMNAME]$Approvers will be reseted and
 * the item [SOURCEITEMNAME]$ApprovedBy will be cleared.
 * 
 * @author rsoika
 * @version 2.0
 * 
 */
public class ApproverPlugin extends AbstractPlugin {

  private static Logger logger = Logger.getLogger(ApproverPlugin.class.getName());

  public static String APPROVEDBY = "$approvedby";
  public static String APPROVERS = "$approvers";

  private static String EVAL_APPROVEDBY = "approvedby";


  /**
   * computes the approvedBy and appovers name fields.
   * 
   * 
   * @throws PluginException
   * 
   **/
  @SuppressWarnings("unchecked")
  @Override
  public ItemCollection run(ItemCollection workitem, ItemCollection documentActivity)
      throws PluginException {
    boolean refresh = false;
    boolean reset = false;

    ItemCollection evalItemCollection =
        this.getWorkflowService().evalWorkflowResult(documentActivity, workitem);

    // test for items with name 'approvedby'
    if (evalItemCollection != null && evalItemCollection.hasItem(EVAL_APPROVEDBY)) {
      boolean debug = logger.isLoggable(Level.FINE);

      // test refresh
      refresh = true;
      if ("false".equals(evalItemCollection.getItemValueString(EVAL_APPROVEDBY + ".refresh"))) {
        refresh = false;
      }
      if (debug) {
        logger.fine("refresh=" + refresh);
      }
      // test reset
      reset = false;
      if ("true".equals(evalItemCollection.getItemValueString(EVAL_APPROVEDBY + ".reset"))) {
        reset = true;
      }
      if (debug) {
        logger.fine("reset=" + reset);
      }
      // 1.) extract the groups definitions
      List<String> groups = evalItemCollection.getItemValue(EVAL_APPROVEDBY);

      // 2.) iterate over all definitions
      for (String aGroup : groups) {

        // fetch name list...
        List<String> nameList = workitem.getItemValue(aGroup);
        // remove empty entries...
        nameList.removeIf(item -> item == null || "".equals(item));
        // create a new instance of a Vector to avoid setting the
        // same vector as reference! We also distinct the List here.
        List<String> newAppoverList = nameList.stream().distinct().collect(Collectors.toList());

        if (!workitem.hasItem(aGroup + APPROVERS) || reset) {
          if (debug) {
            logger.fine("creating new approver list: " + aGroup + "=" + newAppoverList);
          }
          workitem.replaceItemValue(aGroup + APPROVERS, newAppoverList);
          workitem.removeItem(aGroup + APPROVEDBY);
        } else {

          // refresh approver list.....
          if (refresh) {
            refreshApprovers(workitem, aGroup);
          }

          // 2.) add current approver to approvedBy.....
          String currentAppover = getWorkflowService().getUserName();
          List<String> listApprovedBy = workitem.getItemValue(aGroup + APPROVEDBY);
          List<String> listApprovers = workitem.getItemValue(aGroup + APPROVERS);
          if (debug) {
            logger.fine("approved by:  " + currentAppover);
          }
          if (listApprovers.contains(currentAppover) && !listApprovedBy.contains(currentAppover)) {
            listApprovers.remove(currentAppover);
            listApprovedBy.add(currentAppover);
            // remove empty entries...
            listApprovers.removeIf(item -> item == null || "".equals(item));
            listApprovedBy.removeIf(item -> item == null || "".equals(item));
            workitem.replaceItemValue(aGroup + APPROVERS, listApprovers);
            workitem.replaceItemValue(aGroup + APPROVEDBY, listApprovedBy);
            if (debug) {
              logger.fine("new list of approvedby: " + aGroup + "=" + listApprovedBy);
            }
          }
        }
      }

    }

    return workitem;
  }

  /**
   * This method verify if a new member of the existing approvers is available and adds new member
   * into the sourceItem$Approvers'. (issue #150)
   * 
   * @param workitem
   * @param sourceItem - item name of the source user list
   */
  @SuppressWarnings("unchecked")
  void refreshApprovers(ItemCollection workitem, String sourceItem) {
    boolean debug = logger.isLoggable(Level.FINE);
    List<String> nameList = workitem.getItemValue(sourceItem);
    // remove empty entries...
    nameList.removeIf(item -> item == null || "".equals(item));

    // create a new instance of a Vector to avoid setting the
    // same vector as reference! We also distinct the List here.
    List<String> newAppoverList = nameList.stream().distinct().collect(Collectors.toList());

    // verify if a new member of the existing approvers is available...
    // (issue #150)
    List<String> listApprovedBy = workitem.getItemValue(sourceItem + APPROVEDBY);
    List<String> listApprovers = workitem.getItemValue(sourceItem + APPROVERS);
    boolean update = false;
    for (String approver : newAppoverList) {
      if (!listApprovedBy.contains(approver) && !listApprovers.contains(approver)) {
        // add the new member to the existing approver list
        if (debug) {
          logger.fine("adding new approver to list '" + sourceItem + APPROVERS + "'");
        }
        listApprovers.add(approver);
        // remove empty entries...
        listApprovers.removeIf(item -> item == null || "".equals(item));

        update = true;
      }
    }
    if (update) {
      if (debug) {
        logger.fine("updating approver list '" + sourceItem + APPROVERS + "'");
      }
      workitem.replaceItemValue(sourceItem + APPROVERS, listApprovers);
    }
  }

}
