# Imixs-Workflow - Simulation and Testing

The Imixs-Workflow project provides services and test environments to simulate and test the processing life cycle of a specific model or process instance. You can use these services to implement JUnit test classes to test without the need of a full deployment.

For a Maven project just add the following dependencies into your projects `pom.xml` file:

```xml
....
  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
    <maven.compiler.source>11</maven.compiler.source>
    <maven.compiler.target>11</maven.compiler.target>
    <jakarta.version>10.0.0</jakarta.version>
    <org.imixs.workflow.version>6.1.0</org.imixs.workflow.version>
    <microprofile.version>6.0</microprofile.version>
    <microprofile-metrics.version>4.0</microprofile-metrics.version>
    <!-- test dependencies -->
    <junit.jupiter.version>5.9.2</junit.jupiter.version>
    <mockito.version>5.8.0</mockito.version>
    <org.imixs.mock.version>6.1.0</org.imixs.mock.version>
  </properties>
  ....
  <build>
    <plugins>
            ..........
      <!-- use JDK settings for compiling -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.8.1</version>
        <configuration>
          <source>11</source>
          <target>11</target>
        </configuration>
      </plugin>
      <!-- Testing JUnit 5 -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>2.22.2</version>
      </plugin>
    </plugins>
    ...........
    <testResources>
      <testResource>
        <directory>${basedir}/src/test/resources</directory>
      </testResource>
    </testResources>
    <dependencies>
    <!-- Imixs Workflow -->
      <dependency>
          <groupId>org.imixs.workflow</groupId>
          <artifactId>imixs-workflow-core</artifactId>
          <version>${org.imixs.workflow.version}</version>
      </dependency>
      <!-- Jakarta EE -->
      <dependency>
      <groupId>jakarta.platform</groupId>
      <artifactId>jakarta.jakartaee-api</artifactId>
      <version>${jakarta.version}</version>
      <scope>provided</scope>
      </dependency>
      <!-- JUnit 5 Dependencies -->
      <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter-api</artifactId>
      <version>${junit.jupiter.version}</version>
      <scope>test</scope>
      </dependency>
      <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter-engine</artifactId>
      <version>${junit.jupiter.version}</version>
      <scope>test</scope>
      </dependency>
      <dependency>
      <groupId>org.mockito</groupId>
      <artifactId>mockito-core</artifactId>
      <version>${mockito.version}</version>
      <scope>test</scope>
      </dependency>
      <dependency>
      <groupId>org.mockito</groupId>
      <artifactId>mockito-junit-jupiter</artifactId>
      <version>${mockito.version}</version>
      <scope>test</scope>
      </dependency>
      <dependency>
      <groupId>org.eclipse.parsson</groupId>
      <artifactId>jakarta.json</artifactId>
      <version>1.1.1</version>
      <scope>test</scope>
      </dependency>
      <dependency>
      <groupId>org.glassfish.jaxb</groupId>
      <artifactId>jaxb-runtime</artifactId>
      <version>3.0.0</version>
      <scope>test</scope>
      </dependency>
      <dependency>
      <groupId>jakarta.xml.bind</groupId>
      <artifactId>jakarta.xml.bind-api</artifactId>
      <version>3.0.0</version>
      <scope>test</scope>
      </dependency>
      <dependency>
      <groupId>org.imixs.workflow</groupId>
      <artifactId>imixs-mock</artifactId>
      <version>${org.imixs.mock.version}</version>
      <scope>test</scope>
      </dependency>
  </dependencies>
  ......

```

## Simulate a Processing Live Cycle

The `WorkflowService` provides a method called `evalNextTask`. This method evaluates the next task for a process instance (workitem) based on the current model definition. A Workitem must at least provide the properties `$TASKID` and `$EVENTID`. The method call can be helpful in many cases when a business logic just need to compute the next logical BPMN Task Element which will be assigned with a given process instance.

```java
try {
  // simulate the processing life cycle of a given workitem
  ItemCollection nextTaskEnity = workflowService.evalNextTask(workitem);
} catch (ModelException e) {
  throw new PluginException(DocumentComposerPlugin.class.getSimpleName(), e.getErrorCode(), e.getMessage());
}
```

**Note:** During the evaluation life-cycle more than one events can be evaluated. This depends on the model definition which can define follow-up-events, split-events and conditional events. The `evalNextTask` method did not persist the process instance or execute any plugin or adapter classes.

