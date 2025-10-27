package io.github.shangor.data.repo;

import io.github.shangor.data.entity.ConversationEntity;
import org.springframework.data.repository.CrudRepository;

public interface ConversationRepository extends CrudRepository<ConversationEntity, String> {
}
