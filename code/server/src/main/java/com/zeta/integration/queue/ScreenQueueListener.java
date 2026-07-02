package com.zeta.integration.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeta.integration.monitor.MonitorCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 消费 monitord 推送到业务系统的 Redis 队列消息。
 * 按 command 字段分发到对应的处理服务。
 */
@Component
@ConditionalOnProperty(name = "zeta.screen-queue.enabled", havingValue = "true")
public class ScreenQueueListener {

    private static final Logger log = LoggerFactory.getLogger(ScreenQueueListener.class);

    private final StringRedisTemplate redisTemplate;
    private final ScreenQueueProperties properties;
    private final ObjectMapper objectMapper;
    private final MonitorCommandService monitorCommandService;
    private volatile boolean connectionErrorLoggeded = false;

    public ScreenQueueListener(
            StringRedisTemplate redisTemplate,
            ScreenQueueProperties properties,
            ObjectMapper objectMapper,
            MonitorCommandService monitorCommandService) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.monitorCommandService = monitorCommandService;
    }

    @Scheduled(fixedDelayString = "${zeta.screen-queue.poll-interval-ms:2000}")
    public void pollInbound() {
        try {
            String payload = redisTemplate.opsForList().rightPop(
                    properties.getInboundKey(),
                    Duration.ofSeconds(properties.getPollTimeoutSeconds()));
            if (payload == null) {
                return;
            }
            ScreenQueueMessage message = objectMapper.readValue(payload, ScreenQueueMessage.class);
            handleMessage(message);
        } catch (org.springframework.data.redis.RedisConnectionFailureException e) {
            if (!connectionErrorLoggeded) {
                log.warn("Redis 队列连接失败，监听器暂停（{}）。消息发送不受影响。", e.getRootCause() != null ? e.getRootCause().getMessage() : e.getMessage());
                connectionErrorLoggeded = true;
            }
        } catch (Exception e) {
            log.warn("Failed to handle screen queue message: {}", e.getMessage());
        }
    }

    private void handleMessage(ScreenQueueMessage message) {
        String command = message.getCommand();
        log.info("<<< MQ INBOUND: command={} req_id={} success={} data={}",
                command, message.getReqId(), message.getSuccess(), message.getData());

        if (command != null && (
                command.equals("summon_pressboard_status") ||
                command.equals("summon_terminal_status") ||
                command.equals("compare_baseline_settings") ||
                command.equals("summon_logic_monitor"))) {
            monitorCommandService.handleResponse(message);
        } else {
            log.warn("Unknown monitord command: {}", command);
        }
    }
}
