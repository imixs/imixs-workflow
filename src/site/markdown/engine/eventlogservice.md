# The EventLogService 
 
The EventLogService is a service to log events into the database to be
processed in an asynchronous way.

An event that occurs during an update or a processing function within a transaction becomes a fact when the transaction completes successfully. The EventLogService can be used to create this kind of "Change Data Capture"
events. An example is the LuceneUpdateService, which should update the index  of a document only if the document was successfully written to the database.


The service is bound to the current PersistenceContext and stores a defined type of document entity directly in the database to represent an event. These types of events can be queried by clients through the service.

	@EJB
	EventLogService eventLogService;
	....
	    // Transaction A
		eventLogService.createEvent(workitem.getUniqueID(), "MY_TOPIC");
	....
	.......
		
	// Trasaction B	
	List<org.imixs.workflow.engine.jpa.Document> documentList = eventLogService.findEvents(100,"MY_TOPIC");
	for (org.imixs.workflow.engine.jpa.Document eventLogEntry : documentList) {
	  ....
	}
	
		
	