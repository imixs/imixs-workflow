# Microservices and External APIs

Imixs-Workflow supports a great Microservice-Architecture and a powerful Rest-API. Make use of this architecture in all cases you need to integrate external APIs. 

Normally, you would first add an external API via a Maven dependency and then implement it in a plugin or adapter class. Yes, this works, but it also creates unnecessary dependencies in your core application. A better way to connect external APIs is to implement them in a separate microservice and then establish a communication via the Imixs Event-Log API. 



This makes your application lean and more maintainable as you avoid a big single monolithic application artefact.
