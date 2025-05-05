package org.imixs.workflow.bpmn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.MockWorkflowContext;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.exceptions.ModelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openbpmn.bpmn.BPMNModel;
import org.xml.sax.SAXException;

/**
 * Test class test the Imixs BPMNParser concerning AsyncEvents.
 * <p>
 * An Async event is a boundary event attached to a task and will result in the
 * following task attributes:
 * <ul>
 * <li>boundaryEvent.targetEvent</li>
 * <li>boundaryEvent.timerEventDefinition.timeDuration (optional)</li>
 * </ul>
 * 
 * @author rsoika
 */
public class TestModelManagerAsyncEvent {

    BPMNModel model = null;
    ModelManager modelManager = null;
    MockWorkflowContext workflowContext;

    @BeforeEach
    public void setup() {
        try {
            workflowContext = new MockWorkflowContext();
            modelManager = new ModelManager(workflowContext);
            workflowContext.loadBPMNModelFromFile("/bpmn/asyncEventSimple.bpmn");
            model = workflowContext.fetchModel("1.0.0");
            assertNotNull(model);

        } catch (ModelException e) {
            fail(e.getMessage());
        }
    }

    /**
     * This test verifies two different boundaryEvents attached to different tasks
     * and events.
     * 
     * @throws ParseException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws ModelException
     */
    @Test
    public void testSimple()
            throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {

        // test task 100
        ItemCollection task = modelManager.findTaskByID(model, 100);
        assertNotNull(task);
        assertEquals(100, task.getItemValueInteger("boundaryEvent.targetEvent"));
        assertEquals(1000, task.getItemValueInteger("boundaryEvent.timerEventDefinition.timeDuration"));

        // test task 200
        task = modelManager.findTaskByID(model, 200);
        assertNotNull(task);
        assertEquals(200, task.getItemValueInteger("boundaryEvent.targetEvent"));
        assertEquals(0, task.getItemValueInteger("boundaryEvent.timerEventDefinition.timeDuration"));

    }

}
