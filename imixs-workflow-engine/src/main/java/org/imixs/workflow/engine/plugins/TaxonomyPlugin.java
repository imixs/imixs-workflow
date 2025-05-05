/*  
 *  Imixs-Workflow 
 *  
 *  Copyright (C) 2001-2020 Imixs Software Solutions GmbH,  
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
 *      https://www.imixs.org
 *      https://github.com/imixs/imixs-workflow
 *  
 *  Contributors:  
 *      Imixs Software Solutions GmbH - Project Management
 *      Ralph Soika - Software Developer
 */

package org.imixs.workflow.engine.plugins;

import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.util.XMLParser;

/**
 * The Imixs Taxonomy plugin can be used to collect taxonomy data at specific
 * stages of a business process. The plugin can be configured by the activity
 * result :
 * <p>
 * Example:
 * <p>
 * 
 * <pre>
  {@code
   <taxonomy name="approval">
    	<type>start</type>
    	<anonymised>true</anonymised>
    </taxonomy>
    }
 * </pre>
 * <p>
 * 
 * defines a start point named 'approval'
 * <p>
 * The result will be stored into the the following fields:
 * <ul>
 * <li>taxonomy.name : contains a list of all collected taxonomy names (e.g.
 * 'approval')
 * <li>taxonomy.[NAME].start : contains the start time points in a list (latest
 * entry on top!)
 * <li>taxonomy.[NAME].end : contains the end time points (list)
 * <li>taxonomy.[NAME].duration: contains the total time in seconds
 * <li>taxonomy.[NAME].start.by: contains the $owner list at the first start
 * <li>taxonomy.[NAME].end.by: contains the $editor list at the last stop
 * <p>
 * 
 * 
 * @author rsoika
 * 
 */
public class TaxonomyPlugin extends AbstractPlugin {
	public static final String INVALID_FORMAT = "INVALID_FORMAT";

	private static final Logger logger = Logger.getLogger(TaxonomyPlugin.class.getName());

	@Override
	public ItemCollection run(ItemCollection documentContext, ItemCollection event) throws PluginException {
		logger.finest("running TaxonomyPlugin");
		// parse for taxonomy definition....
		ItemCollection taxonomyConfig = this.getWorkflowContextService().evalWorkflowResult(event, "taxonomy",
				documentContext,
				true);
		if (taxonomyConfig == null || taxonomyConfig.getItemNames().size() == 0) {
			// no op - return
			return documentContext;
		}

		// now iterate over all taxonomy definitions....
		for (String name : taxonomyConfig.getItemNames()) {
			logger.log(Level.FINEST, "found taxonomy name={0}", name);
			// for each taxonomy definition evaluate the taxonomy data....
			String xmlDef = taxonomyConfig.getItemValueString(name);
			ItemCollection taxonomyData = XMLParser.parseItemStructure(xmlDef);

			if (taxonomyData != null) {
				String type = taxonomyData.getItemValueString("type");
				boolean anonymised = true;
				if (taxonomyData.hasItem("anonymised")) {
					anonymised = taxonomyData.getItemValueBoolean("anonymised");
				}
				logger.log(Level.FINEST, "... type={0}  anonymised={1}", new Object[] { type, anonymised });
				evalTaxonomyDefinition(documentContext, name, type, anonymised);
			}

		}

		return documentContext;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	void evalTaxonomyDefinition(ItemCollection documentContext, String name, String type, boolean anonymised)
			throws PluginException {

		// test if taxonomy name is valid (^[\w.-]+$)
		if (!name.matches("^[\\w.-]+$")) {
			throw new PluginException(TaxonomyPlugin.class.getSimpleName(), INVALID_FORMAT,
					"Invalid taxonomy definition - the name cannot contain special characters: '" + name + "'");
		}

		// add new taxonomy name
		documentContext.appendItemValueUnique("taxonomy", name);

		List valuesStart = documentContext.getItemValue("taxonomy." + name + ".start");
		List valuesStop = documentContext.getItemValue("taxonomy." + name + ".stop");

		if ("start".equals(type)) {

			// the length of stop list must be the same as the start list
			if (valuesStart.size() != valuesStop.size()) {
				logger.log(Level.WARNING, "Invalid taxonomy definition ''{0}'' starttime without stoptime!"
						+ " - please check model event {1}.{2} taxonomy will be ignored!",
						new Object[] { name, documentContext.getTaskID(), documentContext.getEventID() });
				return;
			}

			// add new start point
			valuesStart.add(0, new Date());
			documentContext.replaceItemValue("taxonomy." + name + ".start", valuesStart);

			// anonymised ?
			if (!anonymised) {
				List owners = documentContext.getItemValue("$owner");
				documentContext.replaceItemValue("taxonomy." + name + ".start.by", owners);

			}

		}
		if ("stop".equals(type)) {

			if (valuesStop.size() != (valuesStart.size() - 1)) {
				logger.log(Level.WARNING, "Invalid taxonomy definition ''{0}'' stoptime without starttime!"
						+ " - please check model entry {1}.{2} taxonomy will be ignored!",
						new Object[] { name, documentContext.getTaskID(), documentContext.getEventID() });
				return;

			}
			// add new start point
			valuesStop.add(0, new Date());
			documentContext.replaceItemValue("taxonomy." + name + ".stop", valuesStop);

			// now we add the new time range....
			int numTotal = documentContext.getItemValueInteger("taxonomy." + name + ".duration");
			Date start = (Date) valuesStart.get(0);
			Date stop = (Date) valuesStop.get(0);

			long lStart = start.getTime() / 1000;
			long lStop = stop.getTime() / 1000;

			numTotal = (int) (numTotal + (lStop - lStart));
			documentContext.replaceItemValue("taxonomy." + name + ".duration", numTotal);

			// anonymised ?
			if (!anonymised) {
				List owners = documentContext.getItemValue("$editor");
				documentContext.replaceItemValue("taxonomy." + name + ".stop.by", owners);

			}
		}
	}

}
