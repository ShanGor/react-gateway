package io.github.shangor.data.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.generator.EventType;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

/**
 * Table DDL:
 CREATE TABLE public.sm_flow_hist (
 use_case_id varchar(37) NOT NULL,
 submission_id varchar(37) NOT NULL,
 author varchar(255) NULL,
 authorized_updater text NULL,
 authorized_reader text NULL,
 creation_time timestamp DEFAULT now() NULL,
 last_update_time timestamp DEFAULT now() NULL
 );
 CREATE UNIQUE INDEX sm_flow_hist_id_idx ON public.sm_flow_hist USING btree (use_case_id, submission_id);
 CREATE INDEX sm_flow_hist_use_case_id_idx ON public.sm_flow_hist USING btree (use_case_id);

 */
@Entity
@Table(name = "sm_flow_hist")
@Data
public class FlowHistoryEntity {
    @EmbeddedId
    private Id id;

    private String author;
    @Convert(converter = ListStringJsonConverter.class)
    private List<String> authorizedUpdater;
    @Convert(converter = ListStringJsonConverter.class)
    private List<String> authorizedReader;

    @Column(updatable = false, insertable = false)
    @Generated(event = EventType.INSERT)
    private Timestamp creationTime;
    @UpdateTimestamp
    private Timestamp lastUpdateTime;

    @Data
    @Embeddable
    public static class Id implements Serializable {
        private String useCaseId;
        private String submissionId;
    }
}
