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

package org.imixs.workflow.jee.ejb;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.jee.jpa.CalendarItem;
import org.imixs.workflow.jee.jpa.DoubleItem;
import org.imixs.workflow.jee.jpa.Entity;
import org.imixs.workflow.jee.jpa.EntityIndex;
import org.imixs.workflow.jee.jpa.IntegerItem;
import org.imixs.workflow.jee.jpa.ReadAccess;
import org.imixs.workflow.jee.jpa.TextItem;
import org.imixs.workflow.jee.jpa.WriteAccess;

/**
 * The EntityService is used to save and load instances of ItemCollections into
 * a Database. The EntityService throws an AccessDeniedException if the
 * CallerPrincipal is not allowed to save or read a specific ItemCollection from
 * the database. So the EntityService can be used to save business objects into
 * a database with individual read- or writeAccess restrictions.
 * <p>
 * The Bean holds an instance of an EntityPersistenceManager for the persistence
 * unit 'org.imixs.workflow.jee.ejb' to manage the following Entity EJBs:
 * <ul>
 * <li>Entity,
 * <li>EntityIndex,
 * <li>TextIndex,
 * <li>IntegerIndex,
 * <li>DoubleIndex,
 * <li>CalendarIndex,
 * <li>ReadAccessEntity
 * <li>WriteAccessEntity
 * </ul>
 * < These Entity EJBs are used to store the attributes of a ItemCollection into
 * the connected database.
 * <p>
 * The save() method persists any instance of an ItemCollection. If a
 * ItemCollection is saved the first time the EntityServiceBean generates the
 * attribute $uniqueid which will be included in the ItemCollection returned by
 * this method. If a ItemCollection was saved before the method updates the
 * corresponding Entity Object.
 * <p>
 * The load() and findAllEntities() methods are used to read ItemCollections
 * from the database. The remove() Method deletes a saved ItemCollection from
 * the database.
 * <p>
 * All methods expect and return Instances of the object
 * org.imixs.workflow.ItemCollection which is no entity EJB. So these objects
 * are not managed by any instance of an EntityPersistenceManager.
 * <p>
 * A collection of ItemCollections can be read using the findAllEntites() method
 * using EQL syntax.
 * <p>
 * This EntityServiceBean has methods to brand out different Attributes of a
 * ItemCollection into external properties (TextIndex, IntegerIndex,
 * DoubleIndex, CalendarIndex) With these functionality a client can query a set
 * of ItemCollections later with EJB Query Language (EQL).
 * <p>
 * To define which attributes should be expended into external properties the
 * Session EJB supports the method addIndex(). This method creates an index
 * entry and ensures that every Entity saved before will be updated and future
 * calls of the save() method will care about the new index to separate
 * attributes into the Index Properties.
 * <p>
 * So each call of the save method automatically disassemble a ItemCollection
 * into predefined Index properties and stores attributes which have an Index
 * into a corresponding IndexData Object (TextIndex, IntegerIndex, DoubleIndex,
 * CalendarIndex). On the other hand the load() method reassembles the
 * ItemCollection including all attributes form the data property an any
 * external index property. The EntiyService did not allow to access a managed
 * Entity EJB directly. This simplifies the usage as a client works only with
 * ItemCollections and so there is no need to handle managed and detached entity
 * EJB instances.
 * <p>
 * Additional to the basic functionality to save and load instances of the
 * object org.imixs.workflow.ItemCollection the method also manages the read-
 * and writeAccess for each instance of an ItemCollection. Therefore the save()
 * method scans an ItemCollection for the attributes '$ReadAccess' and
 * '$WriteAccess' and saves these informations into the Entity EJBs
 * ReadAccessEntity and WriteAccessEntity controlled by each instance of the EJB
 * Entity. The EntityServiceBean implementation verifies in each call of the
 * save() load(), remove() and findAllEntities() methods if the current
 * callerPrincipal is granted to the affected entities. If an ItemCollection was
 * saved with read- or writeAccess the access to an Instance of a saved
 * ItemCollection will be protected for a callerPrincipal with missing read- or
 * writeAccess. The default Read- and WriteAccess attributes '$ReadAccess' and
 * '$WriteAccess' can be overwritten by the environment settings
 * 'READ_ACCESS_FIELDS' and 'WRITE_ACCESS_FIELDS'
 * <p>
 * Some useful links about the oneToMany Relationship
 * http://www.avaje.org/manydatatypes.html
 * http://thomas-schuett.de/2008/11/14/ordered-lists-in-jpa-do-it-yourself/
 * http:
 * //javaehrsson.blogspot.com/2005/10/ejb3-onetomany-and-orderby-set-versus.html
 * http://forums.java.net/jive/thread.jspa?threadID=30869&start=0&tstart=0
 * 
 * @see org.imixs.workflow.jee.ejb.EntityPersistenceManager
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
@Deprecated
public class EntityService implements EntityServiceRemote {

	public static final int TYP_TEXT = 0;

	public static final int TYP_INT = 1;

	public static final int TYP_DOUBLE = 2;

	public static final int TYP_CALENDAR = 3;

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
	private final static Logger logger = Logger.getLogger(EntityService.class.getName());

	public static final String OPERATION_NOTALLOWED = "OPERATION_NOTALLOWED";

	public static final String INVALID_UNIQUEID = "INVALID_UNIQUEID";

	@Resource
	SessionContext ctx;

	@Resource(name = "READ_ACCESS_FIELDS")
	private String readAccessFields = "";
	@Resource(name = "WRITE_ACCESS_FIELDS")
	private String writeAccessFields = "";
	@Resource(name = "ACCESS_ROLES")
	private String accessRoles = "";
	@Resource(name = "DISABLE_OPTIMISTIC_LOCKING")
	private Boolean disableOptimisticLocking = false;

	@PersistenceContext(unitName = "org.imixs.workflow.jpa")
	private EntityManager manager;

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
	 * Returns additional ReadAccessFields defined for the EJB instance.
	 * Default=$ReadAccess
	 * 
	 * @return
	 */
	public String getReadAccessFields() {
		return readAccessFields;
	}

	public void setReadAccessFields(String readAccessFields) {
		this.readAccessFields = readAccessFields;
	}

	/**
	 * Returns additional WriteAccessFields defined for the EJB instance.
	 * Default=$WriteAccess
	 * 
	 * @return
	 */
	public String getWriteAccessFields() {
		return writeAccessFields;
	}

	public void setWriteAccessFields(String writeAccessFields) {
		this.writeAccessFields = writeAccessFields;
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
	@Deprecated
	public List<String> _getUserNameList() {

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
	 * This Method saves an ItemCollection into a database. If the
	 * ItemCollection is saved the first time the method generates a uniqueID
	 * ('$uniqueid') which can be used to identify the ItemCollection by its ID.
	 * If the ItemCollection was saved before, the method updates the
	 * ItemCollection stored in the database. The Method returns an updated
	 * instance of the ItemCollection containing the attributes $modified,
	 * $created, and $uniqueid
	 * <p>
	 * The method throws an AccessDeniedException if the CallerPrincipal is not
	 * allowed to save or update the ItemCollection in the database. The
	 * CallerPrincipial should have at least the access Role
	 * org.imixs.ACCESSLEVEL.AUTHORACCESS
	 * <p>
	 * The method returns a the detached itemCollection with the current
	 * VersionNumber from the persisted entity. (see issue #145)
	 * 
	 * @param ItemCollection
	 *            to be saved
	 * @return updated ItemCollection
	 */
	@Deprecated
	public ItemCollection _save(ItemCollection itemcol) throws AccessDeniedException {

		Entity activeEntity = null;
		ItemCollection slimItemCollection = null;
		logger.finest("[EntityService] save entity started");
		/*
		 * First we get a List of all existing Indices
		 * 
		 * if the FlushModeType=AUTO (which is the default) the getIndices()
		 * call will force an internal flush()
		 */
		Collection<EntityIndex> entityIndexCache = readIndices();

		// Now set flush Mode to COMMIT
		manager.setFlushMode(FlushModeType.COMMIT);

		// check if a $uniqueid is available
		String sID = itemcol.getItemValueString(UNIQUEID);
		if (!"".equals(sID)) {
			// yes so we can try to find the Entity by its primary key
			activeEntity = manager.find(Entity.class, sID);
			if (activeEntity == null) {
				logger.fine("[EntityService] Entity '" + sID + "' not found!");
			} else {

				// try { // #issue 102 - we try to flush :-/
				// logger.info("[EntityServiceBean] #issue 102 - refresh() - "
				// +sID);
				// manager.refresh(activeEntity);
				// } catch (EntityNotFoundException ex) {
				// logger.warning(
				// "[EntityServiceBean] #issue 102 - refresh()
				// EntityNotFoundException");
				// activeEntity = null;
				// }

			}

		}

		// did entity exist?
		if (activeEntity == null) {
			// entity not found in database, create a new instance using the
			// provided id. Test if user is allowed to create Entities....
			if (!(ctx.isCallerInRole(ACCESSLEVEL_MANAGERACCESS) || ctx.isCallerInRole(ACCESSLEVEL_EDITORACCESS)
					|| ctx.isCallerInRole(ACCESSLEVEL_AUTHORACCESS))) {
				throw new AccessDeniedException(OPERATION_NOTALLOWED,
						"[EntityService] You are not allowed to perform this operation");
			}
			// create new one with the provided id
			activeEntity = new Entity(sID);
			// if $Created is provided than overtake this information
			Date datCreated = itemcol.getItemValueDate("$Created");
			if (datCreated != null) {
				Calendar cal = Calendar.getInstance();
				cal.setTime(datCreated);
				// Overwrite Creation Date
				activeEntity.setCreated(cal);
			}

			// now persist the new Entity
			logger.finest("[EntityService] persist activeEntity");
			manager.persist(activeEntity);

		} else {
			// activeEntity exists - verify if current user has write- and
			// readaccess
			if (!isCallerAuthor(activeEntity) || !isCallerReader(activeEntity)) {
				throw new AccessDeniedException(OPERATION_NOTALLOWED,
						"[EntityService] You are not allowed to perform this operation");
			}
		}

		// after all the activeEntity is now managed through the persistence
		// manager!

		// remove the property $isauthor
		itemcol.removeItem("$isauthor");

		String aType = itemcol.getItemValueString("type");
		if ("".equals(aType))
			aType = "Entity";
		activeEntity.setType(aType);

		// update the standard attributes $modified $created and $uniqueid
		Calendar cal = Calendar.getInstance();

		itemcol.replaceItemValue("$uniqueid", activeEntity.getId());
		itemcol.replaceItemValue("$modified", cal.getTime());
		itemcol.replaceItemValue("$created", activeEntity.getCreated().getTime());
		// create a new Instance of this ItemCollection
		slimItemCollection = new ItemCollection(itemcol.getAllItems());

		// update read- and writeAccess List
		updateReadAccessList(slimItemCollection, activeEntity);
		updateWriteAccessList(slimItemCollection, activeEntity);

		explodeEntity(slimItemCollection, activeEntity, entityIndexCache);

		// finally update the data field and store the item map object
		activeEntity.setData(slimItemCollection.getAllItems());

		// verify and update the author access and add again the property
		// '$isauthor'
		itemcol.replaceItemValue("$isauthor", isCallerAuthor(activeEntity));

		// we now increase the $Version number
		// ! not necessary - version can be read after flush blow!

		// if (itemcol.hasItem("$Version")
		// && itemcol.getItemValueInteger("$Version") > 0) {
		// itemcol.replaceItemValue("$Version",
		// itemcol.getItemValueInteger("$Version") + 1);
		// }

		// if this did not work
		// we need to remove the $Version now to ensure that in one transaction
		// multiple saves are not blocked - in this case we can not know the new
		// version number to reactivate optimistic locking the client need to
		// reload the workItem!
		// itemcol.removeItem("$Version");

		/*
		 * Issue #166,#145
		 * 
		 * The flush call is important here. In cases of multiple updates of
		 * different entities in same transaction data can be lost if not
		 * flushed here! After the flush() the current version number can be
		 * read.
		 */
		manager.flush();
		// update version number
		itemcol.replaceItemValue("$Version", activeEntity.getVersion());

		/*
		 * Issue #189
		 * 
		 * We need to detach the activeEntity here. In other cases there are
		 * situations where updates caused by the vm are reflected back into the
		 * entity and increases the version number. This can be tested when a
		 * byte array is stored in a itemCollection. So for this reason we
		 * detach the entity here!!
		 */
		manager.detach(activeEntity);

		// return imploded itemCollection
		return itemcol;
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
	@Deprecated
	@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
	public ItemCollection _saveByNewTransaction(ItemCollection itemcol) throws AccessDeniedException {
		return _save(itemcol);
	}

	/**
	 * This method loads an ItemCollection from the Database. The method expects
	 * a valid $unqiueID to identify the ItemCollection saved before into the
	 * database. The method returns null if no ItemCollection with the
	 * corresponding ID exists.
	 * <p>
	 * The method checks if the CallerPrincipal has read access to
	 * ItemCollection stored in the database. If not the method returns null.
	 * The method dose not throw an AccessDeniedException if the user is not
	 * allowed to read the entity to prevent a aggressor with informations about
	 * the existence of that specific ItemCollection.
	 * <p>
	 * CallerPrincipial should have at least the access Role
	 * org.imixs.ACCESSLEVEL.READACCESS
	 * 
	 * @param id
	 *            - the $unqiueid of the ItemCollection to be loaded
	 * @return ItemCollection object or null if the ItemColleciton dose not
	 *         exist or the CallerPrincipal hat insufficient read access.
	 * 
	 */
	@Deprecated
	public ItemCollection _load(String id) {
		Entity activeEntity = null;
		activeEntity = manager.find(Entity.class, id);

		// implode the ItemCollection object
		if (activeEntity != null && isCallerReader(activeEntity)) {
			/*
			 * try { manager.refresh(activeEntity); } catch
			 * (EntityNotFoundException ex) { logger.warning(
			 * "[EntityServiceBean] #issue 102 - refresh() EntityNotFoundException"
			 * ); activeEntity = null; return null; }
			 */
			return implodeEntity(activeEntity);
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
	 * Also the method removes all existing relation ships of the entity. This
	 * is necessary becaus of the FetchType.LAZY used for most relations.
	 * 
	 * 
	 * @param ItemCollection
	 *            to be removed
	 * @throws AccessDeniedException
	 */
	@Deprecated
	public void _remove(ItemCollection itemcol) throws AccessDeniedException {
		Entity activeEntity = null;
		String sID = itemcol.getItemValueString("$uniqueid");
		activeEntity = manager.find(Entity.class, sID);

		if (activeEntity != null) {
			if (!isCallerReader(activeEntity) || !isCallerAuthor(activeEntity))
				throw new AccessDeniedException(OPERATION_NOTALLOWED,
						"[EntityService] You are not allowed to perform this operation");

			// remove current TextItem List
			for (TextItem aItem : activeEntity.getTextItems())
				manager.remove(aItem);
			activeEntity.getTextItems().clear();

			// remove current CalendarItem List
			for (CalendarItem aItem : activeEntity.getCalendarItems())
				manager.remove(aItem);
			activeEntity.getCalendarItems().clear();

			// remove current IntegerItem List
			for (IntegerItem aItem : activeEntity.getIntegerItems())
				manager.remove(aItem);
			activeEntity.getIntegerItems().clear();

			// remove current IntegerItem List/
			for (DoubleItem aItem : activeEntity.getDoubleItems())
				manager.remove(aItem);
			activeEntity.getDoubleItems().clear();

			// remove current WriteAccess List
			for (WriteAccess aItem : activeEntity.getWriteAccessList())
				manager.remove(aItem);
			activeEntity.getWriteAccessList().clear();

			// remove current ReadAccess List
			for (ReadAccess aItem : activeEntity.getReadAccessList())
				manager.remove(aItem);
			activeEntity.getReadAccessList().clear();

			// remove entity...
			manager.remove(activeEntity);

		} else
			throw new AccessDeniedException(INVALID_UNIQUEID, "[EntityService] invalid $uniqueid");
	}

	/**
	 * Adds an Imixs-Entity-Index for a property provided by ItemCollection
	 * objects. An Imixs-Entity-Index can be used to select ItemCollections
	 * using a JPQL statement. @see findEntitesByQuery
	 * 
	 * The method throws an AccessDeniedException if the CallerPrinciapal is not
	 * in the role org.imixs.ACCESSLEVEL.MANAGERACCESS.
	 * 
	 * @param name
	 *            of a property (not case sensetive)
	 * @param ityp
	 *            - Type of EntityIndex
	 * @throws AccessDeniedException
	 */
	public void addIndex(String stitel, int ityp) throws AccessDeniedException {
		// lower case title
		stitel = stitel.toLowerCase();

		// check if index already exists?
		EntityIndex activeEntityIndex = manager.find(EntityIndex.class, stitel);
		if (activeEntityIndex != null)
			return;

		// Check security roles
		// only MANAGERACCESS is allowed to add index and rebuild existing
		// entities!
		if (ctx.isCallerInRole("org.imixs.ACCESSLEVEL.MANAGERACCESS") == false)
			throw new AccessDeniedException(OPERATION_NOTALLOWED,
					"[EntityService] You are not allowed to add index fields");

		// add new index
		logger.info("[EntityServiceBean] add new Index: " + stitel + ":" + ityp);
		activeEntityIndex = new EntityIndex(stitel, ityp);
		manager.persist(activeEntityIndex);

		// we do no longer update existing entities. Need to be implemented by
		// the client!
		// see issue #94

		// Collection<Entity> entityList = null;
		// Query q = manager.createQuery("SELECT entity FROM Entity entity");
		// entityList = q.getResultList();
		// updateAllEntityIndexFields(entityList, stitel);

	}

	/**
	 * This method removes an existing Imixs-Entity-Index from the current
	 * indexlist. Notice that the index field name will be lowercased! Each EQL
	 * statement should use lower cased fieldnames!
	 * 
	 * The method checks if the Caller is in Role
	 * "org.imixs.ACCESSLEVEL.MANAGERACCESS".
	 * 
	 * 
	 * @param stitel
	 *            - will be automatical lowercased!
	 * @throws AccessDeniedException
	 * 
	 */
	public void removeIndex(String stitel) throws AccessDeniedException {
		int indexType = 0;
		// lower case title
		stitel = stitel.toLowerCase();

		// check if index already exists?
		EntityIndex activeEntityIndex = manager.find(EntityIndex.class, stitel);
		if (activeEntityIndex == null)
			return;

		// Check security roles
		if (ctx.isCallerInRole("org.imixs.ACCESSLEVEL.MANAGERACCESS") == false)
			throw new AccessDeniedException(OPERATION_NOTALLOWED,
					"[EntityService] You are not allowed to add index fields");

		indexType = activeEntityIndex.getTyp();

		logger.info("[EntityService] remove Index: " + stitel + ":" + indexType);

		// remove index
		manager.remove(activeEntityIndex);

		// we do no longer update existing entities. Need to be implemented by
		// the client!
		// see issue #94

		// Collection<Entity> entityList = null;
		// String query = "SELECT wi FROM Entity wi ";
		// switch (indexType) {
		// case EntityIndex.TYP_CALENDAR:
		// query += " JOIN wi.calendarItems AS i1 WHERE i1.itemName='"
		// + stitel + "'";
		// break;
		// case EntityIndex.TYP_DOUBLE:
		// query += " JOIN wi.doubleItems AS i1 WHERE i1.itemName='" + stitel
		// + "'";
		// break;
		// case EntityIndex.TYP_INT:
		// query += " JOIN wi.integerItems AS i1 WHERE i1.itemName='" + stitel
		// + "'";
		// break;
		//
		// default:
		// query += " JOIN wi.textItems AS i1 WHERE i1.itemName='" + stitel
		// + "'";
		// break;
		// }
		//
		// logger.info("[EntityServiceBean] remove Index - update query=" +
		// query);
		// Query q = manager.createQuery(query);
		//
		// entityList = q.getResultList();
		// updateAllEntityIndexFields(entityList, null);

	}

	/**
	 * The method returns a Map containing all EntityIndex Key/Type pairs
	 * 
	 * @return
	 */
	public Map<String, Integer> getIndices() {
		HashMap<String, Integer> indexList = new HashMap<String, Integer>();
		List<EntityIndex> list = readIndices();
		for (EntityIndex aIndex : list) {
			indexList.put(aIndex.getName(), aIndex.getTyp());
		}

		return indexList;
	}

	/**
	 * helper method to read the current EntityIndices
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<EntityIndex> readIndices() {
		logger.finer("readIndices....");
		Query q = manager.createQuery("SELECT entityindex FROM EntityIndex entityindex");

		return q.getResultList();
	}

	/**
	 * The method returns a collection of ItemCollections. The method expects an
	 * valid jPQL statement. The method returns only ItemCollections which are
	 * readable by the CallerPrincipal. With the startpos and count parameters
	 * it is possible to read chunks of entities. The jPQL Statement must match
	 * the conditions of the JPA Object Class Entity
	 * 
	 * @param query
	 *            - JQPL statement
	 * @param startpos
	 *            - optional start position
	 * @param maxcount
	 *            - maximum count of elements to be returned
	 * @return list of ItemCollection elements
	 * @throws InvalidAccessException
	 * 
	 * @see org.imixs.workfow.jee.jpa.Entity
	 */
	@SuppressWarnings("unchecked")
	@Deprecated
	public List<ItemCollection> _findAllEntities(String query, int startpos, int maxcount)
			throws InvalidAccessException {

		long l = 0;

		logger.fine("[EntityService] findAllEntities - Query=" + query);
		logger.fine("[EntityService] findAllEntities - Startpos=" + startpos + " maxcount=" + maxcount);
		List<ItemCollection> vectorResult = new ArrayList<ItemCollection>();

		// optimize query....
		query = optimizeQuery(query);
		try {
			Query q = manager.createQuery(query);
			if (startpos >= 0)
				q.setFirstResult(startpos);
			if (maxcount > 0)
				q.setMaxResults(maxcount);

			l = System.currentTimeMillis();
			Collection<Entity> entityList = q.getResultList();
			logger.fine(
					"[EntityService] findAllEntities - getResultList in " + (System.currentTimeMillis() - l) + " ms");

			if (entityList == null)
				return vectorResult;

			logger.fine("[EntityService] findAllEntities - ResultList size=" + entityList.size());
			l = System.currentTimeMillis();
			for (Entity aEntity : entityList) {
				/*
				 * try { manager.refresh(aEntity); } catch
				 * (EntityNotFoundException ex) { logger.warning(
				 * "[EntityServiceBean] #issue 102 - refresh() EntityNotFoundException"
				 * ); aEntity = null; continue; }
				 */

				// implode the ItemCollection object and add it to the resultset
				vectorResult.add(implodeEntity(aEntity));
			}

			logger.fine("[EntityService] findAllEntities in " + (System.currentTimeMillis() - l) + " ms");

		} catch (RuntimeException nre) {
			throw new InvalidAccessException("[EntityService] Error findAllEntities: '" + query + "' ", nre);
		}
		return vectorResult;

	}

	/**
	 * The method returns only the count of entities for an an valid jPQL
	 * statement. The method counts only ItemCollections which are readable by
	 * the CallerPrincipal. With the startpos and count parameters it is
	 * possible to read chunks of entities. The jPQL Statement must match the
	 * conditions of the JPA Object Class Entity.
	 * 
	 * The method replaces "SELECT DISTINCT entity" into
	 * "SELECT COUNT (entity.id)"
	 * 
	 * @param query
	 *            - JQPL statement
	 * @param startpos
	 *            - optional start position
	 * @param maxcount
	 *            - maximum count of elements to be returned
	 * @return count of elements to returned by this query
	 * @throws InvalidAccessException
	 * 
	 * @see org.imixs.workfow.jee.jpa.Entity
	 */
	public int countAllEntities(String query) throws InvalidAccessException {

		long l = 0;

		logger.fine("[EntityService] countAllEntities - Query=" + query);

		// optimize query....
		query = optimizeQuery(query);

		// replace DISTINCT into COUNT
		StringTokenizer st = new StringTokenizer(query);
		st.nextToken();
		String chunk1 = st.nextToken();
		String chunk2 = st.nextToken();

		int pos = query.indexOf(chunk1);
		query = query.substring(0, pos) + "COUNT" + query.substring(pos + 8);

		pos = query.indexOf(chunk2);
		query = query.substring(0, pos) + "(" + chunk2 + ".id)" + query.substring(pos + chunk2.length());

		Query q = manager.createQuery(query);

		Number cResults = (Number) q.getSingleResult();

		logger.fine("[EntityService] countAllEntities in " + (System.currentTimeMillis() - l) + " ms");
		return cResults.intValue();

	}

	/**
	 * The method returns the parent ItemCollection to a given child
	 * ItemCollection. A parent ItemCollection is referenced by a child
	 * ItemCollection through the property $uniqueidRef which is equals the
	 * parent entity's $uniqueID.
	 * 
	 * @param childentity
	 * @return parententity
	 */
	public ItemCollection findParentEntity(ItemCollection child) throws InvalidAccessException {
		String parentUniqueID = child.getItemValueString("$uniqueidref");
		return this._load(parentUniqueID);
	}

	/**
	 * The method returns a collection of child ItemCollections. A child entity
	 * is defined by the property $uniqueidRef which points to a parent entity.
	 * 
	 * The method returns only ItemCollections which are readable by the
	 * CallerPrincipal. With the startPos and count parameters it is possible to
	 * read chunks of entities.
	 * 
	 * @see findParentEntity
	 * 
	 * @param parententity
	 * @param startpos
	 * @param count
	 * @return
	 * @throws InvalidAccessException
	 */
	public List<ItemCollection> findChildEntities(ItemCollection child, int start, int count)
			throws InvalidAccessException {

		String parentUniqueID = child.getItemValueString("$uniqueid");

		String sQuery = "SELECT wi FROM Entity AS wi ";
		sQuery += " JOIN wi.textItems as t2 ";
		sQuery += " WHERE t2.itemName = '$uniqueidref' and t2.itemValue = '" + parentUniqueID + "' ";

		return this._findAllEntities(sQuery, start, count);
	}

	/**
	 * This method creates a backup of the result set form a JQPL query. The
	 * entity list will be stored into the file system. The method stores the
	 * Map from the ItemCollection to be independent from version upgrades. To
	 * manage large dataSets the method reads the entities in smaller blocks
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

		logger.info("[EntityService] Starting backup....");
		logger.info("[EntityService] Query=" + query);
		logger.info("[EntityService] Target=" + filePath);

		if (filePath == null || filePath.isEmpty()) {
			logger.severe("[EntityService] Invalid FilePath!");
			return;
		}

		FileOutputStream fos = new FileOutputStream(filePath);
		ObjectOutputStream out = new ObjectOutputStream(fos);
		while (hasMoreData) {
			// read a junk....
			Collection<ItemCollection> col = _findAllEntities(query, startpos, JUNK_SIZE);
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
	 * entities into the database.
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
				_save(itemCol);
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
	 * activeEntity.
	 * 
	 * @return true if user has readaccess
	 */
	private boolean isCallerReader(Entity aEntity) {

		List<ReadAccess> readAccessList = aEntity.getReadAccessList();

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
		List<String> auserNameList = _getUserNameList();

		// check each read access
		for (ReadAccess aReadAccess : readAccessList) {
			if (aReadAccess != null && !"".equals(aReadAccess.getValue())) {
				notemptyfield = true;
				if (auserNameList.indexOf(aReadAccess.getValue()) > -1)
					return true;

			}
		}
		if (!notemptyfield)
			return true;

		return false;
	}

	/**
	 * Verifies if the caller has write access to the current entity object.
	 * 
	 * @return
	 */
	private boolean isCallerAuthor(Entity aEntity) {
		List<WriteAccess> writeAccessList = aEntity.getWriteAccessList();

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
				// now wirte access
				return false;
			}

			// get user name list
			List<String> auserNameList = _getUserNameList();

			// check each read access
			for (WriteAccess aWriteAccess : writeAccessList) {
				if (aWriteAccess != null && !"".equals(aWriteAccess.getValue())) {
					if (auserNameList.indexOf(aWriteAccess.getValue()) > -1)
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

	/**
	 * This method updates the internal WriteAccessList. Therefore the method
	 * verifies if the itemCollection contains WriteAccess properties. The
	 * default property name is '$writeaccess'. Additional property names can be
	 * provided by the resource "WRITE_ACCESS_FIELDS" through the deployment
	 * descriptor. Empty values will be ignored.
	 * <p>
	 * The Values of the corresponding Items in the ItemCollection will not be
	 * removed by this method because access values could be assigned to
	 * different properties.
	 * 
	 * @return the new itemCollection without writeaccess properties
	 */
	@SuppressWarnings("rawtypes")
	private void updateWriteAccessList(ItemCollection itemCol, Entity aEntity) {
		List<String> vAccessFieldList = new ArrayList<String>();

		// remove current WriteAccess List

		for (WriteAccess aItem : aEntity.getWriteAccessList())
			manager.remove(aItem);
		aEntity.getWriteAccessList().clear();

		// create fieldname list and add the default fieldname
		vAccessFieldList.add("$writeAccess");

		// test if the Resource WRITE_ACCESS_FIELDS is provided
		if (writeAccessFields != null && !"".equals(writeAccessFields)) {
			String[] stringArrayList = writeAccessFields.split(",");
			for (String aentry : stringArrayList) {
				vAccessFieldList.add(aentry);
			}
		}

		// process all attributes
		for (String aentry : vAccessFieldList) {
			// add values. But do not add empty string entries here!
			List vEntryList = itemCol.getItemValue(aentry);
			for (Object avalue : vEntryList) {
				if (avalue != null && !"".equals(avalue.toString())) {
					// now persist and add the new Item
					WriteAccess newItem = new WriteAccess(avalue.toString());
					manager.persist(newItem);
					aEntity.getWriteAccessList().add(newItem);
				}
			}
		}
	}

	/**
	 * This method updates the internal ReadAccessList. Therefore the method
	 * verifies if the itemCollection contains ReadAccess properties. The
	 * default property name is '$readaccess'. Additional property names can be
	 * provided by the resource "READ_ACCESS_FIELDS" through the deployment
	 * descriptor. Empty values will be ignored!
	 * <p>
	 * The Values of the corresponding Items in the ItemCollection will not be
	 * removed by this method because access values could be assigned to
	 * different properties.
	 * 
	 * @return the new itemCollection without writeaccess properties
	 */
	@SuppressWarnings("rawtypes")
	private void updateReadAccessList(ItemCollection itemCol, Entity aEntity) {
		List<String> vAccessFieldList = new ArrayList<String>();

		// remove current ReadAccess List
		for (ReadAccess aItem : aEntity.getReadAccessList())
			manager.remove(aItem);
		aEntity.getReadAccessList().clear();

		// add default fieldname
		vAccessFieldList.add("$readAccess");

		// test if the Resource WRITE_ACCESS_FIELDS is provided
		if (readAccessFields != null && !"".equals(readAccessFields)) {
			String[] stringArrayList = readAccessFields.split(",");
			for (String aentry : stringArrayList) {
				vAccessFieldList.add(aentry);
			}
		}

		// process all attributes
		for (String aentry : vAccessFieldList) {
			// add values. But do not add empty string entries here!
			List vEntryList = itemCol.getItemValue(aentry);
			for (Object avalue : vEntryList) {
				if (avalue != null && !"".equals(avalue.toString())) {
					// now persist and add the new Item
					ReadAccess newItem = new ReadAccess(avalue.toString());
					manager.persist(newItem);
					aEntity.getReadAccessList().add(newItem);
				}
			}
		}
	}

	/**
	 * Explodes an ItemCollection into a Entity with its index fields. This
	 * method extracts all items with corresponding indices into the sub-tables.
	 * Existing Entries will be removed.
	 * 
	 * This method extracts an single property of the ItemCollection (data
	 * object of the activeEntity) into the corresponding index properties
	 * (TextItem, IntegerItem DoubleItem, CalendarItem).
	 * <p>
	 * The ItemName will be transformed to lowercase!
	 * <p>
	 * If an instance of a JavaDate Object is provided in the ItemCollection the
	 * values will be converted from the Date object into a Calendar Object.
	 * This is because the ItemCollection works typical with Date Objects
	 * instead of Calendar Objects.
	 * <p>
	 * After the method has moved an attribute with its values to the
	 * corresponding Index Property the method removes the attribute form the
	 * itemCollection.
	 * <p>
	 * If a value of a property did not match the indexProperty Type a
	 * ClassCastExcepiton will be thrown. This is the situation if one or more
	 * single values did not match the propertyIndex Type. The method prints a
	 * warning to the log file but did continue proceeding. This results into a
	 * situation where invalid values will be stored in the ItemCollections
	 * value list. <br>
	 * In this situation the entity exists in a invalid structure and did not
	 * match the data model
	 * <p>
	 * issue #140: <br>
	 * If the value list of the new entity is equals to the value list of the
	 * existing, than the method did not remove and persist the values. This
	 * will increase performance and avoids problems with duplicated index
	 * values when a entity is saved several times in one transaction (see issue
	 * #140).
	 * 
	 * 
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void explodeEntity(ItemCollection itemCol, Entity aEntity, Collection<EntityIndex> entityIndexCache) {

		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("[EntityService] explodeEntity ID=" + aEntity.getId());
		}

		logger.finest("[EntityService] disableOptimisticLocking=" + disableOptimisticLocking);

		// in case of optimistic locking is disabled we remove $version
		if (disableOptimisticLocking) {
			itemCol.removeItem("$Version");
		} else if (itemCol.hasItem("$Version") && itemCol.getItemValueInteger("$Version") > 0) {
			// if $version is provided we update the version number of the
			// entity!
			int version = itemCol.getItemValueInteger("$Version");
			aEntity.setVersion(version);
			logger.finest("[EntityService] version=" + version);
		}

		// Issue #159
		// verify if deprecated index values are attached to the entity.
		removeDeprectedIndexValues(aEntity, entityIndexCache);

		// For each index we update the relationships only if the valueList
		// changed
		for (EntityIndex index : entityIndexCache) {

			// TEXT_ITEM....
			if (index.getTyp() == EntityIndex.TYP_TEXT) {
				// get the value list from the itemCollection
				List newValueList = itemCol.getItemValue(index.getName());
				// get value list from existing entity
				List<String> oldValueList = new ArrayList<String>();
				List<TextItem> itemList = new ArrayList<TextItem>();
				for (TextItem aItem : aEntity.getTextItems()) {
					if (aItem.itemName.equals(index.getName())) {
						itemList.add(aItem);
						oldValueList.add(aItem.itemValue);
					}
				}

				// equals?
				if (!newValueList.equals(oldValueList)) {
					// no - remove old relationships....
					for (TextItem aItem : itemList) {
						manager.remove(aItem);
						aEntity.getTextItems().remove(aItem);
					}

					// build new relationships
					for (Object asingleValue : newValueList) {
						TextItem newItem = new TextItem(index.getName(), asingleValue.toString());
						manager.persist(newItem);
						aEntity.getTextItems().add(newItem);

						if (logger.isLoggable(Level.FINEST))
							logger.finest(
									"[EntityService] addTextItem: " + index.getName() + "=" + asingleValue.toString());
					}
				}
				// now remove the attribute from ItemCollection
				itemCol.removeItem(index.getName());
				// finally continue....
				continue;
			}

			// INTEGER_ITEM.....
			if (index.getTyp() == EntityIndex.TYP_INT) {
				boolean bClassCastException = false;
				List vInvalidValues = new Vector();
				// get the value list from the itemCollection
				List newValueList = itemCol.getItemValue(index.getName());
				// get value list from existing entity
				List<Integer> oldValueList = new ArrayList<Integer>();
				List<IntegerItem> itemList = new ArrayList<IntegerItem>();
				for (IntegerItem aItem : aEntity.getIntegerItems()) {
					if (aItem.itemName.equals(index.getName())) {
						itemList.add(aItem);
						oldValueList.add(aItem.itemValue);
					}
				}

				// equals?
				if (!newValueList.equals(oldValueList)) {
					// no - remove old relationships....
					for (IntegerItem aItem : itemList) {
						manager.remove(aItem);
						aEntity.getIntegerItems().remove(aItem);
					}
					// build new relationships
					for (Object asingleValue : newValueList) {
						try {
							IntegerItem newItem = new IntegerItem(index.getName(), (Integer) asingleValue);
							manager.persist(newItem);
							aEntity.getIntegerItems().add(newItem);
							if (logger.isLoggable(Level.FINEST))
								logger.finest("[EntityService] addIntegerItem: " + index.getName() + "="
										+ asingleValue.toString());
						} catch (ClassCastException cce) {
							bClassCastException = true;
							logger.warning("explodeEntity - " + index.getName() + " TYP_INT: " + cce.getMessage()
									+ " ID:" + aEntity.getId());
							vInvalidValues.add(asingleValue);
						}

					}
				}
				// now remove the attribute from ItemCollection
				if (!bClassCastException)
					itemCol.removeItem(index.getName());
				else
					itemCol.replaceItemValue(index.getName(), vInvalidValues);
				continue;
			}

			// DOUBLE_ITEM...
			if (index.getTyp() == EntityIndex.TYP_DOUBLE) {
				boolean bClassCastException = false;
				List vInvalidValues = new Vector();
				// get the value list from the itemCollection
				List newValueList = itemCol.getItemValue(index.getName());
				// get value list from existing entity
				List<Double> oldValueList = new ArrayList<Double>();
				List<DoubleItem> itemList = new ArrayList<DoubleItem>();
				for (DoubleItem aItem : aEntity.getDoubleItems()) {
					if (aItem.itemName.equals(index.getName())) {
						itemList.add(aItem);
						oldValueList.add(aItem.itemValue);
					}
				}

				// equals?
				if (!newValueList.equals(oldValueList)) {
					// no - remove old relationships....
					for (DoubleItem aItem : itemList) {
						manager.remove(aItem);
						aEntity.getDoubleItems().remove(aItem);
					}

					// build new relationships
					for (Object asingleValue : newValueList) {
						try {
							DoubleItem newItem = new DoubleItem(index.getName(), (Double) asingleValue);
							manager.persist(newItem);
							aEntity.getDoubleItems().add(newItem);
							if (logger.isLoggable(Level.FINEST))
								logger.finest("[EntityService] addDoubleItem: " + index.getName() + "="
										+ asingleValue.toString());
						} catch (ClassCastException cce) {
							bClassCastException = true;
							logger.warning("explodeEntity - " + index.getName() + " TYP_DOUBLE: " + cce.getMessage()
									+ " ID:" + aEntity.getId());
							vInvalidValues.add(asingleValue);
						}

					}
				}
				// now remove the attribute from ItemCollection
				if (!bClassCastException)
					itemCol.removeItem(index.getName());
				else
					itemCol.replaceItemValue(index.getName(), vInvalidValues);
				continue;

			}

			// CALENDAR_ITEM....
			if (index.getTyp() == EntityIndex.TYP_CALENDAR) {
				boolean bClassCastException = false;
				List vInvalidValues = new Vector();
				// get the value list from the itemCollection
				List newValueList = itemCol.getItemValue(index.getName());
				// get value list from existing entity
				List<Calendar> oldValueList = new ArrayList<Calendar>();
				List<CalendarItem> itemList = new ArrayList<CalendarItem>();
				for (CalendarItem aItem : aEntity.getCalendarItems()) {
					if (aItem.itemName.equals(index.getName())) {
						itemList.add(aItem);
						oldValueList.add(aItem.itemValue);
					}
				}

				// equals?
				if (!newValueList.equals(oldValueList)) {
					// no - remove old relationships....
					for (CalendarItem aItem : itemList) {
						manager.remove(aItem);
						aEntity.getCalendarItems().remove(aItem);
					}

					// build new relationships
					for (Object asingleValue : newValueList) {
						try {
							if (asingleValue instanceof java.util.Date) {
								Calendar cal = Calendar.getInstance();
								cal.setTime((java.util.Date) asingleValue);
								asingleValue = cal;
							}
							CalendarItem newItem = new CalendarItem(index.getName(), (Calendar) asingleValue);

							manager.persist(newItem);
							aEntity.getCalendarItems().add(newItem);
							if (logger.isLoggable(Level.FINEST))
								logger.finest("[EntityService] addCalendarItem: " + index.getName() + "="
										+ asingleValue.toString());
						} catch (ClassCastException cce) {
							bClassCastException = true;
							logger.warning("explodeEntity - " + index.getName() + " TYP_CALENDAR: " + cce.getMessage()
									+ " ID:" + aEntity.getId());
							vInvalidValues.add(asingleValue);
						}

					}
				}
				// now remove the attribute from ItemCollection
				if (!bClassCastException)
					itemCol.removeItem(index.getName());
				else
					itemCol.replaceItemValue(index.getName(), vInvalidValues);
				continue;
			}

			logger.warning(" explodeEntity - " + index.getName() + " Indextype:" + index.getTyp() + " unknown!");

		}

	}

	/**
	 * This method returns a new Instance of an ItemCollection based of an given
	 * Entity object. The method constructs a new ItemCollection instance
	 * containing all values of the Entity data. Properties which where
	 * separated out into the index properties (TextItems, IntegerItems,
	 * DoubleItems, CalendarItems) will be moved back into the ItemCollection
	 * Object returned by this method. So the returned ItemCollection will
	 * contain all attributes managed currently by the activeEntity.
	 * <p>
	 * The method takes care about the fact that the values of the activeEntity
	 * are typical managed by the PersistenceManger. For this reason the method
	 * did not copy the Vector objects into the new detached Instance of the new
	 * ItemCollection but copies only the values of each vector. This mechanism
	 * guarantees that the reconstruction of indexProperties did not affect the
	 * managed activeEntity and its related objects. So it is not necessary that
	 * the activeEntity was detached before this method is called.
	 * <p>
	 * Calendar Objects will be converted into Date Objects. ItemCollection
	 * works typical with Date Objects instead of Calendar Objects.
	 * <p>
	 * The method verifies if the callerPricipal has author access and adds the
	 * attribute $isAuthor into the ItemCollection to indicate the author access
	 * of this entity for the current user.
	 * 
	 * <p>
	 * Note: $readAccess and $writeAccess fields are not removed during save. so
	 * we did not care about those values
	 * <p>
	 * 
	 * This method will also detach the entity from the persistence manager
	 * after all relation ships are resolved and the data property is read.
	 * 
	 * @see explodeEntity()
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private ItemCollection implodeEntity(Entity aEntity) {

		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("[EntityService] implodeEntity ID=" + aEntity.getId());
		}

		// create new empty ItemCollection
		ItemCollection itemCollection = new ItemCollection();
		// verify author access and add property '$isauthor'
		itemCollection.replaceItemValue("$isauthor", isCallerAuthor(aEntity));

		/*
		 * Now we first copy for each IndexProperty the values into our new
		 * ItemColleciton Object.
		 */

		// so now copy all separated TextItems
		for (TextItem textItem : aEntity.getTextItems()) {
			try {
				List vValue = itemCollection.getItemValue(textItem.itemName);
				vValue.add(textItem.itemValue);
				itemCollection.replaceItemValue(textItem.itemName, vValue);
				if (logger.isLoggable(Level.FINEST)) {
					logger.finest("[EntityService] readTextItem: " + textItem.itemName + "=" + textItem.itemValue);
				}
			} catch (Exception e) {
				logger.warning("[EntityService] could not implode ItemValue: " + textItem.itemName);
			}
		}

		// copy all separated IntegerItems
		for (IntegerItem ii : aEntity.getIntegerItems()) {
			try {
				List vValue = itemCollection.getItemValue(ii.itemName);
				vValue.add(ii.itemValue);
				itemCollection.replaceItemValue(ii.itemName, vValue);
				if (logger.isLoggable(Level.FINEST)) {
					logger.finest("[EntityService] readIntegerItem: " + ii.itemName + "=" + ii.itemValue);
				}

			} catch (Exception e) {
				logger.warning("[EntityService] could not implode ItemValue: " + ii.itemName);
			}
		}

		// copy all separated DoubleItems
		for (DoubleItem di : aEntity.getDoubleItems()) {
			try {
				List vValue = itemCollection.getItemValue(di.itemName);
				vValue.add(di.itemValue);
				itemCollection.replaceItemValue(di.itemName, vValue);
				if (logger.isLoggable(Level.FINEST)) {
					logger.finest("[EntityService] readDoubleItem: " + di.itemName + "=" + di.itemValue);
				}

			} catch (Exception e) {
				logger.warning("[EntityService] could not implode ItemValue: " + di.itemName);
			}
		}

		// copy all separated CalendarItems
		for (CalendarItem ci : aEntity.getCalendarItems()) {
			try {
				List vValue = itemCollection.getItemValue(ci.itemName);
				// Calendar Objects will be converted into Date Objects
				vValue.add(ci.itemValue.getTime());
				itemCollection.replaceItemValue(ci.itemName, vValue);
				if (logger.isLoggable(Level.FINEST)) {
					logger.finest("[EntityService] readCalendarItem: " + ci.itemName + "=" + ci.itemValue);
				}

			} catch (Exception e) {
				logger.warning("[EntityService] could not implode ItemValue: " + ci.itemName);
			}
		}

		/*
		 * After we added all indexProperties to our empty ItemCollection we can
		 * now add the rest of the properties contained in the EntityData
		 * Object. But we need to make sure that we do not overwrite a new
		 * created index property. (see explodeEntity - when a item value did
		 * not match the expected index type). So we do not simply call the
		 * putAll() method. But we verify each property name and value.
		 * 
		 * To avoid any manipulation of an object during the addAll method call
		 * we also detach now the entity. So any changes will not be reflected
		 * back to the attached entity. This was a problem before because this
		 * will change the version number for optimistic locking! (see issue
		 * #145)
		 */

		Map activeMap = aEntity.getData();
		// detach the entity
		manager.detach(aEntity);

		// ! do not use :
		// detachedItemCollection.getAllItems().putAll(activeEntity.getData().getItemCollection().getAllItems());
		if (activeMap != null) {
			// iterate over all properties
			Iterator iter = activeMap.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry mapEntry = (Map.Entry) iter.next();
				// extract name and value
				String activePropertyName = mapEntry.getKey().toString();

				// even if we have the property still created (from the index
				// values) we now add any existing values from the mapEntiry
				// (these values did not match the index type - see
				// explodeEntiy)
				List detachePropertyValue = null;
				// check if this property was already created with index values
				if (itemCollection.hasItem(activePropertyName)) {
					logger.warning("implodeEntity - " + activePropertyName + " contains inconsistent entries - "
							+ " ID:" + aEntity.getId());
					detachePropertyValue = itemCollection.getItemValue(activePropertyName);
				} else {
					// we did not have yet created this property from the
					// indexProperties before, so we add now a new property....
					detachePropertyValue = new Vector();
				}
				List activePropertyValue = (List) mapEntry.getValue();

				// copy objects values
				detachePropertyValue.addAll(activePropertyValue);

				// add property
				itemCollection.replaceItemValue(activePropertyName, detachePropertyValue);

				// now we are sure that we have a new instance of an vector
				// for each property
			}
		}

		// if disable Optimistic Locking is TRUE we do not add the version
		// number
		if (disableOptimisticLocking)
			itemCollection.removeItem("$Version");
		else
			itemCollection.replaceItemValue("$Version", aEntity.getVersion());

		return itemCollection;
	}

	/**
	 * This method verify if deprecated index values are attached to the entity.
	 * The method is only called from explodeEntity()!
	 * 
	 * See Issue #159
	 * 
	 * @param aEntity
	 * @param entityIndexCache
	 */
	private void removeDeprectedIndexValues(Entity aEntity, Collection<EntityIndex> entityIndexCache) {

		// build list of current indexNames....
		List<String> indexNames = new ArrayList<String>();
		for (EntityIndex index : entityIndexCache) {
			indexNames.add(index.getName());
		}

		// test for deprecated textValues
		List<TextItem> deprecatedTextItemList = new ArrayList<TextItem>();
		for (TextItem aItem : aEntity.getTextItems()) {
			if (!indexNames.contains(aItem.itemName)) {
				deprecatedTextItemList.add(aItem);
			}
		}
		for (TextItem aItem : deprecatedTextItemList) {
			manager.remove(aItem);
			aEntity.getTextItems().remove(aItem);
			logger.warning("explodeEntity - fixed deprecated TextItem '" + aItem.itemName + "'");
		}

		// test for deprecated integerValues
		List<IntegerItem> deprecatedIntegerItemList = new ArrayList<IntegerItem>();
		for (IntegerItem aItem : aEntity.getIntegerItems()) {
			if (!indexNames.contains(aItem.itemName)) {
				deprecatedIntegerItemList.add(aItem);
			}
		}
		for (IntegerItem aItem : deprecatedIntegerItemList) {
			manager.remove(aItem);
			aEntity.getIntegerItems().remove(aItem);
			logger.warning("explodeEntity - fixed deprecated IntegerItem '" + aItem.itemName + "'");
		}

		// test for deprecated DoubleValues
		List<DoubleItem> deprecatedDoubleItemList = new ArrayList<DoubleItem>();
		for (DoubleItem aItem : aEntity.getDoubleItems()) {
			if (!indexNames.contains(aItem.itemName)) {
				deprecatedDoubleItemList.add(aItem);
			}
		}
		for (DoubleItem aItem : deprecatedDoubleItemList) {
			manager.remove(aItem);
			aEntity.getDoubleItems().remove(aItem);
			logger.warning("explodeEntity - fixed deprecated DoubleItem '" + aItem.itemName + "'");
		}

		// test for deprecated CalendarValues
		List<CalendarItem> deprecatedCalendarItemList = new ArrayList<CalendarItem>();
		for (CalendarItem aItem : aEntity.getCalendarItems()) {
			if (!indexNames.contains(aItem.itemName)) {
				deprecatedCalendarItemList.add(aItem);
			}
		}
		for (CalendarItem aItem : deprecatedCalendarItemList) {
			manager.remove(aItem);
			aEntity.getCalendarItems().remove(aItem);
			logger.warning("explodeEntity - fixed deprecated CalendarItem '" + aItem.itemName + "'");
		}

	}

	/**
	 * This method is used to optimize a query by adding a where clause which
	 * ensures that the query will only include entities accessible by the
	 * current user. For this reason the method adds a clause which test the
	 * readAccess property for predefined roles and usernames.
	 * <p>
	 * This method is most important to secure each findAllEntiteis method call.
	 * <p>
	 * Example
	 * <p>
	 * Here are some Examples:
	 * <ul>
	 * <li>SELECT entity FROM Entity entity
	 * <li>convert into
	 * <li>SELECT entity FROM Entity entity, entity.readAccessList access WHERE
	 * access.value IN ('xxx')
	 * </ul>
	 * 
	 * <ul>
	 * <li>SELECT entity FROM Entity entity WHERE entity.type='workitem'
	 * <li>convert into
	 * <li>SELECT entity FROM Entity entity, entity.readAccessList access WHERE
	 * access.value IN ('rsoika') AND entity.type='workitem'
	 * </ul>
	 * <p>
	 * The Method also verifies if a DISTINCT clause is used in the Query. If
	 * not the method will add a distinct clause to avoid duplicates in the
	 * returned result set. duplicates can be returned by complex queries with
	 * multiple joins.
	 * 
	 * @param aQuery
	 * @return
	 * @throws InvalidAccessException
	 * 
	 */
	private String optimizeQuery(String aQuery) throws InvalidAccessException {
		// String nameList = "";
		StringBuffer nameListBuf = new StringBuffer();
		String nameList = "";
		aQuery = aQuery.trim();
		StringTokenizer st = new StringTokenizer(aQuery);
		// find identifier for Entity
		if (st.countTokens() < 5)
			throw new InvalidAccessException("[EntityService] Invalid query format: " + aQuery);

		// test if DISTINCT clause is included
		st.nextToken();
		String sDistinct = st.nextToken();
		if (!"distinct".equals(sDistinct.toLowerCase().trim())) {
			// add distinct.
			int iDisPos = aQuery.toLowerCase().indexOf("select") + 6;
			aQuery = aQuery.substring(0, iDisPos) + " DISTINCT" + aQuery.substring(iDisPos);
		}

		// don't optimize for managers...
		if (ctx.isCallerInRole(ACCESSLEVEL_MANAGERACCESS))
			return aQuery;

		// now construct user role list
		List<String> auserNameList = _getUserNameList();
		for (String auserName : auserNameList) {
			nameListBuf.append(",'" + auserName + "'");
		}
		// remove first ,
		nameListBuf.deleteCharAt(0);

		nameList = nameListBuf.toString();

		logger.fine("Optimized NameList = " + nameList);

		// now select identifier - this is the last word before a 'WHERE',
		// 'ORDER BY' or 'JOIN'
		int iPos = 0;
		// we begin with join
		iPos = aQuery.toLowerCase().indexOf("join");
		if (iPos == -1)
			// test for where
			iPos = aQuery.toLowerCase().indexOf("where");
		if (iPos == -1)
			// and end to test for order by clause
			iPos = aQuery.toLowerCase().indexOf("order by");

		String firstPart;
		if (iPos == -1)
			firstPart = aQuery;
		else
			firstPart = aQuery.substring(0, iPos - 1);

		firstPart = firstPart.trim();

		iPos = firstPart.length();
		// go back to first ' '
		String identifier = null;
		identifier = firstPart.substring(firstPart.lastIndexOf(' ')).trim();

		// test for ,
		if (identifier.endsWith(","))
			identifier = identifier.substring(0, identifier.length() - 1);

		// add access JOIN (should be a unique token name)
		String aNewQuery = aQuery.substring(0, iPos);
		aNewQuery += " LEFT JOIN " + identifier + ".readAccessList access807 ";
		aNewQuery += aQuery.substring(iPos);

		aQuery = aNewQuery.trim();
		// test if WHERE clause is available
		int iWherePos = aQuery.toLowerCase().indexOf("where");
		if (iWherePos > -1) {
			// insert access check
			aNewQuery = aQuery.substring(0, iWherePos + 5) + " (access807.value IS NULL OR access807.value IN ("
					+ nameList + ")) AND " + aQuery.substring(iWherePos + 6);
			aQuery = aNewQuery;

		} else {
			// no WHERE clause - so add a new one
			int iOrderPos = aQuery.toLowerCase().indexOf("order by");
			if (iOrderPos > -1)
				aNewQuery = aQuery.substring(0, iOrderPos - 1)
						+ " WHERE (access807.value IS NULL OR access807.value IN (" + nameList + ")) "
						+ aQuery.substring(iOrderPos);
			else
				aNewQuery = aQuery + " WHERE (access807.value IS NULL OR access807.value IN(" + nameList + ")) ";

			aQuery = aNewQuery;
		}

		logger.fine("Optimized Query=" + aQuery);
		return aQuery;
	}

	/**
	 * This Method updates the index properties from a collection of entities.
	 * The Method first implodes the Entity to get all external properties and
	 * than calls an explode to recreate the index properties.
	 * 
	 * 
	 * Method no longer used
	 * 
	 * 
	 * @see issue #94
	 */
	@SuppressWarnings("unused")
	@Deprecated
	private void updateAllEntityIndexFields(Collection<Entity> entityList, String newIndexField) {
		long count = 0;
		logger.info("[EntityServiceBean] found " + entityList.size() + " existing entities. Starting update...");
		// get a List of all existing Indices
		Collection<EntityIndex> entityIndexCache = readIndices();

		Iterator<Entity> iter = entityList.iterator();
		while (iter.hasNext()) {
			Entity activeEntity = iter.next();
			ItemCollection slimItemcol = new ItemCollection(activeEntity.getData());
			// test if activeEntity is affected from this index
			if (newIndexField == null || slimItemcol.hasItem(newIndexField)) {

				try {
					/*
					 * try { manager.refresh(activeEntity); } catch
					 * (EntityNotFoundException ex) { logger.warning(
					 * "[EntityServiceBean] #issue 102 - refresh() EntityNotFoundException"
					 * ); activeEntity = null; }
					 */

					// implode each Entity into its ItemCollection
					ItemCollection itemcol = implodeEntity(activeEntity);

					ItemCollection slimItemCollection = new ItemCollection(itemcol.getAllItems());

					// update read- and writeAccess List
					updateReadAccessList(slimItemCollection, activeEntity);
					updateWriteAccessList(slimItemCollection, activeEntity);

					explodeEntity(slimItemCollection, activeEntity, entityIndexCache);

					// finally update the data field
					activeEntity.setData(slimItemCollection.getAllItems());

				} catch (Exception merex) {
					logger.info("[EntityServiceBean] Error updateAllEntityIndexFields for Entity : "
							+ activeEntity.getId());
					merex.printStackTrace();

				}
				count++;
			}
		}
		logger.info("[EntityServiceBean] " + count + " effective updates");
		logger.info("[EntityServiceBean] index update completed");

	}

}
