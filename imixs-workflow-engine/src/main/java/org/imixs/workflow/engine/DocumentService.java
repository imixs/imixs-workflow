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

import java.io.IOException;
import java.util.List;

import javax.ejb.Local;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.imixs.workflow.ItemCollection;
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
@Local
public interface DocumentService {

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

	public static final String USER_GROUP_LIST = "org.imixs.USER.GROUPLIST";


	public static final String OPERATION_NOTALLOWED = "OPERATION_NOTALLOWED";
	public static final String INVALID_PARAMETER = "INVALID_PARAMETER";
	public static final String INVALID_UNIQUEID = "INVALID_UNIQUEID";



	/**
	 * Returns a comma separated list of additional Access-Roles defined for this
	 * service
	 * 
	 * @return
	 */
	public String getAccessRoles();

	public void setAccessRoles(String accessRoles);

	/**
	 * This method returns a list of user names, roles and application groups the
	 * user belongs to.
	 * 
	 * @return
	 */
	public List<String> getUserNameList();

	/**
	 * This method returns true, if at least one element of the current UserNameList
	 * is contained in a given name list. The comparison is case sensitive!
	 * 
	 * @param nameList
	 * @return
	 */
	public boolean isUserContained(List<String> nameList);
	/**
	 * Test if the caller has a given security role.
	 * 
	 * @param rolename
	 * @return true if user is in role
	 */
	public boolean isUserInRole(String rolename);

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
	public ItemCollection save(ItemCollection document) throws AccessDeniedException;
	/**
	 * This method saves a workitem in a new transaction. The method can be used by
	 * plugins to isolate a save request from the current transaction context.
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
	public ItemCollection saveByNewTransaction(ItemCollection itemcol) throws AccessDeniedException;

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
	public ItemCollection load(String id);

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
	public void remove(ItemCollection itemcol) throws AccessDeniedException;
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
	public int count(String searchTerm) throws QueryException;
	
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
	public int count(String sSearchTerm, int maxResult) throws QueryException;

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
	public int countPages(String searchTerm, int pageSize) throws QueryException;

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
	public List<ItemCollection> find(String searchTerm, int pageSize, int pageIndex) throws QueryException;
	
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
			throws QueryException;
	
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
	public List<ItemCollection> findDocumentsByRef(String uniqueIdRef, int start, int count);

	/**
	 * Returns an unordered list of all documents of a specific type. The method
	 * throws an InvalidAccessException in case no type attribute is defined.
	 * 
	 * @param type
	 * @return
	 * @throws InvalidAccessException
	 */
	public List<ItemCollection> getDocumentsByType(String type);

	/**
	 * Returns all documents of by JPQL statement
	 * 
	 * @param query
	 *            - JPQL statement
	 * @return
	 * 
	 */
	public List<ItemCollection> getDocumentsByQuery(String query);

	
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
	public List<ItemCollection> getDocumentsByQuery(String query, int maxResult);
	
	
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
	public List<ItemCollection> getDocumentsByQuery(String query, int firstResult, int maxResult);

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
	public void backup(String query, String filePath) throws IOException, QueryException;

	/**
	 * This method restores a backup from the file system and imports the Documents
	 * into the database.
	 * 
	 * @param filepath
	 * @throws IOException
	 */
	public void restore(String filePath) throws IOException;

}
