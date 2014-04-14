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

package org.imixs.workflow.jaxrs.plugins;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.jee.ejb.ReportService;
import org.imixs.workflow.jee.ejb.WorkflowService;
import org.imixs.workflow.plugins.jee.VersionPlugin;
import org.imixs.workflow.xml.EntityCollection;
import org.imixs.workflow.xml.XMLItemCollectionAdapter;

/**
 * This plugin handles the creation of a Imixs Reoprt. The coresponding
 * activityEntity provide the following properties:
 * <p>
 * <ul>
 * <li>
 * txtReportName=Name of the Report to be processed
 * <li>
 * txtReportFilePath= filename or filepath the result will be saved
 * <li>
 * txtReportTarget = where the result is saved (0=workitem, 1=blobWorkitem, 2=
 * disk)
 * 
 * 
 * 
 * 
 * 
 * @author Ralph Soika
 * @version 1.0
 */

public class ReportPlugin extends org.imixs.workflow.plugins.jee.AbstractPlugin {

	public static final String INVALID_CONTEXT = "INVALID_CONTEXT";
	public static final String REPORT_UNDEFINED = "REPORT_UNDEFINED";
	public static final String INVALID_REPORT_DEFINITION = "INVALID_REPORT_DEFINITION";

	private EntityService entityService = null;
	private WorkflowService workflowService = null;
	private ReportService reportService = null;

	private String reportName = null;
	private String reportFilePath = null;
	private String reportTarget = null;
	private String sEQL;
	private String sXSL;
	private String sContentType;
	private String sEncoding;
	private ItemCollection blobWorkitem = null;

	private static Logger logger = Logger.getLogger(ReportPlugin.class
			.getName());

	public void init(WorkflowContext actx) throws PluginException {
		super.init(actx);

		// check for an instance of WorkflowService
		if (actx instanceof WorkflowService) {
			// yes we are running in a WorkflowService EJB
			workflowService = (WorkflowService) actx;

			if (workflowService == null)
				throw new PluginException(VersionPlugin.class.getSimpleName(),
						INVALID_CONTEXT,
						"VersionPlugin unable to access WorkflowSerive");

			entityService = workflowService.getEntityService();
			reportService = workflowService.getReportService();
		}

	}

