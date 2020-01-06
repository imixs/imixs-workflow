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

package org.imixs.workflow.engine;

import java.util.ArrayList;
import java.util.List;
import org.imixs.workflow.ItemCollection;

/**
 * The TextEvent provides a CDI observer pattern. The TextEvent is fired by the WorkflowService EJB
 * to adapt a text fragment. An event observer can adapt the text fragment in a given document
 * context.
 * 
 * @author Ralph Soika
 * @version 1.0
 * @see org.imixs.workflow.engine.WorkflowService
 */
public class TextEvent {

  private ItemCollection document;
  private String text;
  private List<String> textList;

  public TextEvent(String text, ItemCollection document) {
    this.text = text;
    this.document = document;
  }

  public ItemCollection getDocument() {
    return document;
  }

  public String getText() {
    // In case we have a textlist return the first entry
    if (text == null && textList != null && textList.size() > 0) {
      text = textList.get(0);
    }
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public List<String> getTextList() {
    // In case we have no textlist return temp list
    if (textList == null && text != null) {
      textList = new ArrayList<String>();
      textList.add(text);
    }

    return textList;
  }

  public void setTextList(List<String> textList) {

    this.textList = textList;
  }

}
