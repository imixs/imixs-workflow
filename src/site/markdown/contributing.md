# Contribute

Imixs-Workflow is an open source project and we sincerely invite you to participate in it. There are different ways how you can do that. Join the Imixs Workflow Project now on [GitHub](https://github.com/imixs/imixs-workflow). You can help to improve the project by reporting new issues or start a discussions using the
[issue tracker](https://github.com/imixs/imixs-workflow/issues).

## Coding Guidelines

The code style used within the Imixs-Workflow project is based on the Java Conventions with the following details:

- tab policy = Spaces only
- indentation = 4 Spaces (do not use tabs)
- tab size = 4 columns
- line length = 120

Within this project we use Eclipse as the main development IDE. You can import the file 'imixs-code-style.xml' into the Eclipse IDE to get the actual formating rules (_Preferences -> Java-Code-Style Formatter Profile_).

Please do not reformat existing code. If you use an other IDE like Eclipse we recommend to import an appropriate formatting style rule.

## Code Formatting & Checkstyle

For automatic code formatting you can use the [imixs-code-style.xml](../../../imixs-code-style.xml) file which defines a central code formatting. The file is available on [Github](https://raw.githubusercontent.com/imixs/imixs-workflow/refs/heads/master/imixs-code-style.xml).

In addition [imixs-checkstyle-8.44.xml](../../../imixs-checkstyle-8.44.xml) is used for static code analysis based on the [Checkstyle project](https://checkstyle.sourceforge.io/). The configuration is also available on [Github](https://raw.githubusercontent.com/imixs/imixs-workflow/master/imixs-checkstyle-8.44.xml)

Both files contain the Imixs-Workflow settings for formatting and clean-up of Java code as well as code templates.

### Maven

For Maven projects just add the following two plugins. This will format you code automatically during build:

```xml
			<!-- Imixs Code Formatter and Checkstyle -->
			<plugin>
				<groupId>net.revelc.code.formatter</groupId>
				<artifactId>formatter-maven-plugin</artifactId>
				<version>2.26.0</version>
				<configuration>
					<configFile>
						https://raw.githubusercontent.com/imixs/imixs-workflow/refs/heads/master/imixs-code-style.xml</configFile>
					<lineEnding>LF</lineEnding>
				</configuration>
				<executions>
					<execution>
						<phase>validate</phase>
						<goals>
							<goal>format</goal>
						</goals>
					</execution>
				</executions>
			</plugin>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-checkstyle-plugin</artifactId>
				<version>3.3.0</version>
				<configuration>
					<configLocation>
						https://raw.githubusercontent.com/imixs/imixs-workflow/master/imixs-checkstyle-8.44.xml</configLocation>
					<failOnViolation>true</failOnViolation>
				</configuration>
				<executions>
					<execution>
						<phase>verify</phase>
						<goals>
							<goal>check</goal>
						</goals>
					</execution>
				</executions>
			</plugin>
```

### Usage in IDEs

If you do not use maven, the imixs-checkstyle settings can optional also be adapted in VSCode using the [Checkstyle for Java](https://marketplace.visualstudio.com/items?itemName=shengchen.vscode-checkstyle).
To activate the checkstyle rules add the following configuration into your `.vscode/settings.json` file

```yaml
{ ? // Organize Java Imports
    "editor.codeActionsOnSave"
  : { "source.organizeImports": true }, ? // Format with Checkstyle
    "java.checkstyle.configuration"
  : "https://raw.githubusercontent.com/imixs/imixs-workflow/master/imixs-checkstyle-8.44.xml", "editor.formatOnSave": true }
```

## Naming Conventions

There are no explicit name conventions within the Imixs-Workflow project. But some rules are recommended to make your workflow code more easy to reuse.

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

For application specific item names the 'dot.Case' format is recommended. It's basically a convention that makes it easier to see what properties are related by using a prefix.

For example:

person.title="Title"
person.surname="Surname"
person.job.description="Some description"
person.skill.description="Some description"

It's easy to see which items are related. The example defines 3 items related to a person object. Two 'description' fields are related to the item categories 'job' and the 'skill'.

In addition it is possible to collect similar properties by filtering the prefix like 'person.' using regular expressions:

Map<String, Object> result = workitem.entrySet()
.stream()
.filter(p -> p.getKey().matches("^person\\.")) //filter by key prefix
.collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));

You can leave the _Dot.Case_ format if a item name is for general usage over all workflows. For example:

name="..."
description="..."

**Note:** You should not use the general format in different input forms or plug-ins if there meaning is not unique over the complete process. It is recommended to always use a prefix!
