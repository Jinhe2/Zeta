package com.zeta.integration.queue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * monitord 命令消息体（JSON）。
 *
 * 请求格式：{ command, req_id, data }
 * 响应格式：{ command, req_id, success, data, error?, error_message? }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScreenQueueMessage {

    private String command;

    @JsonProperty("req_id")
    private String reqId;

    private Map<String, Object> data;

    private Boolean success;

    private String error;

    @JsonProperty("error_message")
    private String errorMessage;

    public ScreenQueueMessage() {
    }

    public ScreenQueueMessage(String command, String reqId, Map<String, Object> data) {
        this.command = command;
        this.reqId = reqId;
        this.data = data;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getReqId() {
        return reqId;
    }

    public void setReqId(String reqId) {
        this.reqId = reqId;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
