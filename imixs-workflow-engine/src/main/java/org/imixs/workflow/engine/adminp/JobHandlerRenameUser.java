package org.imixs.workflow.engine.adminp;

import java.util.Collection;
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

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.lucene.LuceneUpdateService;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.QueryException;

@DeclareRoles({ "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
@LocalBean
public class JobHandlerRenameUser implements JobHandler {

	@Resource
	SessionContext ctx;

	@EJB
	DocumentService documentService;

	
	@EJB
	LuceneUpdateService luceneService;

	private static Logger logger = Logger.getLogger(JobHandlerRenameUser.class.getName());

	/**
	 * This method creates a new AdminP Job. Depending on the data provided in
	 * the job description this will result in different kind of timer tasks.
	 * 
	 * Lucene Search Statement to quey workitems:
	 * 
	 * <code>
		  "(type:\"workitem\" OR type:\"workitemarchive\" OR type:\"childworkitem\" OR type:\"childworkitemarchive\" OR type:\"workitemlob\" )";
		sQuery +=" AND ($writeaccess:\""+fromName+"\" OR $readaccess:\""+fromName+"\" OR namowner:\""+fromName+"\" OR namcreator:\""+fromName+"\" )";
	
	 * </code>
	 * @throws QueryException 
	 * 
	 * 
	 * @throws AccessDeniedException
	 */
	@Override
	public boolean run(ItemCollection adminp) throws AdminPException {

		
		
		long lProfiler = System.currentTimeMillis();
		int iStart = adminp.getItemValueInteger("numStart");
		int iCount = adminp.getItemValueInteger("numMaxCount");
		int iUpdates = adminp.getItemValueInteger("numUpdates");
		int iProcessed = adminp.getItemValueInteger("numProcessed");
		String fromName = adminp.getItemValueString("namFrom");
		String to = adminp.getItemValueString("namTo");
		boolean replace = adminp.getItemValueBoolean("keyReplace");

		
		// update txtWorkflowSummary
		String summary = "Rename: " + fromName + ">>" + to + " (";
		if (replace)
			summary += "replace";
		else
			summary += "no replace";

		summary += ")";
		
		logger.info(summary);
		
		adminp.replaceItemValue("txtWorkflowSummary", summary);
		
		adminp.replaceItemValue("txtworkflowStatus", "Processing");
		// save it...
		// adminp = entityService.save(adminp);
		adminp = ctx.getBusinessObject(JobHandlerRenameUser.class).saveJobEntity(adminp);

		
		// Select Statement - ASC sorting is important here!
		String sQuery = "(type:\"workitem\" OR type:\"workitemarchive\" OR type:\"childworkitem\" OR type:\"childworkitemarchive\" OR type:\"workitemlob\" )";
		sQuery +=" AND ($writeaccess:\""+fromName+"\" OR $readaccess:\""+fromName+"\" OR namowner:\""+fromName+"\" OR namcreator:\""+fromName+"\" )";
	
		
		Collection<ItemCollection> col;
		try {
			col = documentService.find(sQuery, iStart, iCount);
		} catch (QueryException e) {
			throw new InvalidAccessException(InvalidAccessException.INVALID_ID,e.getMessage(),e);			
		}

		// check all selected documents
		for (ItemCollection entity : col) {
			iProcessed++;

			// call from new instance because of transaction new...
			// see: http://blog.imixs.org/?p=155
			// see: https://www.java.net/node/705304
			boolean result = ctx.getBusinessObject(JobHandlerRenameUser.class).updateWorkitemUserIds(entity, fromName, to, replace,
					adminp.getItemValueString(WorkflowKernel.UNIQUEID));
			if (result == true) {
				// inc counter
				iUpdates++;
			}
		}

		// adjust start pos and update count
		adminp.replaceItemValue("numUpdates", iUpdates);
		adminp.replaceItemValue("numProcessed", iProcessed);

		adminp.replaceItemValue("numLastCount", col.size());
		iStart = iStart + col.size();
		adminp.replaceItemValue("numStart", iStart);

		String timerid = adminp.getItemValueString(WorkflowKernel.UNIQUEID);

		long time = (System.currentTimeMillis() - lProfiler) / 1000;

		logger.info("run:" + timerid + " " + col.size() + " workitems processed in " + time + " sec.");

		
		
		
		// if numLastCount<numMacCount then we can stop the timer
		int iMax = adminp.getItemValueInteger("numMaxCount");
		int iLast = adminp.getItemValueInteger("numLastCount");
		if (iLast < iMax) {
			// prepare for rerun
			adminp.replaceItemValue("txtworkflowStatus", "Finished");
			return true;

		} else {
			// prepare for rerun
			adminp.replaceItemValue("txtworkflowStatus", "Waiting");
			return false;
		}
	}
	
	
	/**
	 * Updates read,write and owner of a entity and returns true if an update
	 * was necessary
	 * 
	 * @param entity
	 * @param from
	 * @param to
	 * @param replace
	 * @return true if the entiy was modified.
	 * @throws AccessDeniedException
	 */
	@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
	public boolean updateWorkitemUserIds(ItemCollection entity, String from, String to, boolean replace,
			String adminpUniqueid) throws AccessDeniedException {

		boolean bUpdate = false;
		if (entity == null)
			return false;

		// log current values
		entity.replaceItemValue("txtAdminP", "AdminP:" + adminpUniqueid + " ");

		// Verify Fields
		logOldValues(entity, "$ReadAccess");
		if (updateList(entity.getItemValue("$ReadAccess"), from, to, replace))
			bUpdate = true;

		logOldValues(entity, "$WriteAccess");
		if (updateList(entity.getItemValue("$WriteAccess"), from, to, replace))
			bUpdate = true;

		logOldValues(entity, "namOwner");
		if (updateList(entity.getItemValue("namOwner"), from, to, replace))
			bUpdate = true;

		logOldValues(entity, "namCreator");
		if (updateList(entity.getItemValue("namCreator"), from, to, replace))
			bUpdate = true;

		if (bUpdate) {
			documentService.save(entity);
			logger.fine("[AmdinP] updated: " + entity.getItemValueString(WorkflowKernel.UNIQUEID));
		}
		return bUpdate;
	}

	@SuppressWarnings("rawtypes")
	private void logOldValues(ItemCollection entity, String field) {

		// update log
		String log = entity.getItemValueString("txtAdminP");
		log = log + " " + field + "=";
		List list = entity.getItemValue(field);
		for (Object o : list) {
			log = log + o.toString() + ",";
		}
		entity.replaceItemValue("txtAdminP", log);

	}
	

	/**
	 * Update the values of a single list.
	 * 
	 * @param list
	 * @param from
	 * @param to
	 * @param replace
	 * @return true if the list was modified.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean updateList(List list, String from, String to, boolean replace) {

		boolean update = false;

		if (list == null || list.isEmpty())
			return false;

		if (list.contains(from)) {

			if (to != null && !"".equals(to) && !list.contains(to)) {
				list.add(to);
				update = true;
			}

			if (replace) {
				while (list.contains(from)) {
					list.remove(from);
					update = true;
				}
			}

		}

		return update;
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
