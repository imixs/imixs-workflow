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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.util.XMLParser;

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

@DeclareRoles({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RolesAllowed({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
@LocalBean
public class ReportService implements ReportServiceRemote {

	private static Logger logger = Logger.getLogger(ReportService.class.getName());

	
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

		List<ItemCollection> col = entityService.findAllEntities(sQuery, startpos, count);

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
	public void updateReport(ItemCollection aReport) throws AccessDeniedException {

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
	 * This method executes the JQPL statement of a Report Entity.
	 * 
	 * 
	 * @param reportName
	 *            - name of the report to be executed
	 * @return
	 * @throws Exception
	 */
	public List<ItemCollection> executeReport(String reportName, int istartPos, int imaxcount,
			Map<String, String> params) {
		
		logger.fine("[ReportService] executeReport: "+reportName);
		
		// Load Query Object
		ItemCollection itemCol = findReport(reportName);
		String query = itemCol.getItemValueString("txtQuery");
		if (imaxcount == 0)
			imaxcount = -1;

		// replace params in query statement
		if (params != null) {
			Set<String> keys = params.keySet();
			Iterator<String> iter = keys.iterator();
			while (iter.hasNext()) {
				// read key
				String sKeyName = iter.next().toString();
				// test if key is contained in query
				if (query.indexOf("?" + sKeyName) > -1) {
					String sParamValue = params.get(sKeyName);
					query = query.replace("?" + sKeyName, sParamValue);
					logger.fine("[ReportService] executeReport set param "+sKeyName + "="+sParamValue);
				}
			}
		}

		// now we replace dynamic Date values
		query =  replaceDateString(query);

		// execute query
		logger.fine("[ReportService] executeReport jpql="+query);
		List<ItemCollection> col = entityService.findAllEntities(query, istartPos, imaxcount);
		return col;
	}
	
	
	/**
	 * This method parses a <date /> xml tag and computes a dynamic date by
	 * parsing the attributes:
	 * 
	 * DAY_OF_MONTH
	 * 
	 * DAY_OF_YEAR
	 * 
	 * MONTH
	 * 
	 * YEAR
	 * 
	 * ADD (FIELD,OFFSET)
	 * 
	 * e.g. <date DAY_OF_MONTH="1" MONTH="2" />
	 * 
	 * results in 1. February of the current year
	 * 
	 * 
	 *
	 * <date DAY_OF_MONTH="ACTUAL_MAXIMUM" MONTH="12" ADD="MONTH,-1" />
	 * 
	 * results in 30.November of current year
	 * 
	 * @param xmlDate
	 * @return
	 */
	public static Calendar computeDynamicDate(String xmlDate) {
		Calendar cal = Calendar.getInstance();

		Map<String, String> attributes = XMLParser.findAttributes(xmlDate);

		// test MONTH
		if (attributes.containsKey("MONTH")) {
			String value = attributes.get("MONTH");
			if ("ACTUAL_MAXIMUM".equalsIgnoreCase(value)) {
				// last month of year
				cal.set(Calendar.MONTH, cal.getActualMaximum(Calendar.MONTH));
			} else {
				cal.set(Calendar.MONTH, Integer.parseInt(value) - 1);
			}
		}

		// test YEAR
		if (attributes.containsKey("YEAR")) {
			String value = attributes.get("YEAR");
			cal.set(Calendar.YEAR, Integer.parseInt(value));

		}

		// test DAY_OF_MONTH
		if (attributes.containsKey("DAY_OF_MONTH")) {
			String value = attributes.get("DAY_OF_MONTH");
			if ("ACTUAL_MAXIMUM".equalsIgnoreCase(value)) {
				// last day of month
				cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
			} else {
				cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(value));
			}
		}

		// test DAY_OF_YEAR
		if (attributes.containsKey("DAY_OF_YEAR")) {
			cal.set(Calendar.DAY_OF_YEAR, Integer.parseInt(attributes.get("DAY_OF_YEAR")));
		}

		// test ADD
		if (attributes.containsKey("ADD")) {
			String value = attributes.get("ADD");
			String[] fieldOffset = value.split(",");

			String field = fieldOffset[0];
			int offset = Integer.parseInt(fieldOffset[1]);

			if ("MONTH".equalsIgnoreCase(field)) {
				cal.add(Calendar.MONTH, offset);
			} else if ("DAY_OF_MONTH".equalsIgnoreCase(field)) {
				cal.add(Calendar.DAY_OF_MONTH, offset);
			} else if ("DAY_OF_YEAR".equalsIgnoreCase(field)) {
				cal.add(Calendar.DAY_OF_YEAR, offset);
			}

		}

		return cal;

	}

	/**
	 * This method replaces all occurrences of <date> tags with the
	 * corresponding dynamic date. See computeDynamicdate.
	 * 
	 * @param content
	 * @return
	 */
	public static String replaceDateString(String content) {

		List<String> dates = XMLParser.findTags(content, "date");
		for (String dateString : dates) {
			Calendar cal = computeDynamicDate(dateString);
			// convert into ISO format
			DateFormat f = new SimpleDateFormat("yyyy-MM-dd");
			// f.setTimeZone(tz);
			String dateValue = f.format(cal.getTime());
			content = content.replace(dateString, dateValue);
		}

		return content;
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
		sQuery += " wi FROM Entity as wi " + "JOIN wi.textItems as i " + "WHERE (wi.id='" + aid + "') OR "
				+ "(i.itemName = 'txtname' " + "AND i.itemValue = '" + aid + "') " + " AND wi.type = 'ReportEntity'";

		Collection<ItemCollection> col = entityService.findAllEntities(sQuery, 0, 1);
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
	@SuppressWarnings("rawtypes")
	private ItemCollection updateReport(ItemCollection newReport, ItemCollection oldReport) {
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
