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
