package io.github.shangor.agent.controller;

import io.github.shangor.agent.state.StateGraph;
import io.github.shangor.statemachine.task.MainFlowTask;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;


@RestController
@Slf4j
public class StateController {
    @Data
    @Builder
    public static class ActionStatus {
        private String id;
        private String status;
        private String message;
    }

    @GetMapping("/api/agents/status/{id}")
    public Flux<ServerSentEvent<ActionStatus>> getStatus(@PathVariable String id) {
        return Flux.create(sink -> Thread.ofVirtual().start(() -> {
            try {
                sink.next(ServerSentEvent.builder(ActionStatus.builder().id("1").status("success").message("Agent started").build()).id(id).event("flow-state").build());
                sink.next(ServerSentEvent.builder(ActionStatus.builder().id("2").status("running").message("").build()).id(id).event("flow-state").build());
                Thread.sleep(1000);
                sink.next(ServerSentEvent.builder(ActionStatus.builder().id("2").status("success").build()).id(id).event("flow-state").build());
                sink.next(ServerSentEvent.builder(ActionStatus.builder().id("3").status("running").build()).id(id).event("flow-state").build());
                Thread.sleep(1000);
                sink.next(ServerSentEvent.builder(ActionStatus.builder().id("3").status("success").build()).id(id).event("flow-state").build());
                sink.next(ServerSentEvent.builder(ActionStatus.builder().id("4").status("running").build()).id(id).event("flow-state").build());
                Thread.sleep(1000);
                sink.next(ServerSentEvent.builder(ActionStatus.builder().id("4").status("success").build()).id(id).event("flow-state").build());
                sink.next(ServerSentEvent.builder(ActionStatus.builder().id("5").status("running").build()).id(id).event("flow-state").build());
                Thread.sleep(1000);
                sink.next(ServerSentEvent.builder(ActionStatus.builder().id("5").status("success").build()).id(id).event("flow-state").build());
                sink.next(ServerSentEvent.builder(ActionStatus.builder().id("6").status("success").build()).id(id).event("flow-state").build());
                sink.complete();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
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
