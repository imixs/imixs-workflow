/****************************************************************************
 * Copyright (c) 2022-2025 Imixs Software Solutions GmbH and others.
 * https://www.imixs.com
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * This Source Code may also be made available under the terms of the
 * GNU General Public License, version 2 or later (GPL-2.0-or-later),
 * which is available at https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-or-later
 ****************************************************************************/

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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.index.DefaultOperator;
import org.imixs.workflow.engine.index.SearchService;
import org.imixs.workflow.engine.index.SortOrder;
import org.imixs.workflow.engine.index.UpdateService;
import org.imixs.workflow.engine.jpa.Document;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.QueryException;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

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
public class DocumentService {

	public static final String ACCESSLEVEL_NOACCESS = "org.imixs.ACCESSLEVEL.NOACCESS";

	public static final String ACCESSLEVEL_READERACCESS = "org.imixs.ACCESSLEVEL.READERACCESS";

	public static final String ACCESSLEVEL_AUTHORACCESS = "org.imixs.ACCESSLEVEL.AUTHORACCESS";

	public static final String ACCESSLEVEL_EDITORACCESS = "org.imixs.ACCESSLEVEL.EDITORACCESS";

	public static final String ACCESSLEVEL_MANAGERACCESS = "org.imixs.ACCESSLEVEL.MANAGERACCESS";

	public static final String EVENTLOG_TOPIC_INDEX_ADD = "index.add";
	public static final String EVENTLOG_TOPIC_INDEX_REMOVE = "index.remove";

	public static final String READACCESS = "$readaccess";
	public static final String WRITEACCESS = "$writeaccess";
	public static final String ISAUTHOR = "$isAuthor";
	public static final String NOINDEX = "$noindex";
	public static final String IMMUTABLE = "$immutable";
	public static final String VERSION = "$version";

	private static final String REGEX_UUID = "([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})|([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}-[0-9]{13,15})";
	private static final String REGEX_OLDUID = "([0-9a-f]{8}-.*|[0-9a-f]{11}-.*)";

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

	@Inject
	private UpdateService indexUpdateService;

	@Inject
	private SearchService indexSearchService;

	@Inject
	private EventLogService eventLogService;

	@Inject
	protected Event<DocumentEvent> documentEvents;

	@Inject
	protected Event<UserGroupEvent> userGroupEvents;

