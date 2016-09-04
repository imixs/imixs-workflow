package org.imixs.workflow.faces.fileupload;

/**
 * This class represents an uploaded file object
 * 
 * The class was developed initially by theironicprogrammer@gmail.com
 * 
 * @author theironicprogrammer@gmail.com, rsoika
 * @see http://ironicprogrammer.blogspot.de/2010/03/file-upload-in-jsf2.html
 */
public class FileData {

	private String name;
	private String contentType;
	private byte[] data;

	public FileData(String fileName, String contentType, byte[] fileData) {
		this.name = fileName;
		this.contentType = contentType;
		this.data = fileData;
	}

	public String getName() {
		return name;
	}

	public String getContentType() {
		return contentType;
	}

	public byte[] getData() {
		return data;
	}

	public long getSize() {
		return data.length;
	}

	public void setName(String fileName) {
		this.name = fileName;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public void setData(byte[] fileData) {
		this.data = fileData;
	}
}