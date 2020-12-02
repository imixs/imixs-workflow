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

package org.imixs.workflow.engine.index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.exceptions.QueryException;

/**
 * The IndexSchemaService provides the index Schema.
 * <p>
 * The schema is defined by the following properties:
 * 
 * <ul>
 * <li>index.fields - content which will be indexed</li>
 * <li>index.fields.analyze - fields indexed as analyzed keyword fields</li>
 * <li>index.fields.noanalyze - fields indexed without analyze</li>
 * <li>index.fields.store - fields stored in the index</li>
 * <li>index.operator - default operator</li>
 * <li>index.splitwhitespace - split text on whitespace prior to analysis</li>
 * </ul>
 * 
 * 
 * @version 1.0
 * @author rsoika
 */
@Singleton
public class SchemaService {

    /*
     * index.fields index.fields.analyze index.fields.noanalyze index.fields.store
     * 
     * index.operator index.splitwhitespace
     * 
     * 
     */

    public static final String ANONYMOUS = "ANONYMOUS";

    @Inject
    @ConfigProperty(name = "index.fields")
    Optional<String> indexFields;

    @Inject
    @ConfigProperty(name = "index.fields.analyze")
    Optional<String> indexFieldsAnalyze;

    @Inject
    @ConfigProperty(name = "index.fields.noanalyze")
    Optional<String> indexFieldsNoAnalyze;

    @Inject
    @ConfigProperty(name = "index.fields.store")
    Optional<String> indexFieldsStore;

    @Inject
    private DocumentService documentService;

    private List<String> fieldList = null;
    private List<String> fieldListAnalyze = null;
    private List<String> fieldListNoAnalyze = null;
    private List<String> fieldListStore = null;
    private Set<String> uniqueFieldList = null;

    // default field lists
    public static List<String> DEFAULT_SEARCH_FIELD_LIST = Arrays.asList("$workflowsummary", "$workflowabstract");
    public static List<String> DEFAULT_NOANALYZE_FIELD_LIST = Arrays.asList("$modelversion", "$taskid", "$processid",
            "$workitemid", "$uniqueidref", "type", "$writeaccess", "$snapshotid", "$modified", "$created", "namcreator",
            "$creator", "$editor", "$lasteditor", "$workflowgroup", "$workflowstatus", "txtworkflowgroup", "name",
            "txtname", "$owner", "namowner", "txtworkitemref", "$workitemref", "$uniqueidsource", "$uniqueidversions",
            "$lasttask", "$lastevent", "$lasteventdate", "$file.count", "$file.names");
    public static List<String> DEFAULT_STORE_FIELD_LIST = Arrays.asList("type", "$taskid", "$writeaccess",
            "$snapshotid", "$modelversion", "$workflowsummary", "$workflowabstract", "$workflowgroup",
            "$workflowstatus", "$modified", "$created", "$lasteventdate", "$creator", "$editor", "$lasteditor",
            "$owner", "namowner");

    private static Logger logger = Logger.getLogger(SchemaService.class.getName());

