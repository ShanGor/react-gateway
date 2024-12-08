package io.github.shangor.api.helper.db;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Entity
@Table(name = "requests")
@Data
public class RequestEntity {
    @Id
    private Long id;
    private String content;

    @CreationTimestamp
    private Timestamp creationTime;
    @UpdateTimestamp
    private Timestamp lastUpdateTime;
}
