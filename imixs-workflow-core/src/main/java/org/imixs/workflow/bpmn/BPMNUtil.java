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

package org.imixs.workflow.bpmn;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.BPMNNS;
import org.openbpmn.bpmn.BPMNTypes;
import org.openbpmn.bpmn.elements.Activity;
import org.openbpmn.bpmn.elements.Event;
import org.openbpmn.bpmn.elements.Gateway;
import org.openbpmn.bpmn.elements.SequenceFlow;
import org.openbpmn.bpmn.elements.core.BPMNElementNode;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The {@code BPMNUtil} provides convenient methods to access elements and
 * bpmn2:extension tags within a Open-BPMN Model
 * 
 * 
 * Example for a :
 * 
 * <pre>{@code  
 * <bpmn2:task id="Task_2" imixs:processid="1900" name="Approve">
      <bpmn2:extensionElements>
        <imixs:item name="user.name" type="xs:string">John</imixs:item>
        ....
      </bpmn2:extensionElements>
        }</pre>
 * 
 */
public class BPMNUtil {
    private static Logger logger = Logger.getLogger(BPMNUtil.class.getName());

    public final static String TASK_ITEM_NAME = "name";
    public final static String TASK_ITEM_TASKID = "taskid";
    public final static String TASK_ITEM_DOCUMENTATION = "documentation";
    public final static String TASK_ITEM_WORKFLOW_SUMMARY = "workflow.summary";
    public final static String TASK_ITEM_WORKFLOW_ABSTRACT = "workflow.abstract";
    public final static String TASK_ITEM_APPLICATION_EDITOR = "application.editor";
    public final static String TASK_ITEM_APPLICATION_ICON = "application.icon";
    public final static String TASK_ITEM_APPLICATION_TYPE = "application.type";
    public final static String TASK_ITEM_ACL_OWNER_LIST = "acl.owner_list";
    public final static String TASK_ITEM_ACL_OWNER_LIST_MAPPING = "acl.owner_list_mapping";
    public final static String TASK_ITEM_ACL_READACCESS_LIST = "acl.readaccess_list";
    public final static String TASK_ITEM_ACL_READACCESS_LIST_MAPPING = "acl.readaccess_list_mapping";
    public final static String TASK_ITEM_ACL_WRITEACCESS_LIST = "acl.writeaccess_list";
    public final static String TASK_ITEM_ACL_WRITEACCESS_LIST_MAPPING = "acl.writeaccess_list_mapping";
    public final static String TASK_ITEM_ACL_UPDATE = "acl.update";

    public final static String EVENT_ITEM_NAME = "name";
    public final static String EVENT_ITEM_EVENTID = "eventid";
    public final static String EVENT_ITEM_DOCUMENTATION = "documentation";
    public final static String EVENT_ITEM_ACL_OWNER_LIST = "acl.owner_list";
    public final static String EVENT_ITEM_ACL_OWNER_LIST_MAPPING = "acl.owner_list_mapping";
    public final static String EVENT_ITEM_ACL_READACCESS_LIST = "acl.readaccess_list";
    public final static String EVENT_ITEM_ACL_READACCESS_LIST_MAPPING = "acl.readaccess_list_mapping";
    public final static String EVENT_ITEM_ACL_WRITEACCESS_LIST = "acl.writeaccess_list";
    public final static String EVENT_ITEM_ACL_WRITEACCESS_LIST_MAPPING = "acl.writeaccess_list_mapping";
    public final static String EVENT_ITEM_ACL_UPDATE = "acl.update";

