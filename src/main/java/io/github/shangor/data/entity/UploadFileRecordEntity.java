package io.github.shangor.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "upload_file_record")
public class UploadFileRecordEntity {
    @Id
    private String id;
    private String fileName;
    private String filePath;
    private boolean ocr;
    private String fileType;
    private String fileChecksum;
    private int fileSize;
    private String processStatus;
    @CreationTimestamp
    private Timestamp createTime;
    @UpdateTimestamp
    private Timestamp updateTime;
}
