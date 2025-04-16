package io.github.shangor.data.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import io.github.shangor.statemachine.util.JsonUtil;
import io.micrometer.common.util.StringUtils;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

@Slf4j
@Converter
public class ListStringJsonConverter implements AttributeConverter<List<String>, String> {

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
