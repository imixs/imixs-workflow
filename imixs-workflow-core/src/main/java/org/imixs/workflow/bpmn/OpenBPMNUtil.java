package org.imixs.workflow.bpmn;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.BPMNNS;
import org.openbpmn.bpmn.elements.Activity;
import org.openbpmn.bpmn.elements.Event;
import org.openbpmn.bpmn.elements.core.BPMNElement;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This helper class provides methods to access extension tags within a
 * Open-BPMN Model
 * 
 * 
 * Example:
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
public class OpenBPMNUtil {

    private static Logger logger = Logger.getLogger(OpenBPMNUtil.class.getName());

    public final static String TASK_ITEM_NAME = "name";
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
    private OpenBPMNUtil() {
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
        List<String> valueList = getItemValueList(model, model.getDefinitions(), "txtworkflowmodelversion",
                null);
        if (valueList != null && valueList.size() > 0) {
            return valueList.get(0);
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
            Set<Element> imixsValueElements = findAllImixsElements(imixsItemElement, "value");
            if (imixsValueElements != null) {
                for (Element imixsItemValue : imixsValueElements) {
                    String value = null;
                    // we expect a CDATA, bu we can not be sure
                    Node cdata = findCDATA(imixsItemValue);
                    if (cdata != null) {
                        String cdValue = cdata.getNodeValue();
                        if (cdValue != null) {
                            value = cdValue;
                        }
                    } else {
                        // normal text node
                        value = imixsItemValue.getTextContent();
                    }

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
                Set<Element> imixsValueElements = findAllImixsElements(imixsItemElement, "value");
                if (imixsValueElements != null) {
                    for (Element imixsItemValue : imixsValueElements) {
                        String value = null;
                        // we expect a CDATA, bu we can not be sure
                        Node cdata = findCDATA(imixsItemValue);
                        if (cdata != null) {
                            String cdValue = cdata.getNodeValue();
                            if (cdValue != null) {
                                value = cdValue;
                            }
                        } else {
                            // normal text node
                            value = imixsItemValue.getTextContent();
                        }

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
            for (int i = 0; i < childs.getLength(); i++) {
                Node childNode = childs.item(i);
                if (childNode.getNodeType() == Node.ELEMENT_NODE && tagName.equals(childNode.getNodeName())) {
                    result.add((Element) childNode);
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
    public static boolean isImixsTaskElement(BPMNElement element) {
        return (element instanceof Activity && element.hasAttribute(getNamespace() + ":processid"));

    }

    /**
     * Returns true if the given BPMNElement is a Imixs Event element
     * 
     * <bpmn2:intermediateCatchEvent id="CatchEvent_2" imixs:activityid="20" >
     * 
     * @return
     */
    public static boolean isImixsEventElement(BPMNElement element) {
        return (element instanceof Event && element.hasAttribute(getNamespace() + ":activityid"));
    }

    /**
     * Helper method that finds an optional CDATA node within the current element
     * content.
     * 
     * @param element
     * @return
     */
    private static Node findCDATA(Element element) {
        // search CDATA node
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node instanceof CDATASection) {
                return (CDATASection) node;
            }
        }
        return null;
    }

}
