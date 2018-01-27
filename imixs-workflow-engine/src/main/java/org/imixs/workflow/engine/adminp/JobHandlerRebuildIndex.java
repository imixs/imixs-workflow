package org.imixs.workflow.engine.adminp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.jpa.Document;
import org.imixs.workflow.engine.lucene.LuceneUpdateService;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.PluginException;

/**
 * JobHandler to rebuild the lucene fulltext index.
 * 
 * A Job Document must provide the following information:
 * 
 * numIndex - start position
 * 
 * numBlockSize - documents to read during one run
 * 
 * 
 * 
 * 
 * @author rsoika
 *
 */
@DeclareRoles({ "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
@LocalBean
public class JobHandlerRebuildIndex implements JobHandler {

	private static final int DEFAULT_BLOCK_SIZE = 500;

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
	public boolean run(ItemCollection adminp) throws AdminPException {

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

		adminp.replaceItemValue("$workflowStatus", "Processing");
		// save it...
		// adminp = entityService.save(adminp);
		adminp = ctx.getBusinessObject(JobHandlerRebuildIndex.class).saveJobEntity(adminp);

		String query = buildQuery(adminp);
		logger.fine("JQPL query: " + query);
		adminp.replaceItemValue("txtQuery", query);
		// query documents by direct access to the EntityManager - skip read
		// access verification here because we are still running with
		// ACCESSLEVEL.MANAGERACCESS!
		Query q = manager.createQuery(query);
		q.setFirstResult(iIndex);
		q.setMaxResults(iBlockSize);
		@SuppressWarnings("unchecked")
		Collection<Document> documentList = q.getResultList();
		// collect ItemCollection elements
		List<ItemCollection> col = new ArrayList<ItemCollection>();
		for (Document doc : documentList) {
			col.add(new ItemCollection(doc.getData()));
		}

		int colSize = col.size();
		// Update index
		logger.info("Job " + AdminPService.JOB_REBUILD_LUCENE_INDEX + " (" + adminp.getUniqueID() + ") - reindexing "
				+ col.size() + " documents...");
		luceneService.updateDocuments(col);

		iUpdates = iUpdates + colSize;
		iIndex = iIndex + col.size();
		iProcessed = iProcessed + colSize;

		// adjust start pos and update count
		adminp.replaceItemValue("numUpdates", iUpdates);
		adminp.replaceItemValue("numProcessed", iProcessed);
		adminp.replaceItemValue("numIndex", iIndex);

		long time = (System.currentTimeMillis() - lProfiler) / 1000;
		if (time == 0) {
			time = 1;
		}

		logger.info("Job " + AdminPService.JOB_REBUILD_LUCENE_INDEX + " (" + adminp.getUniqueID() + ") - " + colSize
				+ " documents reindexed in " + time + " sec.  (in total: " + iProcessed);

		// if colSize<numBlockSize we can stop the timer
		if (colSize < iBlockSize) {
			// prepare for rerun
			adminp.replaceItemValue("$workflowStatus", "Finished");
			adminp = ctx.getBusinessObject(JobHandlerRebuildIndex.class).saveJobEntity(adminp);
			return true;

		} else {
			// prepare for rerun
			adminp.replaceItemValue("$workflowStatus", "Waiting");
			adminp = ctx.getBusinessObject(JobHandlerRebuildIndex.class).saveJobEntity(adminp);
			return false;
		}
	}

	/**
	 * Save AdminP Entity
	 */
	@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
	public ItemCollection saveJobEntity(ItemCollection adminp) throws AccessDeniedException {
		logger.fine("saveJobEntity " + adminp.getUniqueID());
		adminp = documentService.save(adminp);
		return adminp;

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
