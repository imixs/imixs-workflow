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
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
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

	public static final String READACCESS = "$readaccess";
	public static final String WRITEACCESS = "$writeaccess";
	public static final String ISAUTHOR = "$isAuthor";
	public static final String NOINDEX = "$noindex";
	public static final String IMMUTABLE = "$immutable";
	public static final String VERSION = "$version";

	public static final String USER_GROUP_LIST = "org.imixs.USER.GROUPLIST";

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

	@Inject
	protected Event<DocumentEvent> documentEvents;

	@Inject
	protected Event<UserGroupEvent> userGroupEvents;

	/**
	 * Returns a comma separated list of additional Access-Roles defined for this
	 * service
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
	 * This method returns a list of user names, roles and application groups the
	 * user belongs to.
	 * <p>
	 * A client can extend the list of user groups associated with a userId by
	 * reacting on the CDI event 'UserGrouptEvent'.
	 * 
	 * @see UserGroupEvent
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

		// To extend UserGroups we fire the CDI Event UserGroupEvent...
		if (userGroupEvents != null) {
			// create Group Event
			UserGroupEvent groupEvent = new UserGroupEvent(ctx.getCallerPrincipal().getName().toString());
			userGroupEvents.fire(groupEvent);
			if (groupEvent.getGroups() != null) {
				userNameList.addAll(groupEvent.getGroups());
			}

		} else {
			logger.warning("Missing CDI support for Event<UserGroupEvent> !");
		}

		// String[] applicationGroups = getUserGroupList();
		// if (applicationGroups != null)
		// for (String auserRole : applicationGroups)
		// userNameList.add(auserRole);

		return userNameList;
	}

	/**
	 * This method returns true, if at least one element of the current UserNameList
	 * is contained in a given name list. The comparison is case sensitive!
	 * 
	 * @param nameList
	 * @return
	 */
	public boolean isUserContained(List<String> nameList) {
		if (nameList == null) {
			return false;
		}
		List<String> userNameList = getUserNameList();
		// check each element of the given nameList
		for (String aName : nameList) {
			if (aName != null && !aName.isEmpty()) {
				if (userNameList.stream().anyMatch(aName::equals)) {
					return true;
				}
			}
		}
		// not found
		return false;
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
	 * This Method saves an ItemCollection into the database. If the ItemCollection
	 * is saved the first time the method generates a uniqueID ('$uniqueid') which
	 * can be used to identify the ItemCollection by its ID. If the ItemCollection
	 * was saved before, the method updates the existing ItemCollection stored in
	 * the database.
	 * 
	 * <p>
	 * The Method returns an updated instance of the ItemCollection containing the
	 * attributes $modified, $created, and $uniqueId
	 * 
	 * <p>
	 * The method throws an AccessDeniedException if the CallerPrincipal is not
	 * allowed to save or update the ItemCollection in the database. The
	 * CallerPrincipial should have at least the access Role
	 * org.imixs.ACCESSLEVEL.AUTHORACCESS
	 * 
	 * <p>
	 * The method adds/updates the document into the lucene index.
	 * 
	 * <p>
	 * The method returns a itemCollection without the $VersionNumber from the
	 * persisted entity. (see issue #226)
	 * <p>
	 * 
	 * <p>
	 * issue #230:
	 * 
	 * The document will be marked as 'saved' so that the methods load() and
	 * getDocumentsByQuery() can evaluate this flag. Depending on the state, the
	 * methods can decide the correct behavior. In general we detach a document in
	 * the load() and getDocumentsByQuery() method. This is for performance reasons
	 * and the fact, that a ItemCollection can hold byte arrays which will be copied
	 * by reference. In cases where these methods are called after a document was
	 * saved (document is now managed), in one single transaction, the detach call
	 * will discard the changes made by the save() method. For that reason we flag
	 * the entity and evaluate this flag in the load method evaluates the save
	 * status.
	 * 
	 * 
	 * @param ItemCollection
	 *            to be saved
	 * @return updated ItemCollection
	 * @throws AccessDeniedException
	 */
	public ItemCollection save(ItemCollection document) throws AccessDeniedException {
		long lSaveTime = System.currentTimeMillis();
		logger.finest("......save - ID=" + document.getUniqueID() + ", provided version="
				+ document.getItemValueInteger(VERSION));
		Document persistedDocument = null;
		// Now set flush Mode to COMMIT
		manager.setFlushMode(FlushModeType.COMMIT);

		// check if a $uniqueid is available
		String sID = document.getItemValueString(WorkflowKernel.UNIQUEID);
		if (!sID.isEmpty()) {
			// yes so we can try to find the Entity by its primary key
			persistedDocument = manager.find(Document.class, sID);
			if (persistedDocument == null) {
				logger.finest("......Document '" + sID + "' not found!");
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
			Date datCreated = document.getItemValueDate("$created");
			if (datCreated != null) {
				Calendar cal = Calendar.getInstance();
				cal.setTime(datCreated);
				// Overwrite Creation Date
				persistedDocument.setCreated(cal);
			}
			// now persist the new EntityBean!
			logger.finest("......persist activeEntity");
			manager.persist(persistedDocument);

		} else {
			// activeEntity exists - verify if current user has write- and
			// readaccess
			if (!isCallerAuthor(persistedDocument) || !isCallerReader(persistedDocument)) {
				throw new AccessDeniedException(OPERATION_NOTALLOWED, "You are not allowed to perform this operation");
			}

			// test if persistedDocument is IMMUTABLE
			if (ItemCollection.createByReference(persistedDocument.getData()).getItemValueBoolean(IMMUTABLE)) {
				throw new AccessDeniedException(OPERATION_NOTALLOWED, "Operation not allowed, document is immutable!");
			}
			// there is no need to merge the persistedDocument because it is
			// already managed by JPA!
		}

		// after all the persistedDocument is now managed through JPA!
		logger.finest(
				"......save - ID=" + document.getUniqueID() + " managed version=" + persistedDocument.getVersion());

		// remove the property $isauthor
		document.removeItem(ISAUTHOR);

		// verify type attribute
		String aType = document.getItemValueString("type");
		if ("".equals(aType)) {
			aType = "document";
			document.replaceItemValue("type", aType);
		}
		// update type attribute
		persistedDocument.setType(aType);

		// update the standard attributes $modified $created and $uniqueID
		document.replaceItemValue("$uniqueid", persistedDocument.getId());
		document.replaceItemValue("$created", persistedDocument.getCreated().getTime());
		// synchronize Document.modified and $modified
		Calendar cal = Calendar.getInstance();
		persistedDocument.setModified(cal);
		document.replaceItemValue("$modified", cal.getTime());

		// Finally we fire the DocumentEvent ON_DOCUMENT_SAVE
		if (documentEvents != null) {
			documentEvents.fire(new DocumentEvent(document, DocumentEvent.ON_DOCUMENT_SAVE));
		} else {
			logger.warning("Missing CDI support for Event<DocumentEvent> !");
		}
		// check consistency of $uniqueid and $created after event was processed.
		if ((!persistedDocument.getId().equals(document.getUniqueID()))
				|| (!persistedDocument.getCreated().getTime().equals(document.getItemValueDate("$created")))) {
			throw new InvalidAccessException(InvalidAccessException.INVALID_ID,
					"Invalid data after DocumentEvent 'ON_DOCUMENT_SAVE'.");
		}

		// Now prepare document for persisting......

		// update current version number into managed entity!
		if (disableOptimisticLocking) {
			// in case of optimistic locking is disabled we remove $version
			document.removeItem("$Version");
		}
		if (!disableOptimisticLocking && document.hasItem(VERSION) && document.getItemValueInteger(VERSION) > 0) {
			// if $version is provided we update the version number of
			// the managaed document to ensure that optimisticLock exception is
			// handled the right way!
			int version = document.getItemValueInteger(VERSION);
			persistedDocument.setVersion(version);
		}

		// finally update the data field by cloning the map object (deep copy)
		ItemCollection clone = (ItemCollection) document.clone();
		persistedDocument.setData(clone.getAllItems());

		/*
		 * Issue #220
		 * 
		 * No em.flush() call - see issue #226
		 **/

		/*
		 * issue #226
		 * 
		 * No em.flush(), em.detach() or em.clear() is needed here. Finally we remove
		 * the $version property from the ItemCollection returned to the client. This is
		 * important for the case that the method is called multiple times in one single
		 * transaction.
		 */

		// remove $version from ItemCollection
		document.removeItem(VERSION);

		// update the $isauthor flag
		document.replaceItemValue(ISAUTHOR, isCallerAuthor(persistedDocument));

		// add/update document into lucene index
		if (!document.getItemValueBoolean(NOINDEX)) {
			luceneUpdateService.updateDocument(document);
		} else {
			// remove from index
			luceneUpdateService.removeDocument(document.getUniqueID());
		}

		/*
		 * issue #230
		 * 
		 * flag this entity which is still managed
		 */
		persistedDocument.setPending(true);

		logger.fine("...'" + document.getUniqueID() + "' saved in " + (System.currentTimeMillis() - lSaveTime) + "ms");
		// return the updated document
		return document;
	}

	/**
	 * This method saves a workitem in a new transaction. The method can be used by
	 * plugins to isolate a save request from the current transaction context.
	 * 
	 * To call this method a EJB session context is necessary: <code>
	 * 		workitem= sessionContext.getBusinessObject(EntityService.class)
						.saveByNewTransaction(workitem);
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
	 * This method loads an ItemCollection from the Database. The method expects a
	 * valid $uniqueID to identify the Document entity saved before into the
	 * database. The method returns null if no Document with the corresponding ID
	 * exists.
	 * <p>
	 * The method checks if the CallerPrincipal has read access to Document stored
	 * in the database. If not, the method returns null.
	 * <p>
	 * <strong>Note:</strong> The method dose not throw an AccessDeniedException if
	 * the user is not allowed to read the entity to prevent a aggressor with
	 * informations about the existence of that specific Document.
	 * <p>
	 * The CallerPrincipial need to have at least the access level
	 * org.imixs.ACCESSLEVEL.READACCESS
	 * 
	 * <p>
	 * issue #230
	 * 
	 * In case a document is not flagged (not saved during same transaction), we
	 * detach the loaded entity. In case a document is flagged (saved during save
	 * transaction) we may not detach it, but make a deepCopy (clone) of the
	 * document instance. This will avoid the effect, that data written to a
	 * document get lost in a long running transaction with save and load calls.
	 * 
	 * @param id
	 *            - the $uniqueid of the ItemCollection to be loaded
	 * @return ItemCollection object or null if the Document dose not exist or the
	 *         CallerPrincipal hat insufficient read access.
	 * 
	 */
	public ItemCollection load(String id) {
		long lLoadTime = System.currentTimeMillis();
		Document persistedDocument = null;

		if (id == null || id.isEmpty()) {
			return null;
		}
		persistedDocument = manager.find(Document.class, id);

		// create instance of ItemCollection
		if (persistedDocument != null && isCallerReader(persistedDocument)) {

			ItemCollection result = null;// new ItemCollection();
			if (persistedDocument.isPending()) {
				// we clone but do not detach
				logger.finest("......clone manged entity '" + id + "' pending status=" + persistedDocument.isPending());
				result = new ItemCollection(persistedDocument.getData());
			} else {
				// the document is not managed, so we detach it
				result = new ItemCollection();
				result.setAllItems(persistedDocument.getData());
				manager.detach(persistedDocument);
			}

			updateMetaData(result, persistedDocument);

			// fire event
			if (documentEvents != null) {
				documentEvents.fire(new DocumentEvent(result, DocumentEvent.ON_DOCUMENT_LOAD));
			} else {
				logger.warning("Missing CDI support for Event<DocumentEvent> !");
			}
			logger.fine(
					"...'" + result.getUniqueID() + "' loaded in " + (System.currentTimeMillis() - lLoadTime) + "ms");
			return result;
		} else
			return null;
	}

	/**
	 * This method removes an ItemCollection from the database. If the
	 * CallerPrincipal is not allowed to access the ItemColleciton the method throws
	 * an AccessDeniedException.
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
	public void remove(ItemCollection document) throws AccessDeniedException {
		if (document == null) {
			return;
		}

		Document persistedDocument = null;
		String sID = document.getItemValueString("$uniqueid");
		persistedDocument = manager.find(Document.class, sID);

		if (persistedDocument != null) {
			if (!isCallerReader(persistedDocument) || !isCallerAuthor(persistedDocument))
				throw new AccessDeniedException(OPERATION_NOTALLOWED,
						"remove - You are not allowed to perform this operation");

			// fire event
			if (documentEvents != null) {
				documentEvents.fire(new DocumentEvent(document, DocumentEvent.ON_DOCUMENT_DELETE));
			} else {
				logger.warning("Missing CDI support for Event<DocumentEvent> !");
			}

			// remove document...
			manager.remove(persistedDocument);
			// remove document form index - @see issue #412
			if (!document.getItemValueBoolean(NOINDEX)) {
				luceneUpdateService.removeDocument(document.getUniqueID());
			}

		} else
			throw new AccessDeniedException(INVALID_UNIQUEID, "remove - invalid $uniqueid");
	}

	/**
	 * Returns the total hits for a given search query. The provided search term
	 * will be extended with a users roles to test the read access level of each
	 * workitem matching the search term. The usernames and user roles will be
	 * search lowercase!
	 * 
	 * @see search(String, int, int, Sort, Operator)
	 * 
	 * @param sSearchTerm
	 * @return total hits of search result
	 * @throws QueryException
	 *             in case the searchterm is not understandable.
	 */
	public int count(String searchTerm) throws QueryException {
		return count(searchTerm, 0);
	}

	/**
	 * Returns the total hits for a given search query. The provided search term
	 * will be extended with a users roles to test the read access level of each
	 * workitem matching the search term. The usernames and user roles will be
	 * search lowercase!
	 * 
	 * The optional param 'maxResult' can be set to overwrite the
	 * DEFAULT_MAX_SEARCH_RESULT.
	 * 
	 * @see search(String, int, int, Sort, Operator)
	 * 
	 * @param sSearchTerm
	 * @param maxResult
	 *            - max search result
	 * @param defaultOperator
	 *            - optional to change the default search operator
	 * 
	 * @return total hits of search result
	 * @throws QueryException
	 *             in case the searchterm is not understandable.
	 */
	public int count(String sSearchTerm, int maxResult) throws QueryException {
		return luceneSearchService.getTotalHits(sSearchTerm, maxResult, null);
	}

	/**
	 * Returns the total pages for a given search term and a given page size.
	 * 
	 * @see count(String sSearchTerm)
	 * 
	 * @param searchTerm
	 * @param pageSize
	 * @return total pages of search result
	 * @throws QueryException
	 *             in case the searchterm is not understandable.
	 */
	public int countPages(String searchTerm, int pageSize) throws QueryException {
		double pages = 1;
		double count = count(searchTerm);
		if (count > 0) {
			pages = Math.ceil(count / pageSize);
		}
		return ((int) pages);
	}

	/**
	 * The method returns a list of ItemCollections by calling the
	 * LuceneSearchService. The method expects an valid Lucene search term.
	 * <p>
	 * The method returns only ItemCollections which are readable by the
	 * CallerPrincipal. With the pageSize and pageNumber it is possible to paginate.
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
	 * LuceneSearchService. The result list can be sorted by a sortField and a sort
	 * direction.
	 * <p>
	 * The method expects an valid Lucene search term. The method returns only
	 * ItemCollections which are readable by the CallerPrincipal. With the pageSize
	 * and pageNumber it is possible to paginate.
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
		return find(searchTerm, pageSize, pageIndex, sortBy, sortReverse, false);
	}

	/**
	 * The method returns a sorted list of ItemCollections by calling the
	 * LuceneSearchService. The result list can be sorted by a sortField and a sort
	 * direction.
	 * <p>
	 * The method expects an valid Lucene search term. The method returns only
	 * ItemCollections which are readable by the CallerPrincipal. With the pageSize
	 * and pageNumber it is possible to paginate.
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
	 * @param loadStubs
	 *            - optional indicates if only the Lucene Document stubs should be
	 *            loaded
	 * @return list of ItemCollection elements
	 * @throws QueryException
	 * 
	 * @see org.imixs.workflow.engine.lucene.LuceneSearchService
	 */
	public List<ItemCollection> find(String searchTerm, int pageSize, int pageIndex, String sortBy, boolean sortReverse,
			boolean loadStubs) throws QueryException {
		logger.finest("......find - SearchTerm=" + searchTerm + "  , pageSize=" + pageSize + " pageNumber=" + pageIndex
				+ " , sortBy=" + sortBy + " reverse=" + sortReverse);

		// create sort object
		Sort sortOrder = null;
		if (sortBy != null && !sortBy.isEmpty()) {
			// we do not support multi values here - see
			// LuceneUpdateService.addItemValues
			// it would be possible if we use a SortedSetSortField class here
			sortOrder = new Sort(new SortField[] { new SortField(sortBy, Type.STRING, sortReverse) });
		}

		return luceneSearchService.search(searchTerm, pageSize, pageIndex, sortOrder, null, loadStubs);

	}

	/**
	 * The method returns a collection of ItemCollections referred by a $uniqueid.
	 * <p>
	 * The method returns only ItemCollections which are readable by the
	 * CallerPrincipal. With the pageSize and pageNumber it is possible to paginate.
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
	public List<ItemCollection> findDocumentsByRef(String uniqueIdRef, int pageSize, int pageIndex) {
		String searchTerm = "(" + "$uniqueidref:\"" + uniqueIdRef + "\")";
		try {
			return find(searchTerm, pageSize, pageIndex);
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
		return getDocumentsByQuery(query, -1);

	}

	/**
	 * Returns all documents of by JPQL statement.
	 * 
	 * @param query
	 *            - JPQL statement
	 * @param maxResult
	 *            - maximum result set
	 * @return
	 * 
	 */
	public List<ItemCollection> getDocumentsByQuery(String query, int maxResult) {
		return getDocumentsByQuery(query, 0, maxResult);
	}

	/**
	 * Returns all documents of by JPQL statement.
	 * 
	 * @param query
	 *            - JPQL statement
	 * @param maxResult
	 *            - maximum result set
	 * @return
	 * 
	 */
	public List<ItemCollection> getDocumentsByQuery(String query, int firstResult, int maxResult) {
		List<ItemCollection> result = new ArrayList<ItemCollection>();
		Query q = manager.createQuery(query);

		// setMaxResults ?
		if (maxResult > 0) {
			q.setMaxResults(maxResult);
		}

		if (firstResult > 0) {
			q.setFirstResult(firstResult);
		}

		long l = System.currentTimeMillis();
		@SuppressWarnings("unchecked")
		Collection<Document> documentList = q.getResultList();

		if (documentList == null) {
			logger.finest("......getDocumentsByQuery - no ducuments found.");
			return result;
		}

		// filter result set by read access
		for (Document doc : documentList) {
			if (isCallerReader(doc)) {

				ItemCollection _tmp = null;

				if (doc.isPending()) {
					// we clone but do not detach
					logger.finest("......clone manged entity '" + doc.getId() + "' pending status=" + doc.isPending());
					_tmp = new ItemCollection(doc.getData());
				} else {
					// the document is not managed, so we detach it
					_tmp = new ItemCollection();
					_tmp.setAllItems(doc.getData());
					manager.detach(doc);
				}

				updateMetaData(_tmp, doc);

				result.add(_tmp);
			}
		}

		logger.fine("...getDocumentsByQuery - found " + documentList.size() + " documents in "
				+ (System.currentTimeMillis() - l) + " ms");
		return result;
	}

	/**
	 * This method creates a backup of the result set form a Lucene search query.
	 * The document list will be stored into the file system. The method stores the
	 * Map from the ItemCollection to be independent from version upgrades. To
	 * manage large dataSets the method reads the documents in smaller blocks
	 * 
	 * @param entities
	 * @throws IOException
	 * @throws QueryException
	 */
	public void backup(String query, String filePath) throws IOException, QueryException {
		boolean hasMoreData = true;
		int JUNK_SIZE = 100;
		long totalcount = 0;
		int pageIndex = 0;
		int icount = 0;

		logger.info("backup - starting...");
		logger.info("backup - query=" + query);
		logger.info("backup - target=" + filePath);

		if (filePath == null || filePath.isEmpty()) {
			logger.severe("Invalid FilePath!");
			return;
		}

		FileOutputStream fos = new FileOutputStream(filePath);
		ObjectOutputStream out = new ObjectOutputStream(fos);
		while (hasMoreData) {
			// read a junk....

			Collection<ItemCollection> col = find(query, JUNK_SIZE, pageIndex);
			totalcount = totalcount + col.size();
			logger.info("backup - processing...... " + col.size() + " documents read....");

			if (col.size() < JUNK_SIZE) {
				hasMoreData = false;
				logger.finest("......all data read.");
			} else {
				pageIndex++;
				logger.finest("......next page...");
			}

			for (ItemCollection aworkitem : col) {
				// get serialized data
				Map<?, ?> hmap = aworkitem.getAllItems();
				// write object
				out.writeObject(hmap);
				icount++;
			}
		}
		out.close();
		logger.info("backup - finished: " + icount + " documents read totaly.");
	}

	/**
	 * This method restores a backup from the file system and imports the Documents
	 * into the database.
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
		logger.info("...starting restor form file " + filePath + "...");
		long l = System.currentTimeMillis();
		while (true) {
			try {
				// read one more object
				Map hmap = (Map) in.readObject();
				ItemCollection itemCol = new ItemCollection(hmap);
				// remove the $version property!
				itemCol.removeItem(VERSION);
				// now save imported data
				// issue #407 - call new transaction context...
				itemCol = ctx.getBusinessObject(DocumentService.class).saveByNewTransaction(itemCol);

				totalcount++;
				icount++;
				if (icount >= JUNK_SIZE) {
					icount = 0;
					logger.info("...restored " + totalcount + " document in " + (System.currentTimeMillis() - l)
							+ "ms....");
					l = System.currentTimeMillis();
				}

			} catch (java.io.EOFException eofe) {
				break;
			} catch (ClassNotFoundException e) {
				errorCount++;
				logger.warning("...error importing workitem at position " + (totalcount + errorCount) + " Error: "
						+ e.getMessage());
			} catch (AccessDeniedException e) {
				errorCount++;
				logger.warning("...error importing workitem at position " + (totalcount + errorCount) + " Error: "
						+ e.getMessage());
			}
		}
		in.close();

		String loginfo = "Import successfull! " + totalcount + " Entities imported. " + errorCount
				+ " Errors.  Import FileName:" + filePath;

		logger.info(loginfo);
	}

	/**
	 * Verifies if the caller has write access to the current ItemCollection
	 * 
	 * @return
	 */
	public boolean isAuthor(ItemCollection itemcol) {
		@SuppressWarnings("unchecked")
		List<String> writeAccessList = itemcol.getItemValue(WRITEACCESS);
	
		/**
		 * 1.) org.imixs.ACCESSLEVEL.NOACCESS allways false - now write access!
		 */
		if (ctx.isCallerInRole(ACCESSLEVEL_NOACCESS))
			return false;
	
		/**
		 * 2.) org.imixs.ACCESSLEVEL.MANAGERACCESS or org.imixs.ACCESSLEVEL.EDITOR
		 * Always true - grant writeaccess.
		 */
		if (ctx.isCallerInRole(ACCESSLEVEL_MANAGERACCESS) || ctx.isCallerInRole(ACCESSLEVEL_EDITORACCESS))
			return true;
	
		/**
		 * 2.) org.imixs.ACCESSLEVEL.AUTHOR
		 * 
		 * check write access in detail
		 */
	
		if (ctx.isCallerInRole(ACCESSLEVEL_AUTHORACCESS)) {
			if (isUserContained(writeAccessList)) {
				// user role known - grant access
				return true;
			}
		}
		return false;
	}

	/**
	 * This method udates the metadata of a new loaded ItemCollection based on the
	 * document attributes.
	 * <p>
	 * The metadata which is updated is:
	 * <ul>
	 * <li>$version - set to current version if OptimisticLocking is not
	 * disabled</li>
	 * <li>$modified - the modify timestamp from the document entity</li>
	 * <li>$isauthor - computed on the current users access level</li>
	 * </ul>
	 * 
	 * @see issue #497
	 * @param itemColection
	 * @param doc
	 */
	private void updateMetaData(ItemCollection itemColection, Document doc) {
		// if disable Optimistic Locking is TRUE we do not add the
		// version number
		if (disableOptimisticLocking) {
			itemColection.removeItem(VERSION);
		} else {
			itemColection.replaceItemValue(VERSION, doc.getVersion());
		}

		// Update $modified base on doc.getModified! (see issue #497)
		itemColection.replaceItemValue("$modified", doc.getModified().getTime());

		// update the $isauthor flag
		itemColection.replaceItemValue(ISAUTHOR, isCallerAuthor(doc));
	}

	/**
	 * This method checks if the Caller Principal has read access for the document.
	 * 
	 * @return true if user has readaccess
	 */
	private boolean isCallerReader(Document document) {

		ItemCollection itemcol = ItemCollection.createByReference(document.getData());

		@SuppressWarnings("unchecked")
		List<String> readAccessList = itemcol.getItemValue(READACCESS);

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
		if (isEmptyList(readAccessList) || isUserContained(readAccessList)) {
			return true;
		}

		return false;
	}

	/**
	 * Verifies if the caller has write access to the given ItemCollection (document).
	 * 
	 * @return true if the current user has author access
	 */
	private boolean isCallerAuthor(Document document) {
		ItemCollection itemcol = ItemCollection.createByReference(document.getData());
		return isAuthor(itemcol);
	}
	
	
	/**
	 * This method returns true if the given list is empty or contains only null or
	 * '' values.
	 * 
	 * @param aList
	 * @return
	 */
	public boolean isEmptyList(List<String> aList) {
		if (aList == null || aList.size() == 0) {
			return true;
		}
		// check each element
		for (String aEntry : aList) {
			if (aEntry != null && !aEntry.isEmpty()) {
				return false;
			}
		}
		return true;
	}

}
