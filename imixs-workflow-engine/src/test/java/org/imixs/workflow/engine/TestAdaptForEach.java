package org.imixs.workflow.engine;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

/**
 * Test the WorkflowService method 'adaptText'
 * 
 * @author rsoika
 * 
 */
public class TestAdaptForEach {
  public ItemCollection documentContext;
  public ItemCollection documentActivity;

  protected WorkflowMockEnvironment workflowMockEnvironment;

  @Before
  public void setUp() throws PluginException, ModelException {
    workflowMockEnvironment = new WorkflowMockEnvironment();
    workflowMockEnvironment.setup();
  }

  /**
   * Test simple value list :
   * 
   * <pre>
   * {@code
      <for-each item="_partid">
          Order-No: <itemvalue>_orderid</itemvalue> - Part ID: <itemvalue>_partid</itemvalue><br />
      </for-each>  
     }
   * </pre>
   * 
   * @throws PluginException
   * 
   */
  @Test

  public void testSimpleValueList() throws PluginException {

    String testString =
        "<for-each item=\"_partid\">Order-No: <itemvalue>_orderid</itemvalue> - Part ID: <itemvalue>_partid</itemvalue><br /></for-each>";
    String expectedStringLast =
        "Order-No: 111222 - Part ID: A123<br />Order-No: 111222 - Part ID: B456<br />";

    // prepare data
    documentContext = new ItemCollection();
    documentContext.setItemValue("_orderid", "111222");
    documentContext.appendItemValue("_partid", "A123");
    documentContext.appendItemValue("_partid", "B456");

    String resultString =
        workflowMockEnvironment.getWorkflowService().adaptText(testString, documentContext);

    Assert.assertEquals(expectedStringLast, resultString);

  }

  /**
   * Test the child item value tag:
   * 
   * <pre>
   * {@code
  <for-each childitem="_childs">
     <itemvalue>_orderid</itemvalue>: <itemvalue>_amount</itemvalue>
  </for-each>  
     }
   * </pre>
   * 
   * @throws PluginException
   * 
   */
  @Test
  public void testEmbeddedChildItemValue() throws PluginException {

    String testString =
        "<for-each item=\"_childs\">Order ID: <itemvalue>_orderid</itemvalue>: <itemvalue>_amount</itemvalue><br /></for-each>";
    String expectedStringLast = "Order ID: A123: 50.55<br />Order ID: B456: 1500000.0<br />";

    // prepare data
    documentContext = new ItemCollection();
    // create 1st child
    ItemCollection child = new ItemCollection();
    child.setItemValue("_orderid", "A123");
    child.setItemValue("_amount", 50.55);
    documentContext.appendItemValue("_childs", child.getAllItems());
    // create 2nd child
    child = new ItemCollection();
    child.setItemValue("_orderid", "B456");
    child.setItemValue("_amount", 1500000.00);
    documentContext.appendItemValue("_childs", child.getAllItems());
    // create a fake value which should be ignored
    documentContext.replaceItemValue("_orderid", "not used");

    String resultString =
        workflowMockEnvironment.getWorkflowService().adaptText(testString, documentContext);

    Assert.assertEquals(expectedStringLast, resultString);

  }

}
