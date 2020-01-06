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
