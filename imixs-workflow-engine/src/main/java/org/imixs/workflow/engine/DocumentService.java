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

package org.imixs.workflow.engine;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.jpa.Document;
import org.imixs.workflow.engine.lucene.LuceneSearchService;
import org.imixs.workflow.engine.lucene.LuceneUpdateService;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.QueryException;

/**
 * The DocumentService is used to save and load instances of ItemCollections
 * into a Database. The DocumentService throws an AccessDeniedException if the
 * CallerPrincipal is not allowed to save or read a specific Document from the
 * database. So the DocumentService can be used to save business objects into a
 * database with individual read- or writeAccess restrictions.
 * <p>
 * The Bean holds an instance of an EntityPersistenceManager for the persistence
 * unit 'org.imixs.workflow.jpa' to manage the Document entity bean class. The
 * Document entity bean is used to store the attributes of a ItemCollection into
 * the connected database.
 * <p>
 * The save() method persists any instance of an ItemCollection. If a
 * ItemCollection is saved the first time the DocumentService generates the
 * attribute $uniqueid which will be included in the ItemCollection returned by
 * this method. If a ItemCollection was saved before the method updates the
 * corresponding Document Object.
 * <p>
 * The load() and find() methods are used to read ItemCollections from the
 * database. The remove() method deletes a saved ItemCollection from the
 * database.
 * <p>
 * All methods expect and return Instances of the object
 * org.imixs.workflow.ItemCollection which is no entity EJB. So these objects
 * are not managed by any instance of an EntityPersistenceManager.
 * <p>
 * A collection of ItemCollections can be read using the find() method using EQL
 * syntax.
 * <p>
 * 
 * Additional to the basic functionality to save and load instances of the
 * object org.imixs.workflow.ItemCollection the method also manages the read-
 * and writeAccess for each instance of an ItemCollection. Therefore the save()
 * method scans an ItemCollection for the attributes '$ReadAccess' and
 * '$WriteAccess'. The DocumentService verifies in each call of the save()
 * load(), remove() and find() methods if the current callerPrincipal is granted
 * to the affected entities. If an ItemCollection was saved with read- or
 * writeAccess the access to an Instance of a saved ItemCollection will be
 * protected for a callerPrincipal with missing read- or writeAccess.
 * <p>
 * 
 * @see org.imixs.workflow.engine.jpa.Document
 * @author rsoika
 * @version 1.0
 * 
 */

