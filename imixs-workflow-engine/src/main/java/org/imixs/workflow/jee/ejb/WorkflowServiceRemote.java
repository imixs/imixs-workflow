package org.imixs.workflow.jee.ejb;

import java.util.List;

import javax.ejb.Remote;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Model;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;

@Remote
public interface WorkflowServiceRemote {

	/**
	 * This method loads a Workitem with the corresponding uniqueid
	 */
	public abstract ItemCollection getWorkItem(String uniqueid);
 
	/**
	 * Returns a collection of workItems belonging to a specified username. The
	 * name is a username or role contained in the $WriteAccess attribute of the
	 * workItem.
	 * 
	 * The method returns only workitems the call has sufficient read access
	 * for.
	 * 
	 * @param name
	 *            = username or role contained in $writeAccess - if null current
	 *            username will be used
	 * @param startpos
	 *            = optional start position
	 * @param count
	 *            = optional count - default = -1
	 * @param type
	 *            = defines the type property of the workitems to be returnd.
	 *            can be null
	 * @param sortorder
	 *            = defines sortorder (SORT_ORDER_CREATED_DESC = 0
	 *            SORT_ORDER_CREATED_ASC = 1 SORT_ORDER_MODIFIED_DESC = 2
	 *            SORT_ORDER_MODIFIED_ASC = 3)
	 * @return List of workitems
	 * 
	 */
	public abstract List<ItemCollection> getWorkListByAuthor(String name, int startpos, int count, String type,
			int sortorder);

	/**
	 * Returns a collection of workitems created by a specified user
	 * (namCreator). The behaivor is simmilar to the method getWorkList.
	 * 
	 * 
	 * @param name
	 *            = username for property namCreator - if null current username
	 *            will be used
	 * @param startpos
	 *            = optional start position
	 * @param count
	 *            = optional count - default = -1
	 * @param type
	 *            = defines the type property of the workitems to be returnd.
	 *            can be null
	 * @param sortorder
	 *            = defines sortorder (SORT_ORDER_CREATED_DESC = 0
	 *            SORT_ORDER_CREATED_ASC = 1 SORT_ORDER_MODIFIED_DESC = 2
	 *            SORT_ORDER_MODIFIED_ASC = 3)
	 * @return List of workitems
	 * 
	 */
	public abstract List<ItemCollection> getWorkListByCreator(String name, int startpos, int count, String type,
			int sortorder);

	/**
	 * Returns a collection of workitems containing a namOwner property
	 * belonging to a specified username. The namOwner property is typical
	 * controled by the OwnerPlugin
	 * 
	 * @param name
	 *            = username for property namOwner - if null current username
	 *            will be used
	 * @param startpos
	 *            = optional start position
	 * @param count
	 *            = optional count - default = -1
	 * @param type
	 *            = defines the type property of the workitems to be returnd.
	 *            can be null
	 * @param sortorder
	 *            = defines sortorder (SORT_ORDER_CREATED_DESC = 0
	 *            SORT_ORDER_CREATED_ASC = 1 SORT_ORDER_MODIFIED_DESC = 2
	 *            SORT_ORDER_MODIFIED_ASC = 3)
	 * @return List of workitems
	 * 
	 */
	public abstract List<ItemCollection> getWorkListByOwner(String name, int startpos, int count, String type,
			int sortorder);

	/**
	 * Returns a collection of workitems where the current user has a
	 * writeAccess. This means the either the username or one of the userroles
	 * is contained in the $writeaccess property
	 * 
	 * 
	 * @param startpos
	 *            = optional start position
	 * @param count
	 *            = optional count - default = -1
	 * @param type
	 *            = defines the type property of the workitems to be returnd.
	 *            can be null
	 * @param sortorder
	 *            = defines sortorder (SORT_ORDER_CREATED_DESC = 0
	 *            SORT_ORDER_CREATED_ASC = 1 SORT_ORDER_MODIFIED_DESC = 2
	 *            SORT_ORDER_MODIFIED_ASC = 3)
	 * @return List of workitems
	 * 
	 */
	public abstract List<ItemCollection> getWorkListByWriteAccess(int startpos, int count, String type, int sortorder);

	public abstract List<ItemCollection> getWorkListByGroup(String name, int startpos, int count, String type,
			int sortorder);

