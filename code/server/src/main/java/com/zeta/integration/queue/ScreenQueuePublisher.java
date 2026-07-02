package com.zeta.integration.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 通过 Redis Pub/Sub 向 monitord 发送命令。
 * monitord 使用 SUBSCRIBE 监听 channel，因此必须使用 PUBLISH 发送。
 */
@Component
@ConditionalOnProperty(name = "zeta.screen-queue.enabled", havingValue = "true")
public class ScreenQueuePublisher {

    private static final Logger log = LoggerFactory.getLogger(ScreenQueuePublisher.class);

    private final StringRedisTemplate redisTemplate;
    private final ScreenQueueProperties properties;
    private final ObjectMapper objectMapper;

    public ScreenQueuePublisher(
            StringRedisTemplate redisTemplate,
            ScreenQueueProperties properties,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public void publish(ScreenQueueMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            // Redis Pub/Sub: PUBLISH channel message
            redisTemplate.convertAndSend(properties.getOutboundKey(), payload);
            log.info(">>> MQ OUTBOUND [PUBLISH {}]: command={} req_id={} data={}",
                    properties.getOutboundKey(), message.getCommand(), message.getReqId(), message.getData());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("无法序列化队列消息", e);
        }
    }
}
