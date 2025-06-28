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
import java.util.Base64;

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
        sURLAccess = Base64.getEncoder().encodeToString(sUserCode.getBytes());

        return sURLAccess;
    }

}
