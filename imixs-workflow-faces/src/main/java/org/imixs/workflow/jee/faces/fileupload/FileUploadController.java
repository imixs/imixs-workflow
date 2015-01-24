package org.imixs.workflow.jee.faces.fileupload;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Logger;

import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.ejb.EntityService;

/**
 * 
 * @author rsoika
 * 
 */
@Named
@SessionScoped
public class FileUploadController implements Serializable {

	private static final long serialVersionUID = 1L;

	private static Logger logger = Logger.getLogger(FileUploadController.class
			.getName());

	public FileUploadController() {
		super();
	}

	/**
	 * Synchronize the file list stored in the current session with a WorkItem.
	 * The method adds all new uploaded files into the WorkItem and deletes
	 * files which where removed from the list. A client can call 'isDirty' to
	 * ask if any changes where made. The parameter 'noContent' indicates if the
	 * content should be stored. If 'true' only the fileNames will be added into
	 * the given workItem. This is used for lazy loading implementations.
	 * 
	 * @param workitem
	 *            - workItem to store the uploaded files
	 * @param noContent
	 *            - if true only file names will be stored.
	 * 
	 */
	@SuppressWarnings("unchecked")
	public void updateWorkitem(ItemCollection workitem, boolean noContent) {
		if (workitem == null)
			return;
		logger.fine("[MultiFileController] updateWorkitem '"
				+ workitem.getItemValueString(EntityService.UNIQUEID));
		HttpServletRequest httpRequest = (HttpServletRequest) (FacesContext
				.getCurrentInstance().getExternalContext().getRequest());

		List<FileData> fileDataList = (List<FileData>) httpRequest.getSession()
				.getAttribute(AjaxFileUploadFilter.IMIXS_FILEDATA_LIST);
		if (fileDataList == null) {
			return;
		}
 
		// add all new uploaded files into the workitem

		for (FileData aFile : fileDataList) {
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

		// reset session IMIXS_FILEDATA_LIST
		reset();
	}

	/**
	 * Removes a attached file object from the workitem.
	 * 
	 * @param sFilename
	 *            - filename to be removed
	 * @return - null
	 */
	public void removeAttachedFile(ItemCollection workitem, String aFilename) {
		workitem.removeFile(aFilename);

	}
 
	/**
	 * clears the current uploaded files from the session param
	 * IMIXS_FILEDATA_LIST
	 */
	public void reset() {
		HttpServletRequest httpRequest = (HttpServletRequest) (FacesContext
				.getCurrentInstance().getExternalContext().getRequest());
		httpRequest.getSession().removeAttribute(
				AjaxFileUploadFilter.IMIXS_FILEDATA_LIST);

	}
	
	
	
	
}
