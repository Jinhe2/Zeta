package com.zeta.integration.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 消费屏柜系统推送到业务系统的 Redis 队列消息（骨架实现，具体业务待屏柜侧协议确定后补充）。
 */
@Component
@ConditionalOnProperty(name = "zeta.screen-queue.enabled", havingValue = "true")
public class ScreenQueueListener {

    private static final Logger log = LoggerFactory.getLogger(ScreenQueueListener.class);

    private final StringRedisTemplate redisTemplate;
    private final ScreenQueueProperties properties;
    private final ObjectMapper objectMapper;

    public ScreenQueueListener(
            StringRedisTemplate redisTemplate,
            ScreenQueueProperties properties,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${zeta.screen-queue.poll-interval-ms:2000}")
    public void pollInbound() {
        String payload = redisTemplate.opsForList().rightPop(
                properties.getInboundKey(),
                Duration.ofSeconds(properties.getPollTimeoutSeconds()));
        if (payload == null) {
            return;
        }
        try {
            ScreenQueueMessage message = objectMapper.readValue(payload, ScreenQueueMessage.class);
            handleMessage(message);
        } catch (Exception e) {
            log.warn("Failed to handle screen queue message: {}", payload, e);
        }
    }

    private void handleMessage(ScreenQueueMessage message) {
        log.info("Received screen queue message type={} payload={}", message.getType(), message.getPayload());
        // TODO: 按 type 分发（如设备数值更新、录播状态变更等）
    }
}