	/**
	 * Returns a collection of workitems belonging to a specified $processID
	 * defined by the workflow model. The behaivor is simmilar to the method
	 * getWorkList.
	 * 
	 * @param aID
	 *            = $ProcessID for the workitems to be returned.
	 * @param startpos
	 *            = optional start position
	 * @param count
	 *            = optional count - default = -1
	 * @param type
	 *            = defines the type property of the workitems to be returnd.
	 *            can be null
	 * @param sortorder
	 *            = defines sortorder (SORT_ORDER_CREATED_DESC = 0
	 *            SORT_ORDER_CREATED_ASC = 1 SORT_ORDER_MODIFIED_DESC = 2
	 *            SORT_ORDER_MODIFIED_ASC = 3)
	 * @return List of workitems
	 * 
	 */
	public abstract List<ItemCollection> getWorkListByProcessID(int aid, int startpos, int count, String type,
			int sortorder);

	/**
	 * Returns a collection of workitems belonging to a specified workitem
	 * identified by the attribute $UniqueIDRef.
	 * 
	 * The behaivor of this Mehtod is simmilar to the method getWorkList.
	 * 
	 * @param aref
	 *            A unique reference to another workitem inside a database *
	 * @param startpos
	 *            = optional start position
	 * @param count
	 *            = optional count - default = -1
	 * @param type
	 *            = defines the type property of the workitems to be returnd.
	 *            can be null
	 * @param sortorder
	 *            = defines sortorder (SORT_ORDER_CREATED_DESC = 0
	 *            SORT_ORDER_CREATED_ASC = 1 SORT_ORDER_MODIFIED_DESC = 2
	 *            SORT_ORDER_MODIFIED_ASC = 3)
	 * @return List of workitems
	 */
	public abstract List<ItemCollection> getWorkListByRef(String aref, int startpos, int count, String type,
			int sortorder);
	

	/**
	 * Returns a collection of all workitems belonging to a specified workitem
	 * identified by the attribute $UniqueIDRef.
	 * 
	 * @return List of workitems
	 */
	public abstract List<ItemCollection> getWorkListByRef(String aref);

	/**
	 * This returns a list of workflow events assigned to a given workitem. The
	 * method evaluates the events for the current $modelversion and $processid.
	 * The result list is filtered by the properties 'keypublicresult' and
	 * 'keyRestrictedVisibility'
	 * 
	 * @see imixs-bpmn
	 * @param workitem
	 * @return
	 * @exception ModelException 
	 */
	public abstract List<ItemCollection> getEvents(ItemCollection workitem) throws ModelException;

	/**
	 * processes a workItem. The workitem have to provide the properties
	 * '$modelversion', '$processid' and '$activityid'
	 * 
	 * The method try to load the current instance of the given workitem and
	 * compares the property $processID. If it is not equal the method throws an
	 * ProcessingErrorException.
	 * 
	 * @param workitem
	 *            - the workItem to be processed
	 * @return updated version of the processed workItem
	 * @throws AccessDeniedException
	 *             - thrown if the user has insufficient access to update the
	 *             workItem
	 * @throws ProcessingErrorException
	 *             - thrown if the workitem could not be processed by the
	 *             workflowKernel
	 * @throws PluginException
	 *             - thrown if processing by a plugin fails
	 */
	public abstract ItemCollection processWorkItem(ItemCollection workitem)
			throws AccessDeniedException, ProcessingErrorException, PluginException;

	public abstract void removeWorkItem(ItemCollection aworkitem) throws AccessDeniedException;

	/**
	 * This Method returns the modelManager Instance. The current ModelVersion
	 * is automatically updated during the Method updateProfileEntity which is
	 * called from the processWorktiem method.
	 * 
	 */
	public abstract Model getModel();

	/**
	 * This method returns an instance of the Imixs JEE ModelService used by the
	 * WorkflowManager Implementation. The method can be used to access the
	 * ModelService during a Plugin call.
	 * 
	 * @return EntityService
	 */
	public abstract ModelService getModelService();

	/**
	 * Obtain the java.security.Principal that identifies the caller and returns
	 * the name of this principal.
	 * 
	 * @return the user name
	 */
	public abstract String getUserName();

	/**
	 * Test if the caller has a given security role.
	 * 
	 * @param rolename
	 * @return true if user is in role
	 */
	public abstract boolean isUserInRole(String rolename);

	/**
	 * This method returns a list of user names, roles and application groups
	 * the caller belongs to.
	 * 
	 * @return
	 */
	public abstract List<String> getUserNameList();

}