## Testing with the WorkflowMockEnvironment

The Test class `WorkflowMockEnvironment` mocks a full workflow environment including a in-memory-database. The `WorkflowMockEnvironment` can be used for more complex integration tests using JUnit 5 or higher.

```java
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.*;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestTemplate {

  protected WorkflowMockEnvironment workflowEnvironment;

  /**
   * Setup the Mock environment
   */
  @BeforeEach
  public void setUp() throws PluginException, ModelException {
    workflowEnvironment = new WorkflowMockEnvironment();
    workflowEnvironment.setUp();
    workflowEnvironment.loadBPMNModel("/bpmn/TestWorkflowService.bpmn");
  }

  /**
   * This test simulates a workflowService process call.
   */
  @Test
  public void testProcessSimple() {
    try {
      // load a test workitem
      ItemCollection workitem = new ItemCollection().model("1.0.0").task(100).event(10);
      workitem = workflowEnvironment.workflowService.processWorkItem(workitem);
      // expected new task is 200
      assertEquals(200, workitem.getTaskID());
    } catch (AccessDeniedException | ProcessingErrorException | PluginException | ModelException e) {
      Assert.fail(e.getMessage());
    }
  }

  /**
   * Test a complex workflow process with conditional events
   */
  @Test
  public void testConditionalEvent()
      throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {

    // load test workitem
    ItemCollection workitem =  new ItemCollection().model("1.0.0").task(200).event(30);
    // add some business data...
    workitem.replaceItemValue("name","Imixs Software Solutions");
    workitem.replaceItemValue("budget", 1000.00);
    // test _budget<=1000 => 1200
    workitem = workflowEnvironment.workflowService.processWorkItem(workitem);
    Assert.assertEquals(1200, workitem.getTaskID());
    // test _budget>1000 => 1100
    workitem = new ItemCollection().model("1.0.0").task(200).event(30);
    workitem.replaceItemValue("budget", 9999);
    workitem = workflowEnvironment.workflowService.processWorkItem(workitem);
    Assert.assertEquals(1100, workitem.getTaskID());
  }
}

```

### How to setup a test case

To setup a test case the Imixs `WorkflowMockEnvironment` provides a setup method to initialize the environment and the method `loadBPMNModel` to load a test model. To setup the environment it is recommended to call the setup() method in a `org.junit.jupiter.api.BeforeEach` annotated init method:

```java
@BeforeEach
public void setUp() throws PluginException, ModelException {
  workflowEnvironment = new WorkflowMockEnvironment();
  workflowEnvironment.setUp();
  workflowEnvironment.loadBPMNModel("/bpmn/TestWorkflowService.bpmn");
}
```

### How to test business cases

The main goal of the `WorkflowMockEnvironment` is to test business logic of a specific workflow model. A workflow instance can be created form a empty ItemCollection and tested with any kind of data.

```java
// Load a test model
workflowEnvironment.loadBPMNModel("/bpmn/myModel.bpmn");
// Create a workflow instance with some business data
ItemCollection workitem = new ItemCollection();
workitem.replaceItemValue("_budget", 99);
// process the workflow instance....
workitem.model("1.0.0").task(1000).event(10);
workitem = workflowEnvironment.workflowService.processWorkItem(workitem);
// evaluate the results....
Assert.assertEquals(1200, workitem.getTaskID());
String uniqueID=workitem.getUniqueID();
workitem=workflowEnvironment.getDocumentService().load(uniqueID);
Assert.notNull(workitem);
```

### Mock Imixs Adapter Classes

The `WorkflowMockEnvironment` also allows you to test custom Imixs Workflow `GenericAdapter` or `SignalAdapter` classes. You just need to call the method `registerAdapter(...)` method before you call `setup()`. See the following example:

```java

public class MyAdapterTest {

  @InjectMocks
  protected MyImixsAdapter myAdapter;

  protected WorkflowMockEnvironment workflowEnvironment;

  @BeforeEach
  public void setUp() throws PluginException, ModelException {
    // Ensures that @Mock and @InjectMocks annotations are processed
    MockitoAnnotations.openMocks(this);
    workflowEnvironment = new WorkflowMockEnvironment();
    // register AccessAdapter Mock
    workflowEnvironment.registerAdapter(accessAdapter);
    // Setup Environment
    workflowEnvironment.setUp();
  }
  .....
}

```