	/**
	 * creates report defined by the txtReportName.
	 * <p>
	 * The method runs the EQL Statement defined in the corepsonding Imixs
	 * Report to compute the collection of entities to be processed by a
	 * template.
	 * <p>
	 * As the current Workitem can not be identically included by the resultset
	 * of the EQL Statement (because the documentContext is yet not saved) the
	 * method tests if the resultset includes the current workitem. In this case
	 * the 'old' result will be replaced with the new currently processed
	 * workitem.
	 * 
	 * 
	 */
	public int run(ItemCollection adocumentContext,
			ItemCollection adocumentActivity) throws PluginException {

		reportName = adocumentActivity.getItemValueString("txtReportName");
		reportFilePath = adocumentActivity
				.getItemValueString("txtReportFilePath");
		if ("".equals(reportFilePath))
			reportFilePath = reportName;

		// replace dynamic field values
		reportFilePath = this.replaceDynamicValues(reportFilePath,
				adocumentContext);

		reportTarget = adocumentActivity.getItemValueString("txtReportTarget");

		if ("".equals(reportName))
			return PLUGIN_OK;

		// autocomplete file extention if missing
		if (!reportName.endsWith(".ixr"))
			reportName = reportName + ".ixr";

		ItemCollection itemCol = reportService.getReport(reportName);
		if (itemCol == null) {
			// report undefined
			throw new PluginException(ReportPlugin.class.getSimpleName(),
					REPORT_UNDEFINED,
					"Report '" + reportName + " is undefined",
					new Object[] { reportName });

		}

		// get Query and output format
		sEQL = itemCol.getItemValueString("txtquery");

		// compute dynamical params
		String sParamString = adocumentActivity
				.getItemValueString("txtReportParams");
		// replace field values...
		sParamString = this
				.replaceDynamicValues(sParamString, adocumentContext);

		// compute jpql statement
		sEQL = computeEQLParams(sEQL, sParamString);
		logger.info("ReportPlugin JPQL=" + sEQL);
		sXSL = itemCol.getItemValueString("txtXSL").trim();
		// if no XSL is provided return
		if ("".equals(sXSL))
			return PLUGIN_OK;

		sContentType = itemCol.getItemValueString("txtcontenttype");
		if ("".equals(sContentType))
			sContentType = "text/html";

		sEncoding = itemCol.getItemValueString("txtencoding");
		// no encoding defined so take a default encoding
		// (UTF-8)
		if ("".equals(sEncoding))
			sEncoding = "UTF-8";

		// query result ....
		Collection<ItemCollection> col = entityService.findAllEntities(sEQL, 0,
				-1);
		// now add the current Workitem into the collection if an older
		// version is
		// included in the result
		String sUnqiueID = adocumentContext.getItemValueString("$uniqueID");
		if (!"".equals(sUnqiueID)) {
			Collection<ItemCollection> colNew = new Vector<ItemCollection>();
			for (ItemCollection aitemCol : col) {
				if (sUnqiueID.equals(aitemCol.getItemValueString("$uniqueid"))) {

					ItemCollection itemTemp = new ItemCollection(
							adocumentContext.getAllItems());
					itemTemp.replaceItemValue("$temp", "true");
					colNew.add(itemTemp);
					logger.info(" ReportPlugin - relaced deprecated workitem from collection");
				} else
					colNew.add(aitemCol);
			}
			col = colNew;
		} else {
			// seems that we are currently processing a new workitem - so
			// include it into the resultset
			ItemCollection itemTemp = new ItemCollection(
					adocumentContext.getAllItems());
			itemTemp.replaceItemValue("$temp", "true");
			col.add(itemTemp);
			logger.info(" ReportPlugin - add current workitem into collection");

		}
		try {
			// Transform XML per XSL and generate output
			EntityCollection xmlCol = XMLItemCollectionAdapter
					.putCollection(col);

			StringWriter xmlWriter = new StringWriter();

			JAXBContext context = JAXBContext
					.newInstance(EntityCollection.class);

			Marshaller m = context.createMarshaller();
			m.setProperty("jaxb.encoding", sEncoding);
			m.marshal(xmlCol, xmlWriter);

			// test if FOP Tranformation
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			try {
				if ("application/pdf".equals(sContentType.toLowerCase()))
					org.imixs.workflow.jaxrs.ReportRestService
							.fopTranformation(xmlWriter.toString(), sXSL,
									sEncoding, outputStream);
				else
					org.imixs.workflow.jaxrs.ReportRestService
							.xslTranformation(xmlWriter.toString(), sXSL,
									sEncoding, outputStream);
			} finally {
				outputStream.close();
			}

			// write to workitem
			if ("0".equals(reportTarget)) {

				adocumentContext.addFile(outputStream.toByteArray(),
						reportFilePath, sContentType);
			}
			// write to blob
			if ("1".equals(reportTarget)) {
				loadBlobWorkItem(adocumentContext);

				blobWorkitem.addFile(outputStream.toByteArray(),
						reportFilePath, sContentType);
				saveBlobWorkitem(adocumentContext);
				// add the file name (with empty data) into the
				// parentWorkitem.
				byte[] empty = { 0 };
				adocumentContext.addFile(empty, reportFilePath, "");
			}
			// write to filesystem
			if ("2".equals(reportTarget)) {
				FileOutputStream fos = null;
				try {
					fos = new FileOutputStream(reportFilePath);
					fos.write(outputStream.toByteArray());
					fos.flush();
				} finally {
					if (fos != null) {
						fos.close();
					}
				}
			}

			return Plugin.PLUGIN_OK;
		} catch (Exception e) {
			// report undefined
			throw new PluginException(ReportPlugin.class.getSimpleName(),
					INVALID_REPORT_DEFINITION, "Unable to process report '"
							+ reportName + "' ", new Object[] { reportName });
		}
	}

