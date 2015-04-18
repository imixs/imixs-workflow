package org.imixs.workflow.bpmn;

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

import junit.framework.Assert;

import org.imixs.workflow.util.Base64;
import org.junit.Test;

public class TestClient {

	@Test
	public void postData() {
		PrintWriter printWriter = null;

		HttpURLConnection urlConnection = null;
		try { 
 
			urlConnection = (HttpURLConnection) new URL(
					" http://localhost:8080/workflow/rest-service/model/xxxbpmn")
					.openConnection();
			urlConnection.setRequestMethod("POST");
			urlConnection.setDoOutput(true);
			urlConnection.setDoInput(true);
			urlConnection.setAllowUserInteraction(false);

			// Authorization

			urlConnection.setRequestProperty("Authorization",
					"Basic " + this.getAccessByUser());

			/** * HEADER ** */
			urlConnection.setRequestProperty("Content-Type",
					"application/xml; charset=" + "UTF-8");

			StringWriter writer = new StringWriter();

			writer.write("<bpmn2:definitions>xxx</bpmn2:definitions>");
			writer.flush();

			// compute length
			urlConnection.setRequestProperty("Content-Length",
					"" + Integer.valueOf(writer.toString().getBytes().length));

			printWriter = new PrintWriter(new BufferedWriter(
					new OutputStreamWriter(urlConnection.getOutputStream(),
							"UTF-8")));

			printWriter.write(writer.toString());
			printWriter.close();

			String sHTTPResponse = urlConnection.getHeaderField(0);

			// get content of result
			readResponse(urlConnection);
			
		} catch (Exception ioe) {
			// ioe.printStackTrace();
			Assert.fail();
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
		StringWriter writer = new StringWriter();
		BufferedReader in = null;
		try {
			// test if content encoding is provided
			String sContentEncoding = urlConnection.getContentEncoding();
			if (sContentEncoding == null || sContentEncoding.isEmpty()) {
				// no so lets see if the client has defined an encoding..
				
			}

			// if an encoding is provided read stream with encoding.....
			if (sContentEncoding != null && !sContentEncoding.isEmpty())
				in = new BufferedReader(new InputStreamReader(
						urlConnection.getInputStream(), sContentEncoding));
			else
				in = new BufferedReader(new InputStreamReader(
						urlConnection.getInputStream()));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
			
				writer.write(inputLine);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (in != null)
				in.close();
		}

		
	}


	/**
	 * Diese Methode setzt fÃ¼r den Zugriff auf eine URL eine Definierte UserID
	 * + Passwort
	 */
	private String getAccessByUser() {
		String sURLAccess = "";
		// UserID:Passwort
		String sUserCode = "anna:demo";
		// String convertieren
		// sURLAccess = Base64.encodeBase64(sUserCode.getBytes()).toString();
		char[] authcode = Base64.encode(sUserCode.getBytes());

		sURLAccess = String.valueOf(authcode);
		return sURLAccess;
	}
}
