# The Rule Engine
The Imixs RuleEngine evaluates business rules based on the buildin Java Script Engine.
  
A business rule can be written in any script language supported by the JVM. The RuleEngine can be used in a BPMN event to evaluate bueness rules based on a Script Language. See the [Imixs RulePlugin](../engine/plugins/ruleplugin.html) for more details.

The RuleEngine provides different methods to access item values from a given current workItem. 
 
    // test first value of the workitem attribute 'txtname'
    var isValid = ('Anna'==workitem.txtname[0]);

A script can also add or update new values for the current workitem by providing the JSON object 'result'.

     var result={ someitem:'Hello World', somenumber:1};


## evaluateBusinessRule

The method _evaluateBusinessRule_ evaluates the business rule defined by the provided event. The method returns the instance of the evaluated result object which can be used
 to continue evaluation. If a rule evaluation was not successful, the method returns null.
 
## evaluateJsonByScript 
 
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


## evaluateBooleanExpression

The method _evaluateBooleanExpression_ evaluates a boolean expression. The method takes a documentContext as argument.


	ItemCollection workitem = new ItemCollection();
	workitem.replaceItemValue("_budget", 1000);

	boolean result = ruleEngine.evaluateBooleanExpression("(workitem._budget && workitem._budget[0]>100)", workitem);
	Assert.assertTrue(result);

	
	
