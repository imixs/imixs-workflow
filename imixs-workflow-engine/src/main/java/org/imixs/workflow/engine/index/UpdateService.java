/*******************************************************************************
 * <pre>
 *  Imixs Workflow 
 *  Copyright (C) 2001-2020 Imixs Software Solutions GmbH,  
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
 *      https://www.imixs.org
 *      https://github.com/imixs/imixs-workflow
 *  
 *  Contributors:  
 *      Imixs Software Solutions GmbH - initial API and implementation
 *      Ralph Soika - Software Developer
 * </pre>
 *******************************************************************************/

package org.imixs.workflow.engine.index;

import java.util.Arrays;
import java.util.List;
import javax.ejb.Local;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.IndexException;

/**
 * The UpdateService defines methods to update the search index. These methods are called by the
 * DocuentService.
 * <p>
 * The method updateIndex(documents) writes documents immediately into the index.
 * <p>
 * The method updateIndex() updates the search index based on the eventLog.
 * <p>
 * The UpdateService provides also the default index schema.
 * 
 * @see SchemaService
 * @version 1.0
 * @author rsoika
 */
@Local
public interface UpdateService {

  // default field lists
  public static List<String> DEFAULT_SEARCH_FIELD_LIST =
      Arrays.asList("$workflowsummary", "$workflowabstract");
  public static List<String> DEFAULT_NOANALYSE_FIELD_LIST =
      Arrays.asList("$modelversion", "$taskid", "$processid", "$workitemid", "$uniqueidref", "type",
          "$writeaccess", "$modified", "$created", "namcreator", "$creator", "$editor",
          "$lasteditor", "$workflowgroup", "$workflowstatus", "txtworkflowgroup", "name", "txtname",
          "$owner", "namowner", "txtworkitemref", "$uniqueidsource", "$uniqueidversions",
          "$lasttask", "$lastevent", "$lasteventdate");
  public static List<String> DEFAULT_STORE_FIELD_LIST =
      Arrays.asList("type", "$taskid", "$writeaccess", "$workflowsummary", "$workflowabstract",
          "$workflowgroup", "$workflowstatus", "$modified", "$created", "$lasteventdate",
          "$creator", "$editor", "$lasteditor", "$owner", "namowner");

  /**
   * This method adds a collection of documents to the index. The documents are added immediately to
   * the index. Calling this method within a running transaction leads to a uncommitted reads in the
   * index. For transaction control, it is recommended to use instead the the method
   * documentService.addDocumentToIndex() which takes care of uncommitted reads.
   * <p>
   * This method is used by the JobHandlerRebuildIndex only.
   * 
   * @param documents of ItemCollections to be indexed
   * @throws IndexException
   */
  public void updateIndex(List<ItemCollection> documents);

  /**
   * This method updates the search index based on the eventLog. Documents are added by the
   * DocumentService as events to the EventLogService. This ensures that only committed documents
   * are added into the index.
   * 
   * @see DocumentService
   */
  public void updateIndex();
}
