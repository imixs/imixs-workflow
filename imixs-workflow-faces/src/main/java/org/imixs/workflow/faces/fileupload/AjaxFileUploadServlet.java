package org.imixs.workflow.faces.fileupload;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.imixs.workflow.FileData;

/**
 * The AjaxFileUploadServlet is a Multipart-Servlet 3.0. It is used by the
 * imixsFileUplad widget. The widget is using a jQuery component to handle the
 * upload of multiple files and supports drag & drop functionality. The servlet
 * is configurered with a max file size to 10MB, and a max request size of 50MB.
 * 
 * @author rsoika
 *
 */
@WebServlet(urlPatterns = { "/fileupload/*" })
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 1024 * 1024 * 10, maxRequestSize = 1024 * 1024 * 50)
public class AjaxFileUploadServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final String REQUEST_METHOD_POST = "POST";
	private static final String REQUEST_METHOD_GET = "GET";
	private static final String CONTENT_TYPE_MULTIPART = "multipart/";
	private static final String CONTENT_DISPOSITION = "content-disposition";
	private static final String CONTENT_DISPOSITION_FILENAME = "filename";
	public static final String IMIXS_FILEDATA_LIST = "IMIXS_FILEDATA_LIST";

	private static Logger logger = Logger.getLogger(AjaxFileUploadServlet.class.getName());

	@Inject
	FileUploadController fileUploadController;

	/**
	 * Upload files to stored in the current user session
	 */
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse response)
			throws ServletException, IOException {
		if (isPostFileUploadRequest(httpRequest)) {
			logger.finest("......add files...");
			List<FileData> fileDataList = getFilesFromRequest(httpRequest);
			// now update the workitem....
			if (fileUploadController != null) {
				// check workitem... issue
				if (fileUploadController.getWorkitem() != null) {
					logger.finest("......add new fileData object...");
					for (FileData filedata : fileDataList) {
						fileUploadController.addAttachedFile(filedata);
					}
				}
			}
			writeJsonMetadata(response, httpRequest.getRequestURI());
		}
	}

	/**
	 * Delete a existing file form the fileData list stored in the current user
	 * session
	 */
	@Override
	protected void doDelete(HttpServletRequest httpRequest, HttpServletResponse response)
			throws ServletException, IOException {

		// List<FileData> fileDataList = getFileList(httpRequest);
		int iCancel = httpRequest.getRequestURI().indexOf("/fileupload/");
		String filename = httpRequest.getRequestURI().substring(iCancel + 12);

		// now update the fileUploadController and the list of uploaded files....
		if (fileUploadController != null) {
			fileUploadController.removeAttachedFile(filename);
		}

		String contextURL = httpRequest.getRequestURI();
		// cut last /....
		contextURL = contextURL.substring(0, contextURL.lastIndexOf('/') + 1);
		writeJsonMetadata(response, contextURL);
	}

	/**
	 * Getter method to return the file content from the fileData list stored in the
	 * current user
	 */
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
			throws ServletException, IOException {

		// check cancel upload...
		if (isGetFileUploadRequest(httpRequest)) {
			int iCancel = httpRequest.getRequestURI().indexOf("/fileupload/");
			String filename = httpRequest.getRequestURI().substring(iCancel + 12);

			// urldecoding...
			filename = URLDecoder.decode(filename, "UTF-8");

			if (fileUploadController != null) {
				// check workitem... issue
				if (fileUploadController.getWorkitem() != null) {
					FileData fileData = fileUploadController.getWorkitem().getFileData(filename);
					// write contenremoveFile(filename);
					if (fileData != null) {
						writeFileContent(httpResponse, fileData);
					} else {
						httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
					}
				}
			}

		}

		// request just the currently uploaded files in json format
		if (isGetRefreshFileUploadRequest(httpRequest)) {
			String contextURL = httpRequest.getRequestURI();
			writeJsonMetadata(httpResponse, contextURL);
		}
	}

	/**
	 * checks if the httpRequest is a fileupload
	 * 
	 * @param httpRequest
	 * @return
	 */
	private boolean isPostFileUploadRequest(HttpServletRequest httpRequest) {
		String sContentType = httpRequest.getContentType();
		logger.finest("......contentType=" + sContentType);

		return (REQUEST_METHOD_POST.equalsIgnoreCase(httpRequest.getMethod()) && httpRequest.getContentType() != null
				&& sContentType.toLowerCase().startsWith(CONTENT_TYPE_MULTIPART));
	}

	/**
	 * checks if the httpRequest is a fileupload get request...
	 * 
	 * @param httpRequest
	 * @return
	 */
	private boolean isGetFileUploadRequest(HttpServletRequest httpRequest) {
		String uri = httpRequest.getRequestURI();

		return (REQUEST_METHOD_GET.equalsIgnoreCase(httpRequest.getMethod())
				&& !(uri.endsWith("/fileupload") || uri.endsWith("/fileupload/")));

	}

	/**
	 * checks if the httpRequest is a fileupload get request...
	 * 
	 * @param httpRequest
	 * @return
	 */
	private boolean isGetRefreshFileUploadRequest(HttpServletRequest httpRequest) {

		String uri = httpRequest.getRequestURI();
		return (REQUEST_METHOD_GET.equalsIgnoreCase(httpRequest.getMethod())
				&& (uri.endsWith("/fileupload") || uri.endsWith("/fileupload/")));

	}

	/**
	 * Returns a file attachment located in the property $file of the specified
	 * workitem
	 * 
	 * The file name will be encoded. With a URLDecode the filename is decoded in
	 * different formats and searched in the file list. This is not a nice solution.
	 * 
	 * @param uniqueid
	 * @return
	 * @throws IOException
	 */
	private void writeFileContent(ServletResponse response, FileData fileData) throws IOException {
		logger.finest("......write file content...");
		ServletOutputStream output = response.getOutputStream();
		output.write(fileData.getContent());
		// now return json string of uploaded files....
		response.setContentType(fileData.getContentType());
		output.close();
	}

	/**
	 * test and extracts the filename of a http request part. The method returns
	 * null if the part dose not contain a file
	 * 
	 * @param part
	 * @return - filename or null if not a file content
	 */
	private String getFilename(Part part) {
		for (String cd : part.getHeader(CONTENT_DISPOSITION).split(";")) {
			if (cd.trim().startsWith(CONTENT_DISPOSITION_FILENAME)) {
				return cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
			}
		}
		return null;
	}

	/**
	 * This method converts mulitple files from the httpRequest into a list of
	 * FileData objects.
	 * 
	 * @param httpRequest
	 * @return list of FileData objects
	 */
	private List<FileData> getFilesFromRequest(HttpServletRequest httpRequest) {
		logger.finest("......Looping parts");

		List<FileData> fileDataList = new ArrayList<FileData>();
		try {
			for (Part p : httpRequest.getParts()) {
				byte[] b = new byte[(int) p.getSize()];
				p.getInputStream().read(b);
				p.getInputStream().close();
				// params.put(p.getName(), new String[] { new String(b) });

				// test if part contains a file
				String fileName = getFilename(p);
				if (fileName != null) {

					/*
					 * issue #106
					 * 
					 * https://developer.jboss.org/message/941661#941661
					 * 
					 * Here we test of the encoding and try to convert to utf-8.
					 */
					byte fileNameISOBytes[] = fileName.getBytes("iso-8859-1");
					String fileNameUTF8 = new String(fileNameISOBytes, "UTF-8");
					if (fileName.length() != fileNameUTF8.length()) {
						// convert to utf-8
						logger.finest("......filename seems to be ISO-8859-1 encoded");
						fileName = new String(fileName.getBytes("iso-8859-1"), "utf-8");
					}

					// extract the file content...
					FileData fileData = null;
					logger.finest("......filename : " + fileName + ", contentType " + p.getContentType());
					fileData = new FileData(fileName, b, p.getContentType());
					fileDataList.add(fileData);

				}
			}

		} catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
		} catch (ServletException ex) {
			logger.log(Level.SEVERE, null, ex);
		}

		return fileDataList;
	}

	/**
	 * This method write a JSON meta data structure for uploaded files into the
	 * httpResponse. This structure is used by the ajax jquery file control. (see
	 * the imixs-faces.js file)
	 * 
	 * 
	 * <code>
			{
			    "files": [
			        {
			            "url": "0:0:0:0:0:0:0:1",
			            "thumbnail_url": "",
			            "name": "start.gif",
			            "type": "image/gif",
			            "size": 128,
			            "delete_url": "",
			            "delete_type": "DELETE"
			        }
			    ]
			}
	 *  </code>
	 * 
	 * 
	 * 
	 * 
	 * * @see https://github.com/blueimp/jQuery-File-Upload/wiki/JSON-Response
	 * 
	 * @throws IOException
	 * 
	 */
	private void writeJsonMetadata(ServletResponse response, String context_url) throws IOException {

		response.setContentType("application/json;charset=UTF-8");
		PrintWriter out = response.getWriter();

		// look if we have a worktiem with filedata....
		if (fileUploadController != null) {
			// check workitem... issue
			if (fileUploadController.getWorkitem() != null) {
				// List<FileData> fileDataList =
				// fileUploadController.getWorkitem().getFileData();// .removeFile(filename);

				List<FileData> fileDataList = fileUploadController.getAttachedFiles();
				logger.finest("......write JSON meta data...");

				String result = "{ \"files\":[";
				for (int i = 0; i < fileDataList.size(); i++) {

					FileData fileData = fileDataList.get(i);
					// we construct a temp file url with the current converstion id....
					result += "{ \"url\": \"" + context_url + fileData.getName() + "?cid="
							+ fileUploadController.getCID() + "\",";
					result += "\"thumbnail_url\": \"\",";
					result += "\"name\": \"" + fileData.getName() + "\",";
					result += "\"type\": \"" + fileData.getContentType() + "\",";
					result += "\"size\": " + fileData.getContent().length + ",";
					result += "\"delete_url\": \"\",";
					result += "\"delete_type\": \"DELETE\"";

					// last element?
					if (i < fileDataList.size() - 1)
						result += "},";
					else
						result += "}";
				}
				result += "]}";
				out.write(result);
			}
		}

		out.close();

	}

}
