package org.imixs.workflow.engine.adminp;

import java.util.Collection;
import java.util.Date;
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

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.jee.ejb.EntityService;

/**
 * JobHandler is used to migrate a workflow version 3.x to 4.0
 * 
 * A Job Document can provide the following information:
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
public class JobHandlerMigration3X implements JobHandler {

	private static final int DEFAULT_BLOCK_SIZE = 100;

	@Resource
	SessionContext ctx;

	@EJB
	DocumentService documentService;

	@EJB
	EntityService entityService;

	private static Logger logger = Logger.getLogger(JobHandlerMigration3X.class.getName());

	/**
	 * This method runs the Migration from Imixs-Worklfow version 3.x to 4.0
	 * 
	 * After the run method is finished, the properties numIndex, numUpdates and
	 * numProcessed are updated.
	 * 
	 * This Job use the old EntityService
	 * 
	 * If the number of documents returned from the EnityService is less the the
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
		adminp.replaceItemValue("txtworkflowStatus", "Processing");
		// save it...
		// adminp = entityService.save(adminp);
		adminp = ctx.getBusinessObject(JobHandlerMigration3X.class).saveJobEntity(adminp);

		String query = "SELECT entity FROM Entity AS entity  ORDER BY entity.created";

		logger.info(
				"Job " + adminp.getUniqueID() + " - index=" + iIndex + " blocksize=" + iBlockSize + " JQPL=" + query);
		adminp.replaceItemValue("txtQuery", query);

		Collection<ItemCollection> col = null;
		try {
			col = entityService._findAllEntities(query, iIndex, iBlockSize);

		} catch (Exception eerror) {
			// prepare for rerun
			logger.severe("Job " + AdminPService.JOB_MIGRATION + " (" + adminp.getUniqueID() + ") - error at: index="
					+ iIndex + " blocksize=" + iBlockSize + " : " + eerror.getMessage());
			adminp.replaceItemValue("txtworkflowStatus", "Error (" + iIndex + "-" + (iIndex + iBlockSize) + ")");
			adminp = ctx.getBusinessObject(JobHandlerMigration3X.class).saveJobEntity(adminp);
			return true;
		}
		int colSize = col.size();
		// Update index
		logger.info("Job " + AdminPService.JOB_MIGRATION + " (" + adminp.getUniqueID() + ") - verifying " + col.size()
				+ " Entity objects for migration. (" + iUpdates + " Entity objects already migrated) ...");

		for (ItemCollection oldEntiy : col) {
			// test if we already have migrated this entity
			String uid = oldEntiy.getUniqueID();
			ItemCollection migratedEntity = documentService.load(uid);
			if (migratedEntity == null) {
				// create log entry....
				oldEntiy.appendItemValue("txtAdminpLog",
						new Date(System.currentTimeMillis()) + " Migrated from Imixs-Workflow 3.X");
				// migrate deprecated fields
				oldEntiy.replaceItemValue("$workflowGroup", oldEntiy.getItemValue("txtworkflowGroup"));
				oldEntiy.replaceItemValue("$workflowStatus", oldEntiy.getItemValue("txtworkflowStatus"));
				// save as new Document
				documentService.save(oldEntiy);
				logger.info("  -> Entity '" + uid + "' migrated.");
				iUpdates++;
			}
		}

		iIndex = iIndex + col.size();

		iProcessed = iProcessed + col.size();

		// adjust start pos and update count
		adminp.replaceItemValue("numUpdates", iUpdates);
		adminp.replaceItemValue("numIndex", iIndex);
		adminp.replaceItemValue("numProcessed", iProcessed);

		long time = (System.currentTimeMillis() - lProfiler) / 1000;

		logger.info("Job " + AdminPService.JOB_MIGRATION + " (" + adminp.getUniqueID() + ") - finished, " + col.size()
				+ " Entity objects verified in " + time + " sec. (" + iUpdates + " Entity objects total migrated)");

		// if colSize<numBlockSize we can stop the timer
		if (colSize < iBlockSize) {
			// prepare for rerun
			adminp.replaceItemValue("txtworkflowStatus", "Finished");
			adminp.replaceItemValue("$workflowStatus", "Finished");
			adminp = ctx.getBusinessObject(JobHandlerMigration3X.class).saveJobEntity(adminp);
			return true;

		} else {
			// prepare for rerun
			adminp.replaceItemValue("txtworkflowStatus", "Waiting");
			adminp.replaceItemValue("$workflowStatus", "Waiting");
			adminp = ctx.getBusinessObject(JobHandlerMigration3X.class).saveJobEntity(adminp);
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

}
