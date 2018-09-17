package org.imixs.workflow.services.rest;

import java.io.IOException;
import java.net.HttpURLConnection;

import org.imixs.workflow.util.Base64;

/**
 * Client Request Filter for basic authentication
 * 
 * 
 * @author rsoika
 *
 */
public class BasicAuthenticator implements RequestFilter {

	private final String user;
	private final String password;

	public BasicAuthenticator(String user, String password) {
		this.user = user;
		this.password = password;
	}

	public void filter(HttpURLConnection connection) throws IOException {
		connection.setRequestProperty("Authorization", "Basic " + getBasicAuthentication());

	}

	/**
	 * This methos set the user password information for basic authentication
	 */
	private String getBasicAuthentication() {
		String sURLAccess = "";
		// UserID:Passwort
		String sUserCode = user + ":" + password;
		// String convertieren
		// sURLAccess = Base64.encodeBase64(sUserCode.getBytes()).toString();
		char[] authcode = Base64.encode(sUserCode.getBytes());

		sURLAccess = String.valueOf(authcode);
		return sURLAccess;
	}

}
