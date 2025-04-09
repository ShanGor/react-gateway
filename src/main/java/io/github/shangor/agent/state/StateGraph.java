package io.github.shangor.agent.state;

import lombok.Builder;
import lombok.Data;

import java.util.List;

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
public class StateGraph {

    private List<Node> nodes;
    private List<Edge> edges;

    @Data
    @Builder
    public static class Node {
        private String id;
        private Type type;
        private String label;
        private String description;
        private Status status;

        public enum  Type {
            START,
            END,
            ACTION,
            CONDITION
        }

        public enum Status {
            PENDING,
            RUNNING,
            SUCCESS,
            FAILURE
        }
    }

    @Data
    @Builder
    public static class Edge {
        private String id;
        private String source;
        private String target;
        private String label;
        private boolean animated;
    }
}
