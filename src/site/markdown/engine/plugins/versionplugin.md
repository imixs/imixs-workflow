#Version Plugin 
The Imixs Version-Plugin handles the creation and the management from versions of a running process instance.  A versioning process can be configured through the {{{/modeler/}Imixs Modeler}} from the activity property tab 'Version'. The plugin can create a new version of a source workitem (master version), or converting an existing version of a workitem
 back into a master version.  All versions of a workitem can be identified by the property $workitemIDRef which points to the $uniqueid of the master version. 
 
The [Imixs-BPMN Modeller](../../modelling/index.html) provides two different modes in which the Version-Plugin can run.
 
##Create a new Version from a source workitem
In this mode the Version-Plugin creates a new version of the current workitem. The two workitems are identically except the attributes $unqiueID and $workitemidRef. The attribute $workitemidRef points to the $uniqueid form the source workitem. So the availability of this property indicates that the workitem is a version of the source workitem. The source workitem has typically no $workitemidRef attribute - except if the source worktitem itself is a version from another worktiem. The Source Workitem is also named Master Version. 
 
After the new version is created the Version-Plugin processes the version with the activity provided by the model (numVersionActivityID). This step is optional.

##Convert an existing version back into a master version
In this mode the plugin converts an existing version of a workitem back into a master version. This means that the $workitemIDRef will be removed. The master version will be processed by the activity provided by the model. The $workitemidRef property of the old master version will be updated to the $uniqueid form the new master version.
 
 
##ActivityEntity 
The definition of creating versions or master versions is stored in the activity entity of the workflow model.  There are the following properties defined through the activity entity:

 * keyVersion - defines the version mode (1=create new version, 2=convert back to master version)
 * numVersionActivityID - optional activity entity which will be processed by the corresponding version

The Version Plugin depends on the Imixs Workflow engine. So the Plugin can not be used in other implementations.  

