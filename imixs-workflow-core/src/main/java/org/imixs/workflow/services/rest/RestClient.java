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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.imixs.workflow.util.Base64;
import org.imixs.workflow.xml.EntityCollection;
import org.imixs.workflow.xml.XMLItemCollection;

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

	// private String NAME_SPACE = "http://imixs.org/workflow/services";

	private String serviceEndpoint;

	private String sUser = null;

	private String sPassword = null;

	private String encoding = "UTF-8";

	private int iLastHTTPResult = 0;
	
	private String content=null;

	// Sets credentials
	public void setCredentials(String auser, String apw) {
		sUser = auser;
		sPassword = apw;
	}

	public void setEncoding(String aEncoding) {
		encoding = aEncoding;
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
	public int postEntity(String uri, XMLItemCollection aItemCol)
			throws Exception {
		PrintWriter printWriter = null;

		HttpURLConnection urlConnection = null;
		try {
			serviceEndpoint = uri;
			iLastHTTPResult = 500;

			urlConnection = (HttpURLConnection) new URL(serviceEndpoint)
					.openConnection();
			urlConnection.setRequestMethod("POST");
			urlConnection.setDoOutput(true);
			urlConnection.setDoInput(true);
			urlConnection.setAllowUserInteraction(false);

			// Authorization
			if (sUser != null) {
				urlConnection.setRequestProperty("Authorization", "Basic "
						+ this.getAccessByUser());
			}
			/** * HEADER ** */
			urlConnection.setRequestProperty("Content-Type",
					"application/xml; charset=" + encoding);

			StringWriter writer = new StringWriter();

			JAXBContext context = JAXBContext
					.newInstance(XMLItemCollection.class);
			Marshaller m = context.createMarshaller();
			m.marshal(aItemCol, writer);

			// System.out.println(writer.toString());

			// compute length
			urlConnection.setRequestProperty("Content-Length",
					"" + Integer.valueOf(writer.toString().getBytes().length));

			printWriter = new PrintWriter(new BufferedWriter(
					new OutputStreamWriter(urlConnection.getOutputStream(),
							encoding)));

			printWriter.write(writer.toString());
			printWriter.close();

			String sHTTPResponse = urlConnection.getHeaderField(0);
			try {
				iLastHTTPResult = Integer.parseInt(sHTTPResponse.substring(9,
						12));
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
	public int postCollection(String uri, EntityCollection aEntityCol)
			throws Exception {
		PrintWriter printWriter = null;

		HttpURLConnection urlConnection = null;
		try {
			serviceEndpoint = uri;
			iLastHTTPResult = 500;

			urlConnection = (HttpURLConnection) new URL(serviceEndpoint)
					.openConnection();
			urlConnection.setRequestMethod("POST");
			urlConnection.setDoOutput(true);
			urlConnection.setDoInput(true);
			urlConnection.setAllowUserInteraction(false);

			// Authorization
			if (sUser != null) {
				urlConnection.setRequestProperty("Authorization", "Basic "
						+ this.getAccessByUser());
			}

			/** * HEADER ** */
			urlConnection.setRequestProperty("Content-Type",
					"application/xml; charset=" + encoding);

			StringWriter writer = new StringWriter();

			JAXBContext context = JAXBContext
					.newInstance(EntityCollection.class);
			Marshaller m = context.createMarshaller();
			m.marshal(aEntityCol, writer);

			// System.out.println(writer.toString());

			// compute length
			urlConnection.setRequestProperty("Content-Length",
					"" + Integer.valueOf(writer.toString().getBytes().length));

			printWriter = new PrintWriter(new BufferedWriter(
					new OutputStreamWriter(urlConnection.getOutputStream(),
							encoding)));

			printWriter.write(writer.toString());
			printWriter.close();
			String sHTTPResponse = urlConnection.getHeaderField(0);
			try {
				iLastHTTPResult = Integer.parseInt(sHTTPResponse.substring(9,
						12));
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
	 * This method posts an JSON String in the Imixs ItemCollection Format to a
	 * Rest Service URI Endpoint.
	 * 
	 * 
	 * @param uri
	 *            - Rest Endpoint RUI
	 * @param entityCol
	 *            - an Entity Collection
	 * @return HTTPResult
	 */
	public int postJsonEntity(String uri, String aItemColString)
			throws Exception {
		PrintWriter printWriter = null;

		HttpURLConnection urlConnection = null;
		try {
			serviceEndpoint = uri;
			iLastHTTPResult = 500;

			urlConnection = (HttpURLConnection) new URL(serviceEndpoint)
					.openConnection();
			urlConnection.setRequestMethod("POST");
			urlConnection.setDoOutput(true);
			urlConnection.setDoInput(true);
			urlConnection.setAllowUserInteraction(false);

			// Authorization
			if (sUser != null) {
				urlConnection.setRequestProperty("Authorization", "Basic "
						+ this.getAccessByUser());
			}
			/** * HEADER ** */
			urlConnection.setRequestProperty("Content-Type",
					"application/json; charset=" + encoding);

			StringWriter writer = new StringWriter();

			writer.write(aItemColString);
			writer.flush();

			// compute length
			urlConnection.setRequestProperty("Content-Length",
					"" + Integer.valueOf(writer.toString().getBytes().length));

			printWriter = new PrintWriter(new BufferedWriter(
					new OutputStreamWriter(urlConnection.getOutputStream(),
							encoding)));

			printWriter.write(writer.toString());
			printWriter.close();

			String sHTTPResponse = urlConnection.getHeaderField(0);
			try {
				iLastHTTPResult = Integer.parseInt(sHTTPResponse.substring(9,
						12));
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
	 * Put the resonse string into the content property
	 * @param urlConnection
	 * @throws IOException
	 */
	private void readResponse(URLConnection urlConnection) throws IOException {
		// get content of result
		StringWriter writer = new StringWriter();
		BufferedReader in=null;
		try {
			in = new BufferedReader(new InputStreamReader(
					urlConnection.getInputStream()));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				System.out.println(inputLine);
				writer.write(inputLine);
			}
		} catch (IOException e) {
			
			e.printStackTrace();
		} finally {
			if (in!=null)
				in.close();
		}
		
		
		setContent(writer.toString());

	}
	
	
	/**
	 * Diese Methode setzt für den Zugriff auf eine URL eine Definierte UserID +
	 * Passwort
	 */
	private String getAccessByUser() {
		String sURLAccess = "";
		// UserID:Passwort
		String sUserCode = sUser + ":" + sPassword;
		// String convertieren
		// sURLAccess = Base64.encodeBase64(sUserCode.getBytes()).toString();
		char[] authcode = Base64.encode(sUserCode.getBytes());

		sURLAccess = String.valueOf(authcode);
		return sURLAccess;
	}

}
