# The AdminP Service
The resource _/adminp_ provides methods to create administrative jobs. Jobs can be monitored with the [Imixs-Admin Client](../administration.html).
 
 
## GET Jobs
The GET method is used to read all running ore completed jobs:


| URI                     | Method | Description                                                        | 
|-------------------------|--------|------------------------------------------------------------|
| /jobs                   | GET    | returns all jobs from the AdminP interface.                | 

 


## POST/PUT a new job
The methods PUT or POST allow to create a new job:


| URI          | Method      | Description                               | 
|--------------|-------------|------------|
| /            | POST, PUT   | posts a new job to be processed by the AdminPServcie. The post data is expected in  xml format   |


The following curl example shows how to create a new job to update the Lucene fulltextindex.  

    curl --user admin:adminpassword -H "Content-Type: text/xml" -d \
      '<document xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xs="http://www.w3.org/2001/	XMLSchema"> \
       <item name="type"><value xsi:type="xs:string">adminp</value></item> \
       <item name="job"><value xsi:type="xs:string">REBUILD_LUCENE_INDEX</value></item> \
       <item name="numblocksize"><value xsi:type="xs:int">1000</value></item> \
       <item name="numindex"><value xsi:type="xs:int">0</value></item> \
       <item name="numinterval"><value xsi:type="xs:int">1</value></item> \
    </document>' \
    http://localhost:8080/api/adminp/jobs



In case the job is not createable the attribute '$error_code' will be returned in the response. 


## DELETE a Document
The methods DELETE allow to remove a running or completed job:


| URI          | Method      | Description                               | 
|--------------|-------------|------------|
| /{uniqueid}  | DELETE | updates ore deletes a document  |






  
   