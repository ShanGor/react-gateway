package io.github.shangor.api.helper.pojo;

import lombok.Data;
import org.springframework.util.LinkedMultiValueMap;

import java.util.LinkedList;

@Data
public class RequestTree {
    private Long id;
    private String name;
    private String description;

    private String url;
    private String method;
    private String body;
    private String contentType;
    private LinkedMultiValueMap<String, String> headers;

    private LinkedList<RequestTree> children;
}
