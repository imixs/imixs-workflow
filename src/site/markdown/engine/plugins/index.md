# Plugin-API - Overview

The **Imixs Plugin-API** is the extension concept of the Imixs-Workflow Engine. The business logic of an application as also technical interfaces can be implemented by plugins which can easily be activated through the workflow model. 

<img src="../../images/modelling/bpmn_screen_32.png"/>

Each model can import a different set of plugins to provide different functionality. You can also add your own plugins providing custom functionality. 

## Standard Plugins

The Imixs-Workflow Engine already ofers a set of plugins that can be used to extend the functionality of your business application. 

 * [Access Plugin](accessplugin.html) - controls the ACL of a process instance
 * [Analysis Plugin](analysisplugin.html) - analyze different phases of your business process
 * [Application Plugin](applicationplugin.html) - provide application specific data (Forms, Icons)
 * [Approver Plugin](applicationplugin.html) - to manage an approval process
 * [Document-Composer](documentcomposerplugin.html) - compose documents 
 * [History Plugin](historyplugin.html) - generates a human readable processing history 
 * [Interval Plugin](intervalplugin.html) - compute time points on a interval definition
 * [Mail Plugin](mailplugin.html) - sends E-Mail notifications
 * [Result Plugin](resultplugin.html) - computes optional processing results
 * [Rule Plugin](ruleplugin.html) - computes business rules
 * [Split & Join Plugin](splitandjoinplugin.html) - supports split and joins


## The Adapter API

As an alternative to a the Plugin-API, business logic can also be implemented in an Adapter class. An adapter is bound to a single event and can be used for a more fine grained process control. Read more about the concept of the [Imixs Adapter-API](../../core/adapter-api.html).

## What's Next...

Read more about:

 * [The Imixs Plugin-API](../../core/plugin-api.html) 
 * [How to Extend The Plugin-API](howto_extend.html) 
 * [Exception Handling](exception_handling.html) 
 * [The Imixs Adapter-API](../../core/adapter-api.html) 
 
 