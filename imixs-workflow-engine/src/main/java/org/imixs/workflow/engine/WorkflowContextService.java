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

package org.imixs.workflow.engine;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ItemCollectionComparator;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.bpmn.BPMNUtil;
import org.imixs.workflow.engine.plugins.ResultPlugin;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.util.XMLParser;
import org.openbpmn.bpmn.BPMNModel;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.LocalBean;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

/**
 * The WorkflowService is the Java EE Implementation for the Imixs Workflow Core
 * API. This interface acts as a service facade and supports basic methods to
 * create, process and access workitems. The Interface extends the core api
 * interface org.imixs.workflow.WorkflowManager with getter methods to fetch
 * collections of workitems.
 * 
 * @author rsoika
 * 
 */

@DeclareRoles({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
        "org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
        "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RolesAllowed({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
        "org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
        "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
@LocalBean
public class WorkflowContextService implements WorkflowContext {

    public static final String INVALID_ITEMVALUE_FORMAT = "INVALID_ITEMVALUE_FORMAT";
    public static final String INVALID_TAG_FORMAT = "INVALID_TAG_FORMAT";

    @Inject
    DocumentService documentService;

    @Inject
    ModelService modelService;

    @Resource
    SessionContext ctx;

    @Inject
    protected Event<TextEvent> textEvents;

    private static final Logger logger = Logger.getLogger(WorkflowService.class.getName());

    public WorkflowContextService() {
        super();
    }

    /**
     * This returns a list of workflow events assigned to a given workitem. The
     * method evaluates the events for the current $modelversion and $taskid. The
     * result list is filtered by the properties 'keypublicresult' and
     * 'keyRestrictedVisibility'.
     * <p>
     * If the property keyRestrictedVisibility exits the method test if the current
     * username is listed in one of the namefields.
     * <p>
     * If the current user is in the role 'org.imixs.ACCESSLEVEL.MANAGERACCESS' the
     * property keyRestrictedVisibility will be ignored.
     * <p>
     * If the model version does not exist the model is resolved by regular
     * expressions using the method findModelByWorkitem
     * <p>
     * If not model can be found or the $taskID is not defined by the model a
     * ModelException is thrown.
     * 
     * @see imixs-bpmn
     * @param workitem
     * @return
     * @throws ModelException if model is not found or task is not defined by the
     *                        given model
     */
    @SuppressWarnings("unchecked")
    public List<ItemCollection> getEvents(ItemCollection workitem) throws ModelException {
        List<ItemCollection> result = new ArrayList<ItemCollection>();
        if (workitem == null) {
            return result;
        }
        // resolve model.....
        BPMNModel model = findModelByWorkitem(workitem);
        if (model == null) {
            throw new ModelException(
                    ModelException.INVALID_MODEL, "Model '" + workitem.getModelVersion() + "' not found.");
        }

        int taskId = workitem.getTaskID();
        ModelManager modelManager = new ModelManager();
        ItemCollection task = modelManager.findTaskByID(model, taskId);
        if (task == null) {
            throw new ModelException(
                    ModelException.UNDEFINED_MODEL_ENTRY,
                    "Task " + taskId + " not defined in model '" + workitem.getModelVersion() + "'.");
        }
        List<ItemCollection> eventList = modelManager.findEventsByTask(model, taskId);

        String username = getUserName();
        boolean bManagerAccess = ctx.isCallerInRole(DocumentService.ACCESSLEVEL_MANAGERACCESS);

        // now filter events which are not public (keypublicresult==false) or
        // restricted for current user (keyRestrictedVisibility).
        for (ItemCollection event : eventList) {
            // ad only activities with userControlled != No
            if ("0".equals(event.getItemValueString("keypublicresult"))) {
                continue;
            }

            // it is not necessary to evaluate $readaccess here (see Issue #832)

            // test RestrictedVisibility
            List<String> restrictedList = event.getItemValue("keyRestrictedVisibility");
            if (!bManagerAccess && !restrictedList.isEmpty()) {
                // test each item for the current user name...
                List<String> totalNameList = new ArrayList<String>();
                for (String itemName : restrictedList) {
                    totalNameList.addAll(workitem.getItemValue(itemName));
                }
                // remove null and empty values....
                totalNameList.removeAll(Collections.singleton(null));
                totalNameList.removeAll(Collections.singleton(""));
                if (!totalNameList.isEmpty() && !totalNameList.contains(username)) {
                    // event is not visible for current user!
                    continue;
                }
            }
            result.add(event);
        }

        // sort by event id
        Collections.sort(result, new ItemCollectionComparator("eventID", true));

        return result;

    }

    /**
     * Returns an instance of the EJB session context.
     * 
     * @return
     */
    public SessionContext getSessionContext() {
        return ctx;
    }

    /**
     * Returns an instance of the DocumentService EJB.
     * 
     * @return
     */
    public DocumentService getDocumentService() {
        return documentService;
    }

    /**
     * Obtain the java.security.Principal that identifies the caller and returns the
     * name of this principal.
     * 
     * @return the user name
     */
    public String getUserName() {
        return ctx.getCallerPrincipal().getName();

    }

    /**
     * Test if the caller has a given security role.
     * 
     * @param rolename
     * @return true if user is in role
     */
    public boolean isUserInRole(String rolename) {
        try {
            return ctx.isCallerInRole(rolename);
        } catch (Exception e) {
            // avoid a exception for a role request which is not defined
            return false;
        }
    }

    /**
     * This method returns a list of user names, roles and application groups the
     * caller belongs to.
     * 
     * @return
     */
    public List<String> getUserNameList() {
        return documentService.getUserNameList();
    }

    /**
     * The method evaluates the next task for a process instance (workitem) based on
     * the current model definition. A Workitem must at least provide the properties
     * $TASKID and $EVENTID.
     * <p>
     * During the evaluation life-cycle more than one events can be evaluated. This
     * depends on the model definition which can define follow-up-events,
     * split-events and conditional events.
     * <p>
     * The method did not persist the process instance or execute any plugin or
     * adapter classes.
     * 
     * @return Task entity
     * @throws PluginException
     * @throws ModelException
     */
    public ItemCollection evalNextTask(ItemCollection workitem) throws PluginException, ModelException {
        WorkflowKernel workflowkernel = new WorkflowKernel(this);
        ItemCollection task = workflowkernel.eval(workitem);
        return task;
    }

    /**
     * The method adaptText can be called to replace predefined xml tags included in
     * a text with custom values. The method fires a CDI event to inform
     * TextAdapterServices to parse and adapt a given text fragment.
     * 
     * @param text
     * @param documentContext
     * @return
     * @throws PluginException
     */
    public String adaptText(String text, ItemCollection documentContext) throws PluginException {
        // fire event
        if (textEvents != null) {
            TextEvent event = new TextEvent(text, documentContext);
            textEvents.fire(event);
            text = event.getText();
        } else {
            logger.warning("CDI Support is missing - TextEvent wil not be fired");
        }
        return text;
    }

    /**
     * The method adaptTextList can be called to replace a text with custom values.
     * The method fires a CDI event to inform TextAdapterServices to parse and adapt
     * a given text fragment. The method expects a textList result.
     * 
     * @param text
     * @param documentContext
     * @return
     * @throws PluginException
     */
    public List<String> adaptTextList(String text, ItemCollection documentContext) throws PluginException {
        // fire event
        if (textEvents != null) {
            TextEvent event = new TextEvent(text, documentContext);
            textEvents.fire(event);
            return event.getTextList();
        } else {
            logger.warning("CDI Support is missing - TextEvent wil not be fired");
        }
        // no result return default
        List<String> textList = new ArrayList<String>();
        textList.add(text);
        return textList;
    }

    /**
     * The method evaluates the WorkflowResult for a given BPMN event and returns a
     * ItemColleciton containing all item values of a specified xml tag. Each tag
     * definition must contain at least a name attribute and may contain an optional
     * list of additional attributes.
     * <p>
     * The method generates a item for each content element
     * and attribute value. e.g.:
     * <p>
     * {@code<item name="comment" ignore="true">text</item>}
     * <p>
     * This example will result in an ItemCollection with the attributes 'comment'
     * with value 'text' and 'comment.ignore' with the value 'true'
     * <p>
     * Also embedded itemVaues can be resolved (resolveItemValues=true):
     * <p>
     * {@code
     * 		<somedata>ABC<itemvalue>$uniqueid</itemvalue></somedata>
     * }
     * <p>
     * This example will result in a new item 'somedata' with the $uniqueid prefixed
     * with 'ABC'
     * 
     * @see https://stackoverflow.com/questions/1732348/regex-match-open-tags-except-xhtml-self-contained-tags
     * @param event
     * @param xmlTag            - XML tag to be evaluated
     * @param documentContext
     * @param resolveItemValues - if true, itemValue tags will be resolved.
     * @return eval itemCollection or null if no tags are contained in the workflow
     *         result.
     * @throws PluginException if the xml structure is invalid
     */
    public ItemCollection evalWorkflowResult(ItemCollection event, String xmlTag, ItemCollection documentContext,
            boolean resolveItemValues) throws PluginException {
        boolean debug = logger.isLoggable(Level.FINE);
        ItemCollection result = new ItemCollection();
        String workflowResult = event.getItemValueString(BPMNUtil.EVENT_ITEM_WORKFLOW_RESULT);
        // support deprecated itemname
        if (workflowResult.isEmpty()) {
            workflowResult = event.getItemValueString("txtActivityResult");
        }
        if (workflowResult.trim().isEmpty()) {
            return null;
        }
        if (xmlTag == null || xmlTag.isEmpty()) {
            logger.warning("cannot eval workflow result - no tag name specified. Verify model!");
            return null;
        }

        // if no <tag exists we skip the evaluation...
        if (workflowResult.indexOf("<" + xmlTag) == -1) {
            return null;
        }

        // replace dynamic values?
        if (resolveItemValues) {
            workflowResult = adaptText(workflowResult, documentContext);
        }

        boolean invalidPattern = false;
        // Fast first test if the tag really exists....
        Pattern patternSimple = Pattern.compile("<" + xmlTag + " (.*?)>(.*?)|<" + xmlTag + " (.*?)./>", Pattern.DOTALL);
        Matcher matcherSimple = patternSimple.matcher(workflowResult);
        if (matcherSimple.find()) {
            invalidPattern = true;
            // we found the starting tag.....

            // Extract all tags with attributes using regex (including empty tags)
            // see also:
            // https://stackoverflow.com/questions/1732348/regex-match-open-tags-except-xhtml-self-contained-tags
            // e.g. <item(.*?)>(.*?)</item>|<item(.*?)./>
            Pattern pattern = Pattern
                    .compile("(?s)(?:(<" + xmlTag + "(?>\\b(?:\".*?\"|'.*?'|[^>]*?)*>)(?<=/>))|(<" + xmlTag
                            + "(?>\\b(?:\".*?\"|'.*?'|[^>]*?)*>)(?<!/>))(.*?)(</" + xmlTag + "\\s*>))", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(workflowResult);
            while (matcher.find()) {
                invalidPattern = false;
                // we expect up to 4 different result groups
                // group 0 contains complete tag string
                // groups 1 or 2 contain the attributes

                String content = "";
                String attributes = matcher.group(1);
                if (attributes == null) {
                    attributes = matcher.group(2);
                    content = matcher.group(3);
                } else {
                    content = matcher.group(2);
                }

                if (content == null) {
                    content = "";
                }

                // now extract the attributes to verify the tag name..
                if (attributes != null && !attributes.isEmpty()) {
                    // parse attributes...
                    String spattern = "(\\S+)=[\"']?((?:.(?![\"']?\\s+(?:\\S+)=|[>\"']))+.)[\"']?";
                    Pattern attributePattern = Pattern.compile(spattern);
                    Matcher attributeMatcher = attributePattern.matcher(attributes);
                    Map<String, String> attrMap = new HashMap<String, String>();
                    while (attributeMatcher.find()) {
                        String attrName = attributeMatcher.group(1); // name
                        String attrValue = attributeMatcher.group(2); // value
                        attrMap.put(attrName, attrValue);
                    }

                    String tagName = attrMap.get("name");
                    if (tagName == null) {
                        throw new PluginException(ResultPlugin.class.getSimpleName(), INVALID_TAG_FORMAT,
                                "<" + xmlTag + "> tag contains no name attribute.");
                    }

                    // now add optional attributes if available
                    for (String attrName : attrMap.keySet()) {
                        // we need to skip the 'name' attribute
                        if (!"name".equals(attrName)) {
                            result.appendItemValue(tagName + "." + attrName, attrMap.get(attrName));
                        }
                    }

                    // test if the type attribute was provided to convert content?
                    String sType = result.getItemValueString(tagName + ".type");
                    String sFormat = result.getItemValueString(tagName + ".format");
                    if (!sType.isEmpty()) {
                        // convert content type
                        if ("boolean".equalsIgnoreCase(sType)) {
                            result.appendItemValue(tagName, Boolean.valueOf(content));
                        } else if ("integer".equalsIgnoreCase(sType)) {
                            try {
                                result.appendItemValue(tagName, Integer.valueOf(content));
                            } catch (NumberFormatException e) {
                                // append 0 value
                                result.appendItemValue(tagName, new Integer(0));
                            }
                        } else if ("double".equalsIgnoreCase(sType)) {
                            try {
                                result.appendItemValue(tagName, Double.valueOf(content));
                            } catch (NumberFormatException e) {
                                // append 0 value
                                result.appendItemValue(tagName, new Double(0));
                            }
                        } else if ("float".equalsIgnoreCase(sType)) {
                            try {
                                result.appendItemValue(tagName, Float.valueOf(content));
                            } catch (NumberFormatException e) {
                                // append 0 value
                                result.appendItemValue(tagName, new Float(0));
                            }
                        } else if ("long".equalsIgnoreCase(sType)) {
                            try {
                                result.appendItemValue(tagName, Long.valueOf(content));
                            } catch (NumberFormatException e) {
                                // append 0 value
                                result.appendItemValue(tagName, new Long(0));
                            }
                        } else if ("date".equalsIgnoreCase(sType)) {
                            if (content == null || content.isEmpty()) {
                                // no value defined - remove item
                                result.removeItem(tagName);
                            } else {
                                // convert content value to date object
                                try {
                                    if (debug) {
                                        logger.finer("......convert string into date object");
                                    }
                                    Date dateResult = null;
                                    if (sFormat == null || sFormat.isEmpty()) {
                                        // use standard format short/short
                                        dateResult = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                                                .parse(content);
                                    } else {
                                        // use given formatter (see: TextItemValueAdapter)
                                        DateFormat dateFormat = new SimpleDateFormat(sFormat);
                                        dateResult = dateFormat.parse(content);
                                    }
                                    result.appendItemValue(tagName, dateResult);
                                } catch (ParseException e) {
                                    if (debug) {
                                        logger.log(Level.FINER, "failed to convert string into date object: {0}",
                                                e.getMessage());
                                    }
                                }
                            }

                        } else
                            // no type conversion
                            result.appendItemValue(tagName, content);
                    } else {
                        // no type definition
                        result.appendItemValue(tagName, content);
                    }

                } else {
                    throw new PluginException(ResultPlugin.class.getSimpleName(), INVALID_TAG_FORMAT,
                            "<" + xmlTag + "> tag contains no name attribute.");

                }
            }
        }

        // test for general invalid format
        if (invalidPattern) {
            throw new PluginException(ResultPlugin.class.getSimpleName(), INVALID_TAG_FORMAT,
                    "invalid <" + xmlTag + "> tag format in workflowResult: " + workflowResult
                            + "  , expected format is <"
                            + xmlTag + " name=\"...\">...</item> ");
        }
        return result;
    }

    /**
     * The method evaluates the WorkflowResult for a given BPMN event and returns a
     * ItemColleciton containing all item values of a specified tag name. Each tag
     * definition of a WorkflowResult contains a name and a optional list of
     * additional attributes. The method generates a item for each content element
     * and attribute value. <br>
     * e.g. <item name="comment" ignore="true">text</item> <br>
     * will result in the attributes 'comment' with value 'text' and
     * 'comment.ignore' with the value 'true'
     * <p>
     * Also embedded itemVaues can be resolved (resolveItemValues=true):
     * <p>
     * {@code
     *      <somedata>ABC<itemvalue>$uniqueid</itemvalue></somedata>
     * }
     * <p>
     * This example will result in a new item 'somedata' with the $uniqueid prefixed
     * with 'ABC'
     * 
     * @see evalWorkflowResult(ItemCollection event, String tag, ItemCollection
     *      documentContext,boolean resolveItemValues)
     * @param event
     * @param tag             - tag to be evaluated
     * @param documentContext
     * @return
     * @throws PluginException
     */
    public ItemCollection evalWorkflowResult(ItemCollection event, String tag, ItemCollection documentContext)
            throws PluginException {
        return evalWorkflowResult(event, tag, documentContext, true);
    }

    /**
     * The method evaluates a XML tag from the WorkflowResult for a given BPMN
     * event.
     * The method returns a list of ItemCollecitons matching the given XML tag and
     * name attribtue. A custom XML configuriaton may contain one or many XML tags
     * with the same
     * name. Each result ItemCollection holds the tag values of each XML tag.
     * 
     * Example:
     * 
     * <pre>
     * {@code
     * <imixs-config name="CONFIG">
     *   <textblock>....</textblock>
     *   <template>....</template>
     * </imixs-config>
     * }
     * </pre>
     * 
     * @param event
     * @param xmlTag            - xml tag to be evaluated
     * @param name              - value of the matching name attribute
     * @param documentContext
     * @param resolveItemValues - if true, itemValue tags will be resolved.
     * @return list of ItemCollections
     * @throws PluginException if the xml structure is invalid
     */
    public List<ItemCollection> evalWorkflowResultXML(ItemCollection event, String xmlTag, String name,
            ItemCollection documentContext,
            boolean resolveItemValues) throws PluginException {

        List<ItemCollection> result = new ArrayList<ItemCollection>();
        // find all xml configs with the given tat name
        ItemCollection configItemCol = evalWorkflowResult(event, xmlTag, documentContext, resolveItemValues);
        if (configItemCol == null) {
            // no configuration found!
            throw new PluginException(WorkflowService.class.getSimpleName(), INVALID_TAG_FORMAT,
                    "Missing XML definition '" + xmlTag + "' in Event " + event.getItemValueInteger("numprocessid")
                            + "." + event.getItemValueInteger("numactivityid"));
        }

        List<String> xmlDefinitions = configItemCol.getItemValueList(name, String.class);
        if (xmlDefinitions != null) {
            for (String definitionXML : xmlDefinitions) {
                if (definitionXML.trim().isEmpty()) {
                    // no definition
                    continue;
                }
                // evaluate the definition (XML format expected here!)
                ItemCollection xmlItemCol = XMLParser.parseItemStructure(definitionXML);
                if (xmlItemCol != null) {
                    result.add(xmlItemCol);
                }
            }
        }

        return result;

    }

    /**
     * Returns a Model matching the $modelversion of a given workitem. The
     * $modelversion can optional be provided as a regular expression.
     * <p>
     * In case no matching model version exits, the method tries to find the highest
     * Model Version matching the corresponding workflow group.
     * <p>
     * The method throws a ModelException in case the model version did not exits.
     **/
    public BPMNModel findModelByWorkitem(ItemCollection workitem) throws ModelException {
        BPMNModel result = null;
        String version = workitem.getModelVersion();
        // first try a direct fetch....
        if (version != null && !version.isEmpty()) {
            result = modelService.getModel(version);
        }

        if (result != null) {
            return result;
        } else {
            // try to find model by regex if version is not empty...
            if (version != null && !version.isEmpty()) {
                String matchingVersion = findVersionByRegEx(version);
                if (matchingVersion != null && !matchingVersion.isEmpty()) {
                    result = modelService.getModel(matchingVersion);
                    if (result != null) {
                        // match
                        // update $modelVersion
                        logger.fine("Update $modelversion by regex " + version + " ▷ " + matchingVersion);
                        workitem.model(matchingVersion);
                        return result;
                    }
                }
            }

            // Still no match, try to find model version by group
            if (!workitem.getWorkflowGroup().isEmpty()) {
                String matchingVersion = findVersionByGroup(workitem.getWorkflowGroup());
                if (matchingVersion != null && !matchingVersion.isEmpty()) {

                    // loggin...
                    if (version.isEmpty()) {
                        logger.log(Level.INFO, "Set model version ''{1}'',"
                                + "  $workflowgroup: ''{2}'', $uniqueid: {3}",
                                new Object[] { version, matchingVersion, workitem.getWorkflowGroup(),
                                        workitem.getUniqueID() });
                    } else {
                        logger.log(Level.INFO, "Update model version: ''{0}'' ▶ ''{1}'',"
                                + "  $workflowgroup: ''{2}'', $uniqueid: {3}",
                                new Object[] { version, matchingVersion, workitem.getWorkflowGroup(),
                                        workitem.getUniqueID() });
                    }

                    // update $modelVersion
                    workitem.model(matchingVersion);
                    result = modelService.getModel(matchingVersion);
                    return result;
                }
            }

            // no match!
            throw new ModelException(ModelException.UNDEFINED_MODEL_VERSION,
                    "$modelversion '" + version + "' not found");
        }
    }

    /**
     * This method returns a sorted list of model versions matching a given regex
     * for a model version. The result is sorted in reverse order, so the highest
     * version number is the first in the result list.
     * 
     * @param group
     * @return
     */
    // @Override
    public String findVersionByRegEx(String modelRegex) {
        boolean debug = logger.isLoggable(Level.FINE);
        // Sorted in reverse order
        Set<String> result = new TreeSet<>(Collections.reverseOrder());
        if (debug) {
            logger.log(Level.FINEST, "......searching model versions for regex ''{0}''...", modelRegex);
        }
        // try to find matching model version by regex
        List<String> modelVersions = modelService.getAllModelVersions();
        for (String _version : modelVersions) {
            if (Pattern.compile(modelRegex).matcher(_version).find()) {
                result.add(_version);
            }
        }
        if (result.size() > 0) {
            return result.iterator().next();
        }
        return null;
    }

    /**
     * Returns a version by Group.
     * The method computes a sorted list of all model versions containing the
     * requested
     * workflow group. The result is sorted in reverse order, so the highest version
     * number is the first in the result list.
     * 
     * @param group - name of the workflow group
     * @return list of matching model versions
     * @throws ModelException
     */
    // @Override
    public String findVersionByGroup(String group) throws ModelException {
        return modelService.findVersionByGroup(group);
    }
}
