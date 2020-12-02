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

package org.imixs.workflow.engine.solr;

import java.io.StringReader;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.index.DefaultOperator;
import org.imixs.workflow.engine.index.SchemaService;
import org.imixs.workflow.engine.index.SearchService;
import org.imixs.workflow.engine.index.SortOrder;
import org.imixs.workflow.exceptions.QueryException;
import org.imixs.workflow.util.JSONParser;

import jakarta.ejb.Stateless;
import jakarta.json.Json;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;

/**
 * This session ejb provides a service to search the solr index.
 * <p>
 * 
 * @version 1.0
 * @author rsoika
 */
@DeclareRoles({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
        "org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
        "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RolesAllowed({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
        "org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
        "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
public class SolrSearchService implements SearchService {

    public static final int DEFAULT_MAX_SEARCH_RESULT = 9999; // limiting the
                                                              // total
    // number of hits
    public static final int DEFAULT_PAGE_SIZE = 100; // default docs in one page

    @Inject
    @ConfigProperty(name = "solr.core", defaultValue = "imixs-workflow")
    private String core;

    @Inject
    private SchemaService schemaService;

    @Inject
    private SolrIndexService solarIndexService;

    @Inject
    private DocumentService documentService;

    private static Logger logger = Logger.getLogger(SolrSearchService.class.getName());

    private SimpleDateFormat luceneDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    /**
     * Returns a collection of documents matching the provided search term. The term
     * will be extended with the current users roles to test the read access level
     * of each workitem matching the search term.
     * <p>
     * The optional param 'searchOrder' can be set to force lucene to sort the
     * search result by any search order.
     * <p>
     * The optional param 'defaultOperator' can be set to Operator.AND
     * <p>
     * The optional param 'stubs' indicates if the full Imixs Document should be
     * loaded or if only the data fields stored in the lucedn index will be return.
     * The later is the faster method but returns only document stubs.
     * 
     * @param searchTerm
     * @param pageSize        - docs per page
     * @param pageIndex       - page number
     * @param sortOrder
     * @param defaultOperator - optional to change the default search operator
     * @param loadStubs       - optional indicates of only the lucene document
     *                        should be returned.
     * @return collection of search result
     * 
     * @throws QueryException in case the searchtem is not understandable.
     */
    @Override
    public List<ItemCollection> search(String _searchTerm, int pageSize, int pageIndex, SortOrder sortOrder,
            DefaultOperator defaultOperator, boolean loadStubs) throws QueryException {
        boolean debug = logger.isLoggable(Level.FINE);
        long ltime = System.currentTimeMillis();

        if (pageSize <= 0) {
            pageSize = DEFAULT_PAGE_SIZE;
        }

        if (pageIndex < 0) {
            pageIndex = 0;
        }
        if (debug) {
            logger.finest("......solr search: pageNumber=" + pageIndex + " pageSize=" + pageSize);
        }
        ArrayList<ItemCollection> workitems = new ArrayList<ItemCollection>();

        String searchTerm = adaptSearchTerm(_searchTerm);
        // test if searchtem is provided
        if (searchTerm == null || "".equals(searchTerm)) {
            return workitems;
        }

        // post query....
        String result = solarIndexService.query(searchTerm, pageSize, pageIndex, sortOrder, defaultOperator, loadStubs);
        if (debug) {
            logger.finest("......Result = " + result);
        }
        if (result != null && !result.isEmpty()) {
            List<ItemCollection> documentStubs = parseQueryResult(result);
            if (loadStubs) {
                workitems.addAll(documentStubs);
            } else {
                // load workitems
                for (ItemCollection stub : documentStubs) {
                    ItemCollection document = documentService.load(stub.getUniqueID());
                    if (document != null) {
                        workitems.add(document);
                    }
                }
            }

        }
        if (debug) {
            logger.fine("...search result computed in " + (System.currentTimeMillis() - ltime) + " ms - loadStubs="
                    + loadStubs);
        }
        return workitems;
    }

    /**
     * Returns the total hits for a given search term from the lucene index. The
     * method did not load any data. The provided search term will we extended with
     * a users roles to test the read access level of each workitem matching the
     * search term.
     * <p>
     * In Solr we can get the count if we the the query param 'row=0'. The the
     * response contains still the numFound but not docs!
     * 
     * 
     * @param sSearchTerm
     * @param maxResult   - max search result
     * @return total hits of search result
     * @throws QueryException in case the searchterm is not understandable.
     */
    @Override
    public int getTotalHits(final String _searchTerm, final int _maxResult, final DefaultOperator defaultOperator)
            throws QueryException {
        long l = System.currentTimeMillis();
        int hits = 0;

        String searchTerm = adaptSearchTerm(_searchTerm);
        // test if searchtem is provided
        if (searchTerm == null || "".equals(searchTerm)) {
            return 0;
        }

        // post query with row = 0
        String result = solarIndexService.query(searchTerm, 0, 0, null, defaultOperator, true);
        try {
            String response = JSONParser.getKey("response", result);
            hits = Integer.parseInt(JSONParser.getKey("numFound", response));
        } catch (NumberFormatException e) {
            logger.severe("getTotalHits - failed to parse solr result object! - " + e.getMessage());
            hits = 0;
        }

        logger.info("......computed totalHits in " + (System.currentTimeMillis() - l) + "ms");
        return hits;
    }

    /**
     * This method extracts the docs from a Solr JSON query result
     * 
     * @param json - solr query response (JSON)
     * @return List of ItemCollection objects
     */
    protected List<ItemCollection> parseQueryResult(String json) {
        boolean debug = logger.isLoggable(Level.FINE);
        long l = System.currentTimeMillis();
        List<ItemCollection> result = new ArrayList<ItemCollection>();
        JsonParser parser = Json.createParser(new StringReader(json));
        Event event = null;
        while (true) {

            try {
                event = parser.next(); // START_OBJECT
                if (event == null) {
                    break;
                }

                if (event.name().equals(Event.KEY_NAME.toString())) {
                    String jsonkey = parser.getString();
                    if ("docs".equals(jsonkey)) {
                        event = parser.next(); // docs array
                        if (event.name().equals(Event.START_ARRAY.toString())) {
                            event = parser.next();
                            while (event.name().equals(Event.START_OBJECT.toString())) {
                                // a single doc..
                                if (debug) {
                                    logger.finest("......parse doc....");
                                }
                                ItemCollection itemCol = parseDoc(parser);
                                // now take the values
                                result.add(itemCol);
                                event = parser.next();
                            }

                            if (event.name().equals(Event.END_ARRAY.toString())) {
                                break;

                            }

                        }

                    }

                }
            } catch (NoSuchElementException e) {
                break;
            }
        }
        if (debug) {
            logger.finest("......total parsing time " + (System.currentTimeMillis() - l) + "ms");
        }
        return result;
    }

    /**
     * Builds a ItemCollection from a json doc strcuture
     * 
     * @param parser
     * @return
     */
    private ItemCollection parseDoc(JsonParser parser) {
        boolean debug = logger.isLoggable(Level.FINE);
        ItemCollection document = new ItemCollection();
        Event event = null;
        event = parser.next(); // a single doc..
        while (event.name().equals(Event.KEY_NAME.toString())) {
            String itemName = parser.getString();
            if (debug) {
                logger.finest("......found item " + itemName);
            }
            List<?> itemValue = parseItem(parser);
            // convert itemName and value....
            itemName = solarIndexService.adaptSolrFieldName(itemName);
            document.replaceItemValue(itemName, itemValue);
            event = parser.next();
        }

        return document;
    }

    /**
     * parses a single item value
     * 
     * @param parser
     * @return
     */
    private List<Object> parseItem(JsonParser parser) {

        List<Object> result = new ArrayList<Object>();
        Event event = null;
        while (true) {
            event = parser.next(); // a single doc..
            if (event.name().equals(Event.START_ARRAY.toString())) {

                while (true) {
                    event = parser.next(); // a single doc..
                    if (event.name().equals(Event.VALUE_STRING.toString())) {
                        // just return the next json object here

                        result.add(convertLuceneValue(parser.getString()));
                    }
                    if (event.name().equals(Event.VALUE_NUMBER.toString())) {
                        // just return the next json object here
                        // result.add(parser.getBigDecimal());

                        result.add(convertLuceneValue(parser.getString()));
                    }
                    if (event.name().equals(Event.VALUE_TRUE.toString())) {
                        // just return the next json object here
                        result.add(true);
                    }
                    if (event.name().equals(Event.VALUE_FALSE.toString())) {
                        // just return the next json object here
                        result.add(false);
                    }
                    if (event.name().equals(Event.END_ARRAY.toString())) {
                        break;
                    }
                }

            }

            if (event.name().equals(Event.VALUE_STRING.toString())) {
                // single value!
                result.add(parser.getString());
            }
            if (event.name().equals(Event.VALUE_NUMBER.toString())) {
                // just return the next json object here
                result.add(parser.getBigDecimal());
            }
            if (event.name().equals(Event.VALUE_TRUE.toString())) {
                // just return the next json object here
                result.add(true);
            }
            if (event.name().equals(Event.VALUE_FALSE.toString())) {
                // just return the next json object here
                result.add(false);
            }

            break;
        }

        return result;
    }

    /**
     * This
     * 
     * @param stringValue
     * @return
     */
    private Object convertLuceneValue(String stringValue) {
        Object objectValue = null;
        // check for numbers....
        if (isNumeric(stringValue)) {
            // is date?
            if (stringValue.length() == 14 && !stringValue.contains(".")) {
                try {
                    objectValue = luceneDateFormat.parse(stringValue);
                } catch (java.text.ParseException e) {
                    // no date!
                }
            }
            // lets see if it is a number..?
            if (objectValue == null) {
                try {
                    Number number = NumberFormat.getInstance().parse(stringValue);
                    objectValue = number;
                } catch (java.text.ParseException e) {
                    // no number - should not happen
                }
            }
        }
        if (objectValue == null) {
            objectValue = stringValue;
        }
        return objectValue;
    }

    /**
     * Helper method to check for numbers.
     * 
     * @see https://stackoverflow.com/questions/1102891/how-to-check-if-a-string-is-numeric-in-java
     * @param str
     * @return
     */
    private static boolean isNumeric(String str) {
        boolean dot = false;
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (char c : str.toCharArray()) {
            if (c == '.' && dot == false) {
                dot = true; // first dot!
                continue;
            }
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;

    }

    /**
     * This method addapts a given Solr search term. The method extend the search
     * term by read access query and also adapts the imixs item names to Solr field
     * names.
     * 
     * @param _serachTerm
     * @return
     * @throws QueryException
     */
    private String adaptSearchTerm(String _serachTerm) throws QueryException {
        if (_serachTerm == null || "".equals(_serachTerm)) {
            return _serachTerm;
        }

        String searchTerm = schemaService.getExtendedSearchTerm(_serachTerm);

        // Because Solr does not accept $ symbol in an item name we need to replace the
        // Imxis Item Names and adapt them into the corresponding Solr Field name
        searchTerm = adaptQueryFieldNames(searchTerm);

        return searchTerm;

    }

    /**
     * This method adapts a search query for Imixs Item names and adapts these names
     * with the corresponding Solr field name (replace $ with _)
     * 
     * @return
     */
    private String adaptQueryFieldNames(String _query) {
        String result = _query;

        if (schemaService == null) {
            return result;
        }

        if (_query == null || !_query.contains("$")) {
            return result;
        }

        for (String imixsItemName : schemaService.getUniqueFieldList()) {
            if (imixsItemName.charAt(0) == '$') {
                // this item starts with $ and we need to parse the query for this item....
                while (result.contains(imixsItemName + ":")) {
                    String solrField = "_" + imixsItemName.substring(1);
                    result = result.replace(imixsItemName + ":", solrField + ":");
                }
            }
        }

        return result;
    }

}
