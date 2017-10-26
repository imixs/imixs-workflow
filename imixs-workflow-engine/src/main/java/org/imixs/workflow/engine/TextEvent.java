package org.imixs.workflow.engine;

import org.imixs.workflow.ItemCollection;

/**
 * The TextEvent provides a CDI observer pattern. The TextEvent is fired
 * by the WorkflowService EJB to adapt a text fragment. An event observer can adapt the text fragmetn in a given document context.
 * 
 * @author Ralph Soika
 * @version 1.0
 * @see org.imixs.workflow.engine.WorkflowService
 */
public class TextEvent {

	private ItemCollection document;
	private String text;

	public TextEvent(String text,ItemCollection document) {
		this.text = text;
		this.document = document;
	}


	public ItemCollection getDocument() {
		return document;
	}


	public String getText() {
		return text;
	}


	public void setText(String text) {
		this.text = text;
	}

}
