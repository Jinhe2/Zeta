package com.zeta.screen.logicdiagram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;

/**
 * 兼容两种格式：
 * - 对象：{"node": "input_1", "inverted": true}
 * - 纯字符串："input_3"（视为 node 值，inverted 默认 false）
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = GateInputDto.Deserializer.class)
public class GateInputDto {

    private String node;
    private Boolean inverted;

    public static class Deserializer extends JsonDeserializer<GateInputDto> {
        @Override
        public GateInputDto deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            GateInputDto dto = new GateInputDto();

            if (p.currentToken().isScalarValue()) {
                // 纯字符串格式: "input_3"
                dto.node = p.getValueAsString();
                dto.inverted = false;
            } else {
                // 对象格式: {"node": "input_1", "inverted": true}
                JsonNode tree = p.readValueAsTree();
                dto.node = tree.has("node") ? tree.get("node").asText() : null;
                dto.inverted = tree.has("inverted") && tree.get("inverted").asBoolean();
            }

            return dto;
        }
    }
}
