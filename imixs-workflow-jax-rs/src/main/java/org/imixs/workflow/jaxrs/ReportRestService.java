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

package org.imixs.workflow.jaxrs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.ReportService;
import org.imixs.workflow.xml.DocumentTable;
import org.imixs.workflow.xml.XMLDataCollection;
import org.imixs.workflow.xml.XMLDataCollectionAdapter;
import org.imixs.workflow.xml.XMLDocument;
import org.imixs.workflow.xml.XMLDocumentAdapter;
import org.imixs.workflow.xml.XSLHandler;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.core.UriInfo;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;

/**
 * The WorkflowService Handler supports methods to process different kind of
 * request URIs
 * 
 * @author rsoika
 * 
 */
@Path("/report")
@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.TEXT_XML })
@Stateless
public class ReportRestService {

    @Inject
    private DocumentService entityService;

    @Inject
    private ReportService reportService;

    @Context
    private HttpServletRequest servletRequest;

    private static final Logger logger = Logger.getLogger(ReportRestService.class.getName());

    @GET
    @Produces("text/html")
    public StreamingOutput getHelpHTML() {
        return new StreamingOutput() {
            public void write(OutputStream out) throws IOException, WebApplicationException {

                out.write("<html><head>".getBytes());
                out.write("<style>".getBytes());
                out.write("table {padding:0px;width: 100%;margin-left: -2px;margin-right: -2px;}".getBytes());
                out.write(
                        "body,td,select,input,li {font-family: Verdana, Helvetica, Arial, sans-serif;font-size: 13px;}"
                                .getBytes());
                out.write("table th {color: white;background-color: #bbb;text-align: left;font-weight: bold;}"
                        .getBytes());

                out.write("table th,table td {font-size: 12px;}".getBytes());

                out.write("table tr.a {background-color: #ddd;}".getBytes());

                out.write("table tr.b {background-color: #eee;}".getBytes());

                out.write("</style>".getBytes());
                out.write("</head><body>".getBytes());

                // body
                out.write("<h1>Imixs-Workflow REST Service</h1>".getBytes());
                out.write(
                        "<p>See the <a href=\"http://www.imixs.org/doc/restapi/reportservice.html\" target=\"_blank\">Imixs-Workflow REST API</a> for more information about this Service.</p>"
                                .getBytes());

                // end
                out.write("</body></html>".getBytes());
            }
        };

    }

