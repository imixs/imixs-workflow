# The Imixs Health Check 

The Imixs-Workflow Service provides a HealthCheck implementation based on the [Microprofile Health API](https://microprofile.io/project/eclipse/microprofile-health).

Health checks are used to probe the state of a computing node from another machine (i.e. a kubernetes service controller).
The Imixs Health Check is used to determine the status of the Imixs-Workflow instance. 

This check indicates the overall status of the workflow engine. If models are available and also database access and security works the service answers with HTTP 200 and a body that looks like this:
 
	{
		"status": "UP",
		"checks": [
			{
				"name": "imixs-workflow",
				"status": "UP",
				"data": {
					"engine.version": "5.2.9-SNAPSHOT",
					"model.groups": 1,
					"model.versions": 1,
					"index.status": "ok",
					"database.status": "ok"
				}
			},
			{
				"name": "ready-deployment.imixs-office-workflow.war",
				"status": "UP"
			}
		]
	}
 
The Health Check return the status 'DOWN' with HTTP 503 in case no workflow model is available the database and index checks failed.  For example in case of a database error the result looks like this:

	{
		"status": "DOWN",
		"checks": [
			{
				"name": "imixs-workflow",
				"status": "DOWN",
				"data": {
					"index.status": "ok",
					"database.status": "failure"
				}
			},
			{
				"name": "ready-deployment.imixs-office-workflow.war",
				"status": "UP"
			}
		]
	}
 
 
 
## Wildfly 

The health service endpoint depends on the application server platform. This is an example for the [Wilfly Server](https://wildfly.org/) where the health service can be called on port 9990

	http://localhost:9990/health
	

### Kubernetes

To validate the health status in Kubernetes you can do a readinessProbe and livenessProbe:

	spec:
	  containers:
	    ...
	    livenessProbe:
	      httpGet:
	        path: /health
	        port: 9990
	      initialDelaySeconds: 120
	      periodSeconds: 10
	      failureThreshold: 3
		...

**Note:** You need to publish port 9990 in a service. 		
			
  
	