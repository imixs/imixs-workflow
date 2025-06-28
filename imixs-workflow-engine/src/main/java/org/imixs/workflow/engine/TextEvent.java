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

import java.util.ArrayList;
import java.util.List;
import org.imixs.workflow.ItemCollection;

/**
 * The TextEvent provides a CDI observer pattern. The TextEvent is fired by the
 * WorkflowService EJB to adapt a text fragment. An event observer can adapt the
 * text fragment in a given document context.
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
