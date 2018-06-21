package org.imixs.workflow.engine.adminp;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.lucene.LuceneUpdateService;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.PluginException;

/**
 * JobHandler to upgrates existing workitems to the lates workflow version.
 * 
 * Missing workflow items will be added.
 * 
 * 
 * @author rsoika
 *
 */
@DeclareRoles({ "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
@LocalBean
public class JobHandlerUpgradeWorkitems implements JobHandler {

	private static final int DEFAULT_BLOCK_SIZE = 100;

	@PersistenceContext(unitName = "org.imixs.workflow.jpa")
	private EntityManager manager;

	@Resource
	SessionContext ctx;

	@EJB
	DocumentService documentService;

	@EJB
	LuceneUpdateService luceneService;

	private static Logger logger = Logger.getLogger(JobHandlerRebuildIndex.class.getName());

	/**
	 * This method runs the RebuildLuceneIndexJob. The adminp job description
	 * contains the start position (numIndex) and the number of documents to read
	 * (numBlockSize).
	 * 
	 * The method updates the index for all affected documents which can be filtered
	 * by 'type' and '$created'.
	 * 
	 * An existing lucene index must be deleted manually by the administrator.
	 * 
	 * After the run method is finished, the properties numIndex, numUpdates and
	 * numProcessed are updated.
	 * 
	 * If the number of documents returned from the DocumentService is less the the
	 * BlockSize, the method returns true to indicate that the Timer should be
	 * canceled.
	 * 
	 * @param adminp
	 * @return true if no more unprocessed documents exist.
	 * @throws AccessDeniedException
	 * @throws PluginException
	 */
	@Override
	public ItemCollection run(ItemCollection adminp) throws AdminPException {

		long lProfiler = System.currentTimeMillis();
		int iIndex = adminp.getItemValueInteger("numIndex");
		int iBlockSize = adminp.getItemValueInteger("numBlockSize");

		// test if numBlockSize is defined.
		if (iBlockSize <= 0) {
			// no set default block size.
			iBlockSize = DEFAULT_BLOCK_SIZE;
			adminp.replaceItemValue("numBlockSize", iBlockSize);
		}

		int iUpdates = adminp.getItemValueInteger("numUpdates");
		int iProcessed = adminp.getItemValueInteger("numProcessed");

		String query = buildQuery(adminp);
		logger.finest("......JQPL query: " + query);
		adminp.replaceItemValue("txtQuery", query);

		List<ItemCollection> workitemList = documentService.getDocumentsByQuery(query, iIndex, iBlockSize);
		int colSize = workitemList.size();
		// Update index
		logger.info("Job " + AdminPService.JOB_UPGRADE + " (" + adminp.getUniqueID() + ") - verifeing " + colSize
				+ " workitems...");
		int iCount = 0;
		for (ItemCollection workitem : workitemList) {
			// only look into documents with a model version...
			if (workitem.hasItem(WorkflowKernel.MODELVERSION)) {
				if (upgradeWorkitem(workitem)) {
					// update workitem...
					documentService.save(workitem);
					iCount++;
				}
			}
		}
		iIndex = iIndex + colSize;
		iUpdates = iUpdates + iCount;
		iProcessed = iProcessed + colSize;

		// adjust start pos and update count
		adminp.replaceItemValue("numUpdates", iUpdates);
		adminp.replaceItemValue("numProcessed", iProcessed);
		adminp.replaceItemValue("numIndex", iIndex);

		long time = (System.currentTimeMillis() - lProfiler) / 1000;
		if (time == 0) {
			time = 1;
		}

		logger.info("Job " + AdminPService.JOB_UPGRADE + " (" + adminp.getUniqueID() + ") - " + colSize
				+ " documents processed, " + iCount + " updates in " + time + " sec.  (in total: " + iProcessed
				+ " processed, " + iUpdates + " updates)");

		// if colSize<numBlockSize we can stop the timer
		if (colSize < iBlockSize) {
			// iscompleted = true
			adminp.replaceItemValue(JobHandler.ISCOMPLETED, true);
		}

		return adminp;
	}

	/**
	 * This method upgrades missing fields in a workitem
	 * 
	 * @param worktem
	 * @return
	 */
	public static boolean upgradeWorkitem(ItemCollection workitem) {
		boolean bUpgrade = false;

		if (workitem.getItemValueBoolean("$immutable")) {
			return false;
		}

		if (!workitem.hasItem("$workflowGroup")) {
			workitem.replaceItemValue("$workflowGroup", workitem.getItemValue("txtworkflowgroup"));
			bUpgrade = true;
		}

		if (!workitem.hasItem("$workflowStatus")) {
			workitem.replaceItemValue("$workflowStatus", workitem.getItemValue("txtworkflowstatus"));
			bUpgrade = true;
		}

		if (!workitem.hasItem("$lastEvent")) {
			workitem.replaceItemValue("$lastEvent", workitem.getItemValue("numlastactivityid"));
			bUpgrade = true;
		}

		if (!workitem.hasItem("$lastEventDate")) {
			workitem.replaceItemValue("$lastEventDate", workitem.getItemValue("timworkflowlastaccess"));
			bUpgrade = true;
		}

		if (!workitem.hasItem("$lasteditor")) {
			workitem.replaceItemValue("$lasteditor", workitem.getItemValue("namcurrenteditor"));
			bUpgrade = true;
		}
		
		if (!workitem.hasItem("$taskid")) {
			workitem.replaceItemValue("$taskid", workitem.getItemValue("$processid"));
			bUpgrade = true;
		}

		return bUpgrade;
	}

	/**
	 * This method builds the query statemetn based on the filter criteria.
	 * 
	 * @param adminp
	 * @return
	 */
	private String buildQuery(ItemCollection adminp) {
		Date datFilterFrom = adminp.getItemValueDate("datfrom");
		Date datFilterTo = adminp.getItemValueDate("datto");
		String typeFilter = adminp.getItemValueString("typelist");
		SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd");

		boolean bAddAnd = false;
		String query = "SELECT document FROM Document AS document ";

		if (datFilterFrom != null || datFilterTo != null || (typeFilter != null && !typeFilter.isEmpty())) {
			query += " WHERE ";

		}

		if (typeFilter != null && !typeFilter.isEmpty()) {
			// convert type list into comma separated list
			List<String> typeList = Arrays.asList(typeFilter.split("\\s*,\\s*"));
			String sType = "";
			for (String aValue : typeList) {
				sType += "'" + aValue.trim() + "',";
			}
			sType = sType.substring(0, sType.length() - 1);
			query += " document.type IN(" + sType + ")";
			bAddAnd = true;
		}

		if (datFilterFrom != null) {
			if (bAddAnd) {
				query += " AND ";
			}
			query += " document.created>='" + isoFormat.format(datFilterFrom) + "' ";
			bAddAnd = true;
		}

		if (datFilterTo != null) {
			if (bAddAnd) {
				query += " AND ";
			}
			query += " document.created<='" + isoFormat.format(datFilterTo) + "' ";
			bAddAnd = true;
		}

		query += " ORDER BY document.created";

		return query;
	}
}