    public final static String EVENT_ITEM_WORKFLOW_RESULT = "workflow.result";
    public final static String EVENT_ITEM_WORKFLOW_PUBLIC = "workflow.public";
    public final static String EVENT_ITEM_WORKFLOW_PUBLIC_ACTORS = "workflow.public_actors";
    public final static String EVENT_ITEM_READACCESS = "$readaccess";
    public final static String EVENT_ITEM_HISTORY_MESSAGE = "history.message";
    public final static String EVENT_ITEM_MAIL_SUBJECT = "mail.subject";
    public final static String EVENT_ITEM_MAIL_BODY = "mail.body";
    public final static String EVENT_ITEM_MAIL_TO_LIST = "mail.to_list";
    public final static String EVENT_ITEM_MAIL_TO_LIST_MAPPING = "mail.to_list_mapping";
    public final static String EVENT_ITEM_MAIL_CC_LIST = "mail.cc_list";
    public final static String EVENT_ITEM_MAIL_CC_LIST_MAPPING = "mail.cc_list_mapping";
    public final static String EVENT_ITEM_MAIL_BCC_LIST = "mail.bcc_list";
    public final static String EVENT_ITEM_MAIL_BCC_LIST_MAPPING = "mail.bcc_list_mapping";
    public final static String EVENT_ITEM_RULE_ENGINE = "rule.engine";
    public final static String EVENT_ITEM_RULE_DEFINITION = "rule.definition";

    public final static String EVENT_ITEM_REPORT_NAME = "report.name";
    public final static String EVENT_ITEM_REPORT_PATH = "report.path";
    public final static String EVENT_ITEM_REPORT_OPTIONS = "report.options";
    public final static String EVENT_ITEM_REPORT_TARGET = "report.target";
    public final static String EVENT_ITEM_VERSION_MODE = "version.mode";
    public final static String EVENT_ITEM_VERSION_EVENT = "version.event";

    public final static String EVENT_ITEM_TIMER_ACTIVE = "timer.active";
    public final static String EVENT_ITEM_TIMER_SELECTION = "timer.selection";
    public final static String EVENT_ITEM_TIMER_DELAY = "timer.delay";
    public final static String EVENT_ITEM_TIMER_DELAY_UNIT = "timer.delay_unit";
    public final static String EVENT_ITEM_TIMER_DELAY_BASE = "timer.delay_base";
    public final static String EVENT_ITEM_TIMER_DELAY_BASE_PROPERTY = "timer.delay_base_property";

    public static String getNamespace() {
        return "imixs";
    }

    public static String getNamespaceURI() {
        return "http://www.imixs.org/bpmn2";
    }

    /**
     * Private constructor to prevent instantiation
     */
    private BPMNUtil() {
    }

    /**
     * This method resolves the imixs version of a model.
     * <p>
     * The method parses the version directly form the imixs:extension element from
     * teh definitions node.
     * 
     * Note: we can not call findDefinition here because of recursive call
     * 
     * @param model
     * @return
     */
    public static String getVersion(BPMNModel model) {
        if (model != null) {
            List<String> valueList = getItemValueList(model, model.getDefinitions(), "txtworkflowmodelversion",
                    null);
            if (valueList != null && valueList.size() > 0) {
                return valueList.get(0);
            }
        }
        return null;
    }

    /**
     * This helper method returns a value list of a given imixs extension element.
     * If no values exists, than the method returns an empty List
     * 
     * The method also avoids duplicates as this can of course not be handled by the
     * react component.
     * 
     * 
     * @param itemName      - name of the item
     * @param referenceList - optional list of allowed values
     * @return the itemValue list.
     */
    public static List<String> getItemValueList(Element imixsItemElement) {
        List<String> uniqueValueList = new ArrayList<>();

        if (imixsItemElement != null) {
            // now iterate over all item:values and add each value into the list
            // <imixs:value><![CDATA[form_basic]]></imixs:value>
            Set<Element> imixsValueElementList = findAllImixsElements(imixsItemElement, "value");
            if (imixsValueElementList != null) {
                for (Element imixsValueElement : imixsValueElementList) {
                    String value = resolveImixsValueElement(imixsValueElement);
                    // avoid duplicates
                    if (value.contains("|")) {
                        String valuePart = value.substring(value.indexOf("|") + 1).trim();
                        if (uniqueValueList.contains(valuePart)) {
                            continue;
                        }
                        uniqueValueList.add(valuePart);
                    } else {
                        if (uniqueValueList.contains(value)) {
                            continue;
                        }
                        uniqueValueList.add(value);
                    }
                }

            }
        }
        return uniqueValueList;
    }

