/****************************************************************************
 * Copyright (c) 2022-2025 Imixs Software Solutions GmbH and others.
 * https://www.imixs.com
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * This Source Code may also be made available under the terms of the
 * GNU General Public License, version 2 or later (GPL-2.0-or-later),
 * which is available at https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-or-later
 ****************************************************************************/

package org.imixs.workflow.services.rest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
import java.util.logging.Level;
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
     * @param filter - request filter instance.
     */
    public void registerRequestFilter(RequestFilter filter) {
        logger.log(Level.FINEST, "......register new request filter: {0}", filter.getClass().getSimpleName());

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
     * <p>
     * If the URI is with protocol :// then the servcieEndpoint is set directly.
     * 
     * 
     * @param uri
     * @throws RestAPIException
     */
    void setServiceEndpoint(String uri) throws RestAPIException {
        // test for protocoll
        if (uri.contains("://")) {
            this.serviceEndpoint = uri;
            return;
        }

        if (rootURL == null) {
            throw new RestAPIException(0, "rootURL is null!");
        }

        // test for double /
        if (uri != null && uri.startsWith("/")) {
            uri = uri.substring(1);
        }

        // add root URL
        uri = rootURL + uri;

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
     * @param uri         - Rest Endpoint URI
     * @param dataString  - content
     * @param contentType - request MediaType
     * @return content
     * @throws RestAPIException
     * @throws Exception
     */
    public String post(String uri, String dataString, String contentType) throws RestAPIException {
        return post(uri, dataString, contentType, null);
    }

    /**
     * Posts a String data object with a specific Content-Type to a Rest Service URI
     * Endpoint. This method can be used to simulate different post scenarios.
     * <p>
     * The parameter 'contnetType' and 'acceptType' can be used to request and
     * accept specific media types.
     * 
     * @param uri         - Rest Endpoint URI
     * @param dataString  - content
     * @param contentType - request MediaType
     * @param acceptType  - accept MediaType
     * @return content
     * @throws RestAPIException
     */
    public String post(String uri, String dataString, final String _contentType, String acceptType)
            throws RestAPIException {
        PrintWriter printWriter = null;
        String contentType = _contentType;

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

            iLastHTTPResult = urlConnection.getResponseCode();
            logger.log(Level.FINEST, "......Sending ''POST'' request to URL : {0}", serviceEndpoint);
            logger.log(Level.FINEST, "......Response Code : {0}", iLastHTTPResult);

            // read response if response was successful
            if (iLastHTTPResult >= 200 && iLastHTTPResult <= 299) {
                return readResponse(urlConnection);
            } else {
                String error = new StringBuilder("Error ").append(iLastHTTPResult)
                        .append(" - failed POST request: '").append(uri).append("'").toString();
                logger.warning(error);
                throw new RestAPIException(iLastHTTPResult, error);
            }

        } catch (IOException ioe) {
            String error = new StringBuilder("Error POST request '").append(uri).append(" - ")
                    .append(ioe.getMessage()).toString();
            logger.warning(error);
            throw new RestAPIException(500, error, ioe);
        } finally {
            // Release current connection
            if (printWriter != null)
                printWriter.close();
        }
    }

    /**
     * Posts a byte array to a Rest Service URI Endpoint. This method can be used to
     * simulate different post scenarios.
     * <p>
     * The parameter 'contnetType' and 'acceptType' can be used to request and
     * accept specific media types.
     * 
     * @param uri         - Rest Endpoint URI
     * @param data        - content
     * @param contentType - request MediaType
     * @param acceptType  - accept MediaType
     * @return content
     * @throws RestAPIException
     */
    public String post(String uri, byte[] data, final String _contentType) throws RestAPIException {
        return post(uri, data, _contentType, null);
    }

    /**
     * Posts a byte array to a Rest Service URI Endpoint. This method can be used to
     * simulate different post scenarios.
     * <p>
     * The parameter 'contnetType' and 'acceptType' can be used to request and
     * accept specific media types.
     * 
     * @param uri         - Rest Endpoint URI
     * @param data        - content
     * @param contentType - request MediaType
     * @param acceptType  - accept MediaType
     * @return content
     * @throws RestAPIException
     */
    public String post(String uri, byte[] data, final String _contentType, String acceptType) throws RestAPIException {

        String contentType = _contentType;

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
            urlConnection.setAllowUserInteraction(true);

            /** * HEADER ** */
            urlConnection.setRequestProperty("Content-Type", contentType + "; charset=" + encoding);
            urlConnection.setRequestProperty("Accept-Charset", encoding);
            urlConnection.setRequestProperty("Accept", acceptType);

            if (requestProperties != null) {
                for (Map.Entry<String, String> entry : requestProperties.entrySet()) {
                    urlConnection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            // process filters....
            for (RequestFilter filter : requestFilterList) {
                filter.filter(urlConnection);
            }

            // transfer data
            OutputStream outputStreamToRequestBody = urlConnection.getOutputStream();
            BufferedWriter httpRequestBodyWriter = new BufferedWriter(
                    new OutputStreamWriter(outputStreamToRequestBody));
            outputStreamToRequestBody.write(data); // , 0, bytesRead);
            outputStreamToRequestBody.flush();
            // Close the streams
            outputStreamToRequestBody.close();
            httpRequestBodyWriter.close();

            iLastHTTPResult = urlConnection.getResponseCode();
            logger.log(Level.FINEST, "......Sending ''POST'' request to URL : {0}", serviceEndpoint);
            logger.log(Level.FINEST, "......Response Code : {0}", iLastHTTPResult);

            // read response if response was successful
            if (iLastHTTPResult >= 200 && iLastHTTPResult <= 299) {
                return readResponse(urlConnection);
            } else {
                String error = new StringBuilder("Error ").append(iLastHTTPResult)
                        .append(" - failed POST request: '").append(uri).append("'").toString();
                logger.warning(error);
                throw new RestAPIException(iLastHTTPResult, error);
            }

        } catch (IOException ioe) {
            String error = new StringBuilder("Error POST request '")
                    .append(uri).append(" - ").append(ioe.getMessage()).toString();
            logger.warning(error);
            throw new RestAPIException(500, error, ioe);
        } finally {

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
     * @param uri - Rest Endpoint RUI
     * @return - content or null if no content is available.
     */
    public String get(String uri) throws RestAPIException {

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

            iLastHTTPResult = urlConnection.getResponseCode();
            logger.log(Level.FINEST, "......Sending ''GET'' request to URL : {0}", serviceEndpoint);
            logger.log(Level.FINEST, "......Response Code : {0}", iLastHTTPResult);
            // read response if response was successful
            if (iLastHTTPResult >= 200 && iLastHTTPResult <= 299) {
                return readResponse(urlConnection);
            } else {
                String error = new StringBuilder("Error ").append(iLastHTTPResult)
                        .append(" - failed GET request from '").append(uri).append("'").toString();
                logger.warning(error);
                throw new RestAPIException(iLastHTTPResult, error);
            }
        } catch (IOException e) {
            String error = new StringBuilder("Error GET request from '")
                    .append(uri).append(" - ").append(e.getMessage()).toString();
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
                logger.log(Level.FINEST, "......{0}", inputLine);
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
