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

package org.imixs.workflow.engine;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;


/**
 * This singleton ejb provides a service to access the imxis.property file. This
 * file can be packaged together with an application in any ejb module.
 * 
 * @version 1.0
 * @author rsoika
 */
@Singleton
public class PropertyService {

	private Properties properties = null;

	private static Logger logger = Logger.getLogger(PropertyService.class.getName());

	/**
	 * PostContruct event - loads the imixs.properties.
	 */
	@PostConstruct
	void init() {
		loadProperties();
	}

	/**
	 * Return the imixs property object
	 * 
	 * @return - current instance of Properties
	 */
	public Properties getProperties() {
		return properties;
	}

	/**
	 * resets and reload the property file
	 */
	public void reset() {
		logger.finest("......reset properties....");

		properties = null;
		loadProperties();
	}

	/**
	 * loads a imixs.property file
	 * 
	 * (located at current threads classpath)
	 * 
	 */
	private void loadProperties() {
		properties = new Properties();
		try {			 
			properties.load(Thread.currentThread().getContextClassLoader()
					.getResource("imixs.properties").openStream());
		} catch (Exception e) {
			logger.warning("PropertyService unable to find imixs.properties in current classpath");
			if (logger.isLoggable(Level.FINE)){
				e.printStackTrace();
			}
		}

	}

}
