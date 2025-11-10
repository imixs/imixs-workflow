package org.imixs.workflow.engine;

import org.imixs.workflow.ItemCollection;

/**
 * CDI Event for conditional expression in BPMN process flows.
 * <p>
 * This event enables extensibility by allowing registered observers to
 * transform or interpret conditions in a BPMN SequenceFlow before they are
 * evaluated by the RuleEngine. This design pattern decouples the core BPMN
 * engine from specific condition implementations.
 * <p>
 * In this way observers can implement domain-specific condition logic or track
 * condition evaluations
 * 
 * 
 * @author [Your Name]
 * @version 1.0
 */
public class ConditionalExpressionEvent {

    /**
     * The condition expression from the BPMN model. Can be JavaScript code or any
     * domain-specific condition for a specialized observer. An observer may
     * transform the condition.
     * <p>
     * The transformed condition should contain a valid JavaScript that evaluates to
     * a boolean.
     * <p>
     * Use @Priority to control order of the evaluation flow.
     */
    private String condition;

    /**
     * The workflow item being processed.
     */
    private ItemCollection workitem;

    /**
     * Creates a new BPMNConditionEvent.
     * 
     * @param condition the condition expression from the BPMN SequenceFlow
     * @param workitem  the workflow item providing context and data
     */
    public ConditionalExpressionEvent(String condition, ItemCollection workitem) {
        this.condition = condition;
        this.workitem = workitem;
    }

    /**
     * Gets the original condition expression.
     * 
     * @return the condition as defined in the BPMN model
     */
    public String getCondition() {
        return condition;
    }

    /**
     * Sets the transformed condition result. Called by observers to provide a new
     * expression that can be evaluated by the RuleEngine. The final condition
     * evaluated by the ModelManager should contain valid VM Script that evaluates
     * to a boolean.
     * 
     * @param condition
     */
    public void setCondition(String condition) {
        this.condition = condition;
    }

    /**
     * Gets the workflow item being processed. Observers use this to access data
     * needed for condition transformation.
     * 
     * @return the ItemCollection containing workflow data
     */
    public ItemCollection getWorkitem() {
        return workitem;
    }

}