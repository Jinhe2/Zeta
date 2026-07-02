package com.zeta.integration.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 向屏柜系统 Redis 队列发送消息。
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
            redisTemplate.opsForList().leftPush(properties.getOutboundKey(), payload);
            log.debug("Published screen queue message command={}", message.getCommand());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("无法序列化队列消息", e);
        }
    }
}
