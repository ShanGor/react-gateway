package io.github.shangor.agent.controller;

import io.github.shangor.agent.state.StateGraph;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@Slf4j
@CrossOrigin(origins = {"*"})
public class StateController {
    @Data
    @Builder
    public static class ActionStatus {
        private String id;
        private String status;
        private String message;
    }

    @GetMapping("/agents/{id}")
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
    @GetMapping("/agents")
    public StateGraph getStateGraph() {
        var nodes = List.of(
                StateGraph.Node.builder()
                        .id("1")
                        .label("start")
                        .type(StateGraph.Node.Type.START)
                        .build(),
                StateGraph.Node.builder()
                        .id("2")
                        .type(StateGraph.Node.Type.ACTION)
                        .label("General Knowledge BA")
                        .build(),
                StateGraph.Node.builder()
                        .id("3")
                        .type(StateGraph.Node.Type.ACTION)
                        .label("Market Research BA")
                        .build(),
                StateGraph.Node.builder()
                        .id("4")
                        .type(StateGraph.Node.Type.ACTION)
                        .label("UK Market BA")
                        .build(),
                StateGraph.Node.builder()
                        .id("5")
                        .type(StateGraph.Node.Type.ACTION)
                        .label("Quality BA")
                        .build(),
                StateGraph.Node.builder()
                        .id("6")
                        .type(StateGraph.Node.Type.END)
                        .label("end")
                        .build()
        );
        var edges = List.of(
                StateGraph.Edge.builder().id("1")
                        .source("1")
                        .target("2")
                        .label("Start")
                        .build(),
                StateGraph.Edge.builder().id("2")
                        .source("2")
                        .target("3")
                        .label("")
                        .build(),
                StateGraph.Edge.builder().id("3")
                        .source("3")
                        .target("4")
                        .label("")
                        .build(),
                StateGraph.Edge.builder().id("4")
                        .source("4")
                        .target("5")
                        .label("")
                        .build(),
                StateGraph.Edge.builder().id("5")
                        .source("5")
                        .target("6")
                        .label("End")
                        .build()
        );
        return StateGraph.builder()
                .nodes(nodes)
                .edges(edges)
                .build();
    }
}
