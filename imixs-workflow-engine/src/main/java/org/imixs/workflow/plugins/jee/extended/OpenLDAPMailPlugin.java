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

package org.imixs.workflow.plugins.jee.extended;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.imixs.workflow.plugins.jee.MailPlugin;

/**
 * This Plugin extends the standard JEE MailPlugin to lookup smtp mail addresses from an OpenLDAP 
 * directory by a given username. Therefore the Plugin expects an JNDI Ressource to the
 * corresponding directory identified by the jndi name:
 * 'org.imixs.mail.directory'
 * <p>
 * The search phrase is <code>(uid=%u)</code> where %u is the username to be
 * translated into a smtp address.
 * 
 * 
 * 
 * @author rsoika
 * 
 */
public class OpenLDAPMailPlugin extends MailPlugin {

	public static final String LDAP_JNDI_NAME = "org.imixs.mail.directory";

	/**
	 * this method tries to lookup a users smtp address from the ldap directory
	 * if no smtp name is provided. The method overwrite the standard behaivor
	 * form the MailPlugin Class.
	 * 
	 * @param aAddr
	 * @return
	 * @throws AddressException
	 */
	public InternetAddress getInternetAddress(String aAddr)
			throws AddressException {

		// is smtp address provided?
		if (aAddr.indexOf('@') > -1)
			// yes - return imitate 
			return super.getInternetAddress(aAddr);

		// try to get email from ldap directory....
		try {
			aAddr = fetchEmail(aAddr);
		} catch (NamingException e) {
			// no valid email was found!
			System.out.println("OpenLDAPMailPlugin: mail for '" + aAddr
					+ "' not found");
			//e.printStackTrace();
		}
		return new InternetAddress(aAddr);
	}

	/**
	 * This method searches a mail address for a specific username.
	 * 
	 * @param aUsername
	 * @return
	 * @throws NamingException
	 */
	@SuppressWarnings("rawtypes")
	private String fetchEmail(String aUsername) throws NamingException {
		Context initCtx = new InitialContext();
		DirContext ldapCtx = (DirContext) initCtx.lookup(LDAP_JNDI_NAME);

		
		SearchControls ctls = new SearchControls();
		ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);

		String searchfilter = "";
		searchfilter = "(uid=" + aUsername + ")";

		NamingEnumeration answer = ldapCtx.search("", searchfilter, ctls);

		if (answer.hasMore()) {
			SearchResult entry = (SearchResult) answer.next();
			Attributes attrs = entry.getAttributes();
			// read mail Attribute
			Attribute attr = attrs.get("mail");
			if (attr != null)
				return (String) attr.get(0);
		}

		return aUsername;
	}
}
