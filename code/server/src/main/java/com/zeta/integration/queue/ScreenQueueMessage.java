package com.zeta.integration.queue;

import java.util.Map;

/**
 * 屏柜系统 ↔ 业务系统 Redis 队列消息体（JSON）。
 */
public class ScreenQueueMessage {

    private String type;
    private Map<String, Object> payload;
    private Long timestamp;

    public ScreenQueueMessage() {
    }

    public ScreenQueueMessage(String type, Map<String, Object> payload) {
        this.type = type;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
