# Data

In some situations, when executing a business process, there need to be additional data defined to excecute a specific workflow task.
For example, when creating a new invoice process a invoice-template may be needed to produce a new document.
In Imixs-BPMN, data can be modeled by BPMN2 Data Objects which can be assigned to one or may Imixs Task Elements. 
These data objects can will be included into the task element in the attribute _dataObject_. 

<img src="../images/modelling/bpmn_screen_34.png" />

Plugins or applications can access these data object by the model interface.


		ItemCollection task = model.getTask(1000);
		List<?> dataObjects = task.getItemValue("dataObjects");
		Assert.assertNotNull(dataObjects);
		// get first data object...
		String[] data=(String[]) dataObjects.get(0);
		// get name
		Assert.assertEquals("Invoice Template",data[0]);
		// get documentation
		Assert.assertEquals("Some data ...",data[1]);
		


 