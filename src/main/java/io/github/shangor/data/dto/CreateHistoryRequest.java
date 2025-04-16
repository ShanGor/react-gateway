package io.github.shangor.data.dto;

import lombok.Data;

@Data
public class CreateHistoryRequest {
    private String agentName;
    private String submissionId;
}
