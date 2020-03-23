/*  
 *  Imixs-Workflow 
 *  
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
 *      Imixs Software Solutions GmbH - Project Management
 *      Ralph Soika - Software Developer
 */

package org.imixs.workflow.engine;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The UserGroupEvent provides a CDI observer pattern. The UserGroupEvent is
 * fired by the DocumentService EJB. An event Observer can react on this event
 * to extend the current user group list. The user group list is used to grant
 * read and write access on a document entity.
 * 
 * 
 * @author Ralph Soika
 * @version 1.1
 * @see org.imixs.workflow.engine.DocumentService
 */
public class UserGroupEvent {

    private String userId;
    private Set<String> groups;

    public UserGroupEvent(String userId) {
        super();
        this.userId = userId;
        this.groups = new LinkedHashSet<String>();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * This method adds a new list of groups to the current group list. Multiple
     * observers can add different groups independently from each other. Duplicates
     * are removed.
     * <p>
     * To reset the current group list a observer can call the method reset.
     * 
     * @param groups
     */
    public void setGroups(List<String> groups) {
        // see issue #654
        this.groups.addAll(groups);
    }

    /**
     * Returns the current list of groups
     * 
     * @return
     */
    public Set<String> getGroups() {
        return groups;
    }

    /**
     * This method empties the current group list
     */
    public void reset() {
        this.groups = new LinkedHashSet<String>();
    }
}
