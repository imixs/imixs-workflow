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

package org.imixs.workflow.xml;

import java.util.Comparator;

/**
 * The XMLItemComparator provides a Comparator for XMLItems contained by a XMLItemCollection.
 * <p>
 * Usage:
 * <p>
 * <code>Collections.sort(collection, new XMLItemComparator());</code>
 * 
 * @author rsoika
 * 
 */
public class XMLItemComparator implements Comparator<XMLItem> {

  @Override
  public int compare(XMLItem o1, XMLItem o2) {
    return o1.getName().compareTo(o2.getName());
  }


}
