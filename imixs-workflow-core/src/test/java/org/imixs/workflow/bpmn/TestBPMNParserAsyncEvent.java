package org.imixs.workflow.bpmn;

import java.io.IOException;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.exceptions.ModelException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;
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
public class TestBPMNParserAsyncEvent {
    BPMNModel model = null;
    ModelManager openBPMNModelManager = null;

    @Before
    public void setup() throws ParseException, ParserConfigurationException, SAXException, IOException {
        openBPMNModelManager = new ModelManager();
        try {
            openBPMNModelManager.addModel(BPMNModelFactory.read("/bpmn/asyncEventSimple.bpmn"));
            model = openBPMNModelManager.getModel("1.0.0");
            Assert.assertNotNull(model);
        } catch (ModelException | BPMNModelException e) {
            e.printStackTrace();
            Assert.fail();
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
        ItemCollection task = openBPMNModelManager.findTaskByID(model, 100);
        Assert.assertNotNull(task);
        Assert.assertEquals(100, task.getItemValueInteger("boundaryEvent.targetEvent"));
        Assert.assertEquals(1000, task.getItemValueInteger("boundaryEvent.timerEventDefinition.timeDuration"));

        // test task 200
        task = openBPMNModelManager.findTaskByID(model, 200);
        Assert.assertNotNull(task);
        Assert.assertEquals(200, task.getItemValueInteger("boundaryEvent.targetEvent"));
        Assert.assertEquals(0, task.getItemValueInteger("boundaryEvent.timerEventDefinition.timeDuration"));

    }

}
