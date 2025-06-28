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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This RequestFilter performs a form based authentication. The filter can be
 * used with a jakarta.ws.rs.client.Client.
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
        logger.log(Level.FINEST, "......baseUIR= {0}", baseUri);
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
            logger.log(Level.FINEST, ".....Response Code : {0}", responseCode);
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
