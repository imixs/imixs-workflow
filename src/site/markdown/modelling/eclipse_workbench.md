
#The Eclipse Workbench

The following section gives an overview of the Eclipse Workbench provided by the BPMN2 Modeler.
The concepts provided by Imixs-BPMN are the same as provided by the 
[Eclipse BPMN2 Modeler](https://www.eclipse.org/bpmn2-modeler/).

As shown in the screenshot below, the "workbench" provided by the BPMN2 Modeler splits into separate section like  the Drawing Canvas in the main area of the editor window, collapsible Tool Palette on the right, tabbed Property sheets, and an Outline Viewer with both 
tree and thumbnail views.

<img src="../images/modelling/bpmn_screen_03.png"/>

##Drawing Canvas
The Drawing Canvas occupies the majority of the editing window and behaves as you would expect:
BPMN process elements can be placed on the canvas by selecting them from the Tool Palette and
clicking anywhere on the canvas; elements can be moved by clicking and dragging; elements can be
connected (with, e.g. Sequence Flows, Associations, Data Flows, etc.) by selecting a connection tool from the palette and then first clicking the source element, then the target element.
The canvas also has its own context menu.


##Tool Palette
The Tool Palette is, by default, located along the right edge of the Drawing Canvas. It consists of several “tool drawers” which contain the “tools” that are dragged onto the Drawing Canvas to create BPMN2 elements. The Tool Palette also provides a section called "Imixs-Workflow" containing the Imixs-Workflow BPMN  elements.

##Outline View
The Outline View is separate from the editor and is intended to show a hierarchical, tree oriented view of the file. This view is synchronized with the Drawing Canvas; when an element is selected on the canvas, it is highlighted in the Outline View. Conversely when an item in the Outline is selected, it is also highlighted on the Drawing Canvas.



##Profiles
The Tool Palette supports the concept of "Profiles" which customizes the tools available based 
on the task to be accomplished. Imixs-BPMN provides three differnt profiles:
 
<img src="../images/modelling/bpmn_screen_11.png" />
 
The profile 'Imixs-Workflow' provides the core elements needed to create a BPMN model executeable on the Imixs-Workflow engine. The profile 'Analytic' extends the Tool Palette with common BPMN elements needed to create analytic  models. The profile 'Full' provides all kind of BPMN elements. 

The Imixs-BPMN elements are shown in the section 'Imixs-Workflow' category:

<img src="../images/modelling/bpmn_screen_12.png"/> 
 
 
##Property View
The Property View is used to edit all parameters for the currently selected BPMN2 element. 
This view is synchronized with the selected BPMN element in the Drawing Canvas. The BPMN2 Modeler uses tabbed property sheets to group different kinds of properties. Although the number of tabs and their contents depends on which BPMN2 element is selected, Imixs-BPMN defines custom properties for the Process Definition, the Workflow Task element and the Workflow Event element. 
 
<img src="../images/modelling/bpmn_screen_13.png"/>


Read the following sections to learn more about:
 
  * [Process Properties](./main_editor.html)

  * [Task Properties](./process.html)

  * [Event Properties](./event.html)


 