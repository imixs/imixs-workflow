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

package org.imixs.workflow.engine.plugins;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.xml.DocumentCollection;
import org.imixs.workflow.xml.XMLItemCollection;
import org.imixs.workflow.xml.XMLItemCollectionAdapter;
import org.imixs.workflow.xml.XSLHandler;

/**
 * This plug-in executes a Imixs Report definition and stores the result either
 * into the current workitem ($file) or into the file system. The corresponding
 * BPMN event provide the following properties:
 * <p>
 * <ul>
 * <li>txtReportName=Name of the Report to be processed
 * <li>txtReportFilePath= optional filename or file path the result will be
 * saved
 * <li>txtReportTarget = where the result is saved (0=workitem, 2= disk)
 * </ul>
 * <p>
 * 
 * CHANGES V 2.0
 * <p>
 * In the current version 2.0, only the processed document will be used as the
 * xml input source for the XSL transformation. A search query will currently
 * not be evaluated.
 * 
 * 
 * @author Ralph Soika
 * @version 2.0
 */

public class ReportPlugin extends AbstractPlugin {

	public static final String INVALID_CONTEXT = "INVALID_CONTEXT";
	public static final String REPORT_UNDEFINED = "REPORT_UNDEFINED";
	public static final String INVALID_REPORT_DEFINITION = "INVALID_REPORT_DEFINITION";

	private static Logger logger = Logger.getLogger(ReportPlugin.class.getName());

	/**
	 * Executes a report defined defined by the event in the attribute
	 * 'txtReportName'.
	 * <p>
	 * The XML Source used by this method is the XML representation of the
	 * current document. The Query Statement will not be evaluated
	 * <p>
	 * 
	 * 
	 */
	public ItemCollection run(ItemCollection adocumentContext, ItemCollection adocumentActivity)
			throws PluginException {

		String reportName = adocumentActivity.getItemValueString("txtReportName");
		String reportFilePath = adocumentActivity.getItemValueString("txtReportFilePath");
		if ("".equals(reportFilePath))
			reportFilePath = reportName;

		// replace dynamic field values
		reportFilePath = this.replaceDynamicValues(reportFilePath, adocumentContext);

		String reportTarget = adocumentActivity.getItemValueString("txtReportTarget");

		if ("".equals(reportName))
			return adocumentContext;

		ItemCollection itemCol = getWorkflowService().getReportService().getReport(reportName);
		if (itemCol == null) {
			// report undefined
			throw new PluginException(ReportPlugin.class.getSimpleName(), REPORT_UNDEFINED,
					"Report '" + reportName + " is undefined", new Object[] { reportName });
		}

		String xslTemplate = itemCol.getItemValueString("xsl").trim();
		// if no XSL is provided return
		if ("".equals(xslTemplate))
			return adocumentContext;

		String sContentType = itemCol.getItemValueString("contenttype");
		if ("".equals(sContentType))
			sContentType = "text/html";

		String encoding = itemCol.getItemValueString("encoding");
		// no encoding defined so take a default encoding
		// (UTF-8)
		if ("".equals(encoding))
			encoding = "UTF-8";

		try {
			// TODO : we need to clarify if the method call unescapeXMLContent() is necessary
			
			XMLItemCollection xml = XMLItemCollectionAdapter.putItemCollection(adocumentContext);
			StringWriter writer = new StringWriter();

			JAXBContext context = JAXBContext.newInstance(DocumentCollection.class);
			Marshaller m = context.createMarshaller();
			m.setProperty("jaxb.encoding", encoding);
			m.marshal(xml, writer);

			// create a ByteArray Output Stream
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			try {
				if ("application/pdf".equals(sContentType.toLowerCase())) {
					logger.warning("FOP Transformation is not yet implementd");
					// org.imixs.workflow.jaxrs.ReportRestService.fopTranformation(xmlContentExtended,
					// xslTemplate, encoding,
					// outputStream);
				} else {
					XSLHandler.transform(writer.toString(), xslTemplate, encoding, outputStream);

				}
			} finally {
				outputStream.close();
			}

			// write to workitem
			if (reportTarget.isEmpty() || "0".equals(reportTarget)) {

				adocumentContext.addFile(outputStream.toByteArray(), reportFilePath, sContentType);
			}
			// write to blob
			if ("1".equals(reportTarget)) {
				logger.warning(
						"Writing into BlobWorkitem is no longer supported - please use the DMSPlugin for transfer");

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

			return adocumentContext;
		} catch (Exception e) {
			// report undefined
			throw new PluginException(ReportPlugin.class.getSimpleName(), INVALID_REPORT_DEFINITION,
					"Unable to process report '" + reportName + "' ", new Object[] { reportName });
		}
	}

	/**
	 * The method replaces the value tags of xml and html elements (starting
	 * with xml and html)
	 * 
	 * 
	 * Example
	 * 
	 * <code>
	    <item><name>htmlanswer</name>
	          <value xsi:type="xs:string">&lt;p&gt;Some conent&lt;/p&gt;</value>
	    </item>
	 * 
	 * </code>
	 * 
	 * We need to iterate over the EntityCollection to replace each entity tag
	 * with the corresponding original values
	 * 
	 * @param aContent
	 *            - xml conent
	 * @param vAttributList
	 *            - list of items to replace content in unescaped format
	 * @param xmlCol
	 *            - xml EntityCollection containing the original values
	 */
	private String unescapeXMLContent(String aContent, List<String> vAttributList, Collection<ItemCollection> col) {

		int entityPos = aContent.indexOf("<document>");
		// iterate over all entities ...
		for (ItemCollection entity : col) {

			for (String fieldname : vAttributList) {

				if (fieldname.toLowerCase().startsWith("html") || fieldname.toLowerCase().startsWith("xml")) {

					// find <name>field</name>
					String tag = "<name>" + fieldname + "</name>";
					String sOriginValue = entity.getItemValueString(fieldname);
					// process only if not an empty value
					if (!sOriginValue.isEmpty()) {
						int iPos = aContent.indexOf(tag, entityPos);
						if (iPos > -1) {
							// find value start pos and end pos
							int start = aContent.indexOf('>', iPos + tag.length());
							if (start > -1) {
								// if empty tag it ends with /> instead of
								// </value>. But we skipt empty values before.
								// So we need not to care about this
								int end = aContent.indexOf("</value>", start);

								if (end > -1) {
									// replace conent with origiaal value...

									aContent = aContent.substring(0, start + 1) + sOriginValue
											+ aContent.substring(end);
								}

							}
						}
					}
				}
			}

			// now we need to forward to the next <entity> element in the xml
			// structure...
			entityPos = aContent.indexOf("<document>", entityPos + 1);
		}

		return aContent;
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

}