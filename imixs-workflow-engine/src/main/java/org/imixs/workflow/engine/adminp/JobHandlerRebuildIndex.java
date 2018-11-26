package org.imixs.workflow.engine.adminp;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.jpa.Document;
import org.imixs.workflow.engine.lucene.LuceneUpdateService;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.PluginException;

/**
 * JobHandler to rebuild the lucene fulltext index.
 * 
 * The job starts at 1970/01/01 and reads documents in sequence.
 * 
 * @author rsoika
 *
 */
@DeclareRoles({ "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
@LocalBean
public class JobHandlerRebuildIndex implements JobHandler {

	private static final int BLOCK_SIZE = 500;
	private static final int READ_AHEAD = 32;
	public final static String ITEM_SYNCPOINT = "syncpoint";
	public final static String ITEM_SYNCDATE = "syncdate";
	public static final String SNAPSHOT_TYPE_PRAFIX = "snapshot-";

	@PersistenceContext(unitName = "org.imixs.workflow.jpa")
	private EntityManager manager;

	@EJB
	LuceneUpdateService luceneUpdateService;

	private static Logger logger = Logger.getLogger(JobHandlerRebuildIndex.class.getName());

	/**
	 * This method runs the RebuildLuceneIndexJob. The job starts at creation date
	 * 1970/01/01 and reads singel documents in sequence.
	 * <p>
	 * After the run method is finished, the properties numIndex, numUpdates and
	 * numProcessed are updated.
	 * <p>
	 * 
	 * 
	 * @param adminp
	 * @return true when finished
	 * @throws AccessDeniedException
	 * @throws PluginException
	 */
	@Override
	public ItemCollection run(ItemCollection adminp) throws AdminPException {
		long lProfiler = System.currentTimeMillis();
		long syncPoint = 0;
		int totalCount = 0;
		int blockCount = 0;

		try {
			while (true) {
				List<ItemCollection> resultList = new ArrayList<ItemCollection>();
				List<Document> documents = findNextDocumentsBySyncPoint(syncPoint);

				if (documents != null && documents.size() > 0) {
					for (Document doc : documents) {
						// update syncpoint
						syncPoint = doc.getCreated().getTimeInMillis();						
						try {
							resultList.add(new ItemCollection(doc.getData()));
						} catch (InvalidAccessException e) {
							logger.warning("...unable to index document '"+doc.getId() + "' "+e.getMessage());
						}
						// detach object!
						manager.detach(doc);
					}

					// update the index
					luceneUpdateService.updateDocumentsUncommitted(resultList);

					// update count
					totalCount += resultList.size();
					blockCount += resultList.size();
					if (blockCount >= BLOCK_SIZE) {
						long time = (System.currentTimeMillis() - lProfiler) / 1000;
						if (time == 0) {
							time = 1;
						}
						logger.info("...Job " + AdminPService.JOB_REBUILD_LUCENE_INDEX + " (" + adminp.getUniqueID()
								+ ") - ..." + totalCount + " documents indexed in " + time + " sec. ... ");
						blockCount = 0;
					}
				} else {
					// no more documents
					break;
				}
			}
		} catch (Exception e) {
			// print exception and stop job
			logger.severe("...Job " + AdminPService.JOB_REBUILD_LUCENE_INDEX + " (" + adminp.getUniqueID()
					+ ") - failed - " + e.getMessage() + " last syncpoint  " + syncPoint + " - " + totalCount
					+ "  documents reindexed....");
			e.printStackTrace();
			adminp.replaceItemValue(JobHandler.ISCOMPLETED, false);
			// update syncpoint
			Date syncDate = new Date(syncPoint);
			adminp.replaceItemValue("error", e.getMessage());
			adminp.replaceItemValue(ITEM_SYNCPOINT, syncPoint);
			adminp.replaceItemValue(ITEM_SYNCDATE, syncDate);
			adminp.replaceItemValue("numUpdates", totalCount);
			adminp.replaceItemValue("numProcessed", totalCount);
			adminp.replaceItemValue("numLastCount", 0);
			return adminp;
		}

		// completed
		long time = (System.currentTimeMillis() - lProfiler) / 1000;
		if (time == 0) {
			time = 1;
		}
		logger.info("...Job " + AdminPService.JOB_REBUILD_LUCENE_INDEX + " (" + adminp.getUniqueID() + ") - Finished: "
				+ totalCount + " documents indexed in " + time + " sec. ");

		adminp.replaceItemValue(JobHandler.ISCOMPLETED, true);
		adminp.replaceItemValue("numUpdates", totalCount);
		adminp.replaceItemValue("numProcessed", totalCount);
		adminp.replaceItemValue("numLastCount", 0);
		return adminp;

	}

	/**
	 * Loads the next documents by a given symcpoint (timestamp in milis) compared
	 * with the created timestamp of a document entity.
	 * <p>
	 * It is possible that more than one document entities have the same created
	 * timestamp. For that reason the method returns all documents with the same
	 * timestamp in a collection.
	 * 
	 * @param lSyncpoint
	 * @return a list of documents with the same creation timestamp after the given
	 *         syncpoint. Returns null in case no more documents were found.
	 */
	@SuppressWarnings("unchecked")
	private List<Document> findNextDocumentsBySyncPoint(long lSyncpoint) {

		Date syncpoint = new Date(lSyncpoint);
		// ISO date time format: '2016-08-25 01:23:46.0',
		DateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		String query = "SELECT document FROM Document AS document ";
		query += " WHERE document.created > '" + isoFormat.format(syncpoint) + "'";
		query += " AND NOT document.type LIKE '" + SNAPSHOT_TYPE_PRAFIX + "%' ";
		query += " ORDER BY document.created ASC";
		Query q = manager.createQuery(query);
		q.setFirstResult(0);
		q.setMaxResults(READ_AHEAD);
		List<Document> documentList = q.getResultList();
		if (documentList != null && documentList.size() > 0) {
			Document lastDocument = null;
			Document nextToLastDocument = null;

			// test if we have two documents with the same creation date (in seldom cases
			// possible)
			if (documentList.size() == READ_AHEAD) {
				lastDocument = documentList.get(READ_AHEAD - 1);
				nextToLastDocument = documentList.get(READ_AHEAD - 2);
				// now test if we have more than one document with the same timestamp at the end
				// of the list
				if (lastDocument != null && nextToLastDocument != null
						&& lastDocument.getCreated().equals(nextToLastDocument.getCreated())) {
					logger.finest("......there are more than one document with the same creation timestamp!");
					// lets build a new collection with the duplicated creation timestamp
					syncpoint = new Date(lastDocument.getCreated().getTimeInMillis());
					query = "SELECT document FROM Document AS document ";
					query += " WHERE document.created = '" + isoFormat.format(syncpoint) + "'";
					query += " AND NOT document.type LIKE '" + SNAPSHOT_TYPE_PRAFIX + "%' ";
					query += " ORDER BY document.created ASC";
					q = manager.createQuery(query);
					q.setFirstResult(0);
					q.setMaxResults(BLOCK_SIZE);
					documentList.addAll(q.getResultList());
					return documentList;

				} else {
					// we found exactly READ_AHEAD documents and the last two ones are not equal
					// so we drop the last one of the result to avoid overlapping duplicates in the
					// next block.
					documentList.remove(lastDocument);
					manager.detach(lastDocument);
					return documentList;
				}
			} else {
				// we are at the end of the list
				return documentList;
			}
		}
		return null;
	}

}
