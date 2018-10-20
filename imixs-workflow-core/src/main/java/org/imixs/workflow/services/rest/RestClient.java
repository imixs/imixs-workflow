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
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.xml.XMLDataCollection;
import org.imixs.workflow.xml.XMLDataCollectionAdapter;
import org.imixs.workflow.xml.XMLDocument;
import org.imixs.workflow.xml.XMLDocumentAdapter;

/**
 * The Imixs RestClient encapsulates the communication with the Imixs Rest API.
 * The Implementation is based on the JAXB API.
 * <p>
 * The Imixs RestClient provides methods to GET and POST XMLDataCollection
 * objects.
 * <p>
 * The client throws a RestAPIException in case of an communication error.
 * 
 * @see org.imixs.workflow.jee.rest
 * @author Ralph Soika
 * 
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
	 * @param filter - request filter instance.
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

	public void setServiceEndpoint(String serviceEndpoint) {
		this.serviceEndpoint = serviceEndpoint;
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
	 * This method posts an XMLDocument in the Imixs XML Format to a Rest Service
	 * URI Endpoint.
	 * 
	 * @param uri       - Rest Endpoint RUI
	 * @param entityCol - an Entity Collection
	 * @return HTTPResult
	 */
	public XMLDocument postXMLDocument(String uri, XMLDocument aItemCol) throws Exception {
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

			/** * HEADER ** */
			urlConnection.setRequestProperty("Content-Type", "application/xml; charset=" + encoding);

			// process filters....
			for (RequestFilter filter : requestFilterList) {
				filter.filter(urlConnection);
			}

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

				if (iLastHTTPResult >= 200 && iLastHTTPResult <= 299) {
					String content = readResponse(urlConnection);

					XMLDocument xmlDocument = XMLDocumentAdapter.readXMLDocument(content.getBytes());
					return xmlDocument;

				}

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

		return null;
	}

	/**
	 * This method posts an Entitycollection in the Imixs XML Format to a Rest
	 * Service URI Endpoint.
	 * 
	 * 
	 * @param uri       - Rest Endpoint RUI
	 * @param entityCol - an Entity Collection
	 * @return HTTPResult
	 */
	public void postCollection(String uri, XMLDataCollection aEntityCol) throws Exception {
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

			/** * HEADER ** */
			urlConnection.setRequestProperty("Content-Type", "application/xml; charset=" + encoding);

			// process filters....
			for (RequestFilter filter : requestFilterList) {
				filter.filter(urlConnection);
			}

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
			
			
			// get content of result
			readResponse(urlConnection);
			
			
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

		
	}

	/**
	 * This method posts an JSON String in the Imixs ItemCollection Format to a Rest
	 * Service URI Endpoint.
	 * 
	 * 
	 * @param uri       - Rest Endpoint RUI
	 * @param entityCol - an Entity Collection
	 * @return XMLDocument
	 */
	public XMLDocument postJsonEntity(String uri, String aItemColString) throws Exception {
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

			/** * HEADER ** */
			urlConnection.setRequestProperty("Content-Type", "application/json; charset=" + encoding);
			// process filters....
			for (RequestFilter filter : requestFilterList) {
				filter.filter(urlConnection);
			}

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

				if (iLastHTTPResult >= 200 && iLastHTTPResult <= 299) {
					String content = readResponse(urlConnection);

					XMLDocument xmlDocument = XMLDocumentAdapter.readXMLDocument(content.getBytes());
					return xmlDocument;

				}

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

		return null;
	}

	/**
	 * This method posts a String data object with a specific Content-Type to a Rest
	 * Service URI Endpoint. This method can be used to simulate different post
	 * scenarios.
	 * 
	 * 
	 * 
	 * @param uri       - Rest Endpoint RUI
	 * @param entityCol - an Entity Collection
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

			/** * HEADER ** */
			urlConnection.setRequestProperty("Content-Type", contentType + "; charset=" + encoding);

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
	 * This method get the content of a GET request from a Rest Service URI
	 * Endpoint. I case of an error the method throws a RestAPIException.
	 * 
	 * @param uri - Rest Endpoint RUI
	 * @return - content or null if no content is available.
	 */
	public String get(String uri) throws RestAPIException {
		int responseCode = -1;

		// test for /
		if (rootURL != null && !rootURL.endsWith("/")) {
			rootURL += "/";
		}
		// test for double /
		if (rootURL.endsWith("/") && uri != null && uri.startsWith("/")) {
			uri = uri.substring(1);
		}
		uri = rootURL + uri;

		try {
			URL url = new URL(uri);

			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

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
			logger.finest("......Sending 'GET' request to URL : " + uri);
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
	 * Returns a list of ItemCollections
	 * 
	 * @return
	 * @throws Exception
	 */
	public List<ItemCollection> getDocumentCollection(String url) throws RestAPIException {
		XMLDataCollection xmlDocuments = getXMLDataCollection(url);
		// convert xmldatacollection into a list of ItemCollection objects
		List<ItemCollection> documents = XMLDataCollectionAdapter.putDataCollection(xmlDocuments);
		return documents;

	}

	/**
	 * Returns a list of XMLDocuments from a XML data source
	 * 
	 * @return
	 * @throws RestAPIException
	 * @throws Exception
	 */
	public XMLDataCollection getXMLDataCollection(String url) throws RestAPIException {
		XMLDataCollection xmlDocuments = null;
		
		setRequestProperty("Accept", MediaType.APPLICATION_XML);
		String xmlResult = this.get(url);

		if (xmlResult == null || xmlResult.isEmpty()) {
			// no content!
			logger.finest("......no content...");
			return null;
		} else {
			// convert into ItemCollection list
			try {
				JAXBContext context;
				context = JAXBContext.newInstance(XMLDataCollection.class);
				Unmarshaller u = context.createUnmarshaller();
				StringReader reader = new StringReader(xmlResult);
				xmlDocuments = (XMLDataCollection) u.unmarshal(reader);
			} catch (JAXBException e) {
				String error = "Error GET request from '" + url + " - " + e.getMessage();
				logger.warning(error);
				throw new RestAPIException(0, error, e);
			}
			return xmlDocuments;
		}
	}

	/**
	 * Returns a ItemCollection
	 * 
	 * @return
	 * @throws Exception
	 */
	public ItemCollection getDocument(String url) throws RestAPIException {
		XMLDocument xmlDocument = getXMLDocument(url);
		// convert xmldocument into a ItemCollection object
		ItemCollection document = XMLDocumentAdapter.putDocument(xmlDocument);
		return document;
	}

	/**
	 * Returns a XMLDocument from a XML data source
	 * 
	 * @return
	 * @throws Exception
	 */
	public XMLDocument getXMLDocument(String url) throws RestAPIException {
		XMLDocument xmlDocument = null;
		this.setRequestProperty("Accept", MediaType.APPLICATION_XML);
		String xmlResult = this.get(url);
		try {
			// convert into ItemCollection list
			JAXBContext context = JAXBContext.newInstance(XMLDocument.class);
			// JAXBContext context = JAXBContext.newInstance( "org.imixs.workflow.xml" );

			Unmarshaller u = context.createUnmarshaller();
			StringReader reader = new StringReader(xmlResult);
			xmlDocument = (XMLDocument) u.unmarshal(reader);
		} catch (JAXBException e) {
			String error = "Error GET request from '" + url + " - " + e.getMessage();
			logger.warning(error);
			throw new RestAPIException(0, error, e);
		}
		return xmlDocument;

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
