package com.zeta.integration.queue;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zeta.screen-queue")
public class ScreenQueueProperties {

    /** 是否启用与屏柜系统的 Redis 队列交互 */
    private boolean enabled = false;

    /** 业务系统消费：屏柜系统 → 业务系统 */
    private String inboundKey = "ct:screen:business:inbound";

    /** 业务系统发送：业务系统 → 屏柜系统 */
    private String outboundKey = "ct:business:screen:outbound";

    /** BRPOP 阻塞超时（秒） */
    private long pollTimeoutSeconds = 5;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getInboundKey() {
        return inboundKey;
    }

    public void setInboundKey(String inboundKey) {
        this.inboundKey = inboundKey;
    }

    public String getOutboundKey() {
        return outboundKey;
    }

    public void setOutboundKey(String outboundKey) {
        this.outboundKey = outboundKey;
    }

    public long getPollTimeoutSeconds() {
        return pollTimeoutSeconds;
    }

    public void setPollTimeoutSeconds(long pollTimeoutSeconds) {
        this.pollTimeoutSeconds = pollTimeoutSeconds;
    }
}
