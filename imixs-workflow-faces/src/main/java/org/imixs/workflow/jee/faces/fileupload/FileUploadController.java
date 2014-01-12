package org.imixs.workflow.jee.faces.fileupload;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.imixs.workflow.ItemCollection;

/**
 * This controller class provides methods to extract a file from a mulitpart
 * http request.
 * 
 * The controller also provides build-in functionality to store the uploaded
 * files into a ItemCollection. A client can set a target WorkItem by calling
 * setWorkitem(). If the WorkItem contains a property "$BlobWorkitem" the
 * controller loads or creates an attached blobWorkitem. The property
 * "$BlobWorkitem" contains than the $UniqueID of the blobWorkItem.
 * 
 * The list of all currently uploaded files will be stored into the property
 * $file. In case a BlobWorkitem is used the file content of this property will
 * be empty!
 * 
 * The FileUploadController implements a lazy loading mechanism. The
 * BlobWorkitem will only be loaded by the Controller if a new Attachment need
 * to to be stored. All other information can be read from the property $File
 * stored in the parent WorkItem. The property 'RestServiceURI' allows the
 * fileUploadController also to provide download urls which are pointing
 * directly to the RestService interface.
 * 
 * 
 * @see http://ironicprogrammer.blogspot.de/2010/03/file-upload-in-jsf2.html
 * 
 */
@Named("fileUploadController")
@SessionScoped
public class FileUploadController implements Serializable {

	private static final long serialVersionUID = 1L;

	private List<FileData> uploadedFiles;
	private List<String> attachedFiles;
	private List<String> removedFiles;
	private boolean dirty;

	// rest service URI
	private String restServiceURI;
	private String workitemID;

	private static Logger logger = Logger.getLogger("org.imixs.workflow");

	@EJB
	org.imixs.workflow.jee.ejb.EntityService entityService;

	public FileUploadController() {
		super();
		uploadedFiles = new ArrayList<FileData>();
	}

	public String getRestServiceURI() {
		return restServiceURI;
	}

	public void setRestServiceURI(String restServiceURI) {
		this.restServiceURI = restServiceURI;
	}

	public String getWorkitemID() {
		return workitemID;
	}

	public void setWorkitemID(String workitemID) {
		this.workitemID = workitemID;
	}

	/**
	 * Returns a list of files contained by the current workitem.
	 * 
	 * @return
	 */
	public List<String> getAttachedFiles() {
		if (attachedFiles == null)
			attachedFiles = new ArrayList<String>();
		return attachedFiles;
	}

	/**
	 * Updates the list of attached files.
	 * 
	 * @param attachedFiles
	 */
	public void setAttachedFiles(List<String> attachedFiles) {
		this.attachedFiles = attachedFiles;
	}

	/**
	 * Returns a list of files which where removed from the AttachedFiles list.
	 * 
	 * @return
	 */
	public List<String> getRemovedFiles() {
		if (removedFiles == null)
			removedFiles = new ArrayList<String>();
		return removedFiles;
	}

	public void setRemovedFiles(List<String> removedFiles) {
		this.removedFiles = removedFiles;
	}

	/**
	 * Returns a FileData list with uploaded files. This method can be used from
	 * a client to store the file content into a database.
	 * 
	 * @return - a list of FileData objects
	 */
	public List<FileData> getUploadedFiles() {
		if (uploadedFiles == null)
			uploadedFiles = new ArrayList<FileData>();

		return uploadedFiles;

	}

	/**
	 * Adds a new file into the uploadedFiles property. The file Data is
	 * converted into a FileData object. A client can call the method
	 * 'updateWorkitem' to synchronize a WorkItem with the new uploaded files.
	 * 
	 * @param e
	 */
	public void doUpload(ActionEvent e) {
		logger.fine("FileUploadBean - doUpload...");

		HttpServletRequest req = (HttpServletRequest) FacesContext
				.getCurrentInstance().getExternalContext().getRequest();

		if (req instanceof MultipartRequestWrapper) {
			MultipartRequestWrapper multi = (MultipartRequestWrapper) req;
			if (uploadedFiles == null)
				uploadedFiles = new ArrayList<FileData>();

			FileData fileData = multi.findFile("imixsFileUpload");
			if (fileData != null) {

				logger.fine("FileName: " + fileData.getName());
				logger.fine("ContentType: " + fileData.getContentType());
				logger.fine("Size: " + fileData.getSize());

				uploadedFiles.add(fileData);
				dirty = true;
			}
		}
	}

	/**
	 * Removes a attached file object from the attachment list. A client can
	 * call the method 'updateWorkitem' to synchronize a WorkItem with the new
	 * uploaded files.
	 * 
	 * @param sFilename
	 *            - filename to be removed
	 * @return - null
	 */
	public String removeAttachmentAction(String sFilename) {
		getRemovedFiles().add(sFilename);
		getAttachedFiles().remove(sFilename);
		dirty = true;

		return null;
	}

	/**
	 * Indicates if one or more files where uploaded or a attached files were
	 * removed. A client can call the method to check if an update of a Workitem
	 * is necessary.
	 * 
	 * @return - true if content has changed
	 */
	public boolean isDirty() {
		return dirty;
	}

	/**
	 * Removes all new uploaded and removed Files. Clears the dirty flag.
	 * 
	 * @param e
	 *            - action event
	 */
	public void doClear(ActionEvent e) {
		logger.fine("FileUploadBean - clear uploadedFiles...");
		uploadedFiles = null;
		removedFiles = null;
		dirty = false;
	}

	/**
	 * Synchronize the current file list with an WorkItem. The method adds all
	 * new uploaded files into the WorkItem and deletes files which where
	 * removed from the list. A client can call 'isDirty' to ask if any changes
	 * where made. The parameter 'noContent' indicates if the content should be
	 * stored. If 'true' only the fileNames will be added into the given
	 * workItem. This is used for lazy loading implementations.
	 * 
	 * @param workitem
	 *            - workItem to store the uploaded files
	 * @param noContent
	 *            - if true only file names will be stored.
	 * 
	 */
	public void updateWorkitem(ItemCollection workitem, boolean noContent) {

		if (!getRemovedFiles().isEmpty()) {
			// delete all removed files
			for (String aFilename : getRemovedFiles()) {
				workitem.removeFile(aFilename);
			}

		}

		// add all new uploaded files into the workitem
		if (!getUploadedFiles().isEmpty()) {
			for (FileData aFile : getUploadedFiles()) {
				if (noContent) {
					byte[] empty = { 0 };
					// add the file name (with empty data) into the
					// parentWorkitem.
					workitem.addFile(empty, aFile.getName(), "");
				} else {
					// now add the file content into blobWorkitem
					workitem.addFile(aFile.getData(), aFile.getName(),
							aFile.getContentType());
				}
			}
		}

	}

}
