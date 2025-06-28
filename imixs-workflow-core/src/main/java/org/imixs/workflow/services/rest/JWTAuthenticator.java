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
