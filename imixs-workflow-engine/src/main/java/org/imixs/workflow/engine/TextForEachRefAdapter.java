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

    private static final Logger logger = Logger.getLogger(TextForEachAdapter.class.getName());

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
    @SuppressWarnings("unchecked")
    public void onEvent(@Observes @Priority(Interceptor.Priority.APPLICATION - 10) TextEvent event) {

        String text = event.getText();
        String textResult = "";
        boolean debug = logger.isLoggable(Level.FINE);

        List<String> tagList = XMLParser.findNoEmptyTags(text, "for-each-ref");
        if (debug) {
            logger.log(Level.FINEST, "......{0} tags found", tagList.size());
        }
        // test if a <for-each> tag exists...
        for (String tag : tagList) {
            // find the item value list...
            String itemName = XMLParser.findAttribute(tag, "item");
            String innervalue = XMLParser.findTagValue(tag, "for-each-ref");

            // next we iterate over all item values and test for each value if the value is
            // a basic value or an embedded ItemCollection.
            List<String> values = event.getDocument().getItemValue(itemName);
            for (String ref : values) {
                ItemCollection _tempDoc = documentService.load(ref);
                // test if the value defines an embedded ItemCollection....
                if (_tempDoc != null) {

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
