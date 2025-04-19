package io.github.shangor.service;

import io.github.shangor.data.entity.LlmAgentEntity;
import io.github.shangor.data.repo.LlmAgentRepository;
import io.github.shangor.exception.UserException;
import io.github.shangor.statemachine.util.ConcurrentUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
@Slf4j
public class LlmAgentService {
    public static LlmAgentService instance;
    private final LlmAgentRepository repo;
    @Getter
    private final ChatModel chatModel;
    public LlmAgentService(LlmAgentRepository repo, ChatModel chatModel) {
        log.info("LlmAgentService initialized");
        this.repo = repo;
        this.chatModel = chatModel;
        instance = this;
    }

    public Optional<LlmAgentEntity> findByAgentName(String agentName) {
        return repo.findById(agentName);
    }

    public Flux<LlmAgentEntity> list() {
        return Flux.create(sink -> ConcurrentUtil.unblockFlux(() -> {
            repo.findAll().forEach(sink::next);
            sink.complete();
        }));
    }

    public Mono<LlmAgentEntity> create(LlmAgentEntity request) {
        return Mono.create(sink ->
            ConcurrentUtil.runAsync(() -> {
                try {
                    sink.success(repo.save(request));
                } catch (Exception e) {
                    sink.error(new UserException(400, "400", e.getMessage()));
                }

            })
        );
    }
}
