package io.github.shangor.data.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.generator.EventType;

import java.sql.Timestamp;
import java.util.List;

/**
 *
 CREATE TABLE llm_agent (
 agent_name varchar(127) NOT NULL,
 alias varchar(128) NULL,
 description text NULL,
 model varchar(127) NULL,
 system_prompt text NULL,
 user_prompt text NULL,
 tools jsonb NULL,
 creation_time timestamp DEFAULT now() NULL,
 last_update_time timestamp DEFAULT now() NULL,
 CONSTRAINT llm_agent_pk PRIMARY KEY (agent_name)
 );
 */
@Data
@Entity
@Table(name = "llm_agent")
public class LlmAgentEntity {
    @Id
    private String agentName;
    private String alias;
    private String description;
    private String model;
    private String systemPrompt;
    private String userPrompt;

    @Convert(converter = ListStringJsonConverter.class)
    private List<String> tools;

    @Column(updatable = false, insertable = false)
    @Generated(event = EventType.INSERT)
    private Timestamp creationTime;
    @UpdateTimestamp
    private Timestamp lastUpdateTime;
}
