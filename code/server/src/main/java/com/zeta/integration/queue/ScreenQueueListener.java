package com.zeta.integration.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeta.integration.monitor.MonitorCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

/**
 * 通过 Redis Pub/Sub 订阅 monitord 的响应 channel。
 * monitord 使用 PUBLISH 发送响应，因此必须使用 SUBSCRIBE 接收。
 */
@Component
@ConditionalOnProperty(name = "zeta.screen-queue.enabled", havingValue = "true")
public class ScreenQueueListener {

    private static final Logger log = LoggerFactory.getLogger(ScreenQueueListener.class);

    private final ObjectMapper objectMapper;
    private final MonitorCommandService monitorCommandService;
    private final ScreenQueueProperties properties;

    public ScreenQueueListener(
            ObjectMapper objectMapper,
            MonitorCommandService monitorCommandService,
            ScreenQueueProperties properties) {
        this.objectMapper = objectMapper;
        this.monitorCommandService = monitorCommandService;
        this.properties = properties;
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            org.springframework.data.redis.connection.RedisConnectionFactory connectionFactory) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        MessageListener listener = (Message message, byte[] pattern) -> {
            try {
                String payload = new String(message.getBody());
                log.info("<<< MQ INBOUND [SUBSCRIBE {}]: {}", properties.getInboundKey(),
                        payload.length() > 500 ? payload.substring(0, 500) + "…" : payload);

                ScreenQueueMessage msg = objectMapper.readValue(payload, ScreenQueueMessage.class);
                handleMessage(msg);
            } catch (Exception e) {
                log.warn("Failed to handle Pub/Sub message: {}", e.getMessage());
            }
        };

        container.addMessageListener(listener, new ChannelTopic(properties.getInboundKey()));
        log.info("Subscribed to Redis Pub/Sub channel: {}", properties.getInboundKey());

        return container;
    }

    private void handleMessage(ScreenQueueMessage message) {
        String command = message.getCommand();
        log.info("<<< MQ INBOUND: command={} req_id={} success={} data={}",
                command, message.getReqId(), message.getSuccess(), message.getData());

        if (command != null && (
                command.equals("summon_pressboard_status") ||
                command.equals("summon_terminal_status") ||
                command.equals("summon_ied_comm_status") ||
                command.equals("compare_baseline_settings") ||
                command.equals("summon_logic_monitor"))) {
            monitorCommandService.handleResponse(message);
        } else {
            log.warn("Unknown monitord command: {}", command);
        }
    }
}
