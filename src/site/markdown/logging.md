# Imixs-Workflow - Logging

The Imixs-Workflow engine provides a logging mechanism based on the Java Logging. There are several log levels defined:

## Loglevel INFO

Loglevel INFO shows general information about the WorkflowService.

**Example:**

    [org.imixs.workflow.bpmn.BPMNParser] (default task-3) BPMN Model 'system-en-1.0.0' parsed in 21ms
    [org.imixs.workflow.WorkflowKernel] (default task-27) processing=85676103-43d9-4193-a8ce-30c96f0f7b31, MODELVERSION=2.0.0, $taskid=1100, $eventid=10

## Loglevel FINE

Loglevel FINE prints processing information and can be used to analyse performance issues on a low level.

**Example:**

    [org.imixs.workflow.engine.DocumentService] (default task-32) ...'85676103-43d9-4193-a8ce-30c96f0f7b31' loaded in 3ms
    [org.imixs.workflow.WorkflowKernel] (default task-32) ...Plugin 'org.imixs.workflow.engine.plugins.ResultPlugin' processing time=0ms
    [org.imixs.workflow.WorkflowKernel] (default task-32) ...Plugin 'org.imixs.workflow.engine.plugins.RulePlugin' processing time=0ms
    [org.imixs.workflow.WorkflowKernel] (default task-32) ...Plugin 'org.imixs.marty.plugins.ProfilePlugin' processing time=0ms
    [org.imixs.workflow.engine.WorkflowService] (default task-32) ...total processing time=184ms

## Loglevel FINEST

Loglevel FINEST shows detailed processing information and is used to debug the workflow engine.

**Example:**

    logger.finest("......updated entity: " + entity.getUnqiueID);

# Activate Logging in Your Runtime

To get log information into your sever log you need to set the corresponding log level in your Runtime. The following section describes how to set the logging for different Runtimes.

## Wildfly

If you run Imixs-Workflow in Wildfly you can set the log level in the `standalone.xml` file.
The standalone.xml file contains different log categories and the general log level. These settings can be changed in the `jboss:domain:logging:` subsystem section:

```xml
    .....
        <subsystem xmlns="urn:jboss:domain:logging:8.0">
            <console-handler name="CONSOLE">
                <!-- INFO | DEBUG -->
                <level name="DEBUG" />
                <formatter>
                    <named-formatter name="COLOR-PATTERN" />
                </formatter>
            </console-handler>
            <periodic-rotating-file-handler name="FILE" autoflush="true">
                <formatter>
                    <named-formatter name="PATTERN" />
                </formatter>
                <file relative-to="jboss.server.log.dir" path="server.log" />
                <suffix value=".yyyy-MM-dd" />
                <append value="true" />
            </periodic-rotating-file-handler>
            <logger category="org.imixs">
                <level name="DEBUG" />
            </logger>
			......
			..........
            <root-logger>
                <level name="INFO" />
                <handlers>
                    <handler name="CONSOLE" />
                    <handler name="FILE" />
                </handlers>
            </root-logger>
            <formatter name="PATTERN">
                <pattern-formatter pattern="%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n" />
            </formatter>
            <formatter name="COLOR-PATTERN">
                <pattern-formatter pattern="%K{level}%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n" />
            </format>
        </subsystem>
	......
```

After you changed the settings you need to restart the server. If you use Docker just build a Docker image

    mvn clean install -Pdocker

You can refine the logging for any java library or for specific classes only.

## Payara

Running Imixs-Workflow in glassfish/payara server you can can increase the log level for a java-package or a single java class.

First you need to log into the server and run the `asadmin` command:

```bash
$ cd ~/appserver/glassfish/bin
$ asadmin
```

Next you can list the current loggers:

```bash
asadmin> list-log-levels
Enter admin password for user "admin">
ShoalLogger
com.hazelcast
com.sun.enterprise.server.logging.GFFileHandler
com.sun.enterprise.server.logging.SyslogHandler
.......
```

To set a specific log level run:

```
set-log-levels org.imixs"=FINEST
```

This setting activates the logging for Imixs-Worklfow.
