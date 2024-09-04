package org.imixs.workflow.engine.index;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test normalization of a search prase
 * 
 * @author rsoika
 * 
 */
public class TestNormalizePhrase {

	SchemaService schemaService = null;
	private static final Logger logger = Logger.getLogger(TestNormalizePhrase.class.getName());

	@BeforeEach
	public void setUp() throws PluginException, ModelException {
		schemaService = new SchemaService();
	}

	/**
	 * Test "Lukas Podolski"
	 */
	@Test
	public void test1() throws PluginException {
		String searchTerm = "Lukas Podolski";
		String result = null;

		result = schemaService.normalizeSearchTerm(searchTerm);
		logger.log(Level.INFO, "{0}  -->  {1}", new Object[] { searchTerm, result });

		assertEquals("lukas podolski", result);

	}

	/**
	 * Test "Europe/Berlin"
	 */
	@Test
	public void test2() throws PluginException {
		String searchTerm = "Europe/Berlin";
		String result = null;

		result = schemaService.normalizeSearchTerm(searchTerm);
		logger.log(Level.INFO, "{0}  -->  {1}", new Object[] { searchTerm, result });

		assertEquals("europe berlin", result);

	}

	/**
	 * Test "rs/82550/201618"
	 */
	@Test
	public void test3() throws PluginException {
		String searchTerm = "rs/82550/201618";
		String result = null;

		result = schemaService.normalizeSearchTerm(searchTerm);
		logger.log(Level.INFO, "{0}  -->  {1}", new Object[] { searchTerm, result });

		assertEquals("rs\\/82550\\/201618", result);

	}

	/**
	 * Test "rs-82550/201618"
	 */
	@Test
	public void test4() throws PluginException {
		String searchTerm = "rs-82550/201618";
		String result = null;

		result = schemaService.normalizeSearchTerm(searchTerm);
		logger.log(Level.INFO, "{0}  -->  {1}", new Object[] { searchTerm, result });

		assertEquals("rs\\-82550\\/201618", result);

	}

}
