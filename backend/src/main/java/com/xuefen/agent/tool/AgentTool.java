package com.xuefen.agent.tool;

import java.util.Map;

/**
 * AI Agent 工具统一接口
 * 所有可被 Agent 调用的工具都实现此接口
 */
public interface AgentTool {

    /**
     * 工具名称（唯一标识）
     */
    String getName();

    /**
     * 工具描述（用于意图匹配和提示词）
     */
    String getDescription();

    /**
     * 工具适用的关键词（用于意图识别）
     */
    String[] getKeywords();

    /**
     * 执行工具
     * @param params 参数字典
     * @return 工具执行结果
     */
    ToolResult execute(Map<String, Object> params);

    /**
     * 工具执行结果
     */
    class ToolResult {
        private boolean success;
        private String message;
        private Object data;

        public static ToolResult ok(Object data) {
            ToolResult r = new ToolResult();
            r.success = true;
            r.message = "执行成功";
            r.data = data;
            return r;
        }

        public static ToolResult ok(String message, Object data) {
            ToolResult r = new ToolResult();
            r.success = true;
            r.message = message;
            r.data = data;
            return r;
        }

        public static ToolResult fail(String message) {
            ToolResult r = new ToolResult();
            r.success = false;
            r.message = message;
            return r;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
    }
}
