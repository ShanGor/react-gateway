package io.github.shangor.agent.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.shangor.agent.state.StateGraph;
import io.github.shangor.data.dto.CreateHistoryRequest;
import io.github.shangor.data.dto.LlmContext;
import io.github.shangor.data.entity.FlowHistoryEntity;
import io.github.shangor.data.repo.FlowHistoryRepository;
import io.github.shangor.statemachine.dao.StatemachineFlowStateRepository;
import io.github.shangor.statemachine.task.MainFlowTask;
import io.github.shangor.statemachine.util.JsonUtil;
import io.micrometer.common.util.StringUtils;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;


@RestController
@Slf4j
@RequiredArgsConstructor
public class StateController {

    private final StatemachineFlowStateRepository flowStateRepo;
    private final FlowHistoryRepository flowHistRepo;

    @Data
    @Builder
    public static class ActionStatus {
        private String id;
        private String status;
        private LlmContext context;
    }

    @GetMapping("/api/agents/status/{id}")
    public Flux<ActionStatus> getStatus(@PathVariable String id) {
        return Flux.create(sink -> Thread.ofVirtual().start(() -> {
            flowStateRepo.findByTransactionId(id).forEach(o -> {
                LlmContext context = null;
                if (StringUtils.isNotBlank(o.getContext())) {
                    try {
                        context = LlmContext.from(o.getContext());
                    } catch (Exception e) {
                        log.error("Failed to parse context", e);
                    }
                }
                var status = ActionStatus.builder().id(o.getId().getNodeId()).status(o.getState()).context(context).build();
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

    @GetMapping("/api/flow-histories/{agentName}")
    public ResponseEntity<?> listHistory(@PathVariable String agentName) {
        var user = "default";
        var opt = MainFlowTask.useCaseInitials.get(agentName);
        if (opt == null) {
            return ResponseEntity.badRequest().body("Invalid agent name");
        }
        var list = flowHistRepo.findAllByIdUseCaseId(opt.getUseCaseId());
        if (!list.isEmpty()) {
            return ResponseEntity.ok(list.stream().filter(o -> user.equals(o.getAuthor())).toList());
        } else {
            return ResponseEntity.ok(List.of());
        }
    }

    @PostMapping("/api/flow-histories")
    public ResponseEntity<?> createHistory(@RequestBody String payload) {
        var user = "default";
        try {
            var o = JsonUtil.getObjectMapper().readValue(payload, CreateHistoryRequest.class);
            var entity = new FlowHistoryEntity();
            var useCase = MainFlowTask.useCaseInitials.get(o.getAgentName());
            if (useCase == null) {
                return ResponseEntity.badRequest().body("Invalid agent name");
            }
            var id = new FlowHistoryEntity.Id();
            id.setUseCaseId(useCase.getUseCaseId());
            id.setSubmissionId(o.getSubmissionId());
            entity.setId(id);
            entity.setAuthor(user);
            var s = flowHistRepo.save(entity);
            return ResponseEntity.ok(s);
        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().body("Invalid payload");
        }
    }

}
