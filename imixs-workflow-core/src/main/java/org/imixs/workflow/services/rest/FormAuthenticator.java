/*  
 *  Imixs-Workflow 
 *  
 *  Copyright (C) 2001-2020 Imixs Software Solutions GmbH,  
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
 *      https://www.imixs.org
 *      https://github.com/imixs/imixs-workflow
 *  
 *  Contributors:  
 *      Imixs Software Solutions GmbH - Project Management
 *      Ralph Soika - Software Developer
 */

package org.imixs.workflow.services.rest;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

/**
 * This RequestFilter performs a form based authentication. The filter can be
 * used with a javax.ws.rs.client.Client.
 * 
 * @author rsoika
 *
 */
public class FormAuthenticator implements RequestFilter {

    private List<HttpCookie> cookies;
    private final String USER_AGENT = "Mozilla/5.0";
    private final static Logger logger = Logger.getLogger(FormAuthenticator.class.getName());

    public FormAuthenticator(String baseUri, String username, String password) {
        // extend the base uri with /j_security_check....
        baseUri += "/j_security_check";
        logger.finest("......baseUIR= " + baseUri);
        // Access secure page on server. In response to this request we will receive
        // the JSESSIONID to be used for further requests.
        try {
            // Instantiate CookieManager;
            // make sure to set CookiePolicy
            CookieManager manager = new CookieManager();
            manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            CookieHandler.setDefault(manager);
            // create the httpURLConnection...
            URL obj = new URL(baseUri);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestProperty("Connection", "close");

            // add request header
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", USER_AGENT);
            // add Post parameters
            String urlParameters = "j_username=" + username + "&j_password=" + password;
            // Send post request
            con.setDoOutput(true);
            con.setDoInput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();
            int responseCode = con.getResponseCode();
            logger.finest(".....Response Code : " + responseCode);
            con.connect();
            // get cookies from underlying CookieStore
            CookieStore cookieJar = manager.getCookieStore();
            cookies = cookieJar.getCookies();

            // get stream and read from it, just to close the response which is important
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

        } catch (IOException e) {
            // something went wrong...
            e.printStackTrace();
        }

    }

    /**
     * In the filter method we put the cookies form the login into the request.
     */
    public void filter(HttpURLConnection connection) throws IOException {
        if (cookies != null && cookies.size() > 0) {
            String values = "";
            for (HttpCookie acookie : cookies) {
                values = values + acookie + ",";
            }
            connection.setRequestProperty("Cookie", values);
        }
    }

}
