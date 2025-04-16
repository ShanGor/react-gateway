package io.github.shangor.data.repo;

import io.github.shangor.data.entity.FlowHistoryEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface FlowHistoryRepository extends CrudRepository<FlowHistoryEntity, FlowHistoryEntity.Id> {
    List<FlowHistoryEntity> findAllByIdUseCaseId(String useCaseId);
}
