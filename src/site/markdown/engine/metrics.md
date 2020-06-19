# Imixs Metrics

Imixs-Workflow provides runtime metrics based on the [Microprofile Metric API](https://microprofile.io/project/eclipse/microprofile-metrics). The Eclipse Microprofile Metrics specification provides an unified way to export Monitoring data ("Telemetry") to management agents and also a unified Java API.

The Imixs-Metric Serivce is disabled per default. To enable the feature set the property 'metrics.enabled' to 'true.

	metrics.enabled=true

## How to access Imixs-Workflow Metrics

The Imxis-Workflow metrics can be collected by the metric rest endpoint depending on the application server.

    # Wildfly
    http://[HOST]:9990/metrics
    # Payara
    http://[HOST]:8080/metrics
    

This is an example how a Imixs-Workflow metric looks like:
	application_workitems_total{event="20",modelversion="invoice-1.2",task="2001",type="workitem",workflowgroup="Invoice",workflowstatus="Open"} 1.0
	application_workitems_total{event="1",modelversion="todo-1.0.0",task="2000",type="workitem",workflowgroup="ToDo",workflowstatus="Offen"} 3.0
	application_documents_total{method="save"} 8.0
	application_documents_total{method="delete"} 3.0
	application_documents_total{method="load"} 133.0
	application_transactions_total 5.0


The Document and Workflow metrics are shown here. Each metric provides a set of tags defining the method and additional metadata like the WorklfowVersion or the current workflow event processed by the Imixs-Workflow Engine.


## Anonymised Metrics

The metrics provided by the Imixs workflow platform are anonymized by default. This is important in order to comply with the data protection directives of many countries as well as within the European Union. But in some cases, it may be necessary to provide personalized metrics. For this purpose you can set the flag '_metrics.anonymised_' to 'false' (default = true). In this case, the metrics are supplemented with the  tag 'user' providing the corresponding userid. 

	METRICS_ENABLED: "true"
	METRICS_ANONYMISED: "false"



## Metrics Endpoint

The Metrics are based on the [Eclipse Microprofile Metric API](https://microprofile.io/project/eclipse/microprofile-metrics) which means that the metric api is part of your application server. Depending on your platform you will get a lot of hardware and environment metrics together with the Imixs-Workflow metrics. The endpoint to access the metrics depends on your server environment. 

### Prometheus Configuration for Widlfly

The default metric api endpoint for Wildfly is:

	https://[HOST]:9990/metrics
	
A Prometheus configuration to scrape the metrics from Imixs-Workflow can look like in the following example:

	global:
	  scrape_interval:     15s # By default, scrape targets every 15 seconds
	  # Attach these labels to any time series or alerts when communicating with
	  # external systems (federation, remote storage, Alertmanager).
	  external_labels:
	    monitor: 'imixs-monitor'
	
	scrape_configs:
	  # Prometheus itself
	  - job_name: 'prometheus'
	    scrape_interval: 5s
	    static_configs:
	      - targets: ['localhost:9090']
	     
	  # Imixs-Office Job
	  - job_name: 'imixs'
	    scrape_interval: 5s
	    metrics_path: /metrics
	    static_configs:
	      - targets: ['app:9990']      
   


### Prometheus Configuration for Payara

The default metric api endpoint for Payara Server is:

	https://[HOST]:8080/metrics
	
A Prometheus configuration to scrape the metrics from Imixs-Workflow can look like in the following example:


	global:
	  scrape_interval:     15s # By default, scrape targets every 15 seconds.
	  # Attach these labels to any time series or alerts when communicating with
	  # external systems (federation, remote storage, Alertmanager).
	  external_labels:
	    monitor: 'imixs-monitor'
	
	scrape_configs:
	  # Prometheus itself
	  - job_name: 'prometheus'
	    scrape_interval: 5s
	    static_configs:
	      - targets: ['localhost:9090']
	     
	  # Imixs-Office Job
	  - job_name: 'imixs'
	    scrape_interval: 5s
	    metrics_path: /metrics
	    static_configs:
	      - targets: ['app:8080']        
 

      
