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
