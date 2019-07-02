/*******************************************************************************
 *  Imixs Workflow 
 *  Copyright (C) 2001, 2011 Imixs Software Solutions GmbH,  
 *  http://www.imixs.com
 *  
 *  This program is free software; you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License 
 *  as published by the Free Software Foundation; either version 2 
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 *  General Public License for more details.
 *  
 *  You can receive a copy of the GNU General Public
 *  License at http://www.gnu.org/licenses/gpl.html
 *  
 *  Project: 
 *  	http://www.imixs.org
 *  	http://java.net/projects/imixs-workflow
 *  
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika - Software Developer
 *******************************************************************************/
package org.imixs.workflow.services.rest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * The Imixs RestClient is a helper class for a Rest based communication without
 * the use of Jax-rs.
 * <p>
 * The Imixs RestClient provides methods to GET and POST data objects.
 * <p>
 * The client throws a RestAPIException in case of an communication error.
 * <p>
 * For a convinient way to access the Imixs-Rest API use the Imixs-Melman
 * project on Github.
 * 
 * @author Ralph Soika
 */
public class RestClient {

	private String serviceEndpoint;
	private Map<String, String> requestProperties = null;
	private String encoding = "UTF-8";
	private int iLastHTTPResult = 0;
	private String rootURL = null;
	private final static Logger logger = Logger.getLogger(RestClient.class.getName());

	protected List<RequestFilter> requestFilterList;

	public RestClient() {
		super();
		requestFilterList = new ArrayList<RequestFilter>();
	}

	public RestClient(String rootURL) {
		this();
		if (rootURL != null && !rootURL.endsWith("/")) {
			rootURL += "/";
		}

		this.rootURL = rootURL;
	}

	/**
	 * Register a ClientRequestFilter instance.
	 * 
	 * @param filter
	 *            - request filter instance.
	 */
	public void registerRequestFilter(RequestFilter filter) {
		logger.finest("......register new request filter: " + filter.getClass().getSimpleName());

		// client.register(filter);
		requestFilterList.add(filter);
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String aEncoding) {
		encoding = aEncoding;
	}

	public String getServiceEndpoint() {
		return serviceEndpoint;
	}

	/**
	 * This method builds the serviceEndpoint based on a given URI . The method
	 * prafix the URI with the root uri if the uri starts with /
	 * 
	 * @param uri
	 * @throws RestAPIException
	 */
	void setServiceEndpoint(String uri) throws RestAPIException {

		if (rootURL == null) {
			throw new RestAPIException(0, "rootURL is null!");
		}

		// test for double /
		if (uri != null && uri.startsWith("/")) {
			uri = uri.substring(1);
		}
		// test for protocoll
		if (!uri.contains("://")) {
			// add root URL
			uri = rootURL + uri;
		}

		this.serviceEndpoint = uri;
	}

	/**
	 * Set a single header request property
	 */
	public void setRequestProperty(String key, String value) {
		if (requestProperties == null) {
			requestProperties = new HashMap<String, String>();
		}
		requestProperties.put(key, value);
	}

	/**
	 * Posts a String data object with a specific Content-Type to a Rest Service URI
	 * Endpoint. This method can be used to simulate different post scenarios.
	 * <p>
	 * The parameter 'contnetType' can be used to request a specific media type.
	 * 
	 * @param uri
	 *            - Rest Endpoint RUI
	 * @param dataString
	 *            - content
	 * @param contentType
	 *            - request MediaType
	 * @return content
	 */
	public String post(String uri, String dataString, String contentType) throws Exception {
		return post(uri, dataString, contentType, null);
	}

