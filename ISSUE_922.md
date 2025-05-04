# Lösung

Die Klasse `WorkflowContext` muss raus!!!!!!!!!!!

- Der `WorkflowKernel` muss in der Methode Process eine exclusive Instanz von `BPMNModel` erzeugen bzw. im COnstructor übergeben bekommen und mit dieser dann thread save arbeiten

- Der der `WorkflowContext` ctx ausschließlich im Constructor verwendet wird und nur das model hält ist hier der Einstieg

- Der Constructor vom Kernel muss das `BPMNMOdel` Objet erzeugen.

- Entsprechend muss die methode `updateModelVersionByEvent` ebenfalls angepasst werden udn auch das BPMMOdel neue laden!!
  BPMNModel model
