package io.github.shangor.api.helper.db;

import org.springframework.data.repository.CrudRepository;

public interface RequestRepository extends CrudRepository<RequestEntity, Long> {
}
