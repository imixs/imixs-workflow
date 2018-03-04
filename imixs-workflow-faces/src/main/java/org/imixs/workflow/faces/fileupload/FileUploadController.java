package org.imixs.workflow.faces.fileupload;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Logger;

import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;

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
	 * The method adds all new uploaded files into the WorkItem property
	 * '$file'.
	 * 
	 * @param workitem
	 *            - workItem to store the uploaded files
	 */
	@SuppressWarnings("unchecked")
	public void updateWorkitem(ItemCollection workitem) {
		if (workitem == null)
			return; 
		logger.finest("...... updateWorkitem '"
				+ workitem.getItemValueString(WorkflowKernel.UNIQUEID));
		HttpServletRequest httpRequest = (HttpServletRequest) (FacesContext
				.getCurrentInstance().getExternalContext().getRequest());

		List<FileData> fileDataList = (List<FileData>) httpRequest.getSession()
				.getAttribute(AjaxFileUploadServlet.IMIXS_FILEDATA_LIST);
		if (fileDataList == null) {
			return;
		}

		// add all new uploaded files into the workitem

		for (FileData aFile : fileDataList) {
			// now add the file content into blobWorkitem
			workitem.addFile(aFile.getData(), aFile.getName(),
					aFile.getContentType());
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
	 * returns the list of uploaded files
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<FileData> getUploades() {
		HttpServletRequest httpRequest = (HttpServletRequest) (FacesContext
				.getCurrentInstance().getExternalContext().getRequest());
		List<FileData> fileDataList = (List<FileData>) httpRequest.getSession()
				.getAttribute(AjaxFileUploadServlet.IMIXS_FILEDATA_LIST);
		return fileDataList;
	}

	/**
	 * clears the current uploaded files from the session param
	 * IMIXS_FILEDATA_LIST
	 */
	public void reset() {
		HttpServletRequest httpRequest = (HttpServletRequest) (FacesContext
				.getCurrentInstance().getExternalContext().getRequest());
		httpRequest.getSession().removeAttribute(
				AjaxFileUploadServlet.IMIXS_FILEDATA_LIST);

	}

}
