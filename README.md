#Imixs-Workflow

Imixs-Workflow is an open source workflow engine based on the Java Enterprise Architecture (JEE). The Imixs-Workflow project provides components to build human-centric workflow applications within a flexible and robust framework. The business logic can be modeled and executed using the Business Process Modelling Notation - BPMN 2.0 standard. 

See the [Project Home](http://www.imixs.org) for more information. 

To join the project follow us on [GitHub](https://github.com/imixs/imixs-workflow)

##imixs-bpmn
Imixs-BPMN is a eclipse based modeling tool to design a business process based on the BPMN 2.0 standard. These models can be executed by the Imixs-Workflow engine. 

Read more about imixs-bpmn on the [project home](http://www.imixs.org/modeler/). 


##imixs-jax-rs
Imixs-Workflow provides a Rest API to integrate the imixs-workflow engine into different kinds of architecture or platforms. Using imixs-workflow as a microservice allows to run Imixs-Workflow in the background and implement clients with on different platforms. 
You can find information about the Rest API [here](http://www.imixs.org/xml/). 



#build
Imixs-Workflow is based on maven and all artifacts are provided in the [maven central repository](http://search.maven.org/#browse).

This is an example how to add a maven dependency of imixs-workflow into your own project:




	<dependencies>
	   
	   .....
		<!-- Imixs Workflow -->
		<dependency>
			<groupId>org.imixs.workflow</groupId>
			<artifactId>imixs-workflow-engine</artifactId>
			<version>${org.imixs.workflow.version}</version>
		</dependency>

		<dependency>
			<groupId>org.imixs.workflow</groupId>
			<artifactId>imixs-workflow-jax-rs</artifactId>
			<version>${org.imixs.workflow.version}</version>
		</dependency>

		<dependency>
			<groupId>org.imixs.workflow</groupId>
			<artifactId>imixs-workflow-faces</artifactId>
			<version>${org.imixs.workflow.version}</version>
		</dependency>
	</dependencies>
	...

##Sample Application

The [Imixs-Workflow Sample Application](https://github.com/imixs/imixs-jsf-example) 
can be used as a scaffold for own projects.


#Joining the Project

If you have any questions post them on [issue tracker](https://github.com/imixs/imixs-workflow/issues)

#License

Imixs-Workflow is free software, because we believe that an open exchange of experiences is fundamental for the development of useful software. All results of this project are provided under the GNU General Public License.
