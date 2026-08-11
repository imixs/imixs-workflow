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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.handler.TextTimestampAdapter;
import org.junit.jupiter.api.Test;

/**
 * Test class for TextTimestampAdapter.
 *
 */
public class TestTextTimestampAdapter {
    private final static Logger logger = Logger.getLogger(TestTextTimestampAdapter.class.getName());

    /**
     * Ein Einfacher test
     *
     * 
     * @throws Exception
     */
    @Test
    public void testBasic() throws Exception {

        TextTimestampAdapter adapter = new TextTimestampAdapter();

        ItemCollection source = new ItemCollection();
        String text = "Today is the <timestamp format=\"dd. MMM yyyy\" />";

        TextEvent event = new TextEvent(text, source);
        adapter.onEvent(event);

        String result = event.getText();

        // The tag itself must no longer be part of the result
        assertFalse(result.contains("<timestamp"));

        // Compute the expected value the same way the adapter does
        String expectedDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd. MMM yyyy"));

        logger.info("Result=" + result);
        assertTrue(result.contains(expectedDate));
        assertEquals("Today is the " + expectedDate, result);

    }

    @Test
    public void testAdjust() throws Exception {

        TextTimestampAdapter adapter = new TextTimestampAdapter();

        ItemCollection source = new ItemCollection();
        String text = "Yesterday was the <timestamp format=\"dd. MMM yyyy\" adjustDays=\"-1\" />";

        TextEvent event = new TextEvent(text, source);
        adapter.onEvent(event);

        String result = event.getText();

        // The tag itself must no longer be part of the result
        assertFalse(result.contains("<timestamp"));

        // Compute the expected value the same way the adapter does
        String expectedDate = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("dd. MMM yyyy"));

        logger.info("Result=" + result);
        assertTrue(result.contains(expectedDate));
        assertEquals("Yesterday was the " + expectedDate, result);

    }

}
