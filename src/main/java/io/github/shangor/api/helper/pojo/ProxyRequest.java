package io.github.shangor.api.helper.pojo;

import lombok.Data;

import java.util.Map;

@Data
public class ProxyRequest {
    private String url;
    private String method;
    private String body;
    private Map<String, String> headers;
}
