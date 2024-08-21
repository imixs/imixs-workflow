package org.imixs.workflow.bpmn;

import java.util.Set;
import java.util.function.Predicate;

import org.openbpmn.bpmn.elements.SequenceFlow;
import org.openbpmn.bpmn.elements.core.BPMNElementNode;
import org.openbpmn.bpmn.exceptions.BPMNValidationException;
import org.openbpmn.bpmn.navigation.BPMNFlowIterator;
import org.openbpmn.bpmn.navigation.BPMNLinkNavigator;

/**
 * This {@code BPMNLinkedFlowIterator} is a custom implementation of the
 * {@link BPMNFlowIterator}. The class overwrite the method
 * {@code getTargetNode} and resolves Link Events.
 * 
 */
public class BPMNLinkedFlowIterator<T> extends BPMNFlowIterator<BPMNElementNode> {

    public BPMNLinkedFlowIterator(BPMNElementNode bpmnElementNode, Predicate<BPMNElementNode> filter) {
        super(bpmnElementNode, filter);
    }

    public BPMNLinkedFlowIterator(BPMNElementNode bpmnElementNode, Predicate<BPMNElementNode> filter,
            Predicate<String> conditionEvaluator)
            throws BPMNValidationException {
        super(bpmnElementNode, filter, conditionEvaluator);
    }

    /**
     * This method tests if the target is a {@code bpmn:intermediateThrowEvent} with
     * a {@code bpmn2:linkEventDefinition} to navigates automatically to the
     * corresponding {@code bpmn:intermediateCatchEvent}
     * 
     * This is a special implementation for Imixs Workflow.
     */
    @Override
    public BPMNElementNode getTargetNode(SequenceFlow flow) {

        BPMNElementNode nextElement = super.getTargetNode(flow);
        // Test if we have a LinkCatchEvent?
        if (BPMNUtil.isLinkCatchEventElement(nextElement)) {
            // find the target of the link by its name
            BPMNLinkNavigator linkNavigator = new BPMNLinkNavigator();
            BPMNElementNode linkTargetElement = linkNavigator.findNext(nextElement);
            Set<SequenceFlow> outFlows = linkTargetElement.getOutgoingSequenceFlows();
            if (outFlows != null && outFlows.size() > 0) {
                // switch to link Target Element....
                nextElement = outFlows.iterator().next().getTargetElement();
            }
        }

        // return the result
        return nextElement;
    }

}