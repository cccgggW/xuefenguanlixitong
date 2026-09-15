package com.xuefen.agent.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 大模型（LLM）接入配置
 * 读取 application.yml 中的 ai.llm.* 配置，支持环境变量覆盖：
 *   AI_LLM_ENABLED / AI_LLM_BASE_URL / AI_LLM_API_KEY / AI_LLM_MODEL / AI_LLM_TIMEOUT
 */
@Component
@ConfigurationProperties(prefix = "ai.llm")
public class LlmProperties {

    /** 是否启用 LLM 智能体（关闭时回退到规则引擎） */
    private boolean enabled = true;

    /** API 基础地址（OpenAI 兼容格式） */
    private String baseUrl = "https://api.deepseek.com";

    /** API Key */
    private String apiKey = "";

    /** 模型名称 */
    private String model = "deepseek-chat";

    /** 请求超时时间（秒） */
    private int timeoutSeconds = 60;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    /** 是否具备调用 LLM 的条件 */
    public boolean isReady() {
        return enabled && apiKey != null && !apiKey.trim().isEmpty()
                && baseUrl != null && !baseUrl.trim().isEmpty();
    }
}
