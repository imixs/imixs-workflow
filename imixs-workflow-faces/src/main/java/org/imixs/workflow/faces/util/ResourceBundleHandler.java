package org.imixs.workflow.faces.util;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * The ResourceBundleHandler provides helper method to lookup a label in
 * different bundles. This simplifies the front end implementation as the client
 * does not have to know the bundle a specific resource is located:
 * <p>
 * {@code
 * <h1>#{resourceBundleHandler.findMessage('application_title')}</h1>}
 * <p>
 * The ResourceBundleHandler load the bundles based on the current user locale.
 * <p>
 * Resource bundle instances created by the getBundle factory methods are cached
 * by default, and the factory methods return the same resource bundle instance
 * multiple times if it has been cached. For that reason a RequestScoped bean is
 * used here.
 * <p>
 * The class searches for the resource bundles named 'bundle.messages',
 * 'bundle.app' and 'bundle.custom'. You can overwrite the bundle names with the
 * imixs property value 'resourcebundle.names'. The later entries have a higher
 * priority in case a key is stored in multiple bundles.
 * 
 * @author rsoika
 * @version 1.0
 */
@Named
@RequestScoped
public class ResourceBundleHandler {

    private Locale browserLocale = null;

    private List<ResourceBundle> resourceBundleList = null;

    @Inject
    @ConfigProperty(name = "resourcebundle.names", defaultValue = "bundle.messages,bundle.app,bundle.custom")
    String bundNameProperty;

    /**
     * This method finds the browser locale
     * 
     */
    @PostConstruct
    public void init() {
        resourceBundleList = new ArrayList<ResourceBundle>();
        browserLocale = FacesContext.getCurrentInstance().getViewRoot().getLocale();

        // iterate over all bundle names and add them to the list
        String[] bundleNameList = bundNameProperty.split(",");
        for (String bundleName : bundleNameList) {
            ResourceBundle bundle = null;
            try {
                bundle = ResourceBundle.getBundle(bundleName, browserLocale);
                if (bundle != null) {
                    resourceBundleList.add(bundle);
                }
            } catch (MissingResourceException e) {
                // bundle not defined -> skip
            }
        }
    }

    public Locale getBrowserLocale() {
        return browserLocale;
    }

    public ResourceBundle getMessagesBundleByName(String name) {
        for (ResourceBundle rb : resourceBundleList) {
            if (rb.getBaseBundleName().equals(name) || rb.getBaseBundleName().endsWith(name)) {
                return rb;
            }
        }
        // not found
        return null;
    }

    /**
     * Default getter method
     * 
     * @param key
     * @return
     */
    public String get(String key) {
        return findMessage(key);
    }

    /**
     * This helper method findes a message by key searching all bundles.
     * 
     * @param pe
     */
    public String findMessage(String key) {

        // we iterate form the last to the first bundle
        ListIterator<ResourceBundle> listIterator = resourceBundleList.listIterator(resourceBundleList.size());
        while (listIterator.hasPrevious()) {
            ResourceBundle bundle = listIterator.previous();
            try {
                String messageFromBundle = bundle.getString(key);
                if (messageFromBundle != null && !messageFromBundle.isEmpty()) {
                    return messageFromBundle;
                }
            } catch (MissingResourceException mre) {
                // no op
            }
        }

        // do do not find an entry
        return "";

    }

}
