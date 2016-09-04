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

import java.util.List;
import java.util.Map;

import javax.ejb.Remote;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.InvalidAccessException;

/**
 * The Remote interface for the EntityService EJB. Can be used by standalone java clients.
 * 
 * @see org.imixs.workflow.jee.ejb.EntityService
 * @author rsoika
 * 
 */
@Remote
public interface EntityServiceRemote {

	/**
	 * Returns additional AccessRoles defined for the EJB instance
	 * 
	 * @return
	 */
	public String getAccessRoles();
	
	public void setAccessRoles(String accessRoles);


	/**
	 * Returns additional ReadAccessFields defined for the EJB instance.
	 * Default=$ReadAccess
	 * 
	 * @return
	 */
	public String getReadAccessFields();
	
	public void setReadAccessFields(String readAccessFields);
	

	/**
	 * Returns additional WriteAccessFields defined for the EJB instance.
	 * Default=$WriteAccess
	 * 
	 * @return
	 */
	public String getWriteAccessFields();
	
	public void setWriteAccessFields(String writeAccessFields);
	
	/**
	 * returns the disable optimistic locking status
	 * 
	 * @return - true if optimistic locking is disabled
	 */
	public void setDisableOptimisticLocking(Boolean disableOptimisticLocking);
	
	public Boolean getDisableOptimisticLocking();

	/**
	 * This Method saves an ItemCollection into a database. If the
	 * ItemCollection is saved the first time the method generates a uniqueID
	 * ('$uniqueid') which can be used to identify the ItemCollection by its ID.
	 * If the ItemCollection was saved before the method updates the
	 * ItemCollection stored in the database. The Method returns an updated
	 * instance of the ItemCollection containing the attributes $modified,
	 * $created, and $uniqueid
	 * <p>
	 * The method throws an AccessDeniedException if the CallerPrincipal is not
	 * allowed to save or update the ItemCollection in the database. The
	 * CallerPrincipial should have at least the access Role
	 * org.imixs.ACCESSLEVEL.AUTHORACCESS
	 * 
	 * @param ItemCollection
	 *            to be saved
	 * @return updated ItemCollection
	 */
	public ItemCollection _save(ItemCollection itemcol)
			throws AccessDeniedException;

	/**
	 * This method loads an ItemCollection from the Database. The method expects
	 * a valid $unqiueID to identify the ItemCollection saved before into the
	 * database. The method returns null if no ItemCollection with the
	 * corresponding ID exists.
	 * <p>
	 * The method checks also if the CallerPrincipal has read access to
	 * ItemCollection stored in the database. If not the method returns null.
	 * The method dose not throw an AccessDeniedException if the user is not
	 * allowed to read the entity to prevent a aggressor with informations about
	 * the existence of that specific ItemCollection.
	 * <p>
	 * CallerPrincipial should have at least the access Role
	 * org.imixs.ACCESSLEVEL.READACCESS
	 * 
	 * @param id
	 *            the $unqiueid of the ItemCollection to be loaded
	 * @return ItemCollection object or null if the ItemColleciton dose not
	 *         exist or the CallerPrincipal hat insufficient read access.
	 * 
	 */
	public ItemCollection _load(String id);

	/**
	 * This method removes an ItemCollection from the database. If the
	 * CallerPrincipal is not allowed to access the ItemColleciton the method
	 * throws an AccessDeniedException.
	 * <p>
	 * The CallerPrincipial should have at least the access Role
	 * org.imixs.ACCESSLEVEL.AUTHORACCESS
	 * 
	 * @param ItemCollection
	 *            to be removed
	 * @throws AccessDeniedException
	 */
	public void _remove(ItemCollection itemcol) throws AccessDeniedException;

	/**
	 * Adds an EntityIndex to the current list of external properties. A
	 * EntityIndex defines a Index to a specific Attribute provided by
	 * ItemCollections saved to the database. The method throws an
	 * AccessDeniedException if the CallerPrinciapal is not in the role
	 * org.imixs.ACCESSLEVEL.MANAGERACCESS.
	 * 
	 * @param stitel
	 * @param ityp
	 *            - Type of EntityIndex
	 * @throws AccessDeniedException
	 */
	public void addIndex(String stitel, int ityp) throws AccessDeniedException;

	public Map<String,Integer>  getIndices();
	
	
	/**
	 * Removes an EntityIndex from the current list of external properties. The
	 * method throws an AccessDeniedException if the CallerPrinciapal is not in
	 * the role org.imixs.ACCESSLEVEL.MANAGERACCESS.
	 * 
	 * @param stitel
	 * @throws AccessDeniedException
	 */
	public void removeIndex(String stitel) throws AccessDeniedException;

	/**
	 * The method returns a collection of ItemCollections. The method expects an
	 * valid EQL statement. The method returns only ItemCollections which are
	 * readable by the CallerPrincipal. With the startpos and count parameters
	 * it is possible to read chunks of entities.
	 * 
	 * @param query
	 * @param startpos
	 * @param count
	 * @return
	 */
	public List<ItemCollection> _findAllEntities(String query,
			int startpos, int count) throws InvalidAccessException;

	/**
	 * The method returns the parent ItemCollection to a given ItemCollection. A
	 * parent entity is referenced by an other entity by the property
	 * $uniqueidRef which points to a parent entity.
	 * 
	 * @param childentity
	 * @return parent entity
	 */
	public ItemCollection findParentEntity(ItemCollection entity)
			throws InvalidAccessException;

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
	 */
	public List<ItemCollection> findChildEntities(ItemCollection entity,
			int startpos, int count) throws InvalidAccessException;

}
