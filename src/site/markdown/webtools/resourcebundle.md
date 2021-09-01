# Imixs ResourceBundleHandler

With the ui util class '*org.imixs.workflow.faces.util.ResourceBundleHandler*' a CDI Bean or faces page can lookup a label in different bundles. This simplifies the front end implementation as the client does not have to know the bundle a specific resource is located:


	<h1>#{resourceBundleHandler.findMessage('application_title')}</h1>

The ResourceBundleHandler load the bundles based on the current user locale. Resource bundle instances created by the getBundle factory methods are cached  by default, and the factory methods return the same resource bundle instance  multiple times if it has been cached. For that reason a RequestScoped bean is used here.
 
The class searches for the resource bundles with the base names '*bundle.messages*', '*bundle.app*' and '*bundle.custom*'. You can overwrite the bundle names with the imixs property value 'resourcebundle.names'. 

	resourcebundle.names=messages,my-app
 
 
 
## CDI Integration

In a CDI bean you can also use the resourceBundleHandler to lookup keys

	@Inject
	protected ResourceBundleHandler resourceBundleHandler = null;
	...
	resourceBundleHandler.findMessage("space.manager"));


 
## Priority
 
The later entries have a higher priority in case a key is stored in multiple bundles.

