package org.imixs.workflow.faces.util;

import java.util.Iterator;
import java.util.Map;

import javax.faces.FacesException;
import javax.faces.application.NavigationHandler;
import javax.faces.application.ViewExpiredException;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerWrapper;
import javax.faces.context.FacesContext;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;

/**
 * This class is used to handle expired sessions. In case a session was expired the 
 * handler caught the ViewExpiredException and redirects into a new page.
 *
 * This class expects a jsf page called 'sessionexpired.xhtml' in the web root
 * context!
 * 
 * 
 * @see ed burns ' dealing_gracefully_with_viewexpiredexception'
 * 
 *      https://www.nofluffjuststuff.com/blog/ed_burns/2009/09/
 *      dealing_gracefully_with_viewexpiredexception_in_jsf2
 * 
 * @author rsoika
 * 
 */
public class ViewExpiredExceptionHandler extends
		ExceptionHandlerWrapper {

	private ExceptionHandler wrapped;

	@SuppressWarnings("deprecation")
	public ViewExpiredExceptionHandler(ExceptionHandler wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public ExceptionHandler getWrapped() {
		return this.wrapped;
	}

	@Override
	public void handle() throws FacesException {
		for (Iterator<ExceptionQueuedEvent> i = getUnhandledExceptionQueuedEvents()
				.iterator(); i.hasNext();) {
			ExceptionQueuedEvent event = i.next();
			ExceptionQueuedEventContext context = (ExceptionQueuedEventContext) event
					.getSource();
			Throwable t = context.getException();
			if (t instanceof ViewExpiredException) {
				ViewExpiredException vee = (ViewExpiredException) t;
				FacesContext fc = FacesContext.getCurrentInstance();
				Map<String, Object> requestMap = fc.getExternalContext()
						.getRequestMap();
				NavigationHandler nav = fc.getApplication()
						.getNavigationHandler();
				try {
					// Push some useful stuff to the request scope for
					// use in the page
					requestMap.put("currentViewId", vee.getViewId());

					nav.handleNavigation(fc, null, "sessionexpired");
					fc.renderResponse();

				} finally {
					i.remove();
				}
			}
		}
		// At this point, the queue will not contain any ViewExpiredEvents.
		// Therefore, let the parent handle them.
		getWrapped().handle();

	}
}
