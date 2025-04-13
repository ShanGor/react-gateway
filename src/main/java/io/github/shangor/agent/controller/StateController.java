package io.github.shangor.agent.controller;

import io.github.shangor.agent.state.StateGraph;
import io.github.shangor.statemachine.dao.StatemachineFlowStateRepository;
import io.github.shangor.statemachine.task.MainFlowTask;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;


@RestController
@Slf4j
@RequiredArgsConstructor
public class StateController {

    private final StatemachineFlowStateRepository flowStateRepo;

    @Data
    @Builder
    public static class ActionStatus {
        private String id;
        private String status;
        private String message;
    }

    @GetMapping("/api/agents/status/{id}")
    public Flux<ActionStatus> getStatus(@PathVariable String id) {
        return Flux.create(sink -> Thread.ofVirtual().start(() -> {
            flowStateRepo.findByTransactionId(id).forEach(o -> {
                var status = ActionStatus.builder().id(o.getId().getNodeId()).status(o.getState()).build();
                sink.next(status);
            });
            sink.complete();
        }));
    }
    @GetMapping("/api/agents/{useCaseName}")
    public ResponseEntity<?> getStateGraph(@PathVariable String useCaseName) {
        var useCase = MainFlowTask.useCaseInitials.get(useCaseName);
        if (useCase == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(StateGraph.fromStateMachineFlow(useCase.getDetail()));
    }
}
