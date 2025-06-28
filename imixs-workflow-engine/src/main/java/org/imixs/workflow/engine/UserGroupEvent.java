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
