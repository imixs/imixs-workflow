imixs-workflow
==============

Imixs Workflow is an open source project based on Java technology. You can use Imixs Workfow in any kind of java application. Imixs Workflow provides a full featured Workflow Management System (WFMS) based on the Java EE  specification. 

See the Project Home for more informations: http://www.imixs.org. 

To join the project follow us on GitHub: https://github.com/imixs/imixs-workflow

imixs-bpmn
-----------
Imixs Workflow models are based on BPMN 2.0 standard. You can create you model using the
Eclipse Plugin Imixs-BPMN.
The Imixs Model REST Service provides different methods to manage model through RestAPI.
Find details at: http://www.imixs.org/xml/restservice/modelservice.html


curl 
-----
You can use curl to upload a Imixs BPMN 2.0 model into a Imixs Workflow instance:

curl --user admin:adminpassword --request POST -Tticket.bpmn http://localhost:8080/workflow/rest-service/model/bpmn


