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
import java.io.StringReader;
import java.io.StringWriter;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.util.Base64;
import org.imixs.workflow.xml.XMLDataCollection;
import org.imixs.workflow.xml.XMLDocument;
import org.imixs.workflow.xml.XMLDocumentAdapter;
import org.imixs.workflow.xml.XMLDataCollectionAdapter;

/**
 * This ServiceClient is a WebService REST Client which encapsulate the
 * communication with a REST web serice based on the Imixs Workflow REST API.
 * The Implementation is based on the JAXB API.
 * 
 * The ServiceClient supports methods for posting EntityCollections and
 * XMLItemCollections.
 * 
 * The post method expects the rest service URI and a Dataobject based ont the
 * Imixs Workflow XML API
 * 
 * @see org.imixs.workflow.jee.rest
 * @author Ralph Soika
 * 
 */
public class RestClient {

	private CookieManager cookieManager = null;

	private String serviceEndpoint;
	private String user = null;
	private String password = null;
	private Map<String, String> requestProperties = null;
	private String encoding = "UTF-8";
	private int iLastHTTPResult = 0;
	private String content = null;

	private final static Logger logger = Logger.getLogger(RestClient.class.getName());

	// Sets credentials
	public void setCredentials(String auser, String apw) {
		user = auser;
		password = apw;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String aEncoding) {
		encoding = aEncoding;
	}

	public CookieManager getCookieManager() {
		return cookieManager;
	}

	public void setCookieManager(CookieManager cookieManager) {
		this.cookieManager = cookieManager;
	}

	public String getServiceEndpoint() {
		return serviceEndpoint;
	}

