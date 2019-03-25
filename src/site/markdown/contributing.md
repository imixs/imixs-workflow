# Contribute

Imixs-Workflow is an open source project and we sincerely invite you to participate in it. There are different ways how you can do that. Join the Imixs Workflow Project now on [GitHub](https://github.com/imixs/imixs-workflow). You can help to improve the project by reporting new issues or start a discussions using the 
[issue tracker](https://github.com/imixs/imixs-workflow/issues).


## Coding Guidelines

We use Eclipse as the main development platform. Also the code style of Imixs-Workflow is based on the Eclipse Code Style rules. 

- tab is used for indention
- line length is set to 120

You can import the file 'imixs-code-style.xml' into the Eclipse IDE to get the actual formating rules (_Preferences -> Java-Code-Style Formatter Profile_).

Please do not reformat existing code. If you use an other IDE like Eclipse we recommend to import an appropriate formatting style rule. 


## Naming Conventions

There are no explicit name conventions within the Imixs-Worklfow project. But some rules are recommended to make your workflow code more easy to reuse. 

### ItemCollection Item Names

Within a _org.imixs.workflow.ItemCollection_ you can store your data into named items. The item name can be freely chosen. But the following rules should be taken into account:

#### Lower Case Item Names
Item names should always be lower cased. 

#### Internal Item Names '$...'
Item names starting with a '$' are reserved for internal usage only. The Imixs-Workflow engine uses the '$' prefix to indicate items which should not be changed by an application. For example 

	$uniqueid
	$taskid
	$eventid

Item names starting with a '$' are controlled by the Imixs-Workflow engine only. 

#### Dot.Case Format

For application specific item names the 'dot.Case' format is recommended. It's basically a convention that makes it easier to see what properties are related.

For example:

	person.title="Title" 
	person.surname="Surname" 
	person.job.description="Some description"

It's easy to see which items are related. You can easily collect similar properties by filtering the prefix like 'person.' to only see the properties for person. 

	 Map<String, Object> result = workitem.entrySet()
         .stream()
         .filter(p -> p.getKey().startsWith("person.")) //filter by key prefix
         .collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));
         

You can leave the Dot.Case format if a item name is for general usage over all workflows. For example:

	name="..."
	description="..."
	
Note: you should not use the general format in different input forms or plug-ins if there meaning is not unique over the complete process.