    /**
     * PostContruct event - The method loads the lucene index properties from the
     * imixs.properties file from the classpath. If no properties are defined the
     * method terminates.
     * 
     */
    @PostConstruct
    void init() {
        boolean debug = logger.isLoggable(Level.FINE);
        if (debug) {
            logger.finest("......lucene FulltextFieldList=" + indexFields);
            logger.finest("......lucene IndexFieldListAnalyze=" + indexFieldsAnalyze);
            logger.finest("......lucene IndexFieldListNoAnalyze=" + indexFieldsNoAnalyze);
            logger.finest("......lucene IndexFieldListStore=" + indexFieldsStore);
        }
        // compute the normal search field list
        fieldList = new ArrayList<String>();
        // add all entries from the default field list
        fieldList.addAll(DEFAULT_SEARCH_FIELD_LIST);
        if (indexFields.isPresent() && !indexFields.get().isEmpty()) {
            StringTokenizer st = new StringTokenizer(indexFields.get(), ",");
            while (st.hasMoreElements()) {
                String sName = st.nextToken().toLowerCase().trim();
                // do not add internal fields
                if (!"$uniqueid".equals(sName) && !"$readaccess".equals(sName) && !fieldList.contains(sName)) {
                    fieldList.add(sName);
                }
            }
        }

        // next we compute the NOANALYZE field list
        fieldListNoAnalyze = new ArrayList<String>();
        // add all entries from the default field list
        fieldListNoAnalyze.addAll(DEFAULT_NOANALYZE_FIELD_LIST);
        if (indexFieldsNoAnalyze.isPresent() && !indexFieldsNoAnalyze.get().isEmpty()) {
            StringTokenizer st = new StringTokenizer(indexFieldsNoAnalyze.get(), ",");
            while (st.hasMoreElements()) {
                String sName = st.nextToken().toLowerCase().trim();
                // avoid duplicates
                if (!"$uniqueid".equals(sName) && !"$readaccess".equals(sName) && !fieldListNoAnalyze.contains(sName)) {
                    fieldListNoAnalyze.add(sName);
                }
            }
        }

        // finally compute Index ANALYZE field list
        fieldListAnalyze = new ArrayList<String>();
        if (indexFieldsAnalyze.isPresent() && !indexFieldsAnalyze.get().isEmpty()) {
            StringTokenizer st = new StringTokenizer(indexFieldsAnalyze.get(), ",");
            while (st.hasMoreElements()) {
                String sName = st.nextToken().toLowerCase().trim();
                // Now we need to avoid also duplicates with the NOANALYZE field list ANALYZE
                // and NOANALYZE must not be mixed (#560). If we already have a field in the
                // NOANALYZE field list we just ignore it!
                if (!"$uniqueid".equals(sName) && !"$readaccess".equals(sName) && !fieldListAnalyze.contains(sName)
                        && !fieldListNoAnalyze.contains(sName)) {
                    fieldListAnalyze.add(sName);
                }
            }
        }

        // compute Index field list (Store)
        fieldListStore = new ArrayList<String>();
        // add all static default field list
        fieldListStore.addAll(DEFAULT_STORE_FIELD_LIST);
        if (indexFieldsStore.isPresent() && !indexFieldsStore.get().isEmpty()) {
            // add additional field list from imixs.properties
            StringTokenizer st = new StringTokenizer(indexFieldsStore.get(), ",");
            while (st.hasMoreElements()) {
                String sName = st.nextToken().toLowerCase().trim();
                if (!fieldListStore.contains(sName))
                    fieldListStore.add(sName);
            }
        }

        // Issue #518:
        // In case a field of the STORE field list is not already part of the ANALYZE
        // field list add not part of NOANALYZE field list, than we add this field to
        // the ANALYZE field list. This is to guaranty that we store the field value in
        // any case!
        for (String fieldName : fieldListStore) {
            if (!fieldListAnalyze.contains(fieldName) && !fieldListNoAnalyze.contains(fieldName)) {
                // add this field into he indexFieldListAnalyze
                fieldListAnalyze.add(fieldName);
            }
        }

        // build unique field list containing all field names
        uniqueFieldList = new HashSet<String>();
        uniqueFieldList.add(WorkflowKernel.UNIQUEID);
        uniqueFieldList.add(DocumentService.READACCESS);
        uniqueFieldList.addAll(fieldListStore);
        uniqueFieldList.addAll(fieldListAnalyze);
        uniqueFieldList.addAll(fieldListNoAnalyze);

    }

    /**
     * Returns the field list defining the default content of the schema. The values
     * of those items are only searchable by fulltext search
     * 
     * @return
     */
    public List<String> getFieldList() {
        return fieldList;
    }

    /**
     * Returns the analyzed field list of the schema. The values of those items are
     * searchable by a field search. The values are analyzed.
     * 
     * @return
     */
    public List<String> getFieldListAnalyze() {
        return fieldListAnalyze;
    }

    /**
     * Returns the no-analyze field list of the schema. The values of those items
     * are searchable by field search. The values are not analyzed.
     * 
     * @return
     */
    public List<String> getFieldListNoAnalyze() {
        return fieldListNoAnalyze;
    }

    /**
     * Returns the field list of items stored in the index.
     * 
     * @return
     */
    public List<String> getFieldListStore() {
        return fieldListStore;
    }

    /**
     * Returns a unique list of all fields part of the index schema.
     * 
     * @return
     */
    public Set<String> getUniqueFieldList() {
        return uniqueFieldList;
    }

    /**
     * Returns the Lucene schema configuration
     * 
     * @return
     */
    public ItemCollection getConfiguration() {
        ItemCollection config = new ItemCollection();

        config.replaceItemValue("lucence.fulltextFieldList", fieldList);
        config.replaceItemValue("lucence.indexFieldListAnalyze", fieldListAnalyze);
        config.replaceItemValue("lucence.indexFieldListNoAnalyze", fieldListNoAnalyze);
        config.replaceItemValue("lucence.indexFieldListStore", fieldListStore);

        return config;
    }

