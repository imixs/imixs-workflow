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

package org.imixs.workflow.jaxrs;

import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * This Utility class provides methods to convert Item Values in Rest API calls.
 */
public class RestAPIUtil {

    /**
     * This method returns a List object from a given comma separated string. The
     * method returns null if no elements are found. The provided parameter looks
     * typical like this: <code>
     *   txtWorkflowStatus,numProcessID,txtName
     * </code>
     * 
     * @param items
     * @return
     */
    public static List<String> getItemList(String items) {
        if (items == null || "".equals(items))
            return null;
        Vector<String> v = new Vector<String>();
        StringTokenizer st = new StringTokenizer(items, ",");
        while (st.hasMoreTokens())
            v.add(st.nextToken());
        return v;
    }
}
