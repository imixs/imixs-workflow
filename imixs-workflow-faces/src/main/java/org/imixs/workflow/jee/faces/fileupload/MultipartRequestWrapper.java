package org.imixs.workflow.jee.faces.fileupload;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.Part;

/**
 * This Class wrapps a HTTP Servlet request. This is used to extract an uploaded
 * file from the parts of a multipart/form-data.
 * 
 * The class was developed initially by theironicprogrammer@gmail.com
 * 
 * @author theironicprogrammer@gmail.com
 * @see http://ironicprogrammer.blogspot.de/2010/03/file-upload-in-jsf2.html
 */
public class MultipartRequestWrapper extends HttpServletRequestWrapper {
	private static final String CONTENT_DISPOSITION = "content-disposition";
	private static final String CONTENT_DISPOSITION_FILENAME = "filename";

	private static Logger logger = Logger.getLogger("org.imixs.workflow");

	private Hashtable<String, String[]> params = new Hashtable<String, String[]>();

	/**
	 * The constructor wrap the http servlet request and puts all params
	 * contained by the request into a hashmap (params)
	 * 
	 * @param request
	 */
	public MultipartRequestWrapper(HttpServletRequest request) {
		super(request);
		logger.fine("Created multipart wrapper....");
		try {
			logger.fine("Looping parts");
			for (Part p : request.getParts()) {
				byte[] b = new byte[(int) p.getSize()];
				p.getInputStream().read(b);
				p.getInputStream().close();
				params.put(p.getName(), new String[] { new String(b) });
			}
		} catch (IOException ex) {
			Logger.getLogger(MultipartRequestWrapper.class.getName()).log(
					Level.SEVERE, null, ex);
		} catch (ServletException ex) {
			Logger.getLogger(MultipartRequestWrapper.class.getName()).log(
					Level.SEVERE, null, ex);
		}

	}

	/**
	 * This method returns the file content of a specific multi part http
	 * request. Notice that we put the file content (byte[]) back into the prams
	 * HashMap after wie read the file inputStream.
	 * 
	 * @param attrName
	 *            - the name of the http request part.
	 * @return a UploadFile object containing the name, contenttype and file
	 *         content
	 */
	public FileData findFile(String attrName) {
		FileData uf = null;
		try {

			Part p = findPart(attrName);
			if (p != null) {
				String fileName = getFilename(p);
				logger.fine("Filename : " + fileName + ", contentType "
						+ p.getContentType());
				byte[] b = new byte[(int) p.getSize()];
				p.getInputStream().read(b);
				p.getInputStream().close();
				params.put(p.getName(), new String[] { new String(b) });
				uf = new FileData(fileName, p.getContentType(), b);
			}

		} catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
		return uf;
	}

	/**
	 * This method extracts a single part of a multipart request
	 * 
	 * @param name
	 *            - the name of the multipart http request
	 * @return the http request part. returns null if part not found
	 * 
	 */
	private Part findPart(String name) {

		HttpServletRequest request = (HttpServletRequest) getRequest();
		Part p = null;
		try {
			for (Part part : request.getParts()) {
				if (part.getName().equals(name)) {
					p = part;
					break;
				}
			}
		} catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
		} catch (ServletException ex) {
			logger.log(Level.SEVERE, null, ex);
		}

		return p;
	}

	/**
	 * extracts the fielname of a http request part
	 * 
	 * @param part
	 * @return
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

	@Override
	public String getParameter(String name) {

		String[] values = getParameterValues(name);
		if (values == null || values.length == 0) {
			return null;
		}

		return values[0];
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		return params;
	}

	@Override
	public Enumeration<String> getParameterNames() {
		return params.keys();
	}

	@Override
	public String[] getParameterValues(String name) {
		return params.get(name);
	}

}
