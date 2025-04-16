package io.github.shangor.data.dto;

import io.github.shangor.service.LlmAgentService;
import io.github.shangor.statemachine.state.StateFlow;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Data
@Slf4j
public class AgentConfig {
    public static final String TYPE_DEFAULT = "typical";
    public static final String TYPE_CUSTOM = "custom";
    private String type;
    private String agentName;

    private String model;
    /**
     * Optional
     */
    private String systemPrompt;

    /**
     * supports {{inputText}} as placeholder
     */
    private String userPrompt;
    private List<String> tools;

    public static Optional<AgentConfig> fromConfig(StateFlow node) {
        var config = node.getConfig();
        if (config == null || config.isEmpty()) {
            log.error("Invalid LlM agent config for node: {}", node);
            return Optional.empty();
        }
        if (!config.containsKey("type") || !config.containsKey("agentName")) {
            log.error("Invalid LLM agent config: {}", config);
            return Optional.empty();
        }
        var type = config.get("type").toString();
        var agentName = config.get("agentName").toString();
        var opt = LlmAgentService.instance.findByAgentName(agentName);
        var o = new AgentConfig();
        o.type = type;
        o.agentName = agentName;
        if (opt.isEmpty()) {
            if (TYPE_DEFAULT.equals(type)) {
                log.error("Not found typical agent name: {}", agentName);
                return Optional.empty();
            } else {
                log.warn("Not found typical agent name: {}", agentName);
            }
        } else {
            var agent = opt.get();
            o.model = agent.getModel();
            o.systemPrompt = agent.getSystemPrompt();
            o.userPrompt = agent.getUserPrompt();
            o.tools = agent.getTools();
        }

        if (TYPE_CUSTOM.equals(type)) {
            if (config.containsKey("model"))
                o.model = config.get("model").toString();
            if (config.containsKey("systemPrompt"))
                o.systemPrompt = config.get("systemPrompt").toString();
            if (config.containsKey("userPrompt"))
                o.userPrompt = config.get("userPrompt").toString();
            if (config.containsKey("tools"))
                o.tools = (List<String>) config.get("tools");
        }

        return Optional.of(o);
    }
}
