package io.github.shangor.config;

import io.github.shangor.agent.state.AgentNode;
import io.github.shangor.statemachine.config.StatemachineAutoConfig;
import io.github.shangor.statemachine.state.ActionHandlers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class StatemachineConfig {
    @Bean
    public ActionHandlers defaultActionHandlers() {
        var actionHandlers = new StatemachineAutoConfig().defaultActionHandlers();
        actionHandlers.registerActionHandler(AgentNode.ACTION_NAME, new AgentNode());
        return actionHandlers;
    }
}
