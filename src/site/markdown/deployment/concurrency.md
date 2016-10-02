#Concurrency and Optimistic Locking
The Imixs-Workflow engine supports an optimistic locking mechanism. Optimistic locking is based on the assumption that most transactions dont't conflict with other transactions, allowing concurrency to be as permissive as possible when  allowing transactions to execute. Therefore the Imixs-Workflow engine holds an attribute '$version' providing the version number of the corresponding entity. So in case two users open the same document, change data and call the save() method, optimistic locking will be activated. This means that  an OptimisticLockException is thrown when the second user tries to save the document.
 
## How to Disable Optimistic Locking  
There are two mechanisms to disable the optimistic locking. Both mechanisms guarantee that both users can save the document. The last call of the save() method wins. This behavior  is different to the default behavior as explained before. To disable the build in optimistic locking mechanism an application can either remove the $version property from a document before saved:
 
	...
	workitem.removeItem("$version");
	workitem=documentService.save(workitem);
    ....
 
or the global property "DISABLE_OPTIMISTIC_LOCKING" can be set to _true_ by the ejb-jar.xml deplyoment descriptor:
 
	...
	<session>
		<ejb-name>DocumentService</ejb-name>
		<env-entry>
			<description>disable optimistic locking</description>
			<env-entry-name>DISABLE_OPTIMISTIC_LOCKING</env-entry-name>
			<env-entry-type>java.lang.Boolean</env-entry-type>
			<env-entry-value>true</env-entry-value>
		</env-entry>
	</session>
	...


