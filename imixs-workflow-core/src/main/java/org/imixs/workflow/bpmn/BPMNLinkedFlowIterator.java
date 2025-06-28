/****************************************************************************
 * Copyright (c) 2022-2025 Imixs Software Solutions GmbH and others.
 * https://www.imixs.com
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * This Source Code may also be made available under the terms of the
 * GNU General Public License, version 2 or later (GPL-2.0-or-later),
 * which is available at https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-or-later
 ****************************************************************************/

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