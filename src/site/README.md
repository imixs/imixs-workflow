#Imixs-Workflow Documenation
Imixs-Workflow documentation is generated using the maven site plugin in the parent project.
The pages are written using markdown. 

## Generate Site

    $ mvn clean site


To skip the report generation:

    $ mvn clean site -DgenerateReports=false



## open tasks:

review workflowservice.md



(review finished: plugin-api.md)



## Plant UML

    java -jar plantuml.jar -verbose plugin_api.uml
    
    java -jar plantuml.jar -verbose adapter_api.uml
    
    java -jar plantuml.jar -verbose analysisplugin.uml