    /**
     * This helper method returns the content of a given Imixs Value Node.
     * The node can contain multiple CDATA sections!
     *
     * @return String - can be empty
     */
    private static String resolveImixsValueElement(Element imixsItemValue) {
        List<Node> cdataNodes = findAllCDATA(imixsItemValue);
        // do we have CDATA nodes?
        if (!cdataNodes.isEmpty()) {
            // Concatenate all CDATA sections
            StringBuilder content = new StringBuilder();
            for (Node cdata : cdataNodes) {
                content.append(cdata.getNodeValue());
            }
            return content.toString();
        } else {
            // normal text node, just return the content
            return imixsItemValue.getTextContent();
        }
    }

    /**
     * Helper method that finds all CDATA nodes within the current element.
     * In complex situations there can be nested CDATA sections inside one tag.
     * 
     * @param element
     * @return
     */
    private static List<Node> findAllCDATA(Element element) {
        List<Node> cdataNodes = new ArrayList<>();
        NodeList childNodes = element.getChildNodes();

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node instanceof CDATASection) {
                cdataNodes.add(node);
            }
        }
        return cdataNodes;
    }

    /**
     * This helper method returns a value list of all imixs:value elements of an
     * imixs:item by a given name.
     * If no extensionElement exists, or no imixs:item with the itemName exists,
     * than the method returns an empty List
     * 
     * The method also avoids duplicates as this can of course not be handled by the
     * react component.
     * 
     * An optional 'referenceList' can be provided (e.g. the Actor Mapping List). If
     * an existing value is not part of the referenceList, than the value will not
     * be set! This is to avoid holding old field mappings. See Issue #18
     * 
     * @param itemName      - name of the item
     * @param referenceList - optional list of allowed values
     * @return the itemValue list.
     */
    private static List<String> getItemValueList(final BPMNModel model, final Element elementNode, String itemName,
            List<String> referenceList) {
        Element extensionElement = model.findChildNodeByName(elementNode, BPMNNS.BPMN2, "extensionElements");
        List<String> uniqueValueList = new ArrayList<>();

        List<String> result = new ArrayList<>();
        if (extensionElement != null) {
            // first find the matching imixs:item
            Element imixsItemElement = findItemByName(extensionElement, itemName);
            if (imixsItemElement != null) {
                // now iterate over all item:values and add each value into the list
                // <imixs:value><![CDATA[form_basic]]></imixs:value>
                Set<Element> imixsValueElementList = findAllImixsElements(imixsItemElement, "value");
                if (imixsValueElementList != null) {
                    for (Element imixsValueElement : imixsValueElementList) {
                        String value = resolveImixsValueElement(imixsValueElement);

                        // avoid duplicates
                        if (value.contains("|")) {
                            String valuePart = value.substring(value.indexOf("|") + 1).trim();
                            if (uniqueValueList.contains(valuePart)) {
                                continue;
                            }
                            uniqueValueList.add(valuePart);
                        } else {
                            if (uniqueValueList.contains(value)) {
                                continue;
                            }
                            uniqueValueList.add(value);
                        }

                        // add value - it is now unique!
                        if (referenceList == null || referenceList.contains(value)) {
                            result.add(value);
                        }
                    }

                }

            }
        }
        // no item found with this item name - return an empty list
        return result;
    }

    /**
     * Helper method that finds a extension item by name
     * <p>
     * 
     * <pre>{@code<imixs:item name="ITEMNAME" type="xs:string">}</pre>
     * 
     * @param extensionElement
     * @param itemName
     * @return
     */
    private static Element findItemByName(Element extensionElement, String itemName) {

        Set<Element> itemElements = findAllImixsElements(extensionElement, "item");
        // iterate through set and verify the name attribute
        for (Element item : itemElements) {
            if (itemName.equals(item.getAttribute("name"))) {
                // match!
                return item;
            }
        }
        return null;
    }

    /**
     * This helper method returns a set of all imixs:ELEMENTS for the given parent
     * node. If no nodes were found, the method returns an empty list.
     * <p>
     * The type can be either 'item' or 'value'
     * 
     * Example:
     * 
     * <pre>{@code<imixs:item name="user.name" type=
     * "xs:string">John</imixs:item>}</pre>
     * 
     * @param parent
     * @param nodeName
     * @return - list of nodes. If no nodes were found, the method returns an empty
     *         list
     */
    public static Set<Element> findAllImixsElements(Element parent, String type) {
        Set<Element> result = new LinkedHashSet<Element>();
        // resolve the tag name
        String tagName = getNamespace() + ":" + type;
        if (parent != null) {
            NodeList childs = parent.getChildNodes();
            if (childs != null) {
                for (int i = 0; i < childs.getLength(); i++) {
                    Node childNode = childs.item(i);
                    if (childNode != null && childNode.getNodeType() == Node.ELEMENT_NODE
                            && tagName.equals(childNode.getNodeName())) {
                        result.add((Element) childNode);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Returns true if the given BPMNElement is a Imixs Task element
     * 
     * <bpmn2:task id="Task_2" imixs:processid="1000" name="Task 1">
     * 
     * @return
     */
    public static boolean isImixsTaskElement(BPMNElementNode element) {
        return (element instanceof Activity && element.hasAttribute(getNamespace() + ":processid"));

    }

    /**
     * Returns true if the given BPMNElement is a Imixs Event element
     * 
     * <bpmn2:intermediateCatchEvent id="CatchEvent_2" imixs:activityid="20" >
     * 
     * @return
     */
    public static boolean isImixsEventElement(BPMNElementNode element) {
        return (element instanceof Event && element.hasAttribute(getNamespace() + ":activityid"));
    }

    /**
     * Returns true if the given BPMNElement is a BPMN TROW_EVENT with a Link
     * definition
     * 
     * <pre>{@code<bpmn2:intermediateThrowEvent id="event_ounTaA" name="HOLD">
     *   <bpmn2:linkEventDefinition id="linkEventDefinition_343OGA"/>
     *   ....
     * </bpmn2:intermediateCatchEvent>}</pre>
     * 
     * @return
     */
    public static boolean isLinkCatchEventElement(BPMNElementNode element) {
        if (element instanceof Event && BPMNTypes.THROW_EVENT.equals(element.getType())) {
            // test if we find a Link definition
            Set<Element> linkDefinitions = ((Event) element).getEventDefinitionsByType(BPMNTypes.EVENT_DEFINITION_LINK);
            if (linkDefinitions != null && linkDefinitions.size() > 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if the given BPMNElement is a BPMN ParallelGateway
     * 
     * @return
     */
    public static boolean isParallelGatewayElement(BPMNElementNode element) {
        if (element instanceof Gateway && BPMNTypes.PARALLEL_GATEWAY.equals(element.getType())) {
            return true;
        }
        return false;
    }

    /**
     * Returns true if the given node is a an ImixsEvent node with no incoming
     * nodes or with one incoming node that comes from a Start event.
     * 
     */
    public static boolean isInitEventNode(BPMNElementNode eventNode) {
        if (BPMNUtil.isImixsEventElement(eventNode)) {
            Set<SequenceFlow> flowSet = eventNode.getIngoingSequenceFlows();
            if (flowSet.isEmpty()) {
                // no incoming flows - match!
                return true;
            } else if (flowSet.size() == 1) {
                // is the incoming flow coming from a bpmn2:startEvent?
                BPMNElementNode sourceElement = flowSet.iterator().next().getSourceElement();
                return BPMNTypes.START_EVENT.equals(sourceElement.getType());
            } else {
                // undefined - more than one incoming flow
            }
        }
        return false;
    }

}
