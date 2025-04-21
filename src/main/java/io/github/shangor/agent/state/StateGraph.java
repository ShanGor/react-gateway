package io.github.shangor.agent.state;

import io.github.shangor.state.StateUtil;
import io.micrometer.common.util.StringUtils;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import io.github.shangor.statemachine.state.*;

/**
 * - Node
 *   - type [start, end, action (agent), condition]
 *   - id
 *   - label
 *   - description
 *   - status [pending, running, success, failure]
 *   - context
 * - Edge
 *   - id
 *   - source
 *   - target
 *   - label
 *   - animated
 */
@Data
@Builder
@Slf4j
public class StateGraph {

    private List<Node> nodes;
    private List<Edge> edges;

    /**
     * Edge node in `StateFlow` should be very simple, only one hop, no multiple hop. No formula. Allowed
     * @param flow The workflow defined in state machine.
     * @return StateGraph A state graph.
     */
    public static StateGraph fromStateMachineFlow(List<StateFlow> flow) {
        var nodes = new LinkedList<Node>();
        var stateProducers = new HashMap<String, StateFlow>();
        var stateAnticipators = new HashMap<String, StateFlow>();
        for (var node : flow) {
            String fromState = node.getFromState();
            if (StringUtils.isNotBlank(fromState)) {
                stateAnticipators.put(fromState, node);
            }
            String toState = node.getToState();
            if (StringUtils.isNotBlank(toState)) {
                stateProducers.put(toState, node);
            }

            String stateFormula = node.getStateFormula();
            if (StringUtils.isNotBlank(stateFormula)) {
                var input =StateUtil.parseFormulaForInput(stateFormula);
                input.getLeft().forEach(state -> stateAnticipators.put(state, node));
                input.getRight().forEach(possibleState -> stateProducers.put(possibleState, node));
            }

            if (node.getOtherStates() != null && !node.getOtherStates().isEmpty()) {
                for (var state : node.getOtherStates()) {
                    stateProducers.put(state, node);
                }
            }

            switch (node.getNodeType()) {
                case ACTION -> {
                    var actionName = node.getActionName();
                    if ("LLM_AGENT".equals(actionName)) {
                        var agentNode = new AgentNode();
                        agentNode.setId(node.getNodeId());
                        agentNode.setLabel(node.getNodeName());
                        agentNode.setDescription(node.getNodeName());
                        nodes.add(agentNode);
                    }
                }
                case EDGE -> {
                    continue;
                }
                case START -> {
                    nodes.add(StartNode.builder()
                            .id(node.getNodeId())
                            .label(node.getNodeName())
                            .build());
                }
                case END -> {
                    nodes.add(EndNode.builder()
                            .id(node.getNodeId())
                            .label(node.getNodeName())
                            .build());
                }
                case CONDITION -> {
                    nodes.add(ConditionNode.builder()
                            .id(node.getNodeId())
                            .label(node.getNodeName())
                            .build());
                }
            }
        }

        var uniqueEdges = new HashMap<String, Edge>();
        var nodeById = new HashMap<String, StateFlow>();
        stateAnticipators.forEach((state, node) -> {
            var sourceNode = stateProducers.get(state);
            if (sourceNode == null) {
                log.warn("state {} not found from any output", state);
                return;
            }

            var targetNodeId = node.getNodeId();
            var sourceNodeId = sourceNode.getNodeId();
            var edgeId = sourceNodeId + "-" + targetNodeId;
            nodeById.put(sourceNodeId, sourceNode);
            nodeById.put(targetNodeId, node);
            uniqueEdges.put(edgeId, Edge.builder().id(edgeId).source(sourceNodeId).target(targetNodeId).build());
        });
        var edges = new LinkedList<>(uniqueEdges.values());
        purifyEdges(edges, nodeById);

        return StateGraph.builder().nodes(nodes).edges(edges).build();
    }

    /**
     * The statemachine has EDGE node type, to facilitate the transition of state, we need to remove the EDGE node. And link the prior and next node.
     * The EDGE node id should be `${sourceId}-${targetId}`.
     */
    private static void purifyEdges(List<Edge> edges, Map<String, StateFlow> nodesById) {
        var pured = true;
        for (var edge : edges) {
            var targetNode = nodesById.get(edge.getTarget());
            if ("EDGE".equals(targetNode.getNodeType())) {
                pured = false;
                for (var next : edges) {
                    if (next.getSource().equals(edge.getTarget())) {
                        var newEdge = Edge.builder()
                                .id(edge.getSource() + "-" + next.getTarget())
                                .source(edge.getSource())
                                .target(next.getTarget())
                                .build();
                        edges.remove(edge);
                        edges.remove(next);
                        edges.add(newEdge);
                        break;
                    }
                }
                break;
            }
        }

        if (pured) {
            return;
        }

        purifyEdges(edges, nodesById);
    }
}
