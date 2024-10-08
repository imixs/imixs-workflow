package org.imixs.workflow.bpmn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.text.ParseException;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ModelManager;
import org.imixs.workflow.exceptions.ModelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
	ModelManager openBPMNModelManager = null;

	@BeforeEach
	public void setup() throws ParseException, ParserConfigurationException, SAXException, IOException {
		openBPMNModelManager = new ModelManager();
		try {
			openBPMNModelManager.addModel(BPMNModelFactory.read("/bpmn/minutes.bpmn"));
		} catch (ModelException | BPMNModelException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testSimple() throws ModelException {

		BPMNModel model = openBPMNModelManager.getModel("1.0.0");
		assertNotNull(model);

		Set<String> groups = openBPMNModelManager.findAllGroupsByModel(model);
		// Test Groups
		assertFalse(groups.contains("Collaboration"));
		assertTrue(groups.contains("Protokoll"));
		assertTrue(groups.contains("Protokollpunkt"));

		// test count of elements
		assertEquals(8, model.findAllActivities().size());

	}

}
