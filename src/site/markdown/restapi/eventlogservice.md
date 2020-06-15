# The EventLog Service
The main resource /eventlog defines a service endpoint to access eventLog entries by topic. 

## The /eventlog resources GET
The /eventlog GET resource is used to fetch a list of event log entries by its topic:


| URI                                           | Description                               					   | 
|-----------------------------------------------|------------------------------------------------------------------|
| /eventlog/{topic}                             | all eventLog entries matching a given topic (or set of topics)   |

The topic parma can contain a single topic or a list of topics. In this case the topics need to be separated by swung dash (~).


## The /eventlog resources DELETE
The /eventlog DELETE resource URI is used to delete an eventLog entry:


| URI                                           | Description                                 | 
|-----------------------------------------------|---------------------------------------------|
| /eventlog/{id}                                | deletes a specified evenLog entry by its id |



## The /eventlog resources POST
The /eventlog POST resource URI is used to lock and unlock an eventLog entry:


| URI                                           | Description                                 | 
|-----------------------------------------------|---------------------------------------------|
| /eventlog/lock/{id}                           | locks a specified evenLog entry by its id   |
| /eventlog/unlock/{id}                         | unlocks a specified evenLog entry by its id |



 
## Resource Options
You can specify additional URI parameters to access only a subset of entries: 

| option      | description                                             | example                          |
|-------------|---------------------------------------------------------|----------------------------------|
| maxCount    | number of eventLog entries returned (default 99)        | ..?maxCount=10                   |


   