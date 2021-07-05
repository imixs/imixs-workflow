# The Imixs Rule Engine


The Imixs Workflow project provides two types of RuleEninges which can be used to evaluate business rules.

 - **BPMN RuleEngine** - used to evaluate a business rule based on a BPMN model independent from a running process instance.
 - **Core RuleEngine** - used to evaluate a business rule during the processing life cycle of a process instance. 


	
## The BPMN Rule Engine

The BPMN Rule Engine can be used to evaluate a business Rule independent from a running process instance. 
The business rules are based on conditional events defined within a BPMN model. This is a very simple and powerful mechanism to 
build and evaluate complex business rules based on a visual model. 

The input parameter is a workitem providing a set of business values. The business rules are evaluated as a chain of [conditional events](../modelling/howto.html#Conditional_Events) by applying the business values.

<img src="../images/modelling/rule_01.png"  />


The BPMN Rule Engine can be initalized with an existing Imixs BPMN Model instance. To evaluate the rules a workitem containing the 
business values can be applied by the eval() method: 


	bpmnRuleEngine=new BPMNRuleEngine(model);
	
	workitem = new ItemCollection().model(MODEL_VERSION).task(100).event(10);
	workitem.setItemValue("a", 1);
	workitem.setItemValue("b", "DE");
	
	// evaluate rule
	Assert.assertEquals(200, bpmnRuleEngine.eval(workitem));


See the section [conditional events](../modelling/howto.html#Conditional_Events) to learn how you can model business rules. 
The BPMN RuleEngine is based on the _Imixs Core Rule Engine_ which is explained in the next section. 


## The Core RuleEngine 

The Core RuleEngine evaluates business rules based on the build-in Java Script Engine.
  
A business rule can be written in any script language supported by the JVM. The RuleEngine can be used in a BPMN event to evaluate business rules based on a Script Language. See the [Imixs RulePlugin](../engine/plugins/ruleplugin.html) for more details.

The RuleEngine provides different methods to access item values from a given current workItem. 
 
    // test first value of the workitem attribute 'name'
    var result={}; result.isValid = ('Anna'==workitem.getItemValueString('name'));

A script can also add or update new values for the current workitem by providing the JSON object 'result'.

     var result={ someitem:'Hello World', somenumber:1};


### evaluateBusinessRule

The method _evaluateBusinessRule_ evaluates the business rule defined by the provided event. The method returns the instance of the evaluated result object which can be used
 to continue evaluation. If a rule evaluation was not successful, the method returns null.
 
### evaluateJsonByScript 
 
Th method _evaluateJsonByScript_ converts a JSON String into a JavaScript JSON Object and
evaluates a script.

The JSON Object is set as a input variable named 'data' so that the script can access the json structure in an easy way.
See the following example:
 
    	String json = "{\"id\": 70805774,\"name\": \"simple data\"}";
		String script = "var result={}; result.name=data.name;result.id=data.id;";

		RuleEngine ruleEngine = new RuleEngine();
		ItemCollection result = ruleEngine.evaluateJsonByScript(json, script);
		Assert.assertEquals("simple data", result.getItemValueString("name"));

The method returns an ItemCollection with the result object.


### evaluateBooleanExpression

The method _evaluateBooleanExpression_ evaluates a boolean expression. The method takes a documentContext as argument.


	ItemCollection workitem = new ItemCollection();
	workitem.replaceItemValue("_budget", 1000);

	boolean result = ruleEngine.evaluateBooleanExpression("(workitem._budget && workitem._budget[0]>100)", workitem);
	Assert.assertTrue(result);

All kind of rules can also be evaluated in [conditional events](../modelling/howto.html#Conditional_Events).