package io.github.shangor.data.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import io.github.shangor.statemachine.state.StateFlow;
import io.github.shangor.statemachine.util.JsonUtil;
import io.micrometer.common.util.StringUtils;
import jakarta.persistence.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.generator.EventType;

import java.sql.Timestamp;
import java.util.Collections;
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

    @Convert(converter = ToolsJsonConverter.class)
    private List<String> tools;

    @Column(updatable = false, insertable = false)
    @Generated(event = EventType.INSERT)
    private Timestamp creationTime;
    @UpdateTimestamp
    private Timestamp lastUpdateTime;

    @Slf4j
    public static class ToolsJsonConverter implements AttributeConverter<List<String>, String> {

        private static final TypeReference<List<String>> MY_TYPE_REFERENCE = new TypeReference<>() {};
        @Override
        public String convertToDatabaseColumn(List<String> tools) {
            try {
                return JsonUtil.getObjectMapper().writeValueAsString(tools);
            } catch (JsonProcessingException e) {
                log.error("Failed to convert to database column: {}!", e.getMessage());
                throw new RuntimeException(e);
            }
        }

        @Override
        public List<String> convertToEntityAttribute(String s) {
            try {
                if (s == null || StringUtils.isBlank(s)) return Collections.emptyList();

                return JsonUtil.getObjectMapper().readValue(s, MY_TYPE_REFERENCE);
            } catch (JsonProcessingException e) {
                log.error("Failed to convert to entity attribute: {}!", e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }
}
