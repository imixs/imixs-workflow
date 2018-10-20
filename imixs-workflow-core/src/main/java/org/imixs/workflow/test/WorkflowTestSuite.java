package org.imixs.workflow.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.services.rest.BasicAuthenticator;
import org.imixs.workflow.services.rest.RequestFilter;
import org.imixs.workflow.services.rest.RestClient;
import org.imixs.workflow.xml.XMLDataCollection;
import org.imixs.workflow.xml.XMLDataCollectionAdapter;
import org.imixs.workflow.xml.XMLDocumentAdapter;

/**
 * The Imixs WorkflowTestSuite provides a test framework for testing business
 * logic. The test suite is based on the Imixs REST API and provides a set of
 * REST clients to simulate and test different szenarios of a business process
 * running in Imixs Workflow.
 * 
 * 
 * The Imixs Test Suite can be used in a JUnit test class.
 * 
 * @author rsoika
 * @version 1.0.0
 */
public class WorkflowTestSuite {
	private static volatile WorkflowTestSuite instance = null;
	private final static Logger logger = Logger.getLogger(WorkflowTestSuite.class.getName());

	Map<String, RestClient> clients = null;
	String host = null;

	// private constructor
	private WorkflowTestSuite() {
		clients = new HashMap<String, RestClient>();
	}

	/**
	 * Singleton Lazy initialization
	 * 
	 * @return
	 */
	public static WorkflowTestSuite getInstance() {
		if (instance == null) {
			synchronized (WorkflowTestSuite.class) {
				// Double check
				if (instance == null) {
					instance = new WorkflowTestSuite();
				}
			}
		}
		return instance;
	}

	/**
	 * Server BASE URL
	 */
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public static Logger getLogger() {
		return logger;
	}

	/**
	 * Public Logger method for test clients.
	 * 
	 * @param level
	 * @param message
	 */
	public static void log(Level level, String message) {
		logger.log(level, message);
	}

	/**
	 * This method can be used to register a new test client. A test client is
	 * identified by its userid. The test client can be used to perform different
	 * methods.
	 * 
	 * @param user     - user id
	 * @param password - optional password
	 */
	public void joinParty(String user, String password) {
		RestClient client = new RestClient();
		if (password != null && !password.isEmpty()) {
			RequestFilter filter = new BasicAuthenticator(user, password);
			client.registerRequestFilter(filter);

		}
		clients.put(user, client);
	}

	/**
	 * Returns a RestClient instance for a specific user. The user must be
	 * registered with the method joinParty.
	 * 
	 * @param user
	 * @return
	 */
	public RestClient getClient(String user) {
		return clients.get(user);
	}

	/**
	 * Get the worklist for a specific user with an optional param
	 * 
	 * @param user
	 * @param resource - resource string
	 * @param param    - optional query param
	 * @return
	 * @see WorkflowSerice
	 */
	public List<ItemCollection> getWorklist(String user, String resourceType, String param) {
		List<ItemCollection> resultList = null;

		RestClient client = clients.get(user);

		try {

			String url = getHost() + "workflow/" + resourceType;
			if (param != null && !param.isEmpty()) {
				url += "/" + param;
			}
			url += ".xml";

			XMLDataCollection result = client.getXMLDataCollection(url);

			if (result != null) {
				resultList = XMLDataCollectionAdapter.putDataCollection(result);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultList;
	}

	/**
	 * Get the default worklist for a specific user
	 * 
	 * @param user
	 * @return
	 */
	public List<ItemCollection> getWorklist(String user) {
		return getWorklist(user, "worklist", null);
	}

	/**
	 * post the workitem for a specific user
	 * 
	 * @param user
	 * @return
	 */
	public ItemCollection processWorkitem(ItemCollection workitem, String user) {

		ItemCollection resultWorkitem = null;
		RestClient client = clients.get(user);

		try {
			resultWorkitem = client.postXMLDocument(getHost() + "workflow/workitem",
					XMLDocumentAdapter.getDocument(workitem));
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultWorkitem;
	}

}