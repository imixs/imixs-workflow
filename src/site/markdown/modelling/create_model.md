#Create a new BPMN2 file
 Before starting modeling with the Imixs-BPMN Plugin make sure that your current eclipse project is  assigned to the Imixs-BPMN runtime.
 
<img src="../images/modelling/bpmn_screen_29.png"/> 
 
 
The Eclipse New File wizard is used to create a new BPMN2 file. From the Main Menu, click File -> New -> Other… which opens the New File Wizard Dialog box. Navigate to the BPMN2 category and you will see several entries as shown here:

<img src="../images/modelling/bpmn_screen_06.png"/>

 The second entry in the BPMN2 category, Generic BPMN 2.0 Diagram, is contributed by the BPMN2 Modeler plug-in, and can be used to create a new, properly initialized Diagram file. This wizard creates a file that is not intended for deployment to any particular BPM process engine (see Target Runtime Extensions for a detailed discussion). Selecting this entry displays the first page of the wizard, as shown here:

<img src="../images/modelling/bpmn_screen_07.png"/>


This allows you to select the type of diagram you wish to create. This page contains a brief description of each of the diagram types; pick one by clicking the image next to the description and then click the  Next > button. See the Appendix for a more detailed discussion of diagram types.


 The next page of the wizard asks for a location, file name and a target namespace. These fields are already filled in with reasonable defaults, but you may want to change them as necessary.

<img src="../images/modelling/bpmn_screen_08.png"/>

 The target namespace "http://www.imixs.org/bmpn2" links the Imxs BPMN model elements with the  modeler. (The target namespace can also be changed later by the Definitions Properties after the model was created)
 
 Finally click the Finish button to create a template for the selected Diagram type and open the BPMN2 Modeler. 