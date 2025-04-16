package io.github.shangor.data.repo;

import io.github.shangor.data.entity.LlmAgentEntity;
import org.springframework.data.repository.CrudRepository;

public interface LlmAgentRepository extends CrudRepository<LlmAgentEntity, String> {
}
