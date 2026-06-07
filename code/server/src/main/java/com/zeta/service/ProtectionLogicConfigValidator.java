package com.zeta.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Component
public class ProtectionLogicConfigValidator {

    private final ObjectMapper objectMapper;

    public ProtectionLogicConfigValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    public String validateAndNormalize(String configJson) {
        if (configJson == null || configJson.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "配置内容不能为空");
        }
        Map<String, Object> config;
        try {
            config = objectMapper.readValue(configJson, Map.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JSON 格式无效：" + e.getOriginalMessage());
        }
        if (config == null || config.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "配置必须是非空 JSON 对象");
        }
        requireArray(config, "inputs");
        requireArray(config, "gates");
        requireArray(config, "outputs");
        ensureArray(config, "timers");
        ensureArray(config, "settings");
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "配置序列化失败");
        }
    }

    private void requireArray(Map<String, Object> config, String field) {
        Object value = config.get(field);
        if (!(value instanceof List)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "配置缺少数组字段：" + field);
        }
    }

    private void ensureArray(Map<String, Object> config, String field) {
        Object value = config.get(field);
        if (value == null) {
            config.put(field, new java.util.ArrayList<>());
            return;
        }
        if (!(value instanceof List)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "字段 " + field + " 必须是数组");
        }
    }
}
