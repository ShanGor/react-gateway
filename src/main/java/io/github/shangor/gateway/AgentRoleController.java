package io.github.shangor.gateway;

import io.github.shangor.data.entity.LlmAgentEntity;
import io.github.shangor.data.repo.LlmAgentRepository;
import io.github.shangor.exception.UserException;
import io.github.shangor.service.LlmAgentService;
import io.github.shangor.statemachine.util.ConcurrentUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class AgentRoleController {
    private final LlmAgentService llmAgentService;
    private final LlmAgentRepository repo;

    public AgentRoleController(LlmAgentService llmAgentService, LlmAgentRepository repo) {
        this.llmAgentService = llmAgentService;
        this.repo = repo;
    }

    @GetMapping("/api/admin/agents")
    public Flux<LlmAgentEntity> listAgents() {
        return llmAgentService.list();
    }

    @PostMapping("/api/admin/agents")
    public Mono<LlmAgentEntity> createAgent(@RequestBody LlmAgentEntity request) {
        if (llmAgentService.findByAgentName(request.getAgentName()).isPresent()) {
            return Mono.error(new UserException(400, "EAEX", "Agent already exists: " + request.getAgentName()));
        }
        return llmAgentService.create(request);
    }

    @PutMapping("/api/admin/agents")
    public Mono<LlmAgentEntity> updateAgent(@RequestBody LlmAgentEntity request) {
        if (llmAgentService.findByAgentName(request.getAgentName()).isEmpty()) {
            return Mono.error(new UserException(400, "EANF", "Not found agent: " + request.getAgentName()));
        }
        return llmAgentService.create(request);
    }

    @GetMapping("/api/admin/agents/{agentName}")
    public Mono<LlmAgentEntity> getAgent(@PathVariable String agentName) {
        return Mono.create(sink -> ConcurrentUtil.runAsync(() -> {
            try {
                var opt = llmAgentService.findByAgentName(agentName);
                if (opt.isEmpty()) {
                    sink.error(new UserException(404, "EANF", "Not found agent: " + agentName));
                } else {
                    sink.success(opt.get());
                }
            } catch (Exception e) {
                sink.error(e);
            }
        }));
    }

    @DeleteMapping("/api/admin/agents/{agentName}")
    public Mono<Void> deleteAgent(@PathVariable String agentName) {
        return Mono.create(sink -> ConcurrentUtil.runAsync(() -> {
            try {
                repo.deleteById(agentName);
                sink.success();
            } catch (Exception e) {
                log.error("Error delete agent: {} - {}", agentName, e.getMessage());
                sink.error(e);
            }
        }));
    }
}
