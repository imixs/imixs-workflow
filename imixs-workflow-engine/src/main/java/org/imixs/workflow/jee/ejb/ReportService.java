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

package org.imixs.workflow.jee.ejb;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;

/**
 * The ReportService supports methods to create, process and find report
 * instances.
 * 
 * A Report Entity is identified by its name represented by the attribute
 * 'txtname' So a ReportService Implementation should ensure that txtname is a
 * unique key for the report entity.
 * 
 * Also each report entity holds a EQL Query in the attribute "txtquery". this
 * eql statement will be processed by the processQuery method and should return
 * a collection of entities defined by the query.
 * 
 * 
 * @author Ralph Soika
 * 
 */

@DeclareRoles({ "org.imixs.ACCESSLEVEL.NOACCESS",
		"org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS",
		"org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RolesAllowed({ "org.imixs.ACCESSLEVEL.NOACCESS",
		"org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS",
		"org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
@LocalBean
public class ReportService implements ReportServiceRemote {

	@EJB
	EntityService entityService;

	/**
	 * Returns a Report Entity identified by the attribute txtname
	 * 
	 * @param aReportName
	 *            - name of the report
	 * @return ItemCollection representing the Report
	 * @throws Exception
	 */
	public ItemCollection getReport(String aReportName) {

		ItemCollection itemCol = findReport(aReportName);
		return itemCol;
	}

	/**
	 * This method returns a collection of reports (ItemCollection). The method
	 * should return a subset of a collection if the start and count parameters
	 * differ form the value -1.
	 * 
	 * The method returns only ItemCollections the call has sufficient read
	 * access for.
	 */
	public List<ItemCollection> getReportList(int startpos, int count) {
		String sQuery = null;
		sQuery = "SELECT";
		sQuery += " wi FROM Entity as wi " + "WHERE wi.type = 'ReportEntity'";

		List<ItemCollection> col = entityService.findAllEntities(sQuery,
				startpos, count);

		return col;
	}

	/**
	 * updates a Entity Report Object. The Entity representing a report must
	 * have at least the attributes : txtQuery, numMaxCount, numStartPost,
	 * txtName.
	 * 
	 * txtName is the unique key to be use to get a query.
	 * 
	 * The method checks if a report with the same key allready exists. If so
	 * this report will be updated. If no report exists the new report will be
	 * created
	 * 
	 * @param report
	 * @throws InvalidItemValueException
	 * @throws AccessDeniedException
	 * 
	 */
	public void updateReport(ItemCollection aReport)
			throws  AccessDeniedException {

		aReport.replaceItemValue("type", "ReportEntity");

		// check if Report has a $uniqueid
		String sUniqueID = aReport.getItemValueString("$uniqueID");
		// if not try to find report by its name
		if ("".equals(sUniqueID)) {
			String sReportName = aReport.getItemValueString("txtName");
			// try to find existing Report by name.
			ItemCollection oldReport = findReport(sReportName);
			if (oldReport != null) {
				// old Report exists allready
				aReport = updateReport(aReport, oldReport);
			}
		}

		entityService.save(aReport);
	}

	/**
	 * Process a QueryEntity Object identified by the attribute txtname. All
	 * informations about the Query are stored in the QueryObject these
	 * attributes are: txtQuery, numMaxCount, numStartPost, txtName
	 * 
	 * 
	 * @param aID
	 * @return
	 * @throws Exception
	 */
	public List<ItemCollection> processReport(String aReportName) {
		// Load Query Object
		ItemCollection itemCol = findReport(aReportName);
		String sQuery = itemCol.getItemValueString("txtQuery");
		int istartPos = itemCol.getItemValueInteger("numStartPos");
		int imaxcount = itemCol.getItemValueInteger("numMaxCount");
		if (imaxcount == 0)
			imaxcount = -1;

		List<ItemCollection> col = entityService.findAllEntities(sQuery,
				istartPos, imaxcount);
		return col;
	}

	/**
	 * helper method returns a QueryEntity identified by its name or uniqueID
	 * 
	 * @param aid
	 * @return
	 */
	private ItemCollection findReport(String aid) {
		String sQuery = null;
		sQuery = "SELECT";
		sQuery += " wi FROM Entity as wi " + "JOIN wi.textItems as i "
				+ "WHERE (wi.id='" + aid + "') OR "
				+ "(i.itemName = 'txtname' " + "AND i.itemValue = '" + aid
				+ "') " + " AND wi.type = 'ReportEntity'";

		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				0, 1);
		if (col.size() > 0)
			return col.iterator().next();
		else
			return null;
	}

	/**
	 * This methode updates the a itemCollection with the attributes supported
	 * by another itemCollection without the $uniqueid
	 * 
	 * @param aworkitem
	 * 
	 */
	private ItemCollection updateReport(ItemCollection newReport,
			ItemCollection oldReport)  {
		Iterator iter = newReport.getAllItems().entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry mapEntry = (Map.Entry) iter.next();
			String sName = mapEntry.getKey().toString();
			Object o = mapEntry.getValue();
			if (isValidAttributeName(sName)) {
				oldReport.replaceItemValue(sName, o);
			}
		}
		return oldReport;
	}

	/**
	 * This method returns true if the attribute name can be updated by a
	 * client. Workflow Attributes are not valid
	 * 
	 * @param aName
	 * @return
	 */
	private boolean isValidAttributeName(String aName) {
		if ("namcreator".equalsIgnoreCase(aName))
			return false;
		if ("$created".equalsIgnoreCase(aName))
			return false;
		if ("$modified".equalsIgnoreCase(aName))
			return false;
		if ("$uniqueID".equalsIgnoreCase(aName))
			return false;
		if ("$isAuthor".equalsIgnoreCase(aName))
			return false;

		return true;

	}
}
