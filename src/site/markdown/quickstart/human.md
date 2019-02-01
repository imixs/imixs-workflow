# What Means Human Centric Workflow?

**Imixs-Workflow** is supporting human skills, activities and relieves collaboration in a task-oriented manner. For this, each process instance can be assigned to different actors. 
The main objective is to support the human actors and provide them with relevant information about the business process. The workflow engine ensures that the business process is aligned to predetermined business rules:
 
  * Who is the owner of a business process
  * Who is allowed to access and modify the data
  * Who need to be informed next
  
In that way Imixs-Workflow help users in starting a new process, finding and processing open tasks and to complete current jobs. The Workflow Engine automatically routes open tasks to the next actor and notifies users about open tasks depending on the current process definition. 

Each business process can involve different users to interact with the Workflow Management System.
These users are called the *actors*. An Actor can either start, update or read a process instance and also the embedded business data
Imixs Workflow allows you to assign any kind of business data with a running process instance.
You can use Imixs workflow to control access to a process instance in a fine-grained way using an ACL. This includes the read and write access for users and roles. The ACL can be defined via the BPMN model for each Task or Event separately. 

<img src="../images/bpmn-example02.png" width="500px" />
 
 