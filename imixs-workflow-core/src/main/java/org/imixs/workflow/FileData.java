package org.imixs.workflow;

/**
 * Helper class to abstract the file content stored in a ItemCollection.
 * 
 * @see ItemCollection.addFile
 * 
 * @author rsoika
 * @version 1.0
 */
public class FileData {
	String name;
	byte[] content;
	String contentType;

	public FileData(String name, byte[] content, String contentType) {
		super();
		this.content = content;
		this.name = name;
		this.contentType = contentType;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public byte[] getContent() {
		return content;
	}

	public void setContent(byte[] content) {
		this.content = content;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

}
