# Imixs Metrics

Imixs-Workflow provides runtime metrics based on the [Microprofile Metric API](https://microprofile.io/project/eclipse/microprofile-metrics). The Eclipse Microprofile Metrics specification provides an unified way to export Monitoring data ("Telemetry") to management agents and also a unified Java API.

The Imxis-Workflow metric can be collected by the following Rest Service resource:

	http://...../metrics

This is an example how a Imixs-Workflow metric looks like:

	# HELP documents_total The total number of accessed documents.
	# TYPE documents_total counter
	documents_loaded_total{method="load"} 98
	documents_deleted_total{method="delete"} 3
	documents_saved_total{method="save"} 6
	# HELP workitems_processed_total The total number of workitems processeds.
	# TYPE workitems_processed_total counter
	workitems_processed_total{type="workitem",modelversion="protokoll-de-2.0.0",task="1100",event="10",workflowgroup="Protokoll",workflowstatus="Erstellung"}  1
	workitems_processed_total{type="workitem",modelversion="protokoll-de-2.0.0",task="1500",event="100",workflowgroup="Protokoll",workflowstatus="Freigegeben"}  2
	
The Document and Workflow metrics are shown here. Each metric provides a set of tags defining the method and additional metadata like the WorklfowVersion or the current workflow event processed by the Imixs-Workflow Engine.