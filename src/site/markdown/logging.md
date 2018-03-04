# Imixs-Workflow - Logging

The Imixs-Worklfow engine provides a logging mechanism based on the Java Logging. There are several log levels defined:

## Loglevel INFO

Loglevel INFO shows general information about the WorkflowService. 

**Example:**

	[org.imixs.workflow.WorkflowKernel] (default task-27) processing=85676103-43d9-4193-a8ce-30c96f0f7b31, MODELVERSION=2.0.0, $processid=1100, $activityid=10
	

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
