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

package org.imixs.workflow.engine;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.util.XMLParser;
import org.imixs.workflow.util.XMLTag;

import jakarta.annotation.Priority;
import jakarta.ejb.Stateless;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;

/**
 * The TextForEachAdapter can be used to format text fragments with the
 * 'for-each-child' tag. The adapter will iterate over the embedded child items
 * specified by the tag item.
 * 
 * The content of the for-each block will be processed in the context for each
 * embedded ItemCollection:
 * 
 * <pre>
 * {@code
  <for-each-child item="_orderitems">
    <itemvalue>_orderid</itemvalue>: <itemvalue>_price</itemvalue>
  </for-each>  
 * }
 * </pre>
 * <p>
 * The result may look like in the following example:
 * <p>
 * 
 * <pre>
 * {@code 
 * Order ID: A123: 50.55
 * Order ID: B456: 150.10
 * }
 * </pre>
 * 
 * 
 * 
 * @author rsoika
 *
 */
@Stateless
public class TextForEachChildAdapter {

    private static final Logger logger = Logger.getLogger(TextForEachAdapter.class.getName());

    @Inject
    protected Event<TextEvent> textEvents;

    /**
     * This method reacts on CDI events of the type TextEvent and parses a string
     * for xml tag <for-each>. Those tags will be replaced with the corresponding
     * system property value.
     * <p>
     * The priority of the CDI event is set to (APPLICATION-10) to ensure that the
     * for-each adapter is triggered before the TextItemValueAdapter
     * 
     */
    @SuppressWarnings("unchecked")
    public void onEvent(@Observes @Priority(Interceptor.Priority.APPLICATION - 10) TextEvent event) {

        String text = event.getText();
        boolean debug = logger.isLoggable(Level.FINE);

        List<XMLTag> tagList = XMLParser.parseTagMatches(text, "for-each-child");
        if (debug) {
            logger.log(Level.FINEST, "......{0} tags found", tagList.size());
        }

        // Iterate in reverse order for safe position-based replacement
        for (int i = tagList.size() - 1; i >= 0; i--) {
            XMLTag tag = tagList.get(i);
            String textResult = "";

            String itemName = tag.getAttribute("item");
            String innervalue = tag.getContent();

            List<Object> values = event.getDocument().getItemValue(itemName);
            for (Object _value : values) {
                ItemCollection _tempDoc = null;
                // We expect an embedded ItemCollection
                if (_value instanceof Map<?, ?>) {
                    try {
                        _tempDoc = new ItemCollection((Map<String, List<Object>>) _value);
                    } catch (ClassCastException e) {
                        logger.warning("unable to cast embedded map to ItemCollection!");
                        continue;
                    }
                } else {
                    // Simple value lists are not supported for for-each-child
                    logger.warning(
                            "for-each-child is not supported for simple value lists! Use instead: for-each-value");
                    continue;
                }

                TextEvent _event = new TextEvent(new String(innervalue), _tempDoc);
                if (textEvents != null) {
                    textEvents.fire(_event);
                    textResult = textResult + _event.getText();
                } else {
                    logger.warning("CDI Support is missing - TextEvent wil not be fired");
                    TextItemValueAdapter tiva = new TextItemValueAdapter();
                    tiva.onEvent(_event);
                    textResult = textResult + _event.getText();
                }
            }

            // Replace by exact position — safe even with duplicate tag content
            text = text.substring(0, tag.getStartPos()) + textResult + text.substring(tag.getEndPos());
        }
        event.setText(text);
    }

}
