package org.imixs.workflow.jee.faces.fileupload;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

/**
 * This is a simple request filter which wrapps the http request with the
 * MultipartRequestWrapper
 * 
 * The class was developed initially by theironicprogrammer@gmail.com
 * 
 * @author theironicprogrammer@gmail.com
 * @see http://ironicprogrammer.blogspot.de/2010/03/file-upload-in-jsf2.html
 */
@WebFilter(urlPatterns = "/*")
public class MultipartRequestFilter implements Filter {
	private static final String REQUEST_METHOD_POST = "POST";
	private static final String CONTENT_TYPE_MULTIPART = "multipart/";

	private static Logger logger = Logger.getLogger("org.imixs.workflow");

	public void init(FilterConfig filterConfig) throws ServletException {

	}

	/**
	 * test if the request is a post request and if the content type is
	 * mulitpart. Then wrap the request...
	 * 
	 */
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) request;

		String sContentType=request.getContentType();
		logger.fine("MulitpartRequestFilter : contentType=" + sContentType);
		
		if (REQUEST_METHOD_POST.equalsIgnoreCase(httpRequest.getMethod())
				&& request.getContentType() != null
				&& sContentType.toLowerCase()
						.startsWith(CONTENT_TYPE_MULTIPART)) {
			logger.fine("Is multipart request.... wrapping it.");
			
			request = new MultipartRequestWrapper(httpRequest);
		}
		chain.doFilter(request, response);
	}

	public void destroy() {

	}

}