    /**
     * Returns the extended search term for a given query. The search term will be
     * extended with a users roles to test the read access level of each workitem
     * matching the search term.
     * 
     * @param sSearchTerm
     * @return extended search term
     * @throws QueryException in case the searchtem is not understandable.
     */
    public String getExtendedSearchTerm(String sSearchTerm) throws QueryException {
        // test if searchtem is provided
        if (sSearchTerm == null || "".equals(sSearchTerm)) {
            logger.warning("No search term provided!");
            return "";
        }
        // extend the Search Term if user is not ACCESSLEVEL_MANAGERACCESS
        if (!documentService.isUserInRole(DocumentService.ACCESSLEVEL_MANAGERACCESS)) {
            // get user names list
            List<String> userNameList = documentService.getUserNameList();
            // create search term (always add ANONYMOUS)
            String sAccessTerm = "($readaccess:" + ANONYMOUS;
            for (String aRole : userNameList) {
                if (!"".equals(aRole))
                    sAccessTerm += " OR $readaccess:\"" + aRole + "\"";
            }
            sAccessTerm += ") AND ";
            sSearchTerm = sAccessTerm + sSearchTerm;
        }
        logger.finest("......lucene final searchTerm=" + sSearchTerm);

        return sSearchTerm;
    }

    /**
     * This helper method escapes special characters found in a lucene search term.
     * The method can be used by clients to prepare a search phrase.
     * <p>
     * Special characters are characters that are part of the lucene query syntax
     * <p>
     * <code>+ - && || ! ( ) { } [ ] ^ " ~ * ? : \ /</code>
     * <p>
     * Clients should use the method normalizeSearchTerm() instead of
     * escapeSearchTerm() to prepare a user input for a lucene search.
     * 
     * @see normalizeSearchTerm
     * @param searchTerm
     * @param ignoreBracket - if true brackes will not be escaped.
     * @return escaped search term
     */
    public String escapeSearchTerm(String searchTerm, boolean ignoreBracket) {
        if (searchTerm == null || searchTerm.isEmpty()) {
            return searchTerm;
        }

        // this is the code from the QueryParser.escape() method without the '*'
        // char!
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < searchTerm.length(); i++) {
            char c = searchTerm.charAt(i);
            // These characters are part of the query syntax and must be escaped
            // (ignore brackets!)
            if (c == '\\' || c == '+' || c == '-' || c == '!' || c == ':' || c == '^' || c == '[' || c == ']'
                    || c == '\"' || c == '{' || c == '}' || c == '~' || c == '?' || c == '|' || c == '&' || c == '/') {
                sb.append('\\');
            }

            // escape bracket?
            if (!ignoreBracket && (c == '(' || c == ')')) {
                sb.append('\\');
            }

            sb.append(c);
        }
        return sb.toString();

    }

    public String escapeSearchTerm(String searchTerm) {
        return escapeSearchTerm(searchTerm, false);
    }

    /**
     * This method normalizes a search term. The method can be used by clients to
     * prepare a search phrase. The serach term will be lowercased and special
     * characters will be replaced by a blank separator
     * <p>
     * e.g. 'europe/berlin' will be normalized to 'europe berlin'
     * <p>
     * In case the searchTerm contains numbers the method escapes special characters
     * instead of replacing with a blank:
     * <p>
     * e.g. 'r-555/333' will be converted into 'r\-555\/333'
     * <p>
     * Special characters are characters that are part of the lucene query syntax
     * <p>
     * <code>+ - && || ! ( ) { } [ ] ^ " ~ ? : \ /</code>
     * <p>
     * 
     * @param searchTerm
     * @return normalized search term
     * 
     */
    public String normalizeSearchTerm(String searchTerm) {

        if (searchTerm == null) {
            return "";
        }
        if (searchTerm.trim().isEmpty()) {
            return "";
        }

        // lowercase
        searchTerm = searchTerm.toLowerCase();

        if (containsDigit(searchTerm)) {
            return escapeSearchTerm(searchTerm, false);
        }

        // now replace Special Characters with blanks
        // + - && || ! ( ) { } [ ] ^ " ~ * ? : \ /
        // this is the code from the QueryParser.escape() method without the '*'
        // char!
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < searchTerm.length(); i++) {
            char c = searchTerm.charAt(i);
            // These characters are part of the query syntax and must be escaped
            // (ignore brackets!)
            if (c == '\\' || c == '+' || c == '-' || c == '!' || c == ':' || c == '^' || c == '[' || c == ']'
                    || c == '\"' || c == '{' || c == '}' || c == '~' || c == '?' || c == '|' || c == '&' || c == '/') {
                sb.append(' ');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Test if a string contains a number. Seems to be faster than regex.
     * <p>
     * See:
     * https://stackoverflow.com/questions/18590901/check-if-a-string-contains-numbers-java
     * 
     * @param s
     * @return
     */
    private boolean containsDigit(String s) {
        boolean containsDigit = false;
        if (s != null && !s.isEmpty()) {
            for (char c : s.toCharArray()) {
                if (containsDigit = Character.isDigit(c)) {
                    break;
                }
            }
        }
        return containsDigit;
    }

}
