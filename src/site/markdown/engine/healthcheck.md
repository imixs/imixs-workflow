# The Imixs Health Check 

The Imixs-Workflow Service provides a HealthCheck implementation based on the [Microprofile Health API](https://microprofile.io/project/eclipse/microprofile-health).

Health checks are used to probe the state of a computing node from another machine (i.e. a kubernetes service controller).
The Imixs Health Check is used to determine the status of the Imixs-Workflow instance. 


The Imixs Health Check  returns the count of workflow models available in the current worklfow instance.

Example:

	{"data":{"model.count":1},"name":"imixs-workflow","state":"UP"}
	
This check indicates the overall status of the workflow engine. If models are
 available also database access and security works.
 
 The Health Check return the status 'DOWN' in case no workflow model is available.  
 
## Wildfly 

The health service endpoint depends on the application server platform. This is an example for the [Wilfly Server](https://wildfly.org/) where the health service can be called on port 9990

	http://localhost:9990/health
	
	{
	  "status":"UP",
	  "checks":[
	    {"name":"imixs-workflow",
	     "status":"UP",
	     "data":
	     {
	     "engine.version":"5.1.11-SNAPSHOT",
	     "model.groups":30,
	     "model.versions":20
	     }
	    }
	  ]
	} 

#### Kubernetes

	spec:
	  containers:
	    ...
	    livenessProbe:
	      exec:
	        command: ["sh", "-c", "curl -s http://localhost:9990/health| grep -q imixs-workflow"]
	      initialDelaySeconds: 60
	    readinessProbe:
	      exec:
	        command: ["sh", "-c", "curl -s http://localhost:9990/health | grep -q imixs-workflow"]
	      initialDelaySeconds: 40
		...
  
	