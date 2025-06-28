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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Stores the result of a Facets search. 
 * <p>
 * It includes a map with all labels within a specific category. 
 * 
 * @version 1.0
 */ 
public class Category {
    
    private String name;
    private int count;   
    Map<String,Integer> labels;
    
    /**
     * Creates a Category
     * 
     * @param name      Name of category
     * @param count     count
     */
    public Category(String name, int count) {
        this.name = name;
        this.count = count;
        labels=new HashMap<String,Integer>();
    }


    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }


    public int getCount() {
        return count;
    }


    public void setCount(int count) {
        this.count = count;
    }
    
    public Map<String,Integer> getLabels() {
    	return labels;
    }
    
    public void setLabel(String label, int count) {
    	labels.put(label, count);
    }
    
    public int getCount(String label) {
    	return labels.get(label);
    }


    @Override
    public int hashCode() {
        return Objects.hash(count, name);
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Category other = (Category) obj;
        return count == other.count && Objects.equals(name, other.name);
    }

   
    
}
