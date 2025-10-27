package io.github.shangor.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Entity
@Table(name = "conversation_detail")
@Data
public class ConversationDetailEntity {
    @Id
    private String id;
    private String conversationId;
    @Column(name = "seq")
    private int sequence;

    private String role;
    private String content;

    private String media;

    private String meta;

    private Timestamp createTime;
    @UpdateTimestamp
    private Timestamp updateTime;
}
