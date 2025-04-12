package io.github.shangor.agent.state;

import io.github.shangor.state.StateUtil;
import io.github.shangor.statemachine.dao.StateMachineControlEntity;
import io.micrometer.common.util.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

    @Data
    @Builder
    public static class StartNode implements Node {
        private String id;
        private String label;
        private String description;
        private Status status;

        @Override
        public Type getType() {
            return Type.START;
        }
    }

    @Data
    @Builder
    public static class EndNode implements Node {
        private String id;
        private String label;
        private String description;
        private Status status;

        @Override
        public Type getType() {
            return Type.END;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActionNode implements Node {
        protected String id;
        protected String label;
        protected String description;
        protected Status status;

        @Override
        public Type getType() {
            return Type.ACTION;
        }


        public Object action(Object ...args) {
            return "actioned";
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConditionNode implements Node {
        protected String id;
        protected String label;
        protected String description;
        protected Status status;
        @Override
        public Type getType() {
            return Type.CONDITION;
        }
    }

    public interface Node {
        String getId();
        String getLabel();
        String getDescription();
        Status getStatus();
        Type getType();

        enum Type {
            START,
            END,
            ACTION,
            CONDITION
        }

        enum Status {
            PENDING,
            RUNNING,
            SUCCESS,
            FAILURE
        }
    }

    @Data
    @Builder
    public static class Edge {
        protected String id;
        protected String source;
        protected String target;
        protected String label;
    }

    /**
     * Edge node in `StateMachineControlEntity.StateFlow` should be very simple, only one hop, no multiple hop. No formula. Allowed
     * @param flow The workflow defined in state machine.
     * @return StateGraph A state graph.
     */
    public static StateGraph fromStateMachineFlow(List<StateMachineControlEntity.StateFlow> flow) {
        var nodes = new LinkedList<Node>();
        var stateProducers = new HashMap<String, StateMachineControlEntity.StateFlow>();
        var stateAnticipators = new HashMap<String, StateMachineControlEntity.StateFlow>();
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

            switch (node.getNodeType()) {
                case "ACTION" -> {
                    nodes.add(StateGraph.ActionNode.builder()
                            .id(node.getNodeId())
                            .label(node.getNodeName())
                            .build());
                }
                case "EDGE" -> {
                    continue;
                }
                case "START" -> {
                    nodes.add(StateGraph.StartNode.builder()
                            .id(node.getNodeId())
                            .label(node.getNodeName())
                            .build());
                }
                case "END" -> {
                    nodes.add(StateGraph.EndNode.builder()
                            .id(node.getNodeId())
                            .label(node.getNodeName())
                            .build());
                }
                case "CONDITION" -> {
                    nodes.add(StateGraph.ConditionNode.builder()
                            .id(node.getNodeId())
                            .label(node.getNodeName())
                            .build());
                }
            }
        }

        var uniqueEdges = new HashMap<String, Edge>();
        var nodeById = new HashMap<String, StateMachineControlEntity.StateFlow>();
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
    private static void purifyEdges(List<Edge> edges, Map<String, StateMachineControlEntity.StateFlow> nodesById) {
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
