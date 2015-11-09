/*******************************************************************************
 *  Imixs Workflow 
 *  Copyright (C) 2001, 2011 Imixs Software Solutions GmbH,  
 *  http://www.imixs.com
 *  
 *  This program is free software; you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License 
 *  as published by the Free Software Foundation; either version 2 
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 *  General Public License for more details.
 *  
 *  You can receive a copy of the GNU General Public
 *  License at http://www.gnu.org/licenses/gpl.html
 *  
 *  Project: 
 *  	http://www.imixs.org
 *  	http://java.net/projects/imixs-workflow
 *  
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika - Software Developer
 *******************************************************************************/

package org.imixs.workflow.plugins;

import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.exceptions.PluginException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * The Imixs Split&Join Plugin provides functionality to create and update
 * sub-process instances from a workflow event in an origin process. It is also
 * possible to update the origin process from the sub-process instance.
 * 
 * @author Ralph Soika
 * @version 1.0
 * @see http://www.imixs.org/doc/engine/plugins/splitandjoinplugin.html
 * 
 */
public class SplitAndJoinPlugin extends AbstractPlugin {

	public static final String INVALID_FORMAT = "INVALID_FORMAT";
	public static final String SUBPROCESS_CREATE = "subprocess_create";
	public static final String SUBPROCESS_UPDATE = "subprocess_update";
	public static final String ORIGIN_UPDATE = "origin_update";

	ItemCollection documentContext;
	String sActivityResult;
	private static Logger logger = Logger.getLogger(SplitAndJoinPlugin.class.getName());

	/**
	 * The method evaluates the workflow activity result
	 */
	public int run(ItemCollection adocumentContext, ItemCollection adocumentActivity) throws PluginException {

		ItemCollection evalItemCollection = ResultPlugin.evaluate(adocumentActivity, adocumentContext);

		if (evalItemCollection.hasItem(SUBPROCESS_CREATE)) {
			logger.info("Evaluate Subprocess Item...");

			String subprocess = evalItemCollection.getItemValueString(SUBPROCESS_CREATE).trim();

			if (subprocess.length() > 0) {
				subprocess = "<?xml version=\"1.0\"?><" + SUBPROCESS_CREATE + ">" + subprocess + "</"
						+ SUBPROCESS_CREATE + ">";
				try {
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder db;

					factory = DocumentBuilderFactory.newInstance();

					StringReader sb1 = new StringReader(subprocess);
					InputSource is1 = new InputSource(sb1);
					db = factory.newDocumentBuilder();
					Document doc = db.parse(is1);

					Node node = doc.importNode(doc.getDocumentElement(), true);

					DocumentFragment docfrag = doc.createDocumentFragment();

					while (node.hasChildNodes()) {
						docfrag.appendChild(node.removeChild(node.getFirstChild()));
					}

					int i = 1;
					i++;
					if (i > 0) {

					}
					// here we add a xml root element just for parsing
					// Document doc = db.parse(subprocess);

					if (doc != null) {

					}

				} catch (ParserConfigurationException e) {
					throw new PluginException(RulePlugin.class.getName(), INVALID_FORMAT,
							SUBPROCESS_CREATE + " failed: " + e.getMessage());

				} catch (SAXException e) {
					throw new PluginException(RulePlugin.class.getName(), INVALID_FORMAT,
							SUBPROCESS_CREATE + " failed: " + e.getMessage());

				} catch (IOException e) {
					throw new PluginException(RulePlugin.class.getName(), INVALID_FORMAT,
							SUBPROCESS_CREATE + " failed: " + e.getMessage());

				}

			}
		}

		return Plugin.PLUGIN_OK;
	}

	@Override
	public void close(int status) throws PluginException {
		// no op

	}

}
