@XmlSchema(elementFormDefault = XmlNsForm.QUALIFIED, attributeFormDefault = XmlNsForm.UNQUALIFIED, xmlns = {
        @XmlNs(namespaceURI = "http://www.w3.org/2001/XMLSchema-instance", prefix = "xsi"),
        @XmlNs(namespaceURI = "http://www.w3.org/2001/XMLSchema", prefix = "xs") }

)
package org.imixs.workflow.xml;

import jakarta.xml.bind.annotation.XmlNs;
import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;

/**
 * package-info.java is used to define the general xml name spaces used by the
 * jax-b marshaler. This will define the following namespaces :
 * 
 * <code>
 * 	xmlns:xs="http://www.w3.org/2001/XMLSchema" 
 *  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 * </code>
 * 
 * @author rsoika
 * 
 */