@DeclareRoles({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RolesAllowed({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
@LocalBean
public class DocumentService {

	public static final String ACCESSLEVEL_NOACCESS = "org.imixs.ACCESSLEVEL.NOACCESS";

	public static final String ACCESSLEVEL_READERACCESS = "org.imixs.ACCESSLEVEL.READERACCESS";

	public static final String ACCESSLEVEL_AUTHORACCESS = "org.imixs.ACCESSLEVEL.AUTHORACCESS";

	public static final String ACCESSLEVEL_EDITORACCESS = "org.imixs.ACCESSLEVEL.EDITORACCESS";

	public static final String ACCESSLEVEL_MANAGERACCESS = "org.imixs.ACCESSLEVEL.MANAGERACCESS";

	public static final String UNIQUEID = "$uniqueid";
	public static final String UNIQUEIDREF = "$uniqueidref";
	public static final String READACCESS = "$readaccess";
	public static final String WRITEACCESS = "$writeaccess";
	public static final String ISAUTHOR = "$isAuthor";

	public static final String USER_GROUP_LIST = "org.imixs.USER.GROUPLIST";

	// private static Logger logger = Logger.getLogger("org.imixs.workflow");
	private final static Logger logger = Logger.getLogger(DocumentService.class.getName());

	public static final String OPERATION_NOTALLOWED = "OPERATION_NOTALLOWED";
	public static final String INVALID_PARAMETER = "INVALID_PARAMETER";
	public static final String INVALID_UNIQUEID = "INVALID_UNIQUEID";

	@Resource
	SessionContext ctx;

	@Resource(name = "ACCESS_ROLES")
	private String accessRoles = "";
	@Resource(name = "DISABLE_OPTIMISTIC_LOCKING")
	private Boolean disableOptimisticLocking = false;

	@PersistenceContext(unitName = "org.imixs.workflow.jpa")
	private EntityManager manager;

	@EJB
	private LuceneUpdateService luceneUpdateService;

	@EJB
	private LuceneSearchService luceneSearchService;

	/**
	 * Returns additional AccessRoles defined for the EJB instance
	 * 
	 * @return
	 */
	public String getAccessRoles() {
		return accessRoles;
	}

	public void setAccessRoles(String accessRoles) {
		this.accessRoles = accessRoles;
	}

	/**
	 * returns the disable optimistic locking status
	 * 
	 * @return - true if optimistic locking is disabled
	 */
	public void setDisableOptimisticLocking(Boolean disableOptimisticLocking) {
		this.disableOptimisticLocking = disableOptimisticLocking;
	}

	public Boolean getDisableOptimisticLocking() {
		return disableOptimisticLocking;
	}

	/**
	 * This method returns a list of user names, roles and application groups
	 * the user belongs to.
	 * 
	 * @return
	 */
	public List<String> getUserNameList() {

		List<String> userNameList = new Vector<String>();

		// Begin with the username
		userNameList.add(ctx.getCallerPrincipal().getName().toString());
		// now construct role list
		String roleList = "org.imixs.ACCESSLEVEL.READERACCESS,org.imixs.ACCESSLEVEL.AUTHORACCESS,org.imixs.ACCESSLEVEL.EDITORACCESS,org.imixs.ACCESSLEVEL.MANAGERACCESS,"
				+ accessRoles;
		// and add each role the user is in to the list
		StringTokenizer roleListTokens = new StringTokenizer(roleList, ",");
		while (roleListTokens.hasMoreTokens()) {
			try {
				String testRole = roleListTokens.nextToken().trim();
				if (!"".equals(testRole) && ctx.isCallerInRole(testRole))
					userNameList.add(testRole);
			} catch (Exception e) {
				// no operation - Role simply not defined
				// this could be an configuration/test issue and need not to be
				// handled as an error
			}
		}

		// read dynamic user roles from the ContextData (if provided) and add
		// them to the query....
		String[] applicationGroups = getUserGroupList();
		if (applicationGroups != null)
			for (String auserRole : applicationGroups)
				userNameList.add(auserRole);

		return userNameList;
	}

	/**
	 * Test if the caller has a given security role.
	 * 
	 * @param rolename
	 * @return true if user is in role
	 */
	public boolean isUserInRole(String rolename) {
		try {
			return ctx.isCallerInRole(rolename);
		} catch (Exception e) {
			// avoid a exception for a role request which is not defined
			return false;
		}
	}

	/**
	 * This Method saves an ItemCollection into the database. If the
	 * ItemCollection is saved the first time the method generates a uniqueID
	 * ('$uniqueid') which can be used to identify the ItemCollection by its ID.
	 * If the ItemCollection was saved before, the method updates the existing
	 * ItemCollection stored in the database.
	 * <p>
	 * The Method returns an updated instance of the ItemCollection containing
	 * the attributes $modified, $created, and $uniqueId
	 * <p>
	 * The method throws an AccessDeniedException if the CallerPrincipal is not
	 * allowed to save or update the ItemCollection in the database. The
	 * CallerPrincipial should have at least the access Role
	 * org.imixs.ACCESSLEVEL.AUTHORACCESS
	 * <p>
	 * The method returns a itemCollection with the current VersionNumber from
	 * the persisted entity. (see issue #145)
	 * <p>
	 * The method adds/updates the document into the lucene index.
	 * 
	 * @param ItemCollection
	 *            to be saved
	 * @return updated ItemCollection
	 * @throws AccessDeniedException
	 */
	public ItemCollection save(ItemCollection document) throws AccessDeniedException {

		logger.info("start: ID=" + document.getUniqueID() + " version=" + document.getItemValueInteger("$version"));

		Document persistedDocument = null;
		// Now set flush Mode to COMMIT
		manager.setFlushMode(FlushModeType.COMMIT);

		// check if a $uniqueid is available
		String sID = document.getItemValueString(UNIQUEID);
		if (!"".equals(sID)) {
			// yes so we can try to find the Entity by its primary key
			persistedDocument = manager.find(Document.class, sID);
			if (persistedDocument == null) {
				logger.fine("Document '" + sID + "' not found!");
			}
		}

		// did the document exist?
		if (persistedDocument == null) {
			// entity not found in database, create a new instance using the
			// provided id. Test if user is allowed to create Entities....
			if (!(ctx.isCallerInRole(ACCESSLEVEL_MANAGERACCESS) || ctx.isCallerInRole(ACCESSLEVEL_EDITORACCESS)
					|| ctx.isCallerInRole(ACCESSLEVEL_AUTHORACCESS))) {
				throw new AccessDeniedException(OPERATION_NOTALLOWED, "You are not allowed to perform this operation");
			}
			// create new one with the provided id
			persistedDocument = new Document(sID);
			// if $Created is provided than overtake this information
			Date datCreated = document.getItemValueDate("$Created");
			if (datCreated != null) {
				Calendar cal = Calendar.getInstance();
				cal.setTime(datCreated);
				// Overwrite Creation Date
				persistedDocument.setCreated(cal);
			}

			// now persist the new Entity
			logger.finest("persist activeEntity");
			manager.persist(persistedDocument);

		} else {
			// activeEntity exists - verify if current user has write- and
			// readaccess
			if (!isCallerAuthor(persistedDocument) || !isCallerReader(persistedDocument)) {
				throw new AccessDeniedException(OPERATION_NOTALLOWED, "You are not allowed to perform this operation");
			}
		}

		// after all the persistedDocument is now managed through the
		// persistence
		// manager!

		// remove the property $isauthor
		document.removeItem("$isauthor");

		String aType = document.getItemValueString("type");
		if ("".equals(aType))
			aType = "Entity";
		persistedDocument.setType(aType);

		// update the standard attributes $modified $created and $uniqueid
		Calendar cal = Calendar.getInstance();
		document.replaceItemValue("$uniqueid", persistedDocument.getId());
		document.replaceItemValue("$modified", cal.getTime());
		document.replaceItemValue("$created", persistedDocument.getCreated().getTime());

		// update current version number
		if (disableOptimisticLocking) {
			// in case of optimistic locking is disabled we remove $version
			document.removeItem("$Version");
		}
		if (!disableOptimisticLocking && document.hasItem("$Version") && document.getItemValueInteger("$Version") > 0) {
			// if $version is provided we update the version number of
			// the document to ensure that optimisticLock exception is
			// handled the right way!
			int version = document.getItemValueInteger("$Version");
			persistedDocument.setVersion(version);
			logger.finest("[save] version=" + version);
		}

		// finally update the data field and store the item map object
		persistedDocument.setData(document.getAllItems());

		// verify and update the author access and add again the property
		// '$isauthor'
		document.replaceItemValue("$isauthor", isCallerAuthor(persistedDocument));

		// we now increase the $Version number
		// ! not necessary - version can be read after flush blow!

		/*
		 * Issue #166,#145
		 * 
		 * The flush call is important here. In cases of multiple updates of
		 * different entities in same transaction data can be lost if not
		 * flushed here! After the flush() the current version number can be
		 * read.
		 */
		manager.flush();

		// get new $version
		document.replaceItemValue("$Version", persistedDocument.getVersion());

		/*
		 * Issue #189
		 * 
		 * We need to detach the activeEntity here. In other cases there are
		 * situations where updates caused by the vm are reflected back into the
		 * entity and increases the version number. This can be tested when a
		 * byte array is stored in a itemCollection. So for this reason we
		 * detach the entity here!!
		 */
		manager.detach(persistedDocument);

		// add/update document into index
		luceneUpdateService.updateDocument(document);

		logger.info("end: ID=" + document.getUniqueID() + " version=" + document.getItemValueInteger("$version"));

		// return itemCollection
		return document;
	}

	/**
	 * This method saves a workitem in a new transaction. The method can be used
	 * by plugins to isolate a save request from the current transaction
	 * context.
	 * 
	 * To call this method a EJB session context is necessary: <code>
	 * 		workitem= sessionContext.getBusinessObject(EntityService.class)
						.saveNewTransaction(workitem);
	 * </code>
	 * 
	 * @param itemcol
	 * @return
	 * @throws AccessDeniedException
	 */
	@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
	public ItemCollection saveByNewTransaction(ItemCollection itemcol) throws AccessDeniedException {
		return save(itemcol);
	}

	/**
	 * This method loads an ItemCollection from the Database. The method expects
	 * a valid $unqiueID to identify the Document entity saved before into the
	 * database. The method returns null if no Document with the corresponding
	 * ID exists.
	 * <p>
	 * The method checks if the CallerPrincipal has read access to Document
	 * stored in the database. If not, the method returns null.
	 * <p>
	 * <strong>Note:</strong> The method dose not throw an AccessDeniedException
	 * if the user is not allowed to read the entity to prevent a aggressor with
	 * informations about the existence of that specific Document.
	 * <p>
	 * The CallerPrincipial need to have at least the access level
	 * org.imixs.ACCESSLEVEL.READACCESS
	 * 
	 * @param id
	 *            - the $unqiueid of the ItemCollection to be loaded
	 * @return ItemCollection object or null if the Document dose not exist or
	 *         the CallerPrincipal hat insufficient read access.
	 * 
	 */
	public ItemCollection load(String id) {
		Document persistedDocument = null;
		persistedDocument = manager.find(Document.class, id);

		// create instance of ItemCollection
		if (persistedDocument != null && isCallerReader(persistedDocument)) {
			ItemCollection result = new ItemCollection(persistedDocument.getData());
			// if disable Optimistic Locking is TRUE we do not add the version
			// number
			if (disableOptimisticLocking)
				result.removeItem("$Version");
			else
				result.replaceItemValue("$Version", persistedDocument.getVersion());
			return result;
		} else
			return null;
	}

	/**
	 * This method removes an ItemCollection from the database. If the
	 * CallerPrincipal is not allowed to access the ItemColleciton the method
	 * throws an AccessDeniedException.
	 * <p>
	 * The CallerPrincipial should have at least the access Role
	 * org.imixs.ACCESSLEVEL.AUTHORACCESS
	 * <p>
	 * Also the method removes the document form the lucene index.
	 * 
	 * 
	 * @param ItemCollection
	 *            to be removed
	 * @throws AccessDeniedException
	 */
	public void remove(ItemCollection itemcol) throws AccessDeniedException {
		Document persistedDocument = null;
		String sID = itemcol.getItemValueString("$uniqueid");
		persistedDocument = manager.find(Document.class, sID);

		if (persistedDocument != null) {
			if (!isCallerReader(persistedDocument) || !isCallerAuthor(persistedDocument))
				throw new AccessDeniedException(OPERATION_NOTALLOWED,
						"remove - You are not allowed to perform this operation");

			// remove document...
			manager.remove(persistedDocument);
			// remove document form index
			luceneUpdateService.removeDocument(itemcol.getUniqueID());
		} else
			throw new AccessDeniedException(INVALID_UNIQUEID, "remove - invalid $uniqueid");
	}

	/**
	 * The method returns only the count of entities for an an valid Lucene
	 * statement. The method counts only ItemCollections which are readable by
	 * the CallerPrincipal. With the startpos and count parameters it is
	 * possible to read chunks of entities. The jPQL Statement must match the
	 * conditions of the JPA Object Class Entity.
	 * 
	 * 
	 * @param query
	 *            - Lucene search query
	 * @param startpos
	 *            - optional start position
	 * @param maxcount
	 *            - maximum count of elements to be returned
	 * @return count of elements to returned by this query
	 * @throws InvalidAccessException
	 * 
	 * @see org.imixs.workflow.engine.jpa.Document.jee.jpa.Entity
	 */
	public int count(String query) {

		long l = 0;

		logger.fine("countAllDocuments - Query=" + query);

		logger.warning("Count not implemented!");
		// TODO - implementation missing

		logger.fine("countAllEntities in " + (System.currentTimeMillis() - l) + " ms");
		return 0;

	}

	/**
	 * The method returns a list of ItemCollections by calling the
	 * LuceneSearchService. The method expects an valid Lucene search term.
	 * <p>
	 * The method returns only ItemCollections which are readable by the
	 * CallerPrincipal. With the pageSize and pageNumber it is possible to
	 * paginate.
	 * 
	 * @param searchTerm
	 *            - Lucene search term
	 * @param pageSize
	 *            - total docs per page
	 * @param pageIndex
	 *            - number of page to start (default = 0)
	 * @return list of ItemCollection elements
	 * @throws QueryException
	 * 
	 * @see org.imixs.workflow.engine.lucene.LuceneSearchService
	 */
	public List<ItemCollection> find(String searchTerm, int pageSize, int pageIndex) throws QueryException {
		return find(searchTerm, pageSize, pageIndex, null, false);
	}

	/**
	 * The method returns a sorted list of ItemCollections by calling the
	 * LuceneSearchService. The result list can be sorted by a sortField and a
	 * sort direction.
	 * <p>
	 * The method expects an valid Lucene search term. The method returns only
	 * ItemCollections which are readable by the CallerPrincipal. With the
	 * pageSize and pageNumber it is possible to paginate.
	 * 
	 * @param searchTerm
	 *            - Lucene search term
	 * @param pageSize
	 *            - total docs per page
	 * @param pageIndex
	 *            - number of page to start (default = 0)
	 * 
	 * @param sortBy
	 *            -optional field to sort the result
	 * @param sortReverse
	 *            - optional sort direction
	 * 
	 * @return list of ItemCollection elements
	 * @throws QueryException
	 * 
	 * @see org.imixs.workflow.engine.lucene.LuceneSearchService
	 */
	public List<ItemCollection> find(String searchTerm, int pageSize, int pageIndex, String sortBy, boolean sortReverse)
			throws QueryException {
		logger.fine("find - SearchTerm=" + searchTerm + "  , pageSize=" + pageSize + " pageNumber=" + pageIndex
				+ " , sortBy=" + sortBy + " reverse=" + sortReverse);

		// create sort object
		Sort sortOrder = null;
		if (sortBy != null && !sortBy.isEmpty()) {
			// we do not support multi values here - see
			// LuceneUpdateService.addItemValues
			// it would be possible if we use a SortedSetSortField class here
			sortOrder = new Sort(new SortField[] { new SortField(sortBy, Type.STRING, sortReverse) });
		}

		return luceneSearchService.search(searchTerm, pageSize, pageIndex, sortOrder, null);

	}

	/**
	 * The method returns a collection of ItemCollections referred by a
	 * $uniqueid.
	 * <p>
	 * The method returns only ItemCollections which are readable by the
	 * CallerPrincipal. With the pageSize and pageNumber it is possible to
	 * paginate.
	 * 
	 * 
	 * @param uniqueIdRef
	 *            - $uniqueId to be referred by the collected documents
	 * @param pageSize
	 *            - total docs per page
	 * @param pageIndex
	 *            - number of page to start (default = 0)
	 * @return resultset
	 * 
	 */
	public List<ItemCollection> findDocumentsByRef(String uniqueIdRef, int start, int count) {
		String searchTerm = "(" + "$uniqueidref:\"" + uniqueIdRef + "\")";
		try {
			return find(searchTerm, start, count);
		} catch (QueryException e) {
			logger.severe("findDocumentsByRef - invalid query: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Returns an unordered list of all documents of a specific type. The method
	 * throws an InvalidAccessException in case no type attribute is defined.
	 * 
	 * @param type
	 * @return
	 * @throws InvalidAccessException
	 */
	public List<ItemCollection> getDocumentsByType(String type) {
		if (type == null || type.isEmpty()) {
			throw new InvalidAccessException(INVALID_PARAMETER, "undefined type attribute");
		}

		String query = "SELECT document FROM Document AS document ";
		query += " WHERE document.type = '" + type + "'";
		query += " ORDER BY document.created DESC";
		return getDocumentsByQuery(query);
	}

	/**
	 * Returns all documents of by JPQL statement
	 * 
	 * @param query
	 *            - JPQL statement
	 * @return
	 * 
	 */
	public List<ItemCollection> getDocumentsByQuery(String query) {
		List<ItemCollection> result = new ArrayList<ItemCollection>();
		Query q = manager.createQuery(query);

		long l = System.currentTimeMillis();
		@SuppressWarnings("unchecked")
		Collection<Document> documentList = q.getResultList();
		logger.fine("getDocumentsByQuery in " + (System.currentTimeMillis() - l) + " ms");

		if (documentList == null) {
			logger.fine("getDocumentsByQuery - no ducuments found.");
			return result;
		}

		l = System.currentTimeMillis();
		// filter resultset by read access
		for (Document doc : documentList) {
			if (isCallerReader(doc)) {
				ItemCollection _tmp = new ItemCollection(doc.getData());
				// if disable Optimistic Locking is TRUE we do not add the version
				// number
				if (disableOptimisticLocking)
					_tmp.removeItem("$Version");
				else
					_tmp.replaceItemValue("$Version", doc.getVersion());
				result.add(_tmp);
			}
		}

		logger.fine("getDocumentsByQuery - ResultList size=" + documentList.size());
		return result;
	}

	/**
	 * This method creates a backup of the result set form a Lucene search
	 * query. The document list will be stored into the file system. The method
	 * stores the Map from the ItemCollection to be independent from version
	 * upgrades. To manage large dataSets the method reads the documents in
	 * smaller blocks
	 * 
	 * @param entities
	 * @throws IOException
	 */
	public void backup(String query, String filePath) throws IOException {
		boolean hasMoreData = true;
		int JUNK_SIZE = 100;
		long totalcount = 0;
		int startpos = 0;
		int icount = 0;

		logger.info("backup - starting...");
		logger.info("backup - query=" + query);
		logger.info("backup - target=" + filePath);

		if (filePath == null || filePath.isEmpty()) {
			logger.severe("[EntityService] Invalid FilePath!");
			return;
		}

		FileOutputStream fos = new FileOutputStream(filePath);
		ObjectOutputStream out = new ObjectOutputStream(fos);
		while (hasMoreData) {
			// read a junk....

			// TODO - implementation missing
			// Collection<ItemCollection> col = findAllEntities(query, startpos,
			// JUNK_SIZE);
			Collection<ItemCollection> col = new Vector<ItemCollection>();

			if (col.size() < JUNK_SIZE)
				hasMoreData = false;
			startpos = startpos + col.size();
			totalcount = totalcount + col.size();

			for (ItemCollection aworkitem : col) {
				// get serialized data
				Map<?, ?> hmap = aworkitem.getAllItems();
				// write object
				out.writeObject(hmap);
				icount++;
			}
			logger.fine("[EntityService] " + totalcount + " entries backuped....");
		}
		out.close();
		logger.info("[EntityService] Backup finished - " + icount + " entities read totaly.");
	}

	/**
	 * This method restores a backup from the file system and imports the
	 * Documents into the database.
	 * 
	 * @param filepath
	 * @throws IOException
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void restore(String filePath) throws IOException {
		int JUNK_SIZE = 100;
		long totalcount = 0;
		long errorCount = 0;
		int icount = 0;

		FileInputStream fis = new FileInputStream(filePath);
		ObjectInputStream in = new ObjectInputStream(fis);

		while (true) {
			try {
				// read one more object
				Map hmap = (Map) in.readObject();
				ItemCollection itemCol = new ItemCollection(hmap);
				// remove the $version property!
				itemCol.removeItem("$Version");
				// now save imported data
				save(itemCol);
				totalcount++;
				icount++;
				if (icount >= JUNK_SIZE) {
					icount = 0;
					logger.info("[EntityService] Restored " + totalcount + " entities....");

				}

			} catch (java.io.EOFException eofe) {
				break;
			} catch (ClassNotFoundException e) {
				errorCount++;
				logger.warning("[EntityService] error importing workitem at position " + (totalcount + errorCount)
						+ " Error: " + e.getMessage());
			} catch (AccessDeniedException e) {
				errorCount++;
				logger.warning("[EntityService] error importing workitem at position " + (totalcount + errorCount)
						+ " Error: " + e.getMessage());
			}
		}
		in.close();

		String loginfo = "Import successfull! " + totalcount + " Entities imported. " + errorCount
				+ " Errors.  Import FileName:" + filePath;

		logger.info(loginfo);
	}

	/**
	 * This method checks if the Caller Principal has read access for the
	 * document.
	 * 
	 * @return true if user has readaccess
	 */
	private boolean isCallerReader(Document document) {

		ItemCollection itemcol = new ItemCollection(document.getData());

		@SuppressWarnings("unchecked")
		List<String> readAccessList = itemcol.getItemValue("$readaccess");

		/**
		 * 1.) org.imixs.ACCESSLEVEL.NOACCESS
		 * 
		 * always = false -> no access
		 */

		if (ctx.isCallerInRole(ACCESSLEVEL_NOACCESS))
			return false;

		/**
		 * 2.) org.imixs.ACCESSLEVEL.MANAGERACCESS
		 * 
		 * always = true -> grant access.
		 */

		if (ctx.isCallerInRole(ACCESSLEVEL_MANAGERACCESS))
			return true;

		/**
		 * 2.) org.imixs.ACCESSLEVEL.EDITOR org.imixs.ACCESSLEVEL.AUTHOR
		 * ACCESSLEVEL.READER
		 * 
		 * check read access
		 */
		if (readAccessList == null || readAccessList.size() == 0) {
			// no restriction found
			return true;
		}

		boolean notemptyfield = false;

		// get user name list
		List<String> auserNameList = getUserNameList();

		// check each read access
		for (String aReadAccess : readAccessList) {
			if (aReadAccess != null && !aReadAccess.isEmpty()) {
				notemptyfield = true;
				if (auserNameList.indexOf(aReadAccess) > -1)
					return true;

			}
		}
		if (!notemptyfield)
			return true;

		return false;
	}

	/**
	 * Verifies if the caller has write access to the current document
	 * 
	 * @return
	 */
	private boolean isCallerAuthor(Document document) {

		ItemCollection itemcol = new ItemCollection(document.getData());

		@SuppressWarnings("unchecked")
		List<String> writeAccessList = itemcol.getItemValue("$writeaccess");

		/**
		 * 1.) org.imixs.ACCESSLEVEL.NOACCESS allways false - now write access!
		 */
		if (ctx.isCallerInRole(ACCESSLEVEL_NOACCESS))
			return false;

		/**
		 * 2.) org.imixs.ACCESSLEVEL.MANAGERACCESS or
		 * org.imixs.ACCESSLEVEL.EDITOR Always true - grant writeaccess.
		 */
		if (ctx.isCallerInRole(ACCESSLEVEL_MANAGERACCESS) || ctx.isCallerInRole(ACCESSLEVEL_EDITORACCESS))
			return true;

		/**
		 * 2.) org.imixs.ACCESSLEVEL.AUTHOR
		 * 
		 * check write access in detail
		 */

		if (ctx.isCallerInRole(ACCESSLEVEL_AUTHORACCESS)) {
			if (writeAccessList == null || writeAccessList.size() == 0) {
				// now write access
				return false;
			}

			// get user name list
			List<String> auserNameList = getUserNameList();

			// check each read access
			for (String aWriteAccess : writeAccessList) {
				if (aWriteAccess != null && !aWriteAccess.isEmpty()) {
					if (auserNameList.indexOf(aWriteAccess) > -1)
						return true; // user role known - grant access
				}
			}
		}
		return false;
	}

	/**
	 * This method read the param USER_GROUP_LIST from the EJB ContextData. This
	 * context data object can provide a string array with application specific
	 * dynamic user groups. These groups are used to grant access to an entity
	 * independent from the static User-Role settings. The method returns null
	 * if no ContextData is set
	 * 
	 * @return - list of user group names or null if not USER_GROUP_LIST is
	 *         defined
	 */
	private String[] getUserGroupList() {
		// read dynamic user roles from the ContextData (if provided) ....
		String[] applicationUserGroupList = (String[]) ctx.getContextData().get(USER_GROUP_LIST);

		if (applicationUserGroupList != null)
			// trim entries....
			for (int i = 0; i < applicationUserGroupList.length; i++) {
			applicationUserGroupList[i] = applicationUserGroupList[i].trim();
			}

		return applicationUserGroupList;
	}

}