    @GET
    @Path("/definitions")
    public XMLDataCollection getReportsDefinitions() {
        try {

            Collection<ItemCollection> col = null;
            col = reportService.findAllReports();
            return XMLDataCollectionAdapter.getDataCollection(col);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new XMLDataCollection();
    }

    /**
     * Returns a single report by name or by uniqueid
     * 
     * @param name reportname or uniqueid of report
     * @return
     */
    @GET
    @Path("/definitions/{name}")
    public XMLDataCollection getReportDefinition(@PathParam("name") String name) {
        try {
            ItemCollection itemCol = reportService.findReport(name);
            return XMLDataCollectionAdapter.getDataCollection(itemCol);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Executes a single report defined by name or uniqueid
     * 
     * The output depends on the requested media format
     * 
     * Since 2.1.2: the ReportService also supports FOP Transformation. If the
     * ContentType is 'application/pdf' the method will call fopTransofrmation
     * instat of xslTransformation. The FOP API need to be provided by the main
     * application.
     * 
     * @param name reportname of the report to be executed
     * @return a collection of entiteis
     * 
     */
    @GET
    @Path("/{name}.imixs-report")
    public Response getExcecuteReport(@PathParam("name") String reportName,
            @DefaultValue("1000") @QueryParam("pageSize") int pageSize,
            @DefaultValue("0") @QueryParam("pageIndex") int pageIndex, @QueryParam("sortBy") String sortBy,
            @QueryParam("sortReverse") boolean sortReverse, @DefaultValue("") @QueryParam("encoding") String encoding,
            @Context UriInfo uriInfo) {
        Collection<ItemCollection> col = null;

        String sXSL;
        String sContentType;

        try {

            ItemCollection report = reportService.findReport(reportName);
            if (report == null) {
                logger.log(Level.SEVERE, "Report ''{0}'' not defined!", reportName);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }

            sXSL = report.getItemValueString("XSL").trim();
            sContentType = report.getItemValueString("contenttype");
            if ("".equals(sContentType))
                sContentType = "text/html";

            // if no encoding is provided by the query string than the encoding
            // from the report will be taken
            if ("".equals(encoding))
                encoding = report.getItemValueString("encoding");
            // no encoding defined so take a default encoding
            // (UTF-8)
            if ("".equals(encoding))
                encoding = "UTF-8";

            // execute report
            Map<String, String> params = getQueryParams(uriInfo);
            col = reportService.getDataSource(report, pageSize, pageIndex, sortBy, sortReverse, params);

            // if no XSL is provided return standard html format...?
            if ("".equals(sXSL)) {
                Response.ResponseBuilder builder = Response.ok(XMLDataCollectionAdapter.getDataCollection(col),
                        "text/html");
                return builder.build();
            }

            // Transform XML per XSL and generate output
            XMLDataCollection xmlCol = XMLDataCollectionAdapter.getDataCollection(col);

            StringWriter writer = new StringWriter();

            JAXBContext context = JAXBContext.newInstance(XMLDataCollection.class);

            Marshaller m = context.createMarshaller();
            m.setProperty("jaxb.encoding", encoding);
            m.marshal(xmlCol, writer);

            // create a ByteArray Output Stream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                // test if FOP Tranformation
                if ("application/pdf".equals(sContentType.toLowerCase()))
                    fopTranformation(writer.toString(), sXSL, encoding, outputStream);
                else
                    XSLHandler.transform(writer.toString(), sXSL, encoding, outputStream);
            } finally {
                outputStream.close();
            }

            /*
             * outputStream.toByteArray() did not work here because the encoding will not be
             * considered. For that reason we use the toString(encoding) method here.
             * 
             * 8.9.2012:
             * 
             * after some tests we see that only toByteArray will work on things like fop
             * processing. So for that reason we switched back to the toByteArray method
             * again. But we still need to solve the encoding issue
             */

            Response.ResponseBuilder builder = Response.ok(outputStream.toByteArray(), sContentType);
            // Response.ResponseBuilder builder = Response.ok(
            // outputStream.toString(encoding), sContentType);
            return builder.build();
        } catch (Exception e) {
            e.printStackTrace();

        }

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();

    }

    /**
     * helper method for .pdf file extention
     * 
     * @param name reportname of the report to be executed
     * @return a collection of entiteis
     * 
     */
    @GET
    @Path("/{name}.pdf")
    public Response getPdfReport(@PathParam("name") String reportName,
            @DefaultValue("1000") @QueryParam("pageSize") int pageSize,
            @DefaultValue("0") @QueryParam("pageIndex") int pageIndex, @QueryParam("sortBy") String sortBy,
            @QueryParam("sortReverse") boolean sortReverse, @DefaultValue("") @QueryParam("encoding") String encoding,
            @Context UriInfo uriInfo) {
        return this.getExcecuteReport(reportName, pageSize, pageIndex, sortBy, sortReverse, encoding, uriInfo);
    }

    /**
     * Returns an HTML Stream with a HTML Datatable corresponding to the report
     * query.
     * 
     * @param name
     * @param start
     * @param count
     * @return
     */
    @SuppressWarnings("unchecked")
    @GET
    @Produces({ MediaType.TEXT_HTML, MediaType.APPLICATION_XHTML_XML })
    @Path("/{name}.html")
    public DocumentTable getHTMLResult(@PathParam("name") String reportName,
            @DefaultValue("1000") @QueryParam("pageSize") int pageSize,
            @DefaultValue("0") @QueryParam("pageIndex") int pageIndex, @QueryParam("sortBy") String sortBy,
            @QueryParam("sortReverse") boolean sortReverse, @DefaultValue("") @QueryParam("encoding") String encoding,
            @Context UriInfo uriInfo, @Context HttpServletResponse servlerResponse) {
        Collection<ItemCollection> col = null;

        try {
            ItemCollection report = reportService.findReport(reportName);
            List<List<String>> attributes = (List<List<String>>) report.getItemValue("attributes");
            List<String> items = new ArrayList<String>();
            List<String> labels = new ArrayList<String>();
            for (List<String> attribute : attributes) {
                items.add(attribute.get(0));
                String label = attribute.get(0);
                if ((attribute.size() >= 2) && !(attribute.get(1).isEmpty())) {
                    label = attribute.get(1);
                }
                labels.add(label);
            }

            // execute report
            Map<String, String> params = getQueryParams(uriInfo);
            col = reportService.getDataSource(report, pageSize, pageIndex, sortBy, sortReverse, params);

            XMLDataCollection documentCollection = XMLDataCollectionAdapter.getDataCollection(col);
            DocumentTable documentTable = new DocumentTable(documentCollection.getDocument(), items, labels, encoding);

            if (encoding == null || "".equals(encoding)) {
                encoding = report.getItemValueString("encoding");
                if ("".equals(encoding)) {
                    // no encoding defined so take a default encoding (UTF-8)
                    encoding = "UTF-8";
                }
            }

            logger.log(Level.FINE, "set encoding :{0}", encoding);
            servlerResponse.setContentType(MediaType.TEXT_HTML + "; charset=" + encoding);

            return documentTable;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns a Datatable corresponding to the report definition and report content
     * type.
     * <p>
     * The path annotation allows any file extension.
     * 
     * @param name
     * @param start
     * @param count
     * @return
     */
    @GET
    @Path("/custom/{name}.{s:.*}")
    public Response getCustomResult(@PathParam("name") String reportName,
            @DefaultValue("1000") @QueryParam("pageSize") int pageSize,
            @DefaultValue("0") @QueryParam("pageIndex") int pageIndex, @QueryParam("sortBy") String sortBy,
            @QueryParam("sortReverse") boolean sortReverse, @DefaultValue("") @QueryParam("encoding") String encoding,
            @Context UriInfo uriInfo, @Context HttpServletResponse servlerResponse) {

        ItemCollection report = reportService.findReport(reportName);
        if (report == null) {
            logger.log(Level.SEVERE, "Report ''{0}'' not defined!", reportName);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        if (encoding == null || "".equals(encoding)) {
            encoding = report.getItemValueString("encoding");
            if ("".equals(encoding)) {
                // no encoding defined so take a default encoding (UTF-8)
                encoding = "UTF-8";
            }
        }

        DocumentTable documentTable = getHTMLResult(reportName, pageSize, pageIndex, sortBy, sortReverse, encoding,
                uriInfo, servlerResponse);

        String sContentType = report.getItemValueString("contenttype");
        if (sContentType.isEmpty()) {
            sContentType = MediaType.APPLICATION_XML;
        }

        // set content type and character encoding
        logger.log(Level.FINE, "set encoding :{0}", encoding);
        servlerResponse.setContentType(sContentType + "; charset=" + encoding);

        return Response.ok(documentTable, sContentType).build();

    }

    /**
     * Returns a xml stream from a report
     * 
     * If a attribute list is defined in the report only the corresponding
     * properties will be returend in the xml stream.
     * 
     * If the query param 'items' is provided the attribute list in the report will
     * be ignored.
     * 
     * @param name  reportname of the report to be executed
     * @param start
     * @param count
     * @return
     * @throws Exception
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    @Path("/{name}.xml")
    public XMLDataCollection getXMLResult(@PathParam("name") String reportName,
            @DefaultValue("100") @QueryParam("pageSize") int pageSize,
            @DefaultValue("0") @QueryParam("pageIndex") int pageIndex, @QueryParam("sortBy") String sortBy,
            @QueryParam("sortReverse") boolean sortReverse, @DefaultValue("") @QueryParam("encoding") String encoding,

            @Context UriInfo uriInfo, @Context HttpServletResponse servlerResponse) throws Exception {
        Collection<ItemCollection> col = null;

        try {
            // execute report
            ItemCollection report = reportService.findReport(reportName);
            Map<String, String> params = getQueryParams(uriInfo);
            col = reportService.getDataSource(report, pageSize, pageIndex, sortBy, sortReverse, params);

            // set content type and character encoding
            if (encoding == null || "".equals(encoding)) {
                encoding = report.getItemValueString("encoding");
                if ("".equals(encoding)) {
                    // no encoding defined so take a default encoding (UTF-8)
                    encoding = "UTF-8";
                }
            }

            logger.log(Level.FINE, "set encoding :{0}", encoding);
            servlerResponse.setContentType(MediaType.APPLICATION_XML + "; charset=" + encoding);

            return XMLDataCollectionAdapter.getDataCollection(col);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns a JSON stream from a report
     * 
     * If a attribute list is defined in the report only the corresponding
     * properties will be returend in the xml stream.
     * 
     * 
     * @param name  reportname of the report to be executed
     * @param start
     * @param count
     * @return
     * @throws Exception
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{name}.json")
    public XMLDataCollection getJSONResult(@PathParam("name") String name,

            @DefaultValue("-1") @QueryParam("pageSize") int pageSize,
            @DefaultValue("0") @QueryParam("pageIndex") int pageIndex, @QueryParam("sortBy") String sortBy,
            @QueryParam("sortReverse") boolean sortReverse,

            @DefaultValue("") @QueryParam("encoding") String encoding, @Context UriInfo uriInfo,
            @Context HttpServletResponse servlerResponse) throws Exception {

        XMLDataCollection result = getXMLResult(name, pageSize, pageIndex, sortBy, sortReverse, encoding, uriInfo,
                servlerResponse);

        servlerResponse.setContentType(MediaType.APPLICATION_JSON + "; charset=" + encoding);

        return result;

    }

    /**
     * Deletes a report by name or by its $uniqueID or name.
     * 
     * @param name of report or uniqueid
     */
    @DELETE
    @Path("/reports/{name}")
    public void deleteReport(@PathParam("name") String name) {
        try {
            ItemCollection itemCol = reportService.findReport(name);
            entityService.remove(itemCol);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * This method updates or creates a Report object provided in a
     * XMLItemCollection object
     * 
     * 
     * @param reportCol - report data
     */
    @PUT
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_OCTET_STREAM, MediaType.TEXT_PLAIN,
            MediaType.TEXT_XML })
    public void putReport(XMLDocument reportCol) {
        ItemCollection itemCollection;
        try {
            itemCollection = XMLDocumentAdapter.putDocument(reportCol);
            reportService.updateReport(itemCollection);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @POST
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_OCTET_STREAM, MediaType.TEXT_PLAIN,
            MediaType.TEXT_XML })
    public void postReport(XMLDocument reportCol) {
        putReport(reportCol);
    }

    /**
     * This method dos a apache FOP transformation using the FopFactory
     * 
     * @param xmlSource
     * @param xslSource
     * @param aEncoding
     * @param outputWriter
     */
    public void fopTranformation(String xmlSource, String xslSource, String aEncoding, OutputStream output)
            throws Exception {
        // configure fopFactory as desired
        // # changed during jakarta ee 9 migration / fop v 2.5
        FopFactory fopFactory = FopFactory.newInstance(new File(".").toURI());
        // FopFactory fopFactory = FopFactory.newInstance();

        FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
        // foUserAgent.setBaseURL(fopFactory.getBaseURL());

        // configure foUserAgent as desired
        // OutputStream out =null;
        try {
            // Construct fop with desired output format
            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, output);

            // Setup XSLT
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            // Set secure process - see #852
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            ByteArrayInputStream baseXSL = new ByteArrayInputStream(xslSource.getBytes());
            InputStreamReader isReaderXSL = new InputStreamReader(baseXSL, aEncoding);
            Source xslSrc = new StreamSource(isReaderXSL);

            Transformer transformer = transformerFactory.newTransformer(xslSrc);

            // Setup input for XSLT transformation
            ByteArrayInputStream baisXML = new ByteArrayInputStream(xmlSource.getBytes());
            InputStreamReader isReaderXML;
            isReaderXML = new InputStreamReader(baisXML, aEncoding);
            Source xmlSrc = new StreamSource(isReaderXML);

            // Resulting SAX events (the generated FO) must be piped through to FOP
            Result res = new SAXResult(fop.getDefaultHandler());

            // Start XSLT transformation and FOP processing
            transformer.transform(xmlSrc, res);

        } finally {
            // out.close();
            // output.flush();
            // output.close();
        }

    }

    /**
     * This method parses the query Params of a Request URL and adds params to a
     * given JPQL Query. In addition the method replace dynamic date values in the
     * JPQLStatement
     * 
     * 
     * @param uriInfo
     * @return
     */

    /**
     * Extracts the query parameters and returns a hashmap with key value pairs
     * 
     * @param aQuery
     * @param uriInfo
     * @return
     */
    private Map<String, String> getQueryParams(UriInfo uriInfo) {
        // test each given QueryParam if it is contained in the EQL Query...
        MultivaluedMap<String, String> mvm = uriInfo.getQueryParameters();
        Map<String, String> result = new HashMap<String, String>();
        Set<String> keys = mvm.keySet();
        Iterator<String> iter = keys.iterator();
        while (iter.hasNext()) {
            // read key
            String sKeyName = iter.next().toString();
            result.put(sKeyName, mvm.getFirst(sKeyName));
        }

        return result;
    }

}
