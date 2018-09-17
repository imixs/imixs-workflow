package org.imixs.workflow.services.rest;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Client request Filter for Imixs-JWT
 * 
 * @author rsoika
 *
 */
public class JWTAuthenticator implements RequestFilter {

	private final String jwt;
	private final static Logger logger = Logger.getLogger(JWTAuthenticator.class.getName());

	public JWTAuthenticator(String jwt) {
		this.jwt = jwt;
	}

	public void filter(HttpURLConnection connection) throws IOException {

		URL uri = connection.getURL();// .getUri();

		String url = uri.toString();
		if (!url.contains("jwt=")) {
			logger.info("adding JSON Web Token...");
			connection.setRequestProperty("jwt", jwt);
		}

	}

}