	/**
	 * Posts a String data object with a specific Content-Type to a Rest Service URI
	 * Endpoint. This method can be used to simulate different post scenarios.
	 * <p>
	 * The parameter 'contnetType' and 'acceptType' can be used to request and
	 * accept specific media types.
	 * 
	 * @param uri
	 *            - Rest Endpoint RUI
	 * @param dataString
	 *            - content
	 * @param contentType
	 *            - request MediaType
	 * @param acceptType
	 *            - accept MediaType
	 * @return content
	 */
	public String post(String uri, String dataString, String contentType, String acceptType) throws Exception {
		PrintWriter printWriter = null;
		if (contentType == null || contentType.isEmpty()) {
			contentType = "application/xml";
		}
		if (acceptType == null || acceptType.isEmpty()) {
			acceptType = contentType;
		}

		HttpURLConnection urlConnection = null;
		try {
			serviceEndpoint = uri;
			iLastHTTPResult = 500;

			urlConnection = (HttpURLConnection) new URL(serviceEndpoint).openConnection();
			urlConnection.setRequestMethod("POST");
			urlConnection.setDoOutput(true);
			urlConnection.setDoInput(true);
			urlConnection.setAllowUserInteraction(false);

			/** * HEADER ** */
			urlConnection.setRequestProperty("Content-Type", contentType + "; charset=" + encoding);
			urlConnection.setRequestProperty("Accept-Charset", encoding);
			urlConnection.setRequestProperty("Accept", acceptType);

			// process filters....
			for (RequestFilter filter : requestFilterList) {
				filter.filter(urlConnection);
			}

			StringWriter writer = new StringWriter();
			writer.write(dataString);
			writer.flush();

			// compute length
			urlConnection.setRequestProperty("Content-Length",
					"" + Integer.valueOf(writer.toString().getBytes().length));

			printWriter = new PrintWriter(
					new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream(), encoding)));
			printWriter.write(writer.toString());
			printWriter.close();

			String sHTTPResponse = urlConnection.getHeaderField(0);
			try {
				iLastHTTPResult = Integer.parseInt(sHTTPResponse.substring(9, 12));
			} catch (Exception eNumber) {
				// eNumber.printStackTrace();
				iLastHTTPResult = 500;
			}
			String content = readResponse(urlConnection);

			return content;

		} catch (Exception ioe) {
			// ioe.printStackTrace();
			throw ioe;
		} finally {
			// Release current connection
			if (printWriter != null)
				printWriter.close();
		}
	}

	/**
	 * This method returns the last HTTP Result
	 * 
	 * @return
	 */
	public int getLastHTTPResult() {
		return iLastHTTPResult;
	}

	/**
	 * Gets the content of a GET request from a Rest Service URI Endpoint. I case of
	 * an error the method throws a RestAPIException.
	 * 
	 * @param uri
	 *            - Rest Endpoint RUI
	 * @return - content or null if no content is available.
	 */
	public String get(String uri) throws RestAPIException {
		int responseCode = -1;

		setServiceEndpoint(uri);
		try {
			HttpURLConnection urlConnection = (HttpURLConnection) new URL(serviceEndpoint).openConnection();

			// optional default is GET
			urlConnection.setRequestMethod("GET");

			urlConnection.setDoOutput(true);
			urlConnection.setDoInput(true);
			urlConnection.setAllowUserInteraction(false);

			if (requestProperties != null) {
				for (Map.Entry<String, String> entry : requestProperties.entrySet()) {
					urlConnection.setRequestProperty(entry.getKey(), entry.getValue());
				}
			}

			// process filters....
			for (RequestFilter filter : requestFilterList) {
				filter.filter(urlConnection);
			}

			responseCode = urlConnection.getResponseCode();
			logger.finest("......Sending 'GET' request to URL : " + serviceEndpoint);
			logger.finest("......Response Code : " + responseCode);
			// read response if response was successful
			if (responseCode >= 200 && responseCode <= 299) {
				return readResponse(urlConnection);
			} else {
				String error = "Error " + responseCode + " - failed GET request from '" + uri + "'";
				logger.warning(error);
				throw new RestAPIException(responseCode, error);
			}
		} catch (IOException e) {
			String error = "Error GET request from '" + uri + " - " + e.getMessage();
			logger.warning(error);
			throw new RestAPIException(0, error, e);

		}
	}

	/**
	 * Reads the response from a http request.
	 * 
	 * @param urlConnection
	 * @throws IOException
	 */
	private String readResponse(URLConnection urlConnection) throws IOException {
		// get content of result
		logger.finest("......readResponse....");
		StringWriter writer = new StringWriter();
		BufferedReader in = null;
		try {
			// test if content encoding is provided
			String sContentEncoding = urlConnection.getContentEncoding();
			if (sContentEncoding == null || sContentEncoding.isEmpty()) {
				// no so lets see if the client has defined an encoding..
				if (encoding != null && !encoding.isEmpty())
					sContentEncoding = encoding;
			}

			// if an encoding is provided read stream with encoding.....
			if (sContentEncoding != null && !sContentEncoding.isEmpty())
				in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), sContentEncoding));
			else
				in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				logger.finest("......" + inputLine);
				writer.write(inputLine);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (in != null)
				in.close();
		}

		return writer.toString();

	}

}
