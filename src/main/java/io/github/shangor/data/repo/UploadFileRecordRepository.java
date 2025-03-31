package io.github.shangor.data.repo;

import io.github.shangor.data.entity.UploadFileRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UploadFileRecordRepository extends CrudRepository<UploadFileRecordEntity, String> {
    Page<UploadFileRecordEntity> findAllByOrderByUpdateTimeDesc(Pageable pageable);
    Optional<UploadFileRecordEntity> findByFileChecksumAndFileSize(String fileChecksum, int fileSize);
}
