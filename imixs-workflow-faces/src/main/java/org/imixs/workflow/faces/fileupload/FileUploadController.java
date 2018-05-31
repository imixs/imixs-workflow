package org.imixs.workflow.faces.fileupload;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Logger;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import org.imixs.workflow.FileData;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;

/**
 * The FileUploadController is a conversation scoped bean and used to hold the
 * upladed files and transfere the to the accoicated workitem. The
 * AjaxFileUploadServlet injects this bean to provide new file data.
 * 
 * @see AjaxFileUploadServlet.doPost
 * @author rsoika
 * 
 */
@Named
// @RequestScoped
@ConversationScoped
public class FileUploadController implements Serializable {

	private static final long serialVersionUID = 1L;

	private ItemCollection workitem = null;

	private static Logger logger = Logger.getLogger(FileUploadController.class.getName());

	@Inject
	private Conversation conversation;
	
//	@Context
//	private HttpServletRequest httpRequest;

	public FileUploadController() {
		super();
	}

	/**
	 * Setter method to get an instance of the current workitem the FileData should
	 * be stored.
	 * 
	 * @return
	 */
	public ItemCollection getWorkitem() {
		return workitem;
	}

	/**
	 * This method set the current workitem and starts a new conversation. With this
	 * mechanism the fileUploadController bean can be used in multiple browser tabs
	 * or browser sessions.
	 * 
	 * @param workitem
	 */
	public void setWorkitem(ItemCollection workitem) {
		this.workitem = workitem;

		if (workitem != null) {
			// start new conversation...
			if (conversation.isTransient()) {
				conversation.setTimeout(
						((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest())
								.getSession().getMaxInactiveInterval() * 1000);
				conversation.begin();
				logger.finest("......starting new conversation, id=" + conversation.getId());
			}
		} else {

			logger.info("oh was ist da los ich bekomme ein null workitem :-(");
		}
	}

	/**
	 * Returns the current conversation id. This id is passed to the
	 * AjaxFileUploadServelt URIs so make sure that the correct FileUploadController
	 * is injected.
	 * 
	 * @return
	 */
	public String getCID() {
		if (conversation != null) {
			return conversation.getId();
		} else {
			// no conversation injected!
			return "";
		}
	}

	/**
	 * The method adds all new uploaded files into the WorkItem property '$file'.
	 * 
	 * @param workitem
	 *            - workItem to store the uploaded files
	 */
	//@SuppressWarnings("unchecked")
	public void updateWorkitem(List<FileData> fileDataList) {
		if (workitem == null)
			return;
		logger.finest("...... updateWorkitem '" + workitem.getItemValueString(WorkflowKernel.UNIQUEID));
		
		
//		HttpServletRequest httpRequest = (HttpServletRequest) (FacesContext.getCurrentInstance().getExternalContext()
//				.getRequest());

//		List<FileData> fileDataList = (List<FileData>) httpRequest.getSession()
//				.getAttribute(AjaxFileUploadServlet.IMIXS_FILEDATA_LIST);
//		if (fileDataList == null) {
//			return;
//		}

		// add all new uploaded files into the workitem

		for (FileData filedata : fileDataList) {
			// now add the file content into blobWorkitem
			workitem.addFileData(filedata);
			//.addFile(aFile.getContent(), aFile.getName(), aFile.getContentType());
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
		HttpServletRequest httpRequest = (HttpServletRequest) (FacesContext.getCurrentInstance().getExternalContext()
				.getRequest());
		List<FileData> fileDataList = (List<FileData>) httpRequest.getSession()
				.getAttribute(AjaxFileUploadServlet.IMIXS_FILEDATA_LIST);
		return fileDataList;
	}

	/**
	 * clears the current uploaded files from the session param IMIXS_FILEDATA_LIST
	 */
	public void reset() {
//		HttpServletRequest httpRequest = (HttpServletRequest) (FacesContext.getCurrentInstance().getExternalContext()
//				.getRequest());
//		httpRequest.getSession().removeAttribute(AjaxFileUploadServlet.IMIXS_FILEDATA_LIST);

	}

}
