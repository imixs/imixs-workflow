package org.imixs.workflow.bpmn;

import java.io.IOException;
import java.text.ParseException;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.exceptions.ModelException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;
import org.xml.sax.SAXException;

/**
 * Test class
 * 
 * Special cases with collaboration diagram containing two workflow groups
 * (participants) with different workflow models.
 * 
 * 
 * 
 * @author rsoika
 */
public class TestBPMNParserCollaborationMinutes {

	BPMNModel model = null;
	OpenBPMNModelManager openBPMNModelManager = null;

	@Before
	public void setup() throws ParseException, ParserConfigurationException, SAXException, IOException {
		openBPMNModelManager = new OpenBPMNModelManager();
		try {
			openBPMNModelManager.addModel(BPMNModelFactory.read("/bpmn/minutes.bpmn"));
		} catch (ModelException | BPMNModelException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	@Test
	public void testSimple() throws ModelException {

		BPMNModel model = openBPMNModelManager.getModel("1.0.0");
		Assert.assertNotNull(model);

		Set<String> groups = openBPMNModelManager.findAllGroups(model);
		// Test Groups
		Assert.assertFalse(groups.contains("Collaboration"));
		Assert.assertTrue(groups.contains("Protokoll"));
		Assert.assertTrue(groups.contains("Protokollpunkt"));

		// test count of elements
		Assert.assertEquals(8, model.findAllActivities().size());

	}

}