	@Inject
	@ConfigProperty(name = "index.defaultOperator", defaultValue = "AND")
	private String indexDefaultOperator;

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
	public boolean isUserContained(List<?> nameList) {
		if (nameList == null) {
			return false;
		}
		List<String> userNameList = getUserNameList();
		// check each element of the given nameList
		for (Object item : nameList) {
			if (item != null) {
				String aName = item.toString();
				if (!aName.isEmpty() &&
						userNameList.stream().anyMatch(aName::equals)) {
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
	 * @param ItemCollection to be saved
	 * @return updated ItemCollection
	 * @throws AccessDeniedException
	 */
	public ItemCollection save(ItemCollection document) throws AccessDeniedException {
		boolean debug = logger.isLoggable(Level.FINE);
		long lSaveTime = System.currentTimeMillis();
		if (debug) {
			logger.log(Level.FINEST, "......save - ID={0}, provided version={1}",
					new Object[] { document.getUniqueID(), document.getItemValueInteger(VERSION) });
		}
		Document persistedDocument = null;
		// Now set flush Mode to COMMIT
		manager.setFlushMode(FlushModeType.COMMIT);

		// check if a $uniqueid is available
		String sID = document.getItemValueString(WorkflowKernel.UNIQUEID);

		if (!sID.isEmpty() && !isValidUIDPattern(sID)) {
			throw new InvalidAccessException(INVALID_PARAMETER, "invalid UUID pattern - " + sID);
		}

		if (!sID.isEmpty()) {
			// yes so we can try to find the Entity by its primary key
			persistedDocument = manager.find(Document.class, sID);
			if (debug && persistedDocument == null) {
				logger.log(Level.FINEST, "......Document ''{0}'' not found!", sID);
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
			if (debug) {
				logger.finest("......persist activeEntity");
			}
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
		if (debug) {
			logger.log(Level.FINEST, "......save - ID={0} managed version={1}",
					new Object[] { document.getUniqueID(), persistedDocument.getVersion() });
		}
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
			addDocumentToIndex(document);
		} else {
			// remove from index
			removeDocumentFromIndex(document.getUniqueID());
		}

		/*
		 * issue #230
		 * 
		 * flag this entity which is still managed
		 */
		persistedDocument.setPending(true);

		if (debug) {
			logger.log(Level.FINE, "...''{0}'' saved in {1}ms",
					new Object[] { document.getUniqueID(), System.currentTimeMillis() - lSaveTime });
		}
		// return the updated document
		return document;
	}

	/**
	 * This method adds a single document into the to the Lucene index. Before the
	 * document is added to the index, a new eventLog is created. The document will
	 * be indexed after the method flushEventLog is called. This method is called by
	 * the LuceneSearchService finder methods.
	 * <p>
	 * The method supports committed read. This means that a running transaction
	 * will not read an uncommitted document from the Lucene index.
	 * 
	 * 
	 * @param documentContext
	 */
	public void addDocumentToIndex(ItemCollection document) {
		// skip if the flag 'noindex' = true
		if (!document.getItemValueBoolean(DocumentService.NOINDEX)) {
			// write a new EventLog entry for each document....
			eventLogService.createEvent(EVENTLOG_TOPIC_INDEX_ADD, document.getUniqueID());
		}
	}

	/**
	 * This method adds a new eventLog for a document to be deleted from the index.
	 * The document will be removed from the index after the method fluschEventLog
	 * is called. This method is called by the LuceneSearchService finder method
	 * only.
	 * 
	 * 
	 * @param uniqueID of the workitem to be removed
	 * @throws PluginException
	 */
	public void removeDocumentFromIndex(String uniqueID) {
		boolean debug = logger.isLoggable(Level.FINE);
		long ltime = System.currentTimeMillis();
		eventLogService.createEvent(EVENTLOG_TOPIC_INDEX_REMOVE, uniqueID);
		if (debug) {
			logger.log(Level.FINE, "... update eventLog cache in {0} ms (1 document to be removed)",
					System.currentTimeMillis() - ltime);
		}
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
	 * @param id - the $uniqueid of the ItemCollection to be loaded
	 * @return ItemCollection object or null if the Document dose not exist or the
	 *         CallerPrincipal hat insufficient read access.
	 * 
	 */
	public ItemCollection load(String id) {
		boolean debug = logger.isLoggable(Level.FINE);
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
				if (debug) {
					logger.log(Level.FINEST, "......clone manged entity ''{0}'' pending status={1}",
							new Object[] { id, persistedDocument.isPending() });
				}
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
			if (debug) {
				logger.log(Level.FINE, "...''{0}'' loaded in {1}ms",
						new Object[] { result.getUniqueID(), System.currentTimeMillis() - lLoadTime });
			}
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
	 * @param ItemCollection to be removed
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
				removeDocumentFromIndex(document.getUniqueID());
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
	 * @throws QueryException in case the searchterm is not understandable.
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
	 * @param maxResult       - max search result
	 * @param defaultOperator - optional to change the default search operator
	 * 
	 * @return total hits of search result
	 * @throws QueryException in case the searchterm is not understandable.
	 */
	public int count(String sSearchTerm, int maxResult) throws QueryException {
		indexUpdateService.updateIndex();
		return indexSearchService.getTotalHits(sSearchTerm, maxResult, null);
	}

	/**
	 * Returns the total pages for a given search term and a given page size.
	 * 
	 * @see count(String sSearchTerm)
	 * 
	 * @param searchTerm
	 * @param pageSize
	 * @return total pages of search result
	 * @throws QueryException in case the searchterm is not understandable.
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
	 * The method returns a list of ItemCollections from the search-index. The
	 * method expects an valid Lucene search term.
	 * <p>
	 * The method returns only ItemCollections which are readable by the
	 * CallerPrincipal. With the pageSize and pageNumber it is possible to paginate.
	 * <p>
	 * The Transactiontype REQUIRES_NEW ensure that during the processing lifecycle
	 * an external service call did not overwrite the current document jpa object
	 * (see Issue #634)
	 * 
	 * @param searchTerm - Lucene search term
	 * @param pageSize   - total docs per page
	 * @param pageIndex  - number of page to start (default = 0)
	 * @return list of ItemCollection elements
	 * @throws QueryException
	 * 
	 * @see org.imixs.workflow.engine.index.SearchService
	 */
	@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
	public List<ItemCollection> find(String searchTerm, int pageSize, int pageIndex) throws QueryException {
		return find(searchTerm, pageSize, pageIndex, null, false);
	}

	/**
	 * The method returns a sorted list of ItemCollections from the search-index.
	 * The result list can be sorted by a sortField and a sort direction.
	 * <p>
	 * The method expects an valid Lucene search term. The method returns only
	 * ItemCollections which are readable by the CallerPrincipal. With the pageSize
	 * and pageNumber it is possible to paginate.
	 * <p>
	 * The Transactiontype REQUIRES_NEW ensure that during the processing lifecycle
	 * an external service call did not overwrite the current document jpa object
	 * (see Issue #634)
	 * 
	 * @param searchTerm  - Lucene search term
	 * @param pageSize    - total docs per page
	 * @param pageIndex   - number of page to start (default = 0)
	 * 
	 * @param sortBy      -optional field to sort the result
	 * @param sortReverse - optional sort direction
	 * 
	 * @return list of ItemCollection elements
	 * @throws QueryException
	 * 
	 * @see org.imixs.workflow.engine.index.SearchService
	 */
	@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
	public List<ItemCollection> find(String searchTerm, int pageSize, int pageIndex, String sortBy, boolean sortReverse)
			throws QueryException {
		boolean debug = logger.isLoggable(Level.FINE);
		if (debug) {
			logger.log(Level.FINEST,
					"......find - SearchTerm={0}  , pageSize={1} pageNumber={2} , sortBy={3} reverse={4}",
					new Object[] { searchTerm, pageSize, pageIndex, sortBy, sortReverse });
		}
		// create sort object
		SortOrder sortOrder = null;
		if (sortBy != null && !sortBy.isEmpty()) {
			// we do not support multi values here - see
			// LuceneUpdateService.addItemValues
			// it would be possible if we use a SortedSetSortField class here
			sortOrder = new SortOrder(sortBy, sortReverse);
		}

		// flush eventlog (see issue #411)
		indexUpdateService.updateIndex();

		// evaluate default index operator
		DefaultOperator defaultOperator = null;

		if (indexDefaultOperator != null && "OR".equals(indexDefaultOperator.toUpperCase())) {
			defaultOperator = DefaultOperator.OR;
		} else {
			defaultOperator = DefaultOperator.AND;
		}
		return indexSearchService.search(searchTerm, pageSize, pageIndex, sortOrder, defaultOperator, false);

	}

	/**
	 * The method returns a sorted list of Document Stubs from the search-index. A
	 * document stub contains only the items stored in the search index. These items
	 * can be defined by the property <code>lucence.indexFieldListStore</code>. See
	 * the LuceneUpdateService for details.
	 * <p>
	 * The result list can be sorted by a sortField and a sort direction.
	 * <p>
	 * The method expects an valid Lucene search term. The method returns only
	 * ItemCollections which are readable by the CallerPrincipal. With the pageSize
	 * and pageNumber it is possible to paginate.
	 * <p>
	 * 
	 * @param searchTerm  - Lucene search term
	 * @param pageSize    - total docs per page
	 * @param pageIndex   - number of page to start (default = 0)
	 * 
	 * @param sortBy      -optional field to sort the result
	 * @param sortReverse - optional sort direction
	 * 
	 * @return list of ItemCollection elements
	 * @throws QueryException
	 * 
	 * @see org.imixs.workflow.engine.index.SearchService
	 */
	public List<ItemCollection> findStubs(String searchTerm, int pageSize, int pageIndex, String sortBy,
			boolean sortReverse) throws QueryException {
		boolean debug = logger.isLoggable(Level.FINE);
		if (debug) {
			logger.log(Level.FINEST,
					"......find - SearchTerm={0}  , pageSize={1} pageNumber={2} , sortBy={3} reverse={4}",
					new Object[] { searchTerm, pageSize, pageIndex, sortBy, sortReverse });
		}
		// create sort object
		SortOrder sortOrder = null;
		if (sortBy != null && !sortBy.isEmpty()) {
			// we do not support multi values here - see
			// LuceneUpdateService.addItemValues
			// it would be possible if we use a SortedSetSortField class here
			sortOrder = new SortOrder(sortBy, sortReverse);
		}

		// flush eventlog (see issue #411)
		indexUpdateService.updateIndex();

		// evaluate default index operator
		DefaultOperator defaultOperator = null;
		;
		if (indexDefaultOperator != null && "OR".equals(indexDefaultOperator.toUpperCase())) {
			defaultOperator = DefaultOperator.OR;
		} else {
			defaultOperator = DefaultOperator.AND;
		}

		// find stubs only!
		return indexSearchService.search(searchTerm, pageSize, pageIndex, sortOrder, defaultOperator, true);

	}

	/**
	 * The method returns a collection of ItemCollections referred by a $uniqueid.
	 * <p>
	 * The method returns only ItemCollections which are readable by the
	 * CallerPrincipal. With the pageSize and pageNumber it is possible to paginate.
	 * <p>
	 * The Transactiontype REQUIRES_NEW ensure that during the processing lifecycle
	 * an external service call did not overwrite the current document jpa object
	 * (see Issue #634)
	 * 
	 * @param uniqueIdRef - $uniqueId to be referred by the collected documents
	 * @param pageSize    - total docs per page
	 * @param pageIndex   - number of page to start (default = 0)
	 * @return resultset
	 * 
	 */
	@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
	public List<ItemCollection> findDocumentsByRef(String uniqueIdRef, int pageSize, int pageIndex) {
		String searchTerm = "(" + "$uniqueidref:\"" + uniqueIdRef + "\")";
		try {
			return find(searchTerm, pageSize, pageIndex);
		} catch (QueryException e) {
			logger.log(Level.SEVERE, "findDocumentsByRef - invalid query: {0}", e.getMessage());
			return null;
		}
	}

	/**
	 * Returns an unordered list of all documents of a specific type. The method
	 * throws an InvalidAccessException in case no type attribute is defined.
	 * <p>
	 * The Transactiontype REQUIRES_NEW ensure that during the processing lifecycle
	 * an external service call did not overwrite the current document jpa object
	 * (see Issue #634)
	 * 
	 * @param type
	 * @return
	 * @throws InvalidAccessException
	 */
	@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
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
	 * <p>
	 * The Transactiontype REQUIRES_NEW ensure that during the processing lifecycle
	 * an external service call did not overwrite the current document jpa object
	 * (see Issue #634)
	 * 
	 * @param query - JPQL statement
	 * @return
	 * 
	 */
	@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
	public List<ItemCollection> getDocumentsByQuery(String query) {
		return getDocumentsByQuery(query, -1);

	}

	/**
	 * Returns all documents of by JPQL statement.
	 * <p>
	 * The Transactiontype REQUIRES_NEW ensure that during the processing lifecycle
	 * an external service call did not overwrite the current document jpa object
	 * (see Issue #634)
	 * 
	 * @param query     - JPQL statement
	 * @param maxResult - maximum result set
	 * @return
	 * 
	 */
	@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
	public List<ItemCollection> getDocumentsByQuery(String query, int maxResult) {
		return getDocumentsByQuery(query, 0, maxResult);
	}

	/**
	 * Returns all documents of by JPQL statement.
	 * <p>
	 * The Transactiontype REQUIRES_NEW ensure that during the processing lifecycle
	 * an external service call did not overwrite the current document jpa object
	 * (see Issue #634)
	 * 
	 * @param query       - JPQL statement
	 * @param firstResult - first result
	 * @param maxResult   - maximum result set
	 * @return - result set
	 * 
	 */
	@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
	public List<ItemCollection> getDocumentsByQuery(String query, int firstResult, int maxResult) {
		boolean debug = logger.isLoggable(Level.FINE);
		List<ItemCollection> result = new ArrayList<ItemCollection>();
		Query q = manager.createQuery(query);

		// setMaxResults ?
		if (maxResult > 0) {
			q.setMaxResults(maxResult);
		}
		// setFirstResult?
		if (firstResult > 0) {
			q.setFirstResult(firstResult);
		}

		long l = System.currentTimeMillis();
		@SuppressWarnings("unchecked")
		Collection<Document> documentList = q.getResultList();

		if (documentList == null) {
			if (debug) {
				logger.log(Level.FINE, "......getDocumentsByQuery: {0} - no ducuments found im {1} ms",
						new Object[] { query, System.currentTimeMillis() - l });
			}
			return result;
		}

		// filter result set by read access
		for (Document doc : documentList) {
			if (isCallerReader(doc)) {
				ItemCollection _tmp = null;
				if (doc.isPending()) {
					// we clone but do not detach
					if (debug) {
						logger.log(Level.FINEST, "......clone manged entity ''{0}'' pending status={1}",
								new Object[] { doc.getId(), doc.isPending() });
					}
					_tmp = new ItemCollection(doc.getData());
				} else {
					// the document is not managed, so we detach it
					_tmp = new ItemCollection();
					_tmp.setAllItems(doc.getData());
					manager.detach(doc);
				}
				updateMetaData(_tmp, doc);
				result.add(_tmp);
				// issue #647
				if (documentEvents != null) {
					documentEvents.fire(new DocumentEvent(_tmp, DocumentEvent.ON_DOCUMENT_LOAD));
				}
			}
		}
		if (debug) {
			logger.log(Level.FINE, "...getDocumentsByQuery: {0} - found {1} documents in {2} ms",
					new Object[] { query, documentList.size(), System.currentTimeMillis() - l });
		}
		return result;
	}

	/**
	 * This method creates a backup of the result set form a Lucene search query.
	 * The document list will be stored into the file system. The method stores the
	 * Map from the ItemCollection to be independent from version upgrades. To
	 * manage large dataSets the method reads the documents in smaller blocks
	 * <p>
	 * The optional parameter 'snapshots' can be set to 'true' to indicate that only
	 * the referred snapshot workitem should be stored. The snapshot is referred by
	 * the item $snapshotId.
	 * 
	 * @param query     - a Lucene search statement
	 * @param filePath  - the target file path in the server local file system
	 * @param snapshots - optional - if true, than only snapshots will be backuped.
	 *                  Default = false
	 * @throws IOException
	 * @throws QueryException
	 */
	public void backup(String query, String filePath, boolean snapshots) throws IOException, QueryException {
		boolean hasMoreData = true;
		int JUNK_SIZE = 100;
		long totalcount = 0;
		int pageIndex = 0;
		int icount = 0;

		logger.info("backup - starting...");
		logger.log(Level.INFO, "backup - query={0}", query);
		logger.log(Level.INFO, "backup - target={0}", filePath);

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
			logger.log(Level.INFO, "backup - processing...... {0} documents read....", col.size());

			if (col.size() < JUNK_SIZE) {
				hasMoreData = false;
				logger.finest("......all data read.");
			} else {
				pageIndex++;
				logger.finest("......next page...");
			}

			for (ItemCollection aworkitem : col) {
				Map<?, ?> hmap = null;
				if (snapshots == true) {
					// load the snapshot
					String snapshotID = aworkitem.getItemValueString("$snapshotid");
					if (!snapshotID.isEmpty()) {
						ItemCollection snapshotDoc = load(snapshotID);
						if (snapshotDoc != null) {
							hmap = snapshotDoc.getAllItems();
						}
					}
				}

				if (hmap == null) {
					// get serialized data
					hmap = aworkitem.getAllItems();
				}
				// write object
				out.writeObject(hmap);
				icount++;
			}
		}
		out.close();
		logger.log(Level.INFO, "backup - finished: {0} documents read totaly.", icount);
	}

	// default method
	public void backup(String query, String filePath) throws IOException, QueryException {
		this.backup(query, filePath, false);
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
		logger.log(Level.INFO, "...starting restor form file {0}...", filePath);
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
					logger.log(Level.INFO, "...restored {0} document in {1}ms....",
							new Object[] { totalcount, System.currentTimeMillis() - l });
					l = System.currentTimeMillis();
				}

			} catch (java.io.EOFException eofe) {
				break;
			} catch (ClassNotFoundException e) {
				errorCount++;
				logger.log(Level.WARNING, "...error importing workitem at position {0}{1} Error: {2}",
						new Object[] { totalcount, errorCount, e.getMessage() });
			} catch (AccessDeniedException e) {
				errorCount++;
				logger.log(Level.WARNING, "...error importing workitem at position {0}{1} Error: {2}",
						new Object[] { totalcount, errorCount, e.getMessage() });
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
		List<?> writeAccessList = itemcol.getItemValue(WRITEACCESS);

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
	 * Verifies if the caller has write access to the given ItemCollection
	 * (document).
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

	/**
	 * This method returns true if the given id is a valid UUID or SnapshotID (UUI +
	 * timestamp
	 * <p>
	 * We also need to support the old uid formats
	 * <code>4832b09a1a-20c38abd-1519421083952</code>
	 * 
	 * @param uid
	 * @return
	 */
	public boolean isValidUIDPattern(String uid) {
		boolean valid = uid.matches(REGEX_UUID);
		if (!valid) {
			// check old snapshot pattern
			valid = uid.matches(REGEX_OLDUID);
		}

		return valid;

	}
}
