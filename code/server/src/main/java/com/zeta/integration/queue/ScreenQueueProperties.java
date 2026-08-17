package com.zeta.integration.queue;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zeta.screen-queue")
public class ScreenQueueProperties {

    /** 是否启用与屏柜系统的 Redis 队列交互 */
    private boolean enabled = false;

    /** 业务系统消费：monitord → 业务系统 */
    private String inboundKey = "monitor_command_response";

    /** 业务系统发送：业务系统 → monitord */
    private String outboundKey = "monitor_command_request";

    /** Zeta 直接发送 MMS 定值召唤命令的频道 */
    private String mmsOutboundKey = "mms_command_request";

    /** MMS 定值召唤响应频道 */
    private String mmsInboundKey = "mms_command_response";

    /** MmsClientDyn 定值响应文件所在的共享目录 */
    private String mmsTempDir = "./temp";

    /** MMS 定值召唤超时（秒） */
    private long mmsTimeoutSeconds = 30;

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

    public String getMmsOutboundKey() { return mmsOutboundKey; }
    public void setMmsOutboundKey(String value) { this.mmsOutboundKey = value; }
    public String getMmsInboundKey() { return mmsInboundKey; }
    public void setMmsInboundKey(String value) { this.mmsInboundKey = value; }
    public String getMmsTempDir() { return mmsTempDir; }
    public void setMmsTempDir(String value) { this.mmsTempDir = value; }
    public long getMmsTimeoutSeconds() { return mmsTimeoutSeconds; }
    public void setMmsTimeoutSeconds(long value) { this.mmsTimeoutSeconds = value; }

    public long getPollTimeoutSeconds() {
        return pollTimeoutSeconds;
    }

    public void setPollTimeoutSeconds(long pollTimeoutSeconds) {
        this.pollTimeoutSeconds = pollTimeoutSeconds;
    }
}
