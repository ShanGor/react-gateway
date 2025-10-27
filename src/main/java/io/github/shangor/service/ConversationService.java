package io.github.shangor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.shangor.data.dto.ConversationItem;
import io.github.shangor.data.entity.ConversationDetailEntity;
import io.github.shangor.data.repo.ConversationDetailRepository;
import io.github.shangor.data.repo.ConversationRepository;
import io.github.shangor.util.IntegrationUtils;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ConversationService {
    private final ConversationRepository conversationRepo;
    private final ConversationDetailRepository conversationDetailRepo;
    private final ObjectMapper objectMapper;

    public ConversationService(ConversationRepository conversationRepo, ConversationDetailRepository conversationDetailRepo, ObjectMapper objectMapper) {
        this.conversationRepo = conversationRepo;
        this.conversationDetailRepo = conversationDetailRepo;
        this.objectMapper = objectMapper;
    }

    public ResponseEntity<String> upsertConversationItem(ConversationItem item) {
        try {
            var conversationOpt = conversationRepo.findById(item.getConversationId());
            if (conversationOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Conversation not found");
            }
            var conversation = conversationOpt.get();
            ConversationDetailEntity chat;
            if (StringUtils.isNotBlank(item.getId())) {
                var opt = conversationDetailRepo.findById(item.getId());
                if (opt.isPresent()) {
                    chat = opt.get();
                } else {
                    return ResponseEntity.badRequest().body("Chat id not found in the conversation!");
                }
            } else {
                conversation.setLastSequence(conversation.getLastSequence() + 1);

                chat = new ConversationDetailEntity();
                chat.setConversationId(item.getConversationId());
                chat.setSequence(conversation.getLastSequence());

                conversationRepo.save(conversation);

                chat.setId(IntegrationUtils.uuidV7().toString());
                chat.setRole(item.getRole());
            }

            chat.setContent(item.getContent());
            var meta = ConversationItem.Meta.builder()
                    .model(item.getModel())
                    .tokens(item.getTokens())
                    .referenceDocuments(item.getReferenceDocuments())
                    .build();
            chat.setMeta(objectMapper.writeValueAsString(meta));
            if (item.getMediaDataUrls() != null) {
                chat.setMedia(objectMapper.writeValueAsString(item.getMediaDataUrls()));
            }

            conversationDetailRepo.save(chat);
            return ResponseEntity.ok(chat.getId());
        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
