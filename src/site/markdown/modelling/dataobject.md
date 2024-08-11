# BPMN Data Objects

BPMN Data Objects are used to specify information, which is not related to the flow of the process. These elements are not executable but can be used for readability, analysis or execution of a business processes.

In Imixs-Workflow Data Objects can be associated with an Imixs Task or Event element. 

<img src="../images/modelling/bpmn_screen_34.png" />

When executing an Event by a Plug-In or an Adapter the application can access the data stored in an associated Data Object.
For example, when creating a new invoice process a invoice-template may be needed to produce a new document. This template code can be stored in a Data Objects.

## How To Access the Data Object

Plugins or Adapter classes can access the data object by the ModelService: 

### Plug-In Example:

	public class MyPlugin implements Plugin {
	   	@Inject
	    ModelService modelService;

		public ItemCollection run(ItemCollection workitem, ItemCollection event) throws Exception {
			// extract the data from a DataObject
			String data = modelService.getDataObject(event, "MyObject");
			....
		}
	}

This example plug-in code extracts the Data stored in a BPMN Data Object with the name 'MyObject' associated to an Event Element. The method 'getDataObject' returns null if no DataObject with the given name is associated with the event element.


### Adapter Example:

	public class DemoAdapter implements org.imixs.workflow.SignalAdapter {
	    // inject services...
	    @EJB
	    ModelService modelService;
	    ...
	    @Override
		public ItemCollection execute(ItemCollection document, ItemCollection event) throws AdapterException {
			...
			// extract the data from a DataObject
			String data = modelService.getDataObject(event, "MyObject");
			....
		}
	}
	

This adapter example extracts the Data stored in a BPMN Data Object with the name 'MyObject' associated to an Event Element. The method 'getDataObject' returns null if no DataObject with the given name is associated with the event element.

**Note:** A Data Object must be associated with an Imixs Task or Event element to get the data. 


### Get Data from a Task:

Data Object can be either associated with a Event or a Task element. To get the Data associated with an Imixs Task Element you need to load the Task first:

		ItemCollection task = model.getTask(1000);
		// extract the data from a DataObject
		String data = modelService.getDataObject(task, "MyObject");

### Get the DataObject form the Model

As an alternative you can access the DataObject directly from the Open-BPMN model instance. See the following example:

	public class DemoAdapter implements org.imixs.workflow.SignalAdapter {
	    // inject services...
	    @Inject
	    ModelService modelService;
	    ...
		public ItemCollection execute(ItemCollection workitem, ItemCollection event) throws AdapterException {
			...
			// fetch DataObjects from BPMN model
			BPMNElementNode bpmnElementNode = modelService.getBPMNElementNode(workitem.getModelVersion(),
				event.getItemValueString("id"));

			Assert.assertNotNull(bpmnElementNode);
			Set<DataObject> dataObjects = bpmnElementNode.getDataObjects();
			// extract the data from a DataObject
			....
		}
	}

 