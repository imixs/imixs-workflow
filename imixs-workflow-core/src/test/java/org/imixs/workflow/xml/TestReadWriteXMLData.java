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

package org.imixs.workflow.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.imixs.workflow.ItemCollection;
import org.junit.jupiter.api.Test;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

/**
 * Test class reads the "/document-example.xml" file via the
 * XMLItemCollectionAdapter and verifies the content. Than the test case writes
 * the data back into a new file "/document-example.xml" on the same file
 * system.
 * 
 * 
 * @author rsoika
 */
public class TestReadWriteXMLData {

	/**
	 * Read /document-example.xml
	 */
	@Test
	public void testRead() {
		List<ItemCollection> col = null;

		try {
			col = XMLDataCollectionAdapter
					.readCollectionFromInputStream(getClass().getResourceAsStream("/document-example.xml"));
		} catch (JAXBException e) {
			fail();
		} catch (IOException e) {
			fail();
		}

		assertEquals(2, col.size());

		// test values
		ItemCollection workitem = col.get(0);
		assertEquals("1.0.1", workitem.getItemValueString("$modelversion"));
		assertEquals(55.123, workitem.getItemValueDouble("amount"), 0);

	}

	/**
	 * reads /document-example.xml input file and writes the jaxb object back into a
	 * new xml file
	 */
	@Test
	// @Ignore
	public void testWrite() {
		List<ItemCollection> col = null;
		// read default content
		try {
			col = XMLDataCollectionAdapter
					.readCollectionFromInputStream(getClass().getResourceAsStream("/document-example.xml"));
		} catch (JAXBException e) {
			fail();
		} catch (IOException e) {
			fail();
		}

		// create JAXB object
		XMLDataCollection xmlCol = null;
		try {
			xmlCol = XMLDataCollectionAdapter.getDataCollection(col);
		} catch (Exception e1) {

			e1.printStackTrace();
			fail();
		}

		// now write back to file
		File file = null;
		try {

			file = new File("src/test/resources/export-test.xml");
			JAXBContext jaxbContext = JAXBContext.newInstance(XMLDataCollection.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

			// output pretty printed
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

			jaxbMarshaller.marshal(xmlCol, file);
			jaxbMarshaller.marshal(xmlCol, System.out);

		} catch (JAXBException e) {
			e.printStackTrace();
			fail();
		}
		assertNotNull(file);
	}

}
