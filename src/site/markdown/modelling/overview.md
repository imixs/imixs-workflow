# Overview

<p class="lead"> 
<strong>Imixs-BPMN</strong> is based on the <a href="https://www.open-bpmn.org/">Open BPMN Modeler</a>. A free and open modeling platform to view, create and maintain BPM models based on the BPMN 2.0 standard. Open BPMN can be used by business analysts to design a top level business process, as also by architects and developers to model the technical details of complex processing logic.

</p>

A Imixs-BPMN Model can be executed within the <strong>Imixs-Worklfow engine</strong>. It extends the BPMN2 metamodel and allows you to add Plug-In and Adapter Classes to control the process flow and the behavior of the Imixs-Workflow engine.

## General concepts

As shown in the screenshot below, Imixs-BPMN provides all of the BPMN features in a Tool Palette at the right side of the editor panel. The Drawing Canvas in the main area of the editor window is used to place the BPMN elements into the model.

<img src="../images/modelling/bpmn_screen_03.png"/>

BPMN elements can be placed on the canvas by selecting them from the Tool Palette and clicking anywhere on the canvas. Elements can be moved on the canvas and
connected (with, e.g. Sequence Flows, Associations, Data Flows, etc.) by selecting a connection tool from the palette and then first clicking the source element.

## The Imixs-BPMN Tools

The Tool Palette is, by default, located along the right edge of the Drawing Canvas. It consists of several standard BPMN2 elements that can be dragged onto the Drawing Canvas. The Imixs-BPMN plug-in provides a separate section called "Imixs-Workflow". This category contains the "Imixs-Workflow Task" and the "Imixs-Workflow Event" Elements. Both elements are extensions to the BPMN Task and Event Elements defined by the BPMN 2.0 spec. The elements can be dragged like any other BPMN element onto the canvas to describe a workflow to be executable inside the Imixs-Workflow engine.

### The Workflow-Task

<img src="../images/modelling/bpmn_screen_04.png"/>

The "Workflow-Task" is used to describe a Task inside a process model controlled by the Imixs-Workflow engine. The Task Element typically reflects a task to be processed by a participant during the process life-cycle of a process instance. A Imixs Workflow-Task contains a custom set of properties used to describe the status of a process instance. Read more about the Task Element in the [section Task Properties](process.html). The "Workflow-Task" Element is an extension to the BPMN 2.0 Element "Task".

### The Workflow-Event

<img src="../images/modelling/bpmn_screen_05.png"/>

The "Workflow-Event" is used to describe a transition from one Task into another. The Event Element contains a custom set of properties to be evaluated during the execution by the Imixs-Workflow engine. Read more about the Event Element in the [section Event Properties](activities.html). The "Workflow-Event" is an extension to the BPMN 2.0 Intermediate CatchEvent.
