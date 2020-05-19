# The EventLogService 
 
The EventLogService is used to create and manage event log entries within a running transaction.
EventLog entries can be processed by a client in a secure, asynchronous and transactional manner.

Only in case the transaction from the source event was committed successfully, the event log entry can be read by a client. For example the LuceneUpdateService uses an event log entry to update the index only if a transaction was successful completed. This is also known as the 'Change Data Capture' design pattern.

## Change Data Capture (CDC)
An event that occurs during an update or a processing function within a transaction becomes a fact when the transaction completes successfully. The EventLogService can be used to create this kind of "Change Data Capture" events. 
For example the LuceneUpdateService should update the index  of a document only if the document was successfully written to the database. This can be ensured with the help of the EventLogService. 


The service is bound to the current PersistenceContext and stores a EventLog entities directly into the database. An EventLog entry can be queried by clients through this service.

	@EJB
	EventLogService eventLogService;
	....
	    // BEGIN Transaction A
		eventLogService.createEvent(workitem.getUniqueID(), "MY_TOPIC");
	....
	    // END Transaction A
	.......
		
		
	// BEGIN Transaction B	
	List<org.imixs.workflow.engine.jpa.EventLog> eventList = eventLogService.findEvents("MY_TOPIC",100);
	for (org.imixs.workflow.engine.jpa.EventLog eventLogEntry : eventList) {
	  // the event created in transaction A is no visible...
	  ....
	}
	// END Transaction B

Typically a new EventLog entity is created within the same transaction of the main processing or update life cycle. With this mechanism a client can be
sure that eventLogEntries returned by the EventLogService are created during a committed Transaction. If the transaction was rolled back for some reason, the EventLog entry will never be written to the database.
	
		
## The EventLog Entity

The EventLog entity describes a unique event created during the processing life-cycle of a workitem or the update life-cycle of a Document.

An EventLog is an immutable entity. The object contains the following properties:

 * id - identifier for the event log entry
 * ref - the reference id of the corresponding workitem or document entity
 * topic - the topic of the eventlog
 * created - the creation timestamp
 * data - an optional data field
 * timeout - an optional timestamp indicated the earliest processing time. 

The 'data' attribute of an eventLog is optional and can hold any kind of event specific data (e.g. a Mail Message).

**Note:** for the same document reference ($uniqueid) there can exist different eventlog entries. Eventlog entries are unique over there internal ID. You can use the method _findEventsByRef_ to verify if a event log entry for a defined Reference was already created by another transaction.  	


## TransactionID

In different to a document entity, a workitem entity hold a *$transactionID* identifying the last processing life-cycle. When an eventLog entry is consumed asynchronous, a client may verify the *$transactionID* item stored in the data field with the last *$transactionID* stored in the workitem. In case the *$transactionID* has changed a client can discard the eventLog entry. This mechanism ensures that an eventLog entity is tied to a specific unique transaction only. 

For example an AsyncEvent must only be executed if the *$transactionID* matches the current workitem status. 


## Timeout

The optional data attribute *timeout* .....