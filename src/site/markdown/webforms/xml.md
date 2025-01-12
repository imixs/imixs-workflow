# XML Handling

When developing custom components for Imixs Forms, you'll work with XML documents representing workflow data. The `DataManager` provides a convenient API for handling these XML objects.

## XML Document Types

The DataManager maintains three main XML objects:

- **taskXML**: Contains the BPMN task definition
- **eventsXML**: Contains available workflow events
- **workitemXML**: Contains the current workitem data

## Reading XML Data

Use the `getItemValue` method to read data from any XML object:

```javascript
class MyComponent {
  constructor(dataManager) {
    this.dataManager = dataManager;
  }

  readWorkflowData() {
    // Read from workitem
    const status = this.dataManager.getItemValue(
      "$workflowstatus",
      this.dataManager.workitemXML
    )?.value;
    const creator = this.dataManager.getItemValue(
      "$creator",
      this.dataManager.workitemXML
    )?.value;

    // Read from task
    const taskName = this.dataManager.getItemValue(
      "txtname",
      this.dataManager.taskXML
    )?.value;
  }
}
```

The `getItemValue` method always returns an object containing `value` and `type`.

## Writing XML Data

Use setItemValue to write or update data in the workitem:

```javascript
updateWorkitem() {
    // Add or update string value
    this.dataManager.setItemValue('subject', 'Hello World', null, this.dataManager.workitemXML);

    // Add number with specific type
    this.dataManager.setItemValue('_budget', 1000.00, 'xs:double', this.dataManager.workitemXML);
}
```

Always provide the XML document (context) when setting values.

## Complex XML Items

Some items (xmlItem type) contain multiple values:

```javascript
readHistoryData() {
    const history = this.dataManager.getItemValue('txtworkflowhistory', this.dataManager.workitemXML);
    if (history) {
        history.forEach(entry => {
            // Each entry has value and type
            console.log(`${entry.value} - ${entry.type}`);
        });
    }
}
```

## Creating New Documents

To create a new XML document with proper namespaces:

```javascript
const doc = this.dataManager.createDocument();
```

## Converting to String

To serialize an XML document to string:

```javascript
const xmlString = this.dataManager.toXMLString(this.dataManager.workitemXML);
```

## Best Practices

1. Always use DataManager methods for XML operations
2. Check for null when reading values
3. Provide correct XML context when setting values
4. Use proper XML Schema types when needed:
   - xs:string (default)
   - xs:boolean
   - xs:int
   - xs:double
5. XML namespaces are handled automatically

## Example Component

Here's a complete example of a custom component:

```javascript
class MyWorkflowComponent {
  constructor(dataManager) {
    this.dataManager = dataManager;
  }

  async initialize() {
    // Read workflow data
    const status = this.dataManager.getItemValue(
      "$workflowstatus",
      this.dataManager.workitemXML
    )?.value;
    const owner = this.dataManager.getItemValue(
      "$owner",
      this.dataManager.workitemXML
    )?.value;

    // Update workflow data
    this.dataManager.setItemValue(
      "_priority",
      "high",
      null,
      this.dataManager.workitemXML
    );
    this.dataManager.setItemValue(
      "_budget",
      2500.0,
      "xs:double",
      this.dataManager.workitemXML
    );

    // Process task events
    const events = await this.modelManager.getTaskEvents();
    console.log("Available events:", events);
  }
}
```

This guide covers the basics of XML handling in Imixs Forms. For more detailed information about the workflow engine, refer to the [Imixs-Workflow documentation](https://www.imixs.org/doc/index.html).
