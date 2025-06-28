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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for DocumentService
 * 
 * 
 * @author rsoika
 */
public class TestDocumentServiceRegex {

    private DocumentService documentService = null;
    private final static Logger logger = Logger.getLogger(TestDocumentServiceRegex.class.getName());

    @BeforeEach
    public void setUp() throws PluginException, ModelException {
        documentService = new DocumentService();
    }

    /**
     * Test uid patterns
     * 
     */
    @Test
    public void testRegexUID() {

        // test standard uid
        String uid = WorkflowKernel.generateUniqueID();
        logger.log(Level.INFO, "verify uid pattern: {0}", uid);
        assertTrue(documentService.isValidUIDPattern(uid));

        // test UUID + snapshot
        uid = WorkflowKernel.generateUniqueID() + "-" + System.currentTimeMillis();
        logger.log(Level.INFO, "verify uid snapshot pattern: {0}", uid);
        assertTrue(documentService.isValidUIDPattern(uid));

        // test old pattern
        uid = "14c1463c9ef-13f6ef4e";
        logger.log(Level.INFO, "verify old uid pattern: {0}", uid);
        assertTrue(documentService.isValidUIDPattern(uid));

        // test old snapshot pattern
        uid = "14c1463c9ef-13f6ef4e" + "-" + System.currentTimeMillis();
        logger.log(Level.INFO, "verify old uid snapshot pattern: {0}", uid);
        assertTrue(documentService.isValidUIDPattern(uid));

    }

    @Test
    public void testInvalidRegexUID() {

        // test wrong pattern
        String uid = "123";
        assertFalse(documentService.isValidUIDPattern(uid));

        uid = "XXX";
        assertFalse(documentService.isValidUIDPattern(uid));

        uid = "aaa-bbb-ccc";
        assertFalse(documentService.isValidUIDPattern(uid));

    }

}
