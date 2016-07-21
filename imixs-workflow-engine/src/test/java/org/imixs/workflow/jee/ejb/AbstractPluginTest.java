package org.imixs.workflow.jee.ejb;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Model;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.bpmn.BPMNModel;
import org.imixs.workflow.bpmn.BPMNParser;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.plugins.TestApplicationPlugin;
import org.junit.Before;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xml.sax.SAXException;

import junit.framework.Assert;

/**
 * Abstract base class for jUnit tests using the WorkflowService.
 * 
 * This test class mocks the classes WorkflowContext, WorkflowService,
 * EntityService and ModelService. The test class generates a test database with
 * process entities and activity entities which can be accessed from a plug-in
 * or the workflowKernel.
 * 
 * A JUnit Test can save, load and process workitems.
 * 
 * JUnit tests can also manipulate the model by changing entities through
 * calling the methods:
 * 
 * getActivityEntity,setActivityEntity,getProcessEntity,setProcessEntity
 * 
 * 
 * @version 2.0
 * @see TestApplicationPlugin
 * @author rsoika
 */
public class AbstractPluginTest extends AbstractWorkflowEnvironment {
	
	@Before
	public void setup() throws PluginException {
		super.setup();
	}
}
