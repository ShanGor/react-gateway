package io.github.shangor.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Entity
@Table(name = "conversation")
@Data
public class ConversationEntity {
    @Id
    private String id;
    private String username;
    private String title;
    private int lastSequence;

    private Timestamp createTime;
    @UpdateTimestamp
    private Timestamp updateTime;
}
