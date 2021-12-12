package org.imixs.workflow.engine;

import java.util.logging.Logger;

import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for DocumentService
 * 
 * 
 * @author rsoika
 */
public class TestDocumentService extends WorkflowMockEnvironment {

    private DocumentService documentService = null;
    private final static Logger logger = Logger.getLogger(TestDocumentService.class.getName());

    @Before
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
        logger.info("verify uid pattern: " + uid);
        Assert.assertTrue(documentService.isValidUIDPattern(uid));

        // test UUID + snapshot
        uid = WorkflowKernel.generateUniqueID() + "-" + System.currentTimeMillis();
        logger.info("verify uid snapshot pattern: " + uid);
        Assert.assertTrue(documentService.isValidUIDPattern(uid));

        // test old pattern
        uid = "14c1463c9ef-13f6ef4e";
        logger.info("verify old uid pattern: " + uid);
        Assert.assertTrue(documentService.isValidUIDPattern(uid));

        // test old snapshot pattern
        uid = "14c1463c9ef-13f6ef4e" + "-" + System.currentTimeMillis();
        logger.info("verify old uid snapshot pattern: " + uid);
        Assert.assertTrue(documentService.isValidUIDPattern(uid));

    }

    @Test
    public void testInvalidRegexUID() {

        // test wrong pattern
        String uid = "123";
        Assert.assertFalse(documentService.isValidUIDPattern(uid));

        uid = "XXX";
        Assert.assertFalse(documentService.isValidUIDPattern(uid));

        uid = "aaa-bbb-ccc";
        Assert.assertFalse(documentService.isValidUIDPattern(uid));

    }

}
