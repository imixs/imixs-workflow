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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.TextEvent;
import org.imixs.workflow.util.XMLParser;
import org.imixs.workflow.util.XMLTag;

import jakarta.annotation.Priority;
import jakarta.ejb.Stateless;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;

/**
 * The TextForEachRefAdapter can be used to format text fragments with the
 * 'for-each-ref' tag. The adapter will iterate over all workitems referenced in
 * the specified item.
 * <p>
 * The content of the for-each block will be processed in the context for each
 * referred ItemCollection:
 * 
 * <pre>
 * {@code
  <for-each item="_orderitems">
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
public class TextForEachRefAdapter {

    private static final Logger logger = Logger.getLogger(TextForEachRefAdapter.class.getName());

    @Inject
    protected Event<TextEvent> textEvents;

    @Inject
    DocumentService documentService;

    /**
     * This method reacts on CDI events of the type TextEvent and parses a string
     * for xml tag <for-each>. Those tags will be replaced with the corresponding
     * system property value.
     * <p>
     * The priority of the CDI event is set to (APPLICATION-10) to ensure that the
     * for-each adapter is triggered before the TextItemValueAdapter
     * 
     */
    public void onEvent(@Observes @Priority(Interceptor.Priority.APPLICATION - 10) TextEvent event) {

        String text = event.getText();
        boolean debug = logger.isLoggable(Level.FINE);

        List<XMLTag> tagList = XMLParser.parseTagMatches(text, "for-each-ref");
        if (debug) {
            logger.log(Level.FINEST, "......{0} tags found", tagList.size());
        }

        // Iterate in reverse order for safe position-based replacement
        for (int i = tagList.size() - 1; i >= 0; i--) {
            XMLTag tag = tagList.get(i);
            String textResult = "";

            String itemName = tag.getAttribute("item");
            String innervalue = tag.getContent();

            // Load each referenced workitem by its uniqueid
            List<String> values = event.getDocument().getItemValue(itemName);
            for (String ref : values) {
                ItemCollection _tempDoc = documentService.load(ref);
                if (_tempDoc != null) {
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
                } else {
                    logger.log(Level.WARNING, "for-each-ref: workitem ''{0}'' not found!", ref);
                }
            }

            // Replace by exact position — safe even with duplicate tag content
            text = text.substring(0, tag.getStartPos()) + textResult + text.substring(tag.getEndPos());
        }
        event.setText(text);
    }

}