	public void setServiceEndpoint(String serviceEndpoint) {
		this.serviceEndpoint = serviceEndpoint;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
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

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	/**
	 * This method posts an XMLItemCollection in the Imixs XML Format to a Rest
	 * Service URI Endpoint.
	 * 
	 * 
	 * @param uri
	 *            - Rest Endpoint RUI
	 * @param entityCol
	 *            - an Entity Collection
	 * @return HTTPResult
	 */
	public int postEntity(String uri, XMLDocument aItemCol) throws Exception {
		PrintWriter printWriter = null;

		HttpURLConnection urlConnection = null;
		try {
			serviceEndpoint = uri;
			iLastHTTPResult = 500;

			urlConnection = (HttpURLConnection) new URL(serviceEndpoint).openConnection();
			urlConnection.setRequestMethod("POST");
			urlConnection.setDoOutput(true);
			urlConnection.setDoInput(true);
			urlConnection.setAllowUserInteraction(false);

			// Authorization
			if (user != null) {
				urlConnection.setRequestProperty("Authorization", "Basic " + this.getAccessByUser());
			}
			/** * HEADER ** */
			urlConnection.setRequestProperty("Content-Type", "application/xml; charset=" + encoding);

			StringWriter writer = new StringWriter();

			JAXBContext context = JAXBContext.newInstance(XMLDocument.class);
			Marshaller m = context.createMarshaller();
			m.marshal(aItemCol, writer);

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

			// get content of result
			readResponse(urlConnection);

		} catch (Exception ioe) {
			// ioe.printStackTrace();
			throw ioe;
		} finally {
			// Release current connection
			if (printWriter != null)
				printWriter.close();
		}

		return iLastHTTPResult;
	}

	/**
	 * This method posts an Entitycollection in the Imixs XML Format to a Rest
	 * Service URI Endpoint.
	 * 
	 * 
	 * @param uri
	 *            - Rest Endpoint RUI
	 * @param entityCol
	 *            - an Entity Collection
	 * @return HTTPResult
	 */
	public int postCollection(String uri, XMLDataCollection aEntityCol) throws Exception {
		PrintWriter printWriter = null;

		HttpURLConnection urlConnection = null;
		try {
			serviceEndpoint = uri;
			iLastHTTPResult = 500;

			urlConnection = (HttpURLConnection) new URL(serviceEndpoint).openConnection();
			urlConnection.setRequestMethod("POST");
			urlConnection.setDoOutput(true);
			urlConnection.setDoInput(true);
			urlConnection.setAllowUserInteraction(false);

			// Authorization
			if (user != null) {
				urlConnection.setRequestProperty("Authorization", "Basic " + this.getAccessByUser());
			}

			/** * HEADER ** */
			urlConnection.setRequestProperty("Content-Type", "application/xml; charset=" + encoding);

			StringWriter writer = new StringWriter();

			JAXBContext context = JAXBContext.newInstance(XMLDataCollection.class);
			Marshaller m = context.createMarshaller();
			m.marshal(aEntityCol, writer);

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

			// get content of result
			readResponse(urlConnection);

		} catch (Exception ioe) {
			// ioe.printStackTrace();
			throw ioe;
		} finally {
			// Release current connection
			if (printWriter != null)
				printWriter.close();
		}

		return iLastHTTPResult;
	}

	/**
	 * This method posts an JSON String in the Imixs ItemCollection Format to a Rest
	 * Service URI Endpoint.
	 * 
	 * 
	 * @param uri
	 *            - Rest Endpoint RUI
	 * @param entityCol
	 *            - an Entity Collection
	 * @return HTTPResult
	 */
	public int postJsonEntity(String uri, String aItemColString) throws Exception {
		PrintWriter printWriter = null;

		HttpURLConnection urlConnection = null;
		try {
			serviceEndpoint = uri;
			iLastHTTPResult = 500;

			urlConnection = (HttpURLConnection) new URL(serviceEndpoint).openConnection();
			urlConnection.setRequestMethod("POST");
			urlConnection.setDoOutput(true);
			urlConnection.setDoInput(true);
			urlConnection.setAllowUserInteraction(false);

			// Authorization
			if (user != null) {
				urlConnection.setRequestProperty("Authorization", "Basic " + this.getAccessByUser());
			}
			/** * HEADER ** */
			urlConnection.setRequestProperty("Content-Type", "application/json; charset=" + encoding);

			StringWriter writer = new StringWriter();

			writer.write(aItemColString);
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

			// get content of result
			readResponse(urlConnection);

		} catch (Exception ioe) {
			// ioe.printStackTrace();
			throw ioe;
		} finally {
			// Release current connection
			if (printWriter != null)
				printWriter.close();
		}

		return iLastHTTPResult;
	}

	/**
	 * This method posts a String data object with a specific Content-Type to a Rest
	 * Service URI Endpoint. This method can be used to simulate different post
	 * scenarios.
	 * 
	 * 
	 * 
	 * @param uri
	 *            - Rest Endpoint RUI
	 * @param entityCol
	 *            - an Entity Collection
	 * @return HTTPResult
	 */
	public int postString(String uri, String dataString, String contentType) throws Exception {
		PrintWriter printWriter = null;
		if (contentType == null || contentType.isEmpty()) {
			contentType = "application/xml";
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

			// Authorization
			if (user != null) {
				urlConnection.setRequestProperty("Authorization", "Basic " + this.getAccessByUser());
			}
			/** * HEADER ** */
			urlConnection.setRequestProperty("Content-Type", contentType + "; charset=" + encoding);

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

			// get content of result
			readResponse(urlConnection);

		} catch (Exception ioe) {
			// ioe.printStackTrace();
			throw ioe;
		} finally {
			// Release current connection
			if (printWriter != null)
				printWriter.close();
		}

		return iLastHTTPResult;
	}

	/**
	 * Returns all cookies set during the last request
	 * 
	 * @return
	 */
	public CookieManager getCookies() {
		return cookieManager;
	}

	/**
	 * Set the cookies to be used for the next request
	 * 
	 * @param cookieManager
	 */
	public void setCookies(CookieManager cookieManager) {
		this.cookieManager = cookieManager;
	}

	/**
	 * This method get the content of a GET request from a Rest Service URI
	 * Endpoint.
	 * 
	 * 
	 * @param uri
	 *            - Rest Endpoint RUI
	 * 
	 * @return HTTPResult
	 */
	public int get(String uri) throws Exception {
		URL obj = new URL(uri);
		HttpURLConnection urlConnection = (HttpURLConnection) obj.openConnection();

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

		// Authorization
		if (user != null) {
			urlConnection.setRequestProperty("Authorization", "Basic " + this.getAccessByUser());
		}

		addCookies(urlConnection);

		int responseCode = urlConnection.getResponseCode();
		logger.finest("......Sending 'GET' request to URL : " + uri);
		logger.finest("......Response Code : " + responseCode);
		// read response if response was successful  
		if (responseCode>=200 && responseCode<=299) {
			readResponse(urlConnection);
		}
		return responseCode;
	}

	/**
	 * Returns a list of ItemCollections from a XML data source
	 * 
	 * @return
	 * @throws Exception 
	 */
	public List<ItemCollection> getDocumentCollection(String url) throws Exception {
		this.setRequestProperty("Accept", MediaType.APPLICATION_XML);
		this.get(url);
		String xmlResult = this.getContent();

		// convert into ItemCollection list
		JAXBContext context = JAXBContext.newInstance(XMLDataCollection.class);
        Unmarshaller u = context.createUnmarshaller();
		StringReader reader = new StringReader(xmlResult);
		XMLDataCollection xmlDocuments = (XMLDataCollection) u.unmarshal(reader);

		List<ItemCollection> documents = XMLDataCollectionAdapter.putDataCollection(xmlDocuments);
		return documents;

	}
	
	/**
	 * Returns a list of ItemCollections from a XML data source
	 * 
	 * @return
	 * @throws Exception 
	 */
	public ItemCollection getDocument(String url) throws Exception {
		this.setRequestProperty("Accept", MediaType.APPLICATION_XML);
		this.get(url);
		String xmlResult = this.getContent();

		// convert into ItemCollection list
		JAXBContext context = JAXBContext.newInstance(XMLDocument.class);
		//JAXBContext context = JAXBContext.newInstance( "org.imixs.workflow.xml" );
		
		Unmarshaller u = context.createUnmarshaller();
		StringReader reader = new StringReader(xmlResult);
		XMLDocument xmlDocument = (XMLDocument) u.unmarshal(reader);

		ItemCollection document = XMLDocumentAdapter.putDocument(xmlDocument);
		return document;

	}

	public void readCookies(HttpURLConnection connection) throws URISyntaxException {
		String COOKIES_HEADER = "Set-Cookie";
		cookieManager = new java.net.CookieManager();

		Map<String, List<String>> headerFields = connection.getHeaderFields();
		List<String> cookiesHeader = headerFields.get(COOKIES_HEADER);

		if (cookiesHeader != null) {
			for (String cookie : cookiesHeader) {
				HttpCookie ding = HttpCookie.parse(cookie).get(0);

				cookieManager.getCookieStore().add(connection.getURL().toURI(), ding);
			}
		}
	}

	public void addCookies(HttpURLConnection connection) {
		if (cookieManager == null)
			return;

		String values = "";
		for (HttpCookie acookie : cookieManager.getCookieStore().getCookies()) {
			values = values + acookie + ",";
		}

		if (cookieManager.getCookieStore().getCookies().size() > 0) {
			connection.setRequestProperty("Cookie", values);
		}
	}

	/**
	 * Put the resonse string into the content property
	 * 
	 * @param urlConnection
	 * @throws IOException
	 */
	private void readResponse(URLConnection urlConnection) throws IOException {
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
				logger.finest("......"+inputLine);
				writer.write(inputLine);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (in != null)
				in.close();
		}

		setContent(writer.toString());

	}

	/**
	 * Diese Methode setzt fÃ¼r den Zugriff auf eine URL eine Definierte UserID +
	 * Passwort
	 */
	private String getAccessByUser() {
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
