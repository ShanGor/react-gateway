package io.github.shangor.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.shangor.data.dto.ConversationItem;
import io.github.shangor.data.entity.ConversationEntity;
import io.github.shangor.data.repo.ConversationRepository;
import io.github.shangor.service.ConversationService;
import io.github.shangor.util.IntegrationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
public class ConversationController {
    private final ConversationRepository conversationRepo;
    private final ObjectMapper objectMapper;
    private final ConversationService service;

    public ConversationController(ConversationRepository repo,
                                  ObjectMapper objectMapper,
                                  ConversationService service) {
        this.conversationRepo = repo;
        this.objectMapper = objectMapper;
        this.service = service;
    }

    @GetMapping(path="/api/conversations", produces = "application/json")
    public Iterable<ConversationEntity> listConversations() {
        return conversationRepo.findAll();
    }

    @PostMapping(path="/api/conversations", produces = "application/json", consumes = "plain/text")
    public ConversationEntity createConversation(@RequestBody String username) {
        var conversationId = IntegrationUtils.uuidV7().toString();
        var conversation = new ConversationEntity();
        conversation.setId(conversationId);
        conversation.setUsername(username);
        conversation.setTitle("Untitled");
        conversation.setLastSequence(0);
        return conversationRepo.save(conversation);
    }

    @PatchMapping(path="/api/conversations/{id}", produces = "application/json", consumes = "plain/text")
    public ResponseEntity<String> patchConversationName(@RequestBody String title, @PathVariable String id) {
        var opt = conversationRepo.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body("Conversation not found");
        } else {
            var conversation = opt.get();
            conversation.setTitle(title);
            conversationRepo.save(conversation);
            return ResponseEntity.ok().body("Conversation updated");
        }
    }

    @PutMapping("/api/conversations")
    public ResponseEntity<String> upsertConversationItem(@RequestBody String payload) {
        try {
            var item = objectMapper.readValue(payload, ConversationItem.class);
            return service.upsertConversationItem(item);
        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
