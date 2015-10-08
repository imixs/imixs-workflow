package org.imixs.workflow.jee.faces.fileupload;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

@WebServlet(urlPatterns = { "/fileupload/*" })
// configure Servlet 3.0 multipart. Limit file size to 100MB, 500MB Request Size
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 1024 * 1024 * 100, maxRequestSize = 1024 * 1024 * 100 * 5)
public class AjaxFileUploadServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final String REQUEST_METHOD_POST = "POST";
	private static final String REQUEST_METHOD_GET = "GET";
	private static final String CONTENT_TYPE_MULTIPART = "multipart/";
	private static final String CONTENT_DISPOSITION = "content-disposition";
	private static final String CONTENT_DISPOSITION_FILENAME = "filename";
	public static final String IMIXS_FILEDATA_LIST = "IMIXS_FILEDATA_LIST";
	private List<FileData> fileDataList = null;

	private static Logger logger = Logger.getLogger(AjaxFileUploadServlet.class
			.getName());

	@SuppressWarnings("unchecked")
	@Override
	protected void doPost(HttpServletRequest httpRequest,
			HttpServletResponse response) throws ServletException, IOException {

		if (isPostFileUploadRequest(httpRequest)) {

			fileDataList = (List<FileData>) httpRequest.getSession()
					.getAttribute(IMIXS_FILEDATA_LIST);
			if (fileDataList == null) {
				fileDataList = new ArrayList<FileData>();
			}

			logger.fine("[MultipartRequestFilter] add files...");
			addFiles(httpRequest);

			// store file content into session
			httpRequest.getSession().setAttribute(IMIXS_FILEDATA_LIST,
					fileDataList);

			String contextURL = httpRequest.getRequestURI();

			writeJsonContent(contextURL, response);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void doDelete(HttpServletRequest httpRequest,
			HttpServletResponse response) throws ServletException, IOException {

		fileDataList = (List<FileData>) httpRequest.getSession().getAttribute(
				IMIXS_FILEDATA_LIST);
		if (fileDataList == null) {
			fileDataList = new ArrayList<FileData>();
		}

		int iCancel = httpRequest.getRequestURI().indexOf("/fileupload/");
		String filename = httpRequest.getRequestURI().substring(iCancel + 12);

		removeFile(filename);

		// store file content into session
		httpRequest.getSession()
				.setAttribute(IMIXS_FILEDATA_LIST, fileDataList);

		// get context url from request uri
		String contextURL = httpRequest.getRequestURI();
		// cut last /....
		contextURL = contextURL.substring(0, contextURL.lastIndexOf('/') + 1);

		writeJsonContent(contextURL, response);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void doGet(HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) throws ServletException,
			IOException {

		fileDataList = (List<FileData>) httpRequest.getSession().getAttribute(
				IMIXS_FILEDATA_LIST);
		if (fileDataList == null) {
			fileDataList = new ArrayList<FileData>();
		}

		// check cancel upload...
		if (isGetFileUploadRequest(httpRequest)) {
			int iCancel = httpRequest.getRequestURI().indexOf("/fileupload/");
			String filename = httpRequest.getRequestURI().substring(
					iCancel + 12);

			FileData fileData = getFile(filename);
			// write contenremoveFile(filename);
			if (fileData != null) {
				writeFileContent(httpResponse, fileData);
			} else {
				httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
			}

		}

		// reques just the currently uploaded files in json format
		if (isGetRefreshFileUploadRequest(httpRequest)) {
			String contextURL = httpRequest.getRequestURI();

			writeJsonContent(contextURL, httpResponse);

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
		logger.fine("[MulitpartRequestFilter]  contentType=" + sContentType);

		return (REQUEST_METHOD_POST.equalsIgnoreCase(httpRequest.getMethod())
				&& httpRequest.getContentType() != null && sContentType
				.toLowerCase().startsWith(CONTENT_TYPE_MULTIPART));
	}

	/**
	 * checks if the httpRequest is a fileupload get request...
	 * 
	 * @param httpRequest
	 * @return
	 */
	private boolean isGetFileUploadRequest(HttpServletRequest httpRequest) {
		String uri = httpRequest.getRequestURI();

		return (REQUEST_METHOD_GET.equalsIgnoreCase(httpRequest.getMethod()) && !(uri
				.endsWith("/fileupload") || uri.endsWith("/fileupload/")));

	}

	/**
	 * checks if the httpRequest is a fileupload get request...
	 * 
	 * @param httpRequest
	 * @return
	 */
	private boolean isGetRefreshFileUploadRequest(HttpServletRequest httpRequest) {

		String uri = httpRequest.getRequestURI();
		return (REQUEST_METHOD_GET.equalsIgnoreCase(httpRequest.getMethod()) && (uri
				.endsWith("/fileupload") || uri.endsWith("/fileupload/")));

	}

	/**
	 * Returns a file attachment located in the property $file of the specified
	 * workitem
	 * 
	 * The file name will be encoded. With a URLDecode the filename is decoded
	 * in different formats and searched in the file list. This is not a nice
	 * solution.
	 * 
	 * @param uniqueid
	 * @return
	 * @throws IOException
	 */
	private void writeFileContent(ServletResponse response, FileData fileData)
			throws IOException {

		logger.fine("[MulitpartRequestFilter] write file content...");

		ServletOutputStream output = response.getOutputStream();
		output.write(fileData.getData());

		// now return json string of uploaded files....
		response.setContentType(fileData.getContentType());

		output.close();

	}

	private void writeJsonContent(String context_url, ServletResponse response)
			throws IOException {
		logger.fine("[MulitpartRequestFilter] return JSON content...");
		// now return json string of uploaded files....
		response.setContentType("application/json;charset=UTF-8");
		PrintWriter out = response.getWriter();
		out.write(getJson(context_url));
		out.close();

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
				return cd.substring(cd.indexOf('=') + 1).trim()
						.replace("\"", "");
			}
		}
		return null;
	}

	/**
	 * removes an uploaded file from the fileDataList...
	 * 
	 * @param file
	 *            - filename to be removed
	 */
	private void removeFile(String file) {
		int pos = -1;
		if (file == null)
			return;
		for (int i = 0; i < fileDataList.size(); i++) {
			FileData fileData = fileDataList.get(i);
			if (file.equals(fileData.getName())) {
				pos = i;
				break;
			}
		}
		// found?
		if (pos > -1) {
			logger.fine("[MultipartRequestWrapper] remove file '" + file + "'");
			fileDataList.remove(pos);
		}
	}

	private void addFiles(HttpServletRequest httpRequest) {
		logger.fine("[MultipartRequestWrapper] Looping parts");
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
						logger.fine("filename seems to be ISO-8859-1 encoded");
						fileName = new String(fileName.getBytes("iso-8859-1"),
								"utf-8");
					}

					// extract the file content...
					FileData fileData = null;
					logger.fine("Filename : " + fileName + ", contentType "
							+ p.getContentType());
					fileData = new FileData(fileName, p.getContentType(), b);
					if (fileData != null) {
						// remove existing file
						removeFile(fileData.getName());
						// update filedataList...
						fileDataList.add(fileData);
					}

				}
			}
		} catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
		} catch (ServletException ex) {
			logger.log(Level.SEVERE, null, ex);
		}

	}

	/**
	 * gets an uploaded file from the fileDataList...
	 * 
	 * @param file
	 *            - filename to be removed
	 */
	private FileData getFile(String file) {
		FileData result = null;
		if (file == null)
			return null;
		for (int i = 0; i < fileDataList.size(); i++) {
			FileData fileData = fileDataList.get(i);
			if (file.equals(fileData.getName())) {
				result = fileData;
				break;
			}
		}
		return result;
	}

	/**
	 * returns a json structure for uploaded files.
	 * 
	 * @see https://github.com/blueimp/jQuery-File-Upload/wiki/JSON-Response
	 * 
	 *      <code>
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
	 * @return
	 */
	private String getJson(String context_url) {

		String result = "{ \"files\":[";
		for (int i = 0; i < fileDataList.size(); i++) {

			FileData fileData = fileDataList.get(i);

			result += "{ \"url\": \"" + context_url + fileData.getName()
					+ "\",";
			result += "\"thumbnail_url\": \"\",";
			result += "\"name\": \"" + fileData.getName() + "\",";
			result += "\"type\": \"" + fileData.getContentType() + "\",";
			result += "\"size\": " + fileData.getSize() + ",";
			result += "\"delete_url\": \"\",";
			result += "\"delete_type\": \"DELETE\"";

			// last element?
			if (i < fileDataList.size() - 1)
				result += "},";
			else
				result += "}";
		}

		result += "]}";

		return result;
	}

}
