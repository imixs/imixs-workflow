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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.util.XMLParser;

import jakarta.annotation.Priority;
import jakarta.ejb.Stateless;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;

/**
 * The TextForEachValueAdapter can be used to format text fragments with the
 * 'for-each' tag. The adapter will iterate over the value list of a specified
 * item.
 * 
 * <pre>
 * {@code
 <for-each item="_partid">
  Order-No: <itemvalue>_orderid</itemvalue> - Part ID: <itemvalue>_partid</itemvalue><br />
 </for-each>  
 * }
 * </pre>
 * 
 * In this example, the for-each block will be executed for each single value of
 * the item '_partid'. Within the for-each block it is possible to access the
 * current value of the iteration as also any other values of the current
 * document. The result may look like in the following example:
 * <p>
 * 
 * <pre>
 * {@code 
 * Order-No: 111222 - Part ID: A123
 * Order-No: 111222 - Part ID: B456
 * }
 * </pre>
 * 
 * 
 * @author rsoika
 *
 */
@Stateless
public class TextForEachValueAdapter {

    private static final Logger logger = Logger.getLogger(TextForEachValueAdapter.class.getName());

    @Inject
    protected Event<TextEvent> textEvents;

    /**
     * This method reacts on CDI events of the type TextEvent and parses a string
     * for xml tag <for-each-value>. Those tags will be replaced with the
     * corresponding system property value.
     * <p>
     * The priority of the CDI event is set to (APPLICATION-10) to ensure that the
     * for-each adapter is triggered before the TextItemValueAdapter
     * 
     */
    @SuppressWarnings("unchecked")
    public void onEvent(@Observes @Priority(Interceptor.Priority.APPLICATION - 10) TextEvent event) {

        String text = event.getText();
        String textResult = "";
        boolean debug = logger.isLoggable(Level.FINE);

        List<String> tagList = XMLParser.findNoEmptyTags(text, "for-each-value");
        if (debug) {
            logger.log(Level.FINEST, "......{0} tags found", tagList.size());
        }
        // test if a <for-each> tag exists...
        for (String tag : tagList) {
            // find the item value list...
            String itemName = XMLParser.findAttribute(tag, "item");
            String innervalue = XMLParser.findTagValue(tag, "for-each-value");

            // next we iterate over all item values and test for each value if the value is
            // a basic value or an embedded ItemCollection.
            List<Object> values = event.getDocument().getItemValue(itemName);
            for (Object _value : values) {
                ItemCollection _tempDoc = null;
                // We treat the value as a normal object and delegate the processing by firing
                // a TextEvent.
                // Here we need to create a temporary document for processing....
                _tempDoc = new ItemCollection(event.getDocument());
                // replace the for-each item value with the current iteration!
                _tempDoc.setItemValue(itemName, _value);

                // now we fire a recursive text event to process the content....
                TextEvent _event = new TextEvent(new String(innervalue), _tempDoc);
                if (textEvents != null) {
                    textEvents.fire(_event);
                    textResult = textResult + _event.getText();
                } else {
                    logger.warning("CDI Support is missing - TextEvent wil not be fired");
                    // here we apply a workaround for junit tests only....
                    TextItemValueAdapter tiva = new TextItemValueAdapter();
                    tiva.onEvent(_event);
                    textResult = textResult + _event.getText();
                }
            }

            // now replace the tag with the result string
            int iStartPos = text.indexOf(tag);
            int iEndPos = text.indexOf(tag) + tag.length();
            // now replace the tag with the result string
            text = text.substring(0, iStartPos) + textResult + text.substring(iEndPos);
        }
        event.setText(text);
    }

}
