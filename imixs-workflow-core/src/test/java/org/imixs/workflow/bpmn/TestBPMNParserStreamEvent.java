package org.imixs.workflow.bpmn;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.ModelException;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import junit.framework.Assert;

/**
 * Test class test the Imixs BPMNParser concerning StreamEvents.
 * <p>
 * A stream event is a boundery event attached to a task and will result in the
 * following task attributes:
 * <ul>
 * <li>boundaryEvent.targetEvent</li>
 * <li>boundaryEvent.timerEventDefinition.timeDuration (optional)</li>
 * </ul>
 * 
 * @author rsoika
 */
public class TestBPMNParserStreamEvent {

    protected BPMNModel model = null;

    @Before
    public void setUp() throws ParseException, ParserConfigurationException, SAXException, IOException {
        InputStream inputStream = getClass().getResourceAsStream("/bpmn/streamEventSimple.bpmn");

        try {
            model = BPMNParser.parseModel(inputStream, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Assert.fail();
        } catch (ModelException e) {
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

        Assert.assertNotNull(model);
        // test count of elements
        Assert.assertEquals(3, model.findAllTasks().size());

        // test task 100
        ItemCollection task = model.getTask(100);
        Assert.assertNotNull(task);
        Assert.assertEquals("1.0.0", task.getItemValueString("$ModelVersion"));
        Assert.assertEquals(100, task.getItemValueInteger("boundaryEvent.targetEvent"));
        Assert.assertEquals(1000, task.getItemValueInteger("boundaryEvent.timerEventDefinition.timeDuration"));

        // test task 200
        task = model.getTask(200);
        Assert.assertNotNull(task);
        Assert.assertEquals("1.0.0", task.getItemValueString("$ModelVersion"));
        Assert.assertEquals(200, task.getItemValueInteger("boundaryEvent.targetEvent"));
        Assert.assertEquals(0, task.getItemValueInteger("boundaryEvent.timerEventDefinition.timeDuration"));

    }

}