	public void close(int status) throws PluginException {

	}

	/**
	 * This method parses the query Params of a Request URL and adds params to a
	 * given EQL Query.
	 * 
	 * The Query params are provided in the attribute txtReportParams from the
	 * Activity Entity in the following format<br>
	 * 
	 * <code>
	      param1=xxx&param2=xxx&param3=xxx
	 * </code>
	 * 
	 * @param uriInfo
	 * @return
	 */
	private String computeEQLParams(String aQuery, String sParamString) {

		// cut prafix ? or & if available
		if (sParamString.startsWith("?") || sParamString.startsWith("&"))
			sParamString = sParamString.substring(1);

		// split params
		StringTokenizer tokenizer = new StringTokenizer(sParamString, "&");

		while (tokenizer.hasMoreTokens()) {

			String aToken = tokenizer.nextToken();
			// if no '=' contained - continue...
			if (aToken.indexOf('=') == -1)
				continue;

			String sKeyName = aToken.substring(0, aToken.indexOf('='));
			String sParamValue = aToken.substring(aToken.indexOf('=') + 1);
			// test if key is contained in query
			if (aQuery.indexOf("?" + sKeyName) > -1)
				aQuery = aQuery.replace("?" + sKeyName, sParamValue);

		}
		return aQuery;
	}

	/**
	 * Loads the BlobWorkitem of a given parent Workitem. The BlobWorkitem is
	 * identified by the $unqiueidRef. If no BlobWorkitem still exists the
	 * method creates a new empty BlobWorkitem which can be saved later.
	 * 
	 * @param itemCol
	 *            - parent workitem where the BlobWorkitem will be attached to
	 * @throws Exception
	 */
	private void loadBlobWorkItem(ItemCollection itemCol) throws Exception {

		String sUniqueID = itemCol.getItemValueString("$uniqueid");

		// search entity...
		String sQuery = " SELECT lobitem FROM Entity as lobitem"
				+ " join lobitem.textItems as t1"
				+ " join lobitem.textItems as t2"
				+ " WHERE t1.itemName = 'type'"
				+ " AND t1.itemValue = 'workitemlob'"
				+ " AND t2.itemName = '$uniqueidref'" + " AND t2.itemValue = '"
				+ sUniqueID + "'";

		Collection<ItemCollection> itemcol = entityService.findAllEntities(
				sQuery, 0, 1);
		if (itemcol != null && itemcol.size() > 0) {

			blobWorkitem = itemcol.iterator().next();
		} else {
			blobWorkitem = new ItemCollection();
			blobWorkitem.replaceItemValue("$uniqueidRef", sUniqueID);
			blobWorkitem.replaceItemValue("type", "workitemlob");
		}

	}

	/**
	 * This method saves the current BlobWorkitem. Therefore the method copies
	 * the read- and write access list from the given parent workitem into the
	 * BlobWorkitem before save.
	 * 
	 * So this method should be called after a WorkflowProcessing step to update
	 * the read- and write access identically to the parentWorkitem
	 * 
	 * @throws Exception
	 */
	private void saveBlobWorkitem(ItemCollection parentWorkitem)
			throws Exception {

		if (blobWorkitem != null && parentWorkitem != null) {

			// Update Read and write access list from parent workitem
			List<?> vAccess = parentWorkitem.getItemValue("$ReadAccess");
			blobWorkitem.replaceItemValue("$ReadAccess", vAccess);

			vAccess = parentWorkitem.getItemValue("$WriteAccess");
			blobWorkitem.replaceItemValue("$WriteAccess", vAccess);

			blobWorkitem.replaceItemValue("$uniqueidRef",
					parentWorkitem.getItemValueString("$uniqueID"));
			blobWorkitem.replaceItemValue("type", "workitemlob");
			// Update BlobWorkitem
			//blobWorkitem = entityService.save(blobWorkitem);
			// new transaction....
			blobWorkitem= this.getEjbSessionContext().getBusinessObject(WorkflowService.class)
						.getEntityService().saveByNewTransaction(blobWorkitem);


		}
	}

}