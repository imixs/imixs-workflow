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

package org.imixs.workflow.engine.index;

import java.util.Arrays;
import java.util.List;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.exceptions.IndexException;

import jakarta.ejb.Stateless;

/**
 * The UpdateService defines methods to update the search index. These methods
 * are called by the DocuentService.
 * <p>
 * The method updateIndex(documents) writes documents immediately into the
 * index.
 * <p>
 * The method updateIndex() updates the search index based on the eventLog.
 * <p>
 * The UpdateService provides also the default index schema.
 * 
 * @see SchemaService
 * @version 1.0
 * @author rsoika
 */
//@Stateless
public interface UpdateService {

    // default field lists
    /*
    public static List<String> DEFAULT_SEARCH_FIELD_LIST = Arrays.asList("$workflowsummary", "$workflowabstract");
    public static List<String> DEFAULT_NOANALYSE_FIELD_LIST = Arrays.asList("$modelversion", "$taskid", "$processid",
            "$workitemid", "$uniqueidref", "type", "$writeaccess", "$modified", "$created", "namcreator", "$creator",
            "$editor", "$lasteditor", "$workflowgroup", "$workflowstatus", "txtworkflowgroup", "name", "txtname",
            "$owner", "namowner", "$workitemref", "txtworkitemref", "$uniqueidsource", "$uniqueidversions", "$lasttask", "$lastevent",
            "$lasteventdate");
    public static List<String> DEFAULT_STORE_FIELD_LIST = Arrays.asList("type", "$taskid", "$writeaccess",
            "$workflowsummary", "$workflowabstract", "$workflowgroup", "$workflowstatus", "$modified", "$created",
            "$lasteventdate", "$creator", "$editor", "$lasteditor", "$owner", "namowner");
    
    
    
    public static List<String> DEFAULT_CATEGORY_FIELD_LIST = Arrays.asList("type", "$taskid","$workflowgroup", "$workflowstatus"
            , "$creator", "$editor",  "$owner");
    */

    /**
     * This method adds a collection of documents to the index. The documents are
     * added immediately to the index. Calling this method within a running
     * transaction leads to a uncommitted reads in the index. For transaction
     * control, it is recommended to use instead the the method
     * documentService.addDocumentToIndex() which takes care of uncommitted reads.
     * <p>
     * This method is used by the JobHandlerRebuildIndex only.
     * 
     * @param documents of ItemCollections to be indexed
     * @throws IndexException
     */
    public void updateIndex(List<ItemCollection> documents);

    /**
     * This method updates the search index based on the eventLog. Documents are
     * added by the DocumentService as events to the EventLogService. This ensures
     * that only committed documents are added into the index.
     * 
     * @see DocumentService
     */
    public void updateIndex();
}
