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

package org.imixs.workflow.plugins.jee.extended;

import java.util.Calendar;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.plugins.AbstractPlugin;

/**
 * This Plugin add workitems to a lucene search index. The plug-in depends on
 * the LucenUpdateService EJB which need to be bound as a reference to the
 * WorkflowService EJB.
 * 
 * @see LuceneUpdateService
 * @author rsoika
 * @version 4.5.1 (Lucene)
 * 
 */
public class LucenePlugin extends AbstractPlugin {
	public static String LUCENE_UPDATE_SERVICE_NOT_FOUND = "LUCENE_UPDATE_SERVICE_NOT_FOUND";

	private static Logger logger = Logger.getLogger(LucenePlugin.class.getName());
	private LuceneUpdateService luceneUpdateService = null;

	@Override
	public void init(WorkflowContext actx) throws PluginException {
		super.init(actx);

		// lookup profile service EJB
		String jndiName = "ejb/LuceneUpdateService";
		InitialContext ictx;
		try {
			ictx = new InitialContext();

			Context ctx = (Context) ictx.lookup("java:comp/env");
			luceneUpdateService = (LuceneUpdateService) ctx.lookup(jndiName);
		} catch (NamingException e) {
			throw new PluginException(LucenePlugin.class.getSimpleName(), LUCENE_UPDATE_SERVICE_NOT_FOUND,
					"[LucenePlugin] unable to lookup LuceneService: ", e);
		}

	}

	/**
	 * This method adds the current workitem to the search index by calling the
	 * method addWorkitem. The method computes temporarily the field $processid
	 * based on the numnextprocessid from thE activty entity. This will ensure
	 * that the workitem is indexed correctly on the $processid the workitem
	 * will hold after the process step is completed.
	 * 
	 * Also the $modified and Created date will be set temporarily for the case
	 * that we process a new WorkItem which was not yet saved by the
	 * entityService.
	 * 
	 * If and how the workitem will be added to the search index is fully
	 * controlled by the method addWorkitem.
	 */
	public int run(ItemCollection documentContext, ItemCollection activity) throws PluginException {

		logger.fine("LucenePlugin: updating '" + documentContext.getUniqueID() + "'");
		// logger.info("Lucene
		// ImplementationVersion="+LucenePackage.get().getImplementationVersion());
		// compute next $processid to be added correctly into the search index
		int nextProcessID = activity.getItemValueInteger("numnextprocessid");
		int currentProcessID = documentContext.getItemValueInteger("$processid");

		// temporarily set a $Created and $Modified Date (used to search for
		// $modified)
		if (!documentContext.hasItem("$Created")) {
			documentContext.replaceItemValue("$Created", Calendar.getInstance().getTime());
		}
		documentContext.replaceItemValue("$modified", Calendar.getInstance().getTime());

		// temporarily replace the $processid
		documentContext.replaceItemValue("$processid", nextProcessID);
		// update the search index for the current Worktitem
		luceneUpdateService.updateWorkitem(documentContext);
		// restore $processid
		documentContext.replaceItemValue("$processid", currentProcessID);

		return Plugin.PLUGIN_OK;
	}

	public void close(int status) throws PluginException {

	}

}
