package io.github.shangor.config;

import io.github.shangor.agent.state.AgentNode;
import io.github.shangor.statemachine.task.MainFlowTask;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StatemachineConfig {
    public StatemachineConfig() {
        MainFlowTask.registerActionHandler(AgentNode.ACTION_NAME, new AgentNode());
    }